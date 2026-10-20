package net.runelite.client.plugins.dt2WhispererAuto;

import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NpcOverrides;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.autoBossCore.combat.ClickRouter;
import net.runelite.client.plugins.autoBossCore.combat.ThrallManager;
import net.runelite.client.plugins.autoBossCore.combat.Hazard;
import net.runelite.client.plugins.autoBossCore.combat.PrayerSwapper;
import net.runelite.client.plugins.autoBossCore.combat.SchedulingTileDodger;
import net.runelite.client.plugins.autoBossCore.consume.ConsumptionEngine;
import net.runelite.client.plugins.autoBossCore.consume.SupplyProfile;
import net.runelite.client.plugins.autoBossCore.phase.PhaseTracker;
import net.runelite.client.plugins.autoBossCore.state.InstanceCoords;
import net.runelite.client.plugins.autoBossCore.state.WidgetReader;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;
import net.runelite.client.plugins.autoBossCore.trip.TripState;
import net.runelite.client.plugins.autoBossCore.ui.BaseOverlay;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Whisperer strategy. BOILERPLATE SCAFFOLD.
 * <p>
 * What is wired now:
 * <ul>
 *   <li>Phase tracker: FIGHT when the boss is awake (12204), IDLE when dormant (12203),
 *       else OUT_OF_FIGHT.</li>
 *   <li>Leviathan-style projectile-driven prayer swapping (MAGIC 2445 / RANGE 2444, no melee)
 *       -- {@link #watchPrayerProjectiles(int)}.</li>
 *   <li>Ground-hazard dodging from the hazard graphics objects -- {@link #pushGraphicsHazards(int)}
 *       (conservative placeholder timing).</li>
 *   <li>Sanity % read from the sanity widget and shown on the overlay.</li>
 *   <li>Minimal supply consumption (sharks + prayer potion).</li>
 *   <li>Weapon-profile attack maintenance (config-driven speed / range).</li>
 * </ul>
 * TODO (not yet implemented):
 * <ul>
 *   <li>Arena region id + special-attack animations (placeholders in {@link WhispererAutoIDs}).</li>
 *   <li>Real hazard timing for the graphics objects (esp. 2450) and tentacle (12208) handling.</li>
 *   <li>Sanity-driven behavior; special phases (enrage screech / shadow realm); death -> prayers-off reset.</li>
 *   <li>Movement planning -- currently defers entirely to the dodger.</li>
 * </ul>
 */
@Singleton
public class WhispererAutoMain
{
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WhispererAutoMain.class);

	public enum WhisPhase { OUT_OF_FIGHT, IDLE, FIGHT }

	/**
	 * Fight structure: each cycle opens with a TENTACLE phase (phase 1 -- she spawns tentacles
	 * around us that we dodge, see {@link #planTentacleDodge(int)}), which then BRANCHES into one
	 * of the three special phases below. The three specials run in a FIXED FORWARD cycle; only the
	 * ENTRY point (the first special after phase 1) is unknown. Once the first is identified the rest
	 * are deterministic:
	 *   SEEDS -> SCREECH -> SIPHON -> SEEDS -> ...   (see {@link WhisSpecialPhase#next()})
	 * i.e. phase1 -> SEEDS   => next is SCREECH;
	 *      phase1 -> SCREECH => next is SIPHON;
	 *      SIPHON            => next is SEEDS.
	 * Each phase is self-contained: it starts on its own trigger and ends on its own condition -- a
	 * phase NEVER inspects or ends another. The cycle/next() is only for anticipating what comes next.
	 * Detection hooks (TODO wire):
	 *   CORRUPTED_SEEDS = ANIM_SEED_PHASE_START (10248) / seed GameObjects present (isSeedPhase())
	 *   SOUL_SIPHON     = ANIM_SOUL_SIPHON_START (10256) / _ACTIVE (10257)
	 *   SCREECH         = screech anims (charge 10251 / attack 10252 / cooldown 10250), via inScreechPhase()
	 */
	public enum WhisSpecialPhase
	{
		CORRUPTED_SEEDS, SCREECH, SOUL_SIPHON;

		/** The next phase in the fixed cycle. */
		public WhisSpecialPhase next()
		{
			switch (this)
			{
				case CORRUPTED_SEEDS: return SCREECH;
				case SCREECH:         return SOUL_SIPHON;
				default:              return CORRUPTED_SEEDS;
			}
		}
	}

	// --- Services ---------------------------------------------------
	private final Client client;
	private final MirrorState mirror;
	private final ClickRouter router;
	private final SchedulingTileDodger dodger;
	private final PrayerSwapper prayers;

	// --- Boss state -------------------------------------------------
	private final ConsumptionEngine engine;
	private final ThrallManager thrall;
	// TODO: add a magic combat-boost tracker (imbued/saturated heart or magic potion) once
	// an offensive boost is carried -- see Leviathan/Vardorvis for the CombatBoostTracker wiring.
	private final TripState trip = new TripState();
	private final PhaseTracker<WhisPhase> phase;

	private volatile boolean active = false;

	private NPC whisperer;
	private int lastAttackTick = -100;

	/** Instance tile resolution bookkeeping. */
	private int lastResolveTick = -100;

	/**
	 * Dedupe of (impactTick, projId) pairs we've already scheduled an overhead swap for,
	 * so a projectile seen across several ticks only queues one swap. Mirrors Leviathan.
	 */
	private final java.util.Set<Integer> scheduledPrayIdentities = new java.util.HashSet<>();

	/**
	 * Latched on death so overheads stay off until the next kill begins. TODO: wire the
	 * death trigger (death animation / boss despawn). Currently never set.
	 */
	private boolean deathPrayersOff = false;

	// --- Seed phase (crossing) --------------------------------------
	// Populated each tick from the scene scan: the tiles of the good (47573) and bad (47574)
	// seed GameObjects, in INSTANCE coords (to match mirror.getPlayerTile()).
	private final java.util.List<WorldPoint> goodSeedTiles = new java.util.ArrayList<>();
	private final java.util.List<WorldPoint> badSeedTiles  = new java.util.ArrayList<>();
	/** Count of overworld seed graphics (2459) visible this tick -- all seeds look alike there, so we
	 *  only use this to keep the phase latched while markers are up; the good tiles come from the
	 *  translated shadow scan ({@link #seedRouteOw}). */
	private int overworldSeedGfxCount = 0;
	/** Tiles currently showing a seed marker (2459) in the overworld, rebuilt each tick. Used as a
	 *  liveness check: we only step onto a planned good tile if a seed is still there. */
	private final java.util.Set<WorldPoint> overworldSeedTiles = new java.util.HashSet<>();
	/** Tick we last (re)targeted the current seed-path tile, for the stuck-skip safety. */
	private int seedTargetSetTick = -100;
	/** The good seed we last stepped from, so we don't reverse direction around the ring. */
	private WorldPoint lastSeedTile;
	/** Last tick we clicked the blackstone fragment, to avoid spamming the realm swap. */
	private int lastBlackstoneTick = -100;
	/** Last tick the Whisperer played a screech animation (charge/attack/cooldown). */
	private int lastScreechAnimTick = -100;
	/** Last tick the Whisperer played a soul-siphon animation (start 10256 / active 10257). */
	private int lastSiphonAnimTick = -100;
	/** Tick she played the soul-siphon END animation (10259), in the OVERWORLD after we cross back. The siphon
	 *  post-phase bind window is anchored HERE, not to the 4th-shot swingTick (which is in the shadow realm before
	 *  the cross-back, so counting from it maneuvers too soon). Reset to -100 at each siphon entry (10257). */
	private int lastSiphonEndAnimTick = -100;
	/** One-shot latch: we fire the siphon action (equip Venator bow + activate blackstone) ONCE per
	 *  siphon, on the 10257 animation, and reset when the siphon phase ends. */
	private boolean siphonActionDone = false;
	/** Last logged signature of the 12212 (shadow lost soul) positions, so the [soul12212] diagnostic
	 *  only prints when the set of souls/positions changes. */
	private String lastSoulSig = "";
	/** Latched pillar hide chain for the screech phase, in HIDE ORDER [1=20%, 2=40%, 3=100%]. Captured
	 *  ONCE at screech start and held (never recomputed), so the overlay stays stable as pillar HP/rank
	 *  shifts during the attack (a 40% becomes 20%, etc.); cleared when screech ends. Dead pillars are
	 *  just skipped by the overlay, so 2 and 3 persist after 1 is gone. */
	private final java.util.List<NPC> hidePillarChain = new java.util.ArrayList<>();
	/** The hide chain's pillar tiles (SW of each 2x2) in SHADOW-realm instance coords, latched at the
	 *  same time as {@link #hidePillarChain} and in the same hide order [1,2,3]. Held as tiles (not just
	 *  live NPC refs) so the overlay can translate them shadow->overworld and draw the plan in EITHER
	 *  realm -- the same cross-realm trick as the seeds -- even when the shadow NPCs aren't in-scene. */
	private final java.util.List<WorldPoint> hidePillarChainShadowTiles = new java.util.ArrayList<>();
	/** The hide chain's SAFESPOT tiles translated to OVERWORLD instance coords, computed and refreshed
	 *  while we're in the shadow realm (where the instance->template translation is reliable) and then
	 *  served frozen in the overworld. Translating a shadow tile from the overworld drifts, so -- exactly
	 *  like the seeds -- we translate once from the shadow side and render the stored result. */
	private final java.util.List<WorldPoint> hidePillarChainOwTiles = new java.util.ArrayList<>();
	/** Screech hide-walk cursor: index into the hide chain of the pillar we're currently behind. */
	private int screechHideIdx = 0;
	/** The tick the CURRENT hide-spot approach began (latched when screechHideIdx changes), so we know how
	 *  many of the move window's ticks are already spent -- the budget a weave attack must fit inside to still
	 *  reach cover in time. {@link #screechMoveStartIdx} is the idx that start tick belongs to. */
	private int screechMoveStartTick = -100;
	private int screechMoveStartIdx = -1;
	/** True once we've fired the single approach weave for the current spot, so we don't re-attack every tick the
	 *  cooldown is up (which kept interrupting the walk to cover). Reset when the approached spot changes. */
	private boolean screechApproachWeaved = false;
	/** True once we've reached the current hide spot's cover tile (then we slide to the tile nearest the
	 *  NEXT spot to prep the next step). */
	private boolean screechHideArrived = false;
	/** True once we've positively seen the current hide spot's OVERWORLD pillar alive. Only then does its
	 *  disappearance count as "consumed" -- guards against load-lag false positives right after we cross
	 *  back (which would otherwise skip us straight past spot 1). Reset when we advance to the next spot. */
	private boolean screechPillarSeen = false;
	/** The SPECIFIC overworld pillar NPC (12210) we're currently hiding behind, latched ONCE per spot. We
	 *  watch THIS one die -- re-scanning for the nearest each tick would keep finding a still-alive
	 *  neighbour pillar (there are several per rank). Cleared when we advance to the next spot. */
	private NPC screechHidePillarNpc = null;
	/** The latched pillar's health ratio when we first hid behind it (baseline). We act the moment it DROPS from
	 *  this (the pillar got hit). -1 = no health bar yet (undamaged); re-latched each time we change hide spots. */
	private int screechHidePillarHp = -2;
	/** The last tile we issued a hide-walk click to -- so we don't re-click the SAME destination every
	 *  tick (which looks inhuman). We only click when the intended tile changes. */
	private WorldPoint lastHideClick = null;
	/** Max weapon attack range (tiles). If the Whisperer is farther than this, an "attack" click would
	 *  PATH us toward her to get in range -- pulling us off cover -- so we only weave when within range.
	 *  Tuned for a Shadow (>= 8); move to config if other weapons are used. */
	private static final int MAX_ATTACK_RANGE = 8;
	/** Venator bow item id -- the CHARGED/wieldable form (27610, from the captured Wield action), which is
	 *  what sits in the inventory and what the CC_OP references. We swap to it for the siphon phase. */
	private static final int ITEM_VENATOR_BOW = 27610;
	/** The ACTUAL item id that was in our weapon slot when the siphon started (captured before we swap to the
	 *  Venator bow). We re-wield THIS on the swap-back, so a charged/variant weapon id the config can't know
	 *  (e.g. a charged Tumeken's shadow vs the uncharged id in {@link WhispererWeapon}) still re-equips. */
	private int savedMainWeaponId = -1;
	/** Venator bow attack range (tiles). The corners sit 6-8 tiles from her centre, so a single tile can't
	 *  reach all four at this range -- we approach each group to within it, fire, then step to the next. */
	private static final int VENATOR_RANGE_TILES = 6;
	/** Venator bow attack speed (ticks between shots). The siphon clear is 4 groups x this cadence; the
	 *  main-weapon {@link #effectiveCooldown()} does NOT apply here because we're wielding the bow. */
	private static final int VENATOR_COOLDOWN_TICKS = 4;
	/** The ONLY thing that decides a between-pillar weave: how far the NEXT cover is. A pillar falls, and if the
	 *  next cover is reachable in this many move-ticks or fewer it's a SHORT trip -- we have a spare tick to
	 *  squeeze one attack before running on. A longer run is a LONG trip -- we run straight so we never arrive
	 *  late (the original 2->3 hit). Measured in move-ticks ({@link #tilesPerTick()}), so with run on a 1-tick
	 *  trip is up to 2 tiles. We gate on the trip itself (not a combined drag-in + return budget, which a drag
	 *  from the attack could blow): short trips attack, long trips don't. Tunable. */
	private static final int SCREECH_SHORT_TRIP_TICKS = 1;
	/** Ticks of slack on the FIRST approach to pillar 1 (straight off the scout cross-back). This window is
	 *  far larger than the inter-pillar move window -- the screech hasn't started locking us yet -- so we can
	 *  usually slip an attack in before settling behind pillar 1. Tunable. */
	private static final int SCREECH_APPROACH_WINDOW_TICKS = 6;
	/** Safe-attack hide tiles for the screech pillar phase, in TEMPLATE (static map) coords -- the two stand tiles
	 *  with a clean LoS that won't path us out. The fight is instanced, so each is converted to the live instance
	 *  tile (InstanceCoords.fromTemplate) before comparing with getPlayerTile. From every other spot we do NOT
	 *  settle-weave; those only attack via the short-trip travel weave between pillars. Weapon-independent. */
	private static final WorldPoint[] SCREECH_SAFE_ATTACK_TEMPLATE = {
		new WorldPoint(2649, 6363, 0),
		new WorldPoint(2663, 6363, 0),
	};
	/** Siphon clear bookkeeping: the quadrants (NE/NW/SE/SW around her centre) we've already fired a
	 *  Venator shot into, so a group whose souls are mid-death isn't targeted twice. Cleared at phase end. */
	private final java.util.Set<String> siphonClearedQuadrants = new java.util.HashSet<>();
	/** Centre used to split souls into the 4 corner quadrants, taken from the SOULS' own centroid (same
	 *  coordinate space as the souls) and latched before the first shot. The Whisperer's tile can't be used
	 *  here -- during the shadow clear it reads in a different space, scrambling the quadrants. Reset per siphon. */
	private WorldPoint siphonCenter = null;
	/** Last tile we issued a siphon walk-click to (camp under the boss), so we don't re-click it every tick. */
	private WorldPoint lastSiphonClick = null;
	/** Whether we were in the siphon phase last tick, to detect the active->inactive EDGE (true end, not a
	 *  cold start) for the weapon swap-back and the post-phase bind flee. */
	private boolean siphonWasActive = false;
	/** One-shot: armed when the siphon clear finishes (all 4 groups fired) so we swap the weapon back and
	 *  open the bind gap exactly once per siphon, not every tick of the lingering ~20-tick timer. */
	private boolean siphonFleeArmed = false;
	/** One-shot: armed when we START the post-siphon flee swing (the tick we land back in the overworld), so we
	 *  don't re-arm it every tick of the lingering siphon timer. Reset at siphon entry (10257). */
	private boolean siphonSwingArmed = false;
	/** Count of Venator shots we've loosed this siphon, by watching projectile 2291 (one per shot). The 4th
	 *  is the final group -- its firing ends the clear (blackstone back to overworld + weapon swap + flee). */
	/** Distinct loose-ticks (startTick) of our 2291 Venator shots this siphon. The bow PIERCES several souls in a
	 *  line, so ONE shot spawns multiple 2291 projectile instances -- counting instances overcounted and ended the
	 *  siphon after the first group. All pierce instances of one shot share a startTick, and real shots are
	 *  >= VENATOR_COOLDOWN_TICKS apart, so we dedupe by startTick and cluster any within a cooldown window into one
	 *  shot ({@link #venatorShotCount}). Sorted; cleared per siphon. */
	private final java.util.TreeSet<Integer> venatorShotTicks = new java.util.TreeSet<>();
	/** Pending main-weapon swap-back after the siphon. We DON'T wield on the same tick we issue a flee walk
	 *  (the walk click would override the wield), so we set this at siphon end and perform the wield on the
	 *  first tick we're NOT walking (the flee's "safe gap" hold), with the timer edge as a final fallback. */
	private boolean mainWeaponSwapPending = false;

	// --- Phase-end bind (she binds us and walks in; keep a gap so she can't touch us) ------------------
	/** Minimum tile gap to keep from the Whisperer's footprint at phase end. On each special-phase end she
	 *  casts a bind (roots us) and walks to us; if she reaches us we take a hit, so we open this gap first. */
	private static final int BIND_SAFE_GAP = 10;
	/** Centre-to-edge of the Whisperer's 3x3 footprint (she's always 3x3). Used to place flee tiles from her CENTRE
	 *  instead of getWorldArea's edge, which under-reports her size during screech / the bind form. */
	private static final int WHISPERER_HALF_FOOTPRINT = 1;
	/** While tick <= this, the post-phase bind flee is active (hold/extend the 10-gap). Set at each phase end:
	 *  seeds = last seed stepped (10252), screech = 3rd/last pillar consumed, siphon = siphon phase ends. */
	private int bindFleeUntil = -100;
	/** She does NOT always bind after a special. So the phase-end flee is CONDITIONAL: we start retreating
	 *  immediately (safe), but if no bind projectile (2467) appears within {@link #BIND_WATCH_TICKS} of the
	 *  phase end, she isn't binding this time -- cancel the flee and resume DPS instead of running out for
	 *  nothing. Last tick we saw projectile 2467 in flight; -100 = never. */
	private int lastBindProjTick = -100;
	/** The first OVERWORLD tick the flee actually becomes enforceable (boss visible) for the current window.
	 *  The bind watch is measured from HERE, not from the arm tick -- the siphon flee is armed in the shadow
	 *  realm several ticks before she can bind, so arming-relative timing would cancel before she even could.
	 *  -100 = not latched this window. */
	private int bindFleeStartTick = -100;
	/** Ticks after the flee becomes enforceable to wait for the bind projectile (2467) before concluding she
	 *  won't bind and cancelling the flee. Short -- she throws it right away if she's going to. Tunable. */
	private static final int BIND_WATCH_TICKS = 3;
	/** Tolerance (ticks) for a bind cast seen slightly BEFORE our phase-end detection latched, so a bind that
	 *  fires a tick or two early still counts as "she bound". */
	private static final int BIND_LOOKBACK_TICKS = 2;
	/** Last tile we issued a bind-flee walk-click to, so we don't re-click the same destination every tick. */
	private WorldPoint lastBindFleeClick = null;
	/** The desired safe tile for the current bind window, latched ONCE so we click it a single time and
	 *  travel there (rather than recomputing a drifting target every tick). Cleared when the window ends. */
	private WorldPoint bindFleeTarget = null;

	// --- Opening oscillation (phase 1, waking -> first special) ----------------------------------------
	/** The tile we land on when the Whisperer wakes and knocks us back (captured live, so it's correct in
	 *  whatever coordinate space the client reports -- the user's reference is ~(2656,6364,0) template), and
	 *  {@link #openingDanceB} one tile south of it. We oscillate between the two while attacking, to dodge the
	 *  opening tentacles, until the first special phase. Cleared when the fight ends. */
	private WorldPoint openingDanceA = null;
	private WorldPoint openingDanceB = null;
	/** Re-enable the opening oscillation (dance between {@link #openingDanceA}/{@link #openingDanceB}) for the
	 *  TENTACLE phase AFTER a siphon that did NOT bind -- "resume tentacle phase on the same tiles as phase 1".
	 *  Set when such a siphon resolves; cleared when the next special begins or the fight ends. */
	private boolean resumeOscillation = false;
	/** Previous-tick awake state, to catch the dormant->awake transition (the knockback moment). */
	private boolean wasWhispererAwake = false;
	/** Latched once the Odd Figure (12203) starts its wake animation (10227). While set and she is not yet
	 *  awake (12204), we HOLD -- the knockback to the start tile and the 12204 spawn happen on their own, so
	 *  re-poking/re-walking would just oscillate. Reset when the fight ends. */
	private boolean oddFigureWaking = false;
	/** Latched the moment we issue the 'Disturb': the menu action auto-walks us up off the start tile toward
	 *  her, which is EXPECTED -- so we hold for the wake anim instead of walking back to the start tile (the
	 *  oscillation). Reset when the fight ends. */
	private boolean oddFigureDisturbed = false;
	private int lastDisturbTick = -100;
	/** Ticks to wait after a Disturb for the wake anim before assuming the click didn't land and re-poking. */
	private static final int DISTURB_WAIT_TICKS = 8;
	/** ENRAGE phase, triggered authoritatively by the chat line "The Whisperer pulls you into the Shadow
	 *  Realm..." (see {@link #onChatMessage}). Latched true on that message; reset when the fight ends (boss
	 *  gone). The enrage behavior itself keys off this flag. */
	private boolean enragePhaseActive = false;
	/** Latched true once the ENRAGE chat fires this fight; cleared on the next wake. Qualifies the
	 *  'fight over -> prayers off' backstop so it can't trip on a transient whisperer==null before a fight. */
	private boolean enrageSeen = false;
	/** The tile we were standing on when the ENRAGE phase began -- the anchor for the 4-tile dodge formation
	 *  (there are no static tiles; the whole formation is relative to this). Null until enrage starts; cleared at
	 *  fight end. The 4 safe tiles are this + {@link #ENRAGE_OFFSETS}. */
	private WorldPoint enrageAnchor = null;
	/** Offsets (E+/N+ tiles) of the 4 enrage safe tiles from {@link #enrageAnchor}: the entry tile itself, then
	 *  the three others from the user's diagram (tile2 +1E/2S, tile3 1W/3S, tile4 2W/1S). */
	private static final int[][] ENRAGE_OFFSETS = { {0, 0}, {1, -2}, {-1, -3}, {-2, -1} };
	/** Enrage tentacles come ~every 2 ticks, continuously (they overlap, so the once-per-spawn re-arm never fires).
	 *  So we DODGE on this fixed cadence while any tentacle is up -- dodge always wins the tick, DPS only fills the
	 *  gaps -- which keeps slow weapons (Shadow) from standing an extra tick to attack and eating a hit. */
	private static final int ENRAGE_STEP_PERIOD = 2;
	/** Last tick the enrage dodge stepped, to drive {@link #ENRAGE_STEP_PERIOD}. */
	private int lastEnrageStepTick = -100;
	/** True once the first special phase of this fight has begun -- ends the opening oscillation. Reset when
	 *  the fight ends (boss gone). */
	private boolean firstSpecialStarted = false;
	/** Last tile we clicked while walking to the fight start tile, to avoid re-clicking every tick. */
	private WorldPoint lastStartClick = null;
	/** True if the FIRST special of this fight was corrupted seeds -- that rotation needs us to swing around
	 *  to the NORTH of her after the seed bind (she's pulled south, and north is the open side). Reset at
	 *  fight end. Other first-specials use their own rotations. */
	private boolean firstSpecialWasSeeds = false;
	/** Armed when the (seed-first) seed phase ends: after the bind flee we reposition to the north of her.
	 *  Cleared on arrival or when the next special begins. This is the MANEUVER stage of the seed-first swing. */
	private boolean repositionNorthActive = false;
	/** Swing-back cardinal (toward centre = opposite the flee), captured at resolveSwing. Exactly one nonzero;
	 *  default north. The reposition drives along this instead of a hardcoded north. */
	private int reposDx = 0, reposDy = 1;
	/** Last tile we clicked while repositioning north, for dedupe. */
	private WorldPoint lastReposClick = null;
	/**
	 * Seed-first post-phase SEQUENCE, the stages BEFORE the north maneuver ({@link #repositionNorthActive}):
	 * <ol><li>FLEE  -- run to the flee tile (centre lane, {@link #BIND_SAFE_GAP} south), opening the gap;</li>
	 *     <li>BOUND -- she has shot the bind (2467) and is walking in; we're rooted, so we HOLD and do nothing
	 *                  (clicking here queues moves that snap us back once the root releases). When she STOPS
	 *                  walking the bind is over and we hand off to the maneuver.</li></ol>
	 */
	/** Which special we are post-processing (flee + bind + maneuver), or NONE. */
	private enum Special { NONE, SEEDS, SCREECH, SIPHON }
	/** Sub-stage of the post-special swing. (MANEUVER is {@link #repositionNorthActive}.) */
	private enum SwingStage { FLEE, BOUND }
	private Special swingSpecial = Special.NONE;
	private SwingStage swingStage = SwingStage.FLEE;
	private int swingTick = -100;                // the tick the special ended (startSwing)
	private int swingArrivedTick = -100;         // first tick at the flee tile; the 12205 bind-watch counts from here
	private WorldPoint lastSwingClick = null;
	/** Flee tile LATCHED once at swing start, so re-clicks don't recompute off her LIVE centre -- during a
	 *  bind she's pulled (and 12205 walks) south, and a live recompute marched the target 13 further south each
	 *  tick, eventually clicking off the map. Fixed for the whole swing; nulled on start/resolve/reset. */
	private WorldPoint swingFleeAnchor = null;
	/** Her 3x3 CENTRE captured at wake -- the fixed arena centreline reference for flee tiles. She doesn't move
	 *  (only the 12205 bind form drifts), so flees stay on the centreline through this X/Y. Recaptured each wake. */
	private WorldPoint whispererStartCentre = null;
	/** Latched by the "The Whisperer binds you in place" chat line: confirms we are ROOTED by the bind. While a
	 *  swing is BOUND and this is set, we DPS the bind form (12205) whenever she's in range (we can't move, but we
	 *  can still attack). Cleared when a swing starts / resolves and at fight end. */
	private boolean boundInPlaceChatSeen = false;
	/** Which special triggered the current north MANEUVER (so its own lingering phase flag -- e.g. siphon's
	 *  20-tick timer -- doesn't self-abort the maneuver). */
	private Special maneuverSpecial = Special.NONE;
	/** Screech post-phase maneuver: after fleeing NORTH (she's pulled south) we walk back DOWN to the centre
	 *  fight camp to resume DPS -- the "resume" in the flowchart means resume from the centre, not from the
	 *  north flee tile. Armed when a screech swing resolves; cleared on arrival or when a new special begins. */
	private boolean recenterActive = false;
	private WorldPoint lastRecenterClick = null;
	/** True if a BIND form (12205) appeared while a screech recentre was active. If she binds and walks the ~10
	 *  tiles up to us, she ends up NORTH of where she was pulled, so recomputing the camp (gap 11 N of her LIVE
	 *  centre) would chase her another ~10 tiles north -- pointless, we're already safe. So when the bind resolves
	 *  we END the recentre and resume DPS in place instead of chasing. Cleared when a recentre starts / fight ends. */
	private boolean recenterBindSeen = false;
	/** Ticks after a special ENDS (swingTick) to watch for the bind form (12205) before concluding she did NOT
	 *  bind. She flips to 12205 within this window if she's going to bind at all; anchored to the special end
	 *  (seeds: 10252 anim; screech: last pillar destroyed; siphon: 4th shot), NOT flee-tile arrival. Per-special
	 *  because the cast delay differs (screech's is longer). See {@link #bindAppearTicks}. */
	private static final int BIND_APPEAR_TICKS_SEEDS   = 5;
	private static final int BIND_APPEAR_TICKS_SCREECH = 9;
	private static final int BIND_APPEAR_TICKS_SIPHON  = 8;
	/** How far north of her centre to settle, and how far east of her footprint the go-around corridor sits. */
	private static final int REPOS_NORTH_OFFSET = 5;
	private static final int REPOS_CORRIDOR_DX = 3;
	/** Latched true for the whole corrupted-seeds phase. Seeds are shadow-only, so this spans
	 *  both realms; set on ANIM_SEED_PHASE_START / seeds visible, cleared on timeout or screech. */
	private boolean seedPhaseActive = false;
	/** Good-seed route captured in the shadow realm, translated to OVERWORLD instance tiles (unordered,
	 *  in capture order). */
	private final java.util.List<WorldPoint> seedRouteOw = new java.util.ArrayList<>();
	/** The route ordered into an actual walkable PATH -- a nearest-neighbour chain from where we
	 *  re-enter the overworld -- plus our progress index. Built once per crossing; the walk advances
	 *  along it dynamically, so capture order never matters and we don't re-target stepped tiles. */
	private final java.util.List<WorldPoint> seedPath = new java.util.ArrayList<>();
	private int seedWalkIdx = 0;
	/** Last tile we issued a seed walk-click to, so we don't re-send the same destination every tick (the
	 *  engine keeps walking to it on its own). Cleared when the route is rebuilt or the phase resets. */
	private WorldPoint lastSeedClick = null;
	/** Last tick seeds were visible (shadow), for the phase timeout. */
	private int lastSeedActivityTick = -100;
	/** Rate-limit for the seed-object discovery dump (capture mode). */
	private int lastSeedDiagTick = -100;
	/** Realm-crossing target for the scout phases (seeds / screech): a PLANE -- OVERWORLD_PLANE (0) or
	 *  SHADOW_REALM_PLANE (3), or -1 for "no crossing wanted". The phase logic sets it;
	 *  serviceRealmCrossing acts on it. */
	private int desiredRealm = -1;
	/** Tick we most recently entered the shadow realm, or -1 when not in shadow. */
	private int shadowEnterTick = -1;
	/** Hazard tiles for the dodge -- the LINE of tiles between us and each tentacle (see {@link #hazardTiles}),
	 *  recomputed at the start of each dodge plan. The dodge AVOIDS these: the tentacle direction classification
	 *  alone misses the line tiles, so without this the dodge can step onto one. */
	private java.util.Set<WorldPoint> dodgeHazards = java.util.Collections.emptySet();
	/** Tick we FIRST saw good seeds (47573) this scout -- the scout dwell counts from here, not from
	 *  entering the plane, so the ~1-tick realm-transition screen can't advance us early. -100 = unseen. */
	private int seedSeenTick = -100;
	/** Latched once the seed scout has captured the full good-seed route in the shadow realm. */
	private boolean seedScoutDone = false;
	/** After a deterministic 10252 seed-phase end, block the "keep alive" re-arm until this tick, so a
	 *  seed object still rendered for a tick or two (while the 10252 anim lingers) can't resurrect the
	 *  phase and spuriously re-run the scout cross. A genuine NEW phase still arms via 10248. */
	private int seedEndLockoutUntil = -100;

	// --- Tentacle dodge (phase 1) -----------------------------------
	/** First tick a tentacle spawn was seen in the scene; -100 = no threat pending. Used to hold
	 *  position (keep attacking) through the telegraph and step at the last safe tick. */
	private int tentacleThreatTick = -100;
	/** True once we've already stepped off the current threat, so we dodge exactly once per spawn. */
	private boolean tentacleDodged = false;
	/** Set the tick we execute a dodge step: the walk breaks our attack lock, so the NEXT tick we
	 *  re-click the Whisperer to re-lock (can't walk and attack the same tick -- last click wins). */
	private boolean tentacleRelockPending = false;

	// Overlay slot the plugin owns; main just updates it.
	private BaseOverlay overlay;

	// Config is bound by the plugin at startUp (like the overlay). May be null before bind.
	private WhispererAutoConfig config;

	@Inject
	public WhispererAutoMain(
		Client client,
		MirrorState mirror,
		ClickRouter router,
		SchedulingTileDodger dodger,
		PrayerSwapper prayers)
	{
		this.client  = client;
		this.mirror  = mirror;
		this.router  = router;
		this.dodger  = dodger;
		this.prayers = prayers;

		this.engine = new ConsumptionEngine(WhispererSupply.PROFILE, mirror);
		this.thrall = new ThrallManager(WhispererAutoIDs.THRALL_CAST_WIDGET, "Whisperer thrall");

		this.phase = new PhaseTracker<>(WhisPhase.OUT_OF_FIGHT)
			.on(WhisPhase.FIGHT, () -> whisperer != null && isWhispererAwake(),
				"whisperer awake (12204)")
			.on(WhisPhase.IDLE, () -> whisperer != null && !isWhispererAwake(),
				"whisperer dormant (12203)")
			.on(WhisPhase.OUT_OF_FIGHT, () -> whisperer == null,
				"no boss");
	}

	public void bindOverlay(BaseOverlay o) { this.overlay = o; }
	public void bindConfig(WhispererAutoConfig c) { this.config = c; }

	/** Normalized, low-churn overlay step (coarse phase/action name only). */
	private void step(String coarse)
	{
		if (overlay != null) overlay.setStep(coarse);
	}

	/** Coarse step on the overlay; the full specific detail is kept out of the HUD and logged in capture mode. */
	private void step(String coarse, String detail)
	{
		if (overlay != null) overlay.setStep(coarse);
		if (config != null && config.captureMode()) log.info("[step] {} -- {}", coarse, detail);
	}

	/** Flee direction label derived from the LATCHED flee anchor (dynamic for seeds/siphon), for the overlay. */
	private String fleeLabel()
	{
		WorldPoint a = swingFleeAnchor;
		WorldPoint c = whispererStartCentre != null ? whispererStartCentre : whispererCenterTile();
		if (a == null || c == null) return "Flee";
		int dx = a.getX() - c.getX(), dy = a.getY() - c.getY();
		if (Math.abs(dy) >= Math.abs(dx)) return dy >= 0 ? "Flee North" : "Flee South";
		return dx >= 0 ? "Flee East" : "Flee West";
	}

	/** Effective attack cooldown in ticks: config override if > 0, else the selected weapon's,
	 *  else the {@link WhispererAutoIDs} fallback (before the config is bound). */
	private int effectiveCooldown()
	{
		if (config == null) return WhispererAutoIDs.WEAPON_COOLDOWN;
		if (config.attackTickOverride() > 0) return config.attackTickOverride();
		return config.weapon().cooldownTicks;
	}

	/** Effective attack range in tiles: config override if > 0, else the selected weapon's. */
	private int effectiveRange()
	{
		if (config == null) return 10;
		if (config.attackRangeOverride() > 0) return config.attackRangeOverride();
		return config.weapon().rangeTiles;
	}

	/** Effective tentacle dodge delay in ticks: config value if bound, else the {@link WhispererAutoIDs}
	 *  fallback. Clamped at 0. */
	private int effectiveTentacleDelay()
	{
		if (config == null) return WhispererAutoIDs.TENTACLE_DODGE_DELAY_TICKS;
		return Math.max(0, config.tentacleDodgeDelay());
	}

	/** Minimum tiles to keep between us and the boss when dodging tentacles (melee avoidance): config
	 *  value if bound, else the {@link WhispererAutoIDs} fallback. Clamped at 1. */
	private int effectiveTentacleGap()
	{
		if (config == null) return WhispererAutoIDs.TENTACLE_MIN_GAP_TILES;
		return Math.max(1, config.tentacleMinGap());
	}

	public boolean   isActive()     { return active; }

	/**
	 * During the SIPHON clear we approach souls by clicking tiles that sit ON the Whisperer's 3x3 footprint,
	 * where the default left-click is "Attack" -- so the walk click would attack her with the Venator bow
	 * instead of moving (the 'approaching soul -> freeze'). We never click her during the siphon, so
	 * deprioritize her menu entries then, making those clicks resolve to "Walk here". Everywhere else she
	 * stays clickable so the fight can attack her normally.
	 */
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!active || !inSiphonPhase()) return;
		MenuEntry e = event.getMenuEntry();
		NPC npc = e.getNpc();
		if (npc == null) return;
		int id = npc.getId();
		if (id == WhispererAutoIDs.NPC_WHISPERER_AWAKE
			|| id == WhispererAutoIDs.NPC_WHISPERER_DORMANT
			|| id == WhispererAutoIDs.NPC_WHISPERER_BINDING)
		{
			e.setDeprioritized(true);
		}
	}
	public void      start()        { active = true; }
	public void      stop()
	{
		active = false;
		// Drop any pending overhead swap so a stale one can't fire after toggle-off.
		prayers.clearScheduled();
		// Reset scout / crossing state so a re-toggle starts clean.
		desiredRealm = -1;
		shadowEnterTick = -1;
		seedSeenTick = -100;
		seedScoutDone = false;
		seedPath.clear();
		seedWalkIdx = 0;
		seedTargetSetTick = -100;
	}
	public WhisPhase currentPhase() { return phase.current(); }
	public TripState trip()         { return trip; }

	/**
	 * Master gate for the non-prayer automation (movement / consumption / attack). OFF for
	 * now: the user asked to wire up only the prayer swapper first, so toggling the plugin on
	 * currently does prayer swapping ONLY and nothing that moves or clicks. Flip to true (or
	 * replace with a config item) once those mechanics are ready to drive the fight.
	 */
	private static final boolean ENABLE_FULL_AUTO = false;

	// -----------------------------------------------------------------
	// Tick loop.
	// -----------------------------------------------------------------

	public void onGameTick()
	{
		int tick = mirror.getTickCounter();

		if (config != null && config.captureMode())
		{
			boolean wvNull = client.getTopLevelWorldView() == null;
			int anim = whisperer == null ? -2 : whisperer.getAnimation();
			log.info("[tickDbg] tick={} active={} seedActive={} siphon={} screech={} whisAnim={} plane={} wv={} me={}",
				tick, active, seedPhaseActive, inSiphonPhase(), inScreechPhase(), anim,
				wvNull ? -99 : client.getTopLevelWorldView().getPlane(),
				wvNull ? "null" : "ok", mirror.getPlayerTile());
			if (inSiphonPhase()) logLostSouls(tick);   // [soul12212] diagnostic (siphon only)
		}

		whisperer = findWhisperer();
		if (whisperer != null)
		{
			// Track her bind cast (projectile 2467) each tick -- the phase-end flee is gated on this, since she
			// does not always bind after a special.
			if (mirror.hasProjectile(WhispererAutoIDs.PROJ_BIND)) lastBindProjTick = tick;

			int a = whisperer.getAnimation();
			if (a == WhispererAutoIDs.ANIM_SCREECH_CHARGE
				|| a == WhispererAutoIDs.ANIM_SCREECH_ATTACK
				|| a == WhispererAutoIDs.ANIM_SCREECH_COOLDOWN)
			{
				lastScreechAnimTick = tick;
			}
			if (a == WhispererAutoIDs.ANIM_SOUL_SIPHON_START
				|| a == WhispererAutoIDs.ANIM_SOUL_SIPHON_ACTIVE)
			{
				lastSiphonAnimTick = tick;
			}
			// SIPHON (not a scouted phase): on the ACTIVE animation (10257), ONCE, equip the Venator bow
			// if we're carrying one and activate the blackstone -- both in the SAME tick.
			if (a == WhispererAutoIDs.ANIM_SOUL_SIPHON_ACTIVE && active && !siphonActionDone)
			{
				captureMainWeaponId();   // remember what's WORN now, before the bow swap, so we re-wield the exact id
				if (mirror.countItem(ITEM_VENATOR_BOW) > 0) equipVenatorBow();
				clickBlackstone(tick);
				siphonActionDone = true;
				siphonCenter = null;            // fresh siphon -> re-latch the quadrant centre from the new ring
				lastSiphonEndAnimTick = -100;   // fresh siphon -> re-arm the end-anim anchor
				siphonSwingArmed = false;       // fresh siphon -> allow the post-phase flee to re-arm on landing
				resumeOscillation = false;      // a new siphon ends any post-siphon oscillation
				step("Siphon", "siphon: venator bow + blackstone");
			}
			// SIPHON END (10259): BACKSTOP for when we didn't finish the clear ourselves (a shot whiffed, so
			// "all 4 groups fired" never latched). Her end animation ends the phase -> swap the weapon back
			// and arm the post-phase bind flee, once. (The normal path arms this the moment the clear finishes
			// in handleSiphonPhase; whichever fires first wins via the siphonFleeArmed one-shot.)
			if (a == WhispererAutoIDs.ANIM_SOUL_SIPHON_END && active)
			{
				// Anchor the BIND window to her 10259 end anim (overworld). The flee itself already began the tick
				// we landed in the overworld (handleSiphonPhase), so we don't start it here -- just mark the window.
				lastSiphonEndAnimTick = tick;
				// WHIFF BACKSTOP: clear never latched (a missed shot) so we never crossed/armed -> arm it all here.
				if (!siphonFleeArmed && (inSiphonPhase() || siphonWasActive))
				{
					mainWeaponSwapPending = true;
					siphonFleeArmed = true;
					if (!siphonSwingArmed) { startSwing(Special.SIPHON, tick); siphonSwingArmed = true; }
					step("Siphon", "siphon: 10259 end (backstop) -> flee");
				}
			}
			// Arm the seed phase here, UNCONDITIONALLY each tick -- 10248 is a brief telegraph and the
			// action paths below (tentacle dodge / relock / attack) early-return, so detecting it only
			// inside handleSeedPhase would miss it whenever we're busy. Latch it the instant we see it.
			// NOT during ENRAGE: enrage shares anim 10248 with seeds but has NO seed phase (only tentacles), so
			// latching seeds there sent us scouting for seeds that never come ("in shadow, waiting for seeds").
			if (a == WhispererAutoIDs.ANIM_SEED_PHASE_START && active && !enragePhaseActive)
			{
				seedPhaseActive = true;
				lastSeedActivityTick = tick;
			}
		}
		// On the siphon active->inactive EDGE (the ~20-tick timer lapsing): clear the clear-tracking state and
		// swap the main weapon back in as a FALLBACK (self-no-ops if already worn). The bind flee is NOT armed
		// here -- the timer fires far too late for the bind; it's armed the moment we finish clearing (all four
		// groups fired) inside handleSiphonPhase, which is the real end.
		boolean siphonNow = inSiphonPhase();
		if (!siphonNow)
		{
			siphonActionDone = false;
			siphonClearedQuadrants.clear();
			siphonCenter = null;
			lastSiphonClick = null;
			siphonFleeArmed = false;
			venatorShotTicks.clear();
			desiredRealm = -1;   // clear the return-to-overworld crossing target armed at the clear's end
			// FINAL fallback for the swap-back: guarantee the main weapon is on by the time the siphon timer
			// lapses (no-ops if already wielded during the flee). By here the flee has usually ended, so there
			// is no walk this tick to override it.
			if (siphonWasActive)
			{
				equipMainWeapon();
				mainWeaponSwapPending = false;
			}
		}
		siphonWasActive = siphonNow;
		phase.update(tick);

		// OPENING OSCILLATION bookkeeping. On the dormant->awake transition (the wake knockback), capture the
		// tile we land on as the dance anchor and the tile one south of it. Latch firstSpecialStarted once any
		// special begins (ends the dance). Clear it all when the fight ends (boss gone).
		boolean awakeNow = whisperer != null && isWhispererAwake();
		if (awakeNow && !wasWhispererAwake)
		{
			WorldPoint k = mirror.getPlayerTile();
			if (k != null)
			{
				openingDanceA = k;
				openingDanceB = new WorldPoint(k.getX(), k.getY() - 2, k.getPlane());
			}
			deathPrayersOff = false;   // fresh fight -> re-arm prayer management
			enrageSeen = false;        // fresh fight -> the enrage/death backstop re-arms
			whispererStartCentre = whispererCenterTile();   // fix the arena centreline off her 3x3 starting X/Y
			// NOTE: do NOT reset the thrall here. The opening thrall is cast pre-wake in handleFightStart; resetting
			// on the wake edge would discard it, trigger a wasted recast (one is already up), and the cast's
			// lastAttackTick bump would delay the first attack. The timer runs continuously through the fight.
		}
		wasWhispererAwake = awakeNow;
		// Latch which special opened the fight. Use the AUTHORITATIVE screech signal (pillars captured), NOT
		// inScreechPhase() -- that keys on anims 10251/10252, and a 10251 attack-charge during the opening would
		// latch this with seedPhaseActive=false, wrongly marking the opener as not-seeds and killing the
		// seed-first north reposition. Siphon anims (10256/10257) don't occur outside siphon, so they're safe.
		if (!firstSpecialStarted && (seedPhaseActive || !hidePillarChainOwTiles.isEmpty() || inSiphonPhase()))
		{
			firstSpecialStarted = true;
			firstSpecialWasSeeds = seedPhaseActive;   // remember the rotation's opener
		}
		// The NEXT special (seeds/screech after the post-siphon tentacles) ends the resumed oscillation. Not gated
		// on inSiphonPhase (its ~20-tick timer lingers into this very oscillation and would clear it immediately).
		if (seedPhaseActive || !hidePillarChainOwTiles.isEmpty()) resumeOscillation = false;
		// During a BIND she flips to NPC 12205 (NPC_WHISPERER_BINDING), which findWhisperer() does NOT match, so
		// `whisperer == null` even though the fight is live and a swing is mid-BOUND. Treating that as fight-over
		// wiped swingSpecial / swingStage / repositionNorthActive EVERY bind tick -> by the time 12205 cleared the
		// swing state was gone and we just resumed attacking instead of running the post-bind maneuver. So only
		// reset when she's genuinely absent (not merely in the bind form).
		boolean bindFormPresent = findNpcAny(WhispererAutoIDs.NPC_WHISPERER_BINDING) != null;
		// Note a bind that ACTUALLY ROOTED us during a screech recentre -> on resolve we stop (we're safe) instead of
		// chasing her new northern centre. Keyed on the "binds you in place" chat (boundInPlaceChatSeen), NOT mere
		// 12205 presence: 12205 can flash up without binding us, which was falsely ending the recentre post-screech.
		if (recenterActive && boundInPlaceChatSeen) recenterBindSeen = true;
		// Also NOT fight-over in the SHADOW REALM: she is never a scene NPC there (siphon clear, and the ENRAGE phase
		// after "pulls you into the Shadow Realm"), so whisperer == null is EXPECTED, not the fight ending. Resetting
		// there wiped enragePhaseActive / enrageAnchor every shadow tick -> the enrage dodge never ran (we fell back
		// to the normal tentacle dodge). Only treat absence as fight-over in the OVERWORLD.
		if (whisperer == null && !bindFormPresent && !inShadowRealm())
		{
			firstSpecialStarted = false;
			firstSpecialWasSeeds = false;
			oddFigureWaking = false;
			oddFigureDisturbed = false;
			lastDisturbTick = -100;
			enragePhaseActive = false;
			enrageAnchor = null;
			lastEnrageStepTick = -100;
			recenterBindSeen = false;
			boundInPlaceChatSeen = false;
			repositionNorthActive = false;
			recenterActive = false;
			resumeOscillation = false;
			swingSpecial = Special.NONE;
			swingFleeAnchor = null;
			maneuverSpecial = Special.NONE;
			openingDanceA = null;
			openingDanceB = null;
			lastStartClick = null;
			lastReposClick = null;
			lastRecenterClick = null;
			lastSwingClick = null;
		}

		// Latch the screech hide chain so the pillar overlay stays stable as pillar HP shifts.
		updateHideChain();

		// Always track hazards, even when inactive, so capture + overlay stay useful.
		// Clear EVERY tick before re-projecting (static-tile hazards only expire when their
		// atTick passes, so stale future projections would otherwise linger).
		dodger.clearAll();
		pushHazards(tick);

		// Seed phase: collect good/bad seed tiles and mark bad ones as hazards (safety). The
		// actual "only LAND on good seeds" path planner is pending -- see planSeedStep().
		collectSeedTiles();

		// SCOUT MEASUREMENT: per-shadow-tick counts so we can confirm the info is fully present on the
		// FIRST loaded tick (so cross-back-on-complete really is "next tick"). good = shadow good seeds
		// (47573), pillars = raw 12209, chain = full ranked hide chain (0-3), ow* = captured overworld
		// caches. A partial ring/chain here means the data streams in over >1 tick.
		if (config != null && config.captureMode() && inShadowRealm())
			log.info("[scoutTick] tick={} good={} pillars={} chain={} owSeed={} owPillars={}",
				tick, goodSeedTiles.size(), shadowPillars().size(), hideChainPillars().size(),
				seedRouteOw.size(), hidePillarChainOwTiles.size());

		if (isSeedPhase())
		{
			for (WorldPoint bad : badSeedTiles)
			{
				dodger.addHazard(Hazard.staticTile(bad, tick,     "bad-seed"));
				dodger.addHazard(Hazard.staticTile(bad, tick + 1, "bad-seed"));
			}
		}
		else
		{
			lastSeedTile = null;   // phase over -> forget our ring position
		}

		// PRAYER SWAPPER (the live feature). Only schedule swaps while active so the toggle
		// controls it; prayers.onGameTick() below executes whatever is queued. On toggle-off,
		// stop() clears pending swaps and we leave the player's prayers alone.
		if (active)
		{
			watchPrayerProjectiles(tick);
		}

		dodger.onGameTick(tick);
		prayers.onGameTick(tick);

		updateOverlay();

		if (!active) return;

		// DEATH -> prayers off: on her death animation (10260), OR SOONER when her HP bar reads 0 while we are in
		// the shadow-realm ENRAGE phase (the real final death). Turn ALL prayers off once; deathPrayersOff then
		// gates the prayer watcher so nothing flicks one back on. Re-armed on the next fight's wake.
		boolean deathAnim = anyNpcHasAnim(WhispererAutoIDs.ANIM_WHISPERER_DEATH);   // id-agnostic: she may not be the tracked 12204
		boolean enrageHpZero = enragePhaseActive && inShadowRealm() && WidgetReader.bossHp(client) == 0;
		// BACKSTOP: after the enrage this fight, once she's gone and we're back in the overworld (not binding,
		// not in the shadow realm) the fight is over -- she's dead. Reliable when the anim/hp-0 triggers miss
		// (she's not a scene NPC in the shadow realm, and the HP bar can vanish before it reads 0).
		boolean fightOverAfterEnrage = enrageSeen && whisperer == null
			&& findNpcAny(WhispererAutoIDs.NPC_WHISPERER_BINDING) == null && inOverworld();
		if ((deathAnim || enrageHpZero || fightOverAfterEnrage) && !deathPrayersOff)
		{
			deathPrayersOff = true;
			turnPrayersOff(tick);
			step("Dead", "death -> prayers off");
		}

		// SUPPLIES: runs EVERY tick. Prayer must sip whenever it's low (below ~45), so it can't be gated to the
		// idle windows -- that starved it (and the fall-through consumption below is behind ENABLE_FULL_AUTO). The
		// idle-window restriction instead lives on FOOD alone (see buildConsumptionRequest), so a heal never
		// delays a cast while prayer stays topped.
		handleConsumption(tick);

		// Realm crossings for the scout phases are driven by the phase handlers via
		// serviceRealmCrossing(desiredRealm) -- not a chat one-shot -- so nothing to do here.

		// CLEANUP (post-death): loot -> decide -> wait for the Odd Figure to respawn. Stub for now; claims the
		// tick so the fight handlers stay idle until she is back in the scene.
		if (handleCleanup(tick)) return;

		// FIGHT START: while she's DORMANT, walk to the start tile, then wake her (attack the dormant form).
		// The wake knockback + opening oscillation take over once she's awake.
		if (handleFightStart(tick)) return;

		// TENTACLE (PHASE 1): HIGHEST priority after fight-start. Tentacles don't pause for a swing, so we dodge
		// even while fleeing/maneuvering (like praying a projectile the instant it's seen). (BOUND never coincides
		// with tentacles, so no root guard is needed.) The swing/relock/attack logic all runs AFTER this.
		// ENRAGE uses a different dodge: hop among the 4-tile formation anchored to our entry tile (tentacles come
		// fast and there are no specials). Otherwise the normal 8-neighbour tentacle dodge.
		WorldPoint tentacleStep = enragePhaseActive ? planEnrageDodge(tick) : planTentacleDodge(tick);
		if (tentacleStep != null)
		{
			// OPENING OSCILLATION: keep the dodge CADENCE (we only step when planTentacleDodge says to -- not
			// every tick), but during the opening (wake knockback -> first special) constrain the destination
			// to the two latched tiles: step to whichever of them we're NOT currently on. Between dodges we
			// stand and attack as normal. After the first special, the computed dodge tile is used again.
			if (!enragePhaseActive && (!firstSpecialStarted || resumeOscillation) && openingDanceA != null && openingDanceB != null && inOverworld())
			{
				WorldPoint me = mirror.getPlayerTile();
				tentacleStep = (me != null && me.equals(openingDanceA)) ? openingDanceB : openingDanceA;
			}
			// Keep the dodge INSIDE the traced arena -- never click a tile outside the walls. No-op unless our
			// own tile is recognised as interior, so a coordinate-space mismatch disables it safely.
			tentacleStep = WhispererArena.clampInside(tentacleStep, mirror.getPlayerTile());
			// FINAL SAFETY: never step onto OR beside her (5x5, corners incl.). bestStep already guards this, but
			// the opening-oscillation override and clampInside above pick tiles WITHOUT that guard, so re-check the
			// final tile here. If it would land on/beside her, HOLD this tick rather than step in (same call the
			// dodge guard makes) -- stepping into her is a guaranteed hit.
			net.runelite.api.coords.WorldArea bArea = whisperer == null ? null : whisperer.getWorldArea();
			if (bArea != null && tentacleStep != null && bArea.distanceTo(tentacleStep) <= 1)
			{
				// The opening-oscillation override / clampInside picked a tile ON or BESIDE her (both run AFTER
				// bestStep, bypassing its 5x5 guard). Stepping in is a guaranteed hit -- but DON'T freeze in the
				// tentacle field: there's almost always another safe tile. Recompute a dodge-safe neighbour and use
				// it; only hold if literally every surrounding tile is a hit (which shouldn't happen here).
				WorldPoint recalc = safeNeighbourStep(mirror.getPlayerTile(), tentacleStep, npcsNow(), effectiveTentacleGap());
				if (recalc == null)
				{
					step("Dodge tentacle", "tentacle dodge: beside boss, no safe tile -> hold");
					return;
				}
				tentacleStep = recalc;
				step("Dodge tentacle", "tentacle dodge: beside boss -> recalc " + recalc);
			}
			if (config != null && config.captureMode())
				log.info("[gate] tentacle dodge -> {} (plane={}) -- skipping seed handling", tentacleStep, currentPlane());
			router.walkTo(tentacleStep);
			tentacleRelockPending = true;   // the walk drops our attack lock; re-lock next tick
			step("Dodge tentacle", "tentacle dodge -> " + tentacleStep);
			return;
		}
		// Dodge step has landed -> re-click the Whisperer to re-lock. ONLY in normal fighting (swingSpecial NONE):
		// mid-swing we must NOT re-attack (that would pull us off the flee) -- handleSwing below resumes the flee.
		if (tentacleRelockPending && swingSpecial == Special.NONE)
		{
			if (config != null && config.captureMode())
				log.info("[gate] relock (plane={}) -- skipping seed handling", currentPlane());
			tentacleRelockPending = false;
			relockWhisperer(tick);
			return;
		}

		// POST-SPECIAL SWING (flee -> bound/hold) for seeds / screech / siphon. Runs AFTER the tentacle dodge now,
		// so a tentacle mid-swing is dodged first; when no dodge step fires, this resumes the flee / bind hold.
		if (handleSwing(tick)) return;

		// Shared SHADOW SCOUT (armed by 10248 for seeds OR screech), disambiguated in shadow: ranked
		// pillars -> screech, good seeds -> corrupted seeds; each crosses back once its info is captured.
		if (config != null && config.captureMode())
			log.info("[gate] reached scout (plane={} seedActive={})", currentPlane(), seedPhaseActive);
		// SIPHON: entry (venator bow + blackstone) fired on 10257 above. In the shadow realm we camp under
		// her 3x3 and clear the 4 soul groups with the bow. Claims the tick while it's running.
		if (handleSiphonPhase(tick)) return;
		// SCREECH takes precedence: once the shadow scout has captured the ranked pillars (the reliable
		// "this is screech" signal), screech owns the tick and crosses us back. Must come BEFORE the seed
		// handler so the seed scout can't sit "waiting for seeds" in shadow or ping-pong us.
		if (handleScreechPhase(tick)) return;

		// CORRUPTED SEEDS / shared scout cross-in (armed by 10248). If it turns out to be screech, the
		// handler above claims it the moment the pillars are captured.
		if (handleSeedPhase(tick)) return;

		// POST-PHASE BIND: right after a phase ends she binds + walks in. Keep the 10-tile gap (flee/hold)
		// through the bind window before resuming DPS. Runs after the phase handlers (they've returned) and
		// before maintainBossAttack so a boss re-click can't drag us back toward her.
		if (handlePhaseEndBind(tick)) return;

		// SEED-FIRST rotation: once the bind flee is done, dance our way around to the NORTH of her (she was
		// pulled south, and the open floor is north). The tentacle dodge above still takes priority.
		if (handleRepositionNorth(tick)) return;

		// SCREECH RECENTRE: after the screech flee (north), walk back down to the centre camp before resuming DPS.
		if (handleRecenter(tick)) return;

		// GUARANTEED main-weapon swap-back (backstop). If we're fighting in the overworld, NOT siphoning, with no
		// swing/pending-swap in flight, yet the main weapon is still sitting in the INVENTORY (so we're still on the
		// Venator bow from a siphon whose swap-back click was dropped -- e.g. lost during the realm crossing), wield
		// it now. Self-correcting: once worn, countItem == 0 and this stops. Gated on swingSpecial==NONE so it never
		// fights the deferred wield inside the swing.
		if (inOverworld() && isWhispererAwake() && !inSiphonPhase()
			&& swingSpecial == Special.NONE && mainWeaponWieldId() > 0)
		{
			equipMainWeapon();
			mainWeaponSwapPending = false;
			step("Equip weapon", "swap-back backstop: wield main weapon");
		}

		// LIVE: attack the awake Whisperer (12204). The dodge / relock / seed handling above take
		// priority; this keeps DPS up the rest of the time. The opening gap (~3 tiles) is irrelevant
		// -- the attack click walks us in and the tentacle dodge maintains our spacing after.
		maintainBossAttack(tick);

		// --- Everything below moves or clicks; held back until its mechanics are ready. ---
		if (!ENABLE_FULL_AUTO) return;

		// Movement planning: TODO -- Whisperer-specific tiles. For now defer to the dodger.
		resolveInstanceTilesIfNeeded(tick);
		WorldPoint me = mirror.getPlayerTile();
		if (me != null)
		{
			WorldPoint step = dodger.computeDodge(me, null, 3).orElse(null);
			if (step != null && !step.equals(me))
			{
				router.walkTo(step);
				step("Dodge tentacle", "dodge -> " + step);
				return;
			}
		}

		// Supply consumption (engine's overheal cap gates eating).
		ConsumptionEngine.Request req = buildConsumptionRequest();
		ConsumptionEngine.Result res = engine.evaluate(req);
		applyConsumption(res);

		// Attack maintenance: poke the boss (IDLE) / keep attacking (FIGHT).
		doAttack(tick);
	}

	// -----------------------------------------------------------------
	// Hazards + prayer.
	// -----------------------------------------------------------------

	/**
	 * Per-tick hazard projection: ground-hazard graphics objects. (Prayer swaps are driven
	 * separately from the tick loop, gated on {@code active}.)
	 * <p>
	 * TODO: the hazard timing here is a conservative placeholder -- it flags each hazard tile
	 * as dangerous now and for the next 2 ticks. Refine once the mechanic is pinned down
	 * (esp. {@link WhispererAutoIDs#GFX_HAZARD_PRIMARY} 2450, which the user will elaborate on),
	 * plus tentacle (12208) handling.
	 */
	private void pushHazards(int tick)
	{
		pushGraphicsHazards(tick);
	}

	/**
	 * Scan the scene for good (47573) / bad (47574) seed objects, recording their tiles. Checks BOTH
	 * GameObjects and GroundObjects (the seeds appear to be GroundObjects, which getGameObjects()
	 * doesn't return). In capture mode, when the seed phase is active but nothing matched, dumps the
	 * distinct object ids present so we can confirm the real seed id / type.
	 */
	private void collectSeedTiles()
	{
		goodSeedTiles.clear();
		badSeedTiles.clear();
		overworldSeedTiles.clear();
		overworldSeedGfxCount = 0;
		try
		{
			// SHADOW realm: good (47573) / bad (47574) seeds are distinct GameObjects. Read the GOOD tiles
			// so we can translate them to overworld coords and walk them after crossing back. The shadow
			// realm may come in as a null/alternate world view, so guard it (and never throw in the tick).
			boolean diag = config != null && config.captureMode() && seedPhaseActive;
			java.util.Set<Integer> diagGame = diag ? new java.util.TreeSet<>() : null;
			Scene scene = client.getTopLevelWorldView() == null ? null : client.getTopLevelWorldView().getScene();
			int plane = client.getTopLevelWorldView() == null ? -1 : client.getTopLevelWorldView().getPlane();
			if (scene != null)
			{
				Tile[][][] tiles = scene.getExtendedTiles();
				if (tiles != null && plane >= 0 && plane < tiles.length && tiles[plane] != null)
				{
					for (Tile[] col : tiles[plane])
					{
						if (col == null) continue;
						for (Tile t : col)
						{
							if (t == null) continue;
							GameObject[] gos = t.getGameObjects();
							if (gos == null) continue;
							for (GameObject g : gos)
							{
								if (g == null) continue;
								int id = g.getId();
								if (diag && id > 0) diagGame.add(id);
								if (id == WhispererAutoIDs.GOBJ_GOOD_SEED)     goodSeedTiles.add(t.getWorldLocation());
								else if (id == WhispererAutoIDs.GOBJ_BAD_SEED) badSeedTiles.add(t.getWorldLocation());
								// OVERWORLD: every seed (good + bad alike) shows as the PERSISTENT on-ground
								// GameObject 47575 -- indistinguishable here. The 2459 graphic is only the
								// transient spawn flash, so we bind to the GameObject: it stays down for the
								// whole phase, keeping the phase latched and letting us confirm a planned good
								// tile still has a seed before we step onto it.
								else if (id == WhispererAutoIDs.GOBJ_OVERWORLD_SEED) overworldSeedTiles.add(t.getWorldLocation());
							}
						}
					}
				}
			}

			overworldSeedGfxCount = overworldSeedTiles.size();

			if (diag && goodSeedTiles.isEmpty())
			{
				int tk = mirror.getTickCounter();
				if (tk - lastSeedDiagTick >= 5)
				{
					lastSeedDiagTick = tk;
					log.info("[seedScan] plane={} good(47573)=0 ow47575={} gameIds={}",
						plane, overworldSeedGfxCount, diagGame);
				}
			}
		}
		catch (Exception e)
		{
			if (config != null && config.captureMode()) log.info("[seedScan] collect threw: {}", e.toString());
		}
	}

	/** True while seeds are present in either realm: shadow GameObjects (good/bad) or the overworld
	 *  persistent seed GameObjects (47575). Keeps the phase latched through the whole scout + overworld walk. */
	private boolean isSeedPhase()
	{
		return !goodSeedTiles.isEmpty() || !badSeedTiles.isEmpty() || overworldSeedGfxCount > 0;
	}

	/**
	 * Drive the corrupted-seeds phase. Returns true while the phase owns the tick.
	 * <p>
	 * Good/bad seeds are only distinguishable in the SHADOW realm (good = GameObject 47573). In the
	 * overworld all seeds look alike (gfx 2459). So: arm on 10248; DPS until the fragment charges;
	 * cross to shadow and read the good seeds; translate them to overworld coords ({@link #seedRouteOw});
	 * cross back; ring-walk the translated good tiles. Cleared on screech onset or a seed timeout.
	 */
	/** Full teardown of the corrupted-seeds phase state -- flags, captured route, walk cursor, scout
	 *  bookkeeping and the realm-crossing target. Shared by the deterministic 10252 end and the
	 *  seeds-gone timeout so both leave identical state. Also arms the post-phase bind flee (she binds +
	 *  walks in after seeds too), so we open the 10-tile gap the moment the phase ends. */
	private void resetSeedPhase(int tick)
	{
		seedPhaseActive = false;
		seedScoutDone = false;
		seedRouteOw.clear();
		seedPath.clear();
		seedWalkIdx = 0;
		lastSeedClick = null;
		seedTargetSetTick = -100;
		lastSeedTile = null;
		shadowEnterTick = -1;
		seedSeenTick = -100;
		desiredRealm = -1;
		// Seeds ended -> run the post-special swing: flee SOUTH, then (if she binds) hold, then maneuver north.
		startSwing(Special.SEEDS, tick);
	}

	private boolean handleSeedPhase(int tick)
	{
		// ENRAGE has no seeds (it just shares anim 10248). Drop any seed phase latched off that anim and bail, so
		// we don't scout / cross for seeds that never come.
		if (enragePhaseActive) { seedPhaseActive = false; return false; }
		// Keep the phase alive while we still have route left to walk. The overworld 2459 markers are
		// transient and can read as "gone" for a few ticks mid-walk, which must NOT time out the phase
		// and reset us (that was the "crash on 5" -- overlay vanished, movement stopped).
		boolean walkingRoute = seedScoutDone && (seedPath.isEmpty() || seedWalkIdx < seedPath.size());
		if ((isSeedPhase() || walkingRoute) && tick > seedEndLockoutUntil)
		{
			seedPhaseActive = true;
			lastSeedActivityTick = tick;
		}
		if (!seedPhaseActive) return false;

		// Logged BEFORE the end-check so we can see why the phase might bail.
		if (config != null && config.captureMode())
		{
			log.info("[seedDbg] plane={} shadow={} ow={} scoutDone={} good={} route={} ow47575={} screech={} sinceAct={} c28356={} c28357={} me={}",
				currentPlane(), inShadowRealm(), inOverworld(), seedScoutDone,
				goodSeedTiles.size(), seedRouteOw.size(), overworldSeedGfxCount,
				inScreechPhase(), tick - lastSeedActivityTick,
				mirror.countItem(WhispererAutoIDs.ITEM_BLACKSTONE_FRAGMENT),
				mirror.countItem(WhispererAutoIDs.ITEM_BLACKSTONE_FRAGMENT_DRAINED), mirror.getPlayerTile());
		}

		// DETERMINISTIC END: stepping the FINAL seed fires 10252 (ANIM_SCREECH_ATTACK) -- the resolution of
		// the charge (10251), whether it fizzles or releases the blast -- which marks the end of the seed
		// phase. This is cleaner than waiting out the timeout below. Gate on the SEED context (pillars NOT
		// captured): a real screech also plays 10252, but there hidePillarChainOwTiles is populated, so this
		// never fires during screech. Idempotent -- re-running it while 10252 lingers just stays ended.
		if (whisperer != null
			&& whisperer.getAnimation() == WhispererAutoIDs.ANIM_SCREECH_ATTACK
			&& hidePillarChainOwTiles.isEmpty())
		{
			if (config != null && config.captureMode())
				log.info("[seedDbg] 10252 on final seed -> ending seed phase (tick={})", tick);
			seedEndLockoutUntil = tick + WhispererAutoIDs.SEED_GONE_END_TICKS;
			resetSeedPhase(tick);
			return false;
		}

		// FALLBACK END on its OWN signal: seeds gone for a few ticks (in case we miss the 10252). Phases are
		// independent -- we never end the seed phase by detecting another (e.g. screech), since her seed-phase
		// animation can itself read as a screech. lastSeedActivityTick refreshes every tick seeds are visible
		// (shadow 47573 or overworld 2459), so this fires only once they are truly gone.
		if (tick - lastSeedActivityTick > WhispererAutoIDs.SEED_GONE_END_TICKS)
		{
			resetSeedPhase(tick);
			return false;
		}

		if (inShadowRealm()) { if (shadowEnterTick < 0) shadowEnterTick = tick; }
		else shadowEnterTick = -1;

		WorldPoint me = mirror.getPlayerTile();

		// --- SCOUT: cross to shadow, read good seeds (47573), translate to overworld tiles. ---
		if (!seedScoutDone)
		{
			desiredRealm = WhispererAutoIDs.SHADOW_REALM_PLANE;
			if (!inShadowRealm())
			{
				// DPS through the ~3-tick gap until the fragment charges (28356); then cross in.
				if (mirror.countItem(WhispererAutoIDs.ITEM_BLACKSTONE_FRAGMENT) <= 0)
				{
					step("Seeds", "seed: armed (10248), DPS until fragment charges");
					return false;
				}
				serviceRealmCrossing(tick);
				step("Seeds", "seed: crossing to shadow");
				return true;
			}
			// In shadow but still mid-transition / not loaded -> no good seeds yet. WAIT (hold position)
			// until we actually see them; the transition screen is ~1 tick, so just keep scanning.
			if (goodSeedTiles.isEmpty())
			{
				step("Seeds", "seed: in shadow, waiting for seeds");
				return true;
			}
			// First good-seed sighting -> start the dwell from here (not from entering the plane).
			if (seedSeenTick < 0) seedSeenTick = tick;
			// Translate each good seed to its overworld tile and UNION into the route (dedup). Never
			// replace/shrink -- so tiles that translate on one tick but not another, or appear across
			// ticks, are all kept. Count null translations so we can see if the math is dropping tiles.
			int ok = 0, dropped = 0;
			WorldPoint nullSrc = null;
			for (WorldPoint s : goodSeedTiles)
			{
				WorldPoint o = shadowToOverworld(s);
				if (o == null) { dropped++; if (nullSrc == null) nullSrc = s; continue; }
				ok++;
				if (!seedRouteOw.contains(o)) seedRouteOw.add(o);
			}
			if (config != null && config.captureMode())
				log.info("[seedMap] goodNow={} okNow={} droppedNow={} routeTotal={} shadowSample={} owSample={} nullSrc={}",
					goodSeedTiles.size(), ok, dropped, seedRouteOw.size(),
					goodSeedTiles.get(0), seedRouteOw.isEmpty() ? "-" : seedRouteOw.get(seedRouteOw.size() - 1),
					nullSrc);
			// CROSS-BACK-ON-COMPLETE: done the moment the route is captured (no fixed dwell), so we spend
			// as little time in shadow as possible. The per-shadow-tick [scoutTick] log lets us confirm the
			// ring is fully present on the capture tick; if it ever streams in over >1 tick we'd re-add a
			// completeness guard here rather than a blind dwell.
			if (!seedRouteOw.isEmpty())
			{
				seedScoutDone = true;
			}
			if (overlay != null)
				step("Seeds", "seed: mapping (shadow) good=" + goodSeedTiles.size() + " route=" + seedRouteOw.size());
			return true;
		}

		// --- RETURN: cross back to the overworld. ---
		desiredRealm = WhispererAutoIDs.OVERWORLD_PLANE;
		if (!inOverworld())
		{
			serviceRealmCrossing(tick);
			step("Seeds", "seed: crossing to overworld (" + seedRouteOw.size() + ")");
			return true;
		}

		// --- WALK the translated good-seed route in the overworld, in PATH order. ---
		if (seedRouteOw.isEmpty())
		{
			step("Seeds", "seed: waiting for route");
			return true;
		}
		// Order the route into a walkable path ONCE, starting from the seed nearest where we re-entered.
		if (seedPath.isEmpty()) buildSeedPath(me);

		// Advance past every seed we're already standing on, THEN click the next in the SAME tick -- so
		// arriving on a tile and clicking the next happen together (no 1-tick pause between tiles).
		while (seedWalkIdx < seedPath.size() && me != null
			&& seedPath.get(seedWalkIdx) != null
			&& cheby(me, seedPath.get(seedWalkIdx)) == 0)
		{
			seedWalkIdx++;
			seedTargetSetTick = tick;
		}

		if (seedWalkIdx >= seedPath.size())
		{
			step("Seeds", "seed: route complete (" + seedPath.size() + ")");
			return true;
		}
		if (seedTargetSetTick < 0) seedTargetSetTick = tick;

		// STUCK safety: if we can't reach the current target within a few ticks, skip it rather than
		// standing in the field (which would get us killed). We rely on THIS -- not seed-marker presence
		// -- since the 2459 graphics are transient and not reliably on every tile each tick.
		if (tick - seedTargetSetTick > WhispererAutoIDs.SEED_STUCK_TICKS)
		{
			seedWalkIdx++;
			seedTargetSetTick = tick;
			step("Seeds", "seed: skip stuck -> " + seedWalkIdx + "/" + seedPath.size());
			return true;
		}

		// CLICK-SKIP: don't click every tile of a straight run. Extend the target to the far end of the
		// straight, contiguous run that starts at it and click THAT -- the engine walks over the colinear
		// seeds in between (e.g. 3,4,5: click 5, 4 is stepped on the way). seedWalkIdx still advances
		// tile-by-tile above as we physically pass over each one, so the stuck timer and completion
		// tracking are unaffected.
		int clickIdx = straightRunEnd(seedWalkIdx, me);
		WorldPoint clickTarget = seedPath.get(clickIdx);

		// WEAVE: attack the Whisperer when the weapon is off cooldown -- but ONLY when the next move is a
		// single tile (a turn/corner), never mid straight-run. Attacking interrupts the walk, so weaving
		// during a multi-tile run would stop us on an in-between tile and break the click-skip glide. At a
		// corner we'd pause anyway, so a cast there is free. No range gate (we're within ~4 tiles on the
		// ring). seedTargetSetTick is refreshed so the attack isn't read as "stuck".
		if (clickIdx == seedWalkIdx && whisperer != null && tick - lastAttackTick >= effectiveCooldown())
		{
			attackWhisperer();
			lastAttackTick = tick;
			seedTargetSetTick = tick;
			lastSeedClick = null;   // attack interrupts the walk -> re-click the run end next tick
			step("Seeds", "seed: weave attack (" + (seedWalkIdx + 1) + "/" + seedPath.size() + ")");
			return true;
		}

		// Issue (or RE-issue) the walk. Re-click when it's a new target OR we've gone idle short of it: a
		// misclick resolves against the pre-move hover tile (often our own), so the walk never takes and we
		// stand frozen in the field. Re-firing the same destination recovers us -- the stuck-skip above
		// (seedTargetSetTick, deliberately NOT reset here) still owns the genuine-unreachable fallback.
		boolean seedNewTarget    = !clickTarget.equals(lastSeedClick);
		boolean seedStalledShort = me != null && clickTarget != null
			&& client.getLocalDestinationLocation() == null && cheby(me, clickTarget) > 0;
		if (me != null && clickTarget != null && (seedNewTarget || seedStalledShort))
		{
			router.walkTo(clickTarget);
			lastSeedClick = clickTarget;
			if (overlay != null)
				step("Seeds", "seed: " + (seedStalledShort && !seedNewTarget ? "re-click " : "-> ") + (seedWalkIdx + 1)
					+ (clickIdx > seedWalkIdx ? ".." + (clickIdx + 1) : "")
					+ "/" + seedPath.size() + " " + clickTarget);
		}
		return true;
	}

	/**
	 * Screech branch of the shared shadow scout. The scout cross-in is armed by 10248 (seed OR screech --
	 * we don't know which yet) and handled by the seed scout, which brings us into the shadow realm. The
	 * RELIABLE "this is screech, not seeds" signal is {@link #hidePillarChainOwTiles} being populated:
	 * {@link #updateHideChain} only latches ranked 12209 pillars, which exist in screech and not in the
	 * seed phase. (We do NOT gate on {@link #inScreechPhase()} -- her seed animation can read as a screech,
	 * so that false-positives during seeds.)
	 *
	 * <p>Once the pillars are captured we OWN the phase: stop the seed scout (it was armed by the shared
	 * 10248 and would otherwise sit "waiting for seeds" in shadow and ping-pong us back after), and cross
	 * straight back to the overworld the same moment -- cross-back-on-complete, no dwell -- so we spend as
	 * little time in the shadow realm as possible. Must run BEFORE the seed handler. Overworld hiding is
	 * still TODO; we hold the tick there so nothing else acts during screech.
	 */
	private boolean handleScreechPhase(int tick)
	{
		if (hidePillarChainOwTiles.isEmpty()) return false;   // not confirmed screech yet -> not our tick

		// Confirmed screech. Stop the seed scout from fighting us (shared 10248 arm) and drop any stale
		// seed-walk state so it can't resume after screech ends.
		seedPhaseActive = false;
		seedScoutDone = false;
		seedRouteOw.clear();
		seedPath.clear();
		seedWalkIdx = 0;

		if (inShadowRealm())
		{
			// Pillars captured -> cross straight back to the overworld (minimal time in shadow).
			desiredRealm = WhispererAutoIDs.OVERWORLD_PLANE;
			serviceRealmCrossing(tick);
			step("Screech", "screech: pillars captured -> crossing back to overworld");
		}
		else
		{
			hideBehindChain(tick);
		}
		return true;   // screech owns its ticks
	}

	/**
	 * Overworld hide walk, advancing on PILLAR CONSUMPTION (not a timer). We take cover behind the current
	 * hide spot's pillar; the instant that pillar despawns/dies we attack the Whisperer and run to the next
	 * spot's nearest tile (fastest cover). Once we're behind a spot we slide to the sub-tile nearest the
	 * NEXT spot ({@link #hideSpotStandTile}) to prep the following step.
	 */
	/** The screech's column-attack projectile (2471) is in flight AT our latched hide pillar -> it is being
	 *  struck. We act on SPAWN + 1 TICK (tick >= startTick + 1): the projectile launches on startTick, and we move
	 *  the tick after -- earlier than the HP ratio updating or the pillar despawning, but one tick off the raw
	 *  launch. Matched to OUR specific pillar (target actor, else target tile within its footprint) so a 2471 aimed
	 *  at another pillar in the sweep doesn't move us early. Both points are world space from the same client, so
	 *  instancing is moot. */
	private boolean hidePillarUnderAttack(int tick)
	{
		NPC pil = screechHidePillarNpc;
		if (pil == null) return false;
		WorldPoint pw = pil.getWorldLocation();
		for (MirrorState.ProjectileRecord pr : mirror.getProjectileRecords())
		{
			if (pr.id != WhispererAutoIDs.PROJ_COLUMN_ATTACK || pr.projectile == null) continue;
			if (tick < pr.startTick + 1) continue;   // act on spawn + 1 tick, not the raw launch tick
			if (pr.projectile.getTargetActor() == pil) return true;
			WorldPoint tp = pr.projectile.getTargetPoint();
			if (tp != null && pw != null && tp.getPlane() == pw.getPlane()
				&& Math.abs(tp.getX() - pw.getX()) <= 2 && Math.abs(tp.getY() - pw.getY()) <= 2)
				return true;
		}
		return false;
	}

	private void hideBehindChain(int tick)
	{
		java.util.List<WorldPoint> spots = hideChainSafespots();
		WorldPoint me = mirror.getPlayerTile();
		if (spots == null || me == null || screechHideIdx >= spots.size()) return;

		// Latch when THIS spot's approach began, so the weave knows how much of the move window is left.
		if (screechHideIdx != screechMoveStartIdx) { screechMoveStartTick = tick; screechMoveStartIdx = screechHideIdx; screechApproachWeaved = false; }

		// DIAGNOSTIC (capture mode): what would attacking the boss from here do? We expect willMove=true while
		// behind a column (no LoS -> the engine drags us around it, attackFrom off cover), and willMove=false
		// with a clear line in range. Validate this against reality before we let it gate a screech weave.
		if (config != null && config.captureMode())
			log.info("[atkPred] spot={} me={} range={} {}", screechHideIdx + 1, me, effectiveRange(), predictBossAttack(me));

		// Latch the SPECIFIC overworld pillar (12210) we hide behind for this spot, ONCE, then watch THAT
		// npc die. Re-scanning for the nearest each tick kept finding a still-alive neighbour pillar (there
		// are several per rank), so it never read as consumed.
		if (screechHidePillarNpc == null)
		{
			screechHidePillarNpc = overworldHidePillar(screechHideIdx);
			if (screechHidePillarNpc != null)
			{
				screechPillarSeen = true;
				screechHidePillarHp = screechHidePillarNpc.getHealthRatio();   // baseline (-1 = no bar yet = undamaged)
			}
		}
		NPC cur = screechHidePillarNpc;
		// We act the instant our pillar GETS HIT -- not when it dies/despawns (that wastes exposure ticks). The
		// screech destroying our pillar is our cue to move to the next cover NOW. gone = despawned; dead = HP 0 /
		// isDead; hit = its health DROPPED from the baseline we latched (a bar appearing, or the ratio falling),
		// i.e. the screech just struck it.
		int hp = cur != null ? cur.getHealthRatio() : -99;
		boolean present = cur != null && cur.getLocalLocation() != null
			&& client.getNpcs() != null && client.getNpcs().contains(cur);
		boolean gone = screechPillarSeen && !present;
		boolean dead = screechPillarSeen && cur != null && (cur.isDead() || hp == 0);
		// PRIMARY hit signal: the 2471 column-attack projectile is in flight at our pillar (fires on launch, the
		// earliest possible cue). Health-ratio fallbacks catch the case where the projectile read is missed.
		boolean projHit = screechPillarSeen && cur != null && hidePillarUnderAttack(tick);
		boolean hit = projHit
			|| (screechPillarSeen && cur != null
				&& ((screechHidePillarHp < 0 && hp >= 0)                              // bar appeared -> took damage
				 || (screechHidePillarHp >= 0 && hp >= 0 && hp < screechHidePillarHp))); // ratio fell -> took damage

		if (config != null && config.captureMode())
			log.info("[hideDbg] idx={} seen={} arrived={} present={} gone={} dead={} hit={} projHit={} hp0={} hp={} me={}",
				screechHideIdx, screechPillarSeen, screechHideArrived, present, gone, dead, hit, projHit,
				screechHidePillarHp, hp, me);

		if (gone || dead || hit)
		{
			if (screechHideIdx + 1 < spots.size())
			{
				// SHORT TRIP vs LONG TRIP decides whether we squeeze a DPS weave on the way to the next cover: the
				// onward move-ticks alone (peek the NEXT spot -- idx hasn't advanced yet). reachable/!willMove guard
				// against clicking attack with no clear in-range tile (which would pull us off cover).
					WorldPoint nextSw = spots.get(screechHideIdx + 1);
					WorldPoint nextCover = nextSw == null ? null : closestSubTile(nextSw, me);
					int ftpt = tilesPerTick();
					int trip = nextCover == null ? Integer.MAX_VALUE / 4 : walkTicksAround(me, nextCover, ftpt);
					boolean shortTrip = trip <= SCREECH_SHORT_TRIP_TICKS;

					// SHORT TRIP has slack to squeeze one DPS shot; LONG TRIP does not. We now act on the HIT (2471), so at the
					// hit tick the pillar is STILL UP and blocks LoS -- a clean stationary shot only exists once it DESPAWNS. So
					// on a short trip we HOLD at cover (even after the hit) until the pillar clears, then weave the shot and move;
					// on a long trip we leave on the hit (no weave) to cut exposure.
					if (shortTrip && !gone && whisperer != null && tick - lastAttackTick >= effectiveCooldown()) return;

					net.runelite.client.plugins.autoBossCore.combat.AttackPredictor.Result fpr = predictBossAttack(me);
					boolean fallWeave = shortTrip && gone && fpr != null && fpr.reachable && fpr.attackFrom != null
						&& !fpr.willMove && whisperer != null && tick - lastAttackTick >= effectiveCooldown();

					screechHideIdx++;
					screechHideArrived = false;
					screechPillarSeen = false;
					screechHidePillarNpc = null;   // re-latch the next spot's pillar
					lastHideClick = null;

					if (config != null && config.captureMode())
						log.info("[fallWeave] spot={} me={} nextCover={} trip={} short={} gone={} pred={} weave={}",
							screechHideIdx + 1, me, nextCover, trip, shortTrip, gone, fpr, fallWeave);
					if (fallWeave)
					{
						attackWhisperer();
						lastAttackTick = tick;
						lastHideClick = null;   // next tick we re-click the cover tile and run
						step("Screech", "screech: pillar clear -> weave, then run to spot " + (screechHideIdx + 1));
					}
					else if (nextCover != null)
					{
						hideWalk(nextCover);
						step("Screech", "screech: pillar hit -> run to spot " + (screechHideIdx + 1));
					}
			}
			else
			{
				// LAST (3rd) pillar HIT (2471 + 1 tick) -> screech is ending. Slip ONE attack on the Whisperer in THIS
				// tick, THEN head to the flee/camp tile via the recentre maneuver below. We fire on the HIT, not on
				// despawn, to get the shot in during the closing gap. Gated only on the weapon being off cooldown +
				// her being present.
				if (whisperer != null && tick - lastAttackTick >= effectiveCooldown())
				{
					attackWhisperer();
					lastAttackTick = tick;
				}
				// Screech MAY be followed by a bind, but we need NO swing/maneuver for it: the recentre already puts us
				// at the flee/camp tile (gap 11 N of her centre), i.e. already north of her. If she then binds she just
				// walks the 10 tiles in and stops 1 short (gap 1, no contact) while we hold rooted; when it clears we
				// resume DPS from the same north tile. So go straight to recentre -- no flee-tile bind-watch.
				maneuverSpecial = Special.SCREECH;
				repositionNorthActive = false;
				recenterActive = true;
				recenterBindSeen = false;
				boundInPlaceChatSeen = false;   // fresh recentre -> only a real post-screech bind (chat) counts
				lastRecenterClick = null;
				step("Screech", "screech: last pillar hit -> attack, then recentre (no swing)");
			}
			return;
		}

		WorldPoint sw = spots.get(screechHideIdx);
		if (sw == null) return;

		if (!screechHideArrived)
		{
			// Running to this spot: SAFETY FIRST -- head for its tile NEAREST us so we reach cover fast.
			// No weaving here; we don't gamble a tick on an attack while still exposed.
			WorldPoint run = closestSubTile(sw, me);
			if (me.equals(run))
			{
				screechHideArrived = true;
			}
			else
			{
				// WEAVE during the approach ONLY for the FIRST pillar (idx 0, off the scout cross-back -- the big
				// APPROACH window, plenty of time), and ONLY as a STATIONARY cast (willMove == false: already in
				// range + LoS). A pull-in attack (willMove == true) would drag us toward the boss, OFF the approach
				// line -- that's what was clicking the cover tile then cancelling the walk. We also fire it at most
				// ONCE per approach ({@link #screechApproachWeaved}); re-attacking every time the cooldown came up
				// kept interrupting the run. After the one stationary cast we run to cover uninterrupted.
				if (screechHideIdx == 0 && !screechApproachWeaved
					&& whisperer != null && tick - lastAttackTick >= effectiveCooldown())
				{
					net.runelite.client.plugins.autoBossCore.combat.AttackPredictor.Result pr = predictBossAttack(me);
					int tpt = tilesPerTick();
					int remaining = SCREECH_APPROACH_WINDOW_TICKS - (tick - screechMoveStartTick);
					boolean stationary = pr != null && !pr.willMove && pr.reachable;   // in place -> won't drag us off
					boolean canCoverAfter = stationary
						&& (1 + walkTicksAround(me, run, tpt)) <= remaining;   // one tick to cast, then run to cover
					if (config != null && config.captureMode())
						log.info("[weaveDbg] spot=1 me={} run={} rem={} pred={} stationary={} coverAfter={}",
							me, run, remaining, pr, stationary, canCoverAfter);
					if (canCoverAfter)
					{
						attackWhisperer();
						lastAttackTick = tick;
						screechApproachWeaved = true;   // one weave per approach -> stop interrupting the run
						lastHideClick = null;   // attack interrupts the run -> re-click the cover tile next tick
						step("Screech", "screech: approach weave -> spot 1");
						return;
					}
				}
				hideWalk(run);
				step("Screech", "screech: -> cover spot " + (screechHideIdx + 1) + " " + run);
				return;
			}
		}

		// SAFE-TILE WEAVE. Two stand tiles have a real LoS to her that won't path us out. They are TEMPLATE coords
		// (SCREECH_SAFE_ATTACK_TEMPLATE); the arena is instanced, so convert each to the live instance tile before
		// comparing with getPlayerTile (hardcoded absolutes never matched). If we're hiding AT one of those pillars,
		// STAND on the safe tile and weave from it -- BEFORE the readjust below, so the normal stand-tile slide can't
		// drag us off it. We don't trust the predictor's LoS here; the tile IS the guard.
		WorldPoint safeW = InstanceCoords.fromTemplate(client, SCREECH_SAFE_ATTACK_TEMPLATE[0]);
		WorldPoint safeE = InstanceCoords.fromTemplate(client, SCREECH_SAFE_ATTACK_TEMPLATE[1]);
		if (config != null && config.captureMode())
			log.info("[holdWeave] spot={} me={} safeW={} safeE={} dW={} dE={}",
				screechHideIdx + 1, me, safeW, safeE,
				safeW == null ? -1 : me.distanceTo(safeW), safeE == null ? -1 : me.distanceTo(safeE));
		for (WorldPoint s : new WorldPoint[]{ safeW, safeE })
		{
			if (s == null) continue;
			if (me.equals(s))
			{
				// On the safe tile -> weave when off cd; otherwise HOLD here (never readjust off it).
				if (whisperer != null && tick - lastAttackTick >= effectiveCooldown())
				{
					attackWhisperer();
					lastAttackTick = tick;
					step("Screech", "screech: safe-tile weave spot " + (screechHideIdx + 1));
				}
				else step("Screech", "screech: on safe tile (cd) spot " + (screechHideIdx + 1));
				return;
			}
		}
		// Hiding right next to a safe tile (i.e. at that pillar) -> step ONTO it to get the clear shot.
		for (WorldPoint s : new WorldPoint[]{ safeW, safeE })
			if (s != null && me.distanceTo(s) <= 1)
			{
				hideWalk(s);
				step("Screech", "screech: -> safe tile " + s);
				return;
			}

		// Not at a safe-attack pillar -> slide to the stand tile nearest the NEXT spot (shortest onward step), no
		// settle-weave (those spots have no clear shot; they only attack via the short-trip weave between pillars).
		WorldPoint rest = hideSpotStandTile(screechHideIdx);
		if (rest != null && !me.equals(rest))
		{
			hideWalk(rest);
			step("Screech", "screech: readjust spot " + (screechHideIdx + 1) + " " + rest);
			return;
		}
		step("Screech", "screech: hiding spot " + (screechHideIdx + 1));
	}

	/** Walk toward {@code target}, but only CLICK when the destination changed from our last hide-walk
	 *  click -- re-sending the same tile every tick looks inhuman. The engine keeps walking to the last
	 *  clicked tile on its own, so one click per destination is enough. */
	private void hideWalk(WorldPoint target)
	{
		if (target == null || target.equals(lastHideClick)) return;
		router.walkTo(target);
		lastHideClick = target;
	}

	/**
	 * Tile gap from {@code from} to the Whisperer's FOOTPRINT (her 3x3 WorldArea), not her centre -- so a
	 * cast only needs &lt;8 tiles of clear gap to any of her tiles, regardless of which corner her anchor
	 * sits at. Chebyshev; 0 when adjacent/overlapping. {@link Integer#MAX_VALUE} if she's absent.
	 */
	private int whispererGap(WorldPoint from)
	{
		if (whisperer == null || from == null) return Integer.MAX_VALUE;
		net.runelite.api.coords.WorldArea area = whisperer.getWorldArea();
		return area == null ? Integer.MAX_VALUE : area.distanceTo(from);
	}

	/** True if the Whisperer's footprint is within weapon range of {@code from}, so an attack casts in
	 *  place rather than pathing us toward her (which would pull us off cover). */
	private boolean inAttackRange(WorldPoint from)
	{
		return whispererGap(from) <= MAX_ATTACK_RANGE;
	}

	/**
	 * Post-phase BIND avoidance + DPS weave. At the end of every special phase (seeds / screech / siphon) the
	 * Whisperer binds us and walks in; if her footprint reaches us we take a hit, so we open {@link #BIND_SAFE_GAP}
	 * tiles of gap. Each phase-end arms a {@link #bindFleeUntil} window. Per tick, in priority order:
	 * <ol>
	 *   <li>swap the main weapon back (siphon only, deferred so no walk overrides the wield) -- done first so
	 *       the run-out DPSes at the main weapon's full range;</li>
	 *   <li>once we're at a safe gap: attack + relock and end the window (normal DPS resumes);</li>
	 *   <li>still running out: weave an attack on each cooldown-ready tick (max DPS while we go), else step
	 *       toward the latched CARDINAL (never diagonal) flee tile.</li>
	 * </ol>
	 * Claims the tick throughout so the normal attack loop can't fight the run. Returns true while active.
	 */
	private boolean handlePhaseEndBind(int tick)
	{
		if (tick > bindFleeUntil)
		{
			bindFleeTarget = null;
			lastBindFleeClick = null;
			bindFleeStartTick = -100;   // window over -> re-latch on the next phase end
			return false;
		}
		// 1) SWAP the main weapon back FIRST, on its own tick (no walk to override the wield), and BEFORE the
		//    boss/realm guards below. The siphon clear arms this while we're still in the SHADOW realm, where
		//    findWhisperer() returns null -- gating the swap behind `whisperer != null` left us stuck on the
		//    Venator bow (the swap was never reached). Wielding is a pure inventory click, independent of the
		//    boss being in the scene; equipping claims the tick (returns true) so no walk cancels the wield.
		if (mainWeaponSwapPending)
		{
			equipMainWeapon();
			mainWeaponSwapPending = false;
			step("Phase-end bind", "phase-end: equip main weapon");
			return true;
		}

		WorldPoint me = mirror.getPlayerTile();
		if (me == null || whisperer == null) return false;   // (shadow realm has no boss) -- watch starts in ow

		// Latch the overworld start of the flee (first tick she could actually bind us), then decide whether a
		// bind is even coming. She does NOT always bind, so we start retreating immediately (safe), but if no
		// bind projectile (2467) shows within BIND_WATCH_TICKS of here, cancel the flee and resume DPS instead
		// of running out for nothing. A bind seen at/after the start (minus a small lookback for an early cast)
		// commits the full window.
		if (bindFleeStartTick < 0) bindFleeStartTick = tick;
		boolean bindSeen = lastBindProjTick >= bindFleeStartTick - BIND_LOOKBACK_TICKS;
		if (!bindSeen && tick - bindFleeStartTick >= BIND_WATCH_TICKS)
		{
			bindFleeUntil = tick;             // no bind this time -> end the window now
			bindFleeTarget = null;
			lastBindFleeClick = null;
			step("Phase-end bind", "phase-end: no bind -> resume DPS");
			return false;
		}

		int gap = whispererGap(me);

		// 2) SAFE (>= 10 gap) -> attack + relock and END the window; normal attack-maintenance resumes.
		if (gap >= BIND_SAFE_GAP)
		{
			attackWhisperer();
			lastAttackTick = tick;
			bindFleeTarget = null;
			lastBindFleeClick = null;
			bindFleeUntil = tick;
			step("Phase-end bind", "phase-end: safe -> attack + relock, resume DPS");
			return true;
		}

		// 3) RUNNING OUT: maximise DPS while we go -- attack on each cooldown-ready tick (the boss is within
		//    the main weapon's range while gap < 10, so the shot fires in place), otherwise step toward the
		//    latched CARDINAL flee tile. Attacking interrupts the walk, so we clear the dedupe to re-issue it.
		if (whispererGap(me) <= effectiveRange() && tick - lastAttackTick >= effectiveCooldown())
		{
			attackWhisperer();
			lastAttackTick = tick;
			lastBindFleeClick = null;
			step("Phase-end bind", "phase-end: run out + weave attack (gap " + gap + ")");
			return true;
		}
		if (bindFleeTarget == null || me.equals(bindFleeTarget)) bindFleeTarget = fleeTileFrom(me);
		if (bindFleeTarget != null && !bindFleeTarget.equals(lastBindFleeClick))
		{
			router.walkTo(bindFleeTarget);
			lastBindFleeClick = bindFleeTarget;
		}
		if (config != null && config.captureMode())
			log.info("[bindDbg] tick={} gap={} run(cardinal)->{}", tick, gap, bindFleeTarget);
		step("Phase-end bind", "phase-end: run out cardinal (gap " + gap + ")");
		return true;
	}

	/**
	 * A tile straight away from the Whisperer's footprint centre along a single CARDINAL axis (N/S/E/W) --
	 * NOT diagonal. We run on the axis we're already most offset on (so we head for open floor, away from
	 * her), ties and dead-centre (the siphon camp) defaulting to east. Far enough to clear {@link #BIND_SAFE_GAP}
	 * with a margin. Not arena-bounds clamped -- walkTo paths as far as the floor allows.
	 */
	private WorldPoint fleeTileFrom(WorldPoint me)
	{
		WorldPoint c = whispererCenterTile();
		if (c == null || me == null) return null;

		// SEED-FIRST retreat (repositionNorthActive is set only for the seed-end bind): lock to her centre
		// LANE (x = her centre x) and open the gap straight along it -- do NOT drift east/west. The generic
		// dominant-axis flee below runs laterally off the lane whenever the seed ring ended east/west of her,
		// which is what broke the centreline. We flee away from her in y (default south when level, matching
		// ending near the start tile); once the bind window ends, handleRepositionNorth dances us around to
		// her north side on this same lane.
		if (repositionNorthActive)
		{
			int dyAway = me.getY() > c.getY() ? 1 : -1;
			int n = BIND_SAFE_GAP - whispererGap(me) + 2;
			if (n < 2) n = 2;
			return new WorldPoint(c.getX(), me.getY() + dyAway * n, me.getPlane());
		}

		int ox = me.getX() - c.getX();
		int oy = me.getY() - c.getY();
		int dx = 0, dy = 0;
		if (Math.abs(ox) >= Math.abs(oy))
			dx = ox >= 0 ? 1 : -1;        // run east/west (default east when dead-centre)
		else
			dy = oy >= 0 ? 1 : -1;        // run north/south
		int need = BIND_SAFE_GAP - whispererGap(me) + 2;   // tiles to move out, with margin
		if (need < 2) need = 2;
		return new WorldPoint(me.getX() + dx * need, me.getY() + dy * need, me.getPlane());
	}

	/**
	 * DIAGNOSTIC (capture mode): log every NPC 12212 (shadow lost soul) with its tile in both coordinate
	 * spaces -- getWorldLocation (wl) and fromLocal(getLocalLocation) (fl) -- plus our own tile, so the
	 * run can be reconstructed into a diagram afterwards. Only prints when the set/positions change.
	 */
	private void logLostSouls(int tick)
	{
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return;
		StringBuilder sb = new StringBuilder();
		int n = 0;
		java.util.TreeSet<Integer> allIds = new java.util.TreeSet<>();   // every npc id present, for fallback
		WorldPoint me = mirror.getPlayerTile();
		for (NPC npc : npcs)
		{
			if (npc == null) continue;
			allIds.add(npc.getId());
			if (npc.getId() != WhispererAutoIDs.NPC_LOST_SOUL_SHADOW) continue;
			WorldPoint wl = npc.getWorldLocation();
			LocalPoint lp = npc.getLocalLocation();
			WorldPoint fl = lp == null ? null : WorldPoint.fromLocal(client, lp);
			if (n > 0) sb.append(" | ");
			sb.append("idx=").append(npc.getIndex()).append(" wl=").append(wl).append(" fl=").append(fl);
			n++;
		}
		String sig = n + ":" + sb + "::" + allIds;
		if (sig.equals(lastSoulSig)) return;   // unchanged -> don't spam
		lastSoulSig = sig;
		if (n > 0)
		{
			log.info("[soul12212] tick={} plane={} count={} me={} souls=[{}]", tick, currentPlane(), n, me, sb);
		}
		else
		{
			// No 12212 present this siphon tick -> dump the npc ids around so we can find the real soul id.
			log.info("[soul12212] tick={} plane={} count=0 me={} -- no 12212; npcIdsPresent={}",
				tick, currentPlane(), me, allIds);
		}
	}

	// -----------------------------------------------------------------
	// Soul siphon: clear the 4 corner groups with the Venator bow.
	// -----------------------------------------------------------------

	/**
	 * Drive the soul-siphon clear. Entry (equip Venator bow + activate the blackstone) is fired on the
	 * 10257 animation in {@link #onGameTick}; this runs once we're in the shadow realm, where the 12 lost
	 * souls (12212) ring the arena in 4 corner groups of 3. The clear:
	 * <ol>
	 *   <li>the Venator only reaches {@link #VENATOR_RANGE_TILES} tiles and the corners sit 6-8 from her
	 *       centre, so one tile can't cover all four -- we APPROACH the nearest un-fired group to within
	 *       range (her body blocks neither movement nor the shot), fire, then step to the next: a small orbit;</li>
	 *   <li>each Venator cycle ({@link #VENATOR_COOLDOWN_TICKS}), fire at the nearest un-fired group's valid
	 *       soul. The bow's bounce clears the other two in the group, so it's one shot per group.</li>
	 * </ol>
	 * Target validity per group: an <b>L</b> (non-collinear triple) lets us hit any soul; a <b>line</b>
	 * (collinear triple) only its two ENDS -- hitting the middle strands the far end out of bounce range.
	 * 4 groups x 4t = 16 ticks, inside the ~20-tick window. Returns true (claims the tick) while it runs.
	 */
	private boolean handleSiphonPhase(int tick)
	{
		if (!inSiphonPhase())
		{
			lastSiphonClick = null;
			return false;
		}
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return false;

		// CLEAR ENDED (4th Venator shot fired, or the backstop below): get back to the OVERWORLD, then hand off.
		// This must run BEFORE the pre-walk block -- once we re-enter the overworld the lingering ~20-tick siphon
		// timer would otherwise re-fire the camp-walk and trap the tick.
		if (siphonFleeArmed)
		{
			if (inShadowRealm())
			{
				// EXIT, mirroring the ENTRY (which equips the Venator bow AND clicks the blackstone in ONE tick):
				// re-wield the main weapon AND click the blackstone to cross back, SAME tick. equipMainWeapon no-ops
				// once the weapon is worn; clickBlackstone dedupes/retries until the realm actually flips, so
				// repeating this block each shadow tick is self-correcting and we land in the overworld already
				// holding the main weapon -- exactly the symmetric opposite of entry.
				equipMainWeapon();
				mainWeaponSwapPending = false;
				clickBlackstone(tick);
				step("Siphon", "siphon: cleared -> equip main weapon + blackstone");
				return true;
			}
			// BACK IN THE OVERWORLD: begin the flee THE MOMENT WE LAND (armed here, once -- the swing can't be
			// armed in the shadow realm, where whisperer==null wipes swingSpecial). handleSwing (runs before this)
			// then owns the flee south; the 10259 anim only anchors the bind window, it doesn't gate the flee.
			if (!siphonSwingArmed)
			{
				startSwing(Special.SIPHON, tick);
				siphonSwingArmed = true;
			}
			return false;   // hand off to handleSwing (already holding the main weapon)
		}

		// PRE-POSITION (overworld, before we cross): the Whisperer is always in the same spot, so her centre
		// tile is known the instant the siphon starts (10256). Walk to it NOW -- while we're still in the
		// overworld waiting on 10257 -- so crossing lands us central, a short step from the nearest group,
		// instead of burning shadow ticks closing in from the edge. Only the walk happens here; the clear
		// (approach -> fire -> next) runs in the shadow realm. We skip the walk on the cross tick
		// (siphonActionDone just latched when 10257 fired the blackstone activate) so that inventory click
		// wins, and we claim the tick throughout so maintainBossAttack can't re-click the boss and drag us
		// off the walk. (inSiphonPhase() implies whisperer != null, so whispererCenterTile() resolves.)
		if (!inShadowRealm())
		{
			if (!siphonActionDone)
			{
				WorldPoint camp = whispererCenterTile();
				if (camp != null && me.distanceTo(camp) > 0 && !camp.equals(lastSiphonClick))
				{
					router.walkTo(camp);
					lastSiphonClick = camp;
					step("Siphon", "siphon: pre-walk to camp -> " + camp);
				}
			}
			return true;
		}

		// END THE CLEAR when we have FIRED AT ALL 4 CORNER GROUPS (siphonClearedQuadrants gets a quadrant added each
		// time WE fire at a group, below). This is OUR shot count, NOT soul deaths, so we EARLY-EXIT the instant the
		// 4th group's shot is away -- the souls die on their own after we leave. We deliberately do NOT end on the
		// venator PROJECTILE count: the bow pierces/bounces, and those 2291 instances can land more than a cooldown
		// apart, inflating venatorShotCount() past 4 and ending the clear while groups are still un-fired (the
		// premature exit). We still collect the shot ticks for the capture log.
		for (MirrorState.ProjectileRecord pr : mirror.getProjectileRecords())
			if (pr.id == WhispererAutoIDs.PROJ_VENATOR_SHOT)
				venatorShotTicks.add(pr.startTick);
		if (siphonClearedQuadrants.size() >= 4)
		{
			mainWeaponSwapPending = true;
			startSwing(Special.SIPHON, tick);
			siphonFleeArmed = true;
			if (config != null && config.captureMode())
				log.info("[siphonDbg] all 4 groups fired -> end clear: blackstone back + swap + flee (tick={} quads={} shotTicks={} projShots={})",
					tick, siphonClearedQuadrants, venatorShotTicks, venatorShotCount());
			step("Siphon", "siphon: 4 groups fired -> blackstone back + swap weapon");
			return true;   // next tick the siphonFleeArmed block crosses us back to the overworld
		}

		// Latch the quadrant centre from the souls' own centroid (full ring = true centre) BEFORE the first
		// shot, then freeze it while we clear. Classifying against the Whisperer's tile scrambled the quadrants
		// in the shadow realm and ended the clear after a single group.
		if (siphonClearedQuadrants.isEmpty())
		{
			WorldPoint ctr = siphonSoulsCentroid();
			if (ctr != null) siphonCenter = ctr;
		}
		NPC target = pickSiphonTarget(me);
		// Fewer than 4 groups fired but no valid target this tick (a group's souls are mid-death / not yet targetable)
		// -> HOLD and keep trying. We do NOT end here -- that was ending with souls still alive. The 10259 end-anim
		// handler is the timer/whiff backstop that gets us out if the phase runs out before we clear 4.
		WorldPoint tt = target == null ? null : npcTile(target);
		if (tt == null) return true;

		// LAST CLUSTER safety: standing ON the boss (gap 0) is fine for the first three groups, but if the
		// FINAL group dies while we're under her we take a hit -- we need a gap of at least 1 when it dies.
		// So before firing the last group's shot, step off her footprint first (toward the target, keeping it
		// in range). Once we have the gap, the fire path below runs. (Direction will later be the preemptive
		// run direction, which depends on the phase order -- pending.)
		boolean lastGroup = siphonClearedQuadrants.size() >= 3;   // 3 cleared -> this 4th group is the last
		if (lastGroup && whispererGap(me) < 1)
		{
			WorldPoint off = offBossToward(me, tt);
			if (off != null && !off.equals(me) && !off.equals(lastSiphonClick))
			{
				router.walkTo(off);
				lastSiphonClick = off;
				step("Siphon", "siphon: last cluster -> step off boss first " + off);
			}
			return true;
		}

		boolean inRange = me.distanceTo(tt) <= VENATOR_RANGE_TILES;

		// IN RANGE + off cooldown -> SHOOT. (In range but on cooldown -> hold; don't wander off the spot.)
		if (inRange)
		{
			if (tick - lastAttackTick >= VENATOR_COOLDOWN_TICKS)
			{
				router.attackNpc(target);
				lastAttackTick = tick;
				lastSiphonClick = null;
				siphonClearedQuadrants.add(soulQuadrant(tt));   // don't re-fire this group while it dies
				if (config != null && config.captureMode())
					log.info("[siphonDbg] tick={} SHOOT {} quad={} cleared={}",
						tick, tt, soulQuadrant(tt), siphonClearedQuadrants);
				// If that was the 4th (final) group's shot, END THE CLEAR THIS TICK -- the shot is already away
				// (mid-air) and will kill its group after we leave, so we don't burn a tick waiting for the top-of-
				// method check next entry. Arm the swap + flee + blackstone cross now.
				if (siphonClearedQuadrants.size() >= 4)
				{
					mainWeaponSwapPending = true;
					startSwing(Special.SIPHON, tick);
					siphonFleeArmed = true;
					step("Siphon", "siphon: 4th shot away -> blackstone back + swap weapon");
					return true;
				}
				step("Siphon", "siphon: shoot soul " + tt);
			}
			return true;
		}

		// OUT OF RANGE: the Venator only reaches 6, so step toward this group's target until we're within it
		// (stop at the edge of range -- no overshoot, no attack-click drag). pickSiphonTarget picks the
		// nearest un-fired quadrant, so these approaches walk a small orbit between adjacent corners.
		WorldPoint approach = approachTile(me, tt, VENATOR_RANGE_TILES);
		if (approach != null && !approach.equals(me) && !approach.equals(lastSiphonClick))
		{
			router.walkTo(approach);
			lastSiphonClick = approach;
			step("Siphon", "siphon: approach soul " + tt + " (gap " + me.distanceTo(tt) + ")");
		}
		return true;
	}

	/**
	 * The nearest tile off the Whisperer's footprint (gap &gt;= 1), stepping from {@code me} toward
	 * {@code target} -- so it keeps the target in range while getting us out from under her. Returns
	 * {@code me} when we're already off her footprint. Used to clear the last siphon cluster without dying.
	 */
	private WorldPoint offBossToward(WorldPoint me, WorldPoint target)
	{
		if (whispererGap(me) >= 1) return me;
		int dx = Integer.signum(target.getX() - me.getX());
		int dy = Integer.signum(target.getY() - me.getY());
		if (dx == 0 && dy == 0) dx = 1;   // degenerate -> step east
		int x = me.getX(), y = me.getY();
		for (int k = 0; k < 4; k++)       // at most a couple tiles clears a 3x3
		{
			x += dx; y += dy;
			WorldPoint t = new WorldPoint(x, y, me.getPlane());
			if (whispererGap(t) >= 1) return t;
		}
		return new WorldPoint(x, y, me.getPlane());
	}

	/**
	 * A tile at the edge of {@code range} from {@code target}, stepping straight from {@code me} toward it
	 * (Chebyshev). Returns {@code me} unchanged when already within range. Lets us close just enough to fire
	 * without overshooting onto the target (which an attack-click would otherwise do).
	 */
	private WorldPoint approachTile(WorldPoint me, WorldPoint target, int range)
	{
		int gap = me.distanceTo(target);
		if (gap <= range) return me;
		int dx = Integer.signum(target.getX() - me.getX());
		int dy = Integer.signum(target.getY() - me.getY());
		int step = gap - range;
		return new WorldPoint(me.getX() + dx * step, me.getY() + dy * step, me.getPlane());
	}

	/** The Whisperer's 3x3 FOOTPRINT centre tile (SW base + half width/height), in the same space as
	 *  {@link MirrorState#getPlayerTile()} and the soul tiles. Null if she's absent. */
	private WorldPoint whispererCenterTile()
	{
		NPC w = whisperer;
		if (w == null) w = findNpcAny(WhispererAutoIDs.NPC_WHISPERER_BINDING);   // 12205 during a bind: same spot/footprint
		if (w == null) return null;
		net.runelite.api.coords.WorldArea a = w.getWorldArea();
		if (a == null) return null;
		return new WorldPoint(a.getX() + a.getWidth() / 2, a.getY() + a.getHeight() / 2, a.getPlane());
	}

	/** An NPC's tile via fromLocal(localLocation) -- the space that matches {@link MirrorState#getPlayerTile()}
	 *  and the Whisperer's WorldArea (proven by the tentacle dodge). Null if not in the loaded scene. */
	private WorldPoint npcTile(NPC npc)
	{
		if (npc == null) return null;
		LocalPoint lp = npc.getLocalLocation();
		return lp == null ? null : WorldPoint.fromLocal(client, lp);
	}

	/** Quadrant label (NE/NW/SE/SW) of a soul tile relative to the Whisperer's centre -- the 4 corner groups. */
	/** Centroid (average tile) of all lost souls (12212) present this tick, in the souls' own space, or null. */
	private WorldPoint siphonSoulsCentroid()
	{
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return null;
		long sx = 0, sy = 0; int n = 0;
		for (NPC npc : npcs)
		{
			if (npc == null || npc.getId() != WhispererAutoIDs.NPC_LOST_SOUL_SHADOW) continue;
			WorldPoint t = npcTile(npc);
			if (t == null) continue;
			sx += t.getX(); sy += t.getY(); n++;
		}
		if (n == 0) return null;
		return new WorldPoint((int) (sx / n), (int) (sy / n), currentPlane());
	}

	private String soulQuadrant(WorldPoint t)
	{
		WorldPoint c = siphonCenter != null ? siphonCenter : whispererCenterTile();
		if (c == null || t == null) return "?";
		return (t.getY() >= c.getY() ? "N" : "S") + (t.getX() >= c.getX() ? "E" : "W");
	}

	/**
	 * Pick the next soul to shoot: bucket the live 12212 souls into the 4 corner-group quadrants (skipping
	 * quadrants we've already fired into), choose the quadrant whose nearest soul is closest to us, then
	 * return that group's nearest VALID target ({@link #validSoulTargets}). Null when nothing's left to clear.
	 */
	private NPC pickSiphonTarget(WorldPoint me)
	{
		WorldPoint c = siphonCenter != null ? siphonCenter : whispererCenterTile();
		if (c == null) return null;
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return null;

		java.util.Map<String, List<NPC>> quads = new java.util.HashMap<>();
		for (NPC npc : npcs)
		{
			if (npc == null || npc.getId() != WhispererAutoIDs.NPC_LOST_SOUL_SHADOW) continue;
			WorldPoint t = npcTile(npc);
			if (t == null) continue;
			String q = soulQuadrant(t);
			if (siphonClearedQuadrants.contains(q)) continue;
			quads.computeIfAbsent(q, k -> new java.util.ArrayList<>()).add(npc);
		}
		if (quads.isEmpty()) return null;

		// Quadrant whose nearest soul is closest to us.
		List<NPC> group = null;
		int bestD = Integer.MAX_VALUE;
		for (List<NPC> g : quads.values())
		{
			for (NPC npc : g)
			{
				WorldPoint t = npcTile(npc);
				int d = t == null ? Integer.MAX_VALUE : me.distanceTo(t);
				if (d < bestD) { bestD = d; group = g; }
			}
		}
		if (group == null) return null;

		// Nearest VALID soul in that group, by TRUE (Euclidean) distance to the player -- for a line this is
		// the end closest to us. Chebyshev (me.distanceTo) ties the two diagonal ends and would keep the
		// first-added (SW-most) one regardless of where we stand, which sometimes picks the farther end.
		NPC best = null;
		long bd = Long.MAX_VALUE;
		for (NPC npc : validSoulTargets(group))
		{
			WorldPoint t = npcTile(npc);
			if (t == null) continue;
			long dx = (long) t.getX() - me.getX(), dy = (long) t.getY() - me.getY();
			long d2 = dx * dx + dy * dy;
			if (d2 < bd) { bd = d2; best = npc; }
		}
		return best;
	}

	/**
	 * The attackable souls in a group. A full triple is an <b>L</b> (any soul) or a <b>line</b> (the two
	 * ends only). We classify by the cross product of the three tiles: zero = collinear (line) -> exclude
	 * the middle (index 1 after sorting along the run); nonzero = L -> all three valid. A degenerate group
	 * (fewer than 3 souls, e.g. mid-death) just returns whatever's present.
	 */
	private List<NPC> validSoulTargets(List<NPC> group)
	{
		if (group.size() < 3) return new java.util.ArrayList<>(group);
		List<NPC> s = new java.util.ArrayList<>(group);
		s.sort((a, b) -> {
			WorldPoint pa = npcTile(a), pb = npcTile(b);
			if (pa.getX() != pb.getX()) return Integer.compare(pa.getX(), pb.getX());
			return Integer.compare(pa.getY(), pb.getY());
		});
		WorldPoint p0 = npcTile(s.get(0)), p1 = npcTile(s.get(1)), p2 = npcTile(s.get(2));
		long cross = (long) (p1.getX() - p0.getX()) * (p2.getY() - p0.getY())
				   - (long) (p1.getY() - p0.getY()) * (p2.getX() - p0.getX());
		List<NPC> out = new java.util.ArrayList<>();
		if (cross == 0)
		{
			out.add(s.get(0));   // end
			out.add(s.get(2));   // end (s.get(1) = the middle is never a valid target)
		}
		else
		{
			out.addAll(s);       // L: any soul works
		}
		return out;
	}

	/**
	 * Realm-crossing controller for the scout phases (seeds / screech). The phase logic sets
	 * {@link #desiredRealm} (a region id, or -1 for "no crossing wanted"); this crosses us there by
	 * activating the blackstone when, and only when, we're in the arena but the WRONG realm and the
	 * fragment is charged (28356). Because it no-ops once current == desired, the fragment staying
	 * charged in the shadow realm never ping-pongs us -- we sit and collect until the phase flips the
	 * target back. {@link #clickBlackstone} supplies the charged gate and dedupe. Siphon is NOT driven
	 * here (it crosses only after clearing adds, handled separately).
	 */
	private void serviceRealmCrossing(int tick)
	{
		if (desiredRealm < 0) return;
		int cur = currentPlane();
		if (cur == desiredRealm) return;                 // already in the target realm (plane)
		if (cur != WhispererAutoIDs.OVERWORLD_PLANE && cur != WhispererAutoIDs.SHADOW_REALM_PLANE)
			return;                                      // not on an arena plane / unknown -> don't click
		clickBlackstone(tick);                           // charged-gated + deduped
	}

	/**
	 * Activate the blackstone fragment to swap realms. Invokes its inventory "Activate" option
	 * (captured as CC_OP id=2 on inventory widget 149) via {@link ClickRouter#useInventoryItem}, which
	 * returns false if the item id isn't found in the inventory -- in which case we report failure
	 * (no spurious "activated" step) and DON'T consume the dedupe window, so we retry next tick. In
	 * capture mode we log whether it dispatched and how many 28357 we actually hold, to catch the
	 * fragment using a different id in its ready state. Returns true only if the click dispatched.
	 */
	private boolean clickBlackstone(int tick)
	{
		// Short dedupe (2 ticks): just long enough to avoid double-firing before the realm actually flips,
		// but short enough that the scout cross-BACK lands inside the fragment's ~3-tick activatable window
		// after the cross-in. serviceRealmCrossing no-ops once the realm matches, so this won't over-cross.
		if (tick - lastBlackstoneTick < 2) return false;
		// The fragment shows as 28356 in the overworld and 28357 in the shadow realm (capture: we hold
		// 28357 while in shadow). Activate whichever form we actually hold -- the CC_OP's itemId must
		// match the slot's item, which is why passing the wrong id silently no-op'd before.
		int c356 = mirror.countItem(WhispererAutoIDs.ITEM_BLACKSTONE_FRAGMENT);
		int c357 = mirror.countItem(WhispererAutoIDs.ITEM_BLACKSTONE_FRAGMENT_DRAINED);
		int id = c356 > 0 ? WhispererAutoIDs.ITEM_BLACKSTONE_FRAGMENT
			: (c357 > 0 ? WhispererAutoIDs.ITEM_BLACKSTONE_FRAGMENT_DRAINED : -1);
		if (id < 0)
		{
			if (config != null && config.captureMode())
				log.info("[blackstone] no fragment held (c28356=0 c28357=0)");
			return false;   // nothing to activate -> retry next tick
		}
		boolean dispatched = router.useInventoryItem(id, WhispererAutoIDs.BLACKSTONE_OPTION);
		if (config != null && config.captureMode())
			log.info("[blackstone] activate id={} dispatched={} c28356={} c28357={}", id, dispatched, c356, c357);
		if (!dispatched) return false;
		lastBlackstoneTick = tick;
		return true;
	}

	/**
	 * Chat hook. The "pulses with dark energy" line means the fragment just became charged (28356);
	 * we no longer drive crossings off it -- the charged item id gate plus {@link #desiredRealm} do --
	 * but we log it in capture mode as a useful phase marker.
	 */
	public void onChatMessage(String message)
	{
		if (message == null) return;
		String lower = message.toLowerCase();
		if (config != null && config.captureMode()
			&& lower.contains("pulses with dark energy"))
		{
			log.info("[blackstone] chat: fragment charged (pulses with dark energy)");
		}
		// ENRAGE trigger: "The Whisperer pulls you into the Shadow Realm..." marks the enrage phase. Latch it as
		// the authoritative signal (cleared at fight end). The enrage behavior keys off enragePhaseActive.
		if (lower.contains("pulls you into the shadow realm"))
		{
			if (!enragePhaseActive && overlay != null) step("Enrage", "ENRAGE phase triggered");
			enragePhaseActive = true;
			enrageSeen = true;
			// Enrage shares anim 10248 with seeds -> drop any seed phase it may have latched, and clear any pending
			// realm crossing so a scout doesn't ping-pong us out of the shadow realm we were just pulled into.
			seedPhaseActive = false;
			desiredRealm = -1;
			if (config != null && config.captureMode())
				log.info("[enrage] chat trigger -> enrage phase active: {}", message);
		}
		// BIND confirmation: "The Whisperer binds you in place" means we're rooted. While bound we can't move but
		// can still DPS -> the BOUND swing stage weaves attacks on the bind form (12205) when she's in range.
		if (lower.contains("binds you in place"))
		{
			boundInPlaceChatSeen = true;
			if (config != null && config.captureMode())
				log.info("[bind] chat: bound in place -> weave DPS while rooted");
		}
	}

	/** Current render plane of the player's world view, or -1. The realms are distinguished by PLANE:
	 *  overworld = {@link WhispererAutoIDs#OVERWORLD_PLANE} (0), shadow = {@link WhispererAutoIDs#SHADOW_REALM_PLANE}
	 *  (3). Plane is far more reliable than the instance->template region math, which can't be trusted
	 *  from the mirror's player tile (it reports in a non-standard space). */
	private int currentPlane()
	{
		return client.getTopLevelWorldView() == null ? -1 : client.getTopLevelWorldView().getPlane();
	}

	private boolean inShadowRealm() { return currentPlane() == WhispererAutoIDs.SHADOW_REALM_PLANE; }
	private boolean inOverworld()   { return currentPlane() == WhispererAutoIDs.OVERWORLD_PLANE; }

	// --- Realm coordinate translation (instance-safe) ---------------
	// The shadow (9571) and overworld (10595) arenas sit side by side; in STATIC template coords
	// the overworld is +REALM_X_OFFSET on X, same Y. The fight is instanced, so we translate
	// through template space: instance -> template -> shift X -> template -> instance.

	/** A shadow-realm instance tile -> its overworld-instance twin (clickable), or null. */
	private WorldPoint shadowToOverworld(WorldPoint shadowInstanceTile)
	{
		return shiftRealm(shadowInstanceTile, WhispererAutoIDs.REALM_X_OFFSET, WhispererAutoIDs.OVERWORLD_PLANE);
	}

	/**
	 * Translate an instance tile to the other realm's instance tile by shifting X by
	 * {@code dxTemplate} in template space, then pinning the result onto {@code targetPlane}
	 * (hardcoded per realm for now: OW 0 / shadow 3). The template lookup uses plane 0 because
	 * both arena templates are on plane 0 (the exported markers were z:0), even though the live
	 * shadow instance sits on plane 3. Returns null if either conversion fails (e.g. the target
	 * realm's chunk isn't part of this instance).
	 */
	private WorldPoint shiftRealm(WorldPoint instanceTile, int dxTemplate, int targetPlane)
	{
		if (instanceTile == null) return null;
		WorldPoint tpl = InstanceCoords.toTemplate(client, instanceTile);
		if (tpl == null) return null;
		WorldPoint shifted = new WorldPoint(tpl.getX() + dxTemplate, tpl.getY(), 0);  // template plane 0
		WorldPoint inst = InstanceCoords.fromTemplate(client, shifted);
		if (inst == null) return null;
		return new WorldPoint(inst.getX(), inst.getY(), targetPlane);
	}

	/**
	 * HP stage of a shadow-realm pillar (12209), decoded from its model-override model IDs
	 * (the devtools "M:" field): 100 / 40 / 20, or -1 if unknown. Ready for pillar handling --
	 * not wired to behavior yet.
	 */
	public int pillarHpStage(NPC pillar)
	{
		if (pillar == null) return -1;
		NpcOverrides mo = pillar.getModelOverrides();
		if (mo == null || mo.getModelIds() == null) return -1;
		for (int id : mo.getModelIds())
		{
			if (id == WhispererAutoIDs.PILLAR_MODEL_HP100) return 100;
			if (id == WhispererAutoIDs.PILLAR_MODEL_HP40)  return 40;
			if (id == WhispererAutoIDs.PILLAR_MODEL_HP20)  return 20;
		}
		return -1;
	}

	/**
	 * Hide-priority rank of a pillar from its HP stage: 20% -> 1, 40% -> 2, 100% -> 3; 0 if
	 * unknown. Lower rank hides first (ascending order during screech).
	 */
	public int pillarRank(NPC pillar)
	{
		switch (pillarHpStage(pillar))
		{
			case 20:  return 1;
			case 40:  return 2;
			case 100: return 3;
			default:  return 0;
		}
	}

	/**
	 * True while the Whisperer is in her screech phase: within SCREECH_GRACE_TICKS of the last
	 * screech animation (charge 10251 / attack 10252 / cooldown 10250). The pillar overlay is
	 * shown for this whole window and disappears once she leaves it.
	 */
	public boolean inScreechPhase()
	{
		return whisperer != null
			&& mirror.getTickCounter() - lastScreechAnimTick <= WhispererAutoIDs.SCREECH_GRACE_TICKS;
	}

	/**
	 * True while the Whisperer is in her soul-siphon phase: within the siphon duration of the last
	 * siphon animation (start 10256 / active 10257). Used (for now) only to keep prayers off through it.
	 */
	public boolean inSiphonPhase()
	{
		return whisperer != null
			&& mirror.getTickCounter() - lastSiphonAnimTick <= WhispererAutoIDs.SOUL_SIPHON_DURATION_TICKS;
	}

	/** Number of distinct Venator SHOTS from the collected loose-ticks: walk them ascending and start a new shot
	 *  only when the tick is >= {@link #VENATOR_COOLDOWN_TICKS} after the last shot's tick, so a
	 *  pierce's several same-shot instances (same / near tick) collapse to one while the 4 real shots (>= a
	 *  cooldown apart) each count. */
	private int venatorShotCount()
	{
		int shots = 0, lastTick = Integer.MIN_VALUE;
		for (int t : venatorShotTicks)
		{
			if (t - lastTick >= VENATOR_COOLDOWN_TICKS) { shots++; lastTick = t; }
		}
		return shots;
	}

	/** Ordered seed path for the overlay (nearest-neighbour walk order), or the raw capture-order route
	 *  if the path hasn't been built yet. Read-only; may be empty. */
	public java.util.List<WorldPoint> seedRouteTiles() { return seedPath.isEmpty() ? seedRouteOw : seedPath; }

	/** Index of the next seed to step on in {@link #seedRouteTiles()}; tiles before it are done. */
	public int seedWalkIndex() { return seedPath.isEmpty() ? 0 : seedWalkIdx; }

	/**
	 * Order the captured route into a walkable RING path: sort the seeds by angle around their
	 * centroid (so angularly-adjacent ring seeds are list-adjacent -- no nearest-neighbour
	 * backtracking where a slight detour like "14" gets skipped then revisited), start at the seed
	 * nearest where we re-entered the overworld, and go around in the configured rotation direction.
	 * This matches how a human walks the ring continuously in one sweep.
	 */
	private void buildSeedPath(WorldPoint from)
	{
		seedPath.clear();
		seedWalkIdx = 0;
		lastSeedClick = null;
		java.util.List<WorldPoint> tiles = new java.util.ArrayList<>(seedRouteOw);
		int n = tiles.size();
		if (n == 0) return;

		// Ring centroid, then order by angle around it.
		double sx = 0, sy = 0;
		for (WorldPoint t : tiles) { sx += t.getX(); sy += t.getY(); }
		final double cx = sx / n, cy = sy / n;
		tiles.sort((a, b) -> Double.compare(
			Math.atan2(a.getY() - cy, a.getX() - cx),
			Math.atan2(b.getY() - cy, b.getX() - cx)));

		// Enter the ring at the seed nearest where we re-entered the overworld.
		int start = 0, bd = Integer.MAX_VALUE;
		if (from != null)
			for (int i = 0; i < n; i++)
			{
				int d = cheby(from, tiles.get(i));
				if (d < bd) { bd = d; start = i; }
			}

		// Sweep around the ring from there, in the configured rotation direction.
		boolean cw = config == null || config.seedClockwise();
		for (int k = 0; k < n; k++)
		{
			int idx = cw ? (((start - k) % n) + n) % n : (start + k) % n;
			seedPath.add(tiles.get(idx));
		}
	}

	/**
	 * Click-skip optimization: return the index of the FARTHEST seed we can click so the engine, walking from
	 * where we ACTUALLY ARE ({@code me}), lands on every good seed in between. The engine moves one tile per
	 * tick toward the target (diagonally while both axes differ, then straight), so it only steps onto an
	 * intermediate seed when the whole run lies on ONE PURE line -- orthogonal (one axis fixed) or an exact
	 * 45-degree diagonal (|dx| == |dy|) -- anchored at {@code me}.
	 * <p>
	 * Anchoring at {@code me} (not at {@code seedPath[start]}) is the key: the collapse is only valid along the
	 * line we're really on. Coming onto the ring from the side, {@code me} is NOT on the seed1->seed2 line, so
	 * we click seed1 directly instead of walking past it to seed2 and MISSING seed1. At a corner the direction
	 * changes, so the run stops there. A shallow/zigzag hop (|dx| != |dy|, not axis-aligned) is never collapsed
	 * -- the engine's diagonal-first path would skip it -- so there we click the next seed directly.
	 * <p>
	 * RUN-OVERSHOOT (the second half of "take our start position into account"): with run ON the engine moves 2
	 * tiles/tick, so walking toward a FAR clicked target it only LANDS on tiles an EVEN Chebyshev distance from
	 * where the leg began ({@code me}); an odd-distance seed in between is stepped OVER (we land past it, then
	 * have to walk back). A CLICKED destination is the exception -- the engine stops exactly on it (a 1-tile
	 * final step). So we may collapse PAST a seed only when that seed sits at an even distance from {@code me};
	 * the first odd-distance seed becomes the click target (landed exactly) and we cannot collapse beyond it.
	 * Walking (run off, 1 tile/tick) lands on every tile, so the parity gate is skipped there.
	 */
	private int straightRunEnd(int start, WorldPoint me)
	{
		int n = seedPath.size();
		if (start < 0 || start >= n || me == null) return start;
		int tpt = tilesPerTick();   // 2 = run on: the engine only LANDS on tiles an even distance from me
		int udx = 0, udy = 0;       // latched pure direction from me; (0,0) until the first seed we aren't on
		int end = start;
		boolean canPassEnd = true;  // may we collapse PAST the current `end`? (it sits on a landing tile)
		for (int i = start; i < n; i++)
		{
			WorldPoint c = seedPath.get(i);
			if (c == null) break;
			int ddx = c.getX() - me.getX();
			int ddy = c.getY() - me.getY();
			if (ddx == 0 && ddy == 0) { end = i; canPassEnd = true; continue; }   // standing on it (dist 0 = even)
			// On a pure line from me? (orthogonal or exact 45-degree diagonal)
			if (!(ddx == 0 || ddy == 0 || Math.abs(ddx) == Math.abs(ddy))) break;
			int sdx = Integer.signum(ddx), sdy = Integer.signum(ddy);
			if (udx == 0 && udy == 0) { udx = sdx; udy = sdy; }         // latch the run's direction
			else if (sdx != udx || sdy != udy) break;                   // corner / different line -> stop
			// To advance the click target onto seed i, the PREVIOUS target seed becomes an intermediate the run
			// must physically land on. If run is on and that seed sat at an odd distance, it gets overshot -> stop
			// here and click the previous seed instead (it lands exactly as a clicked destination).
			if (i > start && !canPassEnd) break;
			end = i;
			int dist = Math.max(Math.abs(ddx), Math.abs(ddy));   // Chebyshev distance from me along the line
			canPassEnd = (tpt <= 1) || (dist % 2 == 0);          // walking lands on every tile; running needs even
		}
		return end;
	}

	/** All shadow-realm pillar NPCs (12209) currently in the scene. */
	public java.util.List<NPC> shadowPillars()
	{
		java.util.List<NPC> out = new java.util.ArrayList<>();
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return out;
		for (NPC n : npcs)
		{
			// Skip dead/dying pillars so their label + square drop as soon as the NPC goes.
			if (n != null && n.getId() == WhispererAutoIDs.NPC_PILLAR_SHADOW && !n.isDead()) out.add(n);
		}
		return out;
	}

	/** Every live overworld-pillar (12210) footprint tile, in player-tile (instance) space. The columns are
	 *  NPCs, so they may not be in the scene collision / LoS flags -- we inject these as the attack predictor's
	 *  movement + LoS blockers (harmless if the flags already carry them). */
	private java.util.Set<WorldPoint> overworldPillarTiles()
	{
		java.util.Set<WorldPoint> out = new java.util.HashSet<>();
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return out;
		for (NPC n : npcs)
		{
			if (n == null || n.getId() != WhispererAutoIDs.NPC_PILLAR_OVERWORLD || n.isDead()) continue;
			net.runelite.api.coords.WorldArea a = n.getWorldArea();
			if (a == null) continue;
			for (int x = a.getX(); x < a.getX() + a.getWidth(); x++)
				for (int y = a.getY(); y < a.getY() + a.getHeight(); y++)
					out.add(new WorldPoint(x, y, a.getPlane()));
		}
		return out;
	}

	/**
	 * Predict whether attacking the Whisperer from {@code from} would pull us in, and exactly which tile we'd
	 * land attacking from -- accounting for run-overshoot (2 tiles/tick running past the range edge) and the
	 * column obstacles (columns block movement + LoS, she blocks neither). Null if the inputs don't resolve.
	 * Diagnostic for now; once validated it gates whether we weave an attack during screech.
	 */
	private net.runelite.client.plugins.autoBossCore.combat.AttackPredictor.Result predictBossAttack(WorldPoint from)
	{
		if (whisperer == null || from == null) return null;
		net.runelite.api.coords.WorldArea target = whisperer.getWorldArea();
		if (target == null) return null;
		java.util.Set<WorldPoint> cols = overworldPillarTiles();
		java.util.function.Predicate<WorldPoint> blocks = cols::contains;
		return net.runelite.client.plugins.autoBossCore.combat.AttackPredictor.predict(
			client.getTopLevelWorldView(), from, target, effectiveRange(), tilesPerTick(),
			blocks, blocks, null, 400);
	}

	/** Our live movement speed: 2 tiles/tick when run is ON and we have energy, else 1 (a toggled-on run with
	 *  0 energy still walks). Used for the attack-pull overshoot and the reach-cover-in-time budget. */
	private int tilesPerTick()
	{
		return (client.getVarpValue(net.runelite.api.gameval.VarPlayerID.OPTION_RUN) == 1
			&& mirror.getRunEnergy() > 0) ? 2 : 1;
	}

	/** Ticks to walk from {@code a} to {@code b} AROUND the columns at {@code tpt} tiles/tick, via the same
	 *  collision BFS; a large sentinel if unreachable within the budget. Used to check we can get back to cover
	 *  after an attack drag. */
	private int walkTicksAround(WorldPoint a, WorldPoint b, int tpt)
	{
		if (a == null || b == null) return Integer.MAX_VALUE / 4;
		if (a.equals(b)) return 0;
		java.util.Set<WorldPoint> cols = overworldPillarTiles();
		// TilePathPredictor's predicate is "true == WALKABLE" -> a tile is walkable iff it's NOT a column.
		java.util.function.Predicate<WorldPoint> walkable = p -> !cols.contains(p);
		java.util.List<WorldPoint> path = net.runelite.client.plugins.autoBossCore.combat.TilePathPredictor.path(
			client.getTopLevelWorldView(), a, b, walkable, null, 400);
		if (path.isEmpty()) return Integer.MAX_VALUE / 4;
		int tiles = path.size() - 1, step = Math.max(1, tpt);
		return (tiles + step - 1) / step;
	}

	/**
	 * The pillars to hide behind, as a nearest-neighbour CHAIN anchored on the single rank-3 pillar:
	 * the rank-3 (100%), then the rank-2 (40%) nearest it, then the rank-1 (20%) nearest THAT rank-2.
	 * We hide 1 -> 2 -> 3, so chaining by proximity keeps that run as short as possible. Ties in that
	 * proximity are broken toward the 100% core so we take the pillar INSIDE the cluster, not an edge one
	 * (see {@link #nearestPillarOfRank}). Only these
	 * chosen pillars are returned (in [rank3, rank2, rank1] order); every other pillar is omitted so
	 * the overlay never shows a pillar we won't use. The chain stops early if a rank is missing, so
	 * the result may hold 1-3 pillars (empty only when no rank-3 is present).
	 */
	public java.util.List<NPC> hideChainPillars()
	{
		java.util.List<NPC> pillars = shadowPillars();
		java.util.List<NPC> chain = new java.util.ArrayList<>();

		NPC r3 = null;
		for (NPC p : pillars) if (pillarRank(p) == 3) { r3 = p; break; }   // single rank-3 anchor
		if (r3 == null) return chain;

		// Entry hide (rank-1) first, then the rank-2 BETWEEN it and the rank-3. We hide 1 -> 2 -> 3, so the
		// rank-2 must be reachable from the rank-1 in one screech window AND still lead to the rank-3. With
		// several rank-2 pillars (e.g. a 1,2,2,2,3 column) the old "rank-2 nearest the rank-3" pick lands two
		// hops from the rank-1 -- unreachable in time. middlePillarOfRank takes the one that minimises the
		// LONGEST of the two hops, i.e. the middle pillar a human would use. (rank-1 picked before rank-2 so it
		// can anchor that choice; if several rank-1, the one nearest the core.)
		NPC r1 = nearestPillarOfRank(pillars, 1, r3, r3);
		NPC r2 = middlePillarOfRank(pillars, 2, r1, r3);

		chain.add(r3);
		if (r2 == null) return chain;
		chain.add(r2);
		if (r1 != null) chain.add(r1);
		return chain;
	}

	/**
	 * Pillar of {@code rank} best positioned BETWEEN hide spots {@code prev} and {@code next}: minimise the
	 * LONGER of the two hops (Chebyshev), so both prev->this and this->next fit the screech move window. For a
	 * line of candidates this is the middle one. Ties break toward the smaller total, then toward {@code prev}
	 * (reachability from the spot we come FROM wins). {@code prev} null -> fall back to nearest {@code next}.
	 */
	private NPC middlePillarOfRank(java.util.List<NPC> pillars, int rank, NPC prev, NPC next)
	{
		WorldPoint a = prev == null ? null : prev.getWorldLocation();
		WorldPoint b = next == null ? null : next.getWorldLocation();
		NPC best = null;
		int bestMax = Integer.MAX_VALUE, bestSum = Integer.MAX_VALUE, bestToPrev = Integer.MAX_VALUE;
		for (NPC p : pillars)
		{
			if (pillarRank(p) != rank) continue;
			WorldPoint w = p.getWorldLocation();
			if (w == null) continue;
			int da = a == null ? 0 : cheby(w, a);
			int db = b == null ? 0 : cheby(w, b);
			int mx = Math.max(da, db), sum = da + db;
			if (mx < bestMax
				|| (mx == bestMax && sum < bestSum)
				|| (mx == bestMax && sum == bestSum && da < bestToPrev))
			{
				bestMax = mx; bestSum = sum; bestToPrev = da; best = p;
			}
		}
		return best;
	}

	/**
	 * Latch the pillar hide chain ONCE at screech start and hold it stable through the phase -- pillar
	 * HP shifts (a 40% dropping to 20% as she attacks) do NOT recompute it, so the overlay never
	 * flickers or disappears. Stored in hide order [1=20%, 2=40%, 3=100%]. Cleared when screech ends.
	 * We only latch a FULL chain (all three ranks present), and keep retrying until one is available.
	 */
	private void updateHideChain()
	{
		if (!inScreechPhase())
		{
			hidePillarChain.clear();
			hidePillarChainShadowTiles.clear();
			hidePillarChainOwTiles.clear();
			screechHideIdx = 0;
			screechMoveStartIdx = -1;
			screechHideArrived = false;
			screechPillarSeen = false;
			screechHidePillarNpc = null;
			lastHideClick = null;
			return;
		}
		if (hidePillarChain.isEmpty())                       // latch the chain ONCE this screech
		{
			java.util.List<NPC> chain = hideChainPillars(); // returns [rank3, rank2, rank1] when full
			if (chain.size() == 3)
			{
				hidePillarChain.add(chain.get(2));  // 1 = rank-1 (20%)
				hidePillarChain.add(chain.get(1));  // 2 = rank-2 (40%)
				hidePillarChain.add(chain.get(0));  // 3 = rank-3 (100%)
				// Snapshot the pillar tiles (SW of each 2x2) in shadow coords.
				for (NPC p : hidePillarChain)
					hidePillarChainShadowTiles.add(p == null ? null : p.getWorldLocation());
				// Fresh hide-walk at the start of this screech.
				screechHideIdx = 0;
				screechMoveStartIdx = -1;
				screechHideArrived = false;
				screechPillarSeen = false;
				screechHidePillarNpc = null;
				lastHideClick = null;
			}
		}
		// Refresh the OVERWORLD safespot cache ONLY from the shadow realm, where translating a shadow tile
		// is reliable. In the overworld hideChainSafespots() serves this frozen copy (translating from the
		// overworld drifts). This is the same translate-from-shadow-then-render approach the seeds use.
		if (inShadowRealm() && !hidePillarChainShadowTiles.isEmpty())
		{
			hidePillarChainOwTiles.clear();
			for (WorldPoint sw : hidePillarChainShadowTiles)
			{
				WorldPoint safe = sw == null ? null : new WorldPoint(sw.getX(), sw.getY() + 2, sw.getPlane());
				hidePillarChainOwTiles.add(safe == null ? null : shadowToOverworld(safe));
			}
		}
	}

	/** The latched hide chain in hide order [1,2,3], for the overlay. May hold dead pillars (the
	 *  overlay skips them, so 2 and 3 persist after 1 is gone). */
	public java.util.List<NPC> hidePillarChainTiles() { return hidePillarChain; }

	/**
	 * Safespot anchor tiles for the latched hide chain, resolved for the CURRENT realm -- the SW tile of
	 * the 2x2 safespot directly NORTH of each hide pillar (+2 Y), translated shadow->overworld when we're
	 * in the overworld (same cross-realm mapping as the seeds) so the overlay draws the plan in BOTH
	 * realms. Hide order [1,2,3]; entries may be null (a pillar that didn't translate). Empty until the
	 * chain is latched / outside screech.
	 */
	public java.util.List<WorldPoint> hideChainSafespots()
	{
		// Overworld: serve the frozen cache translated from the shadow side (see updateHideChain) -- do
		// NOT translate here, that drifts. Shadow realm: build live from the latched pillar tiles (+2 Y).
		if (inOverworld()) return hidePillarChainOwTiles;
		java.util.List<WorldPoint> out = new java.util.ArrayList<>();
		for (WorldPoint sw : hidePillarChainShadowTiles)
			out.add(sw == null ? null : new WorldPoint(sw.getX(), sw.getY() + 2, sw.getPlane()));
		return out;
	}

	/**
	 * The exact tile to STAND on inside hide-spot {@code i}'s 2x2 safespot: the sub-tile closest to the
	 * NEXT hide spot, so the step to the next spot is as short as possible (e.g. spot 2 to the east ->
	 * stand on spot 1's eastern tile). For the final spot (no next) we aim toward the PREVIOUS spot -- the
	 * tile we arrive on. Resolved for the current realm via {@link #hideChainSafespots()}. Null if the
	 * safespots aren't available or spot {@code i}'s anchor is missing.
	 */
	public WorldPoint hideSpotStandTile(int i)
	{
		java.util.List<WorldPoint> spots = hideChainSafespots();
		if (spots == null || i < 0 || i >= spots.size()) return null;
		WorldPoint sw = spots.get(i);
		if (sw == null) return null;

		// Reference = the NEXT spot if there is one, else the PREVIOUS spot (so the last spot faces the
		// direction we came from). The 2x2 is [sw .. sw+1] on both axes; pick the sub-tile nearest ref.
		WorldPoint ref = (i + 1 < spots.size()) ? spots.get(i + 1) : null;
		if (ref == null && i - 1 >= 0) ref = spots.get(i - 1);
		if (ref == null) return sw;   // single spot -> no preferred corner

		return closestSubTile(sw, ref);
	}

	/** The sub-tile of the 2x2 anchored at {@code sw} (tiles [sw .. sw+1] on both axes) nearest {@code to}
	 *  by Chebyshev distance. Used both to slide toward the next spot and to run to the nearest cover. */
	private WorldPoint closestSubTile(WorldPoint sw, WorldPoint to)
	{
		if (sw == null) return null;
		if (to == null) return sw;
		WorldPoint best = sw;
		int bestD = Integer.MAX_VALUE;
		for (int dx = 0; dx <= 1; dx++)
			for (int dy = 0; dy <= 1; dy++)
			{
				WorldPoint t = new WorldPoint(sw.getX() + dx, sw.getY() + dy, sw.getPlane());
				int d = cheby(t, to);
				if (d < bestD) { bestD = d; best = t; }
			}
		return best;
	}

	/**
	 * The live OVERWORLD pillar NPC (12210) for hide spot {@code i}: the 12210 nearest that spot's pillar
	 * position (the safespot is 2 tiles NORTH of its pillar, so the pillar SW is the safespot SW - 2 Y).
	 * NPC positions come through {@code WorldPoint.fromLocal} (the same space as {@code getPlayerTile} and
	 * the translated safespots). Null if none is within a tile or two -- i.e. it despawned (was consumed).
	 */
	private NPC overworldHidePillar(int i)
	{
		java.util.List<WorldPoint> spots = hideChainSafespots();
		if (spots == null || i < 0 || i >= spots.size() || spots.get(i) == null) return null;
		WorldPoint sw = spots.get(i);
		WorldPoint pillar = new WorldPoint(sw.getX(), sw.getY() - 2, sw.getPlane());  // pillar SW
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return null;
		NPC best = null;
		int bestD = Integer.MAX_VALUE;
		for (NPC n : npcs)
		{
			if (n == null || n.getId() != WhispererAutoIDs.NPC_PILLAR_OVERWORLD) continue;
			LocalPoint lp = n.getLocalLocation();
			if (lp == null) continue;
			WorldPoint w = WorldPoint.fromLocal(client, lp);
			if (w == null) continue;
			int d = cheby(w, pillar);
			if (d < bestD) { bestD = d; best = n; }
		}
		return bestD <= 3 ? best : null;   // only accept a pillar actually at this spot
	}

	/**
	 * Pillar of the given hide-rank whose tile is nearest {@code to} (Chebyshev) -- the PRIMARY metric
	 * (least distance to traverse to the adjacent hide spot), or null. On a TIE in that distance, break
	 * it toward {@code core} (the single 100% rank-3 anchor): prefer the candidate nearer the core, i.e.
	 * the one INSIDE the cluster rather than one hanging off the edge. So for pillars 1,3,1,2,1 the two
	 * 20% pillars flanking the 40% are equidistant from it, and we take the inner one (-> 3,1,2) instead
	 * of the outer one (-> 3,2,1). Pass core == to for the rank-2 pick (degenerate tiebreak).
	 */
	private NPC nearestPillarOfRank(java.util.List<NPC> pillars, int rank, NPC to, NPC core)
	{
		WorldPoint t = to == null ? null : to.getWorldLocation();
		if (t == null) return null;
		WorldPoint c = core == null ? null : core.getWorldLocation();
		NPC best = null;
		int bestD = Integer.MAX_VALUE;
		int bestCoreD = Integer.MAX_VALUE;
		for (NPC p : pillars)
		{
			if (pillarRank(p) != rank) continue;
			WorldPoint w = p.getWorldLocation();
			if (w == null) continue;
			int d = cheby(w, t);
			int coreD = c == null ? 0 : cheby(w, c);
			if (d < bestD || (d == bestD && coreD < bestCoreD))
			{
				bestD = d; bestCoreD = coreD; best = p;
			}
		}
		return best;
	}

	private static int cheby(WorldPoint a, WorldPoint b)
	{
		return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
	}

	/** The 8 surrounding tile offsets (orthogonals first, then diagonals). */
	private static final int[][] NEIGHBORS8 = {
		{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
	};

	/**
	 * Tentacle escape (PHASE 1), driven by the tentacle NPCs (12208), which are visible on spawn.
	 * The tentacles spawn a few tiles out (NOT adjacent) and attack INWARD toward us, so the dodge
	 * is decided by which compass direction each tentacle sits in: tentacles on the CARDINALS
	 * (N/E/S/W) -> step to a DIAGONAL neighbour; tentacles on the DIAGONALS -> step to a CARDINAL
	 * neighbour (the deterministic rule from the in-game spawn patterns).
	 * <p>
	 * TIMING (DPS weave): tentacles spawn (t0), the attack strikes (~t2) and lands (~t3); the strike
	 * locks onto our tile, so we must be off BY t2 -- stepping on t1 both dodges and lets us weave an
	 * attack on t0. The hold is {@link #effectiveTentacleDelay()} (config tentacleDodgeDelay, default
	 * 1) ticks after we first detect the spawn ({@link #tentacleThreatTick}); we step once per spawn
	 * ({@link #tentacleDodged}), and both latches reset the moment no tentacle is in the scene so the
	 * next spawn re-arms cleanly. Returns the tile to step to, or null (hold / keep attacking).
	 */
	/** The 4 enrage safe tiles, anchored to {@code a} (our entry tile) via {@link #ENRAGE_OFFSETS}. Plane is taken
	 *  from {@link #currentPlane()} (the reliable render plane -- 3 in the shadow realm), NOT the anchor's stored
	 *  plane: getPlayerTile()'s plane is unreliable and reads 0, which would put the tiles (and the walk click) on
	 *  the wrong plane. */
	private WorldPoint[] enrageFormationTiles(WorldPoint a)
	{
		int plane = currentPlane();
		WorldPoint[] out = new WorldPoint[ENRAGE_OFFSETS.length];
		for (int i = 0; i < ENRAGE_OFFSETS.length; i++)
			out[i] = new WorldPoint(a.getX() + ENRAGE_OFFSETS[i][0], a.getY() + ENRAGE_OFFSETS[i][1], plane);
		return out;
	}

	/** A formation tile is unsafe to hop to if a tentacle (12208) is on or adjacent to it, or it lies on a
	 *  tentacle->player hazard line. */
	private boolean enrageTileSafe(WorldPoint tile, List<NPC> npcs)
	{
		if (dodgeHazards.contains(tile)) return false;   // on a tentacle->player hazard line
		if (npcs == null) return true;
		for (NPC n : npcs)
		{
			if (n == null || n.getId() != WhispererAutoIDs.NPC_TENTACLE) continue;
			net.runelite.api.coords.WorldArea ta = n.getWorldArea();
			if (ta != null && ta.distanceTo(tile) <= 1) return false;
		}
		return true;
	}

	/**
	 * ENRAGE dodge. No specials -- only tentacles, spawning ~every 2 ticks CONTINUOUSLY (they overlap, so the
	 * normal once-per-spawn / tentCount==0 re-arm never fires). DODGE IS THE PRIORITY: while any tentacle is up we
	 * hop on a fixed {@link #ENRAGE_STEP_PERIOD}-tick cadence to the NEAREST safe tile of the 4-tile formation
	 * (anchored to our enrage entry tile), and only the IN-BETWEEN ticks are left for the attack loop. That stops a
	 * slow weapon (Shadow) from standing an extra tick to squeeze a hit and eating a tentacle. Returns the tile to
	 * move to, or null (hold -> DPS this tick).
	 */
	private WorldPoint planEnrageDodge(int tick)
	{
		// Enrage lives in the SHADOW realm (plane 3). Don't anchor or dodge until we're actually there: anchoring on
		// the pre-pull OVERWORLD tile would put the whole formation in the wrong region, and the plane would be wrong.
		if (!inShadowRealm()) { enrageAnchor = null; lastEnrageStepTick = -100; return null; }
		WorldPoint raw = mirror.getPlayerTile();
		if (raw == null) return null;
		// getPlayerTile()'s plane is unreliable (reads 0 in the shadow realm); use the real render plane so me and the
		// formation tiles are on the SAME plane -- otherwise WorldPoint.distanceTo/equals treat them as unreachable.
		WorldPoint me = new WorldPoint(raw.getX(), raw.getY(), currentPlane());
		if (enrageAnchor == null) enrageAnchor = me;   // anchor the formation to our entry tile, once

		List<NPC> npcs = client.getNpcs();
		dodgeHazards = hazardTiles(me, npcs);          // avoid the tentacle->player line tiles
		int tentCount = 0;
		if (npcs != null)
			for (NPC n : npcs) if (n != null && n.getId() == WhispererAutoIDs.NPC_TENTACLE) tentCount++;
		if (tentCount == 0) { lastEnrageStepTick = -100; return null; }           // no threat -> DPS, reset cadence
		if (lastEnrageStepTick >= 0 && tick - lastEnrageStepTick < ENRAGE_STEP_PERIOD) return null;   // between hops -> DPS

		// COOLDOWN-AWARE pick among the safe formation tiles. The tiles sit at knight-move distance (2 = ~1 tick
		// with run) or the long skip 1<->3 / 2<->4 (3 = ~2 ticks). ticksUntilReady = ticks until the weapon can fire
		// again. When it's ready now / next tick, take the SHORTEST hop so we settle and attack ASAP; while it's
		// still on cooldown (an attack this slot is wasted anyway), prefer the move whose travel best FILLS the
		// remaining cooldown -- i.e. the 2-tick skip move -- so we arrive right as the weapon comes up instead of
		// idling. Dodge still happens every cadence regardless; this only chooses WHICH safe tile.
		WorldPoint[] formation = enrageFormationTiles(enrageAnchor);
		int tpt = Math.max(1, tilesPerTick());
		int ticksUntilReady = whisperer == null ? 0 : Math.max(0, effectiveCooldown() - (tick - lastAttackTick));
		WorldPoint best = null; double bestCost = Double.MAX_VALUE; int bestDist = Integer.MAX_VALUE;
		for (WorldPoint t : formation)
		{
			if (t.equals(me) || !enrageTileSafe(t, npcs)) continue;
			int d = me.distanceTo(t);
			int travel = Math.max(1, (d + tpt - 1) / tpt);
			double cost = (ticksUntilReady <= 1) ? travel : Math.abs(travel - ticksUntilReady);
			if (cost < bestCost || (cost == bestCost && d < bestDist)) { bestCost = cost; bestDist = d; best = t; }
		}
		// All safe tiles blocked -> still vacate our current tile (the guaranteed incoming hit): nearest other tile.
		if (best == null)
			for (WorldPoint t : formation)
			{
				if (t.equals(me)) continue;
				int d = me.distanceTo(t);
				if (d < bestDist) { bestDist = d; best = t; }
			}
		if (config != null && config.captureMode())
			log.info("[enrageDbg] me={} anchor={} tents={} cdLeft={} -> {} (d={})",
				me, enrageAnchor, tentCount, ticksUntilReady, best, best == null ? -1 : me.distanceTo(best));
		if (best != null) lastEnrageStepTick = tick;
		return best;
	}

	/** Any tentacle NPC (12208) currently in the scene. While one is up the tentacle DODGE owns movement: the
	 *  straight walks in the swing maneuver / recentre ignore tentacle directions and could step us into an
	 *  attack line, so those handlers hold and defer to planTentacleDodge (which steps to a tentacle-safe tile,
	 *  drifting toward the swing target). */
	private boolean tentacleInScene()
	{
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return false;
		for (NPC n : npcs)
			if (n != null && n.getId() == WhispererAutoIDs.NPC_TENTACLE) return true;
		return false;
	}

	private WorldPoint planTentacleDodge(int tick)
	{
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return null;

		java.util.List<NPC> npcs = client.getNpcs();
		if (npcs == null) return null;
		dodgeHazards = hazardTiles(me, npcs);   // avoid the tentacle->player line tiles, not just their directions

		// Classify each tentacle by the compass direction of its footprint relative to us. The
		// tentacles spawn a few tiles out (not adjacent) and attack INWARD, so what matters is which
		// direction each sits in, not its own tiles. Clamp test (robust to footprint size): the
		// tentacle is east if our X is left of its whole footprint, west if right of it, else
		// axis-aligned (0); same for Y. Both axes non-zero = a DIAGONAL spawn; exactly one non-zero
		// = a CARDINAL spawn. We record the 8-neighbour direction each tentacle occupies ("blocked").
		boolean[] blocked = new boolean[8];   // indexes into NEIGHBORS8
		boolean anyCardinal = false, anyDiagonal = false;
		int tentCount = 0, nearest = Integer.MAX_VALUE;
		java.util.List<String> diag = new java.util.ArrayList<>();
		for (NPC n : npcs)
		{
			if (n == null || n.getId() != WhispererAutoIDs.NPC_TENTACLE) continue;
			net.runelite.api.coords.WorldArea area = n.getWorldArea();
			if (area == null) continue;
			tentCount++;
			int x0 = area.getX(), x1 = area.getX() + area.getWidth()  - 1;
			int y0 = area.getY(), y1 = area.getY() + area.getHeight() - 1;
			int sx = me.getX() < x0 ? 1 : (me.getX() > x1 ? -1 : 0);
			int sy = me.getY() < y0 ? 1 : (me.getY() > y1 ? -1 : 0);
			int ddx = Math.max(0, Math.max(x0 - me.getX(), me.getX() - x1));
			int ddy = Math.max(0, Math.max(y0 - me.getY(), me.getY() - y1));
			int d = Math.max(ddx, ddy);
			if (d < nearest) nearest = d;
			boolean card = (sx == 0) ^ (sy == 0);     // exactly one axis zero
			boolean diagDir = (sx != 0) && (sy != 0);
			if (card) anyCardinal = true;
			if (diagDir) anyDiagonal = true;
			for (int i = 0; i < NEIGHBORS8.length; i++)
				if (NEIGHBORS8[i][0] == sx && NEIGHBORS8[i][1] == sy) blocked[i] = true;
			diag.add("(off=" + (x0 - me.getX()) + "," + (y0 - me.getY())
				+ " " + area.getWidth() + "x" + area.getHeight()
				+ " dir=" + sx + "," + sy + (card ? " C" : diagDir ? " D" : " ?") + ")");
		}

		// --- Diagnostics (capture mode only): live overlay readout + per-tentacle log. ---
		int threatAge = tentacleThreatTick < 0 ? -1 : tick - tentacleThreatTick;
		if (overlay != null)
		{
			overlay.setStat("tent", "n=" + tentCount + " near=" + (tentCount == 0 ? "-" : nearest)
				+ (anyCardinal ? " C" : "") + (anyDiagonal ? " D" : "")
				+ " age=" + threatAge + " dg=" + tentacleDodged);
		}
		if (config != null && config.captureMode() && tentCount > 0)
		{
			log.info("[tentDbg] me={} tentacles={} nearest={} cardinal={} diagonal={} age={} dodged={} dirs={}",
				me, tentCount, nearest, anyCardinal, anyDiagonal, threatAge, tentacleDodged, diag);
		}

		if (tentCount == 0)
		{
			// No tentacle in the scene -> re-arm for the next spawn.
			tentacleThreatTick = -100;
			tentacleDodged = false;
			return null;
		}

		// DPS WEAVE: latch the spawn tick, keep attacking through the telegraph, and step only at the
		// last safe tick -- exactly once per spawn.
		if (tentacleThreatTick < 0) tentacleThreatTick = tick;
		if (tentacleDodged) return null;
		if (tick - tentacleThreatTick < effectiveTentacleDelay()) return null;

		// Pick the safe step: tentacles on the cardinals -> step to a diagonal; tentacles on the
		// diagonals -> step to a cardinal. NEIGHBORS8 is [0..3] cardinals then [4..7] diagonals. Within
		// that allowed group, bestStep() chooses the tile that best keeps our boss spacing (so we
		// oscillate near the boss instead of always drifting north); if the group is somehow fully
		// blocked, fall back to any clear neighbour.
		int start = 0, end = 8;
		if (anyCardinal && !anyDiagonal) { start = 4; end = 8; }       // step diagonal
		else if (anyDiagonal && !anyCardinal) { start = 0; end = 4; }  // step cardinal
		int gap = effectiveTentacleGap();
		// SWING EFFICIENCY: bias the dodge TOWARD the swing's MAIN GOAL, so a tentacle we must dodge carries us toward
		// the destination instead of sideways. CURRENT GOAL vs MAIN GOAL: handleRepositionNorth walks the CURRENT goal
		// (repositionWaypoint -- the east corridor that routes AROUND her 3x3 so a straight walk can't clip her), but
		// the DODGE aims at the MAIN goal: the final north tile for the reposition maneuver, or the flee tile while
		// still running out. bestStep's 5x5 no-step buffer stops us dodging INTO her, so aiming at the north goal just
		// makes the dodge arc around her toward it (NE/NW) instead of stepping due east off the destination line.
		WorldPoint drift = null;
		if (repositionNorthActive && tick > bindFleeUntil)
			drift = repositionMainGoal();
		else if (swingSpecial != Special.NONE && swingStage == SwingStage.FLEE)
			drift = swingFleeTile(swingSpecial);
		WorldPoint step = bestStep(me, blocked, start, end, whisperer, gap, drift);
		if (step == null) step = bestStep(me, blocked, 0, 8, whisperer, gap, drift);
		if (step != null)
		{
			tentacleDodged = true;
			// HUMAN-LIKE 2-TILE DODGE: a real player repositioning hops more than one tile. Extend the chosen step
			// one more tile in the SAME direction when that tile is still safe -- outside the boss min-gap and not
			// adjacent to / on any tentacle. BUT during a swing (drift != null) only keep the 2-tile tile if it is
			// actually CLOSER to the destination than the 1-tile tile -- a blind extension overshoots PAST/AWAY from
			// the goal (e.g. 2 tiles north when the destination is north-WEST and a single west step is nearer).
			int ex = Integer.signum(step.getX() - me.getX());
			int ey = Integer.signum(step.getY() - me.getY());
			WorldPoint two = new WorldPoint(step.getX() + ex, step.getY() + ey, me.getPlane());
			if (dodgeTileSafe(two, npcs, gap) && (drift == null || dist2(two, drift) < dist2(step, drift)))
				step = two;
		}
		if (config != null && config.captureMode())
			log.info("[dodgePick] me={} drift={} step={} (reposN={} fleeStage={})",
				me, drift, step, repositionNorthActive, swingSpecial != Special.NONE && swingStage == SwingStage.FLEE);
		return step;
	}

	/** True if {@code tile} is a safe dodge destination: outside the boss min-gap (and not adjacent to her 3x3),
	 *  and not on or adjacent to any tentacle (12208) footprint. Used to vet the extended 2-tile dodge. */
	private boolean dodgeTileSafe(WorldPoint tile, java.util.List<NPC> npcs, int minGap)
	{
		if (tile == null) return false;
		if (dodgeHazards.contains(tile)) return false;   // on a tentacle->player hazard line
		net.runelite.api.coords.WorldArea bossArea = whisperer == null ? null : whisperer.getWorldArea();
		if (bossArea != null && (bossArea.distanceTo(tile) <= 1 || bossArea.distanceTo(tile) < minGap)) return false;
		if (npcs != null)
			for (NPC n : npcs)
			{
				if (n == null || n.getId() != WhispererAutoIDs.NPC_TENTACLE) continue;
				net.runelite.api.coords.WorldArea ta = n.getWorldArea();
				if (ta != null && ta.distanceTo(tile) <= 1) return false;   // on/adjacent to a tentacle -> unsafe
			}
		return true;
	}

	/** dodgeTileSafe MINUS the ranged min-gap clause: a non-hit tile (never on/beside her 3x3, never on/beside a
	 *  tentacle, never on a hazard line) even if it sits inside our preferred ranged gap. The relaxed pass of
	 *  {@link #safeNeighbourStep} -- being 1-2 tiles off her is a non-hit and beats standing in the tentacle. */
	private boolean dodgeTileMinimallySafe(WorldPoint tile, java.util.List<NPC> npcs)
	{
		if (tile == null) return false;
		if (dodgeHazards.contains(tile)) return false;
		net.runelite.api.coords.WorldArea bossArea = whisperer == null ? null : whisperer.getWorldArea();
		if (bossArea != null && bossArea.distanceTo(tile) <= 1) return false;   // on/beside her 3x3 -> a hit
		if (npcs != null)
			for (NPC n : npcs)
			{
				if (n == null || n.getId() != WhispererAutoIDs.NPC_TENTACLE) continue;
				net.runelite.api.coords.WorldArea ta = n.getWorldArea();
				if (ta != null && ta.distanceTo(tile) <= 1) return false;
			}
		return true;
	}

	/** Convenience: the live NPC list or an empty list (so callers needn't null-check). */
	private java.util.List<NPC> npcsNow()
	{
		java.util.List<NPC> n = client.getNpcs();
		return n != null ? n : java.util.Collections.emptyList();
	}

	/**
	 * Fallback dodge pick for when the planned tile lands on / beside her (the opening-oscillation override and
	 * clampInside bypass bestStep's 5x5 guard). Scans all 8 directions at 1 and 2 tiles for a tile that is INSIDE
	 * the arena and safe, preferring the one nearest the dodge we originally wanted ({@code want}). Two passes:
	 * first requiring our full ranged min-gap, then relaxed to any non-hit tile. Null only if every surrounding
	 * tile is a hit -- which shouldn't occur, so the caller holds just in case rather than clicking into her.
	 */
	private WorldPoint safeNeighbourStep(WorldPoint me, WorldPoint want, java.util.List<NPC> npcs, int gap)
	{
		if (me == null) return null;
		boolean arenaKnown = WhispererArena.isInside(me);   // false -> coord-space mismatch; skip the wall filter (as clampInside does)
		for (int pass = 0; pass < 2; pass++)   // 0 = outside the ranged gap; 1 = relaxed to any non-hit tile
		{
			WorldPoint best = null;
			double bestCost = Double.MAX_VALUE;
			for (int i = 0; i < NEIGHBORS8.length; i++)
			{
				for (int mul = 1; mul <= 2; mul++)
				{
					WorldPoint cand = new WorldPoint(me.getX() + NEIGHBORS8[i][0] * mul,
						me.getY() + NEIGHBORS8[i][1] * mul, me.getPlane());
					if (arenaKnown && !WhispererArena.isInside(cand)) continue;   // outside the walls
					boolean ok = pass == 0 ? dodgeTileSafe(cand, npcs, gap) : dodgeTileMinimallySafe(cand, npcs);
					if (!ok) continue;
					double cost = want != null ? dist2(cand, want) : i;   // nearest the tile we wanted
					if (cost < bestCost) { bestCost = cost; best = cand; }
				}
			}
			if (best != null) return best;
		}
		return null;
	}

	/**
	 * Choose the safe dodge tile from the neighbours in NEIGHBORS8[start..end) that have no tentacle
	 * in their direction. Spacing policy versus the boss: never step INSIDE {@code minGap} tiles (we
	 * fight at range, so melee range is off-limits), and otherwise hug that gap -- pick the clear tile
	 * whose resulting boss distance is closest to it, discouraging purely lateral steps (same distance)
	 * so we oscillate toward/away along the boss line rather than sliding sideways or drifting off.
	 * Falls back to a stable order when the boss reference is missing. Null if the group is all blocked.
	 */
	/** Chebyshev-ish distance from {@code t} to the NEAREST of her two centreline axes (her fixed starting
	 *  X or Y). Minimising it keeps us ON a centreline, which gives the full flee distance along the other
	 *  axis. 0 when her start centre isn't known yet (no bias). */
	private double centrelineDist(WorldPoint t)
	{
		WorldPoint c = whispererStartCentre != null ? whispererStartCentre : whispererCenterTile();
		if (c == null || t == null) return 0.0;
		return Math.min(Math.abs(t.getX() - c.getX()), Math.abs(t.getY() - c.getY()));
	}

	private WorldPoint bestStep(WorldPoint me, boolean[] blocked, int start, int end, NPC boss, int minGap,
		WorldPoint drift)
	{
		net.runelite.api.coords.WorldArea bossArea = boss == null ? null : boss.getWorldArea();
		int curDist = bossArea == null ? -1 : bossArea.distanceTo(me);
		WorldPoint best = null;
		double bestCost = Double.MAX_VALUE;
		for (int i = start; i < end; i++)
		{
			if (blocked[i]) continue;
			WorldPoint cand = new WorldPoint(me.getX() + NEIGHBORS8[i][0], me.getY() + NEIGHBORS8[i][1], me.getPlane());
			// NEVER step onto OR directly beside her: treat her 3x3 as a 5x5 (a 1-tile buffer on every side,
			// corners included). distanceTo is Chebyshev, so <= 1 covers her footprint (0) and every adjacent
			// tile incl. diagonals (1). Hard skip, not a high cost, so even the all-directions fallback can't pick it.
			if (bossArea != null && bossArea.distanceTo(cand) <= 1) continue;
			// HAZARD LINE: a tile on the line from a tentacle to us is a hit tile -- huge penalty so a hazard-free
			// tile always wins, but it stays PICKABLE (not a hard skip) so if literally every option is on a line we
			// still move somewhere rather than freeze.
			double hazardPenalty = dodgeHazards.contains(cand) ? 1_000_000.0 : 0.0;
			double cost;
			if (bossArea == null)
			{
				cost = (drift != null) ? dist2(cand, drift) : i;   // drift target, else deterministic fallback
			}
			else
			{
				int dist = bossArea.distanceTo(cand);
				if (dist < minGap)
				{
					cost = 100.0 + (minGap - dist);        // inside the gap -> avoid (closer = worse). Safety wins.
				}
				else if (drift != null)
				{
					// SWING: among tiles that keep us safely outside the gap, DANCE toward the reposition
					// waypoint (so we arc AROUND her to the north instead of just hugging the gap in place).
					cost = dist2(cand, drift);
				}
				else
				{
					cost = Math.abs(dist - minGap);        // hug the gap
					if (curDist >= 0 && dist == curDist) cost += 2.0;   // lateral step -> discourage
					cost += 0.5 * centrelineDist(cand);    // drift back toward her centre axes -> best flee clearance
				}
			}
			cost += hazardPenalty;
			if (cost < bestCost) { bestCost = cost; best = cand; }
		}
		return best;
	}

	/** Squared Euclidean distance between two tiles (for drift-target comparisons; no sqrt needed). */
	private static double dist2(WorldPoint a, WorldPoint b)
	{
		double dx = a.getX() - b.getX(), dy = a.getY() - b.getY();
		return dx * dx + dy * dy;
	}

	/**
	 * Re-acquire the Whisperer after a tentacle dodge: the dodge walk cancels our attack
	 * interaction, so once we've stepped clear we re-click her to resume DPS. Only re-clicks when
	 * we're not already interacting with her (so we don't reset her attack animation needlessly)
	 * and refreshes the attack-cadence clock.
	 * <p>
	 * TODO: the user will provide the exact attack menu option; {@link ClickRouter#attackNpc}
	 * issues the default "Attack" for now (mirrors the blackstone-activate placeholder approach).
	 */
	private void relockWhisperer(int tick)
	{
		if (whisperer == null) return;
		Player player = mirror.getLocalPlayer();
		if (player != null && player.getInteracting() == whisperer) return;   // still locked on
		attackWhisperer();
		lastAttackTick = tick;
		step("Dodge tentacle", "tentacle dodge: relock whisperer");
	}

	/**
	 * SEED-FIRST reposition: after the seed bind, dance around to the NORTH of her. She's planted (pulled
	 * south by the bind), the open floor is north, so we settle {@link #REPOS_NORTH_OFFSET} tiles north of her
	 * live centre on her lane. We must NOT cut straight through her 3x3 (stepping onto it is gap 0 = a hit),
	 * so we go AROUND via a corridor {@link #REPOS_CORRIDOR_DX} tiles to the side we're already on (west if we
	 * fled west, else east), run north past her, then cut back to the lane. We weave attacks when in range and off
	 * cooldown. Cleared on arrival or when the next special begins. Returns true (claims the tick) while active.
	 */
	/** Begin the post-special swing for {@code s}: flee to its tile, then (if she binds) hold, then apply its
	 *  maneuver rule. Called the moment a special ends. */
	private void startSwing(Special s, int tick)
	{
		swingSpecial = s;
		swingStage = SwingStage.FLEE;
		swingTick = tick;
		swingArrivedTick = -100;         // not at the flee tile yet; the bind-watch starts when we arrive
		lastSwingClick = null;
		swingFleeAnchor = null;          // fresh swing -> recompute (and re-latch) the flee tile once
		boundInPlaceChatSeen = false;    // fresh swing -> wait for this bind's "binds you in place" chat
		repositionNorthActive = false;   // the maneuver is armed only on resolve, if the rule says so
	}

	/**
	 * The special's flee tile on her centre lane, found by stepping out until the live footprint gap
	 * ({@link #whispererGap}, i.e. {@code WorldArea.distanceTo}) reaches {@code BIND_SAFE_GAP + 1}. We target
	 * GAP+1 (not GAP) so that when she binds and walks in she can advance up to {@code BIND_SAFE_GAP} tiles and
	 * stop ADJACENT (gap 1, no contact); at distanceTo == GAP a full walk-in lands on our tile (gap 0 = hit).
	 * Measuring the gap directly makes it exact regardless of which footprint edge / parity we'd compute from.
	 * Centre lane x = {@code a.getX()+width/2}. Null if she's absent.
	 */
	private WorldPoint swingFleeTile(Special s)
	{
		if (swingFleeAnchor != null) return swingFleeAnchor;   // latched at swing start -- do NOT chase her live centre
		// Reference the FIXED arena centreline (her 3x3 starting X/Y, captured at wake), not her live/bind-form
		// position -- so flees stay on the centreline. Fall back to the live centre only before the first wake.
		WorldPoint c = whispererStartCentre != null ? whispererStartCentre : whispererCenterTile();
		if (c == null) return null;
		// Offset from her CENTRE assuming the FULL 3x3 footprint (centre->edge = WHISPERER_HALF_FOOTPRINT). We do
		// NOT measure off getWorldArea's edge: during screech / the bind form it under-reports her size (reports
		// her centre tile, not the 3x3), which made the edge come out 1 tile too close. SAME offset both ways:
		// 12 clear of her edge = half(1) + BIND_SAFE_GAP(10) + 2 = 13 from centre. She always walks BIND_SAFE_GAP
		// (10) tiles in on a bind, so 12 clear leaves ONE empty tile between us after the walk-in (a "gap of 1");
		// no bind, we're just parked 12 out. e.g. screech centre 6357 -> 6370; seeds centre 6369 -> 6356.
		int off = WHISPERER_HALF_FOOTPRINT + BIND_SAFE_GAP + 2;   // = 13
		WorldPoint flee;
		switch (s)
		{
			case SCREECH: flee = new WorldPoint(c.getX(), c.getY() + off, c.getPlane()); break;   // north -> recentre handles it
			case SEEDS:
			case SIPHON:  flee = fleeTileNearestSide(c, off); break;   // DYNAMIC: flee to the side we ended up on
			default:      return c;   // NONE: no flee -- don't latch
		}
		flee = WhispererArena.clampInside(flee, mirror.getPlayerTile());   // never flee into a wall (E/W is a 13-tile exact fit)
		swingFleeAnchor = flee;   // latch: this swing re-uses the same tile no matter where she drifts
		return flee;
	}

	/**
	 * Flee tile {@code off} tiles from her centre along the cardinal our CURRENT position is most aligned with,
	 * so we flee to whichever side we already ended the phase on (shortest hop) instead of always crossing to
	 * the south. The post-special swing recentres us afterwards, so any side is equally fine. N/S wins ties and
	 * near-diagonals (the arena is tall), and a dead-centre / unknown position defaults to south.
	 */
	private WorldPoint fleeTileNearestSide(WorldPoint c, int off)
	{
		WorldPoint me = mirror.getPlayerTile();
		int dx = me == null ? 0 : me.getX() - c.getX();
		int dy = me == null ? 0 : me.getY() - c.getY();
		// Flee to whichever of the 4 cardinals we ended up closest to (there's room on every side); the swing
		// then brings us back toward centre (opposite the flee). N/S wins ties and near-diagonals (the centreline
		// row is the widest); a dead-centre / unknown position defaults south.
		if (dx == 0 && dy == 0) return new WorldPoint(c.getX(), c.getY() - off, c.getPlane());   // dead centre -> south
		if (Math.abs(dy) >= Math.abs(dx))
			return new WorldPoint(c.getX(), c.getY() + (dy >= 0 ? off : -off), c.getPlane());   // north / south
		return new WorldPoint(c.getX() + (dx >= 0 ? off : -off), c.getY(), c.getPlane());        // east / west
	}

	/** Ticks after this special's end to watch for the bind form (12205) before concluding "no bind". */
	private int bindAppearTicks(Special s)
	{
		switch (s)
		{
			case SCREECH: return BIND_APPEAR_TICKS_SCREECH;
			case SIPHON:  return BIND_APPEAR_TICKS_SIPHON;
			case SEEDS:
			default:      return BIND_APPEAR_TICKS_SEEDS;
		}
	}

	/** Whether this special maneuvers NORTH after the flee: Seeds only if she bound, Siphon always, Screech
	 *  never (fleeing north already positions us). */
	private boolean swingManeuvers(Special s, boolean wasBound)
	{
		switch (s)
		{
			case SEEDS:
			case SIPHON:  return wasBound;   // bind -> maneuver north; no bind -> resume (siphon: resume oscillation)
			case SCREECH:
			default:      return false;
		}
	}

	/** True if a special OTHER than {@code mine} is active -- its own lingering phase flag (e.g. siphon's
	 *  20-tick timer) must not count, or the swing/maneuver would self-abort. */
	private boolean otherSpecialActive(Special mine)
	{
		if (mine != Special.SEEDS && seedPhaseActive) return true;
		if (mine != Special.SCREECH && !hidePillarChainOwTiles.isEmpty()) return true;
		if (mine != Special.SIPHON && inSiphonPhase()) return true;
		return false;
	}

	/**
	 * Per-special post-phase swing (the stages before the north maneuver): FLEE to the special's flee tile,
	 * then BOUND = hold while she binds (NPC 12205), then resolve by the special's maneuver rule. Claims the
	 * tick while it owns movement; yields (false) once it hands off to the maneuver or resumes normal fighting.
	 */
	private boolean handleSwing(int tick)
	{
		if (swingSpecial == Special.NONE) return false;
		// NOTE: the siphon weapon swap-back is NOT done here anymore. It is done together with the blackstone
		// cross-back in handleSiphonPhase (one tick, mirroring the entry), so this handler no longer pre-empts it.
		if (otherSpecialActive(swingSpecial)) { swingSpecial = Special.NONE; return false; }   // a NEW special began
		if (!inOverworld()) return false;   // siphon: still crossing back -> the siphon handler owns the tick
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return false;

		// THE reliable bind signal: she flips to NPC 12205 to cast + hold the bind.
		boolean binding = findNpcAny(WhispererAutoIDs.NPC_WHISPERER_BINDING) != null;

		if (swingStage == SwingStage.BOUND)
		{
			// Rooted while she binds + walks in (she's 12205). HOLD -- issue nothing (a click queues and snaps
			// us back when the root releases). The bind is over only when 12205 goes missing AND the awake form
			// (12204) is back; THEN resolve (was bound) -> swing while dodging tentacles (planTentacleDodge runs
			// ahead of this each tick, so the maneuver and the dodge coexist).
			boolean awakeBack = findNpcAny(WhispererAutoIDs.NPC_WHISPERER_AWAKE) != null;
			if (!binding && awakeBack) { resolveSwing(true); return false; }
			// ROOTED and still able to DPS. Attack ONLY once the "binds you in place" chat has CONFIRMED the root
			// (boundInPlaceChatSeen): 12205 appears while she's still casting / walking in -- before the root lands
			// -- so attacking then would PATH us toward her (we move when we shouldn't). Once actually rooted we
			// can't move, so keep hitting 12205 whenever she's in range: re-click when not already locked onto her
			// OR the weapon is off cooldown. Range-gated so an out-of-range click can't queue a walk for when the
			// root lifts.
			if (boundInPlaceChatSeen)
			{
				NPC bindNpc = findNpcAny(WhispererAutoIDs.NPC_WHISPERER_BINDING);
				if (bindNpc != null && isInAttackRange(bindNpc))
				{
					Player boundPlayer = mirror.getLocalPlayer();
					boolean locked = boundPlayer != null && boundPlayer.getInteracting() == bindNpc;
					if (!locked || tick - lastAttackTick >= effectiveCooldown())
					{
						router.interactNpc(bindNpc, net.runelite.api.MenuAction.NPC_SECOND_OPTION,
							WhispererAutoIDs.WHISPERER_ATTACK_OPTION);
						lastAttackTick = tick;
						step("Bound", "swing(" + swingSpecial + "): bound (12205) -> attack");
						return true;
					}
				}
			}
			// NOT rooted yet (12205 is still casting / walking in): KEEP FLEEING to open the bind gap. The FLEE-stage
			// weave above cleared lastSwingClick, so this re-clicks the flee tile the tick right after a weave --
			// which is what stops us standing still the moment she flips 12204 -> 12205. swingFleeTile resolves off
			// the 12205 form now (whispererCenterTile falls back to it). Once ACTUALLY rooted the chat gate above
			// owns the tick and we weave 12205 in place instead.
			if (!boundInPlaceChatSeen)
			{
				WorldPoint meB = mirror.getPlayerTile();
				WorldPoint fleeB = swingFleeTile(swingSpecial);
				if (meB != null && fleeB != null && !meB.equals(fleeB) && !fleeB.equals(lastSwingClick))
				{
					router.walkTo(fleeB);
					lastSwingClick = fleeB;
				}
				step(fleeLabel(), "swing(" + swingSpecial + "): 12205 (not rooted) -> flee " + fleeB);
				return true;
			}
			step("Bound", "swing(" + swingSpecial + "): bound (12205), holding");
			return true;
		}

		// FLEE to the special's flee tile. If she flips to 12205 at any point -> BOUND.
		if (binding) { swingStage = SwingStage.BOUND; return true; }
		// NO-BIND decision, anchored to the special END (not flee-tile arrival), so it fires on time even while
		// we're still running (a no-bind just resumes/maneuvers, so deciding mid-flee is safe):
		//   * Seeds/screech count from swingTick (e.g. the 10252 seed anim).
		//   * SIPHON counts from her 10259 overworld end anim (lastSiphonEndAnimTick) -- swingTick there is the 4th
		//     shot in the SHADOW realm, before the cross-back; until 10259 has fired we don't start the clock.
		// She did NOT bind if, within bindAppearTicks of the anchor, no 12205 appeared. SEEDS additionally bails the
		// instant she throws a normal attack (2443/2444) -- that early-out is SEEDS-ONLY: after a SIPHON she resumes
		// her normal ranged/magic attacks the moment we land, so applying it there would cancel the south flee
		// before it even starts (the bug).
		boolean siphonClockReady = swingSpecial != Special.SIPHON || lastSiphonEndAnimTick >= 0;
		if (siphonClockReady)
		{
			if (swingSpecial == Special.SEEDS
				&& (mirror.hasProjectile(WhispererAutoIDs.PROJ_NORMAL_ALT) || mirror.hasProjectile(WhispererAutoIDs.PROJ_RANGE)))
			{
				step(fleeLabel(), "swing(" + swingSpecial + "): normal attack (2443/2444) -> no bind");
				resolveSwing(false);
				return false;
			}
			int anchor = swingSpecial == Special.SIPHON ? lastSiphonEndAnimTick : swingTick;
			if (tick - anchor >= bindAppearTicks(swingSpecial)) { resolveSwing(false); return false; }
		}
		WorldPoint flee = swingFleeTile(swingSpecial);   // edge-based: gap clear of her 3x3 footprint, centre lane
		if (flee == null) return true;                   // transient (e.g. mid-flip) -> hold a tick
		if (config != null && config.captureMode())
		{
			net.runelite.api.coords.WorldArea wa = whisperer == null ? null : whisperer.getWorldArea();
			log.info("[fleeDbg] special={} me={} flee={} gapMe={} gapFlee={} atTile={} area={}",
				swingSpecial, me, flee, whispererGap(me), whispererGap(flee),
				swingArrivedTick < 0 ? -1 : tick - swingArrivedTick,
				wa == null ? "null" : ("(" + wa.getX() + "," + wa.getY() + " " + wa.getWidth() + "x" + wa.getHeight() + ")"));
		}
		if (me.equals(flee))
		{
			// Arrived. HOLD on the flee tile and watch for 12205. The no-bind timeout / normal-attack early-out
			// above (anchored to the special end) ends the swing if she does NOT bind; if she DOES, the FLEE->BOUND
			// flip above catches the 12205 and we stay here until it clears and 12204 returns.
			if (swingArrivedTick < 0) swingArrivedTick = tick;
			lastSwingClick = null;
			step(fleeLabel(), "swing(" + swingSpecial + "): at flee tile, watching for 12205");
			return true;
		}
		// DPS WHILE RUNNING OUT: after siphon (and seeds) we land next to her and flee south, so she is still in range
		// for the first few tiles. Weave an attack in place when in range + off cooldown BEFORE stepping -- a ranged
		// shot fires without moving, and once we run past her range (gap > weapon range) this stops firing, so it is
		// naturally just the once-or-twice on the way out. The attack interrupts the walk, so re-click the flee tile
		// next tick.
		if (whisperer != null && inAttackRange(me) && tick - lastAttackTick >= effectiveCooldown())
		{
			attackWhisperer();
			lastAttackTick = tick;
			lastSwingClick = null;
			step(fleeLabel(), "swing(" + swingSpecial + "): flee weave DPS -> " + flee);
			return true;
		}
		if (!flee.equals(lastSwingClick)) { router.walkTo(flee); lastSwingClick = flee; }
		step(fleeLabel(), "swing(" + swingSpecial + "): flee -> " + flee);
		return true;
	}

	/** End the swing: arm the north maneuver iff the special's rule says so (given whether she bound), else
	 *  resume normal fighting. */
	private void resolveSwing(boolean wasBound)
	{
		Special resolved = swingSpecial;
		boolean maneuver = swingManeuvers(resolved, wasBound);
		// Swing back OPPOSITE the flee, toward centre -- captured from the flee anchor before we drop it.
		reposDx = 0; reposDy = 1;   // default north (legacy / no anchor)
		{
			WorldPoint fc = whispererStartCentre != null ? whispererStartCentre : whispererCenterTile();
			if (swingFleeAnchor != null && fc != null)
			{
				int fdx = Integer.signum(swingFleeAnchor.getX() - fc.getX());
				int fdy = Integer.signum(swingFleeAnchor.getY() - fc.getY());
				if (fdx != 0 || fdy != 0) { reposDx = -fdx; reposDy = -fdy; }
			}
		}
		swingSpecial = Special.NONE;
		lastSwingClick = null;
		swingFleeAnchor = null;
		boundInPlaceChatSeen = false;   // swing over -> this bind's root is done
		if (maneuver)
		{
			maneuverSpecial = resolved;
			repositionNorthActive = true;
			recenterActive = false;
			step("Reposition", "swing: bind resolved -> maneuver north");
		}
		else if (resolved == Special.SCREECH)
		{
			// Screech fled us NORTH while she was pulled south; now walk back DOWN to the centre camp to resume DPS.
			maneuverSpecial = Special.SCREECH;
			repositionNorthActive = false;
			recenterActive = true;
			step("Recentre", "swing: screech done -> recentre");
		}
		else
		{
			maneuverSpecial = Special.NONE;
			repositionNorthActive = false;
			recenterActive = false;
			// Siphon that did NOT bind -> resume the TENTACLE phase on the opening oscillation tiles (phase 1).
			if (resolved == Special.SIPHON) resumeOscillation = true;
			step("Attacking", resolved == Special.SIPHON
				? "swing: siphon done (no bind) -> resume oscillation" : "swing: resolved -> resume");
		}
	}

	private boolean handleRepositionNorth(int tick)
	{
		if (!repositionNorthActive || tick <= bindFleeUntil) return false;   // wait out the bind flee first
		// Abandon the maneuver only if a GENUINELY new special has begun -- excluding the one that TRIGGERED
		// this maneuver, whose phase flag may still linger (siphon's 20-tick timer especially). Authoritative
		// signals only (hidePillarChainOwTiles for screech -- not inScreechPhase(), which 10251/10252 trip).
		if (otherSpecialActive(maneuverSpecial))
		{
			repositionNorthActive = false;   // a new special started -> abandon the maneuver
			return false;
		}
		if (whisperer == null || !isWhispererAwake() || !inOverworld()) return false;
		WorldPoint me = mirror.getPlayerTile();
		// TENTACLE SAFETY: while a tentacle is up, the DODGE owns movement (planTentacleDodge ran first this tick
		// and, on its step tick, drifts toward this waypoint). Do NOT issue our own straight walk here -- it ignores
		// tentacle directions and is exactly how we stepped into an attack line mid-swing. Weave only a STATIONARY
		// in-range shot (no movement); otherwise hold. Claim the tick so maintainBossAttack can't path us in either.
		if (tentacleInScene())
		{
			if (me != null)
			{
				net.runelite.client.plugins.autoBossCore.combat.AttackPredictor.Result pr = predictBossAttack(me);
				if (pr != null && !pr.willMove && pr.reachable && tick - lastAttackTick >= effectiveCooldown())
				{
					attackWhisperer();
					lastAttackTick = tick;
					lastReposClick = null;
					step("Reposition", "repos: tentacle up -> stationary weave (dodge owns moves)");
					return true;
				}
			}
			lastReposClick = null;
			step("Reposition", "repos: tentacle up -> hold (dodge owns moves)");
			return true;
		}
		WorldPoint step = repositionWaypoint(me);   // corridor-aware; clears the flag on arrival
		if (step == null)
		{
			step("Reposition", "repos: north of boss -- resume");
			return false;
		}

		// DANCE: weave an attack when in range and off cooldown; otherwise take the step.
		if (inAttackRange(me) && tick - lastAttackTick >= effectiveCooldown())
		{
			attackWhisperer();
			lastAttackTick = tick;
			lastReposClick = null;   // attack interrupts the walk -> re-click next tick
			step("Reposition", "repos north: weave attack");
			return true;
		}
		if (!step.equals(lastReposClick))
		{
			router.walkTo(step);
			lastReposClick = step;
			step("Reposition", "repos north -> " + step);
		}
		return true;
	}

	/**
	 * Screech RECENTRE maneuver: after the screech flee (we ran north, she was pulled south) walk back DOWN to
	 * the centre fight camp so DPS / the tentacle oscillation resume from the right spot instead of from the
	 * north flee tile. Weaves an attack when already in range and off cooldown. Claims the tick while walking;
	 * clears on arrival, when the camp is unknown, or when a new special begins.
	 */
	private boolean handleRecenter(int tick)
	{
		if (!recenterActive || tick <= bindFleeUntil) return false;   // wait out any bind flee first
		if (otherSpecialActive(maneuverSpecial)) { recenterActive = false; return false; }   // a new special started
		// A bind rooted us during the recentre. We're already safe north (she walked UP and stopped 1 short), so we
		// do NOT chase the camp -- but while 12205 is still up we are rooted and CAN keep DPSing, so attack the bind
		// form (12205) in range instead of idling. Only once the bind RESOLVES (12205 gone) do we end the recentre
		// and resume normal DPS in place (chasing the camp off her new northern centre would be pointless).
		if (recenterBindSeen)
		{
			NPC bindNpc = findNpcAny(WhispererAutoIDs.NPC_WHISPERER_BINDING);
			if (bindNpc != null)
			{
				if (isInAttackRange(bindNpc))
				{
					Player rp = mirror.getLocalPlayer();
					boolean locked = rp != null && rp.getInteracting() == bindNpc;
					if (!locked || tick - lastAttackTick >= effectiveCooldown())
					{
						router.interactNpc(bindNpc, net.runelite.api.MenuAction.NPC_SECOND_OPTION,
							WhispererAutoIDs.WHISPERER_ATTACK_OPTION);
						lastAttackTick = tick;
						step("Recentre", "recentre: bound (12205) -> attack");
						return true;
					}
				}
				step("Recentre", "recentre: bound (12205), holding");
				return true;
			}
			// 12205 gone -> bind resolved, we're safe. Stop recentering and resume normal DPS in place.
			recenterActive = false;
			recenterBindSeen = false;
			lastRecenterClick = null;
			step("Recentre", "recentre: bind resolved -> resume");
			return false;
		}
		if (whisperer == null || !isWhispererAwake() || !inOverworld()) return false;
		// TENTACLE SAFETY: same as the swing maneuver -- while a tentacle is up, the straight walk to camp would
		// ignore tentacle directions, so hold and let the dodge own movement. Claim the tick so maintainBossAttack
		// can't walk us into a line either.
		if (tentacleInScene())
		{
			lastRecenterClick = null;
			step("Recentre", "recentre: tentacle up -> hold (dodge owns moves)");
			return true;
		}
		WorldPoint me = mirror.getPlayerTile();
		WorldPoint camp = centerCampTile();
		if (me == null || camp == null) { recenterActive = false; return false; }   // no camp ref -> resume in place
		if (me.equals(camp))
		{
			recenterActive = false;
			lastRecenterClick = null;
			step("Recentre", "recentre: arrived");
			return false;
		}
		// Walk STRAIGHT to the camp. No attack-weave here: the camp (+1 N of her start centre) is gap 11 = OUT of
		// range, so an attack would path us IN toward her and we'd never actually reach it (that's why we kept
		// landing back at centre). DPS resumes on its own once we arrive and recenterActive clears.
		if (!camp.equals(lastRecenterClick))
		{
			router.walkTo(camp);
			lastRecenterClick = camp;
			step("Recentre", "recentre -> " + camp);
		}
		return true;
	}

	/** The post-screech recentre tile, computed DYNAMICALLY from her LIVE footprint like the flee tiles -- not a
	 *  captured/hardcoded tile. During screech she's pulled SOUTH, so we settle BIND_SAFE_GAP+1 (11) clear tiles
	 *  NORTH of her on her centre lane -- geometrically the screech flee tile, so reuse it. Steps out measuring
	 *  with whispererGap so the gap is exact; null if she's absent (caller resumes in place). */
	private WorldPoint centerCampTile()
	{
		return swingFleeTile(Special.SCREECH);
	}

	/**
	 * Next waypoint of the seed-first north swing from {@code me}: route AROUND her 3x3 via the east corridor
	 * while south/level, then up her lane to {@link #REPOS_NORTH_OFFSET} north. Null when the swing is inactive
	 * or we've arrived (and it clears {@link #repositionNorthActive} on arrival). Shared by handleRepositionNorth
	 * (walks straight to it during a lull) and the tentacle dodge (dances TOWARD it while dodging tentacles) --
	 * the dodge alone runs every tick a tentacle is up, which is why the swing must live in the dodge too, not
	 * only in handleRepositionNorth (which the dodge preempts).
	 */
	private WorldPoint repositionWaypoint(WorldPoint me)
	{
		if (!repositionNorthActive || me == null) return null;
		WorldPoint c = whispererStartCentre != null ? whispererStartCentre : whispererCenterTile();
		if (c == null) return null;
		int cx = c.getX(), cy = c.getY(), plane = c.getPlane();
		int sx = reposDx, sy = reposDy, off = REPOS_NORTH_OFFSET, corr = REPOS_CORRIDOR_DX;
		// Swing back toward centre (opposite the flee). Never cut through her 3x3, so round her via a corridor
		// PERPENDICULAR to the swing axis: N/S swings round east/west, the west swing (from an east flee) rounds
		// north/south. Her centre is the arena centre, so the perpendicular corridor is interior either way.
		if (sy != 0)
		{
			int proj = (me.getY() - cy) * sy;   // progress along the swing (Y) axis toward the goal side
			if (proj >= off && Math.abs(me.getX() - cx) <= 1) { repositionNorthActive = false; return null; }
			if (proj > 1) return new WorldPoint(cx, cy + sy * off, plane);   // past her -> up the lane to goal
			int cxSide = (me.getX() < cx - 1) ? -1 : 1;   // round on the side we're on (default east)
			int corridorX = cx + cxSide * corr;
			boolean at = cxSide > 0 ? me.getX() >= corridorX : me.getX() <= corridorX;
			return at ? new WorldPoint(corridorX, cy + sy * 2, plane)      // run along the corridor past her
			          : new WorldPoint(corridorX, me.getY(), plane);       // sidestep onto the corridor
		}
		// swing along X (west, from an east flee) -> round via a N/S corridor
		int proj = (me.getX() - cx) * sx;
		if (proj >= off && Math.abs(me.getY() - cy) <= 1) { repositionNorthActive = false; return null; }
		if (proj > 1) return new WorldPoint(cx + sx * off, cy, plane);   // past her -> along the lane to goal
		int cySide = (me.getY() < cy - 1) ? -1 : 1;   // round via N/S corridor (default north)
		int corridorY = cy + cySide * corr;
		boolean at = cySide > 0 ? me.getY() >= corridorY : me.getY() <= corridorY;
		return at ? new WorldPoint(cx + sx * 2, corridorY, plane)      // run along the corridor past her
		          : new WorldPoint(me.getX(), corridorY, plane);       // sidestep onto the corridor
	}

	/** The MAIN GOAL of the north maneuver: the FINAL destination tile on her centre lane
	 *  ({@code cx, cy + REPOS_NORTH_OFFSET}) -- as opposed to the CURRENT goal {@link #repositionWaypoint} (the east
	 *  corridor that routes the straight walk around her 3x3). Used as the DODGE drift target so a tentacle dodge
	 *  heads toward the destination (arcing around her via the 5x5 no-step buffer) instead of due east. Null if
	 *  she's absent. Does NOT clear the maneuver flag (read-only; repositionWaypoint owns the arrival check). */
	private WorldPoint repositionMainGoal()
	{
		WorldPoint c = whispererStartCentre != null ? whispererStartCentre : whispererCenterTile();
		if (c == null) return null;
		return new WorldPoint(c.getX() + reposDx * REPOS_NORTH_OFFSET, c.getY() + reposDy * REPOS_NORTH_OFFSET, c.getPlane());
	}

	/**
	 * FIGHT START. While the Whisperer is DORMANT (12203), walk to the fight start tile and, once standing on
	 * it, wake her (attack the dormant form) -- the fight then begins and the opening oscillation takes over.
	 * Overworld only, and gated on our own tile being recognised as inside the mapped arena, so a coordinate-
	 * space mismatch disables it safely (the user positions and wakes manually instead of us walking wrong).
	 * Returns true (claims the tick) while it's driving the start.
	 */
	/**
	 * Post-death CLEANUP phase: after the Whisperer's FINAL death and before the Odd Figure (12203) respawns
	 * (~10s). Runs only once we've died this trip (deathPrayersOff latched) and the boss NPC is not in the
	 * scene yet. STUB for now -- claims the tick so the fight handlers stay idle while we build it out.
	 * Returns true while in the cleanup window.
	 */
	private boolean handleCleanup(int tick)
	{
		if (!deathPrayersOff || whisperer != null || !inOverworld()) return false;

		// TODO 1: pick up loot (walk to / collect the drop).
		// TODO 2: decide whether to keep going (start another kill) or stop / bank.
		// TODO 3: wait for the Odd Figure (12203) to respawn (~10s) and top off supplies (eat / drink) if needed
		//         while waiting -- this is a safe idle window, so a heal/sip here costs no DPS.

		step("Cleanup", "cleanup: (TODO) loot / decide / wait for respawn");
		return true;
	}

	private boolean handleFightStart(int tick)
	{
		if (whisperer == null || isWhispererAwake()) return false;   // only while she's dormant
		if (!inOverworld()) return false;
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return false;
		// The fight is INSTANCED: our tile is instance-space, but the arena map / start tile are static
		// template coords. Convert our tile to template for the in-arena check, and the start tile to THIS
		// instance so we can walk to it and compare Y with our instance tile.
		WorldPoint meTpl = InstanceCoords.toTemplate(client, me);
		if (meTpl == null || !WhispererArena.isInside(meTpl)) return false;   // space-safe: only in the mapped arena
		WorldPoint start = InstanceCoords.fromTemplate(client, WhispererArena.START_TILE);
		if (start == null) return false;

		// Once the Odd Figure begins its wake animation (10227), STOP acting: the knockback to the start tile
		// and the 12204 spawn follow on their own. Re-poking / re-walking now would just oscillate (Vardorvis bug).
		if (whisperer.getAnimation() == WhispererAutoIDs.ANIM_ODD_FIGURE_WAKE) oddFigureWaking = true;
		if (oddFigureWaking)
		{
			lastStartClick = null;
			step("Start", "start: waking -- hold for whisperer (12204)");
			return true;
		}
		// Already disturbed her: the 'Disturb' auto-walks us up off the start tile toward her, which is EXPECTED.
		// HOLD and wait for the wake anim (10227) -- do NOT walk back to the start tile (that was the oscillation).
		// Only re-poke if the wake hasn't begun after a while (the Disturb click never landed).
		if (oddFigureDisturbed)
		{
			if (tick - lastDisturbTick <= DISTURB_WAIT_TICKS)
			{
				step("Start", "start: disturbed -- waiting for wake");
				return true;
			}
			oddFigureDisturbed = false;   // timed out with no wake anim -> try again below
		}

		// Not yet disturbed. HUMAN shortcut: if we're between the start tile and HER (or south of the start
		// tile), just poke from here -- no point walking down to the start tile only to walk back up to reach
		// her. We reposition down to the start tile ONLY if we're actually north of her (unusual). Her centre Y
		// is the line: at/south of it -> poke now; north of it -> drop to the start tile first.
		WorldPoint wc = whispererCenterTile();
		int pokeLineY = wc != null ? wc.getY() : start.getY();
		if (me.getY() > pokeLineY && !me.equals(start))
		{
			if (!start.equals(lastStartClick))
			{
				router.walkTo(start);
				lastStartClick = start;
			}
			step("Start", "start: walk to start tile " + start);
			return true;
		}

		lastStartClick = null;
		disturbOddFigure();
		oddFigureDisturbed = true;
		lastDisturbTick = tick;
		lastAttackTick = tick;
		step("Start", "start: disturb odd figure");
		return true;
	}

	/** Wake the dormant Whisperer by 'Disturb'-ing the Odd Figure (NPC 12203, NPC_FIRST_OPTION). */
	private void disturbOddFigure()
	{
		NPC fig = findNpcAny(WhispererAutoIDs.NPC_WHISPERER_DORMANT);
		if (fig == null) return;
		router.interactNpc(fig, net.runelite.api.MenuAction.NPC_FIRST_OPTION, WhispererAutoIDs.ODD_FIGURE_OPTION);
	}

	/** True only in the genuinely idle windows where topping off can't cost an attack: running the post-phase
	 *  bind flee, or settled behind a screech pillar waiting it out. */
	private boolean consumeIdleWindow(int tick)
	{
		if (tick <= bindFleeUntil) return true;                       // fleeing to open the bind gap
		if (inScreechPhase() && screechHideArrived) return true;      // hiding behind a pillar, not attacking
		return false;
	}

	/**
	 * Attack the Whisperer. SINGLE SOURCE for the boss attack click -- the relock after a tentacle
	 * dodge and the attack-maintenance loop both route through here, so the exact attack action
	 * lives in exactly one place. No-op if the boss isn't present.
	 * <p>
	 * Captured attack action: NPC_SECOND_OPTION option='Attack' target='The Whisperer'
	 * (see {@link WhispererAutoIDs#WHISPERER_ATTACK_OPTION}).
	 */
	private void attackWhisperer()
	{
		if (whisperer == null) return;
		router.interactNpc(whisperer, net.runelite.api.MenuAction.NPC_SECOND_OPTION,
			WhispererAutoIDs.WHISPERER_ATTACK_OPTION);
	}

	/**
	 * Equip the Venator bow from the inventory for the siphon phase. Captured menu action:
	 * CC_OP id=3 itemId=27610 param1=9764864 (inventory widget) option='Wield' target='Venator bow'.
	 * So this is {@link ClickRouter#useInventoryItem} with identifier 3 and option "Wield". No-op if the
	 * bow isn't in the inventory (useInventoryItem returns false when the slot isn't found).
	 */
	private void equipVenatorBow()
	{
		router.useInventoryItem(ITEM_VENATOR_BOW, 3, "Wield");
	}

	/**
	 * Equip the configured MAIN weapon from the inventory -- the swap-back after the siphon's Venator bow.
	 * Keyed off {@link WhispererAutoConfig#weapon()} / {@link WhispererWeapon#itemId}; e.g. Shadow is the
	 * captured CC_OP id=3 itemId=27275 option='Wield'. The slot is resolved live by
	 * {@link ClickRouter#useInventoryItem} (findInventorySlot), so it's slot-safe. No-op if the weapon's
	 * id is unknown (-1) or it isn't in the inventory (already worn / absent).
	 */
	private void equipMainWeapon()
	{
		int id = mainWeaponWieldId();
		if (id <= 0) return;                            // nothing to wield is in the inventory
		router.useInventoryItem(id, 3, "Wield");
	}

	/** Snapshot the item id currently in our WEAPON equipment slot (called at siphon start, before we swap to the
	 *  Venator bow). Needs the equipment container; harmless no-op if it isn't available. */
	private void captureMainWeaponId()
	{
		ItemContainer eq = client.getItemContainer(InventoryID.EQUIPMENT);
		if (eq == null) return;
		Item w = eq.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		if (w != null && w.getId() > 0) savedMainWeaponId = w.getId();
		if (config != null && config.captureMode())
			log.info("[weaponSwap] captured worn weapon id={} (config id={})",
				savedMainWeaponId, config.weapon() == null ? -1 : config.weapon().itemId);
	}

	/** The id to wield as the main weapon, resolved to whatever is ACTUALLY in the inventory: first the exact id
	 *  we captured from the weapon slot before the bow swap (handles charged/variant ids the config can't know),
	 *  else the configured weapon id. -1 if neither is in the inventory (already worn / absent). */
	private int mainWeaponWieldId()
	{
		if (savedMainWeaponId > 0 && mirror.countItem(savedMainWeaponId) > 0) return savedMainWeaponId;
		WhispererWeapon w = config == null ? WhispererWeapon.STANDARD_SPELLS : config.weapon();
		if (w != null && w.itemId > 0 && mirror.countItem(w.itemId) > 0) return w.itemId;
		return -1;
	}

	/**
	 * Keep attacking the awake Whisperer (12204). Only (re-)clicks when we're not already interacting
	 * with her and the weapon's attack speed ({@link #effectiveCooldown()}) has elapsed -- once locked
	 * on, the client auto-attacks each cycle, so this just re-acquires her after movement (e.g. a
	 * tentacle dodge) broke the lock. No-op while she's dormant (12203) or absent.
	 */
	private void maintainBossAttack(int tick)
	{
		if (!isWhispererAwake()) return;
		// Pause DPS only during a REAL screech (pillars captured). NOT inScreechPhase() -- that reads true for a
		// few ticks after the last seed (anim 10252), which would falsely pause DPS post-seeds. During a real
		// screech handleScreechPhase owns the tick upstream, so this is just a backstop.
		if (!hidePillarChainOwTiles.isEmpty())
		{
			step("Screech", "screech: DPS paused");
			return;
		}
		Player player = mirror.getLocalPlayer();
		if (player == null) return;
		if (player.getInteracting() != whisperer && tick - lastAttackTick >= effectiveCooldown())
		{
			attackWhisperer();
			lastAttackTick = tick;
			step("Attacking", "attacking whisperer (cd " + effectiveCooldown() + "t)");
		}
	}

	/** Hazard tiles for the dodge: the LINE of tiles between us and each tentacle (12208). The tentacle attacks
	 *  straight inward toward us, so every tile on the line from the tentacle to our tile is a hit tile -- we must
	 *  step OFF those lines, not just out of the tentacle's 8-neighbour direction. Computed geometrically (king-move
	 *  line from each tentacle's centre to {@code me}), so it needs no hazard-graphic ids. Same coord space as
	 *  getPlayerTile / the tentacle WorldAreas. */
	private java.util.Set<WorldPoint> hazardTiles(WorldPoint me, java.util.List<NPC> npcs)
	{
		java.util.Set<WorldPoint> out = new java.util.HashSet<>();
		if (me == null || npcs == null) return out;
		for (NPC n : npcs)
		{
			if (n == null || n.getId() != WhispererAutoIDs.NPC_TENTACLE) continue;
			net.runelite.api.coords.WorldArea area = n.getWorldArea();
			if (area == null) continue;
			int fx = area.getX() + area.getWidth() / 2, fy = area.getY() + area.getHeight() / 2;   // tentacle centre
			int x = fx, y = fy;
			for (int guard = 0; (x != me.getX() || y != me.getY()) && guard < 40; guard++)
			{
				out.add(new WorldPoint(x, y, me.getPlane()));   // this line tile is a hit tile
				x += Integer.signum(me.getX() - x);
				y += Integer.signum(me.getY() - y);
			}
		}
		return out;
	}

	/** Mark tiles under the Whisperer's hazard graphics objects as dangerous. */
	private void pushGraphicsHazards(int tick)
	{
		Iterable<GraphicsObject> gfx = client.getGraphicsObjects();
		if (gfx == null) return;
		for (GraphicsObject g : gfx)
		{
			if (g == null) continue;
			boolean hazard = false;
			for (int id : WhispererAutoIDs.GFX_HAZARD_IDS) { if (g.getId() == id) { hazard = true; break; } }
			if (!hazard) continue;

			LocalPoint lp = g.getLocation();
			if (lp == null) continue;
			// fromLocal (not fromLocalInstance) so the hazard tile is in INSTANCE coords and
			// matches mirror.getPlayerTile() -- same convention as Vardorvis spike handling.
			WorldPoint tile = WorldPoint.fromLocal(client, lp);
			if (tile == null) continue;

			dodger.addHazard(Hazard.staticTile(tile, tick,     "whis-hazard"));
			dodger.addHazard(Hazard.staticTile(tile, tick + 1, "whis-hazard"));
			dodger.addHazard(Hazard.staticTile(tile, tick + 2, "whis-hazard"));
		}
	}

	/**
	 * Leviathan-style prayer handling, restricted to MAGIC / RANGE (the Whisperer has no
	 * melee attack to pray). For every prayable projectile in flight, schedule its overhead
	 * one tick before impact (deduped). When nothing prayable is in flight we HOLD the last
	 * overhead rather than flicking it off.
	 * <p>
	 * No-op until {@link WhispererAutoIDs#PROJ_MAGIC} / {@link WhispererAutoIDs#PROJ_RANGE}
	 * are set to the real ids.
	 */
	private void watchPrayerProjectiles(int tick)
	{
		if (whisperer == null || deathPrayersOff) return;

		java.util.List<MirrorState.ProjectileRecord> pending = new java.util.ArrayList<>();
		for (MirrorState.ProjectileRecord pr : mirror.getProjectileRecords())
		{
			boolean prayable = pr.id == WhispererAutoIDs.PROJ_MAGIC
				|| pr.id == WhispererAutoIDs.PROJ_RANGE;
			if (!prayable) continue;
			if (pr.endTick > tick) pending.add(pr);   // schedule a swap for a future impact
		}

		// Prune dedupe entries for impacts that have already passed.
		final int currentTick = tick;
		scheduledPrayIdentities.removeIf(id -> ((id >> 16) & 0xFFFF) <= currentTick);

		if (pending.isEmpty())
		{
			// Nothing prayable in flight. During seed / screech / siphon she throws no prayable attacks, so
			// turn ALL prayers off to save points instead of holding them. (If a prayable projectile IS in
			// flight during an entry gap, pending is non-empty and we still pray it -- we only drop when
			// idle.) Outside those phases, hold the current overhead.
			if (seedPhaseActive || inScreechPhase() || inSiphonPhase()) turnPrayersOff(tick);
			return;
		}

		pending.sort((a, b) -> Integer.compare(a.endTick, b.endTick));
		for (MirrorState.ProjectileRecord pr : pending)
		{
			int identity = (pr.endTick << 16) ^ (pr.id & 0xFFFF);
			if (!scheduledPrayIdentities.add(identity)) continue;   // already queued

			Prayer target = (pr.id == WhispererAutoIDs.PROJ_RANGE)
				? Prayer.PROTECT_FROM_MISSILES
				: Prayer.PROTECT_FROM_MAGIC;

			// Latest safe swap tick: 1 tick before impact (or NOW if we're already there).
			prayers.scheduleSwap(target, Math.max(tick, pr.endTick - 1));
		}
	}

	/** Drop any scheduled overhead swap and deactivate ALL prayers (for now -- later we may keep some,
	 *  e.g. offensive ones, on during these phases). */
	private void turnPrayersOff(int tick)
	{
		prayers.clearScheduled();
		prayers.ensureOnlyOn(tick, java.util.Collections.emptySet(),
			java.util.EnumSet.allOf(Prayer.class));
	}

	// -----------------------------------------------------------------
	// Consumption (ranged).
	// -----------------------------------------------------------------

	/** Live supply handling: build the request (prayer sip rules + the engine's default opportunistic shark eat),
	 *  evaluate against current HP / prayer / inventory, and fire the eat/drink clicks. Called every active tick. */
	private void handleConsumption(int tick)
	{
		ConsumptionEngine.Request req = buildConsumptionRequest();
		ConsumptionEngine.Result res = engine.evaluate(req);
		if (res != null && res.hasAction() && config != null && config.captureMode())
			log.info("[supply] tick={} hp={}/{} pray={}/{} -> drink={} eat={} combo={}",
				tick, mirror.getHp(), mirror.getMaxHp(), mirror.getPrayer(), mirror.getMaxPrayer(),
				res.drink, res.primaryEat == null ? null : res.primaryEat.itemId,
				res.comboEat == null ? null : res.comboEat.itemId);
		applyConsumption(res);
		// DPS thrall upkeep (6 prayer/cast, ~magic-level ticks): recast ASAP once expired, but ONLY in the
		// overworld normal-fight window (thrallConditionsOk) -- she spends parts of the fight in the shadow
		// realm / another region or in specials where a cast is wasted or steals a mechanic tick. Skip if we
		// just drank/ate (one CC_OP per tick); bump lastAttackTick so the cast suppresses this tick's swing.
		boolean acted = res != null && res.hasAction();
		if (!acted && thrall.maintain(tick, mirror, router, thrallConditionsOk()))
			lastAttackTick = tick;
	}

	/**
	 * Whisperer thrall gate: simply being in the OVERWORLD arena. That covers all the cases we want -- before
	 * poking the Odd Figure (she's still dormant), a timer expiry in the overworld, and having no thrall up --
	 * and excludes the shadow realm / another region, where a cast is wasted. We do NOT exclude specials: the
	 * ideal moments are the downtime they create (hiding behind pillars, sitting on a flee tile), and otherwise
	 * we cast on the first possible overworld tick. ThrallManager.ready() still gates expiry, 'spell lit'
	 * (greyed while a thrall is already active), Arceuus and the 6 prayer. Style via THRALL_CAST_WIDGET.
	 */
	private boolean thrallConditionsOk()
	{
		// Only once she's AWAKE (12204 spawned) -- NOT before the wake: a thrall summoned while she's dormant
		// or mid-knockback would burn its ~magic-level-tick lifespan with nothing to hit. So the first cast
		// lands on the first awake overworld tick, and the full duration is spent on the live boss.
		return inOverworld() && isInArenaTemplate() && isWhispererAwake();
	}

	private ConsumptionEngine.Request buildConsumptionRequest()
	{
		ConsumptionEngine.Request.Builder b = ConsumptionEngine.Request.builder();

		// Magic kill -- mostly we just need PRAYER, which sips every tick it's low (below ~45, jittered). EATING
		// is the only thing that would delay a cast, so gate it: eat at/below the critical floor (safety) OR
		// at/below the HP threshold (~40) AND in an idle window (flee / behind a pillar) -- never mid-cast otherwise.
		int hp = mirror.getHp();
		boolean needFood = hp <= WhispererSupply.PROFILE.hpEatCriticalThreshold
			|| (hp <= WhispererSupply.PROFILE.hpEatThreshold && consumeIdleWindow(mirror.getTickCounter()));
		b.wantEat(needFood);
		b.allowComboEat(needFood);

		// Prayer: sip around 45 (jittered) whenever low; when we're also eating, piggyback a free top-up.
		ConsumptionEngine.requestPrayerSip(b, mirror, 45,
			WhispererSupply.PROFILE.prayerSipJitterWindow, needFood);

		// TODO: offensive magic boost top-off + any utility potions (stamina, etc.) go here.

		return b.build();
	}

	private void applyConsumption(ConsumptionEngine.Result r)
	{
		if (r == null || !r.hasAction()) return;
		if (r.drink      != null) router.useInventoryItem(bestDoseId(r.drink), "Drink");
		if (r.primaryEat != null) router.useInventoryItem(r.primaryEat.itemId, "Eat");
		if (r.comboEat   != null) router.useInventoryItem(r.comboEat.itemId, "Eat");
	}

	/** Pick the LOWEST available dose id for a potion kind, so partial (1/2/3-dose) vials drain first and
	 *  empty out of the inventory before we touch full 4-dose ones -- consolidating supplies and freeing
	 *  slots over a long trip instead of leaving a spread of part-used vials. */
	private int bestDoseId(SupplyProfile.DrinkKind k)
	{
		SupplyProfile.PotionEntry pot = WhispererSupply.PROFILE.potion(k);
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

	private void doAttack(int tick)
	{
		if (whisperer == null) return;
		Player player = mirror.getLocalPlayer();
		if (player == null) return;
		// Re-click only when we're not already interacting with the boss, spaced by the
		// weapon's attack speed (config-driven: see WhispererWeapon / the override configs).
		// Out of range is fine -- the attack click auto-walks us into range; effectiveRange()
		// is exposed for future movement logic (e.g. attack-vs-dodge decisions). TODO: if the
		// fight needs precise off-tick DPS timing, anchor to our own cooldown like Duke does.
		if (player.getInteracting() != whisperer && tick - lastAttackTick >= effectiveCooldown())
		{
			attackWhisperer();
			lastAttackTick = tick;
			step("Attacking", 
				(phase.current() == WhisPhase.FIGHT ? "attacking" : "poking")
					+ " whisperer (cd " + effectiveCooldown() + "t"
					+ (isInAttackRange(whisperer) ? "" : ", oor") + ")");
		}
	}

	/** True if the player is within {@link #effectiveRange()} Chebyshev tiles of the boss
	 *  footprint. Informational for now -- attack clicks auto-walk us in regardless. */
	private boolean isInAttackRange(NPC npc)
	{
		if (npc == null) return false;
		WorldPoint me = mirror.getPlayerTile();
		if (me == null) return false;
		net.runelite.api.coords.WorldArea area = npc.getWorldArea();
		if (area == null) return false;
		return area.distanceTo(me) <= effectiveRange();
	}

	// -----------------------------------------------------------------
	// Helpers.
	// -----------------------------------------------------------------

	/** The Whisperer in either state (dormant 12203 or awake 12204), whichever is present. */
	/** True if ANY npc in the scene is playing {@code animId} -- id-agnostic, for her death animation (10260),
	 *  which may play on a form we don't track as the whisperer. */
	private boolean anyNpcHasAnim(int animId)
	{
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return false;
		for (NPC n : npcs) if (n != null && n.getAnimation() == animId) return true;
		return false;
	}

	private NPC findWhisperer()
	{
		return findNpcAny(WhispererAutoIDs.NPC_WHISPERER_AWAKE, WhispererAutoIDs.NPC_WHISPERER_DORMANT);
	}

	/** True once the Whisperer has flipped to her awake/fighting id. */
	private boolean isWhispererAwake()
	{
		return whisperer != null && whisperer.getId() == WhispererAutoIDs.NPC_WHISPERER_AWAKE;
	}

	/**
	 * Sanity readout from {@link WhispererAutoIDs#SANITY_WIDGET_ID}: parses the integer out
	 * of text like "Sanity: 100%". Returns -1 when the widget is absent / unreadable.
	 */
	private int sanityPercent()
	{
		Widget w = client.getWidget(WhispererAutoIDs.SANITY_WIDGET_ID);
		if (w == null || w.isHidden()) return -1;
		String t = w.getText();
		if (t == null) return -1;
		StringBuilder digits = new StringBuilder();
		for (int i = 0; i < t.length(); i++)
		{
			char c = t.charAt(i);
			if (c >= '0' && c <= '9') digits.append(c);
			else if (digits.length() > 0) break;  // stop at the end of the first number
		}
		if (digits.length() == 0) return -1;
		try { return Integer.parseInt(digits.toString()); }
		catch (NumberFormatException e) { return -1; }
	}

	/** Return the first NPC whose id matches any of {@code ids}. Null if none present. */
	private NPC findNpcAny(int... ids)
	{
		List<NPC> npcs = client.getNpcs();
		if (npcs == null) return null;
		for (NPC n : npcs)
		{
			if (n == null) continue;
			for (int id : ids) if (id > 0 && n.getId() == id) return n;
		}
		return null;
	}

	private void updateOverlay()
	{
		if (overlay == null) return;
		overlay.setStat("active", active ? "yes" : "no");
		overlay.setStat("phase",  phase.current().name());
		overlay.setStat("hp",     mirror.getHp()     + "/" + mirror.getMaxHp());
		overlay.setStat("pray",   mirror.getPrayer() + "/" + mirror.getMaxPrayer());
		int san = sanityPercent();
		overlay.setStat("sanity", san < 0 ? "?" : san + "%");
		overlay.setStat("tick",   String.valueOf(mirror.getTickCounter()));
	}

	private void resolveInstanceTilesIfNeeded(int tick)
	{
		WorldPoint pt = mirror.getPlayerTile();
		WorldPoint tpl = InstanceCoords.toTemplate(client, pt);
		boolean inArena = tpl != null && isArenaRegion(tpl.getRegionID());
		if (!inArena) return;
		if (tick - lastResolveTick < 20) return;
		// TODO: resolve Whisperer-specific template tiles (e.g. a safe/attack spot) here.
		lastResolveTick = tick;
	}

	/** Template-space check: are we in the Whisperer arena (either region)? */
	private boolean isInArenaTemplate()
	{
		WorldPoint tpl = InstanceCoords.toTemplate(client, mirror.getPlayerTile());
		return tpl != null && isArenaRegion(tpl.getRegionID());
	}

	/** True if {@code regionId} is one of the Whisperer arena regions. */
	private boolean isArenaRegion(int regionId)
	{
		for (int r : WhispererAutoIDs.ARENA_REGIONS) if (r == regionId) return true;
		return false;
	}
}
