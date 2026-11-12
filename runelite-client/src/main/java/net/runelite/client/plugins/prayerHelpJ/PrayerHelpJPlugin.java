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
import net.runelite.api.events.NpcDespawned;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * prayerHelpJ -- an Inferno prayer helper covering two very different kinds of attacker.
 * <p>
 * Jad (JalTok-Jad): its style VARIES per attack, so it is handled REACTIVELY -- when a Jad plays its magic
 * attack animation (rears up, 7592) pray Magic, its ranged attack animation (slams its feet, 7593) pray
 * Missiles. Jad's long wind-up means the animation leads the hit, so reacting in time works.
 * <p>
 * Jal-Xil (ranger) and Jal-Zek (mager): their style is FIXED (ranger always Missiles, mager always Magic) and
 * they attack every 4 ticks with no wind-up -- animation and projectile launch on the same tick, and
 * protection locks at launch, so reacting to the animation is a tick too late. Instead each mob's 4-tick
 * cadence is ANCHORED off a seen attack and the overhead is PRE-PRAYED a tick early, so it is already up when
 * the next attack launches. Tracked per NPC, so any number of off-ticked rangers/magers are handled; a mob is
 * dropped once it stops attacking or despawns. Melee is ignored (we are never in melee range of these two).
 * <p>
 * All targeting is by id OR name (case-insensitive) so new variants are covered. On a same-tick style clash
 * Magic always wins -- you never want to eat a magic hit.
 */
@PluginDescriptor(
	name = "Prayer Help J",
	description = "Inferno prayer helper: reacts to Jad's attack animation, and pre-prays Jal-Xil / Jal-Zek on their 4-tick cadence.",
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

	// ---- Jal-Xil (ranger) / Jal-Zek (mager): style is FIXED -> predict the 4-tick cadence and pre-pray. ----
	private static final int[] XIL_IDS = { 7698, 7702 };   // Jal-Xil  -> always Protect from Missiles
	private static final String XIL_NAME = "Jal-Xil";
	private static final int XIL_RANGE_ANIM = 7605;        // its ranged attack -- the cadence anchor
	private static final int[] ZEK_IDS = { 7699, 7703 };   // Jal-Zek  -> always Protect from Magic
	private static final String ZEK_NAME = "Jal-Zek";
	private static final int ZEK_MAGE_ANIM = 7610;         // its magic attack -- the cadence anchor
	private static final int XIL_RANGE_PROJECTILE = 1376;  // Jal-Xil ranged projectile (timing logger only)
	private static final int ZEK_MAGE_PROJECTILE  = 1374;  // Jal-Zek magic projectile  (timing logger only)

	private static final int ATTACK_PERIOD = 4;   // both ranger and mager attack every 4 ticks
	private static final int STALE_TICKS = 5;     // drop a tracked mob this many ticks after its last seen attack
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

	/** Per ranger/mager (keyed by NPC index): its fixed overhead and the tick of its last observed attack. */
	private final Map<Integer, Sched> cadence = new HashMap<>();

	/** Projectiles already logged (first sight = launch tick), so the timing logger prints each once. */
	private final Set<Projectile> seenProjectiles = new HashSet<>();

	private static final class Sched
	{
		final Prayer style;
		int anchor;
		final NPC npc;
		Sched(Prayer style, int anchor, NPC npc) { this.style = style; this.anchor = anchor; this.npc = npc; }
	}

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
		cadence.clear();
		pending.clear();
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

		// Ranger / mager: fixed style. Anchor its 4-tick cadence off this attack so onGameTick can have the
		// overhead up on its NEXT attack tick (anchor+4), clicked at anchor+3.
		Prayer fixed = fixedStyleOf(npc);
		if (fixed == null) return;
		if (!isAttackAnim(fixed, anim)) return;   // only its attack animation anchors (melee / idle / move ignored)
		// Bail only if it is clearly attacking SOMEONE ELSE. getInteracting() can read null on the attack tick, and the
		// old strict "== me" check here silently dropped Zek every time (never scheduled -> mage never went up).
		Actor tgt = npc.getInteracting();
		if (tgt != null && tgt != client.getLocalPlayer()) return;
		cadence.put(npc.getIndex(), new Sched(fixed, client.getTickCount(), npc));
		log.info("prayerHelpJ: {} cadence anchored at tick {}", fixed == Prayer.PROTECT_FROM_MAGIC ? "ZEK" : "XIL", client.getTickCount());
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (!flicking)
		{
			if (!cadence.isEmpty()) cadence.clear();
			if (!pending.isEmpty()) pending.clear();
			return;
		}
		int now = client.getTickCount();

		// Fire the Jad overhead scheduled for this tick (PRAY_DELAY after its animation); drop any missed ones.
		Prayer due = pending.remove(now);
		if (due != null) wantPrayer(due);
		pending.keySet().removeIf(k -> k < now);

		// Ranger/mager cadence: pre-pray their fixed style a tick before each 4-tick launch (only if any tracked).
		if (!cadence.isEmpty())
		{
			// Drop mobs that have stopped attacking us (no fresh attack animation within a cadence + slack window).
			for (Iterator<Sched> it = cadence.values().iterator(); it.hasNext(); )
			{
				if (now - it.next().anchor > STALE_TICKS) it.remove();
			}

			// Each mob's hit is checked on its own 4-tick phase (anchor, anchor+4, ...). Click at anchor+3 so the
			// overhead is up ON anchor+4, the mob's next attack tick. (anchor+1 was wrong: with two mobs 2 ticks apart
			// it put each prayer up on the OTHER mob's attack tick -- log of tick 26146-26156.)
			boolean needMagic = false, needRanged = false;
			for (Sched s : cadence.values())
			{
				if (now - s.anchor != ATTACK_PERIOD - 1) continue;   // not one tick before this mob's next attack
				// Skip only if it's clearly attacking SOMEONE ELSE. getInteracting() flickers to null between
				// attacks, and the strict check used to drop the pre-pray on those ticks (missed swaps); null = still ours.
				Actor tgt = s.npc.getInteracting();
				if (tgt != null && tgt != client.getLocalPlayer()) continue;
				if (s.style == Prayer.PROTECT_FROM_MAGIC) needMagic = true;
				else if (s.style == Prayer.PROTECT_FROM_MISSILES) needRanged = true;
			}
			// Off-ticked, only one is ever true. If a ranger and mager ever launch together, magic wins.
			if (needMagic)
			{
				boolean clicked = wantPrayer(Prayer.PROTECT_FROM_MAGIC);
				log.info("prayerHelpJ: ZEK mage {} at tick {} -> up tick {}", clicked ? "CLICK" : "already up", now, now + 1);
			}
			else if (needRanged)
			{
				boolean clicked = wantPrayer(Prayer.PROTECT_FROM_MISSILES);
				log.info("prayerHelpJ: XIL range {} at tick {} -> up tick {}", clicked ? "CLICK" : "already up", now, now + 1);
			}
		}
		// Leave-on: an overhead stays up until a different one is needed (activating it swaps the old one off).
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		if (npc != null) cadence.remove(npc.getIndex());
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
