package net.runelite.client.plugins.prayerHelp;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.autoBossCore.combat.ClickRouter;
import net.runelite.client.plugins.autoBossCore.combat.Prayers;

import javax.inject.Inject;
import java.awt.event.KeyEvent;

/**
 * prayerHelp -- a 4-tick protection-prayer cadence keyed to TICKS SINCE LOGIN (the same counter the Report
 * Button plugin shows as "Game Ticks"). The counter resets to 0 on each login / world hop and counts up one
 * per game tick. Prayer 1 activates on tick {@code firstPrayerTick} since login (e.g. 15); earlier ticks do
 * nothing. From there the plan cycles every 4 ticks: tick firstPrayerTick -> p1, +1 -> p2, +2 -> p3, +3 -> p4,
 * +4 -> p1, ... The hotkey only arms/disarms; it does NOT reset or anchor the timing (login does).
 * <p>
 * Per targeted slot: NONE makes no change (leave the current overhead); an already-active prayer is left on;
 * otherwise it is activated with one click (OSRS auto-swaps the previous overhead off).
 */
@PluginDescriptor(
	name = "Prayer Help",
	description = "Login-tick-anchored 4-tick protection-prayer cadence (melee / ranged / magic) from dropdowns.",
	tags = {"prayer", "protect", "overhead", "flick", "helper"},
	enabledByDefault = false
)
@Slf4j
public class PrayerHelpPlugin extends Plugin implements KeyListener
{
	@Inject private Client client;
	@Inject private ClickRouter router;
	@Inject private KeyManager keyManager;
	@Inject private PrayerHelpConfig config;

	/** Whether the cadence is armed. Timing is login-anchored, independent of when this is toggled. */
	private boolean flicking = false;
	/** Game ticks since the last login / hop -- matches the Report Button plugin's "Game Ticks". Drives timing. */
	private int ticksSinceLogin = 0;
	/** Set on LOGGING_IN / HOPPING so the next LOGGED_IN resets the counter exactly once (Report Button pattern). */
	private boolean ready = false;

	// Inferno off-cadence mobs (bat + Jal-Ak blob family): prayed off their projectiles, NOT the 4-tick cadence.
	private static final int[] OFF_CADENCE_NPCS = { 7692, 7693, 7694, 7695, 7696 };   // bat, blob, 3 mini-blobs
	private static final int[] CADENCE_NPCS     = { 7697, 7698, 7699 };               // meleer, ranger, mage

	@Provides
	PrayerHelpConfig provideConfig(ConfigManager cm)
	{
		return cm.getConfig(PrayerHelpConfig.class);
	}

	@Override
	protected void startUp()
	{
		keyManager.registerKeyListener(this);
	}

	@Override
	protected void shutDown()
	{
		flicking = false;
		keyManager.unregisterKeyListener(this);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// Reset ticks-since-login on each fresh login / hop, exactly like the Report Button plugin, so the
		// number driving our cadence lines up tick-for-tick with its "Game Ticks" display.
		switch (event.getGameState())
		{
			case LOGGING_IN:
			case HOPPING:
				ready = true;
				break;
			case LOGGED_IN:
				if (ready) { ticksSinceLogin = 0; ready = false; }
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		// Advance the login-anchored counter every tick (armed or not) so it stays aligned with the Report
		// Button's "Game Ticks". Increment first: the Nth tick after a login reset reads ticksSinceLogin == N.
		ticksSinceLogin++;
		if (!flicking) return;

		// Off-cadence phase: once the 4-tick mobs (meleer/ranger/mage) are dead and only bats/blobs remain,
		// they don't fit the fixed rotation -- drive prayers off their projectiles (impact tick + style) instead.
		if (useReactiveHandler()) { handleBatsBlobs(); return; }

		// Prayer 1 fires on tick firstPrayerTick SINCE LOGIN; earlier ticks idle.
		int local = ticksSinceLogin;
		int first = config.firstPrayerTick();
		if (local < first) return;

		// Step into the 4-tick plan: tick first -> p1, first+1 -> p2, first+2 -> p3, first+3 -> p4, repeat.
		ProtectionStyle style = slotForStep(local - first);
		if (style == null || style == ProtectionStyle.NONE) return;   // NONE -> no prayer change this tick
		if (!anyNpcAlive(requiredNpcsFor(style))) return;             // no live mob for this style -> skip the switch
		prayStyle(style);
	}

	/** The plan slot for a cadence step (0 -> p1, 1 -> p2, 2 -> p3, 3 -> p4, mod 4; step is always >= 0). */
	private ProtectionStyle slotForStep(int step)
	{
		switch (((step % 4) + 4) % 4)
		{
			case 0:  return config.p1();
			case 1:  return config.p2();
			case 2:  return config.p3();
			default: return config.p4();
		}
	}

	/**
	 * NPC ids that justify praying a given style: a slot is only activated when at least one of these is
	 * in the scene. A style with NO ids here is never gated (always allowed). Extend per encounter.
	 */
	private static int[] requiredNpcsFor(ProtectionStyle style)
	{
		switch (style)
		{
			case RANGED: return new int[]{ 7692, 7693, 7695, 7698 };   // ranged mobs (7693 also attacks with magic)
			case MAGIC:  return new int[]{ 7693, 7694, 7699 };   // magic mobs (7693 also attacks with ranged)
			case MELEE:  return new int[]{ 7696, 7697 };   // melee mobs
			default:     return new int[0];        // ungated -> always allowed
		}
	}

	/** True if no NPC gate applies ({@code ids} empty), or at least one of {@code ids} is a live NPC in the scene. */
	private boolean anyNpcAlive(int[] ids)
	{
		if (ids.length == 0) return true;   // no requirement for this style
		for (NPC npc : client.getNpcs())
		{
			if (npc == null) continue;
			int id;
			try { id = npc.getId(); } catch (Throwable t) { continue; }   // NPC mid-teardown -> skip
			for (int want : ids) if (id == want) return true;
		}
		return false;
	}

	/** Activate the overhead for this style if it isn't already up (OSRS auto-swaps the previous overhead off). */
	private void prayStyle(ProtectionStyle style)
	{
		if (style == null) return;
		Prayer p = style.prayer();
		if (p == null || client.isPrayerActive(p)) return;   // NONE / no mapping, or already up -> leave it on
		int widget = Prayers.widgetId(client, p);            // resolves itself off the prayer book
		if (widget >= 0) router.activatePrayer(widget, Prayers.name(p));
	}

	/**
	 * The reactive projectile handler runs when AT MOST ONE "main" mob (mager/ranger/meleer) is alive and at
	 * least one bat/blob (family) is present. Two or more main mobs fall back to the 4-tick dropdown cadence.
	 */
	private boolean useReactiveHandler()
	{
		return mainMobCount() <= 1 && anyNpcAlive(OFF_CADENCE_NPCS);
	}

	/** Number of alive "main" mobs (mager 7699 / ranger 7698 / meleer 7697). */
	private int mainMobCount()
	{
		int n = 0;
		for (NPC npc : client.getNpcs())
		{
			if (npc == null) continue;
			int id;
			try { id = npc.getId(); } catch (Throwable t) { continue; }   // NPC mid-teardown -> skip
			for (int m : CADENCE_NPCS) if (id == m) { n++; break; }
		}
		return n;
	}

	/**
	 * Projectile-driven prayer for the bat/blob (+ up to one main mob) phase. Each tick, find the NEAREST incoming
	 * attack and pray its style; on a same-tick collision MAGIC always beats RANGED (you never eat a magic hit --
	 * off-tick the mobs to avoid collisions). Reads the actual style off each projectile, so the blob's 'counter
	 * your prayer' choice and the 3/4/6-tick cadences are handled automatically. A lone meleer and all melee bites
	 * are NOT handled here (projectiles only) -- those are tanked.
	 */
	private void handleBatsBlobs()
	{
		Player me = client.getLocalPlayer();
		if (me == null) return;
		java.util.Deque<Projectile> projs = client.getProjectiles();
		if (projs == null) return;

		// Nearest incoming attack tick among recognised, player-targeted projectiles.
		int nearest = Integer.MAX_VALUE;
		for (Projectile pr : projs)
		{
			if (pr == null || pr.getTargetActor() != me || projectileStyle(pr.getId()) == null) continue;
			int t = (pr.getRemainingCycles() + 29) / 30;
			if (t < nearest) nearest = t;
		}
		if (nearest == Integer.MAX_VALUE) return;   // nothing incoming -> leave the current prayer as-is

		// Resolve the style landing on that nearest tick: MAGIC beats RANGED on any same-tick collision.
		boolean magic = false, ranged = false;
		for (Projectile pr : projs)
		{
			if (pr == null || pr.getTargetActor() != me) continue;
			ProtectionStyle st = projectileStyle(pr.getId());
			if (st == null) continue;
			if ((pr.getRemainingCycles() + 29) / 30 != nearest) continue;
			if (st == ProtectionStyle.MAGIC) magic = true; else ranged = true;
		}

		if (magic) prayStyle(ProtectionStyle.MAGIC);          // magic always wins a same-tick collision
		else if (ranged) prayStyle(ProtectionStyle.RANGED);
	}

	/** Style of a recognised bat/blob-family projectile, or null if not one we pray against. */
	private static ProtectionStyle projectileStyle(int projectileId)
	{
		switch (projectileId)
		{
			case 1374:   // Jal-Zek (mager) magic
			case 1380:   // Jal-Ak (blob) magic
			case 1381:   // Jal-AkRek-Mej (mini-blob) magic
				return ProtectionStyle.MAGIC;
			case 1376:   // Jal-Xil (ranger) ranged
			case 1378:   // Jal-Ak (blob) ranged
			case 1379:   // Jal-AkRek-Xil (mini-blob) ranged
			case 1382:   // Jal-MejRah (bat) ranged
				return ProtectionStyle.RANGED;
			default:
				return null;
		}
	}

	@Override public void keyTyped(KeyEvent e) {}
	@Override public void keyReleased(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (!config.toggleKey().matches(e)) return;
		flicking = !flicking;   // arm/disarm only -- timing is login-anchored, so this never resets the cadence
		log.debug("[prayerHelp] {} (ticksSinceLogin {})", flicking ? "armed" : "disarmed", ticksSinceLogin);
		e.consume();
	}
}
