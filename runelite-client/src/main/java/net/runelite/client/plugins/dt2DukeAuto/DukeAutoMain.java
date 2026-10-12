package net.runelite.client.plugins.dt2DukeAuto;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.autoBossCore.camera.ArenaCamera;
import net.runelite.client.plugins.autoBossCore.combat.ClickRouter;
import net.runelite.client.plugins.autoBossCore.combat.Hazard;
import net.runelite.client.plugins.autoBossCore.combat.PrayerSwapper;
import net.runelite.client.plugins.autoBossCore.combat.SchedulingTileDodger;
import net.runelite.client.plugins.autoBossCore.combat.TelegraphedHazards;
import net.runelite.client.plugins.autoBossCore.combat.TilePathPredictor;
import net.runelite.client.plugins.autoBossCore.consume.CombatBoostTracker;
import net.runelite.client.plugins.autoBossCore.consume.ConsumptionEngine;
import net.runelite.client.plugins.autoBossCore.consume.SupplyProfile;
import net.runelite.client.plugins.autoBossCore.phase.PhaseTracker;
import net.runelite.client.plugins.autoBossCore.state.InstanceCoords;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;
import net.runelite.client.plugins.autoBossCore.state.SceneScanner;
import net.runelite.client.plugins.autoBossCore.state.WidgetReader;
import net.runelite.client.plugins.autoBossCore.trip.TripState;
import net.runelite.client.plugins.autoBossCore.ui.BaseOverlay;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Duke Sucellus strategy. Owns the tick loop for the fight: hazard scanning, attack-style
 * prayer swapping, dodge decisions, supply consumption, attack maintenance.
 *
 * <p><b>STATUS: boilerplate skeleton.</b> The control flow and core wiring (phase tracker,
 * consumption, dodger, prayer swapper, overlay, death detection) are in place and mirror
 * the Vardorvis / Leviathan scripts. The Duke-specific mechanics are stubbed and marked
 * TODO -- fill them from capture-mode logs:
 * <ul>
 *   <li>{@link #watchStylePrayer(int)} -- flick Protect-from-Magic / Protect-from-Missiles
 *       to match Duke's alternating magic/ranged attack, impact-1 (see Leviathan's
 *       projectile-driven swapper for the pattern).</li>
 *   <li>{@link #pushPrepHazards(int)} -- PREP-phase ground hazards (GFX 1447 falling, 538
 *       avoid) fed into the shared {@link SchedulingTileDodger}, same as Leviathan rocks.</li>
 *   <li>{@link #planNextTile(WorldPoint, int)} -- dodge/positioning. Currently camps.</li>
 * </ul>
 */
@Slf4j
@Singleton
public class DukeAutoMain
{
	/**
	 * The Duke encounter has two parts: a PREP phase (Duke dormant -- the player runs the
	 * wake/weaken sequence while dodging the prep-only ground hazards GFX 1447 / 538), then
	 * the real FIGHT once he wakes. SHIELD is an in-fight immune window.
	 */
	public enum DukePhase { OUT_OF_FIGHT, PREP, FIGHT, SHIELD }

	/**
	 * Ordered prep sequence. Grinding is NOT its own step any more -- we grind opportunistically
	 * in the idle action-slots of the salt stage (and the fill stage) so it overlaps mining /
	 * walking instead of being a separate standing-still phase. See {@link #grindTick(int)}.
	 */
	public enum PrepStep { COLLECT_ARDER, COLLECT_MUSCA, COLLECT_SALT,
		FILL_VATS, COLLECT_POTIONS, FEED_DUKE, DONE }

	// --- Services ---------------------------------------------------
	private final Client client;
	private final MirrorState mirror;
	private final ClickRouter router;
	private final SchedulingTileDodger dodger;
	private final PrayerSwapper prayers;
	private final ArenaCamera camera;

	// --- Boss state -------------------------------------------------
	private final ConsumptionEngine engine;
	// Melee combat-boost (divine super combat preferred, else super combat). Divine runs on
	// a 5-min timer; regular re-doses below DukeAutoIDs.SUPER_COMBAT_REDRINK_MIN_BOOST.
	// NOTE: if Duke is ranged instead of meleed, replace this with a RANGING/DIVINE_RANGE
	// tracker (see LeviathanAutoMain.rangingBoost).
	private final CombatBoostTracker superCombatBoost = new CombatBoostTracker();
	private final TripState trip = new TripState();
	private final PhaseTracker<DukePhase> phase;

	private volatile boolean active = false;

	private NPC duke;
	private int lastAttackTick = -100;
	/** Next attack is blocked until this tick, from an eat / combo-eat delay (see EAT_DELAY_TICKS). */
	private int attackDelayUntil = -100;
	/** Tick Duke last STARTED his melee attack animation, and his previous-tick anim (to detect the start). */
	private int dukeAttackTick = -100;
	private int dukePrevAnim = -1;
	/**
	 * Duke attacks every WEAPON_COOLDOWN(5) ticks pre-enrage, picking from his pool -- only some picks
	 * animate, so we drive our own cadence off his RHYTHM (this phase = anchorTick % 5), not off seeing
	 * an animation each cycle. -1 until the first swing locks it.
	 */
	private int dukeAttackPhase = -1;
	/** Set once Duke's observed cadence drops off the 5t grid (-> 4t). Resets per kill. */
	private boolean dukeEnraged = false;
	/**
	 * Gaze-attack handling. The gaze animation ({@link #ANIM_DUKE_GAZE}) runs ~6 ticks. We suppress ALL
	 * attacking for the whole window (attacking through it kept catching us on the hit) and resume on the
	 * "avoid ... gaze" chat -- the safe signal -- with a tick timeout as a fallback. gazeTick = the start;
	 * gazeActive = we're inside the window.
	 */
	private int gazeTick = -100;
	private boolean gazeActive = false;
	/**
	 * Latched when Duke plays his death animation ({@link #ANIM_DUKE_DEATH}) so we treat the fight as
	 * over immediately (prayers off + prep start) rather than waiting for the despawn. Cleared when a
	 * fresh dormant Duke appears (next encounter).
	 */
	private boolean sawDeathAnim = false;
	private boolean dukeWasPresent = false;
	/** Set once per kill when we've seen Duke die, so prayers/trackers reset exactly once. */
	private boolean deathHandled = false;

	/** Melee weapon reach. DWH/most melee = 1; Dragon hunter lance = 2. TODO: set per gear. */
	private static final int WEAPON_REACH = 1;
	/** Weapon attack cooldown in ticks. Duke (and our weapon) run a 5-tick attack cycle. */
	private static final int WEAPON_COOLDOWN = 5;
	/**
	 * Eating delays your next attack. A single food (shark) = {@value #EAT_DELAY_TICKS}t; a combo food
	 * (karambwan eaten the same tick) adds {@value #COMBO_EAT_EXTRA_TICKS}t more (shark+kara = 5t). A
	 * potion sip adds NO attack delay (it only interrupts the current attack, so we re-click the target).
	 */
	private static final int EAT_DELAY_TICKS = 3;
	private static final int COMBO_EAT_EXTRA_TICKS = 2;
	/**
	 * Duke's gas-bottle projectile id. While airborne it targets a top-row vent (1/2/3); its gas 3x3
	 * forms there on landing. We read the projectile's target tile and, if its 3x3 would cover our
	 * camp, switch to the other camp WHILE IT'S STILL IN FLIGHT -- earlier than waiting for the cloud.
	 */
	private static final int PROJ_GAS_BOTTLE = 2436;
	/** Awake Duke's melee attack animation. We anchor our attack offset on the tick this STARTS. */
	private static final int ANIM_DUKE_ATTACK = 10176;
	/** Awake Duke's gaze attack animation. ~6-tick window; we keep attacking except on his hit tick. */
	private static final int ANIM_DUKE_GAZE = 10180;
	/** Duke's death animation. On it we drop out of the fight at once (prayers off + prep) vs on despawn. */
	private static final int ANIM_DUKE_DEATH = 12192;
	/** Fallback length of the gaze window (ticks) -- used to resume if the "avoid ... gaze" chat is missed. */
	private static final int GAZE_WINDOW_TICKS = 6;
	/** Tick after the gaze anim that Duke's hit lands (one earlier in enrage). We skip only our swing then. */
	private static final int GAZE_HIT_OFFSET = 4;
	private static final int GAZE_HIT_OFFSET_ENRAGE = 3;
	/**
	 * Shared DT2 boss HP readout widget (group 303 / child 20), same one Leviathan uses.
	 * WidgetReader.fraction parses its "cur/max" text. At/below {@link #ENRAGE_HP} Duke enrages
	 * (attacks every 4 ticks instead of 5).
	 */
	private static final int WIDGET_BOSS_HP = 19857428;
	private static final int ENRAGE_HP = 122;

	// Walk-click dedupe: suppress re-sending the same destination tick-after-tick.
	private WorldPoint lastWalkTarget;
	private int lastWalkTick = -100;

	// Transit hold: when the path to our goal is hazard-blocked and our tile is safe, we WAIT on it
	// (rather than jitter) until the path clears. holdGoal persists the blocked goal so we keep
	// re-evaluating it even after we've cancelled the walk; the flag suppresses collect re-walking.
	private boolean holdForHazard = false;
	private WorldPoint holdGoal = null;

	// Diagnostic heartbeat.
	private int lastDiagTick = -100;

	/** Toxic cloud (NPC 12198) -> tick first seen, so the 3x3 avoid window is anchored to spawn. */
	private final Map<NPC, Integer> toxicCloudSpawnTick = new IdentityHashMap<>();
	/** Extremity (12199/12200/12201) -> tick first seen; its cone attacks at spawn+3. */
	private final Map<NPC, Integer> extremitySpawnTick = new IdentityHashMap<>();

	/** Vent cycle: tick group A spawns (mod-{@value #VENT_CYCLE_PERIOD} phase ref); null until synced. */
	private Integer ventAnchorTick = null;
	/** Tick we began watching 12198 spawns this run/kill; vents alive before this can't anchor the cycle. */
	private int ventWatchStart = -1;

	// --- Prep sequence state ---
	private PrepStep prepStep = PrepStep.COLLECT_ARDER;
	private int lastPrepActionTick = -100;
	private WorldPoint lastPrepWalkTarget;
	private int lastPrepWalkTick = -100;
	/** Min ticks between prep object-interactions, so we don't re-click mid-pick/mine. */
	private static final int PREP_ACTION_COOLDOWN = 2;
	/** Minimum ticks between walk clicks to the same goal (human cadence + stuck recovery). */
	private static final int WALK_RECLICK_MIN_TICKS = 3;
	/** Picks we've requested for the current collect item (clicks issued); never exceeds need. */
	private int picksRequested = 0;
	private int lastPickItemId = Integer.MIN_VALUE;
	/** Whether we've completed the human pre-tile approach for the current collect item. */
	private boolean preApproachDone = false;
	/**
	 * Latched once we've reached the camp and issued the first Feed. After that we spam Feed every tick
	 * until Duke wakes, instead of walking back to camp between feeds (the feed-interact steps us off
	 * camp toward Duke, and re-pathing caused a feed/walk/feed/walk oscillation). Reset per kill.
	 */
	private boolean feedingStarted = false;
	/**
	 * Set when a hazard dodge/stop cancels an in-progress walk. A click on the mushroom (or salt)
	 * object auto-walks us there and marks a pick in flight ({@link #picksRequested}); if a dodge
	 * then steers us off that walk, the pick never lands, so {@code have} never catches up to
	 * {@code picksRequested} and collection would wait forever. This flag tells collectByPicking
	 * to drop the stale in-flight expectation and re-issue the click once the path is clear again.
	 */
	private boolean pickInterrupted = false;
	/** When true, feed Duke the potion at the camp to wake him and start the fight (prep complete). */
	private static final boolean FEED_DUKE_ENABLED = true;
	/** TODO (loot): Duke's loot spawns here (template; centre of the camp row) -- walk + pick up post-kill. */
	private static final WorldPoint LOOT_SPAWN_TILE = new WorldPoint(3039, 6451, 0);
	/** Number of fermentation vats to handle per phase (two vats -> two potions). */
	private static final int VATS_TO_FILL = 2;
	/** The two fermentation-vat tiles (template coords; converted to instance at runtime). */
	private static final WorldPoint VAT_EAST = new WorldPoint(3043, 6438, 0);
	private static final WorldPoint VAT_WEST = new WorldPoint(3033, 6438, 0);
	/** Player animation played when filling / collecting a fermentation vat (collect confirm only). */
	private static final int ANIM_VAT_INTERACT = 2572;
	/** NPC that SPAWNS when a vat is successfully filled -- the reliable fill-confirm signal. */
	private static final int NPC_VAT_FILL = 12197;
	/** Count of the confirm-NPC captured just before the pending vat click (fill confirm). */
	private int pendingVatNpcBaseline = 0;
	/** Order the vats were filled (template tiles), so we collect the first-filled first. */
	private final java.util.List<WorldPoint> vatFillOrder = new java.util.ArrayList<>();
	/** Potion count captured just before a Collect-from click (collect confirm). */
	private int pendingCollectBaseline = 0;
	/** Ticks parked AT a vat (no animation seen) before we settle and count it done anyway. */
	private static final int VAT_ARRIVE_SETTLE = 3;
	/** Ticks after a vat click without reaching it before we give up and re-click. */
	private static final int VAT_ACTION_TIMEOUT = 6;
	/** Vat TEMPLATE tiles already handled in the current phase (fill, then collect). */
	private final java.util.Set<WorldPoint> vatsDone = new java.util.HashSet<>();
	/** Vat we've clicked "Fill" on and are walking to; confirmed filled once we arrive beside it. */
	private WorldPoint pendingVat = null;
	/**
	 * Per-vat pipeline state (continuous collect-then-refill). EMPTY=fillable, FILLING=fill click sent,
	 * BREWING=fermenting, READY=fermented (collectable), COLLECTING=collect click sent. Only the vat ids
	 * 47536 (empty) and 47539 (ready) are observable, so BREWING is tracked here, not read from the scene.
	 */
	private enum VatState { EMPTY, FILLING, BREWING, READY, COLLECTING }
	private final java.util.Map<WorldPoint, VatState> vatState = new java.util.HashMap<>();
	/** True while the pending vat click was a Fill (confirmed by NPC spawn) vs a Collect (potion rise). */
	private boolean pendingVatIsFill = false;
	/** Last tick we issued a grind, so grindTick fires at most once per tick. */
	private int lastGrindTick = -100;
	/** Tick we became idle at a collect object with a pick/mine in flight (-1 = not waiting). */
	private int collectIdleSinceTick = -1;
	/** Idle ticks to wait at an object with no yield before re-issuing the pick/mine click. */
	private static final int PICK_RETRY_TICKS = 5;
	/** Player animation played while mining the salt deposit. */
	private static final int ANIM_MINE_SALT = 7139;
	/** With a known action animation, idle ticks parked-not-animating before re-clicking. */
	private static final int ANIM_RECLICK_TICKS = 2;
	/** Run speed used to sample the walk path tick-by-tick when hazard-checking it. */
	private static final int RUN_TILES_PER_TICK = 2;
	/**
	 * Extra tick(s) BEFORE a falling hazard's impact to also treat its tile as dangerous. A walkTo
	 * redirect only applies next tick, so if the transit check first trips on the impact tick the
	 * sidestep is a tick too late; and because we move {@value #RUN_TILES_PER_TICK} tiles/tick the
	 * single-tick impact window can be skipped by the path sampler entirely. Widening the window
	 * earlier makes the dodge both earlier and more reliably detected.
	 */
	private static final int FALLING_DODGE_LEAD_TICKS = 1;
	/** How many ticks ahead a tile must be clear of hazards to be used as a reroute waypoint. */
	private static final int ROUTE_HAZARD_HORIZON = 5;
	private static final int ARENA_PATH_BUDGET = 600;
	/**
	 * Vent camp sidestep (template tiles). When Duke is NOT alive and a toxic vent (NPC 12198)
	 * is covering a camp tile, step ONE tile outward off the camp and hold there until the vent
	 * despawns: WEST_CAMP (3035,6451) -> (3034,6451); EAST_CAMP (3043,6451) -> (3044,6451).
	 * (While Duke IS awake the fight-camp logic handles gas instead -- this never fires then.)
	 */
	private static final WorldPoint WEST_CAMP_SIDESTEP = new WorldPoint(3034, 6451, 0);
	private static final WorldPoint EAST_CAMP_SIDESTEP = new WorldPoint(3044, 6451, 0);

	/**
	 * Post-retrieval lane EXIT tiles (the mirror of the pre-tile start marker). After collecting a
	 * mushroom we run OUT via the outboard lane column at the camp row before handing off to the next
	 * step, so we leave along the safe column instead of drifting inward off the ideal dodge tiles.
	 * West lane -> (3030,6451); east lane -> (3048,6451).
	 */
	private static final WorldPoint WEST_LANE_EXIT = new WorldPoint(3030, 6451, 0);
	private static final WorldPoint EAST_LANE_EXIT = new WorldPoint(3048, 6451, 0);

	// --- Toxic-vent spawn cycle (prep only) -------------------------------------------------
	// 9 fixed vent sites (47555 ground objects), template tiles in a 3x3 grid. A toxic vent (NPC
	// 12198) spawns CENTERED on one of these, occupying the 3x3 around it. They fire in three
	// groups of three -- one per row AND column (a diagonal split) -- rotating A->B->C every
	// VENT_GROUP_INTERVAL ticks. Each vent: harmless +0..3, gas +4..11, harmless +12..14, despawn
	// +15; we block its 3x3 over the padded window [+VENT_DANGER_FROM, +VENT_DANGER_TO] for margin.
	// Numbered 1-9 L->R, top row (y6450) first: 1,2,3 / 4,5,6 / 7,8,9.
	private static final WorldPoint[] VENT_GROUP_A = {   // 1, 5, 9 (main diagonal)
		new WorldPoint(3036, 6450, 0), new WorldPoint(3039, 6446, 0), new WorldPoint(3042, 6442, 0) };
	private static final WorldPoint[] VENT_GROUP_B = {   // 2, 6, 7
		new WorldPoint(3039, 6450, 0), new WorldPoint(3042, 6446, 0), new WorldPoint(3036, 6442, 0) };
	private static final WorldPoint[] VENT_GROUP_C = {   // 3, 4, 8
		new WorldPoint(3042, 6450, 0), new WorldPoint(3036, 6446, 0), new WorldPoint(3039, 6442, 0) };
	private static final WorldPoint[][] VENT_GROUPS = { VENT_GROUP_A, VENT_GROUP_B, VENT_GROUP_C };
	private static final String[] VENT_GROUP_NAMES = { "A", "B", "C" };
	private static final int VENT_GROUP_INTERVAL  = 10;   // ticks between consecutive group spawns
	private static final int VENT_CYCLE_PERIOD    = 30;   // VENT_GROUP_INTERVAL * 3 groups
	private static final int VENT_DANGER_FROM     = 3;    // padded gas-window start (real gas = +4)
	private static final int VENT_DANGER_TO       = 12;   // padded gas-window end   (real gas = +11)
	private static final int VENT_PREDICT_HORIZON = 24;   // how many ticks ahead to schedule spawns
	/** Last predicted walk path + the first tile on it that would be hit (for the overlay). */
	private java.util.List<WorldPoint> debugPath = java.util.Collections.emptyList();
	private WorldPoint debugHitTile = null;

	// Overlay slot the plugin owns; main just updates it.
	private BaseOverlay overlay;

	@Inject
	public DukeAutoMain(
		Client client,
		MirrorState mirror,
		ClickRouter router,
		SchedulingTileDodger dodger,
		PrayerSwapper prayers,
		ArenaCamera camera)
	{
		this.client  = client;
		this.mirror  = mirror;
		this.router  = router;
		this.dodger  = dodger;
		this.prayers = prayers;
		this.camera  = camera;

		this.engine = new ConsumptionEngine(DukeSupply.PROFILE, mirror);

		this.phase = new PhaseTracker<>(DukePhase.OUT_OF_FIGHT)
			.on(DukePhase.SHIELD, () -> duke != null && isDukeAwake() && isShieldWindow(),
				"duke immune / shield window (fight)")
			.on(DukePhase.FIGHT, () -> duke != null && isDukeAwake() && !isShieldWindow(),
				"duke awake and vulnerable")
			.on(DukePhase.PREP, () -> duke != null && !isDukeAwake(),
				"prep: duke dormant, running the wake sequence")
			.on(DukePhase.OUT_OF_FIGHT, () -> duke == null,
				"no boss");
	}

	public void bindOverlay(BaseOverlay o) { this.overlay = o; }

	public boolean isActive()      { return active; }
	public void    start()
	{
		// First action on F7: detach the camera to a top-down overhead view of the arena so the
		// run is watchable from above. Centre on ARENA_CAMERA_CENTER (converted to the live
		// instance), pitch 4160 (PITCH_OVERHEAD = straight down), yaw north, zoom 300. No-op if
		// we're not in the loaded instance yet (centre unresolved).
		WorldPoint centre = toInstance(DukeAutoIDs.ARENA_CAMERA_CENTER);
		if (centre != null) camera.overhead(centre, DukeAutoIDs.ARENA_CAMERA_ZOOM);
		ventAnchorTick = null;   // fresh run -> re-sync the vent cycle from the next witnessed spawn
		ventWatchStart = -1;
		holdForHazard = false; holdGoal = null;
		dukeAttackTick = -100; dukePrevAnim = -1;
		dukeAttackPhase = -1; dukeEnraged = false; gazeTick = -100; gazeActive = false; feedingStarted = false; attackDelayUntil = -100;
		sawDeathAnim = false;
		vatState.clear();
		active = true;
		log.info("[duke] start -- region={} playerTile={} camCentre={}",
			mirror.getRegion(), mirror.getPlayerTile(), centre);
	}
	public void    stop()          { camera.reset(); active = false; log.info("[duke] stop"); }
	public DukePhase currentPhase(){ return phase.current(); }
	public TripState trip()        { return trip; }

	// -----------------------------------------------------------------
	// Tick loop.
	// -----------------------------------------------------------------

	public void onGameTick()
	{
		int tick = mirror.getTickCounter();

		duke = findDuke();
		// Death by animation (12192): latch it BEFORE phase.update so isDukeAwake() flips false this tick
		// -> phase leaves FIGHT and the death handler below runs now, not on the later despawn. Cleared
		// when a fresh dormant Duke appears (the next encounter).
		if (duke != null)
		{
			if (duke.getAnimation() == ANIM_DUKE_DEATH)
			{
				if (!sawDeathAnim) log.info("[duke] t={} death animation ({}) -> ending fight", tick, ANIM_DUKE_DEATH);
				sawDeathAnim = true;
			}
			else if (duke.getId() == DukeAutoIDs.NPC_DUKE_INACTIVE || duke.getId() == DukeAutoIDs.NPC_DUKE_ASLEEP)
			{
				sawDeathAnim = false;   // fresh dormant Duke -> new encounter
			}
		}
		phase.update(tick);
		trackDukeAttack(tick);   // anchor the attack-offset on the tick Duke starts his swing

		// Death detection: awake Duke gone (despawn or -> dead id) after having been present.
		// Reset trackers + drop overhead prayers exactly once (mirrors Leviathan death handling).
		boolean dukePresentNow = duke != null && isDukeAwake();
		if (dukeWasPresent && !dukePresentNow && !deathHandled)
		{
			deathHandled = true;
			onDukeDead(tick);
		}
		if (dukePresentNow) deathHandled = false;
		dukeWasPresent = dukePresentNow;

		// 1. Always track hazards -- clear every tick before re-projecting (same reasoning as
		// the sibling scripts: static-tile hazards only expire when their atTick passes, so a
		// stale projection from a now-gone telegraph would falsely flag tiles).
		dodger.clearAll();
		pushPrepHazards(tick);          // prep-only: falling 1447 + extremity cones
		pushToxicCloudHazards(tick);   // both phases: toxic cloud 3x3 (prep vents + fight gas bottles)
		syncVentCycle(tick);           // prep: learn the vent rotation from witnessed 12198 spawns (overlay only)

		dodger.onGameTick(tick);
		prayers.onGameTick(tick);

		// Attack-style prayer swap runs every tick (free action, independent of movement).
		watchStylePrayer(tick);

		updateOverlay();

		// Diagnostic heartbeat every ~5 ticks while in arena.
		if (isInArenaTemplate() && tick - lastDiagTick >= 5)
		{
			lastDiagTick = tick;
			log.info("[duke-diag] tick={} active={} phase={} duke={} region={} playerTile={}",
				tick, active, phase.current(),
				duke == null ? "null" : ("id=" + duke.getId() + " tile=" + duke.getWorldLocation()),
				mirror.getRegion(), mirror.getPlayerTile());
		}

		if (!active) return;

		// 1b. Optimistic grind. Grinding is a free action that continues while we walk, so we grind
		// one mushroom on every prep tick -- during the transit legs and even on a dodge tick (the
		// movement block below issues the dodge/destination walk AFTER this, so the walk is never
		// cancelled). Per-type gating inside keeps it from eating a count we're still collecting.
		if (phase.current() == DukePhase.PREP) grindTick(tick);

		// 2. Movement planning (priority: falling hazards -> movement -> attack).
		WorldPoint me = mirror.getPlayerTile();
		if (me != null)
		{
			// Latch the lane starter (pre-tile) the instant we stand on it -- even on a tick a dodge
			// preempts the collector -- so a dodge off the starter never leaves preApproachDone unset
			// (which made the collector keep walking us BACK to it = oscillation). One walk-back to the
			// starter is fine; this just stops the repeated ping-pong.
			if (phase.current() == DukePhase.PREP && !preApproachDone)
			{
				WorldPoint preT = activePreTile();
				if (preT != null && atTemplate(me, preT)) preApproachDone = true;
			}

			// 2a. Vent camp sidestep (priority over the generic dodge). When Duke isn't alive and a
			// toxic vent (12198) covers a camp tile, step one tile outward off the camp and hold there
			// until it despawns -- a fixed, deterministic step rather than whatever the dodger picks.
			WorldPoint campSide = campVentSidestep(me);   // template sidestep tile, or null
			if (campSide != null)
			{
				WorldPoint sideInst = toInstance(campSide);
				if (sideInst != null)
				{
					if (me.equals(sideInst))
					{
						if (overlay != null) overlay.setStep("prep: hold off camp (vent active)");
						return;   // already parked safely off-camp; stay put until the vent clears
					}
					if (!dangerousSoon(sideInst, tick) && !ventCovers(sideInst))
					{
						boolean sameSide = sideInst.equals(lastWalkTarget) && tick - lastWalkTick <= 3;
						if (!sameSide)
						{
							router.walkTo(sideInst);
							lastWalkTarget = sideInst;
							lastWalkTick = tick;
						}
						if (overlay != null) overlay.setStep("prep: sidestep off camp (vent)");
						return;
					}
					// sidestep tile unexpectedly unsafe -> fall through to the generic dodge below
				}
			}

			WorldPoint step = planNextTile(me, tick);
			if (step != null && !step.equals(me))
			{
				boolean sameTarget = step.equals(lastWalkTarget) && tick - lastWalkTick <= 3;
				if (!sameTarget)
				{
					router.walkTo(step);
					lastWalkTarget = step;
					lastWalkTick = tick;
					if (overlay != null) overlay.setStep("walk -> " + step);
				}
				// This walk is downtime (moving, not attacking) -- a free window to eat/drink; the overheal
				// cap keeps us from going over. In FIGHT it's the camp-switch run (our only eat window, a
				// chip-damage fight); in PREP it's transit between collection spots, where we top off for
				// the coming fight. We skip the FEED_DUKE step so eating never competes with the wake feed.
				boolean topOffHere = phase.current() == DukePhase.FIGHT
					|| (phase.current() == DukePhase.PREP && prepStep != PrepStep.FEED_DUKE);
				if (topOffHere)
				{
					applyConsumption(engine.evaluate(buildConsumptionRequest()), true);
				}
				return;
			}
			// Transit is holding for a hazard: wait on the current (safe) tile and do NOT fall through
			// to prep, which would re-issue the walk straight back into the hazard (the oscillation).
			if (holdForHazard) return;
			lastWalkTarget = null;
		}

		// 3. Fight cadence (on camp; movement above already handled any gas-bottle camp switch).
		// Attack Duke, then the NEXT tick click our camp tile to hold position (tick stall);
		// the only real relocation is the camp switch handled in planFightMove.
		if (phase.current() == DukePhase.FIGHT)
		{
			if (fightClickCampNext)
			{
				WorldPoint c = toInstance(fightCamp);
				if (c != null) router.walkTo(c);
				fightClickCampNext = false;
				if (overlay != null) overlay.setStep("fight: hold camp");
			}
			else if (canAttack(tick))
			{
				router.attackNpc(duke);
				lastAttackTick = tick;
				fightClickCampNext = true;
				if (overlay != null) overlay.setStep("fight: attack duke");
			}
		}

		// 3b. Prep actions (gather ingredients, etc). Hazard dodging already took priority in
		// the movement block above (it returns early), so prep only runs when the tile is safe.
		if (phase.current() == DukePhase.PREP)
		{
			runPrep(tick);
		}

		// 4. Supply consumption. This path runs while camped (the camp-switch run consumes in the movement
		// block above). In FIGHT we do NOT eat here -- it's a chip-damage fight, so food is saved for the
		// camp run; we only sip potions, and only on the tick right after an attack (a sip on an attack
		// tick cancels the swing). Outside FIGHT there's no swing to protect, so we eat/drink every tick.
		boolean inFight = phase.current() == DukePhase.FIGHT;
		boolean consumeOk = !inFight
			|| tick == lastAttackTick + 1
			|| tick - lastAttackTick > WEAPON_COOLDOWN;
		if (consumeOk)
		{
			// Eat to top off everywhere EXCEPT while camped in FIGHT (chip damage -> eat on the camp run)
			// and during the FEED_DUKE wake step (don't let eating compete with the feed). Drinks always
			// process; the overheal cap stops food from going over.
			boolean allowEat = !inFight && prepStep != PrepStep.FEED_DUKE;
			ConsumptionEngine.Request req = buildConsumptionRequest();
			ConsumptionEngine.Result res = engine.evaluate(req);
			applyConsumption(res, allowEat);
		}

		// DEBUG: per-tick gaze trace (end of tick, so lastAttackTick reflects this tick's attack).
		logGazeTick(tick);
	}

	/**
	 * DEBUG: trace our state every tick a gaze is active, so we can see why the single-tick skip
	 * sometimes still gets us caught. Logs offset from the gaze start, whether we attacked this tick,
	 * whether we're still interacting with Duke (a lingering attack the camp-click didn't cancel), our
	 * and Duke's animations, and HP (a drop with no "avoid" message = we were caught).
	 */
	private void logGazeTick(int tick)
	{
		if (!gazeActive) return;
		Player p = mirror.getLocalPlayer();
		int pAnim = (p == null) ? -999 : p.getAnimation();
		boolean interactingDuke = (p != null && duke != null && p.getInteracting() == duke);
		int dAnim = (duke == null) ? -999 : duke.getAnimation();
		boolean attackedThisTick = (tick == lastAttackTick);
		int skipTick = gazeTick + (dukeEnraged ? GAZE_HIT_OFFSET_ENRAGE : GAZE_HIT_OFFSET);
		log.info("[gaze-dbg] t={} off={} skipTick@off={} attackedThisTick={} interactingDuke={} pAnim={} dukeAnim={} hp={} enrage={}",
			tick, tick - gazeTick, skipTick - gazeTick, attackedThisTick, interactingDuke, pAnim, dAnim, mirror.getHp(), dukeEnraged);
	}

	// -----------------------------------------------------------------
	// Duke mechanics -- TODO: implement from capture-mode logs.
	// -----------------------------------------------------------------

	/**
	 * Flick Protect-from-Magic / Protect-from-Missiles to match Duke's current attack,
	 * one tick before impact. Model this on LeviathanAutoMain.watchPrayerProjectiles:
	 * read in-flight projectiles (DukeAutoIDs.PROJ_MAGIC / PROJ_RANGED) from
	 * mirror.getProjectileRecords(), and prayers.scheduleSwap(target, impactTick - 1).
	 * TODO: capture the projectile / animation / gfx tells and implement.
	 */
	private void watchStylePrayer(int tick)
	{
		if (!active || !isDukeAwake()) return;   // pray only during the live fight (Duke awake)
		// Camp melee the whole fight: keep Protect from Melee AND Piety up. ensureOn respects a
		// scheduled overhead swap, so when we later add the rare mage-projectile flick it can briefly
		// flip to Protect from Magic and this won't fight it (Piety is offensive, always kept on).
		prayers.ensureOn(tick, Prayer.PROTECT_FROM_MELEE, Prayer.PIETY);
		// TODO (rare): scan mirror.getProjectileRecords() for Duke's magic projectile and
		//       prayers.scheduleSwap(Prayer.PROTECT_FROM_MAGIC, impactTick - 1).
	}

	/**
	 * Feed Duke's ceiling-slam / falling-debris telegraph tiles into the shared dodger via
	 * TelegraphedHazards.addFalling(...), exactly like Leviathan's rock GFX. TODO: capture
	 * GFX_SLAM_TELEGRAPH / GFX_SLAM_ACTIVE and the telegraph->impact delay, then project.
	 */
	/**
	 * PREP-phase ground hazards only. GFX 1447 (falling, 3-tick telegraph) and GFX 538
	 * (avoid-on-sight) appear while Duke is dormant and the player runs the wake sequence;
	 * they do not occur in the awake fight, so we only scan for them during PREP.
	 */
	// -----------------------------------------------------------------
	// Prep sequence executor.
	// -----------------------------------------------------------------

	/**
	 * Drive the ordered prep steps. Each collect step walks to its stand tile and interacts
	 * with the object until the required count of its item is in the inventory, then advances.
	 * NOTE: scan/click uses the MENU id (what the live GameObject.getId() returns and what the
	 * captured click used) -- for the mushrooms that is 4752x (full) / its +1 (depleted), NOT
	 * the 4907x GameObject-overlay id; salt's object id and menu id are both 47522.
	 */
	/**
	 * Target potions for one collection run: the largest EVEN count (whole wakes, 2 potions each) the
	 * 8-pick mushroom cap allows at our Herblore yield. 8 picks give 8*yield powder/type, a potion needs
	 * 6/type, so max = floor(8*yield/6); floored to even so every fill round uses BOTH vats cleanly.
	 * (Herblore 60-69 -> 4 here vs a theoretical 5; the odd 5th would need single-vat handling.)
	 */
	private int batchPotions()
	{
		int y = DukeAutoIDs.herbloreMushroomYield(client.getRealSkillLevel(Skill.HERBLORE));
		return Math.max(2, 2 * Math.floorDiv(8 * y, 12));
	}

	/** Powder (per type) for the batch: 6/potion. By construction <= the 8*yield the 8 picks allow. */
	private int batchPowderGoal() { return 6 * batchPotions(); }

	/** Salax salt for the batch: 6/potion. Salt is freely minable, so it never caps the batch. */
	private int batchSaltGoal() { return 6 * batchPotions(); }

	private void runPrep(int tick)
	{
		// BATCHING CONSTRAINT (future): each Duke kill ATTEMPT consumes 2 arder-musca potions, and
		// each mushroom object can be picked only 8 times per Duke life -- Duke's DEATH respawns the
		// mushrooms. So within one life the mushrooms yield at most 8*herbloreMushroomYield powder
		// each, capping how many potions we can pre-make: potions/cycle = floor(8*yield / 6) (6
		// powder/potion), i.e. floor(potions/2) attempts. When we add multi-potion batching: scale
		// POWDER_GOAL / SALT_GOAL / ARDER_MUSCA_POTION_GOAL by the desired attempt count, but CAP
		// mushroom picks at 8 per object (don't request more than the respawn allows), and plan
		// refills around kills. (Today's single-cycle flow targets 2 potions = 1 attempt.)
		//
		// BATCH THROUGHPUT ("112211"): today's flow fills each vat ONCE then collects each ONCE
		// (fill 1, fill 2, collect 1, collect 2) -- fine for the 2 potions one attempt needs. For a
		// big batch, keep BOTH vats fermenting continuously: after COLLECTING a potion from a vat,
		// immediately RE-FILL that same vat (while ingredients remain and more potions are wanted)
		// before moving on. The unit of work per vat becomes collect-then-fill (the doubled digit),
		// alternating vats: [vat1 collect+fill][vat2 collect+fill][vat1 collect+fill]... = 1122
		// repeating (the "112211" rhythm), so neither vat ever sits empty while we walk/wait.
		// Implementation when we build it: a per-vat state machine (EMPTY -> FILLING -> FERMENTING
		// -> READY -> collect -> back to FILLING) with a remaining-potions counter, rather than the
		// current two-phase fill-all-then-collect-all.
		switch (prepStep)
		{
			case COLLECT_ARDER:
			{
				// Gate on total arder POWDER-EQUIVALENT (raw mushrooms * yield + ground powder) >=
				// POWDER_GOAL, not raw mushroom count -- so concurrent grinding never skews it.
				int y = DukeAutoIDs.herbloreMushroomYield(client.getRealSkillLevel(Skill.HERBLORE));
				if (collectByPicking(tick, DukeAutoIDs.MENU_ID_ARDER_MUSHROOMS, DukeAutoIDs.ARDER_PRE_TILE,
						WEST_LANE_EXIT, DukeAutoIDs.PICK_OPTION, DukeAutoIDs.ITEM_ARDER_MUSHROOM, batchPowderGoal(),
						-1, DukeAutoIDs.ITEM_ARDER_POWDER, y))
					prepStep = PrepStep.COLLECT_MUSCA;
				break;
			}

			case COLLECT_MUSCA:
			{
				int y = DukeAutoIDs.herbloreMushroomYield(client.getRealSkillLevel(Skill.HERBLORE));
				if (collectByPicking(tick, DukeAutoIDs.MENU_ID_MUSCA_MUSHROOMS, DukeAutoIDs.MUSCA_PRE_TILE,
						EAST_LANE_EXIT, DukeAutoIDs.PICK_OPTION, DukeAutoIDs.ITEM_MUSCA_MUSHROOM, batchPowderGoal(),
						-1, DukeAutoIDs.ITEM_MUSCA_POWDER, y))
					prepStep = PrepStep.COLLECT_SALT;
				break;
			}

			case COLLECT_SALT:
				// Mine the deposit until we hold SALT_GOAL (12) salax salt. Each mine yields several
				// (Mining-level dependent), so we just count the item. busyAnim=7139 (mining): while
				// that animation plays we're mining (wait); if we arrive and aren't playing it, the
				// click didn't latch -> re-click promptly instead of standing idle.
				if (collectByPicking(tick, DukeAutoIDs.GOBJ_SALT_DEPOSIT, null, null, DukeAutoIDs.SALT_OPTION,
						DukeAutoIDs.ITEM_SALAX_SALT, batchSaltGoal(), ANIM_MINE_SALT, 0, 0))
					prepStep = PrepStep.FILL_VATS;
				// (grinding runs globally every tick via grindTick, overlapping this stage)
				break;

			case FILL_VATS:
			case COLLECT_POTIONS:
				// Grinding must finish before we can fill (the vats take powder). grindTick (global,
				// every tick) is still clearing any remaining mushrooms, so just hold here until done.
				if (mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSHROOM) > 0
					|| mirror.countItem(DukeAutoIDs.ITEM_MUSCA_MUSHROOM) > 0)
				{
					if (overlay != null) overlay.setStep("prep: finishing grind before fill");
					break;
				}
				// Continuous vat pipeline: collect any ready vat and immediately refill empties, keeping
				// both vats brewing in parallel. For a 2-potion run this is just fill A/fill B/collect
				// A/collect B (ABAB); for a bigger batch it collects-then-refills each vat as it ferments
				// (AABB...), so neither vat ever idles through the brew. Done when we hold batchPotions().
				if (runVats(tick)) prepStep = PrepStep.FEED_DUKE;
				break;

			case FEED_DUKE:
				// With both potions, go to a camp tile and "Feed" Duke to start the fight. This
				// holds (returns in-prep) until Duke wakes -> phase flips to FIGHT and prep stops.
				feedDuke(tick);
				break;

			case DONE:
			default:
				// TODO: grind (pestle), ferment, administer -- next steps to capture.
				if (overlay != null) overlay.setStep("prep: ingredients gathered (TODO next steps)");
				break;
		}
	}

	/**
	 * Walk to {@code standTile} and fire {@code option} on the object {@code objId} until the
	 * inventory holds {@code need} of {@code itemId}. Returns true when that count is reached.
	 * If {@code itemId <= 0} the count can't be verified, so the step never auto-completes
	 * (used for salt until ITEM_SALT is known). {@code busyAnim > 0} suppresses re-clicks while
	 * the player plays that animation.
	 */
	/**
	 * Collect by interacting with the object DIRECTLY ("Pick" / "Mine") -- the click auto-walks
	 * the player to the object and performs the action, so there is no separate walk-to-a-stand-
	 * tile step. One click per action, then wait: skip while still walking to the object, while
	 * the busy animation is playing, or within the cooldown. Returns true once we hold {@code need}
	 * of {@code itemId}. A null object (depleted / regrowing / out of scene) just waits.
	 */
	private boolean collectByPicking(int tick, int objId, WorldPoint preTile, WorldPoint exitTile, String option,
		int itemId, int need, int busyAnim, int powderItemId, int yieldPerMushroom)
	{
		// For mushrooms we gate on TOTAL POWDER-EQUIVALENT in the inventory: raw mushrooms count as
		// yieldPerMushroom powder each, PLUS powder already ground. That sum is invariant under
		// grinding (grinding 1 mushroom: -yield mushroom-equiv, +yield powder = net 0), so grinding
		// can run concurrently without ever corrupting this "have we picked enough?" check. For salt
		// (powderItemId<=0) it's just the item count.
		int have;
		if (powderItemId > 0 && yieldPerMushroom > 0)
			have = mirror.countItem(itemId) * yieldPerMushroom + mirror.countItem(powderItemId);
		else
			have = itemId > 0 ? mirror.countItem(itemId) : -1;
		log.info("[duke-collect] t={} item={} have={}/{} preDone={} requested={}",
			tick, itemId, have, need, preApproachDone, picksRequested);
		if (itemId > 0 && have >= need)
		{
			// Enough retrieved. Before handing off to the next step, run OUT to the lane exit tile --
			// the outboard column at the camp row -- so we depart along the safe column instead of
			// cutting inward off the ideal dodge tiles. Mirror of the pre-tile, for the way out.
			WorldPoint meNow = mirror.getPlayerTile();
			if (exitTile != null && meNow != null && !atTemplate(meNow, exitTile))
			{
				if (!pathWillBeHit(meNow, toInstance(exitTile), tick, "exit"))
					walkArena(exitTile, tick, "prep: exit lane");
				return false;   // stay in this step until we're on the exit tile
			}
			return true;
		}

		// Reset pick-tracking + the approach flag when the collected item changes.
		if (itemId != lastPickItemId)
		{
			lastPickItemId = itemId;
			picksRequested = 0;
			preApproachDone = false;
			collectIdleSinceTick = -1;
		}

		// A dodge/stop cancelled the walk to the object before the requested pick could land, so the
		// in-flight expectation is stale. Drop it back to what we actually hold; the click re-issues
		// below once we're idle and the path is clear again (the destination guard still gates it).
		if (pickInterrupted)
		{
			pickInterrupted = false;
			if (itemId > 0 && picksRequested > have)
			{
				log.info("[duke-collect] t={} pick interrupted by dodge -- reset requested {}->{}",
					tick, picksRequested, have);
				picksRequested = have;
			}
			collectIdleSinceTick = -1;
		}

		WorldPoint me = mirror.getPlayerTile();

		// Human two-leg trip: walk to the pre-tile first (one click), then click the object. Each
		// leg is PROACTIVELY path-checked -- we never click a move whose path would walk us into a
		// hazard; we wait on the current (safe) tile until the path clears.
		if (preTile != null && !preApproachDone)
		{
			if (me != null && atTemplate(me, preTile))
			{
				preApproachDone = true;
			}
			else
			{
				if (pathWillBeHit(me, toInstance(preTile), tick, "pre")) return false;   // unsafe path -- wait
				walkArena(preTile, tick, "prep: approach " + option);
				return false;
			}
		}

		if (client.getLocalDestinationLocation() != null)   // still walking to the object
		{
			collectIdleSinceTick = -1;   // not stuck -- we're in transit; reset the no-yield timer
			return false;
		}

		// Overshoot-proof: request exactly ONE pick at a time and wait for the inventory count to
		// rise (the requested pick landing) before requesting the next. picksRequested never
		// exceeds need, so no timeout retry can queue an extra pick -- this is why arder (a long
		// initial walk) was overcollecting while musca (close) was not.
		if (have < picksRequested)
		{
			// A click is in flight and we've arrived (idle), waiting for the pick/mine to yield.
			// If we have a known action animation (e.g. mining 7139) and we're PLAYING it, the
			// action is genuinely underway -- reset the timer and keep waiting for the ore; never
			// re-click mid-animation.
			if (busyAnim > 0 && playerAnim() == busyAnim)
			{
				collectIdleSinceTick = -1;
				return false;
			}
			// Not animating the action (or no known animation). Count idle time and, when it's
			// clearly not happening, re-issue the click. With a known animation we re-click promptly
			// (we're parked at the object NOT animating -> the click didn't latch); without one we
			// give it the full window. The timer only counts idle-at-object time (reset while
			// walking above), so it never fires mid-walk or queues an extra pick mid-action.
			if (collectIdleSinceTick < 0) collectIdleSinceTick = tick;
			int wait = busyAnim > 0 ? ANIM_RECLICK_TICKS : PICK_RETRY_TICKS;
			if (tick - collectIdleSinceTick < wait) return false;
			log.info("[duke-collect] t={} not {} (anim={}) for {} ticks at object -- retrying {}",
				tick, busyAnim > 0 ? ("anim-" + busyAnim) : "yielding", playerAnim(),
				tick - collectIdleSinceTick, option);
			picksRequested = have;               // drop the stale in-flight -> re-click below
			collectIdleSinceTick = -1;
		}
		if (picksRequested >= need) return false;       // all needed picks already requested -- wait for them

		GameObject go = firstReachableById(objId);   // reachable one if several share the id (two salt deposits)
		if (go == null) return false;   // depleted / regrowing / not in scene / all blocked -- wait

		// Don't click the object if the walk to it would cross a hazard -- wait where we are.
		if (go.getWorldLocation() != null && pathWillBeHit(me, go.getWorldLocation(), tick, "obj")) return false;

		router.interactGameObject(go, MenuAction.GAME_OBJECT_FIRST_OPTION, option);
		picksRequested = have + 1;       // one pick now in flight
		collectIdleSinceTick = -1;       // restart the no-yield timer for this fresh click
		lastPrepActionTick = tick;
		if (overlay != null)
			overlay.setStep("prep: " + option + " (" + (itemId > 0 ? have : "?") + "/" + need + ")");
		return false;
	}

	/**
	 * Grind every {@code mushroomItemId} in the inventory into powder with the pestle and
	 * mortar ("Use" item-on-item). Returns true when none remain (step complete) or when we
	 * have no pestle to grind with. Cooldown-gated so we don't spam the use each tick.
	 * TODO: gate on the grind animation once its id is captured, for tighter pacing.
	 */
	/**
	 * Pick a GameObject with id {@code objId} that is actually reachable -- when several share the
	 * id (e.g. the two salt deposits) and one is walled off, choose the other. Reachability is a
	 * non-empty BFS path to any orthogonal neighbour of the object; among reachable candidates the
	 * nearest (shortest path) wins. Falls back to the first found if none path (don't deadlock).
	 */
	private GameObject firstReachableById(int objId)
	{
		java.util.List<GameObject> gos = SceneScanner.findByIds(client, objId);
		if (gos == null || gos.isEmpty()) return null;
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return gos.get(0);

		GameObject best = null;
		int bestLen = Integer.MAX_VALUE;
		for (GameObject go : gos)
		{
			WorldPoint t = go.getWorldLocation();
			if (t == null) continue;
			int[][] nbrs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
			for (int[] d : nbrs)
			{
				WorldPoint nb = new WorldPoint(t.getX() + d[0], t.getY() + d[1], t.getPlane());
				java.util.List<WorldPoint> path = TilePathPredictor.path(
					client.getTopLevelWorldView(), me, nb, null, this::inArena, ARENA_PATH_BUDGET);
				if (!path.isEmpty() && path.size() < bestLen) { best = go; bestLen = path.size(); }
			}
		}
		return best != null ? best : gos.get(0);
	}

	/**
	 * Grind ONE mushroom into powder (pestle + mortar, "Use" item-on-item) while we move. This is
	 * a free, optimistic action run on EVERY prep tick (see the call in onGameTick) -- including
	 * while walking a transit leg and even on a dodge tick AFTER the destination has been
	 * re-clicked -- so powder is ready by the time the vats need it. It is issued before the
	 * movement block, so the walk is the last action that tick and is never cancelled by the grind.
	 *
	 * <p>Per-type gating: we only grind a mushroom type once its OWN collection is finished, so
	 * grinding never eats into a count we are still gathering. Arder becomes grindable at
	 * COLLECT_MUSCA (arder done); musca at COLLECT_SALT (musca done). Arder is preferred so the
	 * stack we finished first clears first.
	 */
	private void grindTick(int tick)
	{
		if (mirror.countItem(DukeAutoIDs.ITEM_PESTLE_AND_MORTAR) <= 0) return;   // no pestle
		if (tick == lastGrindTick) return;                                      // at most one grind/tick

		// Only grind while we're in transit (a live walk destination) OR during the fill stage,
		// where no mining/picking is happening. Grinding is a one-off action that would INTERRUPT a
		// sustained mine/pick if issued while we stand at the object -- that was why the salt mine
		// never started -- so we never grind while standing still in a collect stage.
		boolean walking = client.getLocalDestinationLocation() != null;
		if (!walking && prepStep != PrepStep.FILL_VATS) return;

		// Grind ANY mushroom we hold, arder first. Collection now gates on powder-EQUIVALENT
		// (mushrooms*yield + powder), which is invariant under grinding, so grinding the very type
		// we're still collecting no longer skews the count -- we can start grinding from the first
		// mushroom instead of waiting for that type's collection to finish.
		int item = mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSHROOM) > 0 ? DukeAutoIDs.ITEM_ARDER_MUSHROOM
			: mirror.countItem(DukeAutoIDs.ITEM_MUSCA_MUSHROOM) > 0 ? DukeAutoIDs.ITEM_MUSCA_MUSHROOM
			: -1;
		if (item < 0) return;   // nothing to grind

		router.useItemOnItem(DukeAutoIDs.ITEM_PESTLE_AND_MORTAR, item);
		lastGrindTick = tick;
	}

	/**
	 * Interact with BOTH fermentation vats in turn -- used for Fill (object {@code 47536}) and for
	 * Collect-from (object {@code 47539}). There are exactly two vats at fixed arena tiles, east
	 * {@link #VAT_EAST} and west {@link #VAT_WEST} (template), so we do them deterministically,
	 * nearest first: click a vat, then WAIT for the player's vat-interaction animation
	 * ({@value #ANIM_VAT_INTERACT}) to confirm the action actually happened before moving to the
	 * other. That is the reliable signal (a bare arrival could miss / mis-fire). A dodge that
	 * cancels the walk drops the pending click so it re-issues; if we arrive and never see the
	 * animation within {@link #VAT_ACTION_TIMEOUT} ticks, we re-click. Done vats are tracked by
	 * their stable TEMPLATE tile. Returns true once both have been interacted.
	 *
	 * @param objId  the vat object id to target ({@code 47536} fillable, {@code 47539} ready).
	 * @param option the menu option ("Fill" or "Collect-from").
	 * @param label  overlay/log label.
	 */
	private boolean interactBothVats(int tick, int objId, String option, String label, int confirmNpcId)
	{
		WorldPoint me = mirror.getPlayerTile();

		// A dodge cancelled the walk to the vat -> drop the pending click so we re-issue it.
		if (pickInterrupted)
		{
			pickInterrupted = false;
			pendingVat = null;
		}

		// Confirm the pending vat once we've stopped walking.
		if (pendingVat != null)
		{
			if (client.getLocalDestinationLocation() != null) return false;        // still walking to it
			WorldPoint pv = toInstance(pendingVat);
			boolean atVat = pv != null && me != null && me.getPlane() == pv.getPlane()
				&& Math.abs(me.getX() - pv.getX()) <= 3 && Math.abs(me.getY() - pv.getY()) <= 3;

			boolean confirmed;
			if (confirmNpcId > 0)
			{
				// Fill: a successful interaction SPAWNS an NPC (12197). We counted them just before
				// clicking, so a rise in that count means this vat took. Reliable -- no animation guess.
				confirmed = countNpcs(confirmNpcId) > pendingVatNpcBaseline;
			}
			else
			{
				// Collect (no spawn signal captured): the vat interaction animation, or a short settle
				// parked at the vat so a missed animation tick can't trap us on one vat.
				confirmed = playerAnim() == ANIM_VAT_INTERACT
					|| (atVat && tick - lastPrepActionTick >= VAT_ARRIVE_SETTLE);
			}

			if (confirmed)
			{
				log.info("[duke-vat] t={} {} done vat={} -> {}/{}", tick, label, pendingVat,
					vatsDone.size() + 1, VATS_TO_FILL);
				vatsDone.add(pendingVat);
				if (confirmNpcId > 0 && !vatFillOrder.contains(pendingVat))
					vatFillOrder.add(pendingVat);   // remember fill order so we collect first-filled first
				pendingVat = null;
			}
			else if (tick - lastPrepActionTick >= VAT_ACTION_TIMEOUT)   // never latched (not reached / no spawn) -> re-click
			{
				log.info("[duke-vat] t={} {} vat={} not confirmed in {} ticks (atVat={}) -- re-clicking",
					tick, label, pendingVat, tick - lastPrepActionTick, atVat);
				pendingVat = null;
			}
			else
			{
				return false;   // waiting for the confirmation signal
			}
		}

		WorldPoint target = nextVatNotDone(me);   // nearer vat TEMPLATE tile not done yet, or null
		if (target == null) return true;
		if (tick - lastPrepActionTick < PREP_ACTION_COOLDOWN) return false;

		WorldPoint inst = toInstance(target);
		GameObject vat = vatObjectAt(inst, objId);
		if (vat == null) return false;   // that vat not in this state/scene yet -- wait (e.g. not fermented)
		if (inst != null && me != null && pathWillBeHit(me, inst, tick, "vat")) return false;   // unsafe path -- wait
		pendingVatNpcBaseline = confirmNpcId > 0 ? countNpcs(confirmNpcId) : 0;   // baseline BEFORE the click spawns one
		router.interactGameObject(vat, MenuAction.GAME_OBJECT_FIRST_OPTION, option);
		pendingVat = target;
		lastPrepActionTick = tick;
		if (overlay != null) overlay.setStep("prep: " + label + " (" + vatsDone.size() + "/" + VATS_TO_FILL + ")");
		return false;
	}

	/** Count live NPCs of the given id. */
	private int countNpcs(int id)
	{
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return 0;
		int n = 0;
		for (NPC npc : npcs)
		{
			if (npc != null && npc.getId() == id) n++;
		}
		return n;
	}

	/** The nearer of the two vat TEMPLATE tiles not yet done this phase (by player distance), or null. */
	private WorldPoint nextVatNotDone(WorldPoint meInstance)
	{
		WorldPoint meT = meInstance == null ? null : InstanceCoords.toTemplate(client, meInstance);
		WorldPoint best = null;
		int bestD = Integer.MAX_VALUE;
		for (WorldPoint v : new WorldPoint[]{ VAT_EAST, VAT_WEST })
		{
			if (vatsDone.contains(v)) continue;
			// Manhattan distance: picks the vat on OUR side first (mining west salt -> west vat is
			// 1 tile across vs 9, even though both are ~9 tiles south -- Chebyshev would tie them).
			int d = meT == null ? 0 : (Math.abs(meT.getX() - v.getX()) + Math.abs(meT.getY() - v.getY()));
			if (d < bestD) { bestD = d; best = v; }
		}
		return best;
	}

	/** Live vat GameObject of {@code objId} whose SW tile is at (or within 2 of) {@code instTile}. */
	private GameObject vatObjectAt(WorldPoint instTile, int objId)
	{
		if (instTile == null) return null;
		java.util.List<GameObject> vats = SceneScanner.findByIds(client, objId);
		if (vats == null) return null;
		GameObject best = null;
		int bestD = Integer.MAX_VALUE;
		for (GameObject go : vats)
		{
			WorldPoint t = go.getWorldLocation();
			if (t == null || t.getPlane() != instTile.getPlane()) continue;
			int d = Math.max(Math.abs(t.getX() - instTile.getX()), Math.abs(t.getY() - instTile.getY()));
			if (d <= 2 && d < bestD) { bestD = d; best = go; }
		}
		return best;
	}

	/** Current player animation id, or -1 if unavailable. */
	private int playerAnim()
	{
		Player p = client.getLocalPlayer();
		return p == null ? -1 : p.getAnimation();
	}

	/**
	 * Nearest known extremity apex (template) to {@code tpl}, or null if none within a small radius.
	 * The attack tile is always one of the 6 fixed lane-column apexes, so we snap to that rather
	 * than trust the live footprint's computed centre.
	 */
	private WorldPoint nearestExtremityApex(WorldPoint tpl)
	{
		if (tpl == null) return null;
		WorldPoint best = null;
		int bestD = Integer.MAX_VALUE;
		for (WorldPoint a : DukeAutoIDs.EXTREMITY_CLUSTER_TIPS)
		{
			if (a.getPlane() != tpl.getPlane()) continue;
			int d = Math.abs(a.getX() - tpl.getX()) + Math.abs(a.getY() - tpl.getY());
			if (d < bestD) { bestD = d; best = a; }
		}
		return (best != null && bestD <= 4) ? best : null;   // only snap when plausibly one of them
	}

	/**
	 * Collect both potions, vat by vat IN FILL ORDER. The first-filled vat ferments first, so we
	 * walk back to it and wait THERE for its object to change to READY (47539), collect it, then
	 * move to the second (waiting again if it hasn't fermented). Pre-walking while it ferments means
	 * the collect is instant once it's ready. Each collect is confirmed by the potion count rising
	 * (the only trustworthy signal here). Returns true once both potions are collected.
	 */
	private boolean collectVats(int tick)
	{
		WorldPoint me = mirror.getPlayerTile();
		if (pickInterrupted) { pickInterrupted = false; pendingVat = null; }

		// Confirm a pending collect: the potion count rose.
		if (pendingVat != null)
		{
			if (mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION) > pendingCollectBaseline)
			{
				log.info("[duke-vat] t={} collected potion from vat={} -> {}", tick, pendingVat,
					mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION));
				vatsDone.add(pendingVat);
				pendingVat = null;
			}
			else if (tick - lastPrepActionTick >= VAT_ACTION_TIMEOUT)   // collect didn't land -> retry
			{
				pendingVat = null;
			}
			else
			{
				return false;   // waiting for the potion to land
			}
		}

		WorldPoint target = nextVatToCollect(me);
		if (target == null) return true;   // both collected

		WorldPoint inst = toInstance(target);
		if (inst == null) return false;
		GameObject ready = vatObjectAt(inst, DukeAutoIDs.GOBJ_FERMENTATION_VAT_READY);

		if (ready != null)
		{
			// Fermented. Collect-from (auto-walks the last step if we're not adjacent yet).
			if (me != null && pathWillBeHit(me, inst, tick, "vat")) return false;       // unsafe path -- wait
			if (tick - lastPrepActionTick < PREP_ACTION_COOLDOWN) return false;
			pendingCollectBaseline = mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION);
			router.interactGameObject(ready, MenuAction.GAME_OBJECT_FIRST_OPTION, DukeAutoIDs.COLLECT_FROM_OPTION);
			pendingVat = target;
			lastPrepActionTick = tick;
			if (overlay != null) overlay.setStep("prep: collect potion ("
				+ mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION) + "/" + batchPotions() + ")");
			return false;
		}

		// Not fermented yet -> pre-walk to this vat so we're parked on it when it changes.
		boolean atVat = me != null && me.getPlane() == inst.getPlane()
			&& Math.abs(me.getX() - inst.getX()) <= 2 && Math.abs(me.getY() - inst.getY()) <= 2;
		if (!atVat && client.getLocalDestinationLocation() == null
			&& !pathWillBeHit(me, inst, tick, "vat-prewalk"))
		{
			walkArena(target, tick, "collect: wait at vat");
		}
		if (overlay != null) overlay.setStep("prep: wait for vat to ferment");
		return false;
	}

	/** Next vat TEMPLATE tile to collect, in FILL ORDER, skipping ones already collected. */
	private WorldPoint nextVatToCollect(WorldPoint meInstance)
	{
		for (WorldPoint v : vatFillOrder)
		{
			if (!vatsDone.contains(v)) return v;
		}
		return nextVatNotDone(meInstance);   // fallback (fill order unknown) -> nearest not done
	}

	/** Nearer (Manhattan) of the two vats currently in {@code want} state, or null if neither is. */
	private WorldPoint nearestVatInState(WorldPoint meInstance, VatState want)
	{
		WorldPoint meT = meInstance == null ? null : InstanceCoords.toTemplate(client, meInstance);
		WorldPoint best = null; int bestD = Integer.MAX_VALUE;
		for (WorldPoint v : new WorldPoint[]{ VAT_EAST, VAT_WEST })
		{
			if (vatState.get(v) != want) continue;
			int d = meT == null ? 0 : (Math.abs(meT.getX() - v.getX()) + Math.abs(meT.getY() - v.getY()));
			if (d < bestD) { bestD = d; best = v; }
		}
		return best;
	}

	/**
	 * A vat's live state read straight from the scene: the vat swaps its game-object id per state --
	 * fillable ({@link DukeAutoIDs#GOBJ_FERMENTATION_VAT}), fermented/harvestable
	 * ({@link DukeAutoIDs#GOBJ_FERMENTATION_VAT_READY}), and anything else = still brewing. Reading this
	 * each tick is the source of truth (internal state tracking desynced and starved the far vat).
	 */
	private VatState liveVatState(WorldPoint v)
	{
		boolean west = v.equals(VAT_WEST);
		// READY first: the fermented object replaces the fillable one.
		if (vatObjOnSide(west, DukeAutoIDs.GOBJ_FERMENTATION_VAT_READY) != null) return VatState.READY;
		// BREWING next: the 12197 NPC sits on the vat while it ferments. CRUCIAL -- the fillable 47536
		// object STAYS present during brewing, so the NPC (not the object) is what distinguishes a
		// brewing vat from a truly-empty one. Checking the object first made brewing vats look EMPTY and
		// starved the fight (we'd try to refill a brewing vat instead of collecting the ready one).
		List<NPC> npcs = client.getNpcs();
		if (npcs != null) for (NPC n : npcs)
			if (n != null && n.getId() == NPC_VAT_FILL && onVatSide(n.getWorldLocation(), west)) return VatState.BREWING;
		// EMPTY: fillable object present and no brewing NPC.
		if (vatObjOnSide(west, DukeAutoIDs.GOBJ_FERMENTATION_VAT) != null) return VatState.EMPTY;
		return VatState.BREWING;   // nothing recognizable -> treat as brewing (not actionable)
	}

	/** X splitting the west vat (x=3033) from the east vat (x=3043), in TEMPLATE coords. */
	private static final int VAT_SPLIT_X = 3038;

	/** True if a scene location (instance coords) is the west / east vat -- by side, not an exact tile. */
	private boolean onVatSide(WorldPoint instLoc, boolean west)
	{
		if (instLoc == null) return false;
		WorldPoint t = InstanceCoords.toTemplate(client, instLoc);
		if (t == null || Math.abs(t.getY() - VAT_WEST.getY()) > 6) return false;   // off the vat row
		return west ? t.getX() < VAT_SPLIT_X : t.getX() > VAT_SPLIT_X;
	}

	/** The vat GameObject of {@code id} on the given side, or null -- matched by side, not exact tile. */
	private GameObject vatObjOnSide(boolean west, int id)
	{
		java.util.List<GameObject> gos = SceneScanner.findByIds(client, id);
		if (gos == null) return null;
		for (GameObject go : gos) if (onVatSide(go.getWorldLocation(), west)) return go;
		return null;
	}

	/** DEBUG: dump every GameObject id sitting at each vat tile, to learn the real empty/brew/ready ids. */
	private void dumpVatObjects(int tick)
	{
		WorldPoint instW = toInstance(VAT_WEST), instE = toInstance(VAT_EAST);
		StringBuilder w = new StringBuilder(), e = new StringBuilder();
		SceneScanner.forEach(client, go ->
		{
			WorldPoint t = go.getWorldLocation();
			if (t == null) return;
			if (instW != null && t.getPlane() == instW.getPlane()
				&& Math.abs(t.getX() - instW.getX()) <= 2 && Math.abs(t.getY() - instW.getY()) <= 2)
				w.append(go.getId()).append('@').append(t.getX()).append(',').append(t.getY()).append(' ');
			if (instE != null && t.getPlane() == instE.getPlane()
				&& Math.abs(t.getX() - instE.getX()) <= 2 && Math.abs(t.getY() - instE.getY()) <= 2)
				e.append(go.getId()).append('@').append(t.getX()).append(',').append(t.getY()).append(' ');
		});
		log.info("[vat-obj] t={} WEST{} objs=[{}] | EAST{} objs=[{}]", tick, instW, w.toString().trim(), instE, e.toString().trim());
	}

	/** Ingredients for ONE more vat fill (one potion): 6 arder powder + 6 musca powder + 6 salax salt. */
	private boolean hasVatIngredients()
	{
		return mirror.countItem(DukeAutoIDs.ITEM_ARDER_POWDER) >= 6
			&& mirror.countItem(DukeAutoIDs.ITEM_MUSCA_POWDER) >= 6
			&& mirror.countItem(DukeAutoIDs.ITEM_SALAX_SALT) >= 6;
	}

	/** The vat TEMPLATE tile the player is standing at (within 2 tiles), or null if at neither. */
	private WorldPoint vatIAmAt(WorldPoint me)
	{
		if (me == null) return null;
		for (WorldPoint v : new WorldPoint[]{ VAT_EAST, VAT_WEST })
		{
			WorldPoint inst = toInstance(v);
			if (inst != null && me.getPlane() == inst.getPlane()
				&& Math.abs(me.getX() - inst.getX()) <= 2 && Math.abs(me.getY() - inst.getY()) <= 2)
				return v;
		}
		return null;
	}

	/** Vats that are NOT empty (filling/brewing/ready/collecting) -- i.e. potions already in flight. */
	private int vatsNotEmpty()
	{
		int n = 0;
		for (WorldPoint v : new WorldPoint[]{ VAT_EAST, VAT_WEST })
			if (vatState.get(v) != null && vatState.get(v) != VatState.EMPTY) n++;
		return n;
	}

	/**
	 * Continuous two-vat pipeline. Each tick: resolve a pending fill/collect; promote brewing vats to
	 * READY when the fermented object appears; then do ONE action, preferring to COLLECT a ready vat and
	 * otherwise FILL an empty one (while more potions are still wanted). This keeps both vats brewing in
	 * parallel -- ABAB for a 2-potion run, collect-then-refill (AABB...) for bigger batches. Returns true
	 * once we hold batchPotions() potions.
	 */
	private boolean runVats(int tick)
	{
		int wanted = batchPotions();
		int collected = mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION);
		if (collected >= wanted) return true;

		WorldPoint me = mirror.getPlayerTile();

		// A dodge cancelled the walk to a vat -> drop the pending click so we re-issue it.
		if (pickInterrupted) { pickInterrupted = false; pendingVat = null; }

		// 1. Resolve the pending fill/collect click (vat state is read fresh from the scene below -- this
		// only de-bounces the click we just issued). Fill confirms on the 12197 NPC spawn, collect on the
		// potion count rising; a timeout drops it so we retry.
		if (pendingVat != null)
		{
			if (pendingVatIsFill)
			{
				// Fill took when THIS vat is no longer fillable (brewing NPC replaced its 47536 object).
				if (liveVatState(pendingVat) != VatState.EMPTY)
				{ log.info("[duke-vat] t={} filled vat={}", tick, pendingVat); pendingVat = null; }
				else if (tick - lastPrepActionTick >= VAT_ACTION_TIMEOUT) pendingVat = null;
				else return false;
			}
			else
			{
				if (mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION) > pendingCollectBaseline)
				{
					collected = mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION);
					log.info("[duke-vat] t={} collected vat={} -> {}/{}", tick, pendingVat, collected, wanted);
					pendingVat = null;
					if (collected >= wanted) return true;
				}
				else if (tick - lastPrepActionTick >= VAT_ACTION_TIMEOUT) pendingVat = null;
				else return false;
			}
		}

		// 2. Read each vat's live state straight from the scene (the vat swaps object id per state).
		VatState wS = liveVatState(VAT_WEST);
		VatState eS = liveVatState(VAT_EAST);
		log.info("[vat-dbg] t={} here={} WEST={} EAST={} collected={}/{} ingred={} pending={}",
			tick, vatIAmAt(me), wS, eS, collected, wanted, hasVatIngredients(), pendingVat);
		dumpVatObjects(tick);   // DEBUG: raw object ids at each vat tile

		if (tick - lastPrepActionTick < PREP_ACTION_COOLDOWN) return false;

		// 3. Service a vat: prefer the one we're STANDING AT, so after harvesting a vat we immediately
		// refill THAT same vat while ingredients remain (no walking) -- the user's rule. Fall through to
		// the other vat only when the current one has nothing to do. Collect a READY vat; fill an EMPTY
		// one as long as we still hold the 6/6/6 ingredients for another potion (that is the fill gate).
		WorldPoint here = vatIAmAt(me);
		WorldPoint[] order = (here != null)
			? new WorldPoint[]{ here, here.equals(VAT_EAST) ? VAT_WEST : VAT_EAST }
			: new WorldPoint[]{ VAT_WEST, VAT_EAST };
		for (WorldPoint v : order)
		{
			boolean west = v.equals(VAT_WEST);
			VatState s = west ? wS : eS;
			WorldPoint inst = toInstance(v);
			if (s == VatState.READY)
			{
				GameObject ready = vatObjOnSide(west, DukeAutoIDs.GOBJ_FERMENTATION_VAT_READY);
				if (ready == null) continue;
				if (me != null && inst != null && pathWillBeHit(me, inst, tick, "vat")) return false; // unsafe -> wait
				pendingCollectBaseline = mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION);
				router.interactGameObject(ready, MenuAction.GAME_OBJECT_FIRST_OPTION, DukeAutoIDs.COLLECT_FROM_OPTION);
				pendingVat = v; pendingVatIsFill = false; lastPrepActionTick = tick;
				if (overlay != null) overlay.setStep("prep: collect potion (" + collected + "/" + wanted + ")");
				return false;
			}
			if (s == VatState.EMPTY && hasVatIngredients())
			{
				GameObject vat = vatObjOnSide(west, DukeAutoIDs.GOBJ_FERMENTATION_VAT);
				if (vat == null) continue;                                           // not fillable yet -> try other / wait
				if (me != null && inst != null && pathWillBeHit(me, inst, tick, "vat")) return false; // unsafe -> wait
				router.interactGameObject(vat, MenuAction.GAME_OBJECT_FIRST_OPTION, DukeAutoIDs.FILL_OPTION);
				pendingVat = v; pendingVatIsFill = true; lastPrepActionTick = tick;
				if (overlay != null) overlay.setStep("prep: fill vat (" + collected + "/" + wanted + ")");
				return false;
			}
		}

		// 4. Both brewing -> pre-walk to a brewing vat so the next collect is instant.
		WorldPoint brewVat = (wS == VatState.BREWING) ? VAT_WEST : (eS == VatState.BREWING ? VAT_EAST : null);
		if (brewVat != null)
		{
			WorldPoint inst = toInstance(brewVat);
			boolean atVat = me != null && inst != null && me.getPlane() == inst.getPlane()
				&& Math.abs(me.getX() - inst.getX()) <= 2 && Math.abs(me.getY() - inst.getY()) <= 2;
			if (!atVat && inst != null && client.getLocalDestinationLocation() == null
				&& !pathWillBeHit(me, inst, tick, "vat-prewalk"))
				walkArena(brewVat, tick, "vats: wait to ferment");
		}
		if (overlay != null) overlay.setStep("prep: wait for vats to ferment");
		return false;
	}

	/**
	 * Walk to the nearer camp tile and "Feed" Duke to start the fight. Each Feed consumes ONE
	 * potion, so with both potions this fires twice (once per tick-cooldown) until none remain;
	 * the second feed wakes Duke and the phase flips to FIGHT, ending prep.
	 */
	private void feedDuke(int tick)
	{
		if (duke == null) return;
		// Only feed a DORMANT Duke. During a death animation the dying Duke is still present but not
		// feedable, so we wait for it to reset to the dormant form before feeding (don't waste clicks).
		if (!isDukeDormant())
		{
			if (overlay != null) overlay.setStep("prep: waiting for Duke to reset");
			return;
		}
		if (mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION) <= 0) return;   // both fed
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return;

		// Walk to camp only until the first feed. Once feeding, the feed-interact steps us off camp
		// toward Duke -- do NOT walk back (that was the feed/walk/feed/walk oscillation). Instead spam
		// Feed every tick (no cooldown) until the last potion wakes him and the phase flips to FIGHT.
		WorldPoint camp = nearestSafeCamp(me, tick);
		if (!feedingStarted && camp != null && !atTemplate(me, camp))
		{
			walkArena(camp, tick, "prep: to camp");
			return;
		}
		if (!FEED_DUKE_ENABLED)   // temporarily disabled: walk to camp but don't start the fight
		{
			if (overlay != null) overlay.setStep("prep: at camp (feeding disabled)");
			return;
		}
		feedingStarted = true;
		router.interactNpc(duke, MenuAction.NPC_FIRST_OPTION, DukeAutoIDs.FEED_OPTION);
		if (overlay != null) overlay.setStep("prep: feed Duke (spam, "
			+ mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION) + " potion(s) left)");
	}

	/** The camp tile (EAST_CAMP / WEST_CAMP) nearer to the given tile, by Chebyshev distance. */
	private WorldPoint nearestCamp(WorldPoint fromInstance)
	{
		WorldPoint e = DukeAutoIDs.EAST_CAMP, w = DukeAutoIDs.WEST_CAMP;   // template
		WorldPoint meT = InstanceCoords.toTemplate(client, fromInstance);
		if (meT == null) return e;
		int de = Math.max(Math.abs(e.getX() - meT.getX()), Math.abs(e.getY() - meT.getY()));
		int dw = Math.max(Math.abs(w.getX() - meT.getX()), Math.abs(w.getY() - meT.getY()));
		return dw < de ? w : e;
	}

	/**
	 * The nearest camp (template) that is ACCESSIBLE: its tile isn't in a hazard (no gas on it) and
	 * there's a path to it that doesn't cross a soon-dangerous tile. A toxic vent can sit on or in
	 * front of the nearer camp, so we try the nearer one first and fall through to the other when
	 * it's blocked. If BOTH are blocked, return the plain nearest (least-bad; the walk routing copes).
	 */
	private WorldPoint nearestSafeCamp(WorldPoint fromInstance, int tick)
	{
		WorldPoint e = DukeAutoIDs.EAST_CAMP, w = DukeAutoIDs.WEST_CAMP;
		WorldPoint meT = InstanceCoords.toTemplate(client, fromInstance);
		WorldPoint[] order;
		if (meT == null)
		{
			order = new WorldPoint[]{ e, w };
		}
		else
		{
			int de = Math.max(Math.abs(e.getX() - meT.getX()), Math.abs(e.getY() - meT.getY()));
			int dw = Math.max(Math.abs(w.getX() - meT.getX()), Math.abs(w.getY() - meT.getY()));
			order = dw < de ? new WorldPoint[]{ w, e } : new WorldPoint[]{ e, w };
		}
		for (WorldPoint c : order)
		{
			WorldPoint inst = toInstance(c);
			if (inst == null) continue;
			if (dangerousSoon(inst, tick)) continue;   // gas on the camp tile itself
			List<WorldPoint> path = TilePathPredictor.path(
				client.getTopLevelWorldView(), fromInstance, inst,
				t -> !dangerousSoon(t, tick), this::inArena, ARENA_PATH_BUDGET);
			if (path != null && !path.isEmpty()) return c;   // reachable without crossing a hazard
		}
		return nearestCamp(fromInstance);   // both blocked -- head to the nearest anyway
	}

	/**
	 * True if a toxic vent's (NPC 12198) 3x3 footprint currently COVERS the given live tile --
	 * i.e. standing there means taking its damage. Vent coordinates are live/instance space.
	 */
	private boolean ventCovers(WorldPoint instanceTile)
	{
		if (instanceTile == null) return false;
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return false;
		for (NPC n : npcs)
		{
			if (n == null || n.getId() != DukeAutoIDs.NPC_TOXIC_VENT) continue;
			WorldArea a = n.getWorldArea();
			if (a == null || a.getPlane() != instanceTile.getPlane()) continue;
			if (instanceTile.getX() >= a.getX() && instanceTile.getX() < a.getX() + a.getWidth()
				&& instanceTile.getY() >= a.getY() && instanceTile.getY() < a.getY() + a.getHeight())
				return true;
		}
		return false;
	}

	/**
	 * If Duke is NOT alive and a toxic vent (12198) actually COVERS our camp tile while we're on
	 * it (or already on its outward sidestep tile), return the TEMPLATE sidestep tile to hold on
	 * until that vent clears; otherwise null. A vent elsewhere in the arena -- one whose 3x3 does
	 * NOT collide with the camp tile -- is ignored, so we never step off a camp that's still safe.
	 * West camp -> (3034,6451); east camp -> (3044,6451).
	 */
	private WorldPoint campVentSidestep(WorldPoint meInstance)
	{
		if (isDukeAwake()) return null;            // Duke alive -> fight-camp logic handles gas instead
		WorldPoint meT = InstanceCoords.toTemplate(client, meInstance);
		if (meT == null) return null;
		WorldPoint campT, sideT;
		if (meT.equals(DukeAutoIDs.WEST_CAMP) || meT.equals(WEST_CAMP_SIDESTEP))
		{
			campT = DukeAutoIDs.WEST_CAMP; sideT = WEST_CAMP_SIDESTEP;
		}
		else if (meT.equals(DukeAutoIDs.EAST_CAMP) || meT.equals(EAST_CAMP_SIDESTEP))
		{
			campT = DukeAutoIDs.EAST_CAMP; sideT = EAST_CAMP_SIDESTEP;
		}
		else return null;
		// Only step off if a vent's 3x3 actually covers the camp tile (collision). Evaluating the
		// CAMP tile (not our transient position) also holds us on the sidestep tile while the camp
		// stays covered and releases us the moment it clears.
		WorldPoint campInst = toInstance(campT);
		if (campInst == null || !ventCovers(campInst)) return null;
		return sideT;
	}

	// -----------------------------------------------------------------
	// Movement helpers.
	// -----------------------------------------------------------------

	/** Template tile -> live instance tile (the Duke arena is a dynamic instance). */
	private WorldPoint toInstance(WorldPoint template) { return InstanceCoords.fromTemplate(client, template); }

	/** The lane-starter (pre-)tile for the current prep step, or null for steps without one (salt/vats). */
	private WorldPoint activePreTile()
	{
		switch (prepStep)
		{
			case COLLECT_ARDER: return DukeAutoIDs.ARDER_PRE_TILE;
			case COLLECT_MUSCA: return DukeAutoIDs.MUSCA_PRE_TILE;
			default:            return null;
		}
	}

	/** True if the player's instance tile corresponds to the given TEMPLATE tile. */
	private boolean atTemplate(WorldPoint me, WorldPoint template) { return InstanceCoords.equalsTemplate(client, me, template); }

	/**
	 * Walk toward a TEMPLATE destination (converted to the live instance tile). Issues ONE click
	 * at the destination and then lets the game path there -- the arena walls are real client
	 * collision, so a single click routes around them. It only clicks again if the goal changed,
	 * or if the player has gone idle (stuck) without arriving AND at least WALK_RECLICK_MIN_TICKS
	 * have passed. This keeps it to one human-like click per walk instead of a click per tick.
	 */
	private void walkArena(WorldPoint templateDest, int tick, String stepLabel)
	{
		WorldPoint me = mirror.getPlayerTile();
		WorldPoint dest = toInstance(templateDest);
		if (me == null || dest == null || me.equals(dest)) return;

		// Route AROUND hazards only when the DESTINATION is off the lane (open-arena transit: camp,
		// salt, vats, centre). If the direct path would cross a soon-dangerous tile, aim at a waypoint
		// on a hazard-avoiding BFS route so the client detours instead of straight-lining into the gas.
		// When the dest is ON a lane (approaching/climbing a mushroom), keep the straight single click
		// and let the transit HOLD-and-wait for a window -- never detour off the low-exposure column
		// (keying on the dest, not our tile, matches planNextTile's goal-on-lane rule for one policy).
		WorldPoint target = dest;
		boolean destOffLane = laneDodgeAllowed(dest) == null;
		if (destOffLane && pathWillBeHit(me, dest, tick, stepLabel))
		{
			WorldPoint way = rerouteStep(me, dest, tick);
			if (way != null) target = way;
		}

		boolean walking  = client.getLocalDestinationLocation() != null;   // en route?
		boolean sameGoal = target.equals(lastPrepWalkTarget);
		if (sameGoal && (walking || tick - lastPrepWalkTick < WALK_RECLICK_MIN_TICKS)) return;

		router.walkTo(target);
		lastPrepWalkTarget = target;
		lastPrepWalkTick = tick;
		if (overlay != null && stepLabel != null) overlay.setStep(stepLabel);
	}

	private void pushPrepHazards(int tick)
	{
		if (phase.current() != DukePhase.PREP) return;
		Iterable<GraphicsObject> gfx = client.getGraphicsObjects();
		if (gfx == null) return;
		for (GraphicsObject g : gfx)
		{
			if (g == null) continue;
			int id = g.getId();
			// Only the 1447 telegraph is tracked. GFX 538 is the landing of the SAME attack;
			// we don't feed it separately because the 1447 time-to-impact already predicts the
			// exact impact tick (and 538 only appears ON impact, too late to react to anyway).
			if (!DukeAutoIDs.isFallingHazardGfx(id)) continue;

			LocalPoint lp = g.getLocation();
			if (lp == null) continue;
			// INSTANCE coords (fromLocal, NOT fromLocalInstance) so the hazard tile matches
			// mirror.getPlayerTile(), which is also instance-space.
			WorldPoint tile = WorldPoint.fromLocal(client, lp);
			if (tile == null) continue;

			// Impact lands FALLING_TO_IMPACT_TICKS (4) after 1447 spawns and ONLY that tick
			// damages, so markWholeFall=FALSE -- the tile stays walkable every other tick (so we
			// can keep attacking from it). The impact tick is derived from the start cycle, so it
			// is stable across re-scans and self-clears after.
			int impactTick = TelegraphedHazards.addFalling(
				dodger, tick, client.getGameCycle(), tile, g.getStartCycle(),
				DukeAutoIDs.FALLING_TO_IMPACT_TICKS, DukeAutoIDs.FALLING_IMPACT_ACTIVE_TICKS,
				false, "duke-fall");
			// Dodge LEAD ticks earlier: also flag the tile for the tick(s) BEFORE impact. A walkTo
			// redirect only takes effect next tick, so if the transit path-check first trips on the
			// impact tick itself the sidestep is a tick too late -- we step on just as it lands.
			// Marking impactTick-1 makes the path-check trip a tick sooner, so the redirect lands in
			// time to steer off the tile.
			for (int lead = 1; lead <= FALLING_DODGE_LEAD_TICKS; lead++)
			{
				int tt = impactTick - lead;
				if (tt >= tick) dodger.addHazard(Hazard.staticTile(tile, tt, "duke-fall-lead"));
			}
			// Log only falls on OUR lane region (west 3030-3032 / east 3046-3048) to cut noise --
			// registration above still covers BOTH lanes, so dodging is unaffected. An off-lane fall
			// (hzSide null) shouldn't happen, so log it as an anomaly if it ever does.
			Integer meSide = arenaSide(mirror.getPlayerTile());
			Integer hzSide = arenaSide(tile);
			if (hzSide == null || (meSide != null && meSide.equals(hzSide)))
				log.info("[duke-fall] t={} id={} tile={} start={} cycle={} impactTick={} lead={} marked={}",
					tick, id, tile, g.getStartCycle(), client.getGameCycle(), impactTick,
					FALLING_DODGE_LEAD_TICKS, (impactTick >= tick));
		}

		// Extremities (12199/12200/12201): 5x5 NPCs whose cone (apex = center, spreading inward)
		// attacks at spawn + EXTREMITY_ATTACK_DELAY_TICKS (3). The cone only DAMAGES at the attack
		// tick (+ its active ticks); the telegraph ticks before it are harmless to stand in or walk
		// through. So mark danger ONLY over [attackTick - lead, end], NOT from spawn -- marking the
		// telegraph made us pre-dodge several ticks early. The lead tick + per-tick re-check still
		// guarantees we're never on a cone tile when it actually lands.
		List<NPC> npcs = client.getNpcs();
		if (npcs != null)
		{
			java.util.Set<NPC> liveExtremities = new java.util.HashSet<>();
			for (NPC n : npcs)
			{
				if (n == null || !DukeAutoIDs.isExtremity(n.getId())) continue;
				liveExtremities.add(n);
				int spawnTick  = extremitySpawnTick.computeIfAbsent(n, k -> tick);
				int attackTick = spawnTick + DukeAutoIDs.EXTREMITY_ATTACK_DELAY_TICKS;
				int end        = attackTick + DukeAutoIDs.EXTREMITY_ATTACK_ACTIVE_TICKS - 1;
				if (tick > end) continue;

				WorldArea area = n.getWorldArea();
				if (area == null) continue;
				// Derive the cone in TEMPLATE space (correct inward direction via the x-split),
				// then convert each tile back to the live instance (rotation/translation safe).
				WorldPoint instCenter = new WorldPoint(
					area.getX() + area.getWidth() / 2,
					area.getY() + area.getHeight() / 2,
					area.getPlane());
				WorldPoint tplCenter = InstanceCoords.toTemplate(client, instCenter);
				if (tplCenter == null) continue;
				// SNAP the apex to the nearest KNOWN apex tile (the 6 fixed lane-column attack tiles).
				// The live 5x5 footprint can sit off-centre from its attack tile, which would shift the
				// computed cone off the lane column -- then the player's lane tile isn't marked and we
				// walk straight into the attack. Snapping anchors the apex on the column where we walk.
				WorldPoint apex = nearestExtremityApex(tplCenter);
				if (apex == null) apex = tplCenter;   // fallback: not near a known apex (shouldn't happen)
				List<WorldPoint> cone = new ArrayList<>();
				for (WorldPoint t : DukeAutoIDs.extremityCone(apex))
				{
					WorldPoint ci = InstanceCoords.fromTemplate(client, t);
					if (ci != null) cone.add(ci);
				}
				log.info("[duke-extremity] t={} npc={} area=({},{},{}x{}) tplCenter={} apex={} attackTick={} me={} coneInst={}",
					tick, n.getId(), area.getX(), area.getY(), area.getWidth(), area.getHeight(),
					tplCenter, apex, attackTick, mirror.getPlayerTile(), cone);

				int dangerFrom = attackTick - FALLING_DODGE_LEAD_TICKS;   // telegraph is harmless; only guard the landing
				for (int tt = Math.max(tick, dangerFrom); tt <= end; tt++)
					dodger.addHazard(Hazard.staticTiles(cone, tt, "extremity-cone"));
			}
			extremitySpawnTick.keySet().retainAll(liveExtremities);
		}
	}

	/**
	 * Which lane region a tile is on: 0 = west (template x 3030-3032), 1 = east (3046-3048), or null
	 * anywhere else -- falling hazards only land on the two lanes, so off-lane tiles have no "side".
	 */
	private Integer arenaSide(WorldPoint instanceTile)
	{
		if (instanceTile == null) return null;
		WorldPoint t = InstanceCoords.toTemplate(client, instanceTile);
		if (t == null) return null;
		int x = t.getX();
		if (x >= 3030 && x <= 3032) return 0;   // west lane region
		if (x >= 3046 && x <= 3048) return 1;   // east lane region
		return null;                            // off-lane: no falling hazards here
	}

	/**
	 * Toxic clouds (NPC 12198): a 3x3 that spawns harmless, then GASSES over the padded window
	 * [spawn+VENT_DANGER_FROM, spawn+VENT_DANGER_TO] (~T4-11 of gas, padded to T3-12). We mark ONLY
	 * that gas window dangerous -- NOT from spawn -- so a vent that pops up on our path during its
	 * harmless wind-up doesn't trigger a wasteful reroute or back-step: we can run straight through
	 * and be clear before the gas lands. Appears in BOTH prep (vents) and the awake fight (Duke's
	 * gas bottles), runs every tick, and is re-added each tick (clearAll wipes it otherwise).
	 */
	private void pushToxicCloudHazards(int tick)
	{
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return;
		java.util.Set<NPC> liveClouds = new java.util.HashSet<>();
		for (NPC n : npcs)
		{
			if (n == null || n.getId() != DukeAutoIDs.NPC_TOXIC_VENT) continue;
			liveClouds.add(n);
			int spawnTick = toxicCloudSpawnTick.computeIfAbsent(n, k -> tick);
			int gasFrom = spawnTick + VENT_DANGER_FROM;   // gas starts ~3 ticks after spawn, not at spawn
			int end     = spawnTick + VENT_DANGER_TO;
			if (tick > end) continue;

			WorldArea area = n.getWorldArea();
			if (area == null) continue;
			List<WorldPoint> footprint = new ArrayList<>();
			for (int dx = 0; dx < area.getWidth(); dx++)
				for (int dy = 0; dy < area.getHeight(); dy++)
					footprint.add(new WorldPoint(area.getX() + dx, area.getY() + dy, area.getPlane()));

			for (int tt = Math.max(tick, gasFrom); tt <= end; tt++)
				dodger.addHazard(Hazard.staticTiles(footprint, tt, "toxic-cloud"));
		}
		toxicCloudSpawnTick.keySet().retainAll(liveClouds);
	}

	/**
	 * Sync the vent cycle to a WITNESSED spawn (prep only). Any 12198 whose first-seen tick is
	 * after we began watching is a genuine spawn (its T0); snap its 3x3 centre to the nearest known
	 * vent tile, find which group it belongs to, and back-compute group A's reference tick. Re-anchors
	 * on every witnessed spawn, so it self-corrects and re-syncs after a cycle reset. Skipped while
	 * Duke is awake (then 12198 is his gas bottles, not the rotating prep vents).
	 */
	private void syncVentCycle(int tick)
	{
		if (isDukeAwake()) return;
		if (ventWatchStart < 0) ventWatchStart = tick;
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return;
		for (NPC n : npcs)
		{
			if (n == null || n.getId() != DukeAutoIDs.NPC_TOXIC_VENT) continue;
			Integer seen = toxicCloudSpawnTick.get(n);
			if (seen == null || seen <= ventWatchStart) continue;   // alive before we watched -> T0 unknown
			WorldArea a = n.getWorldArea();
			if (a == null) continue;
			WorldPoint centreInst = new WorldPoint(a.getX() + 1, a.getY() + 1, a.getPlane());  // 3x3 centre
			WorldPoint centreT = InstanceCoords.toTemplate(client, centreInst);
			if (centreT == null) continue;
			int g = ventGroupOf(centreT);
			if (g < 0) continue;                                    // not on the known 9-vent grid
			Integer prev = ventAnchorTick;
			ventAnchorTick = seen - g * VENT_GROUP_INTERVAL;         // group g spawned at `seen`
			if (prev == null)
				log.info("[duke-vent] synced: group {} spawn t={} -> A-ref t={}",
					VENT_GROUP_NAMES[g], seen, ventAnchorTick);
		}
	}

	/** Group index (0=A,1=B,2=C) a template vent centre belongs to, or -1 if not a known vent tile. */
	private int ventGroupOf(WorldPoint centreTemplate)
	{
		for (int g = 0; g < VENT_GROUPS.length; g++)
			for (WorldPoint c : VENT_GROUPS[g])
				if (c.getX() == centreTemplate.getX() && c.getY() == centreTemplate.getY()) return g;
		return -1;
	}

	/**
	 * NOTE: predicted vent hazards are deliberately NOT fed into the shared dodger. Doing so made the
	 * REACTIVE emergency dodge fire off a camp whose only "danger" was a projected future vent (no live
	 * gas on us), while feedDuke walked us straight back -- a camp<->sidestep oscillation. The cycle is
	 * tracked for the overlay (and is available for a future PLANNING-only layer: camp choice / transit
	 * BFS), but reactive dodging and the camp sidestep stay driven by LIVE coverage only.
	 */

	/**
	 * Positioning / dodge. Returns the tile to step to this tick, or null to stay put.
	 * TODO: once the arena bounds + hazards are captured, route off telegraphed slam tiles
	 *       (dodger.isTileDangerousAt) toward a safe tile. For now: camp (melee range is
	 *       maintained by the attack branch).
	 */
	private WorldPoint planNextTile(WorldPoint me, int tick)
	{
		// In the fight, movement is the camp-switch (east <-> west) when a toxic cloud threatens
		// our camp; everything else camps + attacks.
		if (phase.current() == DukePhase.FIGHT) return planFightMove(me, tick);

		// Where we're currently headed (if walking), used to bias dodges TOWARD the destination.
		net.runelite.api.coords.LocalPoint destLp = client.getLocalDestinationLocation();
		WorldPoint goal = destLp == null ? null : WorldPoint.fromLocal(client, destLp);

		// Emergency dodge. If our tile is dangerous now or about to be (a 1447 fall incoming,
		// or a 538 avoid tile under us), step to a safe tile -- biased toward the destination so
		// we dodge forward/sideways, not backward. Returns null (camp) when the tile is clear.
		// Priority order: falling/avoid hazards -> movement -> attack (attack runs only when
		// this returns null, see onGameTick).
		boolean dangerSoon = dodger.isTileDangerousAt(me, tick)
			|| dodger.isTileDangerousAt(me, tick + 1)
			|| dodger.isTileDangerousAt(me, tick + 2);
		if (dangerSoon)
		{
			holdForHazard = false; holdGoal = null;   // our tile is threatened -> acting now, not holding
			// Defer to forward progress: if we're mid-walk toward a destination and that path is
			// clear, keep running to it -- running to the goal carries us off the dangerous tile
			// faster than a 1-tile lateral dodge, and avoids the stall + reapproach oscillation a
			// dodge causes. Only dodge when continuing is itself unsafe (or we're standing still with
			// nowhere committed to go, so there's no forward progress to defer to).
			if (client.getLocalDestinationLocation() != null && !transitWillBeHit(me, tick))
				return null;
			WorldPoint d = dodgeToward(me, goal, tick);
			if (d == null) d = dodger.computeDodge(me, laneDodgeAllowed(me), 3).orElse(null);
			if (d != null && !d.equals(me)) pickInterrupted = true;   // this move cancels any in-flight pick walk
			return d;
		}

		// Transit safety: we may be walking to a clicked object (e.g. the tile in front of a
		// mushroom). Predict the path to our LIVE walk destination; if any tile we'd stand on would
		// be dangerous when we get there, take over movement. Recomputed EVERY tick, so as a 1447
		// telegraph appears a few ticks ahead we react immediately -- preferring to REROUTE around
		// it (keep progressing toward the goal via safe tiles) and only sidestepping/holding when no
		// safe route exists. Collection re-clicks the object once the direct path is clear again.
		// Transit goal: the live walk destination, or -- if we cancelled our walk last tick to wait out
		// a hazard -- the PERSISTED hold goal, so we keep evaluating the blocked route while stationary.
		WorldPoint transitGoal = goal != null ? goal : (holdForHazard ? holdGoal : null);
		if (transitGoal != null && pathWillBeHit(me, transitGoal, tick, goal != null ? "live" : "hold"))
		{
			// Reroute ONLY when the GOAL is OFF the lane (we're crossing to the centre -- salt/vats/camp)
			// -- there, threading inward around the hazard is right. When the goal is ON a lane (we're
			// approaching or climbing a mushroom), do NOT reroute off the column; HOLD on a safe tile and
			// wait for a window to climb. The column is the low-exposure line and a cone widens inward, so
			// detouring off it is worse -- and holding gives one clean policy instead of the live-reroute
			// yanking us off-line while the collector's own path-check waits (the old jitter).
			boolean goalOnLane = laneDodgeAllowed(transitGoal) != null;
			if (!goalOnLane)
			{
				WorldPoint way = rerouteStep(me, transitGoal, tick);
				if (way != null && !way.equals(me))
				{
					holdForHazard = false; holdGoal = null;
					pickInterrupted = true;   // we're driving movement now -> re-click the object once clear
					log.info("[duke-transit] reroute t={} -> {} (goal={})", tick, way, transitGoal);
					return way;
				}
			}
			// No safe forward route. Our current tile is already SAFE (the emergency dodge above handled
			// any unsafe tile), so HOLD here and wait for the hazard window to pass -- better and more
			// human than jittering sideways with no progress. Cancel the queued walk ONCE; the sticky
			// hold (flag + persisted goal) keeps us here, and the movement block won't re-walk.
			holdForHazard = true; holdGoal = transitGoal; pickInterrupted = true;
			if (client.getLocalDestinationLocation() != null) router.walkTo(me);
			log.info("[duke-transit] HOLD t={} (safe tile, path blocked, waiting) goal={}", tick, transitGoal);
			if (overlay != null) overlay.setStep("hold: waiting for hazard window");
			return me;
		}
		holdForHazard = false; holdGoal = null;   // path clear -> release any hold
		return null;
	}

	/**
	 * A reroute step toward {@code goal} that avoids soon-dangerous tiles. BFS the route with
	 * tiles dangerous within the near horizon treated as impassable (layered on wall collision),
	 * then return the tile {@value #RUN_TILES_PER_TICK} steps along it -- one tick of running on
	 * the safe route. Clicking only the near waypoint (not the far goal) guarantees the client
	 * walks the detour instead of cutting back through the hazard on its own shortest path.
	 * Returns null if there is no safe route (caller then sidesteps / holds).
	 */
	private WorldPoint rerouteStep(WorldPoint me, WorldPoint goal, int tick)
	{
		if (goal == null || goal.equals(me)) return null;
		List<WorldPoint> safePath = TilePathPredictor.path(
			client.getTopLevelWorldView(), me, goal,
			t -> !dangerousSoon(t, tick), this::inArena, ARENA_PATH_BUDGET);
		if (safePath == null || safePath.size() < 2) return null;   // unreachable while avoiding danger
		int idx = Math.min(RUN_TILES_PER_TICK, safePath.size() - 1);
		return safePath.get(idx);
	}

	/** True if {@code tile} is dangerous at any tick within the routing horizon from {@code now}. */
	private boolean dangerousSoon(WorldPoint tile, int now)
	{
		for (int t = now; t <= now + ROUTE_HAZARD_HORIZON; t++)
		{
			if (dodger.isTileDangerousAt(tile, t)) return true;
		}
		return false;
	}

	/**
	 * Pick a single-tile dodge biased TOWARD {@code goal}. Of our four orthogonal neighbours that
	 * are walkable, safe over the routing horizon, and permitted by the lane corridor, choose the
	 * one CLOSEST to the goal -- so a dodge goes forward or sideways (toward where we're headed)
	 * rather than backward. With no goal, any safe neighbour will do. Returns null if none qualify
	 * (the caller then falls back to the generic dodge or holds).
	 */
	private WorldPoint dodgeToward(WorldPoint me, WorldPoint goal, int tick)
	{
		if (me == null) return null;
		java.util.Set<WorldPoint> allowed = laneDodgeAllowed(me);   // lane wall corridor, or null
		Integer columnX = laneColumnX(me);   // template x of the lane home column (3048/3030), or null off-lane
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		WorldPoint best = null;
		int bestColDist = Integer.MAX_VALUE;
		int bestGoalDist = Integer.MAX_VALUE;
		for (int[] d : dirs)
		{
			WorldPoint cand = new WorldPoint(me.getX() + d[0], me.getY() + d[1], me.getPlane());
			if (allowed != null && !allowed.contains(cand)) continue;   // would hit a wall on the lane
			if (dangerousSoon(cand, tick)) continue;                    // not safe over the horizon
			if (!stepReachable(me, cand)) continue;                     // blocked by collision
			int goalDist = goal == null ? 0
				: Math.max(Math.abs(cand.getX() - goal.getX()), Math.abs(cand.getY() - goal.getY()));
			// On a lane, prefer the tile nearest the HOME COLUMN (the wall row -> least future
			// exposure) and use goal distance only to break ties. This pulls us back OUTBOARD
			// toward the column (e.g. 3047 -> 3048, 3031 -> 3030) instead of drifting inward
			// toward centre. The dangerousSoon filter above still governs, so a live extremity
			// cone -- apex on the column, widening inward -- excludes the column tile and we step
			// only as far inward as safety needs. Off-lane, colDist is 0 for every candidate, so
			// the goal bias alone decides (unchanged behaviour for salt / vats / grinding / camp).
			int colDist = 0;
			if (columnX != null)
			{
				WorldPoint candT = InstanceCoords.toTemplate(client, cand);
				colDist = candT == null ? Integer.MAX_VALUE : Math.abs(candT.getX() - columnX);
			}
			if (colDist < bestColDist || (colDist == bestColDist && goalDist < bestGoalDist))
			{
				bestColDist = colDist;
				bestGoalDist = goalDist;
				best = cand;
			}
		}
		return best;
	}

	/**
	 * Template x of the lane HOME COLUMN for the player's current tile -- 3048 on the east/musca
	 * lane, 3030 on the west/arder lane -- or {@code null} when off-lane. This is the wall row we
	 * bias dodges back toward (see {@link #dodgeToward}); it mirrors the lane bands in
	 * {@link #laneDodgeAllowed}.
	 */
	private Integer laneColumnX(WorldPoint meInstance)
	{
		WorldPoint t = InstanceCoords.toTemplate(client, meInstance);
		if (t == null) return null;
		int x = t.getX();
		if (x >= 3046 && x <= 3048) return 3048;
		if (x >= 3030 && x <= 3032) return 3030;
		return null;
	}

	/** True if {@code to} is a single walkable step from {@code from} (one-tile BFS over collision). */
	private boolean stepReachable(WorldPoint from, WorldPoint to)
	{
		List<WorldPoint> p = TilePathPredictor.path(
			client.getTopLevelWorldView(), from, to, null, this::inArena, 8);
		return p != null && p.size() == 2;
	}

	/**
	 * Lane-aware dodge corridor. On the mushroom lanes a wall sits immediately OUTBOARD of the
	 * lane column -- east of x=3048 (east/musca lane) and west of x=3030 (west/arder lane) -- so
	 * an unconstrained dodge keeps picking the wall tile and we never actually move off the
	 * hazard. When the player is in a lane corridor, restrict the dodge candidates to the 3-wide
	 * band on the INBOARD side: on the east lane (template x in 3046..3048) we may only dodge
	 * WEST; on the west lane (3030..3032) only EAST. Returns the allowed set as INSTANCE tiles
	 * (what {@link SchedulingTileDodger#computeDodge} compares neighbors against), or {@code null}
	 * when the player is not on a lane so the normal unconstrained dodge is used for the salt
	 * deposit / vats / grinding / camp positions.
	 *
	 * @param meInstance the player's current tile in INSTANCE coords.
	 */
	private java.util.Set<WorldPoint> laneDodgeAllowed(WorldPoint meInstance)
	{
		WorldPoint t = InstanceCoords.toTemplate(client, meInstance);
		if (t == null) return null;
		int x = t.getX();
		int loX;
		int hiX;
		if (x >= 3046 && x <= 3048) { loX = 3046; hiX = 3048; }        // east lane: column 3048 + inboard gaps
		else if (x >= 3030 && x <= 3032) { loX = 3030; hiX = 3032; }   // west lane: column 3030 + inboard gaps
		else return null;                                              // off-lane: unconstrained dodge

		int cy = t.getY();
		java.util.Set<WorldPoint> set = new java.util.HashSet<>();
		// Allow the lane column AND the inboard band (never the outboard wall). This lets us step
		// into a GAP to dodge a 1447 -- including a gap inward BETWEEN the extremity cones. We rely
		// on the per-tile danger filter (dangerousSoon, used by dodgeToward) to exclude an ACTIVE
		// extremity cone's tiles: a cone's apex is on the column and it WIDENS inward, so while it
		// is live the inward tiles are dangerous and get excluded -> we stay on the column; when no
		// cone is live the inward gaps are free to use. dodgeToward's goal-bias then pulls us back
		// toward the column line (the mushroom is on the column) so we return to the intended tile.
		for (int bx = loX; bx <= hiX; bx++)
		{
			for (int by = cy - 2; by <= cy + 2; by++)
			{
				WorldPoint inst = toInstance(new WorldPoint(bx, by, 0));
				if (inst != null) set.add(inst);
			}
		}
		return set;
	}

	/**
	 * True if the player's current walk (toward {@code getLocalDestinationLocation}) would put us
	 * on a dangerous tile at the tick we'd occupy it. Predicts the walked path over live collision
	 * and samples each tick's standing tile (run = {@value #RUN_TILES_PER_TICK} tiles/tick).
	 */
	/** Reactive check: is our CURRENT walk (live destination) going to put us on a hazard? */
	private boolean transitWillBeHit(WorldPoint me, int tick)
	{
		net.runelite.api.coords.LocalPoint destLp = client.getLocalDestinationLocation();
		if (destLp == null) { debugPath = java.util.Collections.emptyList(); debugHitTile = null; return false; }
		return pathWillBeHit(me, WorldPoint.fromLocal(client, destLp), tick, "live");
	}

	/**
	 * Shared path hazard check used BOTH proactively (before issuing a move/click, so we never
	 * start walking into a hazard) and reactively (for the current live walk). Predicts the full
	 * path {@code from -> goal} over live collision and samples the tile we'd stand on each tick
	 * (run = {@value #RUN_TILES_PER_TICK} tiles/tick) against the dodger. Stores debugPath /
	 * debugHitTile for the overlay. Empty path (can't predict) returns false so we never deadlock.
	 */
	private boolean pathWillBeHit(WorldPoint from, WorldPoint goal, int tick, String why)
	{
		debugPath = java.util.Collections.emptyList();
		debugHitTile = null;
		if (from == null || goal == null || goal.equals(from)) return false;

		List<WorldPoint> path = TilePathPredictor.path(
			client.getTopLevelWorldView(), from, goal, null, this::inArena, ARENA_PATH_BUDGET);
		log.info("[duke-transit] t={} {} from={} goal={} pathSize={}", tick, why, from, goal, path.size());
		if (path.isEmpty()) return false;
		debugPath = path;

		int maxK = (path.size() - 1 + RUN_TILES_PER_TICK - 1) / RUN_TILES_PER_TICK;
		for (int k = 1; k <= maxK; k++)
		{
			WorldPoint stand = TilePathPredictor.tileAfterTicks(path, k, RUN_TILES_PER_TICK);
			if (stand == null) break;
			if (dodger.isTileDangerousAt(stand, tick + k))
			{
				log.info("[duke-transit] HIT t={} {} k={} stand={} atTick={}", tick, why, k, stand, tick + k);
				debugHitTile = stand;
				return true;
			}
		}
		return false;
	}

	/** Live predicted walk path (instance tiles) for the path overlay. */
	public java.util.List<WorldPoint> debugPath() { return debugPath; }
	/** First tile on the predicted path that would be hit, or null. */
	public WorldPoint debugHitTile() { return debugHitTile; }

	/** BFS bounds for transit prediction: stay inside the Duke arena region. */
	private boolean inArena(WorldPoint instanceTile)
	{
		WorldPoint tpl = InstanceCoords.toTemplate(client, instanceTile);
		return tpl != null && tpl.getRegionID() == DukeAutoIDs.ARENA_REGION;
	}

	/** Current fight camp target (EAST_CAMP / WEST_CAMP). Reset per kill. */
	private WorldPoint fightCamp;
	/** True when the next tick should click the camp tile (the stall after an attack). */
	private boolean fightClickCampNext = false;

	/**
	 * Fight movement: camp on a Duke tile and attack; when a toxic cloud threatens the current
	 * camp, switch to the opposite one (east <-> west) if it is clear. Returns the tile to walk
	 * to, or null when we are safely on-camp (attack runs then).
	 */
	private WorldPoint planFightMove(WorldPoint me, int tick)
	{
		if (fightCamp == null) fightCamp = nearestCamp(me);
		// Relocate when our camp is threatened -- either a gas cloud already covers it, OR a gas-bottle
		// projectile is inbound to a vent whose 3x3 covers it (react while it's still airborne, before
		// the cloud forms). Switch to the opposite camp only if IT isn't threatened too.
		if (campThreatened(fightCamp, tick))
		{
			WorldPoint other = fightCamp.equals(DukeAutoIDs.EAST_CAMP)
				? DukeAutoIDs.WEST_CAMP : DukeAutoIDs.EAST_CAMP;
			if (!campThreatened(other, tick)) fightCamp = other;
		}
		if (!atTemplate(me, fightCamp)) { fightClickCampNext = false; return toInstance(fightCamp); }   // move to camp
		return null;   // on camp -> attack / stall cadence (see onGameTick)
	}

	/** Camp is threatened: a gas cloud covers it soon, OR a gas-bottle projectile is inbound to it. */
	private boolean campThreatened(WorldPoint templateCamp, int tick)
	{
		return campDangerSoon(templateCamp, tick) || campThreatenedByProjectile(templateCamp);
	}

	/** True if a toxic cloud covers the camp now or within the next 2 ticks. */
	private boolean campDangerSoon(WorldPoint templateCamp, int tick)
	{
		WorldPoint c = toInstance(templateCamp);
		if (c == null) return false;
		return dodger.isTileDangerousAt(c, tick)
			|| dodger.isTileDangerousAt(c, tick + 1)
			|| dodger.isTileDangerousAt(c, tick + 2);
	}

	/**
	 * True if an in-flight gas-bottle ({@value #PROJ_GAS_BOTTLE}) projectile's landing 3x3 would cover
	 * the camp tile. The projectile targets a vent; its gas forms a 3x3 centred there, so the camp is
	 * threatened when it lies within one tile of the projectile's target (template space).
	 */
	private boolean campThreatenedByProjectile(WorldPoint templateCamp)
	{
		for (MirrorState.ProjectileRecord r : mirror.getProjectileRecords())
		{
			if (r.id != PROJ_GAS_BOTTLE || r.projectile == null) continue;
			LocalPoint lp = r.projectile.getTarget();
			if (lp == null) continue;
			WorldPoint landInst = WorldPoint.fromLocal(client, lp);
			if (landInst == null) continue;
			WorldPoint landT = InstanceCoords.toTemplate(client, landInst);
			if (landT == null) continue;
			if (Math.abs(templateCamp.getX() - landT.getX()) <= 1
				&& Math.abs(templateCamp.getY() - landT.getY()) <= 1) return true;
		}
		return false;
	}

		/** True while Duke is in his damage-reduction / immune ("gas shield") window. */
	private boolean isShieldWindow()
	{
		if (DukeAutoIDs.VARBIT_SHIELD < 0) return false;   // not captured yet
		return mirror.getVarbit(DukeAutoIDs.VARBIT_SHIELD, 0) != 0;
	}

	// -----------------------------------------------------------------
	// Death.
	// -----------------------------------------------------------------

	private void onDukeDead(int tick)
	{
		// Turn ALL our prayers off on death -- the overheads AND the offensive Piety (ensureOnlyOn only
		// touches prayers in the managed set, so we widen it beyond OVERHEAD_SET to include Piety).
		prayers.clearScheduled();
		java.util.Set<Prayer> manageOff = new java.util.HashSet<>(PrayerSwapper.OVERHEAD_SET());
		manageOff.add(Prayer.PIETY);
		prayers.ensureOnlyOn(tick, java.util.Collections.emptySet(), manageOff);
		superCombatBoost.reset();
		toxicCloudSpawnTick.clear();
		extremitySpawnTick.clear();
		ventAnchorTick = null;   // vents restart with the new kill -> re-sync from the next witnessed spawn
		ventWatchStart = tick;
		holdForHazard = false; holdGoal = null;
		dukeAttackTick = -100; dukePrevAnim = -1;
		dukeAttackPhase = -1; dukeEnraged = false; gazeTick = -100; gazeActive = false; feedingStarted = false; attackDelayUntil = -100;
		fightCamp = null;
		fightClickCampNext = false;
		lastPickItemId = Integer.MIN_VALUE;
		picksRequested = 0;
		preApproachDone = false;
		pickInterrupted = false;
		collectIdleSinceTick = -1;
		vatsDone.clear();
		vatFillOrder.clear();
		vatState.clear();
		pendingVat = null;
		// Re-run the full prep (collect -> brew) for the next Duke only if we don't already have the 2
		// Arder/Musca potions needed to wake him; if we do, skip straight to feeding.
		int potions = mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION);
		prepStep = (potions >= DukeAutoIDs.ARDER_MUSCA_POTION_GOAL)
			? PrepStep.FEED_DUKE
			: PrepStep.COLLECT_ARDER;
		lastPrepWalkTarget = null;
		lastWalkTarget = null;
		log.info("[duke] death detected tick={} -- prayers off, trackers reset; potions={} -> prepStep={}",
			tick, potions, prepStep);
	}

	// -----------------------------------------------------------------
	// Consumption (copied pattern from Vardorvis).
	// -----------------------------------------------------------------

	private ConsumptionEngine.Request buildConsumptionRequest()
	{
		ConsumptionEngine.Request.Builder b = ConsumptionEngine.Request.builder();

		// Opportunistic eat + combo are the engine default; its overheal cap is the real gate.
		int pray = mirror.getPrayer();
		int maxPray = mirror.getMaxPrayer();
		int ceiling = Math.max(1, maxPray - (7 + maxPray / 4));
		if (pray <= ceiling - DukeSupply.PROFILE.prayerSipJitterWindow)
		{
			b.requireSip(SupplyProfile.DrinkKind.PRAYER);
		}
		else if (pray <= ceiling)
		{
			b.opportunisticSip(SupplyProfile.DrinkKind.PRAYER);
		}

		if (mirror.getRunEnergy() <= DukeSupply.PROFILE.runEnergySipStam
			&& DukeSupply.PROFILE.hasPotion(SupplyProfile.DrinkKind.STAMINA))
		{
			b.opportunisticSip(SupplyProfile.DrinkKind.STAMINA);
		}

		// Melee combat-boost (divine super combat preferred, else super combat). Enable it ONLY from the
		// FEED_DUKE step onward -- before we wake him there's no fight, so an early sip would just decay
		// and waste the boost. Keep it topped off through the awake fight; otherwise leave it idle.
		boolean boostNow = duke != null && (isDukeAwake() || prepStep == PrepStep.FEED_DUKE);
		if (!boostNow)
		{
			superCombatBoost.reset();
		}
		else
		{
			superCombatBoost.requestSips(b, mirror.getTickCounter(),
				client.getBoostedSkillLevel(Skill.STRENGTH),
				client.getRealSkillLevel(Skill.STRENGTH),
				SupplyProfile.DrinkKind.DIVINE_SUPER_COMBAT,
				SupplyProfile.DrinkKind.SUPER_COMBAT,
				DukeAutoIDs.SUPER_COMBAT_REDRINK_MIN_BOOST);
		}

		return b.build();
	}

	private void applyConsumption(ConsumptionEngine.Result r, boolean allowEat)
	{
		if (r == null || !r.hasAction()) return;
		// Food is eaten only when allowEat -- in the fight that's restricted to the camp-switch run (this
		// is a chip-damage fight, so we never eat while camped). Drinks always process (no eat delay).
		if (allowEat && r.primaryEat != null) router.useInventoryItem(r.primaryEat.itemId, "Eat");
		if (allowEat && r.comboEat   != null) router.useInventoryItem(r.comboEat.itemId, "Eat");
		// Eating delays our next attack (food 3t; + combo food 2t more = 5t). A potion sip adds none,
		// so the delay is computed from the food only -- shark+potion+kara is still 5t, shark+potion 3t.
		// Gate canAttack on this so we don't burn a wasted click (and desync lastAttackTick) mid-delay.
		if (allowEat && r.primaryEat != null)
		{
			int t = mirror.getTickCounter();
			int delay = EAT_DELAY_TICKS + (r.comboEat != null ? COMBO_EAT_EXTRA_TICKS : 0);
			attackDelayUntil = Math.max(attackDelayUntil, t + delay);
			log.info("[duke-eat] t={} ate food (combo={}) -> next attack >= t{}", t, r.comboEat != null, attackDelayUntil);
		}
		if (r.drink      != null)
		{
			SupplyProfile.PotionEntry pot = DukeSupply.PROFILE.potion(r.drink);
			if (pot == null) return;
			int sipId = findLowestDoseItem(pot);
			if (sipId > 0) router.useInventoryItem(sipId, "Drink");
			if (r.drink == SupplyProfile.DrinkKind.DIVINE_SUPER_COMBAT)
				superCombatBoost.onDivineSipped(mirror.getTickCounter());
		}
	}

	private int findLowestDoseItem(SupplyProfile.PotionEntry pot)
	{
		for (int i = 0; i < pot.doseIds.length; i++)
		{
			if (mirror.countItem(pot.doseIds[i]) > 0) return pot.doseIds[i];
		}
		return -1;
	}

	// -----------------------------------------------------------------
	// Boss lookup + geometry.
	// -----------------------------------------------------------------

	/**
	 * Find Duke without depending on the exact awake id (12191 is an unconfirmed guess -- if the live
	 * awake id differs, an id-only match returns null the moment he wakes and the whole fight stalls).
	 * A known dormant id (inactive/asleep) wins when present; otherwise we fall back to ANY live NPC
	 * named "Duke Sucellus", which catches the attackable form whatever its id. The dead body is treated
	 * as gone so death detection still drops us out of the fight.
	 */
	private NPC findDuke()
	{
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return null;
		NPC byId = null, byName = null;
		for (NPC n : npcs)
		{
			if (n == null) continue;
			int id = n.getId();
			if (id == DukeAutoIDs.NPC_DUKE_DEAD) continue;   // dead body -> treat as gone
			if (id == DukeAutoIDs.NPC_DUKE_INACTIVE
				|| id == DukeAutoIDs.NPC_DUKE_ASLEEP
				|| id == DukeAutoIDs.NPC_DUKE_AWAKE)
			{
				byId = n;
			}
			String nm = n.getName();
			if (byName == null && nm != null && nm.toLowerCase().contains("duke sucellus"))
			{
				byName = n;
			}
		}
		return byId != null ? byId : byName;
	}

	/**
	 * Awake = the attackable live form: Duke is present and NOT in a dormant (inactive/asleep) or dead
	 * state. Defined by exclusion rather than an exact awake id so it holds even if the live awake id
	 * isn't 12191.
	 */
	private boolean isDukeAwake()
	{
		if (duke == null) return false;
		if (sawDeathAnim) return false;   // death animation playing -> no longer a live fight
		int id = duke.getId();
		return id != DukeAutoIDs.NPC_DUKE_INACTIVE
			&& id != DukeAutoIDs.NPC_DUKE_ASLEEP
			&& id != DukeAutoIDs.NPC_DUKE_DEAD;
	}

	/** Duke is in a feedable dormant form (inactive/asleep) -- NOT awake, dying, or dead. */
	private boolean isDukeDormant()
	{
		return duke != null
			&& (duke.getId() == DukeAutoIDs.NPC_DUKE_INACTIVE || duke.getId() == DukeAutoIDs.NPC_DUKE_ASLEEP);
	}

	/** Duke's current HP from the shared DT2 boss HP widget, or -1 if it isn't readable this tick. */
	private int dukeHp()
	{
		int[] hp = WidgetReader.fraction(client, WIDGET_BOSS_HP);
		return hp == null ? -1 : hp[0];
	}


	// -----------------------------------------------------------------
	// Awaken-ordering diagnostic (TEMPORARY). Logs the exact tick of the Duke NPC id
	// transition and of the "Duke Sucellus awakens..." chat message so we can see which
	// fires first on this client AND confirm the true attackable (awake) NPC type id.
	// The NPC filter matches on the "Duke" name as well as the known ids, so an awake id
	// that differs from NPC_DUKE_AWAKE (12191) is still captured. Strip once confirmed.
	// -----------------------------------------------------------------

	/** NpcChanged forwarder: logs any id transition involving Duke, with the current tick. */
	public void onNpcChangedDiag(net.runelite.api.events.NpcChanged e)
	{
		NPC npc = e.getNpc();
		if (npc == null) return;
		int newId = npc.getId();
		int oldId = e.getOld() == null ? -1 : e.getOld().getId();
		String name = npc.getName();
		boolean dukeInvolved = (name != null && name.toLowerCase().contains("duke"))
			|| isDukeId(newId) || isDukeId(oldId);
		if (!dukeInvolved) return;
		log.info("[duke-awaken] tick={} NpcChanged name='{}' oldId={} -> newId={} index={} tile={}",
			mirror.getTickCounter(), name, oldId, newId, npc.getIndex(), npc.getWorldLocation());
	}

	/** ChatMessage forwarder: logs any Duke/awaken message, with the current tick. */
	public void onChatMessageDiag(net.runelite.api.events.ChatMessage e)
	{
		String msg = e.getMessage();
		if (msg == null) return;
		String lower = msg.toLowerCase();
		// DEBUG: log EVERY chat message while a gaze is active, so a "caught"/damage line (if any) shows
		// up alongside the gaze trace.
		if (gazeActive)
		{
			log.info("[gaze-dbg] tick={} off={} chat='{}'", mirror.getTickCounter(), mirror.getTickCounter() - gazeTick, msg);
		}
		// Gaze resolved: "You manage to avoid Duke Sucellus' gaze." -> safe to resume attacking. This is
		// the authoritative end of the gaze window (apostrophe encoding varies, so match loosely).
		if (lower.contains("avoid") && lower.contains("gaze"))
		{
			if (gazeActive)
			{
				Player p = mirror.getLocalPlayer();
				log.info("[gaze-dbg] tick={} AVOIDED (chat); off={} interactingDuke={} pAnim={} lastAttack={} (gap {})",
					mirror.getTickCounter(), mirror.getTickCounter() - gazeTick,
					(p != null && duke != null && p.getInteracting() == duke),
					(p == null ? -999 : p.getAnimation()), lastAttackTick, mirror.getTickCounter() - lastAttackTick);
			}
			gazeActive = false;
		}
		if (!lower.contains("awaken") && !lower.contains("duke")) return;
		log.info("[duke-awaken] tick={} ChatMessage type={} msg='{}'",
			mirror.getTickCounter(), e.getType(), msg);
	}

	private static boolean isDukeId(int id)
	{
		return id == DukeAutoIDs.NPC_DUKE_INACTIVE
			|| id == DukeAutoIDs.NPC_DUKE_ASLEEP
			|| id == DukeAutoIDs.NPC_DUKE_AWAKE;
	}

	private NPC findNpcAny(int... ids)
	{
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return null;
		for (NPC n : npcs)
		{
			if (n == null) continue;
			for (int id : ids) if (n.getId() == id) return n;
		}
		return null;
	}

	private boolean canAttack(int tick)
	{
		Player player = mirror.getLocalPlayer();
		if (player == null || duke == null) return false;
		if (tick - lastAttackTick < WEAPON_COOLDOWN) return false;
		if (tick < attackDelayUntil) return false;   // still inside an eat / combo-eat attack delay
		if (player.getInteracting() == duke) return false;   // already attacking
		// Gaze: the gaze resolves at gazeTick + offset (one tick earlier in enrage). An attack's Duke
		// interaction + animation linger one tick, and the camp-click that breaks the lock only lands the
		// tick AFTER the attack -- so an attack on the resolve-1 tick is still "attacking" at the resolve
		// and gets us caught (logged: attack at off=2 -> failed at off=3). Block BOTH the resolve tick and
		// the one before it: the last attack then lands at resolve-2, its camp-click breaks the lock at
		// resolve-1, and we're idle (not interacting) when the gaze resolves. We still attack the rest.
		if (gazeActive)
		{
			int resolve = gazeTick + (dukeEnraged ? GAZE_HIT_OFFSET_ENRAGE : GAZE_HIT_OFFSET);
			if (tick >= resolve - 1 && tick <= resolve) return false;
		}
		// Stay offset from Duke so our hit and his never land the same tick. Duke attacks every 5 ticks
		// pre-enrage, picking from his pool -- only some picks animate, so we drive our own steady 5t
		// cadence off his PHASE (not off seeing an animation each cycle, which would stall us whenever
		// he picks a non-melee attack). One 1-tick nudge sets a permanent stagger; from then on we fire
		// every 5 ticks forever. In enrage (his 4t vs our 5t) the phase no longer holds, so we fall
		// back to just skipping the ticks we actually see him swing.
		if (!dukeEnraged && dukeAttackPhase >= 0)
		{
			if (Math.floorMod(tick, WEAPON_COOLDOWN) == dukeAttackPhase) return false;
		}
		else if (tick == dukeAttackTick) return false;
		// No range check: the fight cadence only runs once we're settled on a camp tile (planFightMove
		// walks us there first and returns null only when we've arrived), and the camp tiles are valid
		// melee spots for both sides. So if we're here, just send the attack.
		return true;
	}

	/**
	 * Detect the START of Duke's attack animation ({@value #ANIM_DUKE_ATTACK}) and record its tick as
	 * the attack-offset anchor. Edge-triggered (prev anim != attack) so a multi-tick animation only
	 * registers once per swing, and it re-anchors every swing -> picks up the 5->4 enrage change for
	 * free. Logs each swing so the offset vs our own attacks can be eyeballed / tuned.
	 */
	private void trackDukeAttack(int tick)
	{
		// Enrage is HP-driven (authoritative): at/below ENRAGE_HP Duke switches to a 4-tick cycle. ONLY
		// read the widget once he's awake -- before the wake poison takes effect the readout isn't
		// accurate (and can briefly read 0/stale), so we require a real positive HP to avoid a false
		// enrage at the wake moment. HP only falls, so once enraged we stay enraged (reset per kill).
		// The anim cadence-shift below is kept only as a fallback if the HP widget can't be read.
		if (!dukeEnraged && isDukeAwake())
		{
			int hp = dukeHp();
			if (hp > 0 && hp <= ENRAGE_HP)
			{
				dukeEnraged = true;
				log.info("[duke-attack] t={} enrage: boss hp {} <= {} -> 4-tick cycle", tick, hp, ENRAGE_HP);
			}
		}
		int anim = (duke != null) ? duke.getAnimation() : -1;
		if (anim == ANIM_DUKE_ATTACK && dukePrevAnim != ANIM_DUKE_ATTACK)
		{
			dukeAttackTick = tick;
			int phase = Math.floorMod(tick, WEAPON_COOLDOWN);
			if (dukeAttackPhase < 0)
			{
				dukeAttackPhase = phase;   // first swing -> lock the 5t rhythm
			}
			else if (phase != dukeAttackPhase && !dukeEnraged)
			{
				// Fallback only (HP is primary): observed swings stay on the 5t grid (5*n % 5 is constant),
				// so a phase that no longer matches means the cadence changed -> enrage (4t).
				dukeEnraged = true;
				log.info("[duke-attack] t={} cadence shift (phase {} -> {}) -> enrage (fallback)", tick, dukeAttackPhase, phase);
			}
			log.info("[duke-attack] t={} Duke swing (anim {}); phase={} enraged={} lastOurAttack={} gap={}",
				tick, ANIM_DUKE_ATTACK, dukeAttackPhase, dukeEnraged, lastAttackTick, tick - lastAttackTick);
		}
		// Gaze tell: suppress ALL attacking until it resolves (the "avoid ... gaze" chat, or the window
		// timeout). We stay camped on the safe tile the whole time, so no movement is needed.
		if (anim == ANIM_DUKE_GAZE && dukePrevAnim != ANIM_DUKE_GAZE)
		{
			gazeTick = tick;
			gazeActive = true;
			log.info("[duke-gaze] t={} Duke gaze (anim {}) -> suppress all attacks until it resolves", tick, ANIM_DUKE_GAZE);
		}
		// Fallback resume: clear if the full gaze window elapsed without a chat clearing it.
		if (gazeActive && tick - gazeTick > GAZE_WINDOW_TICKS)
		{
			gazeActive = false;
			log.info("[duke-gaze] t={} gaze window elapsed ({}t, no chat) -> resume", tick, tick - gazeTick);
		}
		dukePrevAnim = anim;
	}

private boolean isInArenaTemplate()
	{
		WorldPoint tpl = InstanceCoords.toTemplate(client, mirror.getPlayerTile());
		return tpl != null && tpl.getRegionID() == DukeAutoIDs.ARENA_REGION;
	}

	private void updateOverlay()
	{
		if (overlay == null) return;
		overlay.setStat("active", active ? "yes" : "no");
		overlay.setStat("phase",  phase.current().name());
		overlay.setStat("hp",     mirror.getHp()     + "/" + mirror.getMaxHp());
		overlay.setStat("pray",   mirror.getPrayer() + "/" + mirror.getMaxPrayer());
		overlay.setStat("tick",   String.valueOf(mirror.getTickCounter()));
	}
}
