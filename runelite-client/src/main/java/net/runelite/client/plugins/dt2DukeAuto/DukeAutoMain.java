package net.runelite.client.plugins.dt2DukeAuto;

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
import net.runelite.client.plugins.autoBossCore.loot.LootCollector;
import net.runelite.client.plugins.autoBossCore.combat.Hazard;
import net.runelite.client.plugins.autoBossCore.combat.PrayerSwapper;
import net.runelite.client.plugins.autoBossCore.combat.SchedulingTileDodger;
import net.runelite.client.plugins.autoBossCore.combat.TelegraphedHazards;
import net.runelite.client.plugins.autoBossCore.combat.TilePathPredictor;
import net.runelite.client.plugins.autoBossCore.combat.ThrallManager;
import net.runelite.client.plugins.autoBossCore.consume.CombatBoostTracker;
import net.runelite.client.plugins.autoBossCore.consume.ConsumptionEngine;
import net.runelite.client.plugins.autoBossCore.consume.EatDelay;
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
	private final LootCollector lootCollector;   // shared post-death loot pass
	private WorldPoint lastCamTemplate = null;   // last overhead camera centre applied (template), to avoid re-spam
	private int lastCamZoom = -1;                // last overhead camera zoom applied, to avoid re-spam
	/** getTickCount() of the kill, anchoring the post-death loot pass (-1 = not dead / loot done). */
	private int lastDeathTick = -1;
	/** Ticks to look back when anchoring the loot pass at DESPAWN (fallback): the drops land at the death anim,
	 *  a few ticks before Duke despawns, so a despawn anchor must reach back to include them. */
	private static final int DUKE_LOOT_LOOKBACK = 12;
	/** Cleanup decant pass: after loot, combine half pizzas + consolidate potion doses before prep. */
	private boolean decantActive = false;
	private int decantStartTick = -1;
	private int lastDecantTick = -100;
	private static final int DECANT_COOLDOWN_TICKS = 1;   // at most ONE decant action per tick (no spam)
	private static final int CLEANUP_MAX_TICKS = 60;      // hard cap so the between-fights cleanup never hangs
	private static final int[][] DECANT_POTION_DOSES = {
		DukeAutoIDs.PRAYER_POT_DOSES, DukeAutoIDs.SUPER_COMBAT_DOSES,
		DukeAutoIDs.DIVINE_SUPER_COMBAT_DOSES, DukeAutoIDs.STAMINA_DOSES };

	// --- Boss state -------------------------------------------------
	private final ConsumptionEngine engine;
	private final ThrallManager thrall;
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
	/** Shared eat/combo-eat attack-delay tracker (see autoBossCore EatDelay). */
	private final EatDelay eatDelay = new EatDelay();
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

	/** Weapon attack cooldown in ticks. Duke (and our weapon) run a 5-tick attack cycle. */
	private static final int WEAPON_COOLDOWN = 5;
	// Eat/combo-eat attack delay now lives in autoBossCore EatDelay (eatDelay field).
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

	/** Vent cycle: tick group A spawns (mod-30 phase ref); null until synced. */
	private Integer ventAnchorTick = null;
	/** Extremity cone cycle (deterministic B->C->A, 15t): tick of the reference B spawn = cycle t0. */
	private Integer extremityAnchorTick = null;
	private int extremityWatchStart = -1;
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
	/** Exit sequence: sticky flag that leg 1 (reach the lane END) is done, so leg 2 (back to the lane START)
	 *  doesn't re-trigger leg 1 as we move away from the end. Reset on item change / step completion. */
	private boolean exitEndReached = false;
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
	/** The salt deposit (template tile) we've committed to mine; pinned so vent cycling can't re-flip it
	 *  mid-approach. Re-chosen only in the hazard-free zone (or first pick / if it vanishes). */
	private WorldPoint saltTargetTpl;
	/** Walkable approach tile (TEMPLATE) of the chosen salt deposit, so salt transit has a reachable dodge goal. */
	private WorldPoint saltApproachTpl;
	/** When true, feed Duke the potion at the camp to wake him and start the fight (prep complete). */
	private static final boolean FEED_DUKE_ENABLED = true;
	/** The two fermentation-vat tiles (template coords; converted to instance at runtime). */
	private static final WorldPoint VAT_EAST = new WorldPoint(3043, 6438, 0);
	private static final WorldPoint VAT_WEST = new WorldPoint(3033, 6438, 0);
	/** NPC that SPAWNS when a vat is successfully filled -- the reliable fill-confirm signal. */
	private static final int NPC_VAT_FILL = 12197;
	/** Order the vats were filled (template tiles), so we collect the first-filled first. */
	private final java.util.List<WorldPoint> vatFillOrder = new java.util.ArrayList<>();
	/** Potion count captured just before a Collect-from click (collect confirm). */
	private int pendingCollectBaseline = 0;
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
	 * Extra tick(s) AFTER a hazard's active window to keep its tile dangerous. Mirrors the leading
	 * {@link #FALLING_DODGE_LEAD_TICKS}: a dodge RECOVERY walk (2 tiles/tick) can re-enter a tile on
	 * the exact tick the hazard is dying, so holding it one tick longer stops us dodging one hazard
	 * straight into a co-spawned one (e.g. 1447 south, then north into the extremity cone).
	 */
	private static final int HAZARD_TRAIL_TICKS = 1;
	/** Live+imminent hazard tiles (dangerous now..+DODGE_AVOID_HORIZON), rebuilt every tick from the
	 *  dodger -- the single active-hazard set the holistic dodge works off, like the Whisperer tentacle
	 *  hazardAvoid. The dodge triggers when we are IN it and never lands on a tile that is IN it. */
	private java.util.Set<WorldPoint> dodgeAvoid = java.util.Collections.emptySet();
	/** Per-offset danger sets [tick..tick+DODGE_AVOID_HORIZON] (index 0 = this tick), for earliest-hit. */
	private java.util.List<java.util.Set<WorldPoint>> dodgeDangerByTick = java.util.Collections.emptyList();
	/** How many ticks ahead a tile must stay clear to count as a safe dodge/rest tile. */
	private static final int DODGE_AVOID_HORIZON = 3;
	/** How far ahead the dodge flood looks to prefer tiles that STAY safe (get off the hazard line). */
	private static final int DODGE_LOOKAHEAD = 8;
	/** Ticks a path tile must stay clear AFTER we'd arrive on it -- so we never step onto a tile that is
	 *  hit a tick or two later (we'd be standing on it when it lands and dodge too late). */
	private static final int PATH_DWELL_TICKS = 1;
	/** How many run-ticks of the path ahead we check for hazards. Covers a 1447's telegraph->impact (~4
	 *  ticks) so we SEE a fall coming and step around / wait, rather than committing a run onto it. Each
	 *  landing is still judged at ITS OWN arrival tick, so a far tile is only flagged if it will actually be
	 *  dangerous when we get there -- not merely because it's dangerous now. */
	private static final int CHECK_HORIZON_TICKS = 4;
	// Dodge reaction horizon: only react to a path hazard this many ticks out. Short = calm (run until a
	// hazard is imminent, then one hold/sidestep) instead of jittering at far hazards.
	private static final int DODGE_REACT_HORIZON = 2;
	/** 8-way steps for the dodge flood (orthogonal + diagonal). */
	private static final int[][] DODGE_DIRS8 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
	private static final java.util.function.Predicate<WorldPoint> ALWAYS_PASSABLE = pt -> true;
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
	private static final int VENT_GROUP_INTERVAL  = 10;   // ticks between consecutive group spawns
	private static final int VENT_DANGER_FROM     = 3;    // padded gas-window start (real gas = +4)
	private static final int VENT_DANGER_TO       = 12;   // padded gas-window end   (real gas = +11)
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
		ArenaCamera camera,
		LootCollector lootCollector)
	{
		this.client  = client;
		this.mirror  = mirror;
		this.router  = router;
		this.dodger  = dodger;
		this.prayers = prayers;
		this.camera  = camera;
		this.lootCollector = lootCollector;

		this.engine = new ConsumptionEngine(DukeSupply.PROFILE, mirror);
		this.thrall = new ThrallManager(DukeAutoIDs.THRALL_CAST_WIDGET, "Duke thrall");

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
		lastCamTemplate = null;
		applyArenaCamera();   // frame the overhead on the current phase's centre (prep until Duke is awake)
		ventAnchorTick = null;   // fresh run -> re-sync the vent cycle from the next witnessed spawn
		ventWatchStart = -1;
		extremityAnchorTick = null; extremityWatchStart = -1;   // re-sync the extremity cycle too
		holdForHazard = false; holdGoal = null; saltTargetTpl = null; saltApproachTpl = null; decantActive = false; decantStartTick = -1;
		dukeAttackTick = -100; dukePrevAnim = -1;
		dukeAttackPhase = -1; dukeEnraged = false; gazeTick = -100; gazeActive = false; feedingStarted = false; eatDelay.reset(); thrall.reset();
		sawDeathAnim = false;
		vatState.clear();
		active = true;
	}
	public void    stop()          { camera.reset(); active = false;  }
	public DukePhase currentPhase(){ return phase.current(); }

	/**
	 * Keep the overhead camera framed on the right spot: the FIGHT centre while Duke is awake (FIGHT / SHIELD),
	 * else the PREP centre. Only re-sends when the target TEMPLATE tile changes (so we don't reset the focal point
	 * every tick), and skips until the chosen tile resolves in the loaded instance.
	 */
	private void applyArenaCamera()
	{
		// Zoom IN (tight, 400) only once we are READY TO FIGHT -- i.e. we have started FEEDING/waking Duke. We do
		// NOT zoom on merely holding the brewed potions, since that fires a step early while we're still at the vat
		// containers. feedingStarted is set at the first feed and stays set through the whole kill (the potions are
		// spent to 0 at the wake), so it frames the fight; it resets at death, giving one clean switch back to the
		// PREP framing (wide, 250). Keyed off feed state, not the phase, so the death->respawn phase churn can't flip it.
		boolean fighting = feedingStarted;
		WorldPoint wantTemplate = fighting ? DukeAutoIDs.ARENA_CAMERA_CENTER_FIGHT : DukeAutoIDs.ARENA_CAMERA_CENTER;
		int wantZoom = fighting ? DukeAutoIDs.ARENA_CAMERA_ZOOM_FIGHT : DukeAutoIDs.ARENA_CAMERA_ZOOM;
		if (wantTemplate.equals(lastCamTemplate) && wantZoom == lastCamZoom) return;   // already framed there
		WorldPoint live = toInstance(wantTemplate);
		if (live == null) return;                           // not in the loaded instance yet -> retry next tick
		camera.overhead(live, wantZoom);
		lastCamTemplate = wantTemplate;
		lastCamZoom = wantZoom;
	}
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
				if (!sawDeathAnim) lastDeathTick = client.getTickCount();   // drops land at the death anim -> anchor loot here, not the later despawn
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
		syncExtremityCycle(tick);       // learn the B->C->A cone cycle from the first witnessed spawn
		pushPredictedExtremities(tick); // project the KNOWN future cone attacks (deterministic once synced)
		pushToxicCloudHazards(tick);   // both phases: toxic cloud 3x3 (prep vents + fight gas bottles)
		syncVentCycle(tick);           // prep: learn the vent rotation from the first witnessed 12198 spawn
		// predicted vents are NOT fed to the dodger (see ventGassedAt) -- reactive dodging uses LIVE gas only

		dodger.onGameTick(tick);
		updateDukeHazards(tick);   // rebuild the live+imminent hazard set BEFORE any dodge/plan reads it
		prayers.onGameTick(tick);

		// Attack-style prayer swap runs every tick (free action, independent of movement).
		watchStylePrayer(tick);

		updateOverlay();

		// Diagnostic heartbeat every ~5 ticks while in arena.
		if (isInArenaTemplate() && tick - lastDiagTick >= 5)
		{
			lastDiagTick = tick;
		}

		if (!active) return;

		applyArenaCamera();   // prep centre vs fight centre, re-framed on phase change

		// POST-DEATH LOOT: drain Duke's drop pile (richest first, what fits) before the next-kill prep walks off.
		// Holds the tick while collecting; clears itself once the pile is gone / nothing fits / it times out.
		if (lastDeathTick >= 0)
		{
			if (lootCollector.collect(router, lastDeathTick, null) != null) return;
			lastDeathTick = -1;
			decantActive = true; decantStartTick = tick;   // loot done -> run the cleanup decant pass next
		}

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
				if (preT != null && atLaneStartPassby(me, preT)) preApproachDone = true;
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

		// BETWEEN-FIGHTS CLEANUP (runs AFTER the dodge above, so the run to camp is hazard-dodged). First run back
		// to a camp tile (either one -- positions us to feed Duke immediately, no separate walk later), then top
		// HP/prayer/run off and consolidate pizzas / potion doses there. Bounded by CLEANUP_MAX_TICKS.
		if (decantActive)
		{
			if (tick - decantStartTick <= CLEANUP_MAX_TICKS)
			{
				WorldPoint mc = mirror.getPlayerTile();
				if (mc != null && !atAnyCamp(mc))
				{
					WorldPoint campTpl = nearestSafeCamp(mc, tick);
					WorldPoint campI = toInstance(campTpl);
					if (campI != null && pathWillBeHit(mc, campI, tick, "to camp", 1))
					{ holdForHazard = true; holdGoal = campI; return; }   // don't step into a hazard -- wait a tick
					walkArena(campTpl, tick, "cleanup: run to camp");
					return;
				}
				if (cleanupTopOffTick(tick)) return;   // 1) top HP/prayer/run off at camp
				if (decantTick(tick)) return;          // 2) then combine pizzas + consolidate potion doses
			}
			decantActive = false;
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

		// 4. Supply consumption (CAMPED path; the camp-switch RUN consumes in the movement block above). Duke is
		// NOT a top-off fight -- sipping/eating between attacks while camped cancels swings and costs DPS. So in
		// FIGHT we consume ONLY during the GAZE, where our attacks are already suppressed so the window is free
		// (the camp-switch run is the other free window). Outside FIGHT there's no swing to protect -> consume freely.
		boolean inFight = phase.current() == DukePhase.FIGHT;
		boolean consumeOk = !inFight || gazeActive;
		if (consumeOk)
		{
			// Free window (gaze / camp run / prep) -> eating is fine; block it only during the PREP FEED_DUKE wake
			// step so it can't compete with the feed. In FIGHT prepStep is left at FEED_DUKE, so don't let that
			// stop eating in the gaze window. Drinks always process; the overheal cap stops food going over.
			boolean allowEat = phase.current() == DukePhase.FIGHT || prepStep != PrepStep.FEED_DUKE;
			ConsumptionEngine.Request req = buildConsumptionRequest();
			ConsumptionEngine.Result res = engine.evaluate(req);
			applyConsumption(res, allowEat);
			// DPS thrall upkeep: recast ASAP once expired (6 prayer/cast, ~magic-level ticks), but only on this
			// clean tick AND only when consumption didn't already use the tick -- so the cast never collides with
			// a drink/eat or cancels a swing.
			if (!res.hasAction())
				thrall.maintain(tick, mirror, router, thrallConditionsOk());
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
		// TODO (capture): trace player/duke anim, interacting + HP here for gaze-hit timing.
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
		if (itemId > 0 && have >= need)
		{
			// Done collecting. Leave in LANE-ALIGNED legs so we stay on the safe outboard column instead of
			// cutting straight across to the next ingredient: first STAND ON the lane END (exitTile, exact), then
			// head back PAST the lane START (preTile) -- the END is a real checkpoint, the START only a pass-through
			// (reach its latitude or go south of it). planNextTile's dodge handles hazards on both legs.
			WorldPoint meNow = mirror.getPlayerTile();
			// Leg 1: STAND ON the lane END tile (exact) before turning back. Being merely NEAR it let the exit
			// skip straight to 'to lane start' when the last mushroom was picked close to the end.
			if (!exitEndReached && exitTile != null && meNow != null && !atTemplate(meNow, exitTile))
			{
				WorldPoint endI = toInstance(exitTile);
				if (endI != null && pathWillBeHit(meNow, endI, tick, "to lane end"))
				{ holdForHazard = true; holdGoal = endI; return false; }   // path hit -> wait (dodge can escape toward it)
				walkArena(exitTile, tick, "prep: to lane end");
				return false;
			}
			exitEndReached = true;   // sticky: leg 1 done; don't re-trigger it as we turn back toward the start
			// Leg 2: the lane START is a PASS-THROUGH waypoint, not a stand-on checkpoint. Its only purpose is to
			// drop us SOUTH of the extremity apex line, so the exit is DONE the moment we're at or south of the
			// start's latitude -- the real goal is the next ingredient (next mushroom / salt), which takes over.
			// We descend in whatever column we're in (below): on the edge lane that's the real start tile; if a
			// dodge pushed us inward we just keep running south in that column rather than crabbing back to the
			// edge. The dodge itself prefers the extremity-safe edge spots (see safeNeighbour / isExtremitySafeSpot).
			WorldPoint meTpl = meNow != null ? InstanceCoords.toTemplate(client, meNow) : null;
			// EXITED once we're at/south of the start band OR anywhere in the expanded corner zone for this lane
			// (3030-3032 / 3046-3048, y6436-6440). The whole area counts, then the next ingredient takes over -- but
			// the per-tick dodge keeps running, so any hazard inside that corner is still dodged in transit.
			boolean exited = inLaneExitZone(meNow, preTile)
				|| (preTile != null && meTpl != null && meTpl.getY() <= preTile.getY());
			if (preTile != null && meTpl != null && !exited)
			{
				// Still north of the band: head to the start LATITUDE in the column we're ACTUALLY in -- (myX, startY),
				// not the fixed edge tile; pushed inward by a dodge we just keep running south in our own column.
				WorldPoint colStart = new WorldPoint(meTpl.getX(), preTile.getY(), preTile.getPlane());
				WorldPoint colStartI = toInstance(colStart);
				// Don't click into an obvious hazard: if the immediate next landing is hit, wait ONE tick on our (safe)
				// tile instead of stepping forward. (planNextTile only guards a walk already in flight; the FIRST click
				// of a new walk, issued while idle, is otherwise unchecked.) Once moving, the per-tick dodge takes over.
				if (colStartI != null && pathWillBeHit(meNow, colStartI, tick, "exit", 1))
				{ holdForHazard = true; holdGoal = colStartI; return false; }
				walkArena(colStart, tick, "prep: exit south in-column");
				return false;
			}
			exitEndReached = false;   // both legs done -> reset for the next collection step
			return true;
		}

		// Reset pick-tracking + the approach flag when the collected item changes.
		if (itemId != lastPickItemId)
		{
			lastPickItemId = itemId;
			picksRequested = 0;
			preApproachDone = false;
			exitEndReached = false;
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
			if (me != null && atLaneStartPassby(me, preTile))
			{
				preApproachDone = true;
			}
			else
			{
				// Only hold for an IMMINENT landing (horizon 1), not any hazard 4 ticks out on the whole path -- a
				// human closes the distance and run-skips the tile rather than standing at the start. Once we're
				// moving, planNextTile's per-tick dodge (escape-first / go-around / run-through) handles the rest.
				if (pathWillBeHit(me, toInstance(preTile), tick, "pre", 1)) return false;
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
			picksRequested = have;               // drop the stale in-flight -> re-click below
			collectIdleSinceTick = -1;
		}
		if (picksRequested >= need) return false;       // all needed picks already requested -- wait for them

		GameObject go = firstReachableById(objId, tick, preTile);   // mushrooms: lock onto the nearest entry-column one (ranked by TEMPLATE distance to the fixed pre-tile); salt (preTile==null): prefer a hazard-free approach among the two deposits
		if (go == null) return false;   // depleted / regrowing / not in scene / all blocked -- wait

		WorldPoint rock = go.getWorldLocation();
		// SALT (no pre-tile): walk to a WALKABLE approach tile FIRST, then interact once adjacent. Clicking the
		// deposit directly makes the transit goal the deposit's own IMPASSABLE tile, whose BFS path is empty, so
		// both pathWillBeHit and planNextTile read 'clear' and we walk straight through vents with no dodge. A
		// real walkable goal gives planNextTile a path to dodge along (mushrooms already do this via the pre-tile).
		if (preTile == null && rock != null && saltApproachTpl != null)
		{
			WorldPoint meTpl = me == null ? null : InstanceCoords.toTemplate(client, me);
			WorldPoint rockTpl = InstanceCoords.toTemplate(client, rock);
			boolean adjacent = meTpl != null && rockTpl != null
				&& Math.abs(meTpl.getX() - rockTpl.getX()) + Math.abs(meTpl.getY() - rockTpl.getY()) == 1;
			if (!adjacent)
			{
				WorldPoint apprInst = toInstance(saltApproachTpl);
				if (apprInst != null && pathWillBeHit(me, apprInst, tick, "salt-approach"))
				{ holdForHazard = true; holdGoal = apprInst; return false; }   // path hit -> wait (dodge can escape toward it)
				walkArena(saltApproachTpl, tick, "prep: to salt approach");
				return false;
			}
		}
		// Don't click the object if the (now short) walk to it would cross a hazard -- wait where we are.
		if (rock != null && pathWillBeHit(me, rock, tick, "obj")) return false;

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
	private GameObject firstReachableById(int objId, int tick, WorldPoint nearestAnchorTpl)
	{
		java.util.List<GameObject> gos = SceneScanner.findByIds(client, objId);
		if (gos == null || gos.isEmpty()) return null;
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return gos.get(0);

		// Rank reachable candidates: a HAZARD-FREE approach beats a shorter but gassed one, so when
		// the near salt deposit is blocked by a toxic vent we walk to the far one instead of waiting.
		// Shortest path breaks ties within the same safety class -- which keeps us on the deposit we
		// are already standing at (len ~0) rather than ping-ponging as vents cycle.
		// SALT (nearestAnchorTpl == null): the LEFT deposit (smaller template x) is the DEFAULT; take the RIGHT
		// one only when the LEFT is blocked. Pre-compute the leftmost deposit's template x.
		int saltLeftTplX = Integer.MAX_VALUE;
		if (nearestAnchorTpl == null)
			for (GameObject g0 : gos)
			{
				WorldPoint t0 = g0.getWorldLocation();
				WorldPoint tpl0 = t0 == null ? null : InstanceCoords.toTemplate(client, t0);
				if (tpl0 != null) saltLeftTplX = Math.min(saltLeftTplX, tpl0.getX());
			}
		GameObject best = null;
		int bestLen = Integer.MAX_VALUE;
		int bestPrio = -1;
		int bestCheb = Integer.MAX_VALUE;
		int[][] nbrs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (GameObject go : gos)
		{
			WorldPoint t = go.getWorldLocation();
			if (t == null) continue;
			// Nearest REACHABLE approach tile (a neighbour of the rock) + its path length. The rock's own
			// tile is impassable, so the hazard check must target the approach tile we would actually stand
			// on -- checking the rock tile returns an empty path (never "hit"), which made EVERY deposit read
			// as safe and the shortest (west) always win.
			int len = Integer.MAX_VALUE;
			WorldPoint approach = null;
			for (int[] d : nbrs)
			{
				WorldPoint nb = new WorldPoint(t.getX() + d[0], t.getY() + d[1], t.getPlane());
				java.util.List<WorldPoint> path = TilePathPredictor.path(
					client.getTopLevelWorldView(), me, nb, null, this::inArena, ARENA_PATH_BUDGET);
				if (!path.isEmpty() && path.size() < len) { len = path.size(); approach = nb; }
			}
			if (approach == null) continue;   // unreachable (walled off) -- skip
			if (nearestAnchorTpl != null)
			{
				// Mushrooms: LOCK onto the mushroom nearest the ENTRY COLUMN, ranked by TEMPLATE-space distance to
				// the fixed pre-tile. We pass the TEMPLATE pre-tile directly and convert each rock with toTemplate
				// (the same reliable conversion inArena uses). The earlier version ranked against toInstance(preTile),
				// which can return null and silently drop musca into the salt-style safe-first branch below -- exactly
				// the 'stray to the inner/left column' symptom. The per-tick reroute/hold still handles TRANSIENT
				// falls/cones on the approach; anchoring on the fixed pre-tile (not the live player) stops a mid-trip
				// sidestep from flipping the chosen column.
				WorldPoint rockTpl = InstanceCoords.toTemplate(client, t);
				int cheb = rockTpl == null ? Integer.MAX_VALUE - 1
					: Math.max(Math.abs(nearestAnchorTpl.getX() - rockTpl.getX()), Math.abs(nearestAnchorTpl.getY() - rockTpl.getY()));
				if (cheb < bestCheb || (cheb == bestCheb && len < bestLen)) { bestCheb = cheb; bestLen = len; best = go; }
				continue;
			}
			// SALT: the LEFT deposit is the DEFAULT. We take the RIGHT one ONLY when the LEFT is blocked -- i.e.
			// its standing tile is gassed by a vent (the vent-4/8 case). 'blocked' is read from the deterministic
			// vent cycle (ventGassedAt) plus any live gas, on the deposit's own approach tile over the next several
			// ticks, independent of where we stand. Priority: safe LEFT (3) > RIGHT (2, left-blocked only) > blocked LEFT (1).
			WorldPoint rockTpl = InstanceCoords.toTemplate(client, t);
			boolean isLeft = rockTpl != null && rockTpl.getX() == saltLeftTplX;
			boolean blocked = false;
			for (int k = 0; k <= 8; k++)
				if (dodger.isTileDangerousAt(approach, tick + k) || ventGassedAt(approach, tick + k)) { blocked = true; break; }
			int prio = isLeft ? (blocked ? 1 : 3) : 2;
			int saltCheb = Math.max(Math.abs(me.getX() - t.getX()), Math.abs(me.getY() - t.getY()));
			boolean better = (best == null) || prio > bestPrio || (prio == bestPrio && saltCheb < bestCheb);
			if (better) { best = go; bestPrio = prio; bestCheb = saltCheb; bestLen = len; }
		}
		// SALT only: pin the chosen deposit so vents cycling can't re-flip it mid-approach. Re-decide only in
		// the hazard-free zone (or on the first pick, or if the pinned deposit is no longer in the scene).
		if (nearestAnchorTpl == null && best != null)
		{
			WorldPoint bestTpl = InstanceCoords.toTemplate(client, best.getWorldLocation());
			if (saltTargetTpl != null && !inSaltZone(me))
			{
				for (GameObject go2 : gos)
				{
					WorldPoint gt = InstanceCoords.toTemplate(client, go2.getWorldLocation());
					if (gt != null && gt.equals(saltTargetTpl)) { best = go2; bestTpl = gt; break; }
				}
			}
			saltTargetTpl = bestTpl;
			saltApproachTpl = nearestApproachTpl(best, me);   // walkable neighbour of the FINAL deposit, for the transit dodge
		}
		return best != null ? best : gos.get(0);
	}

	/**
	 * Nearest REACHABLE orthogonal neighbour of {@code go} (a BFS-reachable approach tile), as a TEMPLATE
	 * point, or null if none path. Used so salt transit walks to a walkable tile the dodge can path along
	 * instead of clicking the deposit's own impassable tile (empty path == no dodge).
	 */
	private WorldPoint nearestApproachTpl(GameObject go, WorldPoint me)
	{
		if (go == null || me == null) return null;
		WorldPoint t = go.getWorldLocation();
		if (t == null) return null;
		int[][] nbrs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		WorldPoint best = null; int bestLen = Integer.MAX_VALUE;
		for (int[] d : nbrs)
		{
			WorldPoint nb = new WorldPoint(t.getX() + d[0], t.getY() + d[1], t.getPlane());
			java.util.List<WorldPoint> path = TilePathPredictor.path(
				client.getTopLevelWorldView(), me, nb, null, this::inArena, ARENA_PATH_BUDGET);
			if (!path.isEmpty() && path.size() < bestLen) { bestLen = path.size(); best = nb; }
		}
		return best == null ? null : InstanceCoords.toTemplate(client, best);
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
				{  pendingVat = null; }
				else if (tick - lastPrepActionTick >= VAT_ACTION_TIMEOUT) pendingVat = null;
				else return false;
			}
			else
			{
				if (mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION) > pendingCollectBaseline)
				{
					collected = mirror.countItem(DukeAutoIDs.ITEM_ARDER_MUSCA_POTION);
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

	/** On either camp tile (or its vent sidestep) -- 'doesn't matter which one' for the post-kill return. */
	private boolean atAnyCamp(WorldPoint meInst)
	{
		WorldPoint t = meInst == null ? null : InstanceCoords.toTemplate(client, meInst);
		if (t == null) return false;
		return t.equals(DukeAutoIDs.EAST_CAMP) || t.equals(DukeAutoIDs.WEST_CAMP)
			|| t.equals(EAST_CAMP_SIDESTEP) || t.equals(WEST_CAMP_SIDESTEP);
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

	/** True once we've PASSED BY the lane start (not necessarily standing on it): same edge column as
	 *  {@code preTileTpl} at its y or one tile south -- (3030/3048, 6441) or (3030/3048, 6440). The start is a
	 *  pass-through waypoint, not a stand-on checkpoint, so touching that band is enough to proceed. */
	private boolean atLaneStartPassby(WorldPoint meInst, WorldPoint preTileTpl)
	{
		if (meInst == null || preTileTpl == null) return false;
		WorldPoint t = InstanceCoords.toTemplate(client, meInst);
		return t != null && t.getX() == preTileTpl.getX()
			&& (t.getY() == preTileTpl.getY() || t.getY() == preTileTpl.getY() - 1);
	}

	/** Expanded exit corner for a lane, south of the extremity apexes: west x3030-3032 / east x3046-3048, y6436-6440.
	 *  Being anywhere in it counts as having EXITED the lane. It is NOT treated as blanket-safe -- the per-tick
	 *  dodge still runs there, so 1447s/vents inside it are dodged in transit to the next ingredient. */
	private boolean inLaneExitZone(WorldPoint meInst, WorldPoint preTileTpl)
	{
		if (meInst == null || preTileTpl == null) return false;
		WorldPoint t = InstanceCoords.toTemplate(client, meInst);
		if (t == null) return false;
		boolean west = preTileTpl.getX() <= DukeAutoIDs.EXTREMITY_SIDE_SPLIT_X;   // 3030 west / 3048 east
		int xLo = west ? 3030 : 3046, xHi = west ? 3032 : 3048;
		return t.getX() >= xLo && t.getX() <= xHi && t.getY() >= 6436 && t.getY() <= 6440;
	}

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

		// Just click the destination; the client paths there and planNextTile's per-tick dodge handles any
		// hazards on the way. No reroute here -- that double-steering caused the weird paths.
		WorldPoint target = dest;

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
			// Trailing tick(s): keep the tile hot just past impact so a recovery walk doesn't re-enter
			// it on the dying tick (see HAZARD_TRAIL_TICKS).
			for (int trail = 1; trail <= HAZARD_TRAIL_TICKS; trail++)
				dodger.addHazard(Hazard.staticTile(tile,
					impactTick + DukeAutoIDs.FALLING_IMPACT_ACTIVE_TICKS - 1 + trail, "duke-fall-trail"));
			// Log only falls on OUR lane region (west 3030-3032 / east 3046-3048) to cut noise --
			// registration above still covers BOTH lanes, so dodging is unaffected. An off-lane fall
			// (hzSide null) shouldn't happen, so log it as an anomaly if it ever does.
			Integer meSide = arenaSide(mirror.getPlayerTile());
			Integer hzSide = arenaSide(tile);
			if (hzSide == null || (meSide != null && meSide.equals(hzSide)))
				{}
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
				if (tick > end + HAZARD_TRAIL_TICKS) continue;

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

				int dangerFrom = attackTick - FALLING_DODGE_LEAD_TICKS;   // telegraph is harmless; only guard the landing
				int dangerTo   = end + HAZARD_TRAIL_TICKS;                // trailing tick: don't re-enter on the dying tick
				for (int tt = Math.max(tick, dangerFrom); tt <= dangerTo; tt++)
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
	 * Will a predicted toxic vent be GASSING the given tile at {@code absTick}? A pure query over the
	 * DETERMINISTIC cycle locked by {@link #syncVentCycle} -- it does NOT touch the shared dodger, so it can
	 * steer PLANNING (which salt deposit to walk to) WITHOUT making the reactive dodge/hold freeze for a vent
	 * that has not spawned yet. Feeding predictions into the dodger caused exactly that: we'd stand waiting
	 * for a phantom future window, then -- because a predicted spawn can be a tick or two off the real one --
	 * step straight into the live gas. Group gi spawns at anchor + gi*INTERVAL + k*period and gasses its 3x3
	 * sites over [+VENT_DANGER_FROM, +VENT_DANGER_TO].
	 */
	private boolean ventGassedAt(WorldPoint approachInst, int absTick)
	{
		if (ventAnchorTick == null || approachInst == null) return false;
		WorldPoint t = InstanceCoords.toTemplate(client, approachInst);
		if (t == null) return false;
		final int period = VENT_GROUPS.length * VENT_GROUP_INTERVAL;   // 30
		for (int gi = 0; gi < VENT_GROUPS.length; gi++)
		{
			boolean covers = false;
			for (WorldPoint siteT : VENT_GROUPS[gi])
				if (Math.abs(siteT.getX() - t.getX()) <= 1 && Math.abs(siteT.getY() - t.getY()) <= 1) { covers = true; break; }
			if (!covers) continue;   // this group never gasses this tile
			int base = ventAnchorTick + gi * VENT_GROUP_INTERVAL;
			int kLo = Math.floorDiv(absTick - VENT_DANGER_TO   - base, period);
			int kHi = Math.floorDiv(absTick - VENT_DANGER_FROM - base, period);
			for (int k = kLo; k <= kHi; k++)
			{
				int spawn = base + k * period;
				if (absTick >= spawn + VENT_DANGER_FROM && absTick <= spawn + VENT_DANGER_TO) return true;
			}
		}
		return false;
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
				{}
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
	 * used for the salt-deposit choice via ventGassedAt (a pure query, never a dodger hazard); reactive
	 * dodging and the camp sidestep stay driven by LIVE coverage only.
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

		// QUICKEST RESPONSE -- if our OWN tile is (about to be) hit, vacate it NOW, before any path analysis:
		// biased toward the goal when that step is safe, otherwise any safe neighbour. This fires even with NO
		// destination -- standing idle at a deposit / mushroom / waiting between actions -- which previously got
		// no reaction at all (planNextTile returned early on a null goal). Never spend a tick on a doomed tile.
		if (dodgeAvoid.contains(me))
		{
			WorldPoint esc = safeNeighbour(me, goal, tick, true);
			if (esc != null && !esc.equals(me))
			{
				// One calm step to a safe tile (prefers the safe column), then HOLD there until the hazard clears
				// rather than re-dodging every tick -- fewer clicks keeps our timing steady.
				holdForHazard = true; holdGoal = (goal != null ? goal : holdGoal); pickInterrupted = true;
				if (overlay != null) overlay.setStep("dodge: escape");
				return esc;
			}
		}

		// CLEAN DODGE -- local and re-evaluated every tick. We only ever move ONE tile for safety; there is no
		// long-range rerouting, so we can never crab across to a far column or build triangles. The hazard
		// oracle (dodger.isTileDangerousAt / dodgeAvoid) already knows WHEN every tile is dangerous.
		WorldPoint transitGoal = goal != null ? goal : (holdForHazard ? holdGoal : null);
		if (transitGoal == null) { holdForHazard = false; holdGoal = null; return null; }
		boolean walking = client.getLocalDestinationLocation() != null;
		boolean landHit = pathWillBeHit(me, transitGoal, tick, "dodge", DODGE_REACT_HORIZON);  // react only to an IMMINENT path hazard
		// PATH AHEAD CLEAR -> keep moving toward the goal. Crucially we do NOT retreat just because our CURRENT
		// tile will be dangerous soon: if the run lands only on safe tiles it carries us OFF the current tile
		// before a hazard forming on it lands -- e.g. run straight THROUGH a just-spawned vent (harmless for its
		// first few ticks) in one tick instead of hesitating and clicking back. The landing-only, arrival-time
		// check guarantees every tile we actually stop on is safe, and run-through (skipped) tiles never touch us.
		if (!landHit) { holdForHazard = false; holdGoal = null; return null; }
		// Path blocked within the react horizon and our tile is SAFE (escape-first above didn't fire). Stay CALM:
		// HOLD on this safe tile and wait for the hazard to pass instead of crabbing around it click-by-click. We
		// re-plan each tick -- the moment the path clears we resume toward the goal (which lies on the 3030/3048
		// column), and if THIS tile later turns dangerous escape-first moves us. One sidestep, wait, one click back.
		holdForHazard = true; holdGoal = transitGoal; pickInterrupted = true;
		if (walking) router.walkTo(me);
		if (overlay != null) overlay.setStep("dodge: hold");
		return me;
	}

	/**
	 * Nearest walkable, SAFE 8-neighbour of {@code me}, biased toward {@code goal}. "Safe" = not in the
	 * imminent danger set now and not hit on the tick we'd arrive or the tick after (dwell). With
	 * {@code allowBackward} false, neighbours farther from the goal than we already are are rejected
	 * (forward/lateral only). Returns null when nothing qualifies (caller then holds).
	 */
	private WorldPoint safeNeighbour(WorldPoint me, WorldPoint goal, int tick, boolean allowBackward)
	{
		net.runelite.api.WorldView wv = client.getTopLevelWorldView();
		if (wv == null || me == null) return null;
		net.runelite.api.coords.WorldArea area = new net.runelite.api.coords.WorldArea(me, 1, 1);
		int meDist = goal == null ? 0 : chebDist(me, goal);
		WorldPoint best = null; int bestDist = Integer.MAX_VALUE; boolean bestSpot = false;
		for (int[] d : DODGE_DIRS8)
		{
			if (!area.canTravelInDirection(wv, d[0], d[1], ALWAYS_PASSABLE)) continue;   // wall / corner rule
			WorldPoint c = new WorldPoint(me.getX() + d[0], me.getY() + d[1], me.getPlane());
			if (!inArena(c)) continue;
			if (dodgeAvoid.contains(c)) continue;                                        // imminent danger set
			boolean spot = isExtremitySafeSpot(c);
			// Arrival/dwell check only. The dodger now carries the DETERMINISTIC cone schedule (lead-marked at
			// attack-1), so a tile whose cone fires within ~3 ticks already reads dangerous here -- we won't step
			// into a forming cone, yet CAN step onto extremity-zone tiles that stay clear, to dodge a 1447.
			if (dodger.isTileDangerousAt(c, tick + 1) || dodger.isTileDangerousAt(c, tick + 2)) continue;
			int cd = goal == null ? 0 : chebDist(c, goal);
			if (!allowBackward && cd > meDist) continue;                                 // don't move away from the goal
			// Prefer the extremity-safe edge spots: a tile that stays clear across the whole extremity cycle beats a
			// merely-closer one, so a lane dodge holds the safe column instead of crabbing inward. Ties -> nearest.
			boolean take = best == null ? true : (spot != bestSpot ? spot : cd < bestDist);
			if (take) { best = c; bestDist = cd; bestSpot = spot; }
		}
		return best;
	}

	/** Chebyshev (king-move) tile distance. */
	private static int chebDist(WorldPoint a, WorldPoint b)
	{
		return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
	}

	/**
	 * Is this live tile one of the extremity-safe spots on an edge lane column -- x=3030 (west) or x=3048
	 * (east) at y 6443/6445/6447/6449? Those odd-y tiles sit BETWEEN the extremity apexes (y6442/46/50) and
	 * stay clear through the whole cone cycle, so the lane dodge prefers to hold them.
	 */
	private boolean isExtremitySafeSpot(WorldPoint instTile)
	{
		WorldPoint t = instTile == null ? null : InstanceCoords.toTemplate(client, instTile);
		if (t == null || (t.getX() != 3030 && t.getX() != 3048)) return false;
		int y = t.getY();
		return y == 6443 || y == 6445 || y == 6447 || y == 6449;
	}

	private static final int EXTREMITY_CYCLE_TICKS = 15;   // B->C->A->B period
	/** Spawn offset from cycle t0 (the B spawn) by apex row: B(y6446)=0, C(y6442)=2, A(y6450)=4. -1 = unknown. */
	private int extremitySpawnOffset(int apexY)
	{
		switch (apexY) { case 6446: return 0; case 6442: return 2; case 6450: return 4; default: return -1; }
	}
	/** Attack offset from cycle t0 by apex row: B(y6446)=3, C(y6442)=5, A(y6450)=7. -1 = unknown. */
	private int extremityAttackOffset(int apexY)
	{
		switch (apexY) { case 6446: return 3; case 6442: return 5; case 6450: return 7; default: return -1; }
	}

	/**
	 * Sync the extremity cone cycle to a WITNESSED spawn (prep only): identify which (A/B/C) by its snapped apex
	 * row, and back-compute cycle t0 (the B spawn). Re-anchors on every witnessed spawn so it self-corrects.
	 */
	private void syncExtremityCycle(int tick)
	{
		if (isDukeAwake()) return;
		if (extremityWatchStart < 0) extremityWatchStart = tick;
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return;
		for (NPC n : npcs)
		{
			if (n == null || !DukeAutoIDs.isExtremity(n.getId())) continue;
			Integer seen = extremitySpawnTick.get(n);
			if (seen == null || seen <= extremityWatchStart) continue;   // alive before we watched -> t0 unknown
			WorldArea a = n.getWorldArea();
			if (a == null) continue;
			WorldPoint centreInst = new WorldPoint(a.getX() + a.getWidth() / 2, a.getY() + a.getHeight() / 2, a.getPlane());
			WorldPoint apex = nearestExtremityApex(InstanceCoords.toTemplate(client, centreInst));
			if (apex == null) continue;
			int spawnOff = extremitySpawnOffset(apex.getY());
			if (spawnOff < 0) continue;
			extremityAnchorTick = seen - spawnOff;   // cycle t0 = B spawn
		}
	}

	/**
	 * Project the DETERMINISTIC extremity cone schedule into the dodger. Once {@link #syncExtremityCycle} has
	 * locked the anchor, every future attack is known: apex row R attacks at anchor + attackOffset(R) + 15k and
	 * its inward cone is lethal over [attack-lead, attack+active-1+trail]. Marked with the attack-1 lead, so a
	 * tile whose cone fires soon reads dangerous in time -- we never step into a forming cone even before its
	 * NPC is detected, yet zone tiles between attacks stay steppable for 1447 dodges.
	 */
	private void pushPredictedExtremities(int tick)
	{
		if (extremityAnchorTick == null || isDukeAwake()) return;
		final int horizon = 16;
		for (int apexY : new int[]{6442, 6446, 6450})
		{
			int atkOff = extremityAttackOffset(apexY);
			if (atkOff < 0) continue;
			for (int apexX : new int[]{3030, 3048})
			{
				java.util.List<WorldPoint> cone = new ArrayList<>();
				for (WorldPoint t : DukeAutoIDs.extremityCone(new WorldPoint(apexX, apexY, 0)))
				{
					WorldPoint ci = InstanceCoords.fromTemplate(client, t);
					if (ci != null) cone.add(ci);
				}
				if (cone.isEmpty()) continue;
				int base = extremityAnchorTick + atkOff;
				for (int k = Math.floorDiv(tick - base, EXTREMITY_CYCLE_TICKS); ; k++)
				{
					int atk  = base + k * EXTREMITY_CYCLE_TICKS;
					int from = atk - FALLING_DODGE_LEAD_TICKS;
					int to   = atk + DukeAutoIDs.EXTREMITY_ATTACK_ACTIVE_TICKS - 1 + HAZARD_TRAIL_TICKS;
					if (from > tick + horizon) break;
					if (to < tick) continue;
					for (int tt = Math.max(tick, from); tt <= to; tt++)
						dodger.addHazard(Hazard.staticTiles(cone, tt, "extremity-predicted"));
				}
			}
		}
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
	 * True if the player's current walk (toward {@code getLocalDestinationLocation}) would put us
	 * on a dangerous tile at the tick we'd occupy it. Predicts the walked path over live collision
	 * and samples each tick's standing tile (run = {@value #RUN_TILES_PER_TICK} tiles/tick).
	 */

	/**
	 * Shared path hazard check used BOTH proactively (before issuing a move/click, so we never
	 * start walking into a hazard) and reactively (for the current live walk). Predicts the full
	 * path {@code from -> goal} over live collision and samples the tile we'd stand on each tick
	 * (run = {@value #RUN_TILES_PER_TICK} tiles/tick) against the dodger. Stores debugPath /
	 * debugHitTile for the overlay. Empty path (can't predict) returns false so we never deadlock.
	 */
	/**
	 * Rebuild the live+imminent hazard set from the dodger for this tick: {@link #dodgeAvoid} is the union
	 * of every tile dangerous at any tick in [tick, tick+DODGE_AVOID_HORIZON], and {@link #dodgeDangerByTick}
	 * keeps the per-offset sets so the flood can tell WHEN a tile is hit. All hazard kinds (1447 falls,
	 * extremity cones, toxic clouds, gas bottles) are already projected with their timing in the dodger, so
	 * this is the complete active-hazard picture -- the Duke analogue of Whisperer's tentacle hazardAvoid.
	 */
	private void updateDukeHazards(int tick)
	{
		java.util.List<java.util.Set<WorldPoint>> byTick = new java.util.ArrayList<>(DODGE_LOOKAHEAD + 1);
		java.util.Set<WorldPoint> avoid = new java.util.HashSet<>();
		for (int off = 0; off <= DODGE_LOOKAHEAD; off++)
		{
			java.util.Set<WorldPoint> s = dodger.dumpDangerSetAt(tick + off);
			byTick.add(s);
			if (off <= DODGE_AVOID_HORIZON) avoid.addAll(s);   // trigger/near-safe set is the near window only
		}
		dodgeDangerByTick = byTick;
		dodgeAvoid = avoid;
	}

	private boolean pathWillBeHit(WorldPoint from, WorldPoint goal, int tick, String why)
	{
		return pathWillBeHit(from, goal, tick, why, CHECK_HORIZON_TICKS);
	}

	/** {@code horizonK} caps how many run-ticks ahead we react: CHECK_HORIZON_TICKS for navigation (ignore
	 *  far hazards until we approach), a large value for PICKING between destinations (we want the WHOLE
	 *  approach clear -- a vent far up one path should rule that path out, not be ignored as 'too far'). */
	private boolean pathWillBeHit(WorldPoint from, WorldPoint goal, int tick, String why, int horizonK)
	{
		debugPath = java.util.Collections.emptyList();
		debugHitTile = null;
		if (from == null || goal == null || goal.equals(from)) return false;

		List<WorldPoint> path = TilePathPredictor.path(
			client.getTopLevelWorldView(), from, goal, null, this::inArena, ARENA_PATH_BUDGET);
		if (path.isEmpty()) return false;
		debugPath = path;

		boolean hit = false;
		// Only the tiles we LAND on (tick boundaries) can hit us. With run on we move RUN_TILES_PER_TICK
		// tiles per tick and SKIP the intermediate (run-through) tiles -- a hazard we run straight over (the
		// centre of a 2-tile run) never touches us. So sample the LANDING tile each tick, speed-aware (2/tick
		// running, 1/tick walking), not every tile. tileAfterTicks clamps at the goal, itself a landing.
		int speed = mirror.getRunEnergy() > 0 ? RUN_TILES_PER_TICK : 1;
		int maxK = (path.size() - 1 + speed - 1) / speed;
		for (int k = 1; k <= maxK; k++)
		{
			if (k > horizonK) break;   // beyond the near horizon -- don't react yet, re-check as we approach
			WorldPoint stand = TilePathPredictor.tileAfterTicks(path, k, speed);
			if (stand == null) break;
			// Only the IMMEDIATE next landing we're about to commit to (k == 1) gets the extra reaction-lag pad
			// (PATH_DWELL_TICKS) -- a tile that turns dangerous a tick after we stop on it still hits us. FARTHER
			// landings (k >= 2) are judged at their arrival tick only and re-checked every tick as we approach, so
			// we react ~2 ticks out like a player instead of pausing several tiles early for a tile we haven't
			// reached yet. Nothing is missed: next tick that far tile becomes our k==1 and gets the full guard.
			int dwell = PATH_DWELL_TICKS;
			boolean bad = false;
			for (int t = tick + k - 1; t <= tick + k + dwell; t++)
				if (dodger.isTileDangerousAt(stand, t)) { bad = true; break; }
			if (bad)
			{
				debugHitTile = stand;
				hit = true;
				break;
			}
		}
		return hit;
	}

	/** Live predicted walk path (instance tiles) for the path overlay. */
	public java.util.List<WorldPoint> debugPath() { return debugPath; }
	/** First tile on the predicted path that would be hit, or null. */
	public WorldPoint debugHitTile() { return debugHitTile; }

	/** BFS bounds for transit prediction: stay inside the Duke arena region. */
	/** True if {@code meInst} stands in either hazard-free rectangle (template space): x 3034-3044 / y
	 *  6435-6437, or x 3036-3041 / y 6438-6440. This is the safe staging spot where the salt deposit
	 *  choice is (re)committed. */
	private boolean inSaltZone(WorldPoint meInst)
	{
		if (meInst == null) return false;
		WorldPoint t = InstanceCoords.toTemplate(client, meInst);
		if (t == null) return false;
		int x = t.getX(), y = t.getY();
		return (x >= 3034 && x <= 3044 && y >= 6435 && y <= 6437)
			|| (x >= 3036 && x <= 3041 && y >= 6438 && y <= 6440);
	}

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
		if (!sawDeathAnim) lastDeathTick = client.getTickCount() - DUKE_LOOT_LOOKBACK;   // anim missed -> anchor at despawn with a lookback to catch the drops
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
		extremityAnchorTick = null; extremityWatchStart = tick;   // extremities restart too -> re-sync
		holdForHazard = false; holdGoal = null; decantActive = false; decantStartTick = -1;
		dukeAttackTick = -100; dukePrevAnim = -1;
		dukeAttackPhase = -1; dukeEnraged = false; gazeTick = -100; gazeActive = false; feedingStarted = false; eatDelay.reset(); thrall.reset();
		fightCamp = null;
		fightClickCampNext = false;
		lastPickItemId = Integer.MIN_VALUE;
		picksRequested = 0;
		preApproachDone = false; exitEndReached = false;
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
	}

	// -----------------------------------------------------------------
	// Consumption (copied pattern from Vardorvis).
	// -----------------------------------------------------------------

	/** Duke thrall gate: only during the live FIGHT (he's awake, not in a shield window). Pick the thrall
	 *  style via DukeAutoIDs.THRALL_CAST_WIDGET. */
	private boolean thrallConditionsOk()
	{
		return phase.current() == DukePhase.FIGHT;
	}

	/**
	 * Windows where eating is safe because we are NOT mid-swing, so a same-tick eat can't cancel an
	 * attack: the gaze (our attacks are suppressed), the camp-switch RUN / prep transit (moving, not
	 * attacking), and out of fight. Never during the FEED_DUKE wake step. This mirrors Leviathan's
	 * eatPhaseAllowed() and Vardorvis's always-open window -- combined with the HP threshold in
	 * buildConsumptionRequest it gives the shared "window && HP <= threshold" eat rule (no top-off).
	 */
	private boolean eatWindowOpen()
	{
		// In PREP, eat freely EXCEPT during the FEED_DUKE wake step (so eating never competes with the feed).
		// In FIGHT, prepStep is LEFT at FEED_DUKE (prep stopped when Duke woke), so it must NOT block eating --
		// the fight's free windows are the gaze and the camp-switch run.
		if (phase.current() != DukePhase.FIGHT)
			return prepStep != PrepStep.FEED_DUKE;
		return gazeActive || client.getLocalDestinationLocation() != null;
	}

	private ConsumptionEngine.Request buildConsumptionRequest()
	{
		ConsumptionEngine.Request.Builder b = ConsumptionEngine.Request.builder();

		// HYBRID eat rule. A FREE window is one where a swing isn't being spent: the gaze (we idle on camp, our
		// attacks are suppressed) or while RUNNING (the gas-bottle camp switch / transit). In a free window, TOP
		// OFF -- eat whenever a shark would heal (room below max), so it stacks with the prayer sip on one tick.
		// Otherwise ('normal') be OPPORTUNISTIC -- only eat once HP has fallen to the threshold. Either way it's
		// gated to eatWindowOpen (never mid-swing while camped), paced by the eat delay (no food spam), and the
		// overheal cap refuses a bite over max. Eating delays the next attack -- canAttack gates on eatDelay.
		boolean running    = client.getLocalDestinationLocation() != null;   // camp switch / transit (not attacking)
		boolean freeWindow = gazeActive || running;                          // no swing being spent -> top off
		boolean needFood = eatWindowOpen()
			&& !eatDelay.attackBlocked(mirror.getTickCounter())
			&& mirror.getHp() < mirror.getMaxHp()
			&& (freeWindow || mirror.getHp() <= DukeSupply.PROFILE.hpEatThreshold);
		b.wantEat(needFood);
		b.allowComboEat(needFood);

		// Prayer: during downtime (gaze / moving / non-fight) top prayer up for free; while camped
		// only sip below HALF of max (jittered via the shared helper).
		boolean downtime = phase.current() != DukePhase.FIGHT
			|| client.getLocalDestinationLocation() != null
			|| gazeActive;   // the gaze suppresses our attacks -> a free window to sip
		ConsumptionEngine.requestPrayerSip(b, mirror, Math.max(1, mirror.getMaxPrayer() / 2),
			DukeSupply.PROFILE.prayerSipJitterWindow, downtime);

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
		if (allowEat && r.primaryEat != null)
		{
			int eatId = r.primaryEat.itemId;
			// Pineapple-pizza tiebreak: the engine rates the FULL (2301) and HALF (2303) pizza as equal heals, so
			// when it chose a pizza and we hold BOTH, eat the HALF first -- finish the bitten pizza before the full.
			if ((eatId == DukeAutoIDs.ITEM_PINEAPPLE_PIZZA || eatId == DukeAutoIDs.ITEM_HALF_PINEAPPLE_PIZZA)
				&& mirror.countItem(DukeAutoIDs.ITEM_HALF_PINEAPPLE_PIZZA) > 0
				&& mirror.countItem(DukeAutoIDs.ITEM_PINEAPPLE_PIZZA) > 0)
				eatId = DukeAutoIDs.ITEM_HALF_PINEAPPLE_PIZZA;
			router.useInventoryItem(eatId, "Eat");
		}
		if (allowEat && r.comboEat   != null) router.useInventoryItem(r.comboEat.itemId, "Eat");
		// Eating delays our next attack (food 3t; + combo food 2t more = 5t). A potion sip adds none,
		// so the delay is computed from the food only -- shark+potion+kara is still 5t, shark+potion 3t.
		// Gate canAttack on this so we don't burn a wasted click (and desync lastAttackTick) mid-delay.
		if (allowEat) eatDelay.onConsumed(mirror.getTickCounter(), r);
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
		int[] hp = WidgetReader.fraction(client, WidgetReader.BOSS_HP_COMPONENT);
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
		}
		// Gaze resolved: "You manage to avoid Duke Sucellus' gaze." -> safe to resume attacking. This is
		// the authoritative end of the gaze window (apostrophe encoding varies, so match loosely).
		if (lower.contains("avoid") && lower.contains("gaze"))
		{
			if (gazeActive)
			{
				Player p = mirror.getLocalPlayer();
			}
			gazeActive = false;
		}
		if (!lower.contains("awaken") && !lower.contains("duke")) return;
	}

	private static boolean isDukeId(int id)
	{
		return id == DukeAutoIDs.NPC_DUKE_INACTIVE
			|| id == DukeAutoIDs.NPC_DUKE_ASLEEP
			|| id == DukeAutoIDs.NPC_DUKE_AWAKE;
	}

	private boolean canAttack(int tick)
	{
		Player player = mirror.getLocalPlayer();
		if (player == null || duke == null) return false;
		if (tick - lastAttackTick < WEAPON_COOLDOWN) return false;
		if (eatDelay.attackBlocked(tick)) return false;   // still inside an eat / combo-eat attack delay
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
			}
		}
		// Gaze tell: suppress ALL attacking until it resolves (the "avoid ... gaze" chat, or the window
		// timeout). We stay camped on the safe tile the whole time, so no movement is needed.
		if (anim == ANIM_DUKE_GAZE && dukePrevAnim != ANIM_DUKE_GAZE)
		{
			gazeTick = tick;
			gazeActive = true;
		}
		// Fallback resume: clear if the full gaze window elapsed without a chat clearing it.
		if (gazeActive && tick - gazeTick > GAZE_WINDOW_TICKS)
		{
			gazeActive = false;
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

	/**
	 * Cleanup decant pass (one combine per call, {@value #DECANT_COOLDOWN_TICKS}-tick spacing). Combines two
	 * half pineapple pizzas into a full, then consolidates each potion type's doses into the fewest slots.
	 * True while something is still combinable (caller holds the tick); false when minimal. Issues the raw
	 * use-on-use menu actions directly -- onGameTick runs on the client thread -- so it needs no autoBossCore
	 * change; it mirrors ClickRouter.useItemOnItem but can target two slots of the SAME id.
	 */
	private boolean decantTick(int tick)
	{
		if (tick - lastDecantTick < DECANT_COOLDOWN_TICKS) return true;   // one decant per tick, still active
		if (combineTwoOf(DukeAutoIDs.ITEM_HALF_PINEAPPLE_PIZZA, tick)) return true;   // two halves -> one full
		for (int[] doses : DECANT_POTION_DOSES)
			if (decantPotion(doses, tick)) return true;
		return false;
	}

	/** Combine two inventory items of the SAME id (distinct slots). True if an action was issued. */
	private boolean combineTwoOf(int itemId, int tick)
	{
		net.runelite.api.ItemContainer inv = client.getItemContainer(net.runelite.api.InventoryID.INVENTORY);
		if (inv == null) return false;
		net.runelite.api.Item[] items = inv.getItems();
		int s1 = -1, s2 = -1;
		for (int i = 0; i < items.length; i++)
		{
			if (items[i] == null || items[i].getId() != itemId) continue;
			if (s1 < 0) s1 = i; else { s2 = i; break; }
		}
		if (s1 < 0 || s2 < 0) return false;
		decantUse(s1, itemId, s2, itemId, tick);
		return true;
	}

	/**
	 * Consolidate one potion type (its 1..4-dose ids) by packing doses into FULL (4-dose) potions. While TWO
	 * OR MORE non-full (1..3) vials exist, pour the smallest-dose one into the largest-dose one. Each pour
	 * strictly cuts the non-full count (the source empties, or the target hits 4 and the source shrinks), so
	 * it ends at <=1 partial vial = as many full potions as the doses allow. Also tops up equal partials, e.g.
	 * (3)+(3) -> (4)+(2). One combine per call; full vials are never touched.
	 */
	private boolean decantPotion(int[] doseIds, int tick)
	{
		net.runelite.api.ItemContainer inv = client.getItemContainer(net.runelite.api.InventoryID.INVENTORY);
		if (inv == null) return false;
		net.runelite.api.Item[] items = inv.getItems();
		int srcSlot = -1, srcId = -1, srcDose = Integer.MAX_VALUE;   // smallest-dose non-full vial (the donor)
		int tgtSlot = -1, tgtId = -1, tgtDose = -1;                  // largest-dose non-full vial (filled toward 4)
		int partials = 0;
		for (int i = 0; i < items.length; i++)
		{
			if (items[i] == null) continue;
			int d = doseOf(items[i].getId(), doseIds);
			if (d <= 0 || d >= 4) continue;   // not this type, or already full (leave it)
			partials++;
			if (d < srcDose) { srcDose = d; srcSlot = i; srcId = items[i].getId(); }
			if (d > tgtDose) { tgtDose = d; tgtSlot = i; tgtId = items[i].getId(); }
		}
		if (partials < 2) return false;   // <=1 partial left -> maximally packed
		if (tgtSlot == srcSlot)           // all partials equal -> min and max are the same vial; pick another target
			for (int i = 0; i < items.length; i++)
			{
				if (i == srcSlot || items[i] == null) continue;
				int d = doseOf(items[i].getId(), doseIds);
				if (d > 0 && d < 4) { tgtSlot = i; tgtId = items[i].getId(); break; }
			}
		decantUse(srcSlot, srcId, tgtSlot, tgtId, tick);
		return true;
	}

	/** Dose level (1..4) of a potion item within its type's id list, or 0 if not of this type. */
	private int doseOf(int itemId, int[] doseIds)
	{
		for (int k = 0; k < doseIds.length; k++)
			if (doseIds[k] == itemId) return k + 1;
		return 0;
	}

	/** Raw "Use src on tgt" (WIDGET_TARGET then WIDGET_TARGET_ON_WIDGET), by inventory slot. */
	private void decantUse(int srcSlot, int srcId, int tgtSlot, int tgtId, int tick)
	{
		net.runelite.api.ItemComposition sc = client.getItemDefinition(srcId);
		net.runelite.api.ItemComposition tc = client.getItemDefinition(tgtId);
		String src = ClickRouter.ITEM_COLOR_PREFIX + (sc == null ? "" : sc.getName()) + ClickRouter.ITEM_COLOR_SUFFIX;
		String tgt = ClickRouter.ITEM_COLOR_PREFIX + (tc == null ? "" : tc.getName()) + ClickRouter.ITEM_COLOR_SUFFIX;
		client.menuAction(srcSlot, ClickRouter.INVENTORY_WIDGET, MenuAction.WIDGET_TARGET,
			0, srcId, "Use", src);
		client.menuAction(tgtSlot, ClickRouter.INVENTORY_WIDGET, MenuAction.WIDGET_TARGET_ON_WIDGET,
			0, tgtId, "Use", src + "<col=ffffff> -> " + tgt);
		lastDecantTick = tick;
	}

	/**
	 * Between-fights top-off (one action per tick, paced by the eat delay). Eats toward MAX hp (the engine's
	 * overheal cap stops it wasting a bite), tops prayer to the no-waste ceiling, and sips stamina if run is
	 * low. Combat boost is deliberately NOT sipped here -- it would decay through prep before the fight. True
	 * while it still has something to restore (caller holds the tick); false once supplies can't top us more.
	 */
	private boolean cleanupTopOffTick(int tick)
	{
		boolean full = mirror.getHp() >= mirror.getMaxHp();
		boolean canEatNow = !full && !eatDelay.attackBlocked(tick);   // pace food with the eat delay (no spam)
		ConsumptionEngine.Request.Builder b = ConsumptionEngine.Request.builder();
		b.wantEat(canEatNow);
		b.allowComboEat(canEatNow);
		ConsumptionEngine.requestPrayerSip(b, mirror, Math.max(1, mirror.getMaxPrayer() / 2),
			DukeSupply.PROFILE.prayerSipJitterWindow, true);
		if (mirror.getRunEnergy() <= DukeSupply.PROFILE.runEnergySipStam
			&& DukeSupply.PROFILE.hasPotion(SupplyProfile.DrinkKind.STAMINA))
			b.opportunisticSip(SupplyProfile.DrinkKind.STAMINA);
		ConsumptionEngine.Result r = engine.evaluate(b.build());
		if (r.hasAction()) { applyConsumption(r, true); return true; }   // ate/drank -> keep topping off
		// Nothing to consume this tick: hold only while a just-eaten food is still landing and hp has room;
		// otherwise we're as full as the supplies allow -> cleanup done.
		return !full && eatDelay.attackBlocked(tick);
	}
}
