package net.runelite.client.plugins.dt2LeviathanAuto;

import net.runelite.api.coords.WorldPoint;

/**
 * Constants for Leviathan's fight. All IDs below are PLACEHOLDERS -- fill in via the
 * capture logger ({@link net.runelite.client.plugins.autoBossCore.debug.CaptureLogger})
 * once the first run of the plugin dumps real mechanic ids.
 * <p>
 * Supply item ids reference gameval ItemID constants (same pattern as Vardorvis).
 */
public final class LeviathanAutoIDs
{
	private LeviathanAutoIDs() {}

	// --- Boss NPCs ---------------------------------------------------
	/** Regular Leviathan. */
	public static final int NPC_LEVIATHAN          = 12214;
	/** Quest-encounter Leviathan. TODO: capture id. */
	public static final int NPC_LEVIATHAN_QUEST    = -1;
	/** Awakened (hard mode) Leviathan. TODO: capture id. */
	public static final int NPC_LEVIATHAN_AWAKENED = -1;

	// --- Projectiles -------------------------------------------------
	/**
	 * Leviathan's rotating three-attack prayable projectiles. Each has a prayer that
	 * must be ON before impact.
	 */
	public static final int PROJ_MELEE = 2488;   // Protect from Melee
	public static final int PROJ_RANGE = 2487;   // Protect from Missiles
	public static final int PROJ_MAGIC = 2489;   // Protect from Magic

	// --- Leviathan animations ---------------------------------------
	/**
	 * "Roar" animation. Fires on the special-attack telegraph (chain-stomp / drag).
	 * TODO: confirm what the roar actually prefaces mechanically.
	 */
	public static final int ANIM_ROAR = 10282;

	/**
	 * Lightning-phase charge animation. Fires when Leviathan starts the lightning
	 * phase (the "winding up" before projectiles). Used to latch
	 * {@link net.runelite.client.plugins.dt2LeviathanAuto.LeviathanAutoMain} into
	 * "lightning phase is actually running" so the shared phase-2 end signal
	 * (ANIM_PHASE2_END) can be gated.
	 */
	public static final int ANIM_LIGHTNING_CHARGE = 10285;

	/**
	 * Lightning-attack animation. Fires while Leviathan is shooting lightning
	 * projectiles (one per shot, repeated throughout the phase).
	 */
	public static final int ANIM_LIGHTNING = 10286;

	/**
	 * Boulder-phase start animation. Fires once at the beginning of boulder phase.
	 * Leviathan then launches 10 boulder projectiles over the course of the phase.
	 */
	public static final int ANIM_BOULDER_PHASE_START = 10288;

	/**
	 * Boulder-phase smoke-blast attack animation. Fires while Leviathan is actually
	 * delivering the finisher blast after projectile 10. Not used for a dedicated
	 * detection latch -- the shared {@link #ANIM_PHASE2_END} (10287) signal handles
	 * phase-2 ending for both boulder and lightning. Kept as a reference constant
	 * in case we need to telegraph the blast earlier (e.g. for sound/overlay cues).
	 */
	public static final int ANIM_SMOKE_BLAST = 10290;

	/**
	 * SHARED phase-2 end animation. Plays while phase 2 is wrapping up:
	 * <ul>
	 *   <li>Boulder phase: this IS the smoke-blast attack -- active = blast incoming.</li>
	 *   <li>Lightning phase: this is the "stop" / "windown" after the final shot.</li>
	 * </ul>
	 * In either case the detection pattern is the same: the moment Leviathan's
	 * animation transitions OUT of 10287, phase 2 has fully resolved and the
	 * planner moves on via the dynamic phase-2-end lookup.
	 */
	public static final int ANIM_PHASE2_END = 10287;

	/**
	 * Diagnostic reference only: the "canonical" round-2 section-1 start tile
	 * (south-step down the east column from here). The planner no longer uses
	 * this constant directly -- the next-cycle start tile is looked up dynamically
	 * from LeviathanAutoMain#PHASE2_END_TO_NEXT_SECTION, keyed on wherever
	 * phase 2 actually ended.
	 */
	public static final WorldPoint PHASE2_RESET_TILE = new WorldPoint(2090, 6380, 0);

	/** Number of boulder projectiles Leviathan launches once ANIM_BOULDER_PHASE_START fires. */
	public static final int BOULDER_PROJECTILE_COUNT = 10;

	/**
	 * Attack-eligible cluster indexes for SAFE-GAP boulder clusters (end tile is
	 * attackable). Attacks fire at: pos 1 (count 0), pos 3 (count 4), pos 6
	 * backfill (count 10 after skipping pos 5 for cooldown), then WALKBACK to
	 * pos 5 (count ~14). The walkback is handled as a separate state in
	 * LeviathanAutoMain, not an index here. {@code {0, 2, 5}} covers the three
	 * primary count-triggered attacks.
	 */
	public static final int[] BOULDER_ATTACK_IDXS_SAFE_GAP = {0, 2, 5};

	/**
	 * Attack-eligible cluster indexes for UNSAFE-GAP boulder clusters (SE). No
	 * attack from the end-stand tile; the arrival at pos 5 is the last shot of
	 * the sequence (count 8, cooldown ready). {@code {0, 2, 4}} = pos 1, pos 3,
	 * pos 5. After pos 5, the player walks to the safe-stand tile and waits out
	 * the smoke blast with no further attacks.
	 */
	public static final int[] BOULDER_ATTACK_IDXS_UNSAFE_GAP = {0, 2, 4};

	/**
	 * Full attack-idx set: fire on both the gap-jump tile (idx 4) AND the backfill
	 * tile (idx 5). Does NOT trigger walkback -- the backfill shot IS the 4th shot.
	 * Used by NE where the end tile is safe to stand on and the extra attack fits
	 * cleanly inside the weapon cooldown after the gap-jump shot.
	 */
	public static final int[] BOULDER_ATTACK_IDXS_FULL_GAP = {0, 2, 4, 5};

	/**
	 * Per-cluster attack-idx array, parallel to {@link #BOULDER_CLUSTERS}. Each
	 * cluster can pick its own pattern independent of the walkback flag.
	 */
	public static final int[][] BOULDER_CLUSTER_ATTACK_IDXS = {
		BOULDER_ATTACK_IDXS_FULL_GAP,   // NE: 0,2,4,5 -- end tile safe + attack from it
		BOULDER_ATTACK_IDXS_FULL_GAP,   // SE: 0,2,4,5 -- end tile (2091,6369) now safe + attack from it
		BOULDER_ATTACK_IDXS_SAFE_GAP,   // SW: 0,2,5 + walkback (pending verification)
		BOULDER_ATTACK_IDXS_SAFE_GAP,   // NW: 0,2,5 + walkback (pending verification)
	};

	/**
	 * Boulder-phase dodge clusters -- 6 ordered tiles each, keyed by cluster.
	 * Position 1 (index 0) is the arrival/initial stance tile. Positions 2-4
	 * (indexes 1-3) are 1-tile shuffle moves. Position 5 (index 4) is the "gap
	 * jump" -- 2 tiles away, skipping one that's hazardous mid-sequence. Position
	 * 6 (index 5) is the "backfill" -- the skipped gap tile, safe to stand on
	 * once we've advanced past it. The planner reads this by:
	 * {@code tile = cluster[Math.min(boulderProjectileCount / 2, 5)]} -- each
	 * even projectile count advances one position; odd counts stay put.
	 * <p>
	 * Which cluster is used each boulder phase is picked dynamically by the main
	 * class: closest cluster[0] to the player's current tile when the spine
	 * attack lands.
	 */
	public static final WorldPoint[] BOULDER_NE = {
		new WorldPoint(2084, 6381, 0),
		new WorldPoint(2085, 6381, 0),
		new WorldPoint(2086, 6381, 0),
		new WorldPoint(2087, 6381, 0),
		new WorldPoint(2089, 6381, 0),  // gap jump
		new WorldPoint(2088, 6381, 0),  // backfill
	};
	public static final WorldPoint[] BOULDER_SE = {
		new WorldPoint(2089, 6368, 0),
		new WorldPoint(2089, 6367, 0),
		new WorldPoint(2089, 6366, 0),
		new WorldPoint(2089, 6365, 0),
		new WorldPoint(2089, 6363, 0),  // gap jump
		new WorldPoint(2091, 6369, 0),  // safe end-stand tile -- attack Leviathan from here (idx 5)
	};
	public static final WorldPoint[] BOULDER_SW = {
		new WorldPoint(2077, 6364, 0),
		new WorldPoint(2076, 6364, 0),
		new WorldPoint(2075, 6364, 0),
		new WorldPoint(2074, 6364, 0),
		new WorldPoint(2072, 6364, 0),  // gap jump
		new WorldPoint(2073, 6364, 0),  // backfill
	};
	public static final WorldPoint[] BOULDER_NW = {
		new WorldPoint(2073, 6376, 0),
		new WorldPoint(2073, 6377, 0),
		new WorldPoint(2073, 6378, 0),
		new WorldPoint(2073, 6379, 0),
		new WorldPoint(2073, 6381, 0),  // gap jump
		new WorldPoint(2073, 6380, 0),  // backfill
	};

	/** All boulder clusters, in a scanning array so the picker can enumerate. */
	public static final WorldPoint[][] BOULDER_CLUSTERS = { BOULDER_NE, BOULDER_SE, BOULDER_SW, BOULDER_NW };
	public static final String[]      BOULDER_CLUSTER_NAMES = { "NE", "SE", "SW", "NW" };

	/**
	 * Parallel to BOULDER_CLUSTERS. Controls walkback behavior AFTER idx-5 attack:
	 * <ul>
	 *   <li>true  = after firing from idx 5, walk back to idx 4 for one more shot
	 *               (SAFE_GAP + walkback pattern used by SW/NW).</li>
	 *   <li>false = no walkback; the cluster's attack pattern is self-contained
	 *               (used by NE's FULL_GAP {0,2,4,5} and SE's UNSAFE_GAP {0,2,4}).</li>
	 * </ul>
	 * Attack-idx pattern is now controlled by {@link #BOULDER_CLUSTER_ATTACK_IDXS}
	 * independently of this flag.
	 */
	public static final boolean[] BOULDER_CLUSTER_WALKBACK = { false, false, true, true };

	/** Boulder projectile id -- observed in-fight. */
	public static final int PROJ_BOULDER = 2472;

	// --- Hazard graphics --------------------------------------------
	/**
	 * BREAKABLE rock graphics objects. Transient damage patches -- harmful while
	 * active but clear away on their own without leaving a persistent obstacle.
	 */
	public static final int[] GFX_BREAKABLE_ROCK_IDS = { 2475, 2476 };

	/**
	 * UNBREAKABLE rock graphics objects. Each landing becomes a persistent
	 * {@link #GOBJ_ROCK} (47590) obstacle for the rest of the fight. These are the
	 * drops section 1 south-steps away from, and the tiles they land on are
	 * permanently unwalkable afterward. Four visual variants observed in-game, all
	 * sharing the same mechanic.
	 */
	public static final int[] GFX_UNBREAKABLE_ROCK_IDS = { 2477, 2478, 2479, 2480 };

	/** True if {@code id} is one of the breakable (transient) rock graphics IDs. */
	public static boolean isBreakableRockGfx(int id)
	{
		for (int b : GFX_BREAKABLE_ROCK_IDS) if (b == id) return true;
		return false;
	}

	/** True if {@code id} is one of the unbreakable (permanent) rock graphics IDs. */
	public static boolean isUnbreakableRockGfx(int id)
	{
		for (int u : GFX_UNBREAKABLE_ROCK_IDS) if (u == id) return true;
		return false;
	}

	/**
	 * How long (in ticks) a hazard tile stays damaging after the graphics object
	 * appears. Starts as a conservative 3 and can be calibrated via capture mode.
	 * Applies to breakable rocks (they expire after this window); unbreakable rocks
	 * are tracked permanently via the GameObject scan.
	 */
	public static final int HAZARD_ACTIVE_TICKS = 3;

	// Fall-to-impact: how many ticks after a rock GFX SPAWNS the rock actually lands and
	// deals damage. The GFX being on a tile is the falling animation -- the tile is only
	// dangerous at impact (spawnTick + this), NOT while the rock is still in the air, and
	// it is safe again once impact has passed. Tune from the [lev-rockgfx] diagnostic:
	// watch a drop and set this to (tick the rock lands) - (tick its GFX first appeared).
	public static final int ROCK_FALL_TO_IMPACT_TICKS = 6;  // measured: all rock GFX = 6 (to confirm)

	// How many ticks the landed rock actually damages its tile, starting at impact. The
	// falling-rock spotanim finishes (and despawns) at impact, so for a breakable rock the
	// tile is safe the moment it lands -- the hazard is essentially the single impact tick.
	// Kept as a constant for slop/tuning; raise only if a drop is seen to damage for longer.
	public static final int ROCK_IMPACT_ACTIVE_TICKS = 2;  // 2-tick window hedges the +/-1 uncertainty in ROCK_FALL_TO_IMPACT_TICKS so we don't step back onto a tile a tick before the real landing

	/**
	 * Time-to-impact (ticks from spawn to landing) for a falling-rock GFX id. Every falling
	 * hazard is marked at its own impact via this lookup; split the cases out if a given
	 * rock is observed to fall faster or slower than the others. Returns
	 * {@link #ROCK_FALL_TO_IMPACT_TICKS} for all known rock GFX, -1 for anything else.
	 */
	public static int rockFallToImpactTicks(int gfxId)
	{
		if (isBreakableRockGfx(gfxId) || isUnbreakableRockGfx(gfxId)) return ROCK_FALL_TO_IMPACT_TICKS;
		return -1;
	}

	// --- GameObjects ------------------------------------------------
	/**
	 * "Rock" obstacle spawned by GFX_HAZARD_ROCK_DROP (2479) landing. These persist for
	 * the rest of the fight once dropped, so each rock permanently blocks its tile. Also
	 * present as part of the arena's static scenery.
	 */
	public static final int GOBJ_ROCK = 47590;

	/**
	 * DT2 Leviathan arena "Handholds" game object. Its first (left-click) option is
	 * "Climb", used to enter/exit the arena. Captured menuAction:
	 * {@code GAME_OBJECT_FIRST_OPTION id=47592 itemId=-1 param0=54 param1=48
	 * option='Climb' target='<col=ffff>Handholds'} (param0/param1 are instance-relative
	 * SCENE coords and are derived live from the object, not hardcoded).
	 */
	public static final int GOBJ_HANDHOLDS = 47592;

	/**
	 * Packed component id of the boss HP readout widget (group 303 / child 20). Its
	 * {@code getText()} reads like "700/700" -- parse with
	 * {@link net.runelite.client.plugins.autoBossCore.state.WidgetReader#fraction}.
	 */

	/**
	 * Below this boss HP, section-1 roars are done from the north-row finisher spot
	 * {@link LeviathanAutoMain#LOW_HP_ROAR_CONFIG} instead of the mapped lane. Checked
	 * only at a roar-phase START (never mid-roars).
	 */
	public static final int LOW_HP_ROAR_THRESHOLD = 250;

	/** Template tiles from which the player climbs the {@link #GOBJ_HANDHOLDS}. */
	public static final WorldPoint[] HANDHOLDS_CLIMB_TILES_TEMPLATE = {
		new WorldPoint(2067, 6370, 0),
		new WorldPoint(2069, 6368, 0),
	};

	// --- Arena ------------------------------------------------------
	/** Leviathan instance region id. */
	public static final int ARENA_REGION = 8291;

	/**
	 * Bounding box (TEMPLATE coords) of the mapped-out arena -- the extent of
	 * {@code ARENA_INACCESSIBLE} walls (X 2069..2093, Y 6361..6384). "Within the walls"
	 * means inside this box; the overworld Handholds approach (e.g. 2067,6370) is west
	 * of it. Used to tell the playable arena apart from the rest of the instance, since
	 * both share the region id and isInInstancedRegion() can't distinguish them.
	 */
	public static final int ARENA_MIN_X = 2069, ARENA_MAX_X = 2093;
	public static final int ARENA_MIN_Y = 6361, ARENA_MAX_Y = 6384;

	/** True if {@code tpl} is inside the mapped arena bounding box. Null-safe. */
	public static boolean isWithinArenaTemplate(WorldPoint tpl)
	{
		if (tpl == null) return false;
		int x = tpl.getX(), y = tpl.getY();
		return x >= ARENA_MIN_X && x <= ARENA_MAX_X && y >= ARENA_MIN_Y && y <= ARENA_MAX_Y;
	}

	/**
	 * Starting tile the plugin positions the player on at fight entry. Leviathan is a
	 * MOBILE fight -- there is no static home tile for the whole kill. After the first
	 * rotation the ideal anchor shifts, so this is only the kick-off position.
	 */
	public static final WorldPoint START_TILE_TEMPLATE = new WorldPoint(2071, 6377, 0);

	/**
	 * Home tiles the plugin parks the player on while waiting for Leviathan to spawn.
	 * When more than one is listed the plugin repositions to whichever is CLOSEST to
	 * the player's current tile (the arena is large, so a second home tile near the
	 * far entry saves a long pre-fight walk). {@link #START_TILE_TEMPLATE} is the
	 * canonical first home and is also the round-1 south-step start.
	 */
	public static final WorldPoint[] HOME_TILES_TEMPLATE = {
		START_TILE_TEMPLATE,
	};

	/**
	 * Arena entry tiles -- where the player lands after climbing in. While Leviathan is
	 * not yet alive and the player is on one of these, the plugin walks them to the
	 * closest {@link #HOME_TILES_TEMPLATE} so we're set up for fight start.
	 */
	public static final WorldPoint[] ARENA_ENTRY_TILES_TEMPLATE = {
		new WorldPoint(2071, 6368, 0),
	};

	// --- Spells ------------------------------------------------------
	/**
	 * Spellbook widget id for Shadow Rush. Captured from an in-game menu-option log:
	 * {@code action=WIDGET_TARGET param1=14286944 option='Cast' target='Shadow Rush'}.
	 * Used by section 2 once we've reached the southernmost section-1 tile.
	 */
	public static final int SPELL_SHADOW_RUSH_WIDGET = 14286944;
	public static final String SPELL_SHADOW_RUSH_NAME = "Shadow Rush";

	/**
	 * Player animation that fires when we successfully cast Shadow Rush. Used as the
	 * edge trigger for the section-2 post-cast movement -- after this animation we
	 * walk to the next anchor tile (TBD).
	 */
	public static final int ANIM_CAST_SHADOW_RUSH = 10092;

	/**
	 * Lightning-phase anchor tiles. Two options sit on the east side of the arena.
	 * After Shadow Rush, Leviathan is facing semi-south-west, so his spine (the
	 * weak spot) faces north-east -- {@link #EAST_NORTH_LIGHTNING} lets us land
	 * our shots on it. {@link #EAST_SOUTH_LIGHTNING} is the alternate position in
	 * the lightning rotation; usage rules TBD (will fill in once wired up).
	 * <p>
	 * Boulder-phase anchor tiles will be a separate pair, filled in later.
	 */
	// Lightning-phase lane tiles. Four lanes (N, S, W, E), each a straight line
	// of 9 tiles. The named constants mark the two ends and the center of each
	// lane; the lane arrays below hold all 9 tiles for pickers / iteration.

	// East lane (x=2086, y=6368..6376) -- the one we use post-Shadow-Rush when
	// Leviathan faces semi-SW (cycle 0 and even cycles).
	public static final WorldPoint EAST_NORTH_LIGHTNING  = new WorldPoint(2086, 6376, 0);
	public static final WorldPoint EAST_CENTER_LIGHTNING = new WorldPoint(2086, 6372, 0);
	public static final WorldPoint EAST_SOUTH_LIGHTNING  = new WorldPoint(2086, 6368, 0);

	/**
	 * Scripted roar order for the FIRST east-lightning phase-2 follow-up. A straight
	 * west lane is blocked by the wall at (2084-2086,6363), so the roars zigzag between
	 * y=6363 and y=6364 as they march west. Driven as an explicit ordered path (not a
	 * straight SectionConfig). Only used for the first east lightning; later lightning
	 * phases keep the (2090,6380) south column.
	 */
	public static final WorldPoint[] EAST_LIGHTNING_P2_ROAR_PATH = {
		new WorldPoint(2088, 6363, 0),
		new WorldPoint(2088, 6364, 0),
		new WorldPoint(2087, 6363, 0),
		new WorldPoint(2087, 6364, 0),
		new WorldPoint(2086, 6364, 0),
		new WorldPoint(2085, 6364, 0),
	};

	// West lane (x=2076, y=6368..6376) -- mirror of east, used on odd cycles when
	// Leviathan is faced from the east column post-Shadow-Rush.
	public static final WorldPoint WEST_NORTH_LIGHTNING  = new WorldPoint(2076, 6376, 0);
	public static final WorldPoint WEST_CENTER_LIGHTNING = new WorldPoint(2076, 6372, 0);
	public static final WorldPoint WEST_SOUTH_LIGHTNING  = new WorldPoint(2076, 6368, 0);

	// North lane (y=6377, x=2077..2085).
	public static final WorldPoint NORTH_WEST_LIGHTNING   = new WorldPoint(2077, 6377, 0);
	public static final WorldPoint NORTH_CENTER_LIGHTNING = new WorldPoint(2081, 6377, 0);
	public static final WorldPoint NORTH_EAST_LIGHTNING   = new WorldPoint(2085, 6377, 0);

	// South lane (y=6367, x=2077..2085).
	public static final WorldPoint SOUTH_WEST_LIGHTNING   = new WorldPoint(2077, 6367, 0);
	public static final WorldPoint SOUTH_CENTER_LIGHTNING = new WorldPoint(2081, 6367, 0);
	public static final WorldPoint SOUTH_EAST_LIGHTNING   = new WorldPoint(2085, 6367, 0);

	/** All 9 tiles of the east lightning lane (end to end, including center). */
	public static final WorldPoint[] EAST_LIGHTNING_LANE = {
		new WorldPoint(2086, 6368, 0), new WorldPoint(2086, 6369, 0),
		new WorldPoint(2086, 6370, 0), new WorldPoint(2086, 6371, 0),
		new WorldPoint(2086, 6372, 0), new WorldPoint(2086, 6373, 0),
		new WorldPoint(2086, 6374, 0), new WorldPoint(2086, 6375, 0),
		new WorldPoint(2086, 6376, 0),
	};
	/** All 9 tiles of the west lightning lane. */
	public static final WorldPoint[] WEST_LIGHTNING_LANE = {
		new WorldPoint(2076, 6368, 0), new WorldPoint(2076, 6369, 0),
		new WorldPoint(2076, 6370, 0), new WorldPoint(2076, 6371, 0),
		new WorldPoint(2076, 6372, 0), new WorldPoint(2076, 6373, 0),
		new WorldPoint(2076, 6374, 0), new WorldPoint(2076, 6375, 0),
		new WorldPoint(2076, 6376, 0),
	};
	/** All 9 tiles of the north lightning lane. */
	public static final WorldPoint[] NORTH_LIGHTNING_LANE = {
		new WorldPoint(2077, 6377, 0), new WorldPoint(2078, 6377, 0),
		new WorldPoint(2079, 6377, 0), new WorldPoint(2080, 6377, 0),
		new WorldPoint(2081, 6377, 0), new WorldPoint(2082, 6377, 0),
		new WorldPoint(2083, 6377, 0), new WorldPoint(2084, 6377, 0),
		new WorldPoint(2085, 6377, 0),
	};
	/** All 9 tiles of the south lightning lane. */
	public static final WorldPoint[] SOUTH_LIGHTNING_LANE = {
		new WorldPoint(2077, 6367, 0), new WorldPoint(2078, 6367, 0),
		new WorldPoint(2079, 6367, 0), new WorldPoint(2080, 6367, 0),
		new WorldPoint(2081, 6367, 0), new WorldPoint(2082, 6367, 0),
		new WorldPoint(2083, 6367, 0), new WorldPoint(2084, 6367, 0),
		new WorldPoint(2085, 6367, 0),
	};

	/** All 4 lightning lanes, in NE/SE/SW/NW scanning order (matches BOULDER_CLUSTERS). */
	public static final WorldPoint[][] LIGHTNING_LANES = {
		EAST_LIGHTNING_LANE, SOUTH_LIGHTNING_LANE,
		WEST_LIGHTNING_LANE, NORTH_LIGHTNING_LANE,
	};
	public static final String[] LIGHTNING_LANE_NAMES = { "EAST", "SOUTH", "WEST", "NORTH" };

	// BOULDER_POST_SPINE removed -- the active boulder cluster's position 1 tile
	// is picked dynamically now; see LeviathanAutoMain#pickBoulderClusterIfNeeded.

	/**
	 * Spotanim id Leviathan plays when Shadow Rush successfully lands on him
	 * (i.e. NPC.getGraphic() == this value). Confirms the spell connected --
	 * used as the gate for routing to {@link #EAST_CENTER_LIGHTNING} post-cast,
	 * so a splashed cast doesn't send the player chasing a tile that only
	 * makes sense when the Shadow debuff is actually ticking.
	 */
	public static final int GFX_SHADOW_RUSH_IMPACT = 2379;

	// --- Enrage phase (new mechanic) --------------------------------
	/**
	 * Enrage phase start signal. Five of these spotanims appear in a `+` shape
	 * (center + 4 cardinal neighbors). The arm-ends mark the quadrant corners of
	 * the arena -- the quadrant containing the arm NOT shared with the center
	 * tells us where the Abyssal Pathfinder will spawn. See
	 * {@link #NPC_ABYSSAL_PATHFINDER}.
	 *
	 * While this GFX is active, overhead prayers must NOT be forced off (the
	 * Pathfinder attacks us during enrage; we need protection prayers back on).
	 */
	public static final int GFX_ENRAGE_PLUS = 2298;

	/**
	 * Abyssal Pathfinder -- a 3x3 add that spawns in one quadrant of the arena
	 * during enrage phase, triggered by {@link #GFX_ENRAGE_PLUS}. The quadrant
	 * it spawns in dictates which side of the arena square to run to.
	 */
	public static final int NPC_ABYSSAL_PATHFINDER = 12220;

	// --- Enrage phase geometry --------------------------------------
	/** Arena center (template coords). Used to classify the pathfinder's quadrant. */
	public static final WorldPoint ARENA_CENTER_TEMPLATE = new WorldPoint(2081, 6372, 0);

	/**
	 * Enrage destination tiles. Pathfinder travels CLOCKWISE around the arena, so
	 * we stand two cardinal sides clockwise from its spawn quadrant.
	 * <ul>
	 *   <li>NW spawn -> east of center ({@link #ENRAGE_DEST_EAST})</li>
	 *   <li>NE spawn -> south of center ({@link #ENRAGE_DEST_SOUTH})</li>
	 *   <li>SE spawn -> west of center ({@link #ENRAGE_DEST_WEST})</li>
	 *   <li>SW spawn -> north of center ({@link #ENRAGE_DEST_NORTH})</li>
	 * </ul>
	 * Coords mirror the lightning cardinal-center tiles; adjust if the enrage
	 * safe-stand turns out to need different tiles.
	 */
	public static final WorldPoint ENRAGE_DEST_EAST  = new WorldPoint(2086, 6372, 0);
	public static final WorldPoint ENRAGE_DEST_WEST  = new WorldPoint(2076, 6372, 0);
	public static final WorldPoint ENRAGE_DEST_NORTH = new WorldPoint(2081, 6377, 0);
	public static final WorldPoint ENRAGE_DEST_SOUTH = new WorldPoint(2081, 6367, 0);

	// Approximate corners of the pathfinder's clockwise travel rectangle.
	// Direction is determined by which side (top / right / bottom / left) the
	// pathfinder is closest to:
	//   top    y=ENRAGE_PATH_TOP   -> heading EAST  (+X)
	//   right  x=ENRAGE_PATH_RIGHT -> heading SOUTH (-Y)
	//   bottom y=ENRAGE_PATH_BOTTOM-> heading WEST  (-X)
	//   left   x=ENRAGE_PATH_LEFT  -> heading NORTH (+Y)
	public static final int ENRAGE_PATH_TOP    = 6378;
	public static final int ENRAGE_PATH_RIGHT  = 2087;
	public static final int ENRAGE_PATH_BOTTOM = 6366;
	public static final int ENRAGE_PATH_LEFT   = 2075;

	// --- Section 2 phase classification ----------------------------
	/**
	 * Leviathan rolls one of two post-section-1 phases at fight start. The first
	 * roar's rock-drop pattern reveals which:
	 * <ul>
	 *   <li><b>Lightning</b>: extra rocks fall in a 5x5 patch around the WEST and
	 *       EAST pattern-centers.</li>
	 *   <li><b>Boulder</b>: extra rocks fall in a 5x5 patch around the NORTH and
	 *       SOUTH pattern-centers.</li>
	 * </ul>
	 * Our south-step column (x=2071) INTERSECTS the west 5x5 patch, so a drop on
	 * the player's own tile is ambiguous (could be the player-targeted drop or a
	 * west-pattern rock). Classification is unambiguous when we check N and S:
	 * if ANY rock has landed in the N or S patch -> boulder; else -> lightning.
	 */
	public static final WorldPoint PHASE_W_CENTER_TEMPLATE = new WorldPoint(2073, 6372, 0);
	public static final WorldPoint PHASE_E_CENTER_TEMPLATE = new WorldPoint(2089, 6372, 0);
	public static final WorldPoint PHASE_N_CENTER_TEMPLATE = new WorldPoint(2081, 6379, 0);
	public static final WorldPoint PHASE_S_CENTER_TEMPLATE = new WorldPoint(2081, 6365, 0);

	/** Chebyshev radius of a pattern patch -- the 5x5 is center +/- 2. */
	public static final int PHASE_PATCH_RADIUS = 2;

	// --- Section 1 completion -------------------------------------
	/**
	 * Number of roars (= rock-on-player south-step triggers) that constitutes a
	 * complete section 1 for this kill route. Replaces the old Y-threshold
	 * completion check so sightline-blocked tiles can be skipped mid-section
	 * without over- or under-counting.
	 */
	public static final int TOTAL_SECTION_1_ROARS = 6;

	// --- Section 1: diagnostic reference tile ----------------------
	/**
	 * Diagnostic reference only: the tile section 1 would land on with
	 * {@link #TOTAL_SECTION_1_ROARS} clean roars and zero sightline-skips
	 * (START Y=6377 minus 6 south-steps = 6371). Section-1 completion is
	 * driven by the roar counter now, not this tile.
	 */
	public static final WorldPoint SECTION_1_END_TEMPLATE = new WorldPoint(2071, 6371, 0);

	/**
	 * Side-step rectangle (template coords) used when the intended south-tile is
	 * temporarily blocked by a 2475/2476 falling-rock. Scanner picks the Chebyshev-
	 * closest safe tile in this rect, waits out the hazard, then the planner naturally
	 * resumes walking to the real anchor on the next tick.
	 */
	public static final int SECTION_1_SIDESTEP_X_MIN = 2071;
	public static final int SECTION_1_SIDESTEP_X_MAX = 2072;
	public static final int SECTION_1_SIDESTEP_Y_MIN = 6369;
	public static final int SECTION_1_SIDESTEP_Y_MAX = 6377;

	/*
	 * Arena bounding box in TEMPLATE coords. Used by any arena-scan fallback for safe-tile
	 * selection. TODO: fill in once we know the arena dimensions.
	 */
	public static final int ARENA_X_MIN = 0;
	public static final int ARENA_X_MAX = 0;
	public static final int ARENA_Y_MIN = 0;
	public static final int ARENA_Y_MAX = 0;

	// --- Supplies ---------------------------------------------------
	// Fill these in from the gameval ItemID constants once the kill-config is finalised.
	// Mirrors Vardorvis's layout -- shark primary, karambwan combo, prayer / super combat
	// / stamina pots. Leviathan ALSO benefits from an antivenom-plus-plus for the acid
	// phase; add here when needed.
	public static final int FOOD_PRIMARY       = 385;   // Shark
	public static final int FOOD_COMBO         = 3144;  // Cooked karambwan

	public static final int[] PRAYER_POT_DOSES   = { 143, 141, 139, 2434 };  // Prayer potion (1..4)
	public static final int[] RANGING_DOSES      = { 173, 171, 169, 2444 };  // Ranging potion (1..4)
	// Divine ranging potion (1..4). PREFERRED over the regular ranging potion when
	// present -- the engine's DIVINE_RANGE kind outranks RANGING in the drink priority,
	// and only one of the two is ever carried. Either (or neither) may be absent.
	public static final int[] DIVINE_RANGING_DOSES = { 23742, 23739, 23736, 23733 };  // Divine ranging (1..4)
	// Regular ranging potion re-dose threshold: re-drink once the REMAINING Ranged boost
	// (boosted - base) falls BELOW this. Divine ranging ignores this (it's on a 5-min
	// timer). Tune to taste: 1 = only when the boost is fully gone; higher = re-pot sooner.
	public static final int RANGING_REDRINK_MIN_BOOST = 1;
	public static final int[] STAMINA_DOSES      = { -1, -1, -1, -1 };

	// --- Arena accessibility ----------------------------------------
	/**
	 * INACCESSIBLE arena tiles (template coords, plane 0): the octagonal
	 * outer boundary plus the ring around Leviathan's hitbox. The player can
	 * never stand on or walk-click any of these. Exported from the arena
	 * planner. Use {@link #isInaccessibleTemplate(WorldPoint)} for the fast
	 * membership test rather than scanning this array.
	 */
	public static final WorldPoint[] ARENA_INACCESSIBLE = {
		new WorldPoint(2069, 6369, 0),
		new WorldPoint(2069, 6370, 0),
		new WorldPoint(2069, 6371, 0),
		new WorldPoint(2069, 6372, 0),
		new WorldPoint(2069, 6373, 0),
		new WorldPoint(2070, 6366, 0),
		new WorldPoint(2070, 6367, 0),
		new WorldPoint(2070, 6368, 0),
		new WorldPoint(2070, 6369, 0),
		new WorldPoint(2070, 6373, 0),
		new WorldPoint(2070, 6374, 0),
		new WorldPoint(2070, 6375, 0),
		new WorldPoint(2070, 6376, 0),
		new WorldPoint(2070, 6377, 0),
		new WorldPoint(2070, 6378, 0),
		new WorldPoint(2071, 6363, 0),
		new WorldPoint(2071, 6364, 0),
		new WorldPoint(2071, 6365, 0),
		new WorldPoint(2071, 6366, 0),
		new WorldPoint(2071, 6378, 0),
		new WorldPoint(2071, 6379, 0),
		new WorldPoint(2071, 6380, 0),
		new WorldPoint(2071, 6381, 0),
		new WorldPoint(2071, 6382, 0),
		new WorldPoint(2072, 6362, 0),
		new WorldPoint(2072, 6363, 0),
		new WorldPoint(2072, 6382, 0),
		new WorldPoint(2073, 6362, 0),
		new WorldPoint(2073, 6382, 0),
		new WorldPoint(2074, 6362, 0),
		new WorldPoint(2074, 6382, 0),
		new WorldPoint(2074, 6383, 0),
		new WorldPoint(2075, 6361, 0),
		new WorldPoint(2075, 6362, 0),
		new WorldPoint(2075, 6383, 0),
		new WorldPoint(2076, 6361, 0),
		new WorldPoint(2076, 6383, 0),
		new WorldPoint(2077, 6361, 0),
		new WorldPoint(2077, 6369, 0),
		new WorldPoint(2077, 6370, 0),
		new WorldPoint(2077, 6371, 0),
		new WorldPoint(2077, 6372, 0),
		new WorldPoint(2077, 6373, 0),
		new WorldPoint(2077, 6374, 0),
		new WorldPoint(2077, 6375, 0),
		new WorldPoint(2077, 6383, 0),
		new WorldPoint(2078, 6361, 0),
		new WorldPoint(2078, 6368, 0),
		new WorldPoint(2078, 6369, 0),
		new WorldPoint(2078, 6375, 0),
		new WorldPoint(2078, 6376, 0),
		new WorldPoint(2078, 6382, 0),
		new WorldPoint(2078, 6383, 0),
		new WorldPoint(2079, 6361, 0),
		new WorldPoint(2079, 6368, 0),
		new WorldPoint(2079, 6376, 0),
		new WorldPoint(2079, 6382, 0),
		new WorldPoint(2080, 6361, 0),
		new WorldPoint(2080, 6368, 0),
		new WorldPoint(2080, 6376, 0),
		new WorldPoint(2080, 6382, 0),
		new WorldPoint(2081, 6361, 0),
		new WorldPoint(2081, 6362, 0),
		new WorldPoint(2081, 6368, 0),
		new WorldPoint(2081, 6376, 0),
		new WorldPoint(2081, 6382, 0),
		new WorldPoint(2082, 6362, 0),
		new WorldPoint(2082, 6368, 0),
		new WorldPoint(2082, 6376, 0),
		new WorldPoint(2082, 6382, 0),
		new WorldPoint(2082, 6383, 0),
		new WorldPoint(2083, 6362, 0),
		new WorldPoint(2083, 6363, 0),
		new WorldPoint(2083, 6368, 0),
		new WorldPoint(2083, 6376, 0),
		new WorldPoint(2083, 6383, 0),
		new WorldPoint(2083, 6384, 0),
		new WorldPoint(2084, 6363, 0),
		new WorldPoint(2084, 6368, 0),
		new WorldPoint(2084, 6369, 0),
		new WorldPoint(2084, 6375, 0),
		new WorldPoint(2084, 6376, 0),
		new WorldPoint(2084, 6384, 0),
		new WorldPoint(2085, 6363, 0),
		new WorldPoint(2085, 6369, 0),
		new WorldPoint(2085, 6370, 0),
		new WorldPoint(2085, 6371, 0),
		new WorldPoint(2085, 6372, 0),
		new WorldPoint(2085, 6373, 0),
		new WorldPoint(2085, 6374, 0),
		new WorldPoint(2085, 6375, 0),
		new WorldPoint(2085, 6384, 0),
		new WorldPoint(2086, 6362, 0),
		new WorldPoint(2086, 6363, 0),
		new WorldPoint(2086, 6384, 0),
		new WorldPoint(2087, 6362, 0),
		new WorldPoint(2087, 6383, 0),
		new WorldPoint(2087, 6384, 0),
		new WorldPoint(2088, 6362, 0),
		new WorldPoint(2088, 6383, 0),
		new WorldPoint(2089, 6362, 0),
		new WorldPoint(2089, 6382, 0),
		new WorldPoint(2090, 6362, 0),
		new WorldPoint(2090, 6363, 0),
		new WorldPoint(2090, 6381, 0),
		new WorldPoint(2091, 6363, 0),
		new WorldPoint(2091, 6364, 0),
		new WorldPoint(2091, 6365, 0),
		new WorldPoint(2091, 6366, 0),
		new WorldPoint(2091, 6367, 0),
		new WorldPoint(2091, 6368, 0),
		new WorldPoint(2091, 6377, 0),
		new WorldPoint(2091, 6378, 0),
		new WorldPoint(2091, 6379, 0),
		new WorldPoint(2091, 6380, 0),
		new WorldPoint(2092, 6368, 0),
		new WorldPoint(2092, 6369, 0),
		new WorldPoint(2092, 6374, 0),
		new WorldPoint(2092, 6375, 0),
		new WorldPoint(2092, 6376, 0),
		new WorldPoint(2092, 6377, 0),
		new WorldPoint(2093, 6369, 0),
		new WorldPoint(2093, 6370, 0),
		new WorldPoint(2093, 6371, 0),
		new WorldPoint(2093, 6372, 0),
		new WorldPoint(2093, 6373, 0),
		new WorldPoint(2093, 6374, 0),
		// Leviathan hitbox interior (center) -- he occupies these tiles, not standable
		new WorldPoint(2078, 6369, 0),
		new WorldPoint(2078, 6370, 0),
		new WorldPoint(2078, 6371, 0),
		new WorldPoint(2078, 6372, 0),
		new WorldPoint(2078, 6373, 0),
		new WorldPoint(2078, 6374, 0),
		new WorldPoint(2078, 6375, 0),
		new WorldPoint(2079, 6369, 0),
		new WorldPoint(2079, 6370, 0),
		new WorldPoint(2079, 6371, 0),
		new WorldPoint(2079, 6372, 0),
		new WorldPoint(2079, 6373, 0),
		new WorldPoint(2079, 6374, 0),
		new WorldPoint(2079, 6375, 0),
		new WorldPoint(2080, 6369, 0),
		new WorldPoint(2080, 6370, 0),
		new WorldPoint(2080, 6371, 0),
		new WorldPoint(2080, 6372, 0),
		new WorldPoint(2080, 6373, 0),
		new WorldPoint(2080, 6374, 0),
		new WorldPoint(2080, 6375, 0),
		new WorldPoint(2081, 6369, 0),
		new WorldPoint(2081, 6370, 0),
		new WorldPoint(2081, 6371, 0),
		new WorldPoint(2081, 6372, 0),
		new WorldPoint(2081, 6373, 0),
		new WorldPoint(2081, 6374, 0),
		new WorldPoint(2081, 6375, 0),
		new WorldPoint(2082, 6369, 0),
		new WorldPoint(2082, 6370, 0),
		new WorldPoint(2082, 6371, 0),
		new WorldPoint(2082, 6372, 0),
		new WorldPoint(2082, 6373, 0),
		new WorldPoint(2082, 6374, 0),
		new WorldPoint(2082, 6375, 0),
		new WorldPoint(2083, 6369, 0),
		new WorldPoint(2083, 6370, 0),
		new WorldPoint(2083, 6371, 0),
		new WorldPoint(2083, 6372, 0),
		new WorldPoint(2083, 6373, 0),
		new WorldPoint(2083, 6374, 0),
		new WorldPoint(2083, 6375, 0),
		new WorldPoint(2084, 6369, 0),
		new WorldPoint(2084, 6370, 0),
		new WorldPoint(2084, 6371, 0),
		new WorldPoint(2084, 6372, 0),
		new WorldPoint(2084, 6373, 0),
		new WorldPoint(2084, 6374, 0),
		new WorldPoint(2084, 6375, 0),
	};

	private static final java.util.Set<Long> ARENA_INACCESSIBLE_KEYS = buildInaccessibleKeys();
	private static java.util.Set<Long> buildInaccessibleKeys()
	{
		java.util.Set<Long> set = new java.util.HashSet<>();
		for (WorldPoint w : ARENA_INACCESSIBLE)
			set.add(((long) w.getX() << 32) | (w.getY() & 0xFFFFFFFFL));
		return set;
	}

	/** True if the TEMPLATE tile is an inaccessible arena tile (boundary / hitbox ring). */
	public static boolean isInaccessibleTemplate(WorldPoint tpl)
	{
		if (tpl == null) return false;
		return ARENA_INACCESSIBLE_KEYS.contains(((long) tpl.getX() << 32) | (tpl.getY() & 0xFFFFFFFFL));
	}
}
