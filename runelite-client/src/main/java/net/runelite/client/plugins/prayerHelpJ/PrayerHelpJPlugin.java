package net.runelite.client.plugins.prayerHelpJ;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.plugins.autoBossCore.combat.ClickRouter;
import net.runelite.client.plugins.autoBossCore.combat.Prayers;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * prayerHelpJ -- an Inferno prayer helper covering two very different kinds of attacker.
 * <p>
 * Jad (JalTok-Jad): its style VARIES per attack, so it is handled REACTIVELY -- when a Jad plays its magic
 * attack animation (rears up, 7592) pray Magic, its ranged attack animation (slams its feet, 7593) pray
 * Missiles. Jad's long wind-up means the animation leads the hit, so reacting in time works.
 * <p>
 * Jal-Xil (ranger) / Jal-Zek (mager) "set" switching. Style is FIXED (Xil = Missiles, Zek = Magic), both attack
 * every 4 ticks. You turn on the overhead for the FIRST mob yourself and attack it. On that mob's first attack
 * aimed at you, it becomes the first mob: from then on your starting prayer is up ONLY on its attack ticks
 * (T, T+4, T+8 ...) and the other prayer is up on every tick in between, which covers the second mob without
 * tracking it. A click is active the next tick, so the other prayer is clicked on T (up T+1) and the starting
 * prayer on T+3 (up T+4). If a Xil and Zek attack on the same tick, magic wins. Xil dies -> mage only;
 * Zek dies -> range only. Jad wins any tick it needs a prayer.
 * <p>
 * All targeting is by id OR name (case-insensitive) so new variants are covered.
 */
@PluginDescriptor(
	name = "Prayer Help J",
	description = "Inferno prayer helper: reacts to Jad's attack animation, and switches prayers for a Jal-Xil / Jal-Zek set.",
	tags = {"prayer", "protect", "overhead", "jad", "jaltok", "jal-xil", "jal-zek", "inferno", "helper"},
	enabledByDefault = false
)
@Slf4j
public class PrayerHelpJPlugin extends Plugin implements KeyListener
{
	// ---- Jad (JalTok-Jad): style VARIES per attack -> read the animation and react (Jad has a wind-up). ----
	private static final int[] JAD_IDS = { 7700, 7704, 10623 };
	private static final String JAD_NAME = "JalTok-Jad";
	private static final int[] MAGE_ANIMS  = { 7592 };   // JalTok-Jad magic attack (rears up)
	private static final int[] RANGE_ANIMS = { 7593 };   // JalTok-Jad ranged attack (slams feet)

	// ---- Jal-Xil (ranger) / Jal-Zek (mager): style is FIXED -> set switching on the first mob's 4-tick cycle. ----
	private static final int[] XIL_IDS = { 7698, 7702 };   // Jal-Xil  -> always Protect from Missiles
	private static final String XIL_NAME = "Jal-Xil";
	private static final int XIL_RANGE_ANIM = 7605;        // its ranged attack -- the cadence anchor
	private static final int[] ZEK_IDS = { 7699, 7703 };   // Jal-Zek  -> always Protect from Magic
	private static final String ZEK_NAME = "Jal-Zek";
	private static final int ZEK_MAGE_ANIM = 7610;         // its magic attack -- the cadence anchor
	private static final int XIL_RANGE_PROJECTILE = 1376;  // Jal-Xil ranged projectile (timing logger only)
	private static final int ZEK_MAGE_PROJECTILE  = 1374;  // Jal-Zek magic projectile  (timing logger only)

	private static final int ATTACK_PERIOD = 4;   // both ranger and mager attack every 4 ticks
	private static final int PRAY_DELAY = 2;      // measured: a Jad hit lands at animation+3, and a click is active the next tick, so click at animation+2

	@Inject private Client client;
	@Inject private ClickRouter router;
	@Inject private KeyManager keyManager;
	@Inject private PrayerHelpJConfig config;
	@Inject private OverlayManager overlayManager;
	@Inject private PrayerHelpJOverlay overlay;

	/** Whether the helper is armed. Nothing is prayed while this is false. */
	private boolean flicking = false;


	// Jad: scheduled overhead activations keyed by the tick to click the prayer ON (PRAY_DELAY after the
	// animation). Two Jads scheduled for the same tick with different styles resolve to magic (never eat magic).
	private final Map<Integer, Prayer> pending = new HashMap<>();

	// ---- Xil/Zek set switching. "First mob" = the one whose style matches the overhead you turned on. ----
	private NPC primary = null;              // the first mob (null = not switching)
	private Prayer primaryStyle = null;      // your starting prayer: up only on the first mob's attack ticks
	private Prayer otherStyle = null;        // the other prayer: up on every tick in between
	private int anchor = -1;                 // tick of the first mob's last attack animation
	private int lastSwitchClickTick = -1;    // at most one switching click per tick
	private Prayer lastSwitchClicked = null;
	private int jadClickTick = -1;           // a Jad prayer was clicked this tick -> Jad wins, no switching click
	private int lastZekAttackTick = -1;      // same-tick rule: a Zek attacking you on the first mob's tick -> magic
	private NPC lastZekAttacker = null;

	/** Projectiles already logged (first sight = launch tick), so the timing logger prints each once. */
	private final Set<Projectile> seenProjectiles = new HashSet<>();

	/** Whether the helper is armed -- read by the overlay. */
	boolean isFlicking() { return flicking; }

	@Provides
	PrayerHelpJConfig provideConfig(ConfigManager cm)
	{
		return cm.getConfig(PrayerHelpJConfig.class);
	}

	@Override
	protected void startUp()
	{
		keyManager.registerKeyListener(this);
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		flicking = false;
		pending.clear();
		primary = null; primaryStyle = null; otherStyle = null; anchor = -1;
		keyManager.unregisterKeyListener(this);
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		logXilZekAnim(event);   // always-on timing logger (runs before the flicking gate, no F8 needed)

		if (!flicking) return;

		Actor actor = event.getActor();
		if (!(actor instanceof NPC)) return;
		NPC npc = (NPC) actor;

		int anim;
		try { anim = npc.getAnimation(); } catch (Throwable t) { return; }

		if (isJad(npc))
		{
			// Jad: its wind-up leads the hit. Schedule the overhead PRAY_DELAY ticks after the animation so it is
			// up on the hit tick; onGameTick clicks it on at that tick and leaves it on (a later style swaps it).
			Prayer want = jadPrayerForAnim(anim);
			if (want == null) return;   // not a magic/ranged attack animation (melee / idle / move)
			if (!targetingMe(npc)) return;   // only pre-pray a Jad whose attack is aimed at us

			int fire = client.getTickCount() + PRAY_DELAY;
			// Same click tick for two Jads of different styles -> magic wins (never eat a magic hit).
			if (pending.get(fire) == Prayer.PROTECT_FROM_MAGIC && want == Prayer.PROTECT_FROM_MISSILES) return;
			pending.put(fire, want);
			return;
		}

		// ---- Jal-Xil / Jal-Zek set switching ----
		Prayer style = fixedStyleOf(npc);
		if (style == null || !isAttackAnim(style, anim)) return;
		int now = client.getTickCount();
		Actor tgt = npc.getInteracting();
		boolean atMe = tgt == null || tgt == client.getLocalPlayer();   // null can flicker on the attack tick; the glyph is not us
		if (style == Prayer.PROTECT_FROM_MAGIC && atMe) { lastZekAttackTick = now; lastZekAttacker = npc; }

		if (primary == null)
		{
			// Not switching yet: the mob whose style matches the overhead YOU turned on becomes the first mob, on its
			// first attack aimed at you -- and only while both a Xil and a Zek are alive.
			if (!atMe || !client.isPrayerActive(style) || !bothTypesAlive()) return;
			if (style == Prayer.PROTECT_FROM_MISSILES && lastZekAttackTick == now && lastZekAttacker != null)
				setPrimary(lastZekAttacker, Prayer.PROTECT_FROM_MAGIC, now, "Zek attacked on the same tick -> magic");
			else
				setPrimary(npc, style, now, "first attack with your overhead up");
		}
		else if (npc == primary)
		{
			anchor = now;   // re-sync on every attack it makes (any target), so the cycle never drifts
			if (primaryStyle == Prayer.PROTECT_FROM_MISSILES && lastZekAttackTick == now && lastZekAttacker != null)
				setPrimary(lastZekAttacker, Prayer.PROTECT_FROM_MAGIC, now, "Zek attacked on the same tick -> magic");
		}
		else if (primaryStyle == Prayer.PROTECT_FROM_MISSILES && style == Prayer.PROTECT_FROM_MAGIC && atMe && anchor == now)
		{
			setPrimary(npc, Prayer.PROTECT_FROM_MAGIC, now, "Zek attacked on the same tick -> magic");
		}
		else return;   // the second mob: ignored -- the other prayer already covers it in the gaps
		drive(now);
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (!flicking)
		{
			if (!pending.isEmpty()) pending.clear();
			if (primary != null) clearPrimary("disarmed (F8)");
			return;
		}
		int now = client.getTickCount();

		// Jad: fire the overhead scheduled for this tick. Jad wins -- nothing else clicks a prayer this tick.
		Prayer due = pending.remove(now);
		pending.keySet().removeIf(k -> k < now);
		if (due != null) { wantPrayer(due); jadClickTick = now; return; }

		if (primary == null) return;

		// Death rules, on the death itself (isDead), not the despawn: Xil dead -> mage only; Zek dead -> range only.
		int xils = 0, zeks = 0;
		boolean primaryAlive = false;
		for (NPC n : client.getNpcs())
		{
			if (n == null || n.isDead()) continue;
			Prayer s = fixedStyleOf(n);
			if (s == Prayer.PROTECT_FROM_MISSILES) xils++;
			else if (s == Prayer.PROTECT_FROM_MAGIC) zeks++;
			else continue;
			if (n == primary) primaryAlive = true;
		}
		if (xils == 0 || zeks == 0)
		{
			clearPrimary(xils == 0 && zeks == 0 ? "no Xil or Zek left" : (xils == 0 ? "Xil dead -> mage only" : "Zek dead -> range only"));
			if (xils == 0 && zeks > 0) wantPrayer(Prayer.PROTECT_FROM_MAGIC);
			else if (zeks == 0 && xils > 0) wantPrayer(Prayer.PROTECT_FROM_MISSILES);
			return;
		}
		if (!primaryAlive)
		{
			clearPrimary("first mob died while another of its kind is alive -- next attack with your overhead up picks again");
			return;
		}
		drive(now);
	}

	/**
	 * Set switching: your starting prayer up ONLY on the first mob's attack ticks (anchor, anchor+4, ...), the other
	 * prayer on every tick in between. A click is active the NEXT tick, so this picks the prayer for now+1. At most
	 * one click per tick, and never re-clicks a prayer still waiting to come up (a second click would toggle it off).
	 * Keeps switching on the last known timing even if the first mob turns to something else.
	 */
	private void drive(int now)
	{
		if (primary == null) return;
		if (jadClickTick == now || pending.containsKey(now)) return;   // Jad wins this tick
		if (lastSwitchClickTick == now) return;
		boolean attackNext = Math.floorMod(now + 1 - anchor, ATTACK_PERIOD) == 0;
		Prayer want = attackNext ? primaryStyle : otherStyle;
		if (client.isPrayerActive(want)) return;
		if (lastSwitchClickTick == now - 1 && lastSwitchClicked == want) return;
		if (wantPrayer(want))
		{
			lastSwitchClickTick = now;
			lastSwitchClicked = want;
			log.info("prayerHelpJ: SET {} CLICK at tick {} -> up tick {} ({})", nameOf(want), now, now + 1,
				attackNext ? "first mob attacks then" : "gap");
		}
	}

	private void setPrimary(NPC npc, Prayer style, int now, String why)
	{
		primary = npc;
		primaryStyle = style;
		otherStyle = style == Prayer.PROTECT_FROM_MAGIC ? Prayer.PROTECT_FROM_MISSILES : Prayer.PROTECT_FROM_MAGIC;
		anchor = now;
		log.info("prayerHelpJ: SET first mob = {} at tick {} ({}): {} on its attack ticks, {} in between",
			style == Prayer.PROTECT_FROM_MAGIC ? "ZEK" : "XIL", now, why, nameOf(primaryStyle), nameOf(otherStyle));
	}

	private void clearPrimary(String why)
	{
		log.info("prayerHelpJ: SET switching stopped at tick {} -- {}", client.getTickCount(), why);
		primary = null;
		primaryStyle = null;
		otherStyle = null;
		anchor = -1;
	}

	/** True while at least one live (not dying) Jal-Xil AND one live Jal-Zek are in the scene. */
	private boolean bothTypesAlive()
	{
		boolean x = false, z = false;
		for (NPC n : client.getNpcs())
		{
			if (n == null || n.isDead()) continue;
			Prayer st = fixedStyleOf(n);
			if (st == Prayer.PROTECT_FROM_MISSILES) x = true;
			else if (st == Prayer.PROTECT_FROM_MAGIC) z = true;
		}
		return x && z;
	}

	private static String nameOf(Prayer p)
	{
		return p == Prayer.PROTECT_FROM_MAGIC ? "mage" : (p == Prayer.PROTECT_FROM_MISSILES ? "range" : "none");
	}

	/** Overlay text: which mob the set switching is following, or "-" when not switching. */
	String setStatus()
	{
		if (primary == null) return "-";
		return primaryStyle == Prayer.PROTECT_FROM_MAGIC ? "Zek first" : "Xil first";
	}

	// --- Always-on timing logger for Xil/Zek (no F8 needed): logs each attack-animation START tick and each
	//     projectile LAUNCH tick, so L = projectileTick - animTick. Jad is deliberately not logged here. ---

	private void logXilZekAnim(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (!(actor instanceof NPC)) return;
		NPC npc = (NPC) actor;
		int anim;
		try { anim = npc.getAnimation(); } catch (Throwable t) { return; }
		if (anim != XIL_RANGE_ANIM && anim != ZEK_MAGE_ANIM) return;
		// Also print WHO played it and WHO it is aimed at, so a log shows which gate (id/name or target) drops it.
		int id;
		try { id = npc.getId(); } catch (Throwable t) { id = -1; }
		Actor tgt = npc.getInteracting();
		Actor me = client.getLocalPlayer();
		String tgtDesc = tgt == null ? "null" : (tgt == me ? "ME" : String.valueOf(tgt.getName()));
		Prayer style = fixedStyleOf(npc);
		log.info("prayerHelpJ: {} attack anim {} at tick {} [id={} name={} target={} style={}]",
			anim == XIL_RANGE_ANIM ? "XIL" : "ZEK", anim, client.getTickCount(),
			id, npc.getName(), tgtDesc, style == null ? "NOT-RECOGNIZED" : style);
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile pr = event.getProjectile();
		if (pr == null) return;
		int id;
		try { id = pr.getId(); } catch (Throwable t) { return; }
		if (id != XIL_RANGE_PROJECTILE && id != ZEK_MAGE_PROJECTILE) return;
		if (seenProjectiles.size() > 512) seenProjectiles.clear();   // cap growth over a long session
		if (!seenProjectiles.add(pr)) return;                        // first sight = launch tick; print once
		log.info("prayerHelpJ: {} projectile {} launched at tick {}", id == ZEK_MAGE_PROJECTILE ? "ZEK" : "XIL", id, client.getTickCount());
	}

	/** True if this NPC is a Jad -- by id {7700,7704,10623} or name "JalTok-Jad". */
	private boolean isJad(NPC npc)
	{
		int id;
		try { id = npc.getId(); } catch (Throwable t) { id = -1; }
		for (int j : JAD_IDS) if (id == j) return true;
		String name = npc.getName();
		return name != null && name.equalsIgnoreCase(JAD_NAME);
	}

	/** True if this NPC is currently targeting the local player, so its attack is aimed at us. */
	private boolean targetingMe(NPC npc)
	{
		if (npc == null) return false;
		Actor me = client.getLocalPlayer();
		return me != null && npc.getInteracting() == me;
	}

	/** The protection prayer a Jad attack animation calls for, or null if the animation is not a magic/ranged attack. */
	private static Prayer jadPrayerForAnim(int anim)
	{
		for (int a : MAGE_ANIMS)  if (anim == a) return Prayer.PROTECT_FROM_MAGIC;
		for (int a : RANGE_ANIMS) if (anim == a) return Prayer.PROTECT_FROM_MISSILES;
		return null;
	}

	/** Jal-Xil -> Missiles, Jal-Zek -> Magic (by id or name, case-insensitive); null if neither. Style is fixed. */
	private Prayer fixedStyleOf(NPC npc)
	{
		int id;
		try { id = npc.getId(); } catch (Throwable t) { id = -1; }
		String name = npc.getName();
		for (int x : XIL_IDS) if (id == x) return Prayer.PROTECT_FROM_MISSILES;
		if (name != null && name.equalsIgnoreCase(XIL_NAME)) return Prayer.PROTECT_FROM_MISSILES;
		for (int z : ZEK_IDS) if (id == z) return Prayer.PROTECT_FROM_MAGIC;
		if (name != null && name.equalsIgnoreCase(ZEK_NAME)) return Prayer.PROTECT_FROM_MAGIC;
		return null;
	}

	/** True when this animation is the mob's attack (the cadence anchor): 7605 for Jal-Xil, 7610 for Jal-Zek. */
	private static boolean isAttackAnim(Prayer style, int anim)
	{
		if (style == Prayer.PROTECT_FROM_MISSILES) return anim == XIL_RANGE_ANIM;
		if (style == Prayer.PROTECT_FROM_MAGIC)    return anim == ZEK_MAGE_ANIM;
		return false;
	}

	/** Activate the given overhead if it isn't already up (OSRS auto-swaps the previous overhead off); the prayer is left on -- a later style swaps it, nothing turns it off. */
	private boolean wantPrayer(Prayer p)
	{
		if (p == null || client.isPrayerActive(p)) return false;   // already up -> leave it on
		int widget = Prayers.widgetId(client, p);
		if (widget < 0) return false;
		router.activatePrayer(widget, Prayers.name(p));
		return true;   // click sent
	}

	/** Number of Jads currently in the scene -- shown on the overlay. */
	int jadCount()
	{
		int n = 0;
		for (NPC npc : client.getNpcs())
		{
			if (npc == null) continue;
			if (isJad(npc)) n++;
		}
		return n;
	}

	@Override public void keyTyped(KeyEvent e) {}
	@Override public void keyReleased(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (!config.toggleKey().matches(e)) return;
		flicking = !flicking;
		log.debug("[prayerHelpJ] {}", flicking ? "armed" : "disarmed");
		e.consume();
	}
}
