package net.runelite.client.plugins.dt2LeviathanAuto;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.autoBossCore.combat.ClickRouter;
import net.runelite.client.plugins.autoBossCore.camera.ArenaCamera;
import net.runelite.client.plugins.autoBossCore.loot.LootCollector;
import net.runelite.client.plugins.autoBossCore.loot.GroundItemTracker;
import net.runelite.client.plugins.autoBossCore.loot.DecantManager;
import net.runelite.client.plugins.autoBossCore.combat.Hazard;
import net.runelite.client.plugins.autoBossCore.combat.PrayerSwapper;
import net.runelite.client.plugins.autoBossCore.combat.SchedulingTileDodger;
import net.runelite.client.plugins.autoBossCore.consume.ConsumptionEngine;
import net.runelite.client.plugins.autoBossCore.phase.PhaseTracker;
import net.runelite.client.plugins.autoBossCore.state.InstanceCoords;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;
import net.runelite.client.plugins.autoBossCore.state.SceneScanner;
import net.runelite.client.plugins.autoBossCore.state.WidgetReader;
import net.runelite.client.plugins.autoBossCore.trip.TripState;
import net.runelite.client.plugins.autoBossCore.bank.BankRun;
import net.runelite.client.plugins.autoBossCore.ui.BaseOverlay;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Leviathan strategy skeleton. Owns the per-tick orchestration: find the boss, update
 * the phase, project hazards, plan a step, run prayer / supply / attack maintenance.
 * <p>
 * Mirrors {@link net.runelite.client.plugins.dt2VardorvisAuto.VardorvisAutoMain}. The
 * mechanic-specific bits (axe projection, QTE solver, scripted outgoing-return) are
 * dropped since Leviathan has different mechanics. Fill them in as the fight is
 * reverse-engineered via the capture logger.
 * <p>
 * TODOs: hazard projection ({@code pushHazards}), per-attack prayer swap, home-tile
 * selection + arena-scan fallback once we know the viable kiting pattern.
 */
@Slf4j
@Singleton
public class LeviathanAutoMain
{
	public enum LevPhase { OUT_OF_FIGHT, IDLE, FIGHT }

	/**
	 * Post-section-1 phase, deterministically chosen by Leviathan at fight start and
	 * revealed by the first roar's rock-drop pattern. See
	 * {@link LeviathanAutoIDs#PHASE_W_CENTER_TEMPLATE} for the geometry.
	 */
	public enum Section2Phase { UNKNOWN, LIGHTNING, BOULDER }

	// --- Services ---------------------------------------------------
	private final Client client;
	private final MirrorState mirror;
	private final ClickRouter router;
	private final SchedulingTileDodger dodger;
	private final PrayerSwapper prayers;
	private final LootCollector lootCollector;   // shared post-death loot pass
	private final GroundItemTracker groundItems; // shared ground-item tracker (loot + decant make-room)
	private final DecantManager decant;          // shared potion-decant / tidy / make-room pass

	// --- Boss state -------------------------------------------------
	private final ConsumptionEngine engine;
	// Ranged combat-boost (divine ranging preferred, else ranging). Divine runs on a 5-min
	// timer; regular re-doses below LeviathanAutoIDs.RANGING_REDRINK_MIN_BOOST. See tracker.
	private final net.runelite.client.plugins.autoBossCore.consume.CombatBoostTracker rangingBoost =
		new net.runelite.client.plugins.autoBossCore.consume.CombatBoostTracker();
	private final TripState trip = new TripState();
	private final BankRun bankRun;   // shared POH->bank travel run (WIP: manual F7-in-POH trigger)
	private final PhaseTracker<LevPhase> phase;

	private volatile boolean active = false;
	/** Detached overhead arena camera (shared helper). */
	private final ArenaCamera camera;
	private WorldPoint lastCamCenter = null;   // instance tile the camera is currently framed on

	// -----------------------------------------------------------------
	// Section-1 dynamic configuration.
	// -----------------------------------------------------------------

	/** Axis along which the section-1 anchor advances. COLUMN = fixed X, varying Y;
	 *  ROW = fixed Y, varying X. */
	public enum Axis { COLUMN, ROW }

	/**
	 * Dynamic section-1 configuration. Replaces the old hardcoded "south-step down
	 * x=2071 column" rule so section 1 can run on any axis + direction. One of
	 * these is active per cycle; the next-cycle config is looked up from
	 * {@link #PHASE2_END_TO_NEXT_SECTION} at cycle-end.
	 */
	public static final class SectionConfig
	{
		public final Axis axis;
		public final int fixedCoord;   // X for COLUMN, Y for ROW
		public final int startCoord;   // Y for COLUMN, X for ROW -- starting value of the varying coord
		public final int direction;    // +1 or -1 along the varying axis
		public final int plane;

		public SectionConfig(Axis axis, int fixedCoord, int startCoord, int direction, int plane)
		{
			this.axis = axis;
			this.fixedCoord = fixedCoord;
			this.startCoord = startCoord;
			this.direction = direction;
			this.plane = plane;
		}

		/** Starting template tile for this section. */
		public WorldPoint startTile()
		{
			return axis == Axis.COLUMN
				? new WorldPoint(fixedCoord, startCoord, plane)
				: new WorldPoint(startCoord, fixedCoord, plane);
		}

		/** One step in {@link #direction} from {@code current}, respecting the axis. */
		public WorldPoint advance(WorldPoint current)
		{
			if (current == null) return null;
			return axis == Axis.COLUMN
				? new WorldPoint(fixedCoord, current.getY() + direction, current.getPlane())
				: new WorldPoint(current.getX() + direction, fixedCoord, current.getPlane());
		}

		/** The varying coord of {@code wp} along this config's axis. */
		public int variableCoord(WorldPoint wp)
		{
			return axis == Axis.COLUMN ? wp.getY() : wp.getX();
		}

		@Override public String toString()
		{
			return "SectionConfig{" + axis + " fixed=" + fixedCoord + " start=" + startCoord
				+ " dir=" + (direction > 0 ? "+" : "") + direction + "}";
		}
	}

	/** Round-1 bootstrap config: south-step down the west column from START_TILE_TEMPLATE. */
	public static final SectionConfig ROUND_1_CONFIG =
		new SectionConfig(Axis.COLUMN, 2071, 6377, -1, 0);

	/**
	 * Low-HP finisher config: once Leviathan is under
	 * {@link LeviathanAutoIDs#LOW_HP_ROAR_THRESHOLD}, roar along the north row east-step
	 * from (2074,6381) instead of the mapped next lane. Only taken at a roar-phase start.
	 */
	public static final SectionConfig LOW_HP_ROAR_CONFIG =
		new SectionConfig(Axis.ROW, 6381, 2074, +1, 0);

	/**
	 * Sentinel config for the first east-lightning phase-2 follow-up. Its start tile is
	 * the zigzag path's first tile; beginCycle recognises it (by reference) and installs
	 * {@link LeviathanAutoIDs#EAST_LIGHTNING_P2_ROAR_PATH} as a scripted roar order.
	 */
	public static final SectionConfig EAST_LIGHTNING_P2_ZIGZAG_CONFIG =
		new SectionConfig(Axis.COLUMN, 2088, 6363, +1, 0);

	/**
	 * Lookup: phase-2 end tile (template) -&gt; next cycle's SectionConfig. ONLY the
	 * 3 canonical paths the user hand-mapped. Any phase-2 end tile not in this map
	 * means an upstream picker was suboptimal and we stall (manual intervention).
	 */
	private static final Map<WorldPoint, SectionConfig> PHASE2_END_TO_NEXT_SECTION;
	static
	{
		Map<WorldPoint, SectionConfig> m = new HashMap<>();
		// Boulder NE cluster backfill -> east column south-step.
		m.put(new WorldPoint(2088, 6381, 0), new SectionConfig(Axis.COLUMN, 2090, 6380, -1, 0));
		// Boulder SE cluster end tile (2091,6369, now the safe attack tile) -> east column
		// (2091) north-step from y=6369. The end tile IS the next section's start tile.
		m.put(new WorldPoint(2091, 6369, 0), new SectionConfig(Axis.COLUMN, 2091, 6369, +1, 0));
		// WEST_SOUTH_LIGHTNING end -> SW column north-step.
		m.put(new WorldPoint(2076, 6368, 0), new SectionConfig(Axis.COLUMN, 2072, 6365, +1, 0));
		// WEST_NORTH_LIGHTNING end -> north row east-step.
		m.put(new WorldPoint(2076, 6376, 0), new SectionConfig(Axis.ROW,    6381, 2074, +1, 0));

		// ---- Lightning-first scenario (cycle-0 phase-2 is lightning on the EAST lane) ----
		// Either east-lightning end -> NE column x=2090, south-step 6 roars (6380..6375).
		m.put(new WorldPoint(2086, 6368, 0), new SectionConfig(Axis.COLUMN, 2090, 6380, -1, 0));
		m.put(new WorldPoint(2086, 6376, 0), new SectionConfig(Axis.COLUMN, 2090, 6380, -1, 0));
		// SW boulder cluster backfill -> SW column x=2072, north-step 6 roars (6365..6370).
		m.put(new WorldPoint(2073, 6364, 0), new SectionConfig(Axis.COLUMN, 2072, 6365, +1, 0));
		PHASE2_END_TO_NEXT_SECTION = Collections.unmodifiableMap(m);
	}

	private NPC leviathan;
	private int lastAttackTick = -100;

	/**
	 * Was Leviathan non-null on the previous tick? Used to detect the death/despawn
	 * transition (present -&gt; null) so one-shot per-fight state can repopulate:
	 * {@link #usedBoulderClusterIdxs} unlocks all 4 clusters again, and the
	 * permanent-rock tracking in {@link #trackedRockDropTemplates} clears (the
	 * rocks disappear visually when Leviathan dies).
	 */
	private boolean leviathanWasPresent = false;
	// Latched once the boss HP widget reads 0 (the kill), so prayers are turned off at death
	// -- a few ticks before the NPC despawns -- and only once. Cleared in resetFightState.
	private boolean deathPrayersOff = false;
	/** getTickCount() of the kill, anchoring the post-death loot pass (-1 = not dead / loot done). */
	private int lastDeathTick = -1;
	/** Ticks of lookback when arming loot on a MISSED HP-0 read: anchor at despawn, reach back to the kill. */
	private static final int LEVIATHAN_LOOT_LOOKBACK = 12;
	/** Last readable boss HP, cached each tick. The HP bar can briefly read -1 (hidden) at the phase-2-end
	 *  animation transition -- exactly when the next-section route is decided -- so we fall back to this. */
	private int lastKnownBossHp = -1;
	/** getTickCount() of the last between-fights top-off eat -- paces cleanup eating (no EatDelay here). */
	private int lastCleanupEatTick = -100;
	private int lastTopOffEatTick = -100;   // paces in-fight top-off eating during the safe run windows

	/**
	 * Projectile identities (endTick<<16 ^ id) we've already scheduled a prayer swap for,
	 * so impact-1 swaps aren't re-queued every tick while a projectile is still in flight.
	 * Pruned once the impact tick passes.
	 */
	private final java.util.Set<Integer> scheduledPrayIdentities = new java.util.HashSet<>();
	// Measurement: per falling-hazard GFX id, the MAX observed ticks-from-spawn (spawn ->
	// last tick the spotanim was visible), i.e. roughly each hazard's time-to-impact. Updated
	// as hazards are seen and dumped when Leviathan dies ([lev-hazard-times]). Kept across
	// kills so the estimate stabilises. Sorted by id for a readable dump.
	private final java.util.Map<Integer,Integer> observedImpactTicksById = new java.util.TreeMap<>();

	/**
	 * Current Leviathan loadout is a 4-tick ranged weapon. TODO: lift the speed into
	 * a shared weapon database in autoBossCore (itemId -> attack speed) so per-boss
	 * profiles stop hardcoding numbers that drift from gear changes.
	 */
	private static final int WEAPON_COOLDOWN = 4;   // 4-tick attack speed
	/** Ticks after the lightning-charge chat message before the oscillation starts. */
	private static final int LIGHTNING_CHARGE_TO_OSC_TICKS = 7;

	/**
	 * DEBUG switch: when true, the plugin stop()s itself the first tick the player
	 * lands on {@link LeviathanAutoIDs#EAST_CENTER_LIGHTNING} with Shadow Rush already
	 * cast. Lets us observe section-2 mechanics (lightning telegraph, Leviathan anim,
	 * boulder drops) without the script interfering. Flip to false once the lightning
	 * / boulder sub-handlers are wired up.
	 */
	private static final boolean DEBUG_PAUSE_AT_POSTCAST = false;

	// Instance-resolved start tile. Null until the player enters the arena region.
	// Leviathan is a mobile fight -- this is only the kick-off position; the planner
	// does NOT anchor to it mid-fight.
	private WorldPoint startTile;
	private int lastResolveTick = -100;

	/**
	 * Section 1 anchor TEMPLATE coord. The planner always walks the player to the
	 * instance-resolution of this tile. Advances south each time an unbreakable
	 * rock drop (see {@link LeviathanAutoIDs#GFX_UNBREAKABLE_ROCK_IDS}) lands on
	 * our current tile and Leviathan is roaring. One roar normally advances by one
	 * tile, but {@link #skipSightlineBlockedAhead(WorldPoint)} may extend the step
	 * when a same-column tracked rock would block our LOS to Leviathan. Section 1
	 * terminates when {@link #section1RoarCount} reaches
	 * {@link LeviathanAutoIDs#TOTAL_SECTION_1_ROARS}, not on any specific tile.
	 * Reset to START_TILE_TEMPLATE on each FIGHT entry.
	 */
	private WorldPoint section1AnchorTemplate = ROUND_1_CONFIG.startTile();

	/** The dynamic config driving section 1 this cycle. Determines axis, direction,
	 *  anchor advance, sightline-skip, and spine-mirror geometry. Reset to
	 *  {@link #ROUND_1_CONFIG} in {@link #start()}, swapped by {@link #beginCycle}. */
	private SectionConfig activeSectionConfig = ROUND_1_CONFIG;

	/** Dedupe tick for south-step triggers. The 2479 GFX persists several ticks,
	 *  but by the time we step south the GFX is no longer on our (new) tile, which
	 *  naturally gates the next trigger. This gives one extra tick of safety. */
	private int lastSouthStepTick = -100;

	/**
	 * Count of section-1 roars observed so far. Each anchor advance (whether primary
	 * trigger "rock on player" or fallback trigger "anchor tile in tracked set")
	 * increments by one. Section 1 completes when this reaches
	 * {@link LeviathanAutoIDs#TOTAL_SECTION_1_ROARS}. Replaces the old Y-threshold
	 * completion so tiles can be skipped (LOS-blocked by a same-column rock) without
	 * disturbing the count. Reset on each {@link #start()}.
	 */
	private int section1RoarCount = 0;

	/**
	 * Per-section roar target. Normally {@link LeviathanAutoIDs#TOTAL_SECTION_1_ROARS}
	 * (6), but the post-SE-boulder east-column section (x=2091, north from y=6369) uses
	 * {@code 6 - shadowRushUses}: each shadow rush used earlier in the fight has eaten
	 * one more tile off that lane. Set by {@link #beginCycle} / {@link #resetFightState}.
	 */
	private int section1RoarTarget = LeviathanAutoIDs.TOTAL_SECTION_1_ROARS;

	/**
	 * When non-null, section-1 follows this explicit ordered tile list (a scripted zigzag)
	 * instead of the straight {@link #activeSectionConfig} lane. {@link #scriptedRoarIdx}
	 * is the current index. Installed by {@link #beginCycle}, cleared otherwise.
	 */
	private WorldPoint[] scriptedRoarPath = null;
	private int scriptedRoarIdx = 0;

	/**
	 * Count of Shadow Rush casts CONFIRMED (player cast anim seen) so far this fight.
	 * Persists across cycles -- reset only in {@link #resetFightState}, never in
	 * {@link #beginCycle} -- because it measures cumulative lane consumption.
	 */
	private int shadowRushUses = 0;

	/**
	 * Cached section-2 phase, classified once the first roar's drops land. Stays
	 * UNKNOWN until classification succeeds; then LIGHTNING or BOULDER for the rest
	 * of the kill. Reset on each {@link #start()}.
	 */
	private Section2Phase section2Phase = Section2Phase.UNKNOWN;

	// --- Enrage phase state (new mechanic) ------------------------------------
	/** Orthogonal to section2Phase: enrage can trigger during BOULDER or LIGHTNING. */
	public enum EnrageQuadrant { UNKNOWN, NE, SE, SW, NW }
	/** True once GFX_ENRAGE_PLUS has been seen. Cleared on fight start / Leviathan despawn. */
	private boolean enragePhaseActive = false;
	/** SW corner (TEMPLATE) of the 3x3 the GFX_ENRAGE_PLUS (2298) '+' marked at the instant enrage began --
	 *  captured once and committed as the approach target, so we run straight there from wherever we were
	 *  when it appeared instead of re-chasing the moving pathfinder (which dithered CW<->CCW). */
	private WorldPoint enrageApproachTargetTemplate = null;
	/** Eagle Eye enrage latch: set once we stand in the pathfinder 3x3 during enrage, then kept ON until
	 *  the kill ends. Reset at fight start only (NOT per cycle). */
	private boolean eagleEyeEnrageLatched = false;
	/** Resolved from the + layout the tick enrage is first seen. */
	private EnrageQuadrant enrageQuadrant = EnrageQuadrant.UNKNOWN;
	/** The Abyssal Pathfinder reference once it spawns (NPC id 12220). */
	private net.runelite.api.NPC abyssalPathfinder = null;

	/**
	 * Set to true the tick we fire the single Shadow Rush that transitions out of
	 * section 1. Flips the planner's destination from the south-column anchor to
	 * {@link LeviathanAutoIDs#EAST_CENTER_LIGHTNING} (spine-side back-hit tile, gated by shadowRushCastConfirmed,
	 * since Shadow Rush was cast from the west column so Leviathan now faces west),
	 * and gates the attack path from "cast spell" back to "ranged auto-attack".
	 * Reset on each {@link #start()}; we only ever cast Shadow Rush once per kill.
	 */
	private boolean shadowRushCast = false;

	/**
	 * Latches true the first tick we observe the PLAYER'S own Shadow Rush cast
	 * animation ({@link LeviathanAutoIDs#ANIM_CAST_SHADOW_RUSH}, 10092). This
	 * confirms the server accepted our cast dispatch -- it fires immediately on
	 * the cast tick, with no projectile-travel delay (so we start walking to the
	 * spine tile as soon as the cast is confirmed, not when the spell impacts).
	 * Reset in start().
	 */
	private boolean shadowRushCastConfirmed = false;

	/**
	 * Set true the first tick we land a ranged attack while standing on the spine
	 * tile ({@link #postCastTargetTemplate()}). Transitions the planner from the
	 * "walk to spine" phase to the "walk to post-spine phase-2 tile" phase. One
	 * attack at the spine is enough to apply the back-hit damage; we don't linger.
	 * Reset in start().
	 */
	private boolean spineAttackDone = false;

	/**
	 * Latches true the first tick Leviathan plays
	 * {@link LeviathanAutoIDs#ANIM_BOULDER_PHASE_START}. Gates the boulder dodge
	 * sequence so it only runs when we're actually in boulder phase. Reset in start().
	 */
	private boolean boulderPhaseActive = false;

	/**
	 * Latches true the first tick we observe {@link LeviathanAutoIDs#ANIM_LIGHTNING}
	 * (10285) on Leviathan while {@link #section2Phase} is LIGHTNING. Gate for the
	 * shared phase-2-end detection -- without this, a spurious 10287 earlier in the
	 * fight could trigger a false "lightning ended". Reset in start()/beginCycle.
	 */
	private boolean lightningPhaseActive = false;

	/**
	 * Tick at which Leviathan's "charges up a lightning attack" chat message was seen
	 * this lightning phase (via {@link #onChatMessage}), or < 0 if not yet seen. The
	 * oscillation starts {@link #LIGHTNING_CHARGE_TO_OSC_TICKS} ticks later. Re-armed
	 * each cycle.
	 */
	private int lightningChargeMsgTick = -100;

	/**
	 * Count of distinct {@link LeviathanAutoIDs#PROJ_BOULDER} projectiles observed
	 * since boulder phase started. {@code count / 2} (capped at 5) indexes into
	 * {@link #activeBoulderCluster} to pick the current dodge tile. Reset in start().
	 */
	private int boulderProjectileCount = 0;

	/**
	 * The boulder cluster chosen for this boulder phase. One of
	 * {@link LeviathanAutoIDs#BOULDER_CLUSTERS}, picked by {@link #pickBoulderClusterIfNeeded()}
	 * at spine-attack time -- whichever cluster's position 1 is closest (Chebyshev)
	 * to the player's tile at that moment. null until picked, cleared each cycle.
	 */
	private WorldPoint[] activeBoulderCluster = null;

	/** Cluster indexes we've already fired an explicit boulder-phase attack from.
	 *  Dedupe so each attack slot (indexes 0, 2, 4, 5 per
	 *  {@link LeviathanAutoIDs#BOULDER_ATTACK_IDXS_SAFE_GAP} and
	 *  {@link LeviathanAutoIDs#BOULDER_ATTACK_IDXS_UNSAFE_GAP}) fires at most once per phase.
	 *  The slip-attack at index 5 may require waiting a tick or two for cooldown;
	 *  that waiting is naturally retried each tick until canAttack succeeds. */
	private final java.util.Set<Integer> firedBoulderAttackIdxs = new java.util.HashSet<>();

	/** Latches true after we fire the backfill attack at cluster index 5 on a
	 *  safe-gap cluster. Triggers the planner to route back to cluster[4] (pos 5)
	 *  for the walkback attack. Reset in start()/beginCycle. */
	private boolean boulderBackfillAttackDone = false;

	/** Latches true after the walkback attack lands at cluster[4] (pos 5) following
	 *  the backfill. Planner holds at cluster[4] for the rest of the phase.
	 *  Reset in start()/beginCycle. */
	private boolean boulderWalkbackAttackDone = false;

	/**
	 * Indexes into {@link LeviathanAutoIDs#BOULDER_CLUSTERS} that have already
	 * been consumed this fight. Each cluster is one-shot -- once used, it can't
	 * be picked again. Persists across cycles (NOT reset by beginCycle), only
	 * cleared in start().
	 * <p>
	 * TODO (edge case): what happens if all 4 are used and another boulder phase
	 * fires. For now the picker will log and no routing fires; add fallback later.
	 */
	private final java.util.Set<Integer> usedBoulderClusterIdxs = new java.util.HashSet<>();

	/**
	 * Current target END tile of the back-and-forth lightning-phase run. Lane is
	 * picked by cycle (east for even cycles, west for odd); initial target is
	 * whichever end is closer (Chebyshev) to the player at spine-attack time
	 * (ties → north). Once {@link #lightningPhaseActive} is set, each arrival at
	 * this tile swaps it with {@link #lightningOtherEnd} to keep the player
	 * running end-to-end. Null until picked; cleared each cycle.
	 */
	private WorldPoint activeLightningEnd = null;

	/** The FAR tile of the lightning back-and-forth: the attack-cooldown-sized
	 *  stop short of the opposite end, NOT the opposite end itself. For a 4-tick
	 *  weapon on a 5-position lane, oscillation covers 3 position-steps = 6 tiles
	 *  (positions 1↔4 or 5↔2). For a 5-tick weapon, 4 position-steps = full lane.
	 *  Computed from {@link #activeLightningEnd} via {@link #lightningOscillationTileDist()}.
	 *  Swapped with activeLightningEnd each arrival. Null until picked. */
	private WorldPoint lightningOtherEnd = null;

	/** Guard so the "swap at center" trigger fires once per crossing (on the tick
	 *  we enter the center tile), not every tick we're standing on it. */
	private boolean wasOnLightningCenterLastTick = false;
	/** Tick on which we fired a lightning end shot. Deferring the end-swap one tick
	 *  prevents the planner's walk click (issued immediately after the swap) from
	 *  canceling the attack packet we just sent. -100 = inactive. */
	private int lightningEndAttackedAtTick = -100;

	/** Dedupe set for boulder projectiles -- same (startTick, id) hash as prayer swaps. */
	private final java.util.Set<Integer> seenBoulderProjectileIdentities = new java.util.HashSet<>();

	/**
	 * Latches true the first tick we observe {@link LeviathanAutoIDs#ANIM_PHASE2_END}
	 * (10287) on Leviathan. The blast is in-flight while this anim is active; the
	 * moment it clears to anything else, {@link #phase2EndResolved} flips and the
	 * planner then uses the dynamic PHASE2_END_TO_NEXT_SECTION lookup to route
	 * to the next cycle's start tile. Reset in start().
	 */
	private boolean phase2EndSignalSeen = false;

	/**
	 * Latches true the first tick we observe Leviathan's animation transition OUT
	 * of ANIM_PHASE2_END (10287). Gate for the walk to the phase-2 reset tile.
	 * Reset in start().
	 */
	private boolean phase2EndResolved = false;

	/**
	 * Next-section config, decided ONCE when phase 2 resolves and latched for the whole
	 * post-phase-2 walk. Routing and beginCycle both read this instead of recomputing
	 * nextSectionConfigFor() every tick -- the per-tick recompute flickered the target
	 * (e.g. the HP-finisher check or the lightning end snapping) and made us click one
	 * roar-start tile then a different one the next tick. Reset each cycle.
	 */
	private SectionConfig resolvedNextConfig = null;

	/**
	 * Cycle counter. 0 = round 1 (west column section-1, east spine, phase 2 classified
	 * by first roar). 1 = round 2 (east column, west spine, phase 2 = opposite of
	 * cycle 0). N = alternates column + spine side + phase 2 each round. Reset in start().
	 */
	private int cycleIndex = 0;

	/** Dedupe for the unmapped-phase-2-end warn log so it fires at most once per stall. */
	private boolean loggedUnmappedEnd = false;

	/**
	 * Running set of every TEMPLATE tile where we've observed a 2479 rock-drop
	 * graphics object. Each such tile becomes a permanent {@link
	 * LeviathanAutoIDs#GOBJ_ROCK} after landing, so this set is the full history of
	 * dropped-rock positions for the fight.
	 * <p>
	 * Why maintain a dedicated tracker when {@link #pushRockHazards(int)} already
	 * reads live rocks from the scene? Two reasons: (a) the 2479 telegraph fires a
	 * tick or two BEFORE the GameObject spawns, so we know a tile is about to be a
	 * permanent wall before it actually is; (b) line-of-sight / pathfinding logic in
	 * later sections needs the aggregated rock layout even if a particular rock
	 * briefly leaves the scene's render horizon.
	 * <p>
	 * TODO: use this set for section-2+ LOS checks when the planner needs to pick
	 * a tile with a clear line to Leviathan.
	 */
	private final java.util.Set<WorldPoint> trackedRockDropTemplates = new java.util.HashSet<>();

	// Walk-click dedupe.
	private WorldPoint lastWalkTarget;
	private int lastWalkTick = -100;

	// Handholds climb throttle (don't re-fire the menuAction every tick on the exit tile).
	private int lastHandholdsClimbTick = -100;

	// Overlay slot the plugin owns.
	private BaseOverlay overlay;

	@Inject
	public LeviathanAutoMain(
		Client client,
		MirrorState mirror,
		ClickRouter router,
		SchedulingTileDodger dodger,
		PrayerSwapper prayers,
		LootCollector lootCollector,
		ArenaCamera camera,
		GroundItemTracker groundItems)
	{
		this.client  = client;
		this.mirror  = mirror;
		this.router  = router;
		this.dodger  = dodger;
		this.prayers = prayers;
		this.lootCollector = lootCollector;
		this.camera = camera;
		this.groundItems = groundItems;
		this.decant = new DecantManager(client, LeviathanSupply.PROFILE);

		this.engine = new ConsumptionEngine(LeviathanSupply.PROFILE, mirror);
		this.bankRun = new BankRun(client, mirror, router, LeviathanSupply.BANK);

		this.phase = new PhaseTracker<>(LevPhase.OUT_OF_FIGHT)
			.on(LevPhase.FIGHT, () -> leviathan != null && isLeviathanActive(),
				"leviathan alive, active markers present")
			.on(LevPhase.IDLE,  () -> leviathan != null && !isLeviathanActive(),
				"leviathan present but dormant")
			.on(LevPhase.OUT_OF_FIGHT, () -> leviathan == null,
				"no boss");
	}

	public void bindOverlay(BaseOverlay o) { this.overlay = o; }

	public boolean   isActive()     { return active; }
	public void      start()
	{
		active = true;
		leviathanWasPresent = false;
		lastCamCenter = null;
		decant.reset();
		resetFightState();
		// WIP banking: starting the bot while standing in the POH means "go bank" -- kick off the
		// POH->pool->nexus->booth travel run. Starting anywhere else is a normal (combat) start.
		bankRun.reset();
		if (bankRun.inPoh()) bankRun.begin();
	}

	/**
	 * Reset ALL per-fight state to the round-1 bootstrap. Called by {@link #start()}
	 * and on Leviathan death/despawn, so the next kill begins clean and the planner
	 * routes the player back to the home tile ({@link LeviathanAutoIDs#START_TILE_TEMPLATE}).
	 */
	private void resetFightState()
	{
		deathPrayersOff = false;
		// Leviathan is dead/despawned -- dump every falling hazard we measured this session
		// and its observed time-to-impact (max spawn->last-seen ticks; impact ~= that + 1).
		if (!observedImpactTicksById.isEmpty())
		{
			for (java.util.Map.Entry<Integer,Integer> e : observedImpactTicksById.entrySet())
			{
			}
		}
		rangingBoost.reset();
		activeSectionConfig = ROUND_1_CONFIG;
		section1AnchorTemplate = activeSectionConfig.startTile();
		lastSouthStepTick = -100;
		trackedRockDropTemplates.clear();
		section2Phase = Section2Phase.UNKNOWN;
		enragePhaseActive = false;
		eagleEyeEnrageLatched = false;
		lastKnownBossHp = -1;
		enrageQuadrant = EnrageQuadrant.UNKNOWN;
		abyssalPathfinder = null;
		enrageApproachTargetTemplate = null;
		shadowRushCast = false;
		shadowRushCastConfirmed = false;
		spineAttackDone = false;
		boulderPhaseActive = false;
		lightningPhaseActive = false;
		lightningChargeMsgTick = -100;
		boulderProjectileCount = 0;
		seenBoulderProjectileIdentities.clear();
		activeBoulderCluster = null;
		firedBoulderAttackIdxs.clear();
		boulderBackfillAttackDone = false;
		boulderWalkbackAttackDone = false;
		activeLightningEnd = null;
		lightningOtherEnd = null;
		wasOnLightningCenterLastTick = false;
		lightningEndAttackedAtTick = -100;
		usedBoulderClusterIdxs.clear();
		phase2EndSignalSeen = false;
		phase2EndResolved = false;
		resolvedNextConfig = null;
		cycleIndex = 0;
		loggedUnmappedEnd = false;
		section1RoarCount = 0;
		section1RoarTarget = LeviathanAutoIDs.TOTAL_SECTION_1_ROARS;
		shadowRushUses = 0;
		scriptedRoarPath = null;
		scriptedRoarIdx = 0;
		scheduledPrayIdentities.clear();
		lastWalkTarget = null;
		lastWalkTick = -100;
	}
	public void      stop()         { camera.reset(); decant.reset(); bankRun.reset(); active = false; lastCamCenter = null; }
	public LevPhase  currentPhase() { return phase.current(); }
	public TripState trip()         { return trip; }

	// -----------------------------------------------------------------
	// Tick loop.
	// -----------------------------------------------------------------

	/**
	 * Fed from the plugin's ChatMessage subscriber. Records {@link #lightningChargeMsgTick}
	 * when Leviathan's "charges up a lightning attack" message appears; onGameTick then
	 * starts the oscillation (gated to the post-Shadow-Rush lightning window).
	 */
	public void onChatMessage(String message)
	{
		if (message == null) return;
		if (message.toLowerCase().contains("the leviathan charges up a lightning attack..."))
		{
			lightningChargeMsgTick = mirror.getTickCounter();
		}
	}

	/** Frame the arena camera: detached overhead view centred on ARENA_CENTER_TEMPLATE (2081,6372) at zoom
	 *  350. Applied once per instance (re-applies if the resolved tile changes); does not fight manual
	 *  nudges within an instance, and only while we are in the Leviathan arena region. */
	private void applyArenaCamera()
	{
		WorldPoint tpl = InstanceCoords.toTemplate(client, mirror.getPlayerTile());
		if (tpl == null || tpl.getRegionID() != LeviathanAutoIDs.ARENA_REGION) return;   // only in the arena
		WorldPoint centre = InstanceCoords.fromTemplate(client, LeviathanAutoIDs.ARENA_CENTER_TEMPLATE);
		if (centre == null || centre.equals(lastCamCenter)) return;
		camera.overhead(centre, LeviathanAutoIDs.ARENA_CAMERA_ZOOM);
		lastCamCenter = centre;
	}

	public void onGameTick()
	{
		int tick = mirror.getTickCounter();

		// WIP banking: while a bank run is travelling it OWNS the tick -- skip all combat logic. Once it
		// reaches the open bank (READY) it stops owning the tick; there is no restock/return leg yet, so
		// we just idle at the open bank until F7 stops the bot.
		if (bankRun.active())
		{
			bankRun.step(tick);
			if (overlay != null) overlay.setStep(bankRun.label());
			return;
		}
		if (bankRun.bankReady())
		{
			if (overlay != null) overlay.setStep(bankRun.label());
			return;
		}

		leviathan = findLeviathan();
		int bhpNow = bossCurrentHp();
		if (bhpNow >= 0) lastKnownBossHp = bhpNow;   // cache while readable; used when the bar is hidden
		if (leviathanWasPresent && leviathan == null)
		{
			// Death / despawn: full reset so the next kill starts clean from the home
			// tile. Rocks disappear when Leviathan dies, so the whole per-fight state
			// (clusters used, rock tracker, cycle index, phase flags) is stale.
			resetFightState();
			// Backstop prayers-off on despawn (the HP-0 check below is the primary, earlier
			// trigger). resetFightState cleared deathPrayersOff, so this re-disables cleanly.
			turnPrayersOff(tick);
			// Loot-arm backstop: if the HP-0 read was missed, the loot pass never got armed. Anchor it at
			// despawn with a lookback so the GroundItemTracker still covers the drops that landed at the kill.
			if (lastDeathTick < 0) lastDeathTick = client.getTickCount() - LEVIATHAN_LOOT_LOOKBACK;
		}
		leviathanWasPresent = (leviathan != null);

		// Primary death trigger: the boss HP readout hits 0 at the kill, a few ticks before the
		// NPC despawns. Turn prayers off immediately (once) so we stop draining during the death
		// animation. deathPrayersOff also gates watchPrayerProjectiles so a last in-flight shot
		// can't flick an overhead back on.
		// Primary: HP bar reads 0. Fallback: Leviathan's death animation (10293) -- in case the HP bar
		// hides without a clean 0 read, we still drop prayers at death.
		if (!deathPrayersOff && leviathan != null
			&& (bossCurrentHp() == 0 || leviathan.getAnimation() == LeviathanAutoIDs.ANIM_DEATH))
		{
			// Kill confirmed. Reset ALL per-fight state NOW -- phase-2 sub-phase (boulder/lightning), cycle
			// index, section-1 progression, rock tracker -- so the NEXT kill in a multi-kill trip starts clean.
			// The despawn-time reset doesn't reliably fire between back-to-back kills, which left phase 2 carrying
			// over (e.g. starting on boulder because the previous fight ended on boulder). resetFightState leaves
			// lastDeathTick untouched and clears deathPrayersOff, so re-arm both AFTER it.
			resetFightState();
			deathPrayersOff = true;
			lastDeathTick = client.getTickCount();   // anchor the loot pass on the tracker's clock
			turnPrayersOff(tick);
		}
		phase.update(tick);

		// Arena exit: if we're parked on the Handholds climb tile (2067,6370), climb out.
		// Fires the captured "Climb" menuAction and skips the rest of the fight logic.
		if (climbHandholdsIfAtExitTile(tick)) return;

		// Clear + re-project hazards every tick so stale projections never poison
		// decisions. (Same pattern as Vardorvis.)
		dodger.clearAll();
		pushHazards(tick);

		// Eager phase2 classification -- only in cycle 0 where section2Phase hasn't
		// been forced by beginCycle. In cycles 1+ the phase alternates deterministically.
		if (cycleIndex == 0 && section2Phase == Section2Phase.UNKNOWN) classifyPhase();

		// Boulder cluster pick: once section2Phase=BOULDER and spine attack has fired,
		// snapshot the player's position and lock in the nearest cluster.
		pickBoulderClusterIfNeeded();
		// Lightning end pick: ditto for LIGHTNING -- picks the closer end of the
		// cycle-side lane as the initial back-and-forth target.
		pickLightningEndIfNeeded();
		// Enrage phase: triggered by GFX_ENRAGE_PLUS (2298) appearing in a + shape.
		// Resolves the pathfinder's quadrant and stashes the pathfinder NPC ref.
		detectEnragePhaseIfNeeded(tick);

		// Lightning back-and-forth: swap target each time the player CROSSES the
		// lane center (position 3 of the 5-position lane). The player never actually
		// reaches the oscillation ends -- they U-turn at center. Attack maintenance
		// fires naturally mid-run on each cooldown (attack suppression is lifted
		// below), so the shot lands at or near center on each 4-tick cycle.
		if (lightningPhaseActive && activeLightningEnd != null && lightningOtherEnd != null)
		{
			WorldPoint centerTpl = currentLightningLaneCenter();
			WorldPoint me = mirror.getPlayerTile();
			WorldPoint centerInstance = centerTpl == null ? null
				: InstanceCoords.fromTemplate(client, centerTpl);
			boolean onCenter = me != null && centerInstance != null && me.equals(centerInstance);
			wasOnLightningCenterLastTick = onCenter;

			WorldPoint activeEndInstance = InstanceCoords.fromTemplate(client, activeLightningEnd);
			boolean atEnd = activeEndInstance != null && me != null && me.equals(activeEndInstance);
			// DIAG: every tick while lightning phase is active, dump the state so we
			// can tell exactly what branch (swap vs fire vs hold vs skip) ran.

			// Deferred swap -- runs REGARDLESS of atEnd. On the west lane the server
			// walks us 2 tiles north off the clicked endpoint for the attack-NPC
			// interaction tile, so by the time we're on the "swap tick" we are no
			// longer standing on activeEnd. Gating this on atEnd starved the swap and
			// locked us to one endpoint forever.
			if (lightningEndAttackedAtTick == tick - 1)
			{
				WorldPoint prev = activeLightningEnd;
				activeLightningEnd = lightningOtherEnd;
				lightningOtherEnd = prev;
				lightningEndAttackedAtTick = -100;
			}
			else if (atEnd)
			{
				if (leviathan != null && canAttack(tick))
				{
					// Fire the shot this tick. Do NOT swap yet -- planNextTile would then
					// walk us toward the opposite end on this very tick, and that walk click
					// cancels the attack packet. Defer the swap to the next tick (above).
					router.attackNpc(leviathan);
					lastAttackTick = tick;
					lightningEndAttackedAtTick = tick;
				}
				else
				{
					// Cooldown not ready yet. Don't swap -- stay on this end so planNextTile
					// keeps targeting it (same tile, no new walk click) and we re-check next tick.
				}
			}
		}

		// Shadow Rush cast-confirmation latch. Once shadowRushCast is set, watch for
		// the PLAYER's own cast animation (ANIM_CAST_SHADOW_RUSH, 10092). This fires
		// on the cast tick itself -- no projectile-travel delay like the on-Leviathan
		// impact spotanim would introduce. First sighting latches
		// shadowRushCastConfirmed, which gates the spine-tile routing.
		if (shadowRushCast && !shadowRushCastConfirmed
		    && mirror.getPlayerAnim() == LeviathanAutoIDs.ANIM_CAST_SHADOW_RUSH)
		{
			shadowRushCastConfirmed = true;
			shadowRushUses++;
		}

		// Boulder-phase detection. Latch on ANIM 10288 (one-shot phase start), then
		// count distinct boulder projectiles for the dodge sequence.
		if (section2Phase == Section2Phase.BOULDER && !boulderPhaseActive && leviathan != null
		    && leviathan.getAnimation() == LeviathanAutoIDs.ANIM_BOULDER_PHASE_START)
		{
			boulderPhaseActive = true;
		}
		if (boulderPhaseActive)
		{
			countBoulderProjectiles(tick);
			tryFireBoulderAttack(tick);
		}

		// Lightning-phase activation: start the oscillation once Leviathan's chat message
		// "The Leviathan charges up a lightning attack..." is seen (lightningChargeMsgTick,
		// set in onChatMessage), plus LIGHTNING_CHARGE_TO_OSC_TICKS. GATED to the post-
		// Shadow-Rush lightning window so a stray match can't start it during the roar phase.
		if (section2Phase == Section2Phase.LIGHTNING && !lightningPhaseActive && leviathan != null
		    && shadowRushCast && spineAttackDone
		    && lightningChargeMsgTick >= 0 && tick >= lightningChargeMsgTick + LIGHTNING_CHARGE_TO_OSC_TICKS)
		{
			lightningPhaseActive = true;
			// Kick off the oscillation. Right now the player is standing on
			// activeLightningEnd (the initial pick, closer end). Swap targets so the
			// planner routes toward lightningOtherEnd (the far oscillation tile) --
			// otherwise planNextTile returns the player's own tile each tick and we
			// stand still forever.
			if (activeLightningEnd != null && lightningOtherEnd != null)
			{
				WorldPoint prev = activeLightningEnd;
				activeLightningEnd = lightningOtherEnd;
				lightningOtherEnd = prev;
			}
		}

		// Shared phase-2 end detection. ANIM_PHASE2_END (10287) is the "attack ending"
		// animation for BOTH boulder (smoke blast) and lightning (stop) finishers.
		// Latch phase2EndSignalSeen on first sighting while a phase-2 is active, then
		// phase2EndResolved when the animation transitions OUT of 10287.
		// Guard includes phase2EndSignalSeen so resolution still latches after we clear
		// the phase-active flags on the signal tick (below).
		if ((boulderPhaseActive || lightningPhaseActive || phase2EndSignalSeen) && leviathan != null)
		{
			int lanim = leviathan.getAnimation();
			if (!phase2EndSignalSeen && lanim == LeviathanAutoIDs.ANIM_PHASE2_END)
			{
				phase2EndSignalSeen = true;
				// STOP the lightning oscillation immediately on the end signal. It is fine
				// to abandon mid-oscillation without reaching the far end -- the planner
				// now holds, then routes to the next section once the end resolves.
				lightningPhaseActive = false;
				lightningChargeMsgTick = -100;
			}
			else if (phase2EndSignalSeen && !phase2EndResolved && lanim != LeviathanAutoIDs.ANIM_PHASE2_END)
			{
				phase2EndResolved = true;
				// Decide the next section ONCE, now, and latch it. activeLightningEnd is
				// frozen (oscillation stopped) and HP is read a single time, so the target
				// can't flicker during the walk to it.
				resolvedNextConfig = nextSectionConfigFor(currentPhase2EndTile());
			}
		}

		// Cycle transition: once phase2EndResolved, currentPhase2EndTile() returns
		// the end tile (boulder cluster backfill OR activeLightningEnd); look it up in
		// PHASE2_END_TO_NEXT_SECTION and route to the next cycle. The
		// player must already be standing on the next config's start tile (routing
		// via postSpineTargetTemplate() walks them there) before we flip cycles.
		WorldPoint phase2End = currentPhase2EndTile();
		if (phase2End != null)
		{
			SectionConfig next = resolvedNextConfig;
			if (next == null)
			{
				if (!loggedUnmappedEnd)
				{
					loggedUnmappedEnd = true;
				}
			}
			else
			{
				WorldPoint me = mirror.getPlayerTile();
				WorldPoint nextStartInstance = InstanceCoords.fromTemplate(client, next.startTile());
				if (me != null && nextStartInstance != null && me.equals(nextStartInstance))
				{
					beginCycle(cycleIndex + 1, next);
				}
			}
		}

		dodger.onGameTick(tick);
		prayers.onGameTick(tick);

		// Eagle Eye: ON during DPS windows (section-1 roars, boulder, the post-Shadow-Rush back-hit) and
		// latched ON through the enrage once we reach the pathfinder 3x3; OFF during lightning, the shadow-
		// stun/approach, movement between windows, and outside the fight. EAGLE_EYE is not an overhead, so
		// this never fights the projectile-driven protect swaps.
		if (enragePhaseActive && isInsidePathfinderFootprint()) eagleEyeEnrageLatched = true;
		if (wantEagleEye())
		{
			prayers.ensureOn(tick, Prayer.EAGLE_EYE);
		}
		else
		{
			prayers.ensureOnlyOn(tick, java.util.Collections.emptySet(),
				java.util.Collections.singleton(Prayer.EAGLE_EYE));
		}

		// Prayers are driven entirely by watchPrayerProjectiles (called from pushHazards):
		// pray against any prayable projectile in flight, overheads OFF whenever there's
		// nothing to pray against. No phase-specific camp here -- the shadow-stun window
		// turns off naturally (Leviathan isn't throwing prayable projectiles then).
		updateOverlay();

		if (!active) return;

		applyArenaCamera();   // detached overhead framing; no-op once framed for this instance

		// POST-DEATH LOOT: drain the drop pile (richest first, what fits) before anything else. Holds the tick
		// while collecting; clears itself once the pile is gone / nothing fits / it times out.
		if (lastDeathTick >= 0)
		{
			String lootStatus = lootCollector.collect(router, lastDeathTick, null, true);  // includeAllTracked: never abandon leftover loot
			if (lootStatus != null)
			{
				// Full inventory with nothing that fits: the pass would just hold on the pile until the loot
				// timeout. Free a slot via decant/make-room (consolidate partials, drop vials, make room) so the
				// NEXT tick can pick the drop up. Skip when collect just issued a take-click this tick (a stackable
				// we already hold can be grabbed even at 28 slots) so we don't double-click. DecantManager self-
				// gates make-room to "only when full".
				if (inventoryFull() && !lootStatus.startsWith("loot: take")) decant.tick(router, groundItems, tick, true);
				return;
			}
			lastDeathTick = -1;
		}

		// BETWEEN-FIGHTS, ON THE MOVE: once Leviathan is dead, run back to the start tile (the movement block
		// below does that) while tidying up EN ROUTE. Fire ONE non-blocking action per tick -- top off HP/
		// prayer/stamina first, else decant (drop empty vials + consolidate partial potions). We do NOT
		// return, so the walk home still fires this tick. Make-room is disabled (false) so decant never
		// walks us off the route to chase a floor potion.
		if (leviathan == null)
		{
			if (!cleanupTopOffTick(tick)) decant.tick(router, groundItems, tick, false);
		}

		// Movement planning.
		resolveInstanceTilesIfNeeded(tick);
		advanceSection1AnchorIfNeeded(tick);
		WorldPoint me = mirror.getPlayerTile();

		// DEBUG: pause once we reach EAST_NORTH_LIGHTNING so section-2 mechanics can be
		// observed by hand. Idempotent -- stop() flips active off and the next tick's
		// early-return skips planning entirely.
		if (DEBUG_PAUSE_AT_POSTCAST && shadowRushCast && me != null)
		{
			WorldPoint destTpl = currentSection2TargetTemplate();
			WorldPoint destInstance = destTpl == null ? null : InstanceCoords.fromTemplate(client, destTpl);
			if (destInstance != null && me.equals(destInstance))
			{
				if (overlay != null) overlay.setStep("DEBUG paused @ " + destTpl);
				stop();
				return;
			}
		}

		if (me != null)
		{
			WorldPoint step = planNextTile(me, tick);
			// Action priority each tick: correct prayer (free, already done above) > falling-rock
			// dodge > movement > attack. The DPS interleave below lets an attack take THIS tick in
			// place of a walk -- but only for safe travel movement. If staying put would put us on
			// a live hazard at tick+1/+2, the walk is a DODGE and must never be skipped for a shot.
			boolean currentDangerSoon = dodger.isTileDangerousAt(me, tick + 1)
				|| dodger.isTileDangerousAt(me, tick + 2);
			// Post-phase-2 transit DPS: while walking to the next section after phase 2
			// has resolved, skip THIS tick's walk when the weapon is off cooldown so the
			// attack maintenance below fires. The walk resumes next tick (cooldown
			// blocked), so we still reach the next tile -- just landing shots on the way.
			// Hit-and-run: interleave attacks with the walk while repositioning but safe this tick -- the
			// post-phase-2 transit (as before) AND section 1 (if Leviathan spawned before we reached the start
			// tile, we run there while landing shots). !currentDangerSoon keeps a dodge-walk from being traded
			// for a shot.
			boolean section1Run = phase.current() == LevPhase.FIGHT && !shadowRushCast && !enragePhaseActive;
			boolean attackInsteadOfWalk = (phase2EndResolved || section1Run) && leviathan != null
				&& canAttack(tick) && !shouldSuppressAttack() && !currentDangerSoon;
			if (step != null && !step.equals(me) && !attackInsteadOfWalk)
			{
				// Hard accessibility gate: never issue a walk click onto an inaccessible
				// tile (arena boundary or Leviathan hitbox). The planner shouldn't produce
				// one, but this is the last line of defense -- a bad click outside the
				// arena is wasted and can desync the sequence.
				WorldPoint stepTpl = InstanceCoords.toTemplate(client, step);
				if (stepTpl != null && LeviathanAutoIDs.isInaccessibleTemplate(stepTpl))
				{
					return;
				}
				// Distance-aware re-click dedupe. The old flat 3-tick window spammed
				// walkTo mid-trip on long traversals (e.g. ~8 run-ticks from the cast
				// tile to EAST_CENTER_LIGHTNING would re-click twice). Use the estimated
				// travel time instead: Chebyshev tiles from the click origin to the
				// target, with a minimum of 3 ticks so a dropped click eventually retries.
				// Shadow-Rush STUN window: boss is stunned, so top off MID-RUN if needed. Eat this
				// tick, then force the destination re-click below so the run resumes after the eat:
				// cast rush -> click dest -> eat if needed -> click dest again.
				boolean ateThisTick = false;
				if (eatPhaseAllowed())
				{
					ateThisTick = doConsumption(tick, true);
				}
				int dx = Math.abs(step.getX() - me.getX());
				int dy = Math.abs(step.getY() - me.getY());
				int travelTicks = Math.max(3, Math.max(dx, dy));
				// During the post-phase-2 transit we interleave attacks with the walk, and
				// each attack interrupts movement -- so re-click the destination EVERY
				// non-attack tick (no dedupe) to keep walking between attack cooldowns.
				// Outside transit, the distance-aware dedupe avoids spamming walkTo mid-trip.
				// Force a re-click (bypass dedupe) on a tick we ate, so the walk resumes after the eat.
				boolean sameTarget = !phase2EndResolved && !ateThisTick
					&& step.equals(lastWalkTarget) && tick - lastWalkTick <= travelTicks;
				if (!sameTarget)
				{
					router.walkTo(step);
					lastWalkTarget = step;
					lastWalkTick = tick;
					if (overlay != null) overlay.setStep("walk -> " + step);
				}
				else
				{
				}
				return;
			}
			if (step == null) { lastWalkTarget = null; }
		}

		// Supply consumption for a PARKED tick (not walking). Mid-run eating during the
		// Shadow-Rush stun window is handled inside the walk branch above.
		doConsumption(tick);

		// Attack / cast maintenance. The Shadow Rush path is a MANUAL SPELL CAST and
		// is NOT gated by canAttack(tick) -- in OSRS a manual spell click interrupts
		// the weapon attack queue and starts a fresh cast cycle, so a lingering ranged
		// cooldown from the final section-1 shot must not delay the transition. The
		// ranged branches (section 1 south-stepping, section 2 back-hits) stay gated.
		if (phase.current() == LevPhase.FIGHT)
		{
			if (isSection1Complete() && !shadowRushCast && !enragePhaseActive)
			{
				// One-shot: cast Shadow Rush. section2Phase was pinned earlier in the
				// tick loop (eager classification); worst case still UNKNOWN here, which
				// the overlay reports honestly. lastAttackTick is bumped so the first
				// post-cast ranged shot waits the normal weapon cooldown from the cast tick.
				router.castSpellOnNpc(
					leviathan,
					LeviathanAutoIDs.SPELL_SHADOW_RUSH_WIDGET,
					LeviathanAutoIDs.SPELL_SHADOW_RUSH_NAME);
				shadowRushCast = true;
				lastAttackTick = tick;
				// Do NOT force overheads off here. Prayers are projectile-driven
				// (watchPrayerProjectiles): if Leviathan still has projectiles in flight
				// when we cast, we keep praying them; once the shadow stun halts his
				// attacks and nothing is live, overheads drop off on their own.
				if (overlay != null) overlay.setStep("cast Shadow Rush (next: " + section2Phase + ")");
			}
			else if (canAttack(tick) && !shouldSuppressAttack())
			{
				// Section 1: south-stepping ranged auto. Section 2 pre-spine: suppressed
				// until we land on the spine tile -- firing mid-trip would interrupt the
				// walk. During lightning back-and-forth: NOT suppressed -- the whole
				// mechanic is attacking while running end to end on each cooldown.
				router.attackNpc(leviathan);
				lastAttackTick = tick;
				String stepLabel;
				if (!shadowRushCast)              stepLabel = "attack";
				else if (!spineAttackDone)        stepLabel = "attack (spine)";
				else                              stepLabel = "attack (phase2=" + section2Phase + ")";
				if (overlay != null) overlay.setStep(stepLabel);
				if (shadowRushCast && shadowRushCastConfirmed && !spineAttackDone)
				{
					spineAttackDone = true;
				}
			}
		}
	}

	/**
	 * Section 1 is complete once we've absorbed {@link LeviathanAutoIDs#TOTAL_SECTION_1_ROARS}
	 * roars. Counting roars (not the anchor's Y coord) means every LOS-skip the
	 * anchor takes mid-section is still a single roar, so the one-shot Shadow Rush
	 * fires on the correct tick regardless of how many tiles the anchor skipped.
	 */
	private boolean isSection1Complete()
	{
		return section1RoarCount >= section1RoarTarget;
	}

	/**
	 * Classify which section-2 phase this kill will enter by looking at where the
	 * first roar's rocks landed. Both patches are POSITIVE signals:
	 * <ul>
	 *   <li>Any tile in {@link #trackedRockDropTemplates} within the N or S 5x5
	 *       patch -> {@link Section2Phase#BOULDER}.</li>
	 *   <li>Any tile within the E patch -> {@link Section2Phase#LIGHTNING}.</li>
	 * </ul>
	 * The W patch is intentionally excluded: it overlaps our south-step column, so
	 * a W-pattern rock is indistinguishable from an on-player south-step trigger and
	 * yields no information. The E patch is the mirror of W and is far from us, so
	 * it's the unambiguous lightning marker.
	 * <p>
	 * Returns {@link Section2Phase#UNKNOWN} (without caching) if no conclusive rock
	 * has been seen yet -- caller is expected to retry each tick while UNKNOWN.
	 * Classification is one-shot: once set to BOULDER or LIGHTNING it never flips.
	 */
	private Section2Phase classifyPhase()
	{
		if (section2Phase != Section2Phase.UNKNOWN) return section2Phase;

		for (WorldPoint tpl : trackedRockDropTemplates)
		{
			if (isWithinRadius(tpl, LeviathanAutoIDs.PHASE_N_CENTER_TEMPLATE, LeviathanAutoIDs.PHASE_PATCH_RADIUS)
				|| isWithinRadius(tpl, LeviathanAutoIDs.PHASE_S_CENTER_TEMPLATE, LeviathanAutoIDs.PHASE_PATCH_RADIUS))
			{
				section2Phase = Section2Phase.BOULDER;
				return section2Phase;
			}
			if (isWithinRadius(tpl, LeviathanAutoIDs.PHASE_E_CENTER_TEMPLATE, LeviathanAutoIDs.PHASE_PATCH_RADIUS))
			{
				section2Phase = Section2Phase.LIGHTNING;
				return section2Phase;
			}
		}

		return Section2Phase.UNKNOWN;
	}

	/** Chebyshev-radius containment test. */
	private static boolean isWithinRadius(WorldPoint point, WorldPoint center, int radius)
	{
		if (point == null || center == null) return false;
		return Math.abs(point.getX() - center.getX()) <= radius
			&& Math.abs(point.getY() - center.getY()) <= radius
			&& point.getPlane() == center.getPlane();
	}

	// -----------------------------------------------------------------
	// Boss discovery / phase helpers.
	// -----------------------------------------------------------------

	private NPC findLeviathan()
	{
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return null;
		for (NPC n : npcs)
		{
			if (n == null) continue;
			int id = n.getId();
			if (id == LeviathanAutoIDs.NPC_LEVIATHAN
				|| id == LeviathanAutoIDs.NPC_LEVIATHAN_AWAKENED)
			{
				return n;
			}
		}
		return null;
	}

	/**
	 * True while Leviathan is actively fighting (not post-death idle). TODO: refine
	 * once we identify the active-state marker (gameobject, animation, varbit, etc).
	 * Default: boss is present therefore active.
	 */
	private boolean isLeviathanActive()
	{
		return leviathan != null;
	}

	// -----------------------------------------------------------------
	// Hazards.
	// -----------------------------------------------------------------

	/**
	 * Push hazards for the tick. For Leviathan this is PRIMARILY the three-prayer
	 * rotation: Leviathan's melee / range / magic projectiles each have a prayer that
	 * must be ON before impact. The projectiles speed up over the course of a kill
	 * (minimum reaction window is ONE tick), so there is NO humanizer and NO pre-pray
	 * lead-in like Vardorvis uses. We swap the instant we see the projectile.
	 * <p>
	 * Pool / spike / chain hazards go in here too once we have IDs for them.
	 */
	private void pushHazards(int tick)
	{
		watchPrayerProjectiles(tick);
		pushGraphicsHazards(tick);
		pushRockHazards(tick);
	}

	/**
	 * Scan {@link Client#getGraphicsObjects()} for Leviathan's ground hazards. All three
	 * (2475, 2476, 2479) mark their tile as damaging for a short active window. 2479 is
	 * special: when it lands, the game spawns a persistent {@link LeviathanAutoIDs#GOBJ_ROCK}
	 * on the same tile. The rock is handled by {@link #pushRockHazards(int)} once it
	 * actually appears -- here we just mark the drop tile as dangerous.
	 */
	private void pushGraphicsHazards(int tick)
	{
		Iterable<GraphicsObject> gfx = client.getGraphicsObjects();
		if (gfx == null) return;
		for (GraphicsObject g : gfx)
		{
			if (g == null) continue;
			int id = g.getId();
			boolean breakable   = LeviathanAutoIDs.isBreakableRockGfx(id);
			boolean unbreakable = LeviathanAutoIDs.isUnbreakableRockGfx(id);
			if (!breakable && !unbreakable) continue;

			LocalPoint lp = g.getLocation();
			if (lp == null) continue;
			// Use fromLocal (NOT fromLocalInstance) -- we want INSTANCE coords here so
			// the hazard tile matches mirror.getPlayerTile() (which is also INSTANCE
			// via player.getWorldLocation). fromLocalInstance returns TEMPLATE which
			// would never equal the player's INSTANCE tile.
			WorldPoint tileInstance = WorldPoint.fromLocal(client, lp);
			if (tileInstance == null) continue;

			String label = unbreakable ? "rock-drop" : "breakable-rock";
			// Falling hazard: the GFX on the tile is the FALL, not the impact. Register it with
			// its per-id time-to-impact via the generic TelegraphedHazards helper, which marks
			// the tile dangerous ONLY at the computed impact tick (ROCK_IMPACT_ACTIVE_TICKS wide,
			// default 1) -- not during the fall, and not after it has landed. The impact tick is
			// derived from the spotanim's start cycle, so it is stable across re-scans and goes
			// SAFE on its own once impact has passed.
			int tti = LeviathanAutoIDs.rockFallToImpactTicks(id);
			// markWholeFall=true: flag the tile dangerous for the WHOLE fall (from when we see the
			// GFX through impact), so we ALWAYS dodge a rock in the air toward it even if the GFX
			// rendered late. ROCK_IMPACT_ACTIVE_TICKS adds a small post-impact margin for landing slop.
			net.runelite.client.plugins.autoBossCore.combat.TelegraphedHazards.addFalling(
				dodger, tick, client.getGameCycle(), tileInstance, g.getStartCycle(),
				tti, LeviathanAutoIDs.ROCK_IMPACT_ACTIVE_TICKS, true, label);
			// Record this hazard's observed time-to-impact: max elapsed ticks from its spawn
			// cycle while the spotanim is still visible. The max across a hazard's lifetime is
			// the spawn->last-seen duration (impact lands ~1 tick later, when it despawns).
			int elapsedTicks = Math.floorDiv(client.getGameCycle() - g.getStartCycle(), 30);
			observedImpactTicksById.merge(id, elapsedTicks, Integer::max);

			// Permanence tracking: unbreakable drops (2477/2478/2479/2480) become
			// persistent GOBJ_ROCK (47590). Record their TEMPLATE tiles so later-
			// section logic (LOS, kiting, wall planning) can reference the full
			// history. Breakable rocks (2475/2476) are transient so we don't track
			// them here.
			if (unbreakable)
			{
				WorldPoint tpl = InstanceCoords.toTemplate(client, tileInstance);
				if (tpl != null) trackedRockDropTemplates.add(tpl);
			}
		}
	}

	/**
	 * Flag every known permanent-rock tile as a hazard. Primary source is
	 * {@link #trackedRockDropTemplates}: once we observe an unbreakable rock-drop GFX,
	 * the tile is permanently blocked for the rest of the fight regardless of what
	 * RuneLite's scene query returns (the rock may render as a Ground/Decorative/Wall
	 * object rather than a GameObject, so {@link Tile#getGameObjects()} alone misses
	 * it). The scene-GameObject scan is a belt-and-suspenders secondary.
	 */
	private void pushRockHazards(int tick)
	{
		// Primary: historical rock-drop templates. These are definitive because we
		// recorded them from GraphicsObjectCreated events; the tiles stay blocked for
		// the whole fight.
		for (WorldPoint tplRock : trackedRockDropTemplates)
		{
			WorldPoint instance = InstanceCoords.fromTemplate(client, tplRock);
			if (instance == null) continue;
			for (int dt = 0; dt < 5; dt++)
			{
				dodger.addHazard(Hazard.staticTile(instance, tick + dt, "rock-tracked"));
			}
		}

		// Secondary: scene scan for GOBJ_ROCK as a GameObject, in case pre-existing
		// static rock scenery is present in the arena. If the rock is a different
		// object type (ground / decorative / wall) this misses it, but the tracker
		// above covers everything dropped during the fight. Tiles come back in INSTANCE
		// coords so they match the player's getWorldLocation.
		for (WorldPoint wp : SceneScanner.tiles(client, LeviathanAutoIDs.GOBJ_ROCK))
		{
			for (int dt = 0; dt < 5; dt++)
			{
				dodger.addHazard(Hazard.staticTile(wp, tick + dt, "rock-scene"));
			}
		}
	}

	/**
	 * If the player is standing on one of {@link LeviathanAutoIDs#HANDHOLDS_CLIMB_TILES_TEMPLATE}
	 * (2067,6370,0) or (2069,6368,0), fire the "Climb" menuAction on the {@link LeviathanAutoIDs#GOBJ_HANDHOLDS}
	 * (id 47592). Reproduces the captured human click
	 * {@code GAME_OBJECT_FIRST_OPTION id=47592 itemId=-1 param0=54 param1=48 option='Climb'
	 * target='<col=ffff>Handholds'}.
	 * <p>
	 * param0/param1 are instance-relative SCENE coords: we derive them live from the
	 * Handholds object in the loaded scene (so they stay correct across instances), and
	 * only fall back to the captured 54/48 if the object isn't in the scene this tick.
	 * Throttled to once every 3 ticks so standing on the tile doesn't spam the action.
	 *
	 * @return true if the climb was issued this tick.
	 */
	private boolean climbHandholdsIfAtExitTile(int tick)
	{
		if (!active) return false;  // never act unless the script is started
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return false;
		WorldPoint tpl = InstanceCoords.toTemplate(client, me);
		if (tpl == null) return false;
		boolean onExitTile = false;
		for (WorldPoint exit : LeviathanAutoIDs.HANDHOLDS_CLIMB_TILES_TEMPLATE)
		{
			if (tpl.equals(exit)) { onExitTile = true; break; }
		}
		if (!onExitTile) return false;
		// We ARE on a handholds tile. If we issued the climb within the last few ticks,
		// HOLD this tick (return true -> skip the rest of onGameTick) instead of handing
		// control to the planner. Otherwise the planner would click the start tile while
		// we're still on the handholds mid-climb -- interrupting the climb / firing a
		// stray click. We keep holding (and re-issue the climb once the window lapses)
		// until the climb moves us OFF the handholds; then onExitTile is false and normal
		// in-arena routing resumes.
		if (tick - lastHandholdsClimbTick < 3) return true;

		// Derive the scene coords live from the Handholds object (robust to instance
		// scene-base shifts); fall back to the captured 54/48 if not found this tick.
		int sceneX = 54, sceneY = 48;
		boolean found = false;
		GameObject handholds = SceneScanner.firstById(client, LeviathanAutoIDs.GOBJ_HANDHOLDS);
		if (handholds != null)
		{
			net.runelite.api.Point sceneMin = handholds.getSceneMinLocation();
			if (sceneMin != null)
			{
				sceneX = sceneMin.getX();
				sceneY = sceneMin.getY();
				found = true;
			}
		}

		router.gameObjectAction(sceneX, sceneY, MenuAction.GAME_OBJECT_FIRST_OPTION,
			LeviathanAutoIDs.GOBJ_HANDHOLDS, "Climb", "<col=ffff>Handholds");
		lastHandholdsClimbTick = tick;
		return true;
	}

	/**
	 * Scan this tick's projectile mirror for Leviathan's three prayable projectiles and
	 * schedule the correct overhead prayer for each. Projectiles are processed in
	 * EARLIEST-IMPACT-FIRST order so overlapping shots each get their own swap at the
	 * right tick -- a magic projectile landing in 5 ticks doesn't overwrite a melee
	 * projectile landing in 2.
	 * <p>
	 * No humanizer / pre-pray buffer: Leviathan's projectiles speed up through the kill
	 * and can be as fast as 1 tick spawn-to-impact. Each swap is scheduled at
	 * {@code max(tick, impactTick - 1)} -- swap as late as safely possible, but fire
	 * NOW if the projectile is already 1 tick away.
	 */
	private void watchPrayerProjectiles(int tick)
	{
		if (leviathan == null || deathPrayersOff) return;  // no boss / dead -> nothing to pray; keeps prayers off
		// DIAG [pray-diag]: every tick where prayers are in play, dump the gating flags and
		// every projectile in flight (id/start/end/prayable) so we can see (a) whether the
		// shadow-stun turn-off window is suppressing a real projectile, and (b) the exact
		// endTick vs the tick we actually swap. Remove once prayer timing is nailed down.
		{
			StringBuilder pdSb = new StringBuilder();
			boolean anyPrayable = false;
			for (MirrorState.ProjectileRecord pr : mirror.getProjectileRecords())
			{
				boolean pray = pr.id == LeviathanAutoIDs.PROJ_MELEE
					|| pr.id == LeviathanAutoIDs.PROJ_RANGE
					|| pr.id == LeviathanAutoIDs.PROJ_MAGIC;
				anyPrayable |= pray;
				pdSb.append(" {id=").append(pr.id).append(" s=").append(pr.startTick)
					.append(" e=").append(pr.endTick).append(" pray=").append(pray).append('}');
			}
			boolean turnOffWindow = shadowRushCast && !enragePhaseActive && !phase2EndResolved;
			if (anyPrayable || shadowRushCast || turnOffWindow)
			{
			}
		}
		// Impact-1 scheduling: for each prayable projectile in flight, schedule its overhead
		// 1 tick before its own impact (deduped). Overheads are turned OFF only at a PHASE
		// CHANGE -- the shadow-stun window after Shadow Rush, while the phase-2 mechanic
		// runs and Leviathan throws nothing prayable. We do NOT turn off in the gap between
		// one roar's projectiles and the next: there's no reason to drop the overhead
		// mid-combat, so we just hold the last one until the next scheduled swap.
		// A PHASE CHANGE is the shadow-stun window after Shadow Rush (and before phase 2
		// resolves): shadowRushCast && !enrage && !phase2EndResolved. Enrage is NOT a turn-off
		// window -- the pathfinder throws prayable projectiles we must keep flicking.
		boolean turnOffWindow = shadowRushCast && !enragePhaseActive && !phase2EndResolved;

		java.util.List<MirrorState.ProjectileRecord> pending = new java.util.ArrayList<>();
		for (MirrorState.ProjectileRecord pr : mirror.getProjectileRecords())
		{
			boolean prayable = pr.id == LeviathanAutoIDs.PROJ_MELEE
				|| pr.id == LeviathanAutoIDs.PROJ_RANGE
				|| pr.id == LeviathanAutoIDs.PROJ_MAGIC;
			if (!prayable) continue;
			if (pr.endTick > tick) pending.add(pr);    // schedule a swap for a future impact
		}
		// Prune dedupe entries for impacts that have already passed.
		int currentTick = tick;
		scheduledPrayIdentities.removeIf(id -> ((id >> 16) & 0xFFFF) <= currentTick);

		if (pending.isEmpty())
		{
			// Nothing prayable in flight. At a PHASE CHANGE (shadow-stun / phase-2 mechanic,
			// where Leviathan throws nothing prayable) drop the overheads. BETWEEN ROARS --
			// any non-phase-change gap -- HOLD the current overhead on; never flicker it off
			// in a 1-tick lull between a roar's projectiles.
			if (turnOffWindow)
			{
				prayers.ensureOnlyOn(tick, java.util.Collections.emptySet(),
					net.runelite.client.plugins.autoBossCore.combat.PrayerSwapper.OVERHEAD_SET());
			}
			return;
		}

		// Prayable projectiles ARE in flight -- schedule impact-1 for each, EVEN inside the
		// shadow-stun turn-off window. If roar shots are still travelling when we cast Shadow
		// Rush, or a shot arrives before phase 2 fully resolves, we must keep praying them
		// rather than dropping our guard and eating the hit. Overheads only come off above,
		// once nothing prayable remains.

		pending.sort((a, b) -> Integer.compare(a.endTick, b.endTick));
		for (MirrorState.ProjectileRecord pr : pending)
		{
			int identity = (pr.endTick << 16) ^ (pr.id & 0xFFFF);
			if (!scheduledPrayIdentities.add(identity)) continue;  // already queued

			Prayer target;
			if (pr.id == LeviathanAutoIDs.PROJ_MELEE)      target = Prayer.PROTECT_FROM_MELEE;
			else if (pr.id == LeviathanAutoIDs.PROJ_RANGE) target = Prayer.PROTECT_FROM_MISSILES;
			else                                            target = Prayer.PROTECT_FROM_MAGIC;

			// Latest safe swap tick: 1 tick before impact (or NOW if we're already there).
			prayers.scheduleSwap(target, Math.max(tick, pr.endTick - 1));
		}
	}

	// -----------------------------------------------------------------
	// Instance tile resolution.
	// -----------------------------------------------------------------

	private void resolveInstanceTilesIfNeeded(int tick)
	{
		WorldPoint pt = mirror.getPlayerTile();
		WorldPoint tpl = InstanceCoords.toTemplate(client, pt);
		boolean inArena = tpl != null && tpl.getRegionID() == LeviathanAutoIDs.ARENA_REGION;
		if (!inArena) return;
		if (startTile != null && tick - lastResolveTick < 20) return;

		startTile = InstanceCoords.fromTemplate(client, LeviathanAutoIDs.START_TILE_TEMPLATE);
		lastResolveTick = tick;
	}

	// -----------------------------------------------------------------
	// Planner.
	// -----------------------------------------------------------------

	/**
	 * Decide which tile to walk to this tick (or null to stay).
	 * <p>
	 * Section 1 model: there is a single LIVE anchor ({@link #section1AnchorTemplate})
	 * that starts at START_TILE_TEMPLATE and advances one tile south each time a 2479
	 * drops on our current tile. The planner just keeps us on that anchor:
	 * <ol>
	 *   <li>If current tile is dangerous at tick+1 or tick+2 -> dodger-driven dodge
	 *       (hazard-aware; still prefers tiles close to current).</li>
	 *   <li>Otherwise if we're not on the live anchor's instance tile and it's safe,
	 *       walk to it.</li>
	 *   <li>Otherwise stay put.</li>
	 * </ol>
	 */
	/**
	 * The home tile from {@link LeviathanAutoIDs#HOME_TILES_TEMPLATE} CLOSEST (Chebyshev
	 * distance, template space) to {@code fromInstance}, returned in INSTANCE coords.
	 * With a single home tile this is just that tile; the selection matters once a
	 * second home tile is added for the far side of the arena. Null-safe.
	 */
	private WorldPoint closestHomeTileInstance(WorldPoint fromInstance)
	{
		if (fromInstance == null) return null;
		WorldPoint fromTpl = InstanceCoords.toTemplate(client, fromInstance);
		WorldPoint bestTpl = null;
		int bestDist = Integer.MAX_VALUE;
		for (WorldPoint home : LeviathanAutoIDs.HOME_TILES_TEMPLATE)
		{
			if (fromTpl == null) { bestTpl = home; break; }  // can't measure -> first home
			int d = Math.max(Math.abs(home.getX() - fromTpl.getX()),
			                 Math.abs(home.getY() - fromTpl.getY()));
			if (d < bestDist) { bestDist = d; bestTpl = home; }
		}
		return bestTpl == null ? null : InstanceCoords.fromTemplate(client, bestTpl);
	}

	private WorldPoint planNextTile(WorldPoint current, int tick)
	{
		// Pre-fight home-tile routing only applies while we're WITHIN THE ARENA WALLS --
		// the mapped-out playable box (LeviathanAutoIDs.isWithinArenaTemplate). The rest
		// of the instance (the Handholds approach) and the overworld are both "outside",
		// so we must not click the inside start tile there -- the climb
		// (climbHandholdsIfAtExitTile) handles entry; a stray start-tile click while
		// outside is the bug. isInInstancedRegion() can't tell these apart (same region),
		// hence the geometric box test on our template tile.
		WorldPoint selfTpl = InstanceCoords.toTemplate(client, current);
		boolean inArena = LeviathanAutoIDs.isWithinArenaTemplate(selfTpl);

		// Pre-fight prep: Leviathan isn't alive and we're on an arena-entry tile
		// (e.g. 2071,6368 after climbing in) -> walk to the CLOSEST home tile to set up
		// for the next kill. Explicit so the entry->home reposition is obvious; the
		// general pre-fight block below is the catch-all for any other non-FIGHT tile.
		if (leviathan == null && inArena)
		{
			if (selfTpl != null)
			{
				for (WorldPoint entry : LeviathanAutoIDs.ARENA_ENTRY_TILES_TEMPLATE)
				{
					if (selfTpl.equals(entry))
					{
						WorldPoint home = closestHomeTileInstance(current);
						return (home != null && !current.equals(home)) ? home : null;
					}
				}
			}
		}

		// Pre-fight setup: Leviathan hasn't spawned yet (or we just killed it). Walk to
		// the closest home tile from wherever we are and hold there until the next spawn --
		// but ONLY while inside the arena instance (see above). Outside, hold (the climb
		// handles entry).
		if (phase.current() != LevPhase.FIGHT)
		{
			if (!inArena) return null;
			WorldPoint home = closestHomeTileInstance(current);
			if (home != null && !current.equals(home))
			{
				return home;
			}
			return null;  // on a home tile, wait for the fight to start
		}

		// Enrage phase overrides all other routing: boulder/lightning are over, and
		// movement is dictated purely by the pathfinder's position.
		if (enragePhaseActive && phase.current() == LevPhase.FIGHT)
		{
			return planEnrageTile(current, tick);
		}

		// Section-2 routing. Once Shadow Rush has fired we don't need the south-column
		// anchor anymore -- the destination is EAST_CENTER_LIGHTNING, the spine-shot tile
		// (Leviathan is facing semi-south-west after Shadow Rush, so his back/spine faces
		// Phase sub-handlers may later swap between MID / NORTH / SOUTH lightning tiles
		// as the lightning rotation progresses, and the boulder-phase handler will route
		// to its own tile pair entirely.
		if (shadowRushCast && phase.current() == LevPhase.FIGHT)
		{
			// Hold position until the player's own cast anim (10092) confirms the
			// server accepted our Shadow Rush dispatch. Walking before that risks
			// canceling the cast queue.
			if (!shadowRushCastConfirmed)
			{
				return null;  // stay on cast tile until the cast anim fires
			}

			WorldPoint destTpl = currentSection2TargetTemplate();
			WorldPoint destInstance = destTpl == null ? null : InstanceCoords.fromTemplate(client, destTpl);
			if (destInstance == null)
			{
				return null;
			}

			// Lightning phase: HALT the planner's danger logic. Movement cadence is
			// driven exclusively by the oscillation end-arrival block in onGameTick
			// (deferred swap + attack). The planner's only job here is to route to
			// activeLightningEnd when we aren't already standing on it; the dodger
			// must not dodge off the endpoint mid-oscillation (that's how we ended
			// up on tiles 1 west of the lane). No destBad, no computeDodge.
			if (lightningPhaseActive)
			{
				if (current.equals(destInstance)) return null;  // on endpoint, let onGameTick handle attack/swap
				return destInstance;
			}

			if (!current.equals(destInstance))
			{
				// After phase 2 resolves, our oscillation end tile sits ON a lightning lane/corner. Those
				// count as bite tiles ONLY during this weave to the next roar start -- so before running
				// there, step OFF the blacklist tile first (biased toward the destination), then resume next
				// tick. Gated to phase2EndResolved: it is fine to stand on these tiles during the lightning
				// phase itself (active lightning returns above anyway) and during the shadow-rush run.
				if (phase2EndResolved && isOnLightningLane(current))
				{
					WorldPoint esc = enrageEscapeTile(current, tick, destInstance);
					if (esc != null && !esc.equals(current)) return esc;
				}
				// Transit dodge: only interrupt the straight run if STANDING where we'd be next
				// tick would put us on a live hazard. A hazard we merely pass through is ignored.
				// On a hit, step one tile aside (staying closest to dest); the sidestep is a
				// different walk target than dest, so it breaks the re-click dedupe and dest is
				// re-clicked next tick -- resuming the run.
				if (transitStepWillBeHit(current, destInstance, tick, this::transitPassable))
				{
					WorldPoint aside = findTransitDodge(current, destInstance, tick, this::transitPassable);
					if (aside != null && !aside.equals(current))
					{
						return aside;
					}
					// No safe sidestep found -- keep heading to dest (best effort).
				}
				return destInstance;
			}
			else
			{
				// On the spine-shot tile. Only re-plan if the tile itself becomes dangerous.
				boolean curBadT1 = dodger.isTileDangerousAt(current, tick + 1);
				boolean curBadT2 = dodger.isTileDangerousAt(current, tick + 2);
				if (curBadT1 || curBadT2)
				{
					return dodger.computeDodge(current, null, 3).orElse(null);
				}
				return null;  // stay, attack
			}
		}
		else if (shadowRushCast && phase.current() != LevPhase.FIGHT)
		{
		}

		WorldPoint anchorInstance = section1AnchorTemplate == null
			? null
			: InstanceCoords.fromTemplate(client, section1AnchorTemplate);
		boolean inFightSection = anchorInstance != null && phase.current() == LevPhase.FIGHT;

		// DIAG [lev-roar-dodge]: during section-1 FIGHT, surface any rock hazard approaching the
		// current tile or the anchor we're about to walk to. If anchorDangerTick is beyond the
		// tick+2 anchorBad window below while we still walk onto the anchor, that's the 'walked
		// onto a falling rock' case (2-tick commit window vs ~6-tick rock fall).
		if (inFightSection)
		{
			int curDangerTick = firstDangerTickWithin(current, tick, 10);
			int anchorDangerTick = firstDangerTickWithin(anchorInstance, tick, 10);
			if (curDangerTick >= 0 || anchorDangerTick >= 0)
			{
			}
		}

		// Priority 1: not on anchor and anchor is a usable destination. Walking there IS
		// the dodge from any current-tile danger (e.g. a 2479 just landed on us; the new
		// south-anchor was just advanced by advanceSection1AnchorIfNeeded and is clear).
		if (inFightSection && !current.equals(anchorInstance))
		{
			boolean anchorBad = dodger.isTileDangerousAt(anchorInstance, tick + 1)
			                  || dodger.isTileDangerousAt(anchorInstance, tick + 2);
			if (!anchorBad) return anchorInstance;

			// Anchor temporarily blocked (2475 / 2476 landing on it). Divert within the
			// section-1 sidestep rect. Sidestep scanner INCLUDES current -- if current
			// is already the closest safe tile it returns current (meaning "stay"), and
			// the caller sees step == me and lets attack-maintenance fire.
			WorldPoint sidestep = findClosestSafeSidestep(current, anchorInstance, tick);
			if (sidestep != null && !sidestep.equals(current))
			{
				return sidestep;
			}
			return null;  // stay on current safe sidestep, attack
		}

		// Priority 2: on the anchor (or no anchor / out of FIGHT) and current is bad.
		// In section 1 we prefer the sidestep scanner over the generic dodger because it
		// stays inside the rock-drop corridor; outside section 1 fall back to the dodger.
		boolean curBadT1 = dodger.isTileDangerousAt(current, tick + 1);
		boolean curBadT2 = dodger.isTileDangerousAt(current, tick + 2);
		if (curBadT1 || curBadT2)
		{
			if (inFightSection)
			{
				WorldPoint sidestep = findClosestSafeSidestep(current, current, tick);
				if (sidestep != null && !sidestep.equals(current))
				{
					return sidestep;
				}
			}
			return dodger.computeDodge(current, null, 3).orElse(null);
		}

		return null;
	}

	/** First tick in [fromTick, fromTick+horizon] at which {@code tile} is a live hazard, or
	 *  -1 if none. Used to see hazards beyond the short dodge lookahead windows. */
	private int firstDangerTickWithin(WorldPoint tile, int fromTick, int horizon)
	{
		if (tile == null) return -1;
		for (int tt = fromTick; tt <= fromTick + horizon; tt++)
		{
			if (dodger.isTileDangerousAt(tile, tt)) return tt;
		}
		return -1;
	}

	/**
	 * Lane-relative sidestep. When the current section-1 tile is temporarily hazarded, step
	 * 1 (then 2) tiles PERPENDICULAR to the active lane from {@code fromInstance} (the tile
	 * we are standing on), trying the direction TOWARD THE ARENA CENTER first and away from
	 * center only as a fallback. The player pops one tile off the lane and back on once the
	 * hazard clears -- never across the arena. {@code refInstance} is accepted for call-site
	 * symmetry but no longer drives the candidate geometry (dodging relative to the blocked
	 * anchor could sit a row away and force an outward escape). Returns null if no
	 * perpendicular tile is safe + accessible.
	 */
	private WorldPoint findClosestSafeSidestep(WorldPoint fromInstance, WorldPoint refInstance, int tick)
	{
		if (fromInstance == null || refInstance == null || activeSectionConfig == null) return null;
		SectionConfig cfg = activeSectionConfig;
		// Base the perpendicular candidates on the tile we are ACTUALLY standing on
		// (fromInstance), and step toward the ARENA CENTER first. refInstance is no longer
		// used for candidate geometry: on the scripted zigzag the blocked anchor can sit a
		// row/column away from us (e.g. anchor (2087,6363) on the wall row while we stand on
		// (2088,6364)), which generated only the exterior/east escape (2089,6363). Dodging
		// relative to our own tile, center-first, instead yields the inward tile (2087,6364).
		WorldPoint fromTpl = InstanceCoords.toTemplate(client, fromInstance);
		if (fromTpl == null) return null;
		int centerX = LeviathanAutoIDs.ARENA_CENTER_TEMPLATE.getX();
		int centerY = LeviathanAutoIDs.ARENA_CENTER_TEMPLATE.getY();
		int plane = cfg.plane;
		java.util.List<WorldPoint> cands = new java.util.ArrayList<>();
		// Stay-put candidate (dist 0): if the tile we're already standing on is still safe, hold it until
		// the anchor clears instead of drifting one more tile aside each tick. In P2 current is bad, so
		// it gets filtered out and we still step off. Restores the INCLUDES-current behavior the P1 caller documents.
		cands.add(fromTpl);
		if (cfg.axis == Axis.COLUMN)
		{
			int y = fromTpl.getY();
			int baseX = fromTpl.getX();
			// Perpendicular to a COLUMN lane is the X axis. Step toward center first.
			int toward = baseX > centerX ? -1 : +1;
			for (int d = 1; d <= 2; d++)
			{
				cands.add(new WorldPoint(baseX + toward * d, y, plane)); // toward center first
				cands.add(new WorldPoint(baseX - toward * d, y, plane)); // then away from center
			}
		}
		else
		{
			int x = fromTpl.getX();
			int baseY = fromTpl.getY();
			// Perpendicular to a ROW lane is the Y axis. Step toward center first.
			int toward = baseY > centerY ? -1 : +1;
			for (int d = 1; d <= 2; d++)
			{
				cands.add(new WorldPoint(x, baseY + toward * d, plane));
				cands.add(new WorldPoint(x, baseY - toward * d, plane));
			}
		}
		WorldPoint best = null; int bestDist = Integer.MAX_VALUE;
		for (WorldPoint candTpl : cands)
		{
			if (LeviathanAutoIDs.isInaccessibleTemplate(candTpl)) continue;
			WorldPoint candInstance = InstanceCoords.fromTemplate(client, candTpl);
			if (candInstance == null) continue;
			if (dodger.isTileDangerousAt(candInstance, tick + 1)) continue;
			if (dodger.isTileDangerousAt(candInstance, tick + 2)) continue;
			int dx = Math.abs(candInstance.getX() - fromInstance.getX());
			int dy = Math.abs(candInstance.getY() - fromInstance.getY());
			int dist = Math.max(dx, dy);
			if (dist < bestDist) { bestDist = dist; best = candInstance; }
		}
		return best;
	}

	// Assumed run speed for transit-dodge prediction. OSRS run moves 2 tiles/tick; walking
	// moves 1. We check both so a low-energy walk tick is still covered.
	private static final int RUN_TILES_PER_TICK = 2;
	// How many ticks of the walked path to look ahead when checking for a hazard landing on us.
	// Kept short (2) so we react to an IMMINENT path hazard like a human -- stepping aside a tick or
	// two out -- instead of pausing many tiles early for a hazard we have not reached yet (Duke uses 2).
	private static final int TRANSIT_DODGE_HORIZON = 2;
	/** Max lookahead for isDestinationHazardAtArrival -- ~4 tiles at run speed. Beyond this the dodger's
	 *  predictions are too noisy to act on, so a far target is not rejected on far-future hazards. */
	private static final int ARRIVAL_HAZARD_MAX_TICKS = 2;
	// BFS node budget for the in-arena path. The playable box is ~25x25, so a near goal is
	// found in far fewer; this is a safety cap.
	private static final int ARENA_PATH_BUDGET = 400;

	/**
	 * Transit collision test for an A->B run. A hazard only damages us if we are STANDING on
	 * its tile at the exact tick it deals damage -- a hazard on a tile we merely pass through
	 * is harmless. We predict the REAL walked path over live collision ({@link
	 * net.runelite.client.plugins.autoBossCore.combat.TilePathPredictor}, with Leviathan's
	 * footprint blocked and the search bounded to the arena), walk it at run speed, and return
	 * true if any per-tick standing tile is a live hazard AT the tick we'd arrive on it.
	 * Falls back to the straight-line next-tile guess only if no path is available.
	 */
	private boolean transitStepWillBeHit(WorldPoint from, WorldPoint dest, int now,
		java.util.function.Predicate<WorldPoint> passable)
	{
		if (from == null || dest == null) return false;

		java.util.List<WorldPoint> path = net.runelite.client.plugins.autoBossCore.combat.TilePathPredictor.path(
			client.getTopLevelWorldView(), from, dest, passable, this::inArenaInstance, ARENA_PATH_BUDGET);

		if (path.isEmpty())
		{
			// No path (collision maps unavailable / goal unreachable in-arena): fall back to the
			// straight-line next-tile prediction. Also honour the passable gate -- for the enrage
			// approach a straight line can cross a lightning lane (melee/bite range = guaranteed
			// hit), so treat a non-passable next tile as a hit too and force the off-lane sidestep.
			int next = now + 1;
			for (int adv = 1; adv <= RUN_TILES_PER_TICK; adv++)
			{
				WorldPoint pos = stepToward(from, dest, adv);
				if (!passable.test(pos) || dodger.isTileDangerousAt(pos, next)) return true;
			}
			return false;
		}

		int ticksToArrive = (path.size() - 1 + RUN_TILES_PER_TICK - 1) / RUN_TILES_PER_TICK;
		int horizon = Math.min(TRANSIT_DODGE_HORIZON, ticksToArrive);
		for (int k = 1; k <= horizon; k++)
		{
			WorldPoint standing = net.runelite.client.plugins.autoBossCore.combat.TilePathPredictor.tileAfterTicks(
				path, k, RUN_TILES_PER_TICK);
			if (standing == null) break;
			if (dodger.isTileDangerousAt(standing, now + k)) return true;
		}
		return false;
	}

	/** Passable gate for the path predictor: Leviathan's footprint isn't in the collision map
	 *  (NPCs never are), so block it here. True == tile is walkable. */
	private boolean transitPassable(WorldPoint wp)
	{
		return leviathan == null || leviathan.getWorldArea() == null
			|| !leviathan.getWorldArea().contains2D(wp);
	}

	/** Passable gate for the ENRAGE approach: like transitPassable, but ALSO blocks lightning-
	 *  lane tiles. Standing on a lane tile while running to the pathfinder gets us hit by
	 *  Leviathan, so the path must route AROUND the lanes, not merely avoid them as the end tile. */
	private boolean enrageApproachPassable(WorldPoint wp)
	{
		return transitPassable(wp) && !isOnLightningLane(wp);
	}

	/** Bounds gate for the path predictor: keep the search inside the mapped arena box (which
	 *  also keeps every collision query inside the loaded scene). */
	private boolean inArenaInstance(WorldPoint wp)
	{
		WorldPoint tpl = InstanceCoords.toTemplate(client, wp);
		return tpl != null && LeviathanAutoIDs.isWithinArenaTemplate(tpl);
	}

	/** One Chebyshev move of up to {@code tiles} steps from {@code a} toward {@code b} (clamped at b). */
	private WorldPoint stepToward(WorldPoint a, WorldPoint b, int tiles)
	{
		int x = a.getX(), y = a.getY();
		for (int i = 0; i < tiles; i++)
		{
			if (x == b.getX() && y == b.getY()) break;
			x += Integer.signum(b.getX() - x);
			y += Integer.signum(b.getY() - y);
		}
		return new WorldPoint(x, y, a.getPlane());
	}

	/**
	 * Pick a one-tile sidestep off the straight path when {@link #transitStepWillBeHit}
	 * fires: an accessible neighbour that is safe to STAND on for the next two ticks (so we
	 * don't dodge straight into another hit) and keeps us as close to dest as possible so we
	 * barely lose ground. Returns null if no neighbour beats staying put.
	 */
	private WorldPoint findTransitDodge(WorldPoint from, WorldPoint dest, int now,
		java.util.function.Predicate<WorldPoint> passable)
	{
		if (from == null || dest == null) return null;
		// We RUN (RUN_TILES_PER_TICK tiles/tick), so consider every tile reachable in ONE run tick -- up to a
		// knight's-L two-tile hop, not just a 1-tile sidestep. Pick the safe landing tile CLOSEST to dest
		// (tiebreak: the bigger hop), so the dodge advances TOWARD the goal and loses no ground instead of
		// stepping aside and stalling.
		// Tiered: a dodge MUST survive arrival (now+1). Prefer a tile also safe at now+2, but if none
		// qualifies accept an arrival-safe tile toward the goal rather than returning nothing (which made
		// the caller walk straight into the hazard). Both tiers pick CLOSEST to dest, tiebreak bigger hop.
		WorldPoint best2 = null; int best2Dist = Integer.MAX_VALUE; int best2Step = 0;  // safe now+1 AND now+2
		WorldPoint best1 = null; int best1Dist = Integer.MAX_VALUE; int best1Step = 0;  // safe now+1 only (fallback)
		for (int ddx = -RUN_TILES_PER_TICK; ddx <= RUN_TILES_PER_TICK; ddx++)
		{
			for (int ddy = -RUN_TILES_PER_TICK; ddy <= RUN_TILES_PER_TICK; ddy++)
			{
				if (ddx == 0 && ddy == 0) continue;
				int step = Math.max(Math.abs(ddx), Math.abs(ddy));   // Chebyshev tiles this run tick (1..2)
				WorldPoint cand = new WorldPoint(from.getX() + ddx, from.getY() + ddy, from.getPlane());
				WorldPoint candTpl = InstanceCoords.toTemplate(client, cand);
				if (candTpl == null || LeviathanAutoIDs.isInaccessibleTemplate(candTpl)) continue;
				if (!passable.test(cand)) continue;  // never LAND on a blocked tile (boss footprint / lane)
				if (step == 2)
				{
					// A 2-tile run passes through one intermediate (diagonal) tile: it must be walkable or the
					// server can't reach cand this tick. The run SKIPS it, so a hazard there doesn't touch us.
					WorldPoint mid = new WorldPoint(from.getX() + Integer.signum(ddx),
						from.getY() + Integer.signum(ddy), from.getPlane());
					WorldPoint midTpl = InstanceCoords.toTemplate(client, mid);
					if (midTpl == null || LeviathanAutoIDs.isInaccessibleTemplate(midTpl) || !passable.test(mid)) continue;
				}
				if (dodger.isTileDangerousAt(cand, now + 1)) continue;   // hard: must survive arrival
				int d = Math.max(Math.abs(cand.getX() - dest.getX()), Math.abs(cand.getY() - dest.getY()));
				if (!dodger.isTileDangerousAt(cand, now + 2)
					&& (d < best2Dist || (d == best2Dist && step > best2Step)))
				{
					best2Dist = d; best2 = cand; best2Step = step;
				}
				if (d < best1Dist || (d == best1Dist && step > best1Step))
				{
					best1Dist = d; best1 = cand; best1Step = step;
				}
			}
		}
		return best2 != null ? best2 : best1;   // prefer 2-tick-safe, else any arrival-safe tile toward the goal
	}

	/**
	 * Section 1 south-stepper. Two triggers advance {@link #section1AnchorTemplate}
	 * (counting the roar toward {@link LeviathanAutoIDs#TOTAL_SECTION_1_ROARS}):
	 * <ol>
	 *   <li><b>Primary</b>: any {@link LeviathanAutoIDs#GFX_UNBREAKABLE_ROCK_IDS} (2477-2480) is
	 *       currently on the player's tile. Leviathan's roar animation (10282) usually
	 *       co-occurs but we don't require it -- the GFX presence is the real signal
	 *       and the roar anim can be only 1 tick wide which risks missing the window.</li>
	 *   <li><b>Fallback</b>: the current anchor's template coord is in
	 *       {@link #trackedRockDropTemplates} already. That means a rock was dropped on
	 *       this tile previously (or has dropped since we last re-evaluated) -- the
	 *       tile is permanently unusable, so the anchor MUST advance regardless of
	 *       the GFX trigger.</li>
	 * </ol>
	 * The anchor advance is NOT a hard "step south by one". After computing the
	 * nominal south tile, {@link #skipSightlineBlockedAhead(WorldPoint)} slides it
	 * further south while any tracked rock in the same column (same X, Y &lt;= dest Y)
	 * would block attack LOS to Leviathan. One roar can therefore advance the anchor
	 * by two or more tiles, but it still counts as a single roar for completion.
	 */
	private void advanceSection1AnchorIfNeeded(int tick)
	{
		if (section1AnchorTemplate == null) return;
		if (isSection1Complete()) return;

		// FALLBACK: if the anchor's template tile already has a known rock-drop recorded,
		// advance south regardless. This fires if the primary trigger was missed.
		if (trackedRockDropTemplates.contains(section1AnchorTemplate))
		{
			section1AnchorTemplate = advanceSection1Anchor(section1AnchorTemplate);
			section1RoarCount++;
			lastSouthStepTick = tick;
			return;
		}

		if (tick - lastSouthStepTick < 2) return;

		WorldPoint playerTile = mirror.getPlayerTile();
		if (playerTile == null) return;

		Iterable<GraphicsObject> gfx = client.getGraphicsObjects();
		if (gfx == null) return;
		boolean rockDropOnMe = false;
		for (GraphicsObject g : gfx)
		{
			if (g == null) continue;
			if (!LeviathanAutoIDs.isUnbreakableRockGfx(g.getId())) continue;
			LocalPoint lp = g.getLocation();
			if (lp == null) continue;
			// fromLocal -> INSTANCE coords, same space as mirror.getPlayerTile().
			WorldPoint gfxTile = WorldPoint.fromLocal(client, lp);
			if (gfxTile != null && gfxTile.equals(playerTile))
			{
				rockDropOnMe = true;
				break;
			}
		}
		if (!rockDropOnMe) return;

		section1AnchorTemplate = advanceSection1Anchor(section1AnchorTemplate);
		section1RoarCount++;
		lastSouthStepTick = tick;
	}

	/**
	 * Starting at {@code dest}, slide further one tile at a time along the active
	 * section's direction while any tile in {@link #trackedRockDropTemplates} shares
	 * the lane's fixed coord AND sits ahead of (or on) {@code dest} along the
	 * direction of travel. Those rocks would block LOS from {@code dest} to Leviathan.
	 * Hard-capped at a few iterations so a weird edge case can't spin forever.
	 */
	/**
	 * Next section-1 anchor. For a scripted zigzag ({@link #scriptedRoarPath} non-null)
	 * this steps to the next tile in the explicit list (the hand-picked wall-dodging
	 * order); otherwise it is one straight step along {@link #activeSectionConfig}, slid
	 * past LOS-blocking rocks by {@link #skipSightlineBlockedAhead(WorldPoint)}.
	 */
	private WorldPoint advanceSection1Anchor(WorldPoint current)
	{
		if (scriptedRoarPath != null)
		{
			scriptedRoarIdx++;
			int i = Math.min(scriptedRoarIdx, scriptedRoarPath.length - 1);
			return scriptedRoarPath[i];
		}
		return skipSightlineBlockedAhead(activeSectionConfig.advance(current));
	}

	private WorldPoint skipSightlineBlockedAhead(WorldPoint dest)
	{
		if (dest == null || activeSectionConfig == null) return dest;
		int maxSkips = 4;
		while (maxSkips-- > 0 && hasBlockingRockAhead(dest))
		{
			dest = activeSectionConfig.advance(dest);
		}
		return dest;
	}

	/** Perpendicular buffer (tiles) for the sightline-skip LOS check. Rocks within
	 *  this many tiles of our lane, on Leviathan's side, are treated as blocking.
	 *  Covers the W/E lightning-pattern rocks (perp offset 2-3) that aren't on our
	 *  exact column but still block LOS from our anchor to Leviathan. Tune up if
	 *  LOS blocks are still missed, down if we skip too eagerly from distant rocks. */
	private static final int SIGHTLINE_PERP_BUFFER_TILES = 3;

	/**
	 * True if any tracked rock sits in the "forward cone" from our anchor to
	 * Leviathan AND is ahead of (or at) {@code dest} along the direction of travel.
	 * <p>
	 * The forward cone is: perpendicular offset from our lane in [0, SIGHTLINE_PERP_BUFFER_TILES]
	 * on Leviathan's side (positive if we're on the west/north side of arena center,
	 * negative otherwise), AND travel-direction coord &lt;= destVar for a negative
	 * direction / &gt;= destVar for a positive direction.
	 * <p>
	 * This covers both same-lane rocks (perp offset 0) and off-lane rocks like the
	 * W-pattern lightning drops at x=2073 that block LOS from our x=2071 column.
	 */
	private boolean hasBlockingRockAhead(WorldPoint dest)
	{
		if (dest == null || activeSectionConfig == null) return false;
		SectionConfig cfg = activeSectionConfig;
		int destVar = cfg.variableCoord(dest);
		int arenaCenter = (cfg.axis == Axis.COLUMN) ? 2081 : 6372;
		// perpDir = +1 if Leviathan is on the positive side of our lane (east for COLUMN
		// at west column, south for ROW at north row), -1 otherwise.
		int perpDir = cfg.fixedCoord < arenaCenter ? 1 : -1;
		for (WorldPoint tpl : trackedRockDropTemplates)
		{
			if (tpl.getPlane() != dest.getPlane()) continue;
			int tplFixed, tplVar;
			if (cfg.axis == Axis.COLUMN) { tplFixed = tpl.getX(); tplVar = tpl.getY(); }
			else                          { tplFixed = tpl.getY(); tplVar = tpl.getX(); }
			int perpOffset = (tplFixed - cfg.fixedCoord) * perpDir;
			if (perpOffset < 0 || perpOffset > SIGHTLINE_PERP_BUFFER_TILES) continue;
			// Only treat as blocking when the rock is on the SAME row/column as dest.
			// The old check (tplVar <= destVar for south-step) treated any rock ahead
			// of us as a block, so a rock 4 tiles south of our next tile would make the
			// skip loop iterate past 3 safe tiles too. Now we only skip the roar that
			// actually puts us on the rock's row.
			if (tplVar == destVar) return true;
		}
		return false;
	}

	// -----------------------------------------------------------------
	// Supply.
	// -----------------------------------------------------------------

	/** Between-fights top-off: eat toward MAX hp and sip prayer (and stamina) to full before tidy/decant.
	 *  No EatDelay tracker here, so eating is paced with a ~3-tick gap. The ranged boost is NOT sipped --
	 *  it would decay before the next kill. True while still restoring (hold the window), false once full. */
	/** True when all 28 inventory slots are occupied. */
	private boolean inventoryFull()
	{
		net.runelite.api.ItemContainer inv = client.getItemContainer(net.runelite.api.InventoryID.INVENTORY);
		if (inv == null) return false;
		int used = 0;
		for (net.runelite.api.Item it : inv.getItems()) if (it != null && it.getId() >= 0) used++;
		return used >= 28;
	}

	private boolean cleanupTopOffTick(int tick)
	{
		boolean full = mirror.getHp() >= mirror.getMaxHp();
		boolean canEatNow = !full && tick - lastCleanupEatTick >= 3;
		ConsumptionEngine.Request.Builder b = ConsumptionEngine.Request.builder();
		b.wantEat(canEatNow);
		b.allowComboEat(canEatNow);
		ConsumptionEngine.requestPrayerSip(b, mirror, 30, LeviathanSupply.PROFILE.prayerSipJitterWindow, true);
		if (mirror.getRunEnergy() <= LeviathanSupply.PROFILE.runEnergySipStam
			&& LeviathanSupply.PROFILE.hasPotion(net.runelite.client.plugins.autoBossCore.consume.SupplyProfile.DrinkKind.STAMINA))
		{
			b.opportunisticSip(net.runelite.client.plugins.autoBossCore.consume.SupplyProfile.DrinkKind.STAMINA);
		}
		ConsumptionEngine.Result r = engine.evaluate(b.build());
		if (r.hasAction())
		{
			if (r.primaryEat != null || r.comboEat != null) lastCleanupEatTick = tick;
			applyConsumption(r);
			return true;
		}
		return !full && tick - lastCleanupEatTick < 3;   // hold while a just-eaten bite is still landing
	}

	private ConsumptionEngine.Request buildConsumptionRequest(boolean topOff, int tick)
	{
		ConsumptionEngine.Request.Builder b = ConsumptionEngine.Request.builder();

		// Eating is gated to phases where we are NOT attacking, so a same-tick eat can't re-path
		// us off an attack interaction. See eatPhaseAllowed(): the post-Shadow-Rush stun window
		// and the enrage APPROACH run to the pathfinder. Everywhere else (roars, lightning,
		// boulder, in-footprint enrage, transit back) eating is OFF -- the fight is mostly chip
		// damage. No critical-HP override.
		// Eat only in a safe (non-attacking) phase. PARKED/attacking (topOff=false): wait for the HP
		// threshold so an eat doesn't cost a shot. RUNNING through a safe window (topOff=true -- the walk
		// to Leviathan's back and the run to the pathfinder): not attacking, so top off to full, paced to
		// the ~3-tick food cooldown; primary food only (no combo) so we don't waste karambwan.
		boolean needFood = eatPhaseAllowed() && (topOff
			? (mirror.getHp() < mirror.getMaxHp() && tick - lastTopOffEatTick >= 3)
			: (mirror.getHp() <= LeviathanSupply.PROFILE.hpEatThreshold));
		b.wantEat(needFood);
		b.allowComboEat(needFood && !topOff);

		// Prayer: standalone sip below HALF of max (jittered); when eating, piggyback a free top-up.
		ConsumptionEngine.requestPrayerSip(b, mirror, 30,
			LeviathanSupply.PROFILE.prayerSipJitterWindow, needFood);

		if (mirror.getRunEnergy() <= LeviathanSupply.PROFILE.runEnergySipStam
			&& LeviathanSupply.PROFILE.hasPotion(net.runelite.client.plugins.autoBossCore.consume.SupplyProfile.DrinkKind.STAMINA))
		{
			b.opportunisticSip(net.runelite.client.plugins.autoBossCore.consume.SupplyProfile.DrinkKind.STAMINA);
		}

		// Ranged combat-boost top-off (divine ranging preferred, else ranging; only one is
		// ever carried). Divine is refreshed on a 5-min timer; regular re-doses once the
		// remaining Ranged boost drops below the boss threshold. The engine still gates the
		// actual drink to a safe tick and to what's in the inventory.
		rangingBoost.requestSips(b, mirror.getTickCounter(),
			client.getBoostedSkillLevel(net.runelite.api.Skill.RANGED),
			client.getRealSkillLevel(net.runelite.api.Skill.RANGED),
			net.runelite.client.plugins.autoBossCore.consume.SupplyProfile.DrinkKind.DIVINE_RANGE,
			net.runelite.client.plugins.autoBossCore.consume.SupplyProfile.DrinkKind.RANGING,
			LeviathanAutoIDs.RANGING_REDRINK_MIN_BOOST);

		return b.build();
	}

	/** Build, evaluate and apply the consumption request this tick. Returns true if an eat
	 *  (primary or combo food) was actually issued. Skips entirely on the lightning end-arrival
	 *  attack tick -- a same-tick attackNpc + CC_OP eat re-paths us off the oscillation. */
	private boolean doConsumption(int tick) { return doConsumption(tick, false); }

	private boolean doConsumption(int tick, boolean topOff)
	{
		boolean lightningAttackThisTick = lightningPhaseActive && lightningEndAttackedAtTick == tick;
		if (lightningAttackThisTick) return false;
		ConsumptionEngine.Request req = buildConsumptionRequest(topOff, tick);
		ConsumptionEngine.Result res = engine.evaluate(req);
		applyConsumption(res);
		boolean ate = res != null && (res.primaryEat != null || res.comboEat != null);
		if (ate) lastTopOffEatTick = tick;
		return ate;
	}

	private void applyConsumption(ConsumptionEngine.Result r)
	{
		if (r == null || !r.hasAction()) return;
		// Potions (prayer / stamina / combat-boost) are only ever sipped INSIDE Leviathan's arena.
		// Outside it (bank, Handholds approach, overworld) we never drink; food eating is unaffected.
		boolean mayDrink = r.drink != null && isWithinLeviathanArena();
		if (mayDrink) router.useInventoryItem(bestDoseId(r.drink), "Drink");
		if (r.primaryEat != null) router.useInventoryItem(r.primaryEat.itemId, "Eat");
		if (r.comboEat   != null) router.useInventoryItem(r.comboEat.itemId, "Eat");
		// Restart the divine-ranging 5-min timer whenever a divine dose actually went down.
		if (mayDrink && r.drink == net.runelite.client.plugins.autoBossCore.consume.SupplyProfile.DrinkKind.DIVINE_RANGE)
			rangingBoost.onDivineSipped(mirror.getTickCounter());
	}

	/** True when the player's current tile is inside the mapped Leviathan arena box. Potions are
	 *  gated to this -- we never sip outside the arena (bank, Handholds approach, overworld). */
	private boolean isWithinLeviathanArena()
	{
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return false;
		return LeviathanAutoIDs.isWithinArenaTemplate(InstanceCoords.toTemplate(client, me));
	}

	/** Pick the LOWEST available dose id for a potion kind, so partial (1/2/3-dose) vials drain first and
	 *  empty out of the inventory before we touch full 4-dose ones -- consolidating supplies and freeing
	 *  slots over a long trip instead of leaving a spread of part-used vials. */
	private int bestDoseId(net.runelite.client.plugins.autoBossCore.consume.SupplyProfile.DrinkKind k)
	{
		net.runelite.client.plugins.autoBossCore.consume.SupplyProfile.PotionEntry pot =
			LeviathanSupply.PROFILE.potion(k);
		if (pot == null) return -1;
		for (int i = 0; i < pot.doseIds.length; i++)   // index 0 = 1-dose .. 3 = 4-dose: lowest first
		{
			int id = pot.doseIds[i];
			if (id > 0 && mirror.countItem(id) > 0) return id;
		}
		return -1;
	}

	// -----------------------------------------------------------------
	// Attack maintenance.
	// -----------------------------------------------------------------

	/**
	 * True once Shadow Rush has been cast but we aren't yet standing on
	 * {@link LeviathanAutoIDs#EAST_CENTER_LIGHTNING}. Attack maintenance uses this to
	 * suppress ranged shots mid-trip: a shot that fires while we're still running
	 * east would interrupt the walk, waste the attack-cooldown, and in any case
	 * won't hit the spine-weak-spot damage multiplier until we're on-position.
	 */
	private boolean isTravelingToPostCast()
	{
		if (!shadowRushCast) return false;
		WorldPoint me = mirror.getPlayerTile();
		WorldPoint destTpl = currentSection2TargetTemplate();
		if (destTpl == null) return false;
		WorldPoint dest = InstanceCoords.fromTemplate(client, destTpl);
		return me != null && dest != null && !me.equals(dest);
	}

	/**
	 * Should attack maintenance suppress its shot this tick? Three reasons to suppress:
	 * <ul>
	 *   <li>Post-Shadow-Rush travel (spine walk, pre-boulder-dodge walk, inter-dodge
	 *       movement) -- firing mid-trip would interrupt pathing.</li>
	 *   <li>Lightning phase is a CONTROLLED phase -- the only valid shot is the
	 *       explicit center-crossing attack fired in onGameTick. Any natural
	 *       canAttack firing (e.g. while standing stationary at a lane end) would
	 *       break the controlled cadence and waste the shot off-center.</li>
	 *   <li>Boulder phase is ALSO a controlled phase -- movement is on a tight 2-tick
	 *       cadence (one projectile per tick, move every 2nd). A natural attack on
	 *       the current dodge-stance tile burns the tick we need for the next move,
	 *       so suppress and let the sequence drive. (Explicit boulder attack triggers
	 *       at specific dodge positions are TBD; for now boulder = dodge-only.)</li>
	 * </ul>
	 */
	private boolean shouldSuppressAttack()
	{
		// Enrage: suppress attacks while walking TO the pathfinder's 3x3 so the walk
		// clicks don't get canceled by auto-attack packets. Once inside the 3x3,
		// attacks resume (handled by planEnrageTile's max-DPS short-circuit).
		if (enragePhaseActive) return !isInsidePathfinderFootprint();
		// Phase 2 is over: Leviathan is attackable again while we walk to the next
		// section, so keep attacking (interleaved with the walk in the planNextTile
		// dispatch). This is the main DPS win on long transits -- e.g. lightning ends
		// south but the next lane is north.
		if (phase2EndResolved) return false;
		if (isTravelingToPostCast()) return true;
		if (lightningPhaseActive) return true;
		if (boulderPhaseActive) return true;
		return false;
	}

	/** True while Eagle Eye should be up: the DPS windows (section-1 roars, boulder, and the post-Shadow-
	 *  Rush back-hit), plus the whole enrage once we have reached the pathfinder 3x3 (latched to end of
	 *  kill). OFF during lightning, the shadow-stun/approach, movement between windows, and out of fight.
	 *  Per-cycle flags (shadowRushCast, section1RoarCount, boulder/lightning) reset in beginCycle, so this
	 *  re-arms each cycle's roars; the enrage latch is the only state that persists across cycles. */
	private boolean wantEagleEye()
	{
		if (deathPrayersOff) return false;                             // Leviathan HP hit 0 -- all prayers off, stay off
		if (phase.current() != LevPhase.FIGHT) return false;
		if (enragePhaseActive) return eagleEyeEnrageLatched;            // on once in the 3x3, stays on to kill end
		if (lightningPhaseActive) return false;                        // lightning kite -- no DPS
		if (shadowRushCast && !shadowRushCastConfirmed) return false;  // shadow-stun / moving to the back
		if (boulderPhaseActive) return true;                           // boulder DPS
		if (shadowRushCastConfirmed && !spineAttackDone) return !isTravelingToPostCast();  // off while walking to the back; on once ON the spine tile, then attack
		if (!shadowRushCast && !isSection1Complete()) return true;     // section-1 roars
		return false;                                                  // between windows / other movement
	}

	/** True if the player's current tile is inside the Abyssal Pathfinder's 3x3 footprint. */
	private boolean isInsidePathfinderFootprint()
	{
		WorldPoint sw = effectiveEnrageFootprintSW();
		if (sw == null) return false;
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return false;
		return me.getX() >= sw.getX() && me.getX() <= sw.getX() + 2
			&& me.getY() >= sw.getY() && me.getY() <= sw.getY() + 2
			&& me.getPlane() == sw.getPlane();
	}

	/**
	 * SW corner of the enrage 3x3 footprint: the Abyssal Pathfinder's own tile once it
	 * has spawned, else derived from the {@link LeviathanAutoIDs#GFX_ENRAGE_PLUS} (2298)
	 * '+' spotanims (their bounding box is that same 3x3). INSTANCE coords, or null if
	 * neither is visible this tick.
	 */
	private WorldPoint effectiveEnrageFootprintSW()
	{
		if (abyssalPathfinder != null)
		{
			WorldPoint sw = abyssalPathfinder.getWorldLocation();
			if (sw != null) return sw;
		}
		return enragePlusFootprintSW();
	}

	/**
	 * SW corner (INSTANCE coords) of the 3x3 marked by the GFX_ENRAGE_PLUS '+' this tick,
	 * or null if no 2298 is visible. The '+' spans a 3x3; we take the midpoint of the
	 * spotanims' bounding box as the center and return center-(1,1).
	 */
	private WorldPoint enragePlusFootprintSW()
	{
		Iterable<GraphicsObject> gfx = client.getGraphicsObjects();
		if (gfx == null) return null;
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, plane = 0;
		boolean any = false;
		for (GraphicsObject g : gfx)
		{
			if (g == null || g.getId() != LeviathanAutoIDs.GFX_ENRAGE_PLUS) continue;
			LocalPoint lp = g.getLocation();
			if (lp == null) continue;
			WorldPoint wp = WorldPoint.fromLocal(client, lp);
			if (wp == null) continue;
			minX = Math.min(minX, wp.getX()); maxX = Math.max(maxX, wp.getX());
			minY = Math.min(minY, wp.getY()); maxY = Math.max(maxY, wp.getY());
			plane = wp.getPlane();
			any = true;
		}
		if (!any) return null;
		int cx = (minX + maxX) / 2, cy = (minY + maxY) / 2;
		return new WorldPoint(cx - 1, cy - 1, plane);
	}

	/**
	 * One-shot spine-attack destination for the Shadow Rush -> back-hit transition,
	 * in TEMPLATE coords. STRICTLY for the post-Shadow-Rush walk.
	 * <p>
	 * Generalized: the spine lane is the LIGHTNING lane on the opposite side of
	 * Leviathan from the player's section-1 lane. The spine coord within that lane
	 * is the mirror of the section-1 end's varying coord around the spine lane's
	 * center.
	 * <ul>
	 *   <li>Section-1 COLUMN at west (x&lt;2080) -&gt; spine on EAST lane (x=2086),
	 *       pivot Y=6372, spineY = 2*6372 - section1EndY.</li>
	 *   <li>Section-1 COLUMN at east (x&gt;2080) -&gt; spine on WEST lane (x=2076),
	 *       same pivot Y=6372.</li>
	 *   <li>Section-1 ROW at north (y&gt;6372) -&gt; spine on SOUTH lane (y=6367),
	 *       pivot X=2081, spineX = 2*2081 - section1EndX.</li>
	 *   <li>Section-1 ROW at south (y&lt;6372) -&gt; spine on NORTH lane (y=6377),
	 *       same pivot X=2081.</li>
	 * </ul>
	 */
	private WorldPoint postCastTargetTemplate()
	{
		if (section1AnchorTemplate == null) return null;
		// Run to the lightning lane OPPOSITE the side of Leviathan we cast Shadow Rush
		// from (the section-1 end tile is our cast position). Pick the dominant axis of
		// our offset from arena center (2081,6372):
		//   cast W -> E column (x=2086)   cast E -> W column (x=2076)
		//   cast S -> N row    (y=6377)   cast N -> S row    (y=6367)
		// The along-lane coord mirrors our end around center, clamped to the lane's span
		// (columns y=6368..6376, rows x=2077..2085). This generalises every roar lane
		// including the SW-corner zigzag (south side -> north row) with one rule.
		int cx = LeviathanAutoIDs.ARENA_CENTER_TEMPLATE.getX();  // 2081
		int cy = LeviathanAutoIDs.ARENA_CENTER_TEMPLATE.getY();  // 6372
		int endX = section1AnchorTemplate.getX();
		int endY = section1AnchorTemplate.getY();
		int plane = section1AnchorTemplate.getPlane();
		int dx = endX - cx, dy = endY - cy;
		if (Math.abs(dx) >= Math.abs(dy))
		{
			int spineX = dx > 0 ? 2076 : 2086;                 // cast E -> W lane, cast W -> E lane
			int spineY = Math.max(6368, Math.min(6376, 2 * cy - endY));
			return new WorldPoint(spineX, spineY, plane);
		}
		else
		{
			int spineY = dy > 0 ? 6367 : 6377;                 // cast N -> S row, cast S -> N row
			int spineX = Math.max(2077, Math.min(2085, 2 * cx - endX));
			return new WorldPoint(spineX, spineY, plane);
		}
	}

	// -----------------------------------------------------------------
	// Per-cycle configuration.
	// -----------------------------------------------------------------

	/**
	 * Reset per-cycle state and advance to the given cycle. Called with cycle=0 by
	 * start() (via the field initializers) and with cycle=N when the player arrives
	 * at the previous cycle's reset tile with phase2EndResolved. Phase 2 alternates
	 * strictly: cycle 0 is classified by first-roar rocks, cycle 1+ is the opposite
	 * of the previous cycle. trackedRockDropTemplates is NOT cleared -- round-1
	 * rocks are permanent walls that round-2 pathing still needs to avoid.
	 */
	/**
	 * Returns the phase-2 "end tile" (template coord) when phase 2 is finished and
	 * we're ready to transition to the next cycle. Returns null if phase 2 isn't
	 * resolved yet (phase2EndResolved == false) or no active destination is set.
	 * <ul>
	 *   <li>BOULDER: end tile = {@link #activeBoulderCluster}[5] (the backfill).</li>
	 *   <li>LIGHTNING: end tile = {@link #activeLightningEnd}.</li>
	 * </ul>
	 * Both paths gate on {@link #phase2EndResolved} which the shared 10287
	 * (ANIM_PHASE2_END) detection sets.
	 */
	private WorldPoint currentPhase2EndTile()
	{
		if (!phase2EndResolved) return null;
		if (section2Phase == Section2Phase.BOULDER && activeBoulderCluster != null)
		{
			return activeBoulderCluster[5];
		}
		if (section2Phase == Section2Phase.LIGHTNING && activeLightningEnd != null)
		{
			// activeLightningEnd can be a SHRUNK oscillation tile (e.g. 6370/6374),
			// which isn't a transition-map key. Since we know this was lightning,
			// snap to the nearest canonical full-lane end (6368/6376 column, or
			// 2077/2085 row) so the next-section lookup hits instead of stalling.
			return canonicalLightningEnd(activeLightningEnd);
		}
		return null;
	}

	/**
	 * Snap any oscillation tile on a lightning lane to that lane's nearest canonical
	 * end -- the tile the {@link #PHASE2_END_TO_NEXT_SECTION} map is keyed on. Column
	 * lanes (x=2086/2076) snap Y to 6376 (north half) or 6368 (south half); row lanes
	 * (y=6377/6367) snap X to 2085 (east half) or 2077 (west half).
	 */
	private WorldPoint canonicalLightningEnd(WorldPoint osc)
	{
		if (osc == null) return null;
		int x = osc.getX(), y = osc.getY(), plane = osc.getPlane();
		if (x == 2086 || x == 2076) return new WorldPoint(x, y >= 6372 ? 6376 : 6368, plane);
		if (y == 6377 || y == 6367) return new WorldPoint(x >= 2081 ? 2085 : 2077, y, plane);
		return osc;
	}

	/**
	 * Roar target for a section. Default is {@link LeviathanAutoIDs#TOTAL_SECTION_1_ROARS}.
	 * The post-SE-boulder east column (COLUMN x=2091, north-step from y=6369) instead
	 * uses {@code TOTAL_SECTION_1_ROARS - shadowRushUses} (floored at 1): every shadow
	 * rush used earlier in the fight has consumed one more tile of that lane, so there
	 * are fewer roars to take. Keyed on the config geometry so it survives being reached
	 * from the {@link #PHASE2_END_TO_NEXT_SECTION} map.
	 */
	/**
	 * The next section's config for a given phase-2 end tile: the
	 * {@link #PHASE2_END_TO_NEXT_SECTION} mapping, overridden by the low-HP finisher
	 * ({@link #LOW_HP_ROAR_CONFIG}) when Leviathan is under
	 * {@link LeviathanAutoIDs#LOW_HP_ROAR_THRESHOLD}. Used by both the post-phase-2 walk
	 * routing and the cycle transition, so routing and {@link #beginCycle} agree. This
	 * is only ever consulted at a roar-phase START -- never mid-roars -- so a roar
	 * section already in progress above the threshold is never redirected.
	 */
	private SectionConfig nextSectionConfigFor(WorldPoint phase2End)
	{
		SectionConfig mapped = phase2End == null ? null : PHASE2_END_TO_NEXT_SECTION.get(phase2End);
		if (mapped == null) return null;
		int hp = bossCurrentHp();
		if (hp < 0) hp = lastKnownBossHp;   // HP bar can read -1 at the phase-2-end anim transition; use last known
		// After an east-lightning phase-2, our higher-than-expected DPS can leave Leviathan very low; the
		// usual SW zigzag / low-HP north roar then over-runs the pathfinder setup. If HP <= NE_ROAR_HP_THRESHOLD
		// (400), route straight to the NE-quadrant roar (2090,6380 = the mapped lightning follow-up) instead.
		if (isEastLightningEnd(phase2End) && hp >= 0 && hp <= LeviathanAutoIDs.NE_ROAR_HP_THRESHOLD)
		{
			return mapped;
		}
		if (hp >= 0 && hp < LeviathanAutoIDs.LOW_HP_ROAR_THRESHOLD)
		{
			return LOW_HP_ROAR_CONFIG;
		}
		// First east-lightning phase-2 (cycleIndex 0): roar the SW-corner zigzag instead
		// of the (2090,6380) south column -- a wall at (2084-2086,6363) blocks a straight
		// lane. Later lightning phases (cycleIndex >= 1) keep the mapped (2090,6380) route.
		if (cycleIndex == 0 && isEastLightningEnd(phase2End))
		{
			return EAST_LIGHTNING_P2_ZIGZAG_CONFIG;
		}
		return mapped;
	}

	/** True if {@code end} is an east-lightning oscillation end (the zigzag trigger). */
	private boolean isEastLightningEnd(WorldPoint end)
	{
		return end != null
			&& (end.equals(LeviathanAutoIDs.EAST_SOUTH_LIGHTNING)
			 || end.equals(LeviathanAutoIDs.EAST_NORTH_LIGHTNING));
	}

	private int roarTargetFor(SectionConfig config)
	{
		// Every roar phase is TOTAL_SECTION_1_ROARS - shadowRushUses (floored at 1): each
		// Shadow Rush used so far has dropped rocks that eat one tile off the next lane.
		// Round 1 runs before any rush (shadowRushUses == 0), so it gets the full count.
		// The scripted zigzag obeys the same rule -- when fewer than its full length, we
		// just roar the first N tiles of the path (advanceSection1Anchor clamps the index).
		int target = Math.max(1, LeviathanAutoIDs.TOTAL_SECTION_1_ROARS - shadowRushUses);
		if (config == EAST_LIGHTNING_P2_ZIGZAG_CONFIG)
			target = Math.min(target, LeviathanAutoIDs.EAST_LIGHTNING_P2_ROAR_PATH.length);
		return target;
	}

	private void beginCycle(int cycle, SectionConfig config)
	{
		Section2Phase previous = section2Phase;
		cycleIndex = cycle;
		loggedUnmappedEnd = false;
		activeSectionConfig = config;
		section1AnchorTemplate = config.startTile();
		section1RoarCount = 0;
		section1RoarTarget = roarTargetFor(config);
		// Scripted zigzag path for the first east-lightning follow-up; null otherwise.
		if (config == EAST_LIGHTNING_P2_ZIGZAG_CONFIG)
		{
			scriptedRoarPath = LeviathanAutoIDs.EAST_LIGHTNING_P2_ROAR_PATH;
			scriptedRoarIdx = 0;
		}
		else
		{
			scriptedRoarPath = null;
			scriptedRoarIdx = 0;
		}
		lastSouthStepTick = -100;
		shadowRushCast = false;
		shadowRushCastConfirmed = false;
		spineAttackDone = false;
		boulderPhaseActive = false;
		lightningPhaseActive = false;
		lightningChargeMsgTick = -100;
		boulderProjectileCount = 0;
		seenBoulderProjectileIdentities.clear();
		activeBoulderCluster = null;
		firedBoulderAttackIdxs.clear();
		boulderBackfillAttackDone = false;
		boulderWalkbackAttackDone = false;
		activeLightningEnd = null;
		lightningOtherEnd = null;
		wasOnLightningCenterLastTick = false;
		phase2EndSignalSeen = false;
		phase2EndResolved = false;
		resolvedNextConfig = null;
		if (cycle == 0)
		{
			section2Phase = Section2Phase.UNKNOWN;  // classify from first roar
		}
		else if (previous == Section2Phase.BOULDER)
		{
			section2Phase = Section2Phase.LIGHTNING;
		}
		else if (previous == Section2Phase.LIGHTNING)
		{
			section2Phase = Section2Phase.BOULDER;
		}
		else
		{
			section2Phase = Section2Phase.UNKNOWN;  // prior unclassified -- fall back to classify
		}
	}

	/**
	 * Post-spine phase-2 destination (template coords). After the spine attack
	 * fires, the planner routes here depending on which section-2 phase was
	 * classified: LIGHTNING -> {@link LeviathanAutoIDs#EAST_SOUTH_LIGHTNING},
	 * BOULDER -> active boulder cluster's current dodge tile. Returns null while
	 * section2Phase is still UNKNOWN so the planner holds position instead of
	 * guessing wrong.
	 */
	private WorldPoint postSpineTargetTemplate()
	{
		switch (section2Phase)
		{
			case LIGHTNING:
				// Phase-2 end: look up next cycle's start tile via dynamic lookup,
				// keyed on activeLightningEnd.
				if (phase2EndResolved)
				{
					return resolvedNextConfig != null ? resolvedNextConfig.startTile() : null;
				}
				// End signal seen but not yet resolved: oscillation is stopped, so hold
				// here for the 1-2 ticks until resolution routes us to the next section.
				if (phase2EndSignalSeen) return null;
				// Picked by pickLightningEndIfNeeded. Fallback to east-south until the
				// pick fires (first tick after spine attack).
				return activeLightningEnd != null ? activeLightningEnd : LeviathanAutoIDs.EAST_SOUTH_LIGHTNING;
			case BOULDER:
				// After the smoke-blast resolves, phase 2 is over -- look up where to
				// walk next from PHASE2_END_TO_NEXT_SECTION keyed on the backfill tile.
				// If unmapped, return null (stall). The cycle-transition trigger in
				// onGameTick calls beginCycle once the player arrives.
				if (phase2EndResolved)
				{
					return resolvedNextConfig != null ? resolvedNextConfig.startTile() : null;
				}
				// Once boulder phase has actually started (ANIM 10288 seen), the dodge
				// sequence drives the destination each new projectile. Before it starts,
				// we hold at the active cluster's position 1 (the nearest cluster to
				// where we landed the spine shot).
				WorldPoint dodge = currentBoulderDodgeTile();
				if (dodge != null) return dodge;
				return activeBoulderCluster != null ? activeBoulderCluster[0] : null;
			default:        return null;
		}
	}

	/**
	 * Current section-2 destination template coord. Pre-spine-attack: the spine
	 * tile from {@link #postCastTargetTemplate()}. Post-spine-attack: the phase-2
	 * tile from {@link #postSpineTargetTemplate()}. Caller must have already
	 * confirmed shadowRushCast && shadowRushCastConfirmed.
	 */
	private WorldPoint currentSection2TargetTemplate()
	{
		return spineAttackDone ? postSpineTargetTemplate() : postCastTargetTemplate();
	}

	/**
	 * Scan projectile records this tick, dedupe by (startTick, id), and increment
	 * {@link #boulderProjectileCount} for each new boulder projectile observed.
	 * Called every tick during boulder phase.
	 */
	private void countBoulderProjectiles(int tick)
	{
		for (MirrorState.ProjectileRecord pr : mirror.getProjectileRecords())
		{
			if (pr.id != LeviathanAutoIDs.PROJ_BOULDER) continue;
			int identity = (pr.startTick << 16) ^ (pr.id & 0xFFFF);
			if (!seenBoulderProjectileIdentities.add(identity)) continue;
			boulderProjectileCount++;
			WorldPoint next = currentBoulderDodgeTile();
		}
	}

	/**
	 * Resolve the current boulder-dodge destination tile (template coords).
	 * Reads activeBoulderCluster indexed by boulderProjectileCount/2 (capped). From
	 * the current projectile count to find the most recent non-null entry -- that's
	 * the last tile the sequence told us to be on. Returns null if boulder phase
	 * isn't active or the count is out of range.
	 */
	private WorldPoint currentBoulderDodgeTile()
	{
		if (!boulderPhaseActive) return null;
		if (activeBoulderCluster == null) return null;
		// Walkback path (safe-gap clusters only): after backfill attack, route back
		// to cluster[4] (pos 5) for the walkback attack, then hold there.
		if (boulderBackfillAttackDone)
		{
			return activeBoulderCluster[4];
		}
		// Pre-walk by 1 tick: use (count + 1) / 2 instead of count / 2 so the planner
		// routes to the NEXT dodge position one tick before the count-threshold tick.
		// OSRS can't walk and attack on the same tick -- a walkTo click clears the
		// attack queue. Pre-walking means by the time the count hits an attack-idx
		// threshold, the player is already standing on the attack tile and
		// tryFireBoulderAttack can fire on-time (no 1-tick lag).
		int pos = Math.min((boulderProjectileCount + 1) / 2, activeBoulderCluster.length - 1);
		return activeBoulderCluster[pos];
	}

	/** True if the active cluster uses walkback after idx-5 attack.
	 *  See {@link LeviathanAutoIDs#BOULDER_CLUSTER_WALKBACK}. */
	private boolean activeClusterWalkback()
	{
		if (activeBoulderCluster == null) return false;
		for (int i = 0; i < LeviathanAutoIDs.BOULDER_CLUSTERS.length; i++)
		{
			if (LeviathanAutoIDs.BOULDER_CLUSTERS[i] == activeBoulderCluster)
			{
				return LeviathanAutoIDs.BOULDER_CLUSTER_WALKBACK[i];
			}
		}
		return false;
	}

	private int[] activeClusterAttackIdxs()
	{
		if (activeBoulderCluster == null) return LeviathanAutoIDs.BOULDER_ATTACK_IDXS_UNSAFE_GAP;
		for (int i = 0; i < LeviathanAutoIDs.BOULDER_CLUSTERS.length; i++)
		{
			if (LeviathanAutoIDs.BOULDER_CLUSTERS[i] == activeBoulderCluster)
			{
				return LeviathanAutoIDs.BOULDER_CLUSTER_ATTACK_IDXS[i];
			}
		}
		return LeviathanAutoIDs.BOULDER_ATTACK_IDXS_UNSAFE_GAP;
	}

	/**
	 * Choose the closest boulder cluster to the player's current INSTANCE tile.
	 * Called the first tick after the spine attack lands in a BOULDER phase; the
	 * cluster then stays locked in for the whole boulder phase. Chebyshev distance
	 * from player to each cluster's position-1 tile (in instance coords), min wins.
	 */
	// =========================================================================
	// Enrage phase: triggered by five GFX_ENRAGE_PLUS (2298) spotanims appearing
	// in a `+` shape. The arm-ends mark the four quadrant corners; the Abyssal
	// Pathfinder (id 12220, 3x3) spawns in one quadrant. The quadrant drives
	// which side of the arena square to run to.
	//
	// TODO (needs info from user):
	//   - Exact tile coords of the five + arms (we may be able to detect quadrant
	//     purely from the pathfinder NPC's world location, without reading GFX).
	//   - Mapping of quadrant -> safe side (destination tile) to run to.
	//   - Enrage phase end condition (pathfinder death? specific anim?)
	//   - Attack behaviour during enrage (attack pathfinder? Leviathan? neither?)
	// =========================================================================
	private void detectEnragePhaseIfNeeded(int tick)
	{
		if (enragePhaseActive) return;
		Iterable<GraphicsObject> gfx = client.getGraphicsObjects();
		if (gfx == null) return;
		boolean seen = false;
		for (GraphicsObject g : gfx)
		{
			if (g != null && g.getId() == LeviathanAutoIDs.GFX_ENRAGE_PLUS) { seen = true; break; }
		}
		if (!seen) return;
		// Edge case: the enrage '+' markers (GFX_ENRAGE_PLUS) can appear while Leviathan is still
		// mid smoke-blast (ANIM_SMOKE_BLAST, 10290) from the boulder-phase finisher. Committing to
		// the enrage approach now means we start walking before that smoke attack lands and eat it.
		// Defer to the next tick and re-check: as long as the smoke blast is animating we hold, and
		// boulder-phase routing keeps us parked on the cluster until it resolves.
		if (leviathan != null && leviathan.getAnimation() == LeviathanAutoIDs.ANIM_SMOKE_BLAST)
		{
			return;
		}
		enragePhaseActive = true;
		// Boulder/lightning are over the moment enrage starts. Clear their state so
		// their flags don't suppress attacks or route into stale targets.
		boulderPhaseActive = false;
		lightningPhaseActive = false;
		lightningChargeMsgTick = -100;
		activeBoulderCluster = null;
		activeLightningEnd = null;
		// Snapshot the 2298 '+' footprint NOW, at the instant enrage begins, as the committed approach
		// target: we run to this fixed 3x3 from wherever we are rather than re-chasing the moving NPC.
		WorldPoint plusSw = enragePlusFootprintSW();
		if (plusSw != null) enrageApproachTargetTemplate = InstanceCoords.toTemplate(client, plusSw);
		// Resolve quadrant and stash pathfinder.
		resolveEnrageQuadrantAndPathfinder();
	}

	private void resolveEnrageQuadrantAndPathfinder()
	{
		// Find the Abyssal Pathfinder NPC if it has spawned.
		for (net.runelite.api.NPC n : client.getNpcs())
		{
			if (n != null && n.getId() == LeviathanAutoIDs.NPC_ABYSSAL_PATHFINDER)
			{
				abyssalPathfinder = n;
				break;
			}
		}
		// Classify quadrant from the pathfinder's center tile relative to the arena
		// center. Fall back to Leviathan's position if the pathfinder hasn't spawned
		// yet this tick (user: "even using leviathan location is good enough").
		net.runelite.api.coords.WorldPoint ref = null;
		if (abyssalPathfinder != null)
		{
			net.runelite.api.coords.WorldPoint sw = abyssalPathfinder.getWorldLocation();
			if (sw != null)
			{
				// 3x3 NPC: center = SW corner + (1, 1).
				ref = new net.runelite.api.coords.WorldPoint(sw.getX() + 1, sw.getY() + 1, sw.getPlane());
			}
		}
		if (ref == null && leviathan != null)
		{
			ref = leviathan.getWorldLocation();
		}
		if (ref == null) { enrageQuadrant = EnrageQuadrant.UNKNOWN; return; }

		// Translate ARENA_CENTER_TEMPLATE into instance space so we can compare
		// against ref (which is instance-space).
		net.runelite.api.coords.WorldPoint centerInstance =
			InstanceCoords.fromTemplate(client, LeviathanAutoIDs.ARENA_CENTER_TEMPLATE);
		if (centerInstance == null) { enrageQuadrant = EnrageQuadrant.UNKNOWN; return; }

		boolean west  = ref.getX() <  centerInstance.getX();
		boolean north = ref.getY() >  centerInstance.getY();
		if (north && west)      enrageQuadrant = EnrageQuadrant.NW;
		else if (north)         enrageQuadrant = EnrageQuadrant.NE;
		else if (west)          enrageQuadrant = EnrageQuadrant.SW;
		else                    enrageQuadrant = EnrageQuadrant.SE;
	}

	/**
	 * Set of all lightning-lane tile keys ((templateX << 32) | templateY) across the four
	 * lanes. The lanes form the four sides of a square around arena center, and any
	 * tile within the pathfinder's 3x3 that lands on one of these is "unusable" --
	 * we skip it when choosing a stand-tile during enrage.
	 */
	private static final java.util.Set<Long> LIGHTNING_LANE_TEMPLATE_KEYS = buildLightningLaneKeys();

	private static java.util.Set<Long> buildLightningLaneKeys()
	{
		java.util.Set<Long> keys = new java.util.HashSet<>();
		for (WorldPoint[] lane : LeviathanAutoIDs.LIGHTNING_LANES)
		{
			for (WorldPoint tile : lane)
			{
				keys.add(((long) tile.getX() << 32) | (tile.getY() & 0xffffffffL));
			}
		}
		// Also blacklist the four corner tiles where the lanes meet -- still enrage melee range.
		for (WorldPoint tile : LeviathanAutoIDs.ENRAGE_CORNER_BLACKLIST)
		{
			keys.add(((long) tile.getX() << 32) | (tile.getY() & 0xffffffffL));
		}
		return keys;
	}

	/** True if the given INSTANCE tile is a template tile on any lightning lane OR one of the four
	 *  enrage corner-blacklist tiles -- all off-limits during enrage. */
	private boolean isOnLightningLane(WorldPoint instanceTile)
	{
		if (instanceTile == null) return false;
		WorldPoint tpl = InstanceCoords.toTemplate(client, instanceTile);
		if (tpl == null) return false;
		long key = ((long) tpl.getX() << 32) | (tpl.getY() & 0xffffffffL);
		return LIGHTNING_LANE_TEMPLATE_KEYS.contains(key);
	}

	/** Pathfinder clockwise travel direction, derived from its position on the
	 * rectangle whose corners are the ENRAGE_PATH_* constants. */
	private enum PathfinderDir { EAST, SOUTH, WEST, NORTH }

	/** Nearest (Chebyshev to {@code current}) tile in {@code cands} that is safe at arrival,
	 *  off every lightning lane, and accessible. Null if none qualifies. */
	private WorldPoint nearestUsableEnrageTile(java.util.List<WorldPoint> cands, WorldPoint current, int tick)
	{
		WorldPoint best = null; int bestDist = Integer.MAX_VALUE;
		for (WorldPoint cand : cands)
		{
			if (isDestinationHazardAtArrival(cand, current, tick)) continue;
			if (isOnLightningLane(cand)) continue;
			WorldPoint ctpl = InstanceCoords.toTemplate(client, cand);
			if (ctpl != null && LeviathanAutoIDs.isInaccessibleTemplate(ctpl)) continue;
			int d = Math.max(Math.abs(cand.getX() - current.getX()), Math.abs(cand.getY() - current.getY()));
			if (d < bestDist) { bestDist = d; best = cand; }
		}
		return best;
	}

	private PathfinderDir classifyPathfinderDirection(WorldPoint pathfinderCenterInstance)
	{
		if (pathfinderCenterInstance == null) return PathfinderDir.EAST;
		WorldPoint tpl = InstanceCoords.toTemplate(client, pathfinderCenterInstance);
		if (tpl == null) return PathfinderDir.EAST;
		int dTop    = Math.abs(tpl.getY() - LeviathanAutoIDs.ENRAGE_PATH_TOP);
		int dRight  = Math.abs(tpl.getX() - LeviathanAutoIDs.ENRAGE_PATH_RIGHT);
		int dBottom = Math.abs(tpl.getY() - LeviathanAutoIDs.ENRAGE_PATH_BOTTOM);
		int dLeft   = Math.abs(tpl.getX() - LeviathanAutoIDs.ENRAGE_PATH_LEFT);
		int min = Math.min(Math.min(dTop, dRight), Math.min(dBottom, dLeft));
		if (min == dTop)    return PathfinderDir.EAST;
		if (min == dRight)  return PathfinderDir.SOUTH;
		if (min == dBottom) return PathfinderDir.WEST;
		return PathfinderDir.NORTH;
	}

	/** Lead-score for a candidate tile: higher = further forward along dir. */
	private static int leadScore(int x, int y, int cx, int cy, PathfinderDir dir)
	{
		switch (dir)
		{
			case EAST:  return x - cx;
			case WEST:  return cx - x;
			case SOUTH: return cy - y;
			case NORTH: return y - cy;
			default:    return 0;
		}
	}

	/**
	 * Enrage LOS check. Returns true if a permanent rock sits between the
	 * candidate stand-tile and Leviathan.
	 * <p>
	 * Enrage stand-tiles are on the four cardinal sides of center. Which axis is
	 * "dominant" (the one the shot travels along) is picked from the player's
	 * delta to arena center:
	 * <ul>
	 *   <li>East/West of center (|dx| > |dy|): LOS is horizontal, rocks block when
	 *     they share Y with the candidate AND their X is between candidate.x and
	 *     Leviathan.x.</li>
	 *   <li>North/South of center (|dy| >= |dx|): LOS is vertical, rocks block
	 *     when they share X with the candidate AND their Y is between.</li>
	 * </ul>
	 * Candidate tile and tracked rocks are both TEMPLATE coords (via
	 * InstanceCoords.toTemplate), so arena rotation doesn't need special-casing.
	 */
	private boolean isLosBlockedFromEnrageTile(WorldPoint candidateInstance)
	{
		if (candidateInstance == null) return false;
		if (trackedRockDropTemplates.isEmpty()) return false;
		WorldPoint candTpl = InstanceCoords.toTemplate(client, candidateInstance);
		if (candTpl == null) return false;
		int cx = candTpl.getX(), cy = candTpl.getY();
		int lx = LeviathanAutoIDs.ARENA_CENTER_TEMPLATE.getX();
		int ly = LeviathanAutoIDs.ARENA_CENTER_TEMPLATE.getY();
		int dx = Math.abs(cx - lx);
		int dy = Math.abs(cy - ly);
		boolean horizontal = dx >= dy;
		for (WorldPoint rock : trackedRockDropTemplates)
		{
			if (rock.getPlane() != candTpl.getPlane()) continue;
			if (horizontal)
			{
				if (rock.getY() != cy) continue;
				int xMin = Math.min(cx, lx), xMax = Math.max(cx, lx);
				if (rock.getX() > xMin && rock.getX() < xMax) return true;
			}
			else
			{
				if (rock.getX() != cx) continue;
				int yMin = Math.min(cy, ly), yMax = Math.max(cy, ly);
				if (rock.getY() > yMin && rock.getY() < yMax) return true;
			}
		}
		return false;
	}

	/**
	 * Predicted pathfinder SW corner 1 tick from now (shifts by pathfinder dir).
	 * Pathfinder moves 1 tile/tick clockwise around the arena rectangle.
	 */
	private WorldPoint predictPathfinderSwNextTick(WorldPoint sw, PathfinderDir dir)
	{
		switch (dir)
		{
			case EAST:  return new WorldPoint(sw.getX() + 1, sw.getY(),     sw.getPlane());
			case WEST:  return new WorldPoint(sw.getX() - 1, sw.getY(),     sw.getPlane());
			case SOUTH: return new WorldPoint(sw.getX(),     sw.getY() - 1, sw.getPlane());
			case NORTH: return new WorldPoint(sw.getX(),     sw.getY() + 1, sw.getPlane());
			default:    return sw;
		}
	}

	/**
	 * Nearest usable tile of the committed enrage approach target -- the 3x3 the 2298 '+' marked when
	 * enrage began ({@link #enrageApproachTargetTemplate}), or {@code fallbackSw} (the live footprint) if
	 * we never captured it. INSTANCE coords; falls back to the 3x3 center if no cell is usable.
	 */
	private WorldPoint approachTargetNearestTile(WorldPoint current, int tick, WorldPoint fallbackSw)
	{
		// Once the pathfinder NPC is live, approach ITS CURRENT footprint (fallbackSw) by shortest path.
		// The 2298 snapshot only guides the PRE-SPAWN run -- it goes stale the moment the pathfinder slides
		// off its spawn tile, and chasing it then runs us to an empty / unreachable spot (the red X click).
		boolean npcLive = abyssalPathfinder != null && abyssalPathfinder.getWorldLocation() != null;
		WorldPoint sw = (!npcLive && enrageApproachTargetTemplate != null)
			? InstanceCoords.fromTemplate(client, enrageApproachTargetTemplate)
			: fallbackSw;
		if (sw == null) return null;
		java.util.List<WorldPoint> cells = new java.util.ArrayList<>();
		for (int x = sw.getX(); x <= sw.getX() + 2; x++)
			for (int y = sw.getY(); y <= sw.getY() + 2; y++)
				cells.add(new WorldPoint(x, y, sw.getPlane()));
		WorldPoint t = nearestUsableEnrageTile(cells, current, tick);
		if (t == null) t = new WorldPoint(sw.getX() + 1, sw.getY() + 1, sw.getPlane());
		return t;
	}

	/** True if `tile` sits inside the 3x3 footprint rooted at `sw`. */
	private static boolean inFootprint(WorldPoint tile, WorldPoint sw)
	{
		return tile.getX() >= sw.getX() && tile.getX() <= sw.getX() + 2
			&& tile.getY() >= sw.getY() && tile.getY() <= sw.getY() + 2
			&& tile.getPlane() == sw.getPlane();
	}

	/**
	 * Movement planner for enrage phase.
	 * <ol>
	 *   <li>If the pathfinder hasn't resolved yet, route to the quadrant-based
	 *     destination so we arrive where it will swing by.</li>
	 *   <li>Once the pathfinder is known, keep inside its 3x3 footprint while
	 *     it moves clockwise. If the current tile is inside AND the dodger
	 *     reports no incoming hazard on it, stay put.</li>
	 *   <li>Otherwise, pick the closest safe tile inside the 3x3. Safety =
	 *     dodger.isTileDangerousAt == false for this tick and the next two.</li>
	 *   <li>If every tile in the 3x3 is unsafe (shouldn't happen in practice),
	 *     fall back to the pathfinder's center -- better to eat a hit than to
	 *     leave the aggro zone.</li>
	 * </ol>
	 */
	private WorldPoint planEnrageTile(WorldPoint current, int tick)
	{
		// ESCAPE (highest priority): if we are standing ON a forbidden tile -- a lightning lane or one
		// of the four corner-blacklist tiles -- getting off it beats everything else (attacking, holding,
		// chasing the pathfinder). Leviathan melees/bites anything parked here during enrage, so step to
		// the nearest safe off-forbidden tile on the next possible tick. This covers both running to the
		// pathfinder and maintaining its footprint. Only if no escape tile exists do we fall through to
		// normal planning (whose scan also avoids these tiles).
		if (isOnLightningLane(current))
		{
			WorldPoint esc = enrageEscapeTile(current, tick);
			if (esc != null && !esc.equals(current)) return esc;
		}
		// Max-DPS short-circuit: if the weapon is off cooldown and the current tile
		// won't be hit THIS tick, return null so step() falls through to the attack
		// branch. We'll reposition on the next cooldown-blocked tick (walkTo cancels
		// any queued attack, so sending a walk click on an attackable tick would
		// burn the shot). t+1/t+2 hazards are tolerated here -- they'll force a
		// reposition on the next non-attack tick, before the hazard actually lands.
		if (isInsidePathfinderFootprint()
			&& canAttack(tick) && !dodger.isTileDangerousAt(current, tick) && !isOnLightningLane(current)
			&& !isLosBlockedFromEnrageTile(current))
		{
			return null;
		}

		// Refresh pathfinder reference each tick -- it moves and may spawn late.
		if (abyssalPathfinder == null || abyssalPathfinder.getWorldLocation() == null)
		{
			resolveEnrageQuadrantAndPathfinder();
		}

		// Effective 3x3 SW corner: the pathfinder NPC once it has spawned, otherwise the
		// GFX_ENRAGE_PLUS (2298) '+' bounding box, which marks the SAME 3x3 the pathfinder
		// will occupy. 2298 is NOT a hazard, so we move INTO its 3x3 ahead of the spawn
		// instead of waiting. If neither is visible yet, hold.
		WorldPoint sw = effectiveEnrageFootprintSW();
		if (sw == null) return null;
		int minX = sw.getX(), maxX = sw.getX() + 2;
		int minY = sw.getY(), maxY = sw.getY() + 2;
		int plane = sw.getPlane();

		// Pre-spawn: only the static GFX_ENRAGE_PLUS '+' is up (no pathfinder NPC yet).
		// Nothing is moving, so don't use the lead-score / next-tick prediction (those
		// aim us at the forward edge of a MOVING pathfinder). Just step to the tile in
		// the 3x3 NEAREST our current position so we're already inside when it spawns.
		boolean npcSpawned = abyssalPathfinder != null && abyssalPathfinder.getWorldLocation() != null;
		if (!npcSpawned)
		{
			if (inFootprint(current, sw)) return null;  // already inside -> hold & attack
			// Run to the committed 2298 footprint (snapshotted when enrage began) by the shortest lane-safe
			// path from wherever we are -- no travel-direction bias. guardEnrageApproach keeps us off lanes.
			return guardEnrageApproach(current, approachTargetNearestTile(current, tick, sw), tick);  // null -> hold
		}

		// Predict the pathfinder's next-tick footprint and decide where to click:
		//   - If we'll still be inside next tick AND the tile is safe/off-lane,
		//     stay put (planNextTile returns null -> attack branch fires).
		//   - Else prefer the "leading + 1" tile in the direction of travel. We
		//     move toward it while the pathfinder slides forward, and we meet on
		//     the leading edge of its next-tick footprint.
		PathfinderDir dir = classifyPathfinderDirection(new WorldPoint(sw.getX() + 1, sw.getY() + 1, plane));
		WorldPoint swNext = predictPathfinderSwNextTick(sw, dir);
		boolean insideNow = inFootprint(current, sw);
		boolean insideNextTick = inFootprint(current, swNext);
		boolean cooldownReady = canAttack(tick);
		if (insideNextTick && !isEnrageTileHazard(current, tick) && !isOnLightningLane(current)
			&& (!cooldownReady || !isLosBlockedFromEnrageTile(current))
			&& leadScore(current.getX(), current.getY(), sw.getX() + 1, sw.getY() + 1, dir) >= 1)
		{
			// Hold & attack ONLY at the leading edge or ahead. From center/trailing we fall through to the scan
			// and advance to lead+1 -- otherwise the pathfinder's 1-tile slide keeps shoving us to the back.
			return null;
		}

		// FAR APPROACH: only for a genuine cross-arena run (enrage start, or we've lagged well behind) --
		// nearest-tile targeting. When we've merely fallen 1-2 tiles off the BACK, the nearest tile is the
		// trailing/center one and we'd just chase the pathfinder; hand those close cases to the lead+1 scan
		// below instead. (dist > 4 ~= more than 2 tiles past the footprint edge = a real cross-arena gap.)
		int ctrDist = Math.max(Math.abs(current.getX() - (sw.getX() + 1)), Math.abs(current.getY() - (sw.getY() + 1)));
		if (!insideNow && !insideNextTick && ctrDist > 4)
		{
			WorldPoint t = approachTargetNearestTile(current, tick, sw);
			if (t != null && !t.equals(current)) return guardEnrageApproach(current, t, tick);
		}

		// Scan the 25 tiles for a stand-tile. Scoring (high to low priority):
		//   1. Lead-score: how far forward along the pathfinder's direction. The
		//      pathfinder moves clockwise, so standing ahead of center means we stay
		//      inside the 3x3 for more ticks before falling off the trailing edge.
		//   2. Chebyshev distance to our current tile -- minimize travel.
		// Hard filters: hazards (3-tick lookahead) and lightning-lane tiles.
		int cx = sw.getX() + 1, cy = sw.getY() + 1;
		// Extend the scan ONE tile past the leading edge in the travel direction (the "lead+1" line): standing
		// there puts us on the leading column AFTER the pathfinder slides, so we ride lead -> center -> trailing
		// instead of being shoved to the trailing column every tick. leadScore makes those tiles win; lane tiles
		// stay hard-excluded and the pick is routed lane-safely below, so we never path into bite range.
		int sMinX = minX, sMaxX = maxX, sMinY = minY, sMaxY = maxY;
		switch (dir)
		{
			case EAST:  sMaxX = maxX + 1; break;
			case WEST:  sMinX = minX - 1; break;
			case SOUTH: sMinY = minY - 1; break;
			case NORTH: sMaxY = maxY + 1; break;
		}
		WorldPoint best = null;
		int bestLead = Integer.MIN_VALUE;
		boolean bestSafe = false;
		int bestDist = Integer.MAX_VALUE;
		for (int x = sMinX; x <= sMaxX; x++)
		{
			for (int y = sMinY; y <= sMaxY; y++)
			{
				WorldPoint cand = new WorldPoint(x, y, plane);
				WorldPoint candTpl = InstanceCoords.toTemplate(client, cand);
				if (candTpl == null || LeviathanAutoIDs.isInaccessibleTemplate(candTpl)) continue; // lead+1 may be out of bounds
				if (isOnLightningLane(cand)) continue; // lightning-square sides are off-limits (enrage melee range)
				if (cooldownReady && isLosBlockedFromEnrageTile(cand)) continue; // only matters when we're shooting
				int lead = leadScore(x, y, cx, cy, dir);
				boolean safe = !isDestinationHazardAtArrival(cand, current, tick);
				int d = Math.max(Math.abs(x - current.getX()), Math.abs(y - current.getY()));
				// Forward-ness (leadScore) DOMINATES: we hold the leading edge in the travel direction and never
				// retreat behind it to dodge. Safety is only a tiebreak AMONG equally-forward tiles (prefer a safe
				// leading-edge tile; else stay on the leading edge and eat the hit), then nearest.
				boolean better = lead > bestLead
					|| (lead == bestLead && safe && !bestSafe)
					|| (lead == bestLead && safe == bestSafe && d < bestDist);
				if (better)
				{
					best = cand;
					bestLead = lead;
					bestSafe = safe;
					bestDist = d;
				}
			}
		}

		if (best == null)
		{
			// Every tile is a lane / LOS-blocked. Fall back to center rather than leave the zone.
			WorldPoint centerFallback = new WorldPoint(cx, cy, plane);
			return current.equals(centerFallback) ? null : centerFallback;
		}
		if (current.equals(best)) return null;
		return guardEnrageApproach(current, best, tick);
	}

	/**
	 * Apply the path-based transit dodge to an enrage APPROACH move -- running to the
	 * pathfinder spot, which can be a cross-arena traversal. If continuing straight to
	 * {@code target} would put us STANDING on a live hazard at its impact tick (checked
	 * along the real walked path), sidestep this tick; the sidestep breaks the walk dedupe
	 * so {@code target} is re-clicked next tick and the run resumes. Short in-footprint hops
	 * never trip this -- it only fires when a hazard actually lies on the route.
	 */
	private WorldPoint guardEnrageApproach(WorldPoint current, WorldPoint target, int tick)
	{
		if (target == null || target.equals(current)) return target;
		if (transitStepWillBeHit(current, target, tick, this::enrageApproachPassable))
		{
			WorldPoint aside = findTransitDodge(current, target, tick, this::enrageApproachPassable);
			if (aside != null && !aside.equals(current))
			{
				// NEVER dodge BACKWARD during the approach. At the sealed blacklist-corner bottleneck the only
				// arrival-safe tile can be behind us (a rock blocks the single forward tile). Stepping there and
				// forward again next tick as the rock clears is the east-side N/S oscillation. If the dodge loses
				// ground toward the target, prefer to HOLD on our current tile (when it is safe to stand on) and
				// let the rock land/clear; take the backward dodge only as a last resort if standing here is unsafe.
				int curD   = Math.max(Math.abs(current.getX() - target.getX()), Math.abs(current.getY() - target.getY()));
				int asideD = Math.max(Math.abs(aside.getX() - target.getX()),   Math.abs(aside.getY() - target.getY()));
				if (asideD <= curD) return aside;   // dodge only when it does not lose ground
				if (!dodger.isTileDangerousAt(current, tick + 1)) return current;  // backward dodge -> hold instead
				return aside;                       // holding is unsafe -> accept the backward step
			}
			// Our next step lands on a hazard and no run-move toward the goal is arrival-safe: HOLD on the
			// current (safe) tile and re-evaluate next tick -- never walk the straight waypoint into the hazard.
			return current;
		}
		// Click the farthest LANE-FREE waypoint toward the target rather than the target itself: a direct
		// click lets the server path us straight THROUGH a lightning lane (Leviathan's melee/bite range).
		// When the route is already a straight, lane-free line this waypoint IS the target -- one smooth
		// click -- so it only bends us around a lane when one actually lies in the path (the redirect we
		// want), re-evaluating at each bend. The single dodge above still handles a hazard on the route.
		WorldPoint wp = nextLaneSafeWaypoint(current, target);
		return wp != null ? wp : target;
	}

	/**
	 * Farthest waypoint along the lane-free BFS route from {@code current} to {@code dest} that
	 * lies on the route's INITIAL straight run (one heading). Clicking it makes the server walk
	 * exactly that segment -- which, being a prefix of the lane-avoiding BFS path, never enters
	 * a lightning lane and never corner-cuts through one. Returns null if no lane-free route.
	 */
	private WorldPoint nextLaneSafeWaypoint(WorldPoint current, WorldPoint dest)
	{
		java.util.List<WorldPoint> path = net.runelite.client.plugins.autoBossCore.combat.TilePathPredictor.path(
			client.getTopLevelWorldView(), current, dest, this::enrageApproachPassable, this::inArenaInstance,
			ARENA_PATH_BUDGET);
		if (path.size() < 2) return null;
		// Click the FARTHEST tile on the lane-free route reachable in a single straight run -- the last path
		// tile whose straight line from `current` is itself all lane-free + accessible. The server walks that
		// clear line directly (no lane crossing), so one click covers the whole run instead of re-clicking
		// every couple of tiles; it only shortens when a lane actually bends the route.
		WorldPoint best = path.get(1);
		for (int i = 2; i < path.size(); i++)
		{
			if (!straightLineClear(current, path.get(i))) break;
			best = path.get(i);
		}
		return best;
	}

	/** True if the OSRS straight run from {@code a} to {@code b} (diagonal until axis-aligned, then
	 *  cardinal) stays entirely on lane-free, accessible tiles -- so a single click walks it without
	 *  the server detouring through a lightning lane. Used only by the enrage approach waypoint. */
	private boolean straightLineClear(WorldPoint a, WorldPoint b)
	{
		int x = a.getX(), y = a.getY(), plane = a.getPlane();
		while (x != b.getX() || y != b.getY())
		{
			x += Integer.signum(b.getX() - x);
			y += Integer.signum(b.getY() - y);
			WorldPoint t = new WorldPoint(x, y, plane);
			if (!enrageApproachPassable(t)) return false;
			WorldPoint tpl = InstanceCoords.toTemplate(client, t);
			if (tpl == null || LeviathanAutoIDs.isInaccessibleTemplate(tpl)) return false;
		}
		return true;
	}

	/**
	 * Nearest tile to step to when we are standing on a forbidden enrage tile (a lightning lane or a
	 * corner-blacklist tile). Spirals outward by Chebyshev radius and returns the closest tile that is
	 * accessible, passable, off every forbidden tile, reachable by a lane-free straight run, and not a
	 * hazard on arrival. Returns null if none is found within the search radius.
	 */
	private WorldPoint enrageEscapeTile(WorldPoint current, int tick)
	{
		return enrageEscapeTile(current, tick, null);
	}

	/**
	 * As {@link #enrageEscapeTile(WorldPoint, int)}, but when {@code goal} is non-null the tiles in the
	 * closest usable ring are tie-broken toward {@code goal} -- so we step OFF the blacklist tile in the
	 * direction we actually want to run (e.g. toward the next roar start) rather than an arbitrary side.
	 */
	private WorldPoint enrageEscapeTile(WorldPoint current, int tick, WorldPoint goal)
	{
		int plane = current.getPlane();
		for (int r = 1; r <= 4; r++)
		{
			WorldPoint best = null;
			int bestGoalDist = Integer.MAX_VALUE;
			for (int dx = -r; dx <= r; dx++)
			{
				for (int dy = -r; dy <= r; dy++)
				{
					if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue; // ring at radius r only
					WorldPoint cand = new WorldPoint(current.getX() + dx, current.getY() + dy, plane);
					WorldPoint tpl = InstanceCoords.toTemplate(client, cand);
					if (tpl == null || LeviathanAutoIDs.isInaccessibleTemplate(tpl)) continue;
					if (isOnLightningLane(cand)) continue;                 // off both lanes AND corners
					if (!transitPassable(cand)) continue;
					if (!straightLineClear(current, cand)) continue;       // don't cross a lane getting off
					if (isDestinationHazardAtArrival(cand, current, tick)) continue;
					if (goal == null) return cand;                         // no bias -> first in this ring
					int gd = Math.max(Math.abs(cand.getX() - goal.getX()), Math.abs(cand.getY() - goal.getY()));
					if (gd < bestGoalDist) { best = cand; bestGoalDist = gd; }
				}
			}
			if (best != null) return best; // closest ring wins, biased toward goal
		}
		return null;
	}

	/** Strict hazard check (used when we're deciding whether to STAY on a tile):
	 *  reject if ANY of ticks N..N+2 would deal damage here. */
	private boolean isEnrageTileHazard(WorldPoint tile, int tick)
	{
		return dodger.isTileDangerousAt(tile, tick)
			|| dodger.isTileDangerousAt(tile, tick + 1)
			|| dodger.isTileDangerousAt(tile, tick + 2);
	}

	/** Loose hazard check for a CLICK destination: a rock may be visibly falling on
	 * the tile right now, but it only hurts us at its impact tick. We're safe to
	 * click as long as the impact doesn't coincide with our arrival; the next
	 * planner tick re-reads the dodger and routes us off before subsequent impacts.
	 * Travel estimate: Chebyshev tiles divided by 2 (run speed), rounded up, floored at 1. */
	private boolean isDestinationHazardAtArrival(WorldPoint dest, WorldPoint from, int tick)
	{
		if (dest == null || from == null) return false;
		int dx = Math.abs(dest.getX() - from.getX());
		int dy = Math.abs(dest.getY() - from.getY());
		int cheby = Math.max(dx, dy);
		// "On arrival" means the IMPACT tick we would actually be standing there. Cap the lookahead at
		// ARRIVAL_HAZARD_MAX_TICKS (~4 tiles at run speed): past that the dodger's rock predictions are
		// noisy and flip tick-to-tick, which made the far cross-arena approach reject/jitter its target
		// cell. A rock predicted 5-6 ticks out will have resolved before we get there anyway, and we
		// re-evaluate the real stand tile with the close-range scan once we are within ~2 tiles.
		int travelTicks = Math.min(ARRIVAL_HAZARD_MAX_TICKS, Math.max(1, (cheby + 1) / 2));
		return dodger.isTileDangerousAt(dest, tick + travelTicks);
	}

	// TEST FLAG: when >= 0, pickBoulderClusterIfNeeded() ignores proximity and forces
	// this cluster index every time. -1 = disabled (use normal closest-cluster logic).
	// Indexes: 0=NE, 1=SE, 2=SW, 3=NW (matches BOULDER_CLUSTERS).
	private static final int FORCE_BOULDER_CLUSTER_IDX = -1;  // -1 = proximity pick (SE for boulder-first c0, SW for lightning-first c1)

	private void pickBoulderClusterIfNeeded()
	{
		if (activeBoulderCluster != null) return;
		if (section2Phase != Section2Phase.BOULDER) return;
		if (!spineAttackDone) return;
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return;

		if (FORCE_BOULDER_CLUSTER_IDX >= 0)
		{
			int idx = FORCE_BOULDER_CLUSTER_IDX;
			if (!usedBoulderClusterIdxs.contains(idx))
			{
				activeBoulderCluster = LeviathanAutoIDs.BOULDER_CLUSTERS[idx];
				usedBoulderClusterIdxs.add(idx);
			}
			else
			{
			}
			return;
		}

		WorldPoint[] best = null;
		String bestName = null;
		int bestIdx = -1;
		int bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < LeviathanAutoIDs.BOULDER_CLUSTERS.length; i++)
		{
			if (usedBoulderClusterIdxs.contains(i)) continue;  // one-shot per fight
			// Never use NW as the FIRST boulder cluster. Once any cluster has been used
			// (used-set non-empty), NW is allowed again.
			if (usedBoulderClusterIdxs.isEmpty()
				&& LeviathanAutoIDs.BOULDER_CLUSTERS[i] == LeviathanAutoIDs.BOULDER_NW)
			{
				continue;
			}
			WorldPoint[] cluster = LeviathanAutoIDs.BOULDER_CLUSTERS[i];
			WorldPoint pos1Instance = InstanceCoords.fromTemplate(client, cluster[0]);
			if (pos1Instance == null) continue;
			int d = Math.max(Math.abs(pos1Instance.getX() - me.getX()),
							 Math.abs(pos1Instance.getY() - me.getY()));
			if (d < bestDist)
			{
				bestDist = d;
				best = cluster;
				bestName = LeviathanAutoIDs.BOULDER_CLUSTER_NAMES[i];
				bestIdx = i;
			}
		}
		if (best != null)
		{
			activeBoulderCluster = best;
			usedBoulderClusterIdxs.add(bestIdx);
		}
		else
		{
			// Edge case: all 4 clusters used. TODO: fallback behavior (reuse least-recent?
			// pick by raw proximity ignoring used-set? abort boulder dodge?). For now,
			// log once and leave activeBoulderCluster null -- routing will stall.
		}
	}

	/**
	 * Explicit boulder-phase attack trigger. Mirrors the lightning center-crossing
	 * pattern: natural canAttack is suppressed throughout boulder phase, and attacks
	 * are issued here at specific sequence positions. See
	 * {@link LeviathanAutoIDs#BOULDER_ATTACK_IDXS_SAFE_GAP} (safe gaps) or
	 * {@link LeviathanAutoIDs#BOULDER_ATTACK_IDXS_UNSAFE_GAP} (SE) for which
	 * cluster positions trigger a shot. Each position fires at most once per phase
	 * (dedupe via {@link #firedBoulderAttackIdxs}). Gated by canAttack so the shot
	 * only fires when cooldown is ready -- the index-5 "slip" may wait a tick or
	 * two after arrival while cooldown clears.
	 */
	private void tryFireBoulderAttack(int tick)
	{
		if (activeBoulderCluster == null || leviathan == null) return;
		if (phase2EndSignalSeen) return;  // once the end anim fires, stop attacking -- transition imminent
		if (boulderWalkbackAttackDone) return;  // walkback was the final shot

		boolean walkback = activeClusterWalkback();
		int[] attackIdxs = activeClusterAttackIdxs();

		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return;

		// --- Walkback attack path (safe-gap only, fires after backfill done) ---
		if (walkback && boulderBackfillAttackDone)
		{
			if (!canAttack(tick)) return;
			WorldPoint pos5Instance = InstanceCoords.fromTemplate(client, activeBoulderCluster[4]);
			if (pos5Instance == null || !me.equals(pos5Instance)) return;  // not yet walked back
			router.attackNpc(leviathan);
			lastAttackTick = tick;
			boulderWalkbackAttackDone = true;
			return;
		}

		// --- Normal count-threshold attack path ---
		int currentIdx = Math.min(boulderProjectileCount / 2, activeBoulderCluster.length - 1);
		boolean isAttackIdx = false;
		for (int ai : attackIdxs)
		{
			if (ai == currentIdx) { isAttackIdx = true; break; }
		}
		if (!isAttackIdx) return;
		if (firedBoulderAttackIdxs.contains(currentIdx)) return;
		if (!canAttack(tick)) return;
		WorldPoint target = InstanceCoords.fromTemplate(client, activeBoulderCluster[currentIdx]);
		if (target == null || !me.equals(target)) return;  // only fire when standing on the attack tile

		router.attackNpc(leviathan);
		lastAttackTick = tick;
		firedBoulderAttackIdxs.add(currentIdx);
		if (currentIdx == 5 && walkback) boulderBackfillAttackDone = true;  // trigger walkback routing only for walkback clusters
	}

	/**
	 * Choose which END of the active lightning lane to stand on. Lane is picked
	 * by cycle (east for even, west for odd). Picks whichever end is closer
	 * (Chebyshev, instance coords) to the player's tile; ties go to the north
	 * end. Called the first tick after the spine attack lands in a LIGHTNING phase;
	 * cached in {@link #activeLightningEnd} for the whole phase.
	 */
	private void pickLightningEndIfNeeded()
	{
		if (activeLightningEnd != null) return;
		if (section2Phase != Section2Phase.LIGHTNING) return;
		if (!spineAttackDone) return;
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return;

		WorldPoint northEnd;
		WorldPoint southEnd;
		if ((cycleIndex & 1) == 0)
		{
			northEnd = LeviathanAutoIDs.EAST_NORTH_LIGHTNING;
			southEnd = LeviathanAutoIDs.EAST_SOUTH_LIGHTNING;
		}
		else
		{
			northEnd = LeviathanAutoIDs.WEST_NORTH_LIGHTNING;
			southEnd = LeviathanAutoIDs.WEST_SOUTH_LIGHTNING;
		}

		WorldPoint nInstance = InstanceCoords.fromTemplate(client, northEnd);
		WorldPoint sInstance = InstanceCoords.fromTemplate(client, southEnd);
		int dN = nInstance == null ? Integer.MAX_VALUE
			: Math.max(Math.abs(nInstance.getX() - me.getX()), Math.abs(nInstance.getY() - me.getY()));
		int dS = sInstance == null ? Integer.MAX_VALUE
			: Math.max(Math.abs(sInstance.getX() - me.getX()), Math.abs(sInstance.getY() - me.getY()));
		activeLightningEnd = (dN <= dS) ? northEnd : southEnd;
		lightningOtherEnd = computeLightningFarTile(activeLightningEnd);
	}

	/**
	 * Oscillation distance (in tiles) for the lightning back-and-forth. Full lane
	 * span is 8 tiles (9 tiles inclusive, e.g. east lane y=6368..6376). For
	 * lightning phase we shrink to span 6 (7 tiles inclusive) so the attack
	 * cadence lines up with the weapon cooldown + run-on behavior:
	 * <ul>
	 *   <li>4-tick weapon: span 6. Starting idx 0 (y=6368) oscillates 6368 ↔
	 *       6374; starting idx 8 (y=6376) oscillates 6376 ↔ 6370. Shoots fire
	 *       at BOTH endpoints.</li>
	 *   <li>5-tick weapon: full lane one-way (handled by a separate code path --
	 *       TODO wire).</li>
	 * </ul>
	 */
	private int lightningOscillationTileDist()
	{
		// 4-tick: WEAPON_COOLDOWN*2 - 2 = 6 (span 6, endpoints 6 apart).
		// Cap at 8 so 5-tick's WEAPON_COOLDOWN*2-2=8 stays inside the lane.
		return Math.min(WEAPON_COOLDOWN * 2 - 2, 8);
	}

	/**
	 * Compute the FAR oscillation tile for the back-and-forth given a starting end.
	 * For COLUMN lanes (east x=2086 / west x=2076, varying Y), shifts Y toward center
	 * by {@link #lightningOscillationTileDist()}. For ROW lanes (north y=6377 / south
	 * y=6367, varying X), shifts X toward center. Returns null if the end's position
	 * doesn't match any known lane.
	 */
	private WorldPoint computeLightningFarTile(WorldPoint end)
	{
		if (end == null) return null;
		int dist = lightningOscillationTileDist();
		int x = end.getX();
		int y = end.getY();
		int plane = end.getPlane();
		boolean isColumnLane = (x == 2086 || x == 2076);
		boolean isRowLane    = (y == 6377 || y == 6367);
		if (isColumnLane)
		{
			// End Y is 6368 (south) -> shift north by dist. End Y is 6376 (north) -> shift south.
			int farY = (y <= 6372) ? y + dist : y - dist;
			return new WorldPoint(x, farY, plane);
		}
		if (isRowLane)
		{
			int farX = (x <= 2081) ? x + dist : x - dist;
			return new WorldPoint(farX, y, plane);
		}
		return null;
	}

	/**
	 * Center tile (position 3) of the active lightning lane. Pivot for the swap-on-crossing
	 * trigger: the player U-turns here on each traversal rather than reaching the ends.
	 * Returns the appropriate named CENTER constant based on which lane
	 * {@link #activeLightningEnd} belongs to, or null if no active end is picked.
	 */
	private WorldPoint currentLightningLaneCenter()
	{
		if (activeLightningEnd == null) return null;
		int x = activeLightningEnd.getX();
		int y = activeLightningEnd.getY();
		if (x == 2086) return LeviathanAutoIDs.EAST_CENTER_LIGHTNING;
		if (x == 2076) return LeviathanAutoIDs.WEST_CENTER_LIGHTNING;
		if (y == 6377) return LeviathanAutoIDs.NORTH_CENTER_LIGHTNING;
		if (y == 6367) return LeviathanAutoIDs.SOUTH_CENTER_LIGHTNING;
		return null;
	}

	/**
	 * Leviathan's current HP from the HP readout widget, or -1 if the widget isn't
	 * present/parseable this tick.
	 */
	private int bossCurrentHp()
	{
		// Boss HP HUD (shared BOSS_HP_COMPONENT, group 303/child 21) -- a "cur / max" fraction; we take the
		// numerator (current HP). Reads -1 when the bar is hidden; callers fall back to lastKnownBossHp.
		int[] hp = WidgetReader.fraction(client, WidgetReader.BOSS_HP_COMPONENT);
		return hp == null ? -1 : hp[0];
	}

	/** Drop any scheduled overhead swap and deactivate all three protect prayers. */
	private void turnPrayersOff(int tick)
	{
		prayers.clearScheduled();
		prayers.ensureOnlyOn(tick, java.util.Collections.emptySet(),
			net.runelite.client.plugins.autoBossCore.combat.PrayerSwapper.OVERHEAD_SET());
	}

	/** Phases where eating is allowed because we are NOT attacking, so a same-tick CC_OP eat
	 *  can't re-path us off an attack interaction: the post-Shadow-Rush STUN window (boss
	 *  stunned) and the enrage APPROACH run to the pathfinder (before we're in the footprint
	 *  attacking). Off during roars, lightning, boulder, in-footprint enrage, and transit back. */
	private boolean eatPhaseAllowed()
	{
		boolean shadowStun = shadowRushCast && !enragePhaseActive && !lightningPhaseActive
			&& !boulderPhaseActive && !phase2EndResolved;
		boolean enrageApproach = enragePhaseActive && !isInsidePathfinderFootprint();
		return shadowStun || enrageApproach;
	}

	private boolean canAttack(int tick)
	{
		if (leviathan == null) return false;
		if (tick - lastAttackTick < WEAPON_COOLDOWN) return false;
		// TODO: in-range + lock-broken check once gear + arena layout are known.
		return true;
	}

	// -----------------------------------------------------------------
	// Overlay.
	// -----------------------------------------------------------------

	private void updateOverlay()
	{
		if (overlay == null) return;
		overlay.setStep((active ? "ACTIVE " : "idle ") + phase.current().name());
		// Pin the classified section-2 phase to the fight overlay so it stays visible
		// after the one-shot "cast Shadow Rush (next: X)" step scrolls into history.
		// null while UNKNOWN clears the row via BaseOverlay#setStat contract.
		overlay.setStat("phase2",
			section2Phase == Section2Phase.UNKNOWN ? null : section2Phase.name());
		// Section-1 roar progress -- clears once section 1 is done so the row doesn't
		// stick around for the rest of the kill at "5 / 5".
		overlay.setStat("roars",
			isSection1Complete() ? null
				: section1RoarCount + " / " + section1RoarTarget);
	}
}
