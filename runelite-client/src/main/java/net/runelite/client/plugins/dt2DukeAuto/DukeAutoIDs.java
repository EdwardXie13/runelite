package net.runelite.client.plugins.dt2DukeAuto;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Constants for Duke Sucellus's fight.
 * <p>
 * Boss NPC ids are taken from {@link net.runelite.api.gameval.NpcID} and are known-good.
 * Mechanic ids (projectiles, graphics, animations, the attack-style tells, ceiling-slam
 * telegraphs) are marked TODO -- capture them in-game with the config's "capture mode"
 * (set arenaRegion to -1 to log everywhere until the region is confirmed), exactly like
 * Vardorvis / Leviathan were built.
 */
public final class DukeAutoIDs
{
	private DukeAutoIDs() {}

	// --- Boss NPCs ---------------------------------------------------
	// Gameval (net.runelite.api.gameval.NpcID):
	//   DUKE_SUCELLUS_INACTIVE       = 12166  (pre-wake, not yet engaged)
	//   DUKE_SUCELLUS_ASLEEP         = 12167  (dormant; player wakes him via the vents)
	//   DUKE_SUCELLUS_AWAKE          = 12191  (ACTIVE fight id -- this is what we fight)
	//   DUKE_SUCELLUS_DEAD           = 12192  (death transition / corpse)
	//   Quest-encounter variants: 12193 inactive, 12194 asleep, 12195 awake (NOT fought here)
	public static final int NPC_DUKE_INACTIVE = 12166;
	public static final int NPC_DUKE_ASLEEP   = 12167;
	public static final int NPC_DUKE_AWAKE    = 12191;
	public static final int NPC_DUKE_DEAD     = 12192;

	// DPS thrall: which Greater thrall to summon. 14287043 = magic (ghost), 14287044 = ranged (skeleton),
	// 14287045 = melee (zombie). Change this one value to switch the thrall's style for Duke.
	public static final int THRALL_CAST_WIDGET = 14287045;   // melee zombie

	/**
	 * USER-CONFIRMED: the in-encounter Duke NPC id observed in play is 12167
	 * (== NPC_DUKE_ASLEEP). Whether Duke flips to NPC_DUKE_AWAKE (12191) for the attackable
	 * fight, or stays 12167 throughout, drives the FIGHT-phase gate -- see isDukeAwake().
	 */
	public static final int NPC_DUKE_ENCOUNTER = NPC_DUKE_ASLEEP;   // 12167

	/** Duke's tile footprint. He is a large (5x5) NPC. TODO: confirm size from capture. */
	public static final int DUKE_SIZE = 5;

	// --- Encounter NPCs (prep / fight mechanics) ---------------------
	/** Toxic vent / toxic cloud -- multiple spawn during the encounter; each is a 3x3 NPC. */
	public static final int NPC_TOXIC_VENT = 12198;
	public static final int TOXIC_VENT_SIZE = 3;
	/**
	 * The 9 possible toxic-cloud spawn anchors (3x3 grid, x=3036/3039/3042, y=6442/6446/6450).
	 * Coords are the captured spawn tiles; each cloud covers that tile + its 3x3 footprint.
	 */
	public static final WorldPoint[] TOXIC_VENT_SPAWNS = {
		new WorldPoint(3036, 6450, 0), new WorldPoint(3039, 6450, 0), new WorldPoint(3042, 6450, 0),
		new WorldPoint(3036, 6446, 0), new WorldPoint(3039, 6446, 0), new WorldPoint(3042, 6446, 0),
		new WorldPoint(3036, 6442, 0), new WorldPoint(3039, 6442, 0), new WorldPoint(3042, 6442, 0),
	};
	/** A toxic cloud's 3x3 footprint must be avoided for this many ticks from spawn. */
	public static final int TOXIC_VENT_ACTIVE_TICKS = 11;
	/** Duke's extremities (attack / interact targets to weaken him). */
	public static final int[] NPC_EXTREMITIES = { 12199, 12200, 12201 };
	public static boolean isExtremity(int id)
	{
		for (int e : NPC_EXTREMITIES) if (id == e) return true;
		return false;
	}
	/*
	 * Extremity timeline, anchored to spawn tick:
	 *   t0 spawn | t1 - | t2 - | t3 ATTACK | t4 despawn
	 * So the attack lands exactly EXTREMITY_ATTACK_DELAY_TICKS (3) after spawn; the NPC is
	 * gone by t4. Use the spawn tick (tracked in the main loop) to schedule the response.
	 */
	public static final int EXTREMITY_ATTACK_DELAY_TICKS = 3;
	public static final int EXTREMITY_DESPAWN_TICKS       = 4;
	/** Ticks the cone tiles stay damaging from the attack tick (the hit is a single tick). */
	public static final int EXTREMITY_ATTACK_ACTIVE_TICKS = 1;

	/** Extremity NPC footprint (5x5). The attacked apex is its center = SW + (2,2). */
	public static final int EXTREMITY_SIZE = 5;
	/** x threshold splitting west extremities (center.x < split, cone spreads +x) from east. */
	public static final int EXTREMITY_SIDE_SPLIT_X = 3039;
	/** Cone depth: apex, then rows widening by +/-1 out to this many tiles (apex,3,5 => depth 3). */
	public static final int EXTREMITY_CONE_DEPTH = 3;

	/**
	 * The 6 possible cone apexes (== extremity 5x5 centers). West apexes x=3030 spread east,
	 * east apexes x=3048 spread west; y in {6442,6446,6450}. Reference/overlay data -- live
	 * marking derives the cone from each NPC's actual center via {@link #extremityCone}.
	 */
	public static final WorldPoint[] EXTREMITY_CLUSTER_TIPS = {
		new WorldPoint(3030, 6442, 0), new WorldPoint(3030, 6446, 0), new WorldPoint(3030, 6450, 0),
		new WorldPoint(3048, 6442, 0), new WorldPoint(3048, 6446, 0), new WorldPoint(3048, 6450, 0),
	};

	/**
	 * Build the damaging cone for an extremity given its 5x5 center (apex). The cone spreads
	 * inward -- east for a west extremity (center.x &lt; EXTREMITY_SIDE_SPLIT_X), west for an
	 * east one -- as apex(1) -> 3 -> 5, widening +/-depth in y. 9 tiles for depth 3.
	 */
	public static List<WorldPoint> extremityCone(WorldPoint center)
	{
		List<WorldPoint> tiles = new ArrayList<>(9);
		if (center == null) return tiles;
		int dirX = center.getX() < EXTREMITY_SIDE_SPLIT_X ? +1 : -1;
		for (int d = 0; d < EXTREMITY_CONE_DEPTH; d++)
			for (int w = -d; w <= d; w++)
				tiles.add(new WorldPoint(center.getX() + dirX * d, center.getY() + w, center.getPlane()));
		return tiles;
	}

	// --- Fight: camp tiles -------------------------------------------
	/** The two player camp spots for the fight (captured world coords, region 12132). */
	public static final WorldPoint EAST_CAMP = new WorldPoint(3043, 6451, 0);
	public static final WorldPoint WEST_CAMP = new WorldPoint(3035, 6451, 0);

	/**
	 * Player positioning lines between the extremities (attack/positioning). West column
	 * x=3030, east column x=3048; both every other tile y=6441..6451.
	 */
	public static final WorldPoint[] WEST_EXTREMITY_TILES = {
		new WorldPoint(3030, 6441, 0),
		new WorldPoint(3030, 6443, 0),
		new WorldPoint(3030, 6445, 0),
		new WorldPoint(3030, 6447, 0),
		new WorldPoint(3030, 6449, 0),
		new WorldPoint(3030, 6451, 0),
	};
	public static final WorldPoint[] EAST_EXTREMITY_TILES = {
		new WorldPoint(3048, 6441, 0),
		new WorldPoint(3048, 6443, 0),
		new WorldPoint(3048, 6445, 0),
		new WorldPoint(3048, 6447, 0),
		new WorldPoint(3048, 6449, 0),
		new WorldPoint(3048, 6451, 0),
	};

	// --- Attack-style tells (prayer swap) ----------------------------
	/*
	 * Duke attacks the player with MAGIC and RANGED, alternating. Protect-from-Magic /
	 * Protect-from-Missiles must be flicked to match BEFORE the hit lands (impact-1, same
	 * model as Leviathan's projectile watcher). The tell is a graphics/animation on Duke
	 * (an icy/blue cue for one style, a rocky/ground cue for the other) plus the projectile
	 * id in flight.
	 *
	 * TODO (capture): fill these in.
	 */
	public static final int PROJ_MAGIC  = -1;   // TODO capture -- pray Magic before impact
	public static final int PROJ_RANGED = -1;   // TODO capture -- pray Missiles before impact
	public static final int GFX_TELL_MAGIC  = -1;   // TODO capture
	public static final int GFX_TELL_RANGED = -1;   // TODO capture
	public static final int ANIM_ATTACK_MAGIC  = -1;   // TODO capture
	public static final int ANIM_ATTACK_RANGED = -1;   // TODO capture

	// --- Fight phase (Duke awake, NPC 12191) -------------------------
	/** Duke's basic attack animation. */
	public static final int ANIM_DUKE_BASIC_ATTACK = 10176;
	/** Duke's "Gaze" animation. TODO: confirm the required response (pray / look away / move). */
	public static final int ANIM_DUKE_GAZE = 10180;

	/**
	 * Gas-bottle projectile Duke throws at one of the 9 vents; on landing it spawns a toxic
	 * cloud (NPC_TOXIC_VENT 12198) whose 3x3 damages. Same cloud NPC as the prep phase, so its
	 * hazard handling runs in BOTH phases. Camp response: if a cloud threatens our camp tile,
	 * switch to the opposite camp (east <-> west).
	 */
	public static final int PROJ_GAS_BOTTLE = 2436;

	// --- Ceiling / falling hazards -----------------------------------
	/*
	 * Two captured ground hazards, handled by the shared TelegraphedHazards / dodger model
	 * (see Leviathan's rock handling):
	 *
	 *   GFX 1447 -- FALLING hazard. Telegraphed: the spotanim plays while the hazard is in
	 *     the air, impact lands 3 ticks after it spawns. Marked whole-fall so we ALWAYS
	 *     dodge a tile with one incoming, then it clears itself once impact has passed.
	 *
	 *   GFX 538 -- AVOID hazard. A damaging tile that must simply never be stood on while
	 *     the spotanim is present (no fall delay). Flagged dangerous for a short rolling
	 *     window each tick it is seen, so the dodge/move logic routes off it.
	 */
	public static final int GFX_FALLING_HAZARD        = 1447;
	/**
	 * One telegraphed attack: GFX 1447 is the incoming telegraph, GFX 538 is the landing.
	 * Timeline anchored to the 1447 spawn:
	 *   t0 1447 spawns | t1 - | t2 - | t3 - | t4 1447 gone + 538 spawns (IMPACT) | t5 538 dormant
	 * So impact lands FALLING_TO_IMPACT_TICKS (4) after 1447 appears, and ONLY that single
	 * tick damages -- you may stand on the tile any other tick. Hence markWholeFall=FALSE and
	 * a 1-tick active window (unlike the Leviathan roar rocks, which are whole-fall).
	 */
	public static final int FALLING_TO_IMPACT_TICKS     = 4;
	public static final int FALLING_IMPACT_ACTIVE_TICKS = 1;

	/** The landing spotanim. Dangerous only on its own spawn tick (== the 1447 impact); dormant after. */
	public static final int GFX_IMPACT_HAZARD = 538;

	public static boolean isFallingHazardGfx(int id) { return id == GFX_FALLING_HAZARD; }
	public static boolean isImpactHazardGfx(int id)  { return id == GFX_IMPACT_HAZARD; }

	// --- White/weaken (vent) mechanic --------------------------------
	/*
	 * Duke gains a damage-reduction/"gas shield" window periodically (the alchemical / icy
	 * vapour he is immune through until re-weakened). For now this is just a phase marker;
	 * the automation suspends offence during it rather than wasting hits.
	 *
	 * TODO (capture): the varbit / gfx / overhead that marks the immune window.
	 */
	public static final int VARBIT_SHIELD = -1;   // TODO capture

	// --- Arena --------------------------------------------------------
	/**
	 * Duke Sucellus instance region id. Ghorrock dungeon arena.
	 * CONFIRMED: the prep-phase mushroom tiles (3030,6452) / (3048,6452) sit in this region
	 * ((3030>>6)=47, (6452>>6)=100 -> 47*256+100 = 12132).
	 */
	public static final int ARENA_REGION = 12132;

	/** Player's tile on first entering the arena (spawn-in point; NOT a camp/fight tile). */
	public static final WorldPoint ENTRY_TILE = new WorldPoint(3039, 6433, 0);

	/** Tile the overhead camera focuses on (template coords; converted to instance at runtime). */
	/** Overhead camera centre during PREP (collect / brew / wake). */
	public static final WorldPoint ARENA_CAMERA_CENTER = new WorldPoint(3039, 6447, 0);
	/** Overhead camera centre during the actual FIGHT (Duke awake: FIGHT / SHIELD). */
	public static final WorldPoint ARENA_CAMERA_CENTER_FIGHT = new WorldPoint(3039, 6451, 0);
	/** Overhead-camera zoom (CAMERA_DO_ZOOM arg; lower = wider field, higher = closer). */
	public static final int ARENA_CAMERA_ZOOM = 250;        // PREP overhead zoom (wide, for collecting)
	public static final int ARENA_CAMERA_ZOOM_FIGHT = 550;  // FIGHT overhead zoom (tight on Duke)

	/**
	 * Arena WALL tiles (TEMPLATE coords, region 12132) the BFS path planner treats as
	 * IMPASSABLE, on top of the game's live collision. Exported from the Duke Arena Path
	 * Planner (approximate outer boundary). Use {@link #isWall(WorldPoint)} for the fast
	 * membership test rather than scanning this array.
	 */
	public static final WorldPoint[] WALL_TILES = {
		new WorldPoint(3029, 6434, 0), new WorldPoint(3029, 6435, 0), new WorldPoint(3029, 6436, 0),
		new WorldPoint(3029, 6437, 0), new WorldPoint(3029, 6438, 0), new WorldPoint(3029, 6439, 0),
		new WorldPoint(3029, 6440, 0), new WorldPoint(3029, 6441, 0), new WorldPoint(3029, 6442, 0),
		new WorldPoint(3029, 6443, 0), new WorldPoint(3029, 6444, 0), new WorldPoint(3029, 6445, 0),
		new WorldPoint(3029, 6446, 0), new WorldPoint(3029, 6447, 0), new WorldPoint(3029, 6448, 0),
		new WorldPoint(3029, 6449, 0), new WorldPoint(3029, 6450, 0), new WorldPoint(3029, 6451, 0),
		new WorldPoint(3029, 6452, 0), new WorldPoint(3029, 6453, 0), new WorldPoint(3029, 6454, 0),
		new WorldPoint(3030, 6434, 0), new WorldPoint(3030, 6454, 0), new WorldPoint(3031, 6434, 0),
		new WorldPoint(3031, 6454, 0), new WorldPoint(3032, 6434, 0), new WorldPoint(3032, 6454, 0),
		new WorldPoint(3033, 6434, 0), new WorldPoint(3033, 6440, 0), new WorldPoint(3033, 6441, 0),
		new WorldPoint(3033, 6442, 0), new WorldPoint(3033, 6443, 0), new WorldPoint(3033, 6444, 0),
		new WorldPoint(3033, 6445, 0), new WorldPoint(3033, 6446, 0), new WorldPoint(3033, 6447, 0),
		new WorldPoint(3033, 6448, 0), new WorldPoint(3033, 6449, 0), new WorldPoint(3033, 6450, 0),
		new WorldPoint(3033, 6451, 0), new WorldPoint(3033, 6452, 0), new WorldPoint(3033, 6454, 0),
		new WorldPoint(3034, 6434, 0), new WorldPoint(3034, 6440, 0), new WorldPoint(3034, 6444, 0),
		new WorldPoint(3034, 6448, 0), new WorldPoint(3034, 6452, 0), new WorldPoint(3034, 6454, 0),
		new WorldPoint(3035, 6434, 0), new WorldPoint(3035, 6440, 0), new WorldPoint(3035, 6444, 0),
		new WorldPoint(3035, 6448, 0), new WorldPoint(3035, 6452, 0), new WorldPoint(3035, 6454, 0),
		new WorldPoint(3036, 6434, 0), new WorldPoint(3036, 6454, 0), new WorldPoint(3037, 6433, 0),
		new WorldPoint(3037, 6434, 0), new WorldPoint(3037, 6454, 0), new WorldPoint(3038, 6432, 0),
		new WorldPoint(3038, 6433, 0), new WorldPoint(3038, 6454, 0), new WorldPoint(3039, 6454, 0),
		new WorldPoint(3040, 6433, 0), new WorldPoint(3040, 6454, 0), new WorldPoint(3041, 6433, 0),
		new WorldPoint(3041, 6434, 0), new WorldPoint(3041, 6454, 0), new WorldPoint(3042, 6434, 0),
		new WorldPoint(3042, 6454, 0), new WorldPoint(3043, 6434, 0), new WorldPoint(3043, 6440, 0),
		new WorldPoint(3043, 6444, 0), new WorldPoint(3043, 6448, 0), new WorldPoint(3043, 6452, 0),
		new WorldPoint(3043, 6454, 0), new WorldPoint(3044, 6434, 0), new WorldPoint(3044, 6440, 0),
		new WorldPoint(3044, 6444, 0), new WorldPoint(3044, 6448, 0), new WorldPoint(3044, 6452, 0),
		new WorldPoint(3044, 6454, 0), new WorldPoint(3045, 6434, 0), new WorldPoint(3045, 6440, 0),
		new WorldPoint(3045, 6441, 0), new WorldPoint(3045, 6442, 0), new WorldPoint(3045, 6443, 0),
		new WorldPoint(3045, 6444, 0), new WorldPoint(3045, 6445, 0), new WorldPoint(3045, 6446, 0),
		new WorldPoint(3045, 6447, 0), new WorldPoint(3045, 6448, 0), new WorldPoint(3045, 6449, 0),
		new WorldPoint(3045, 6450, 0), new WorldPoint(3045, 6451, 0), new WorldPoint(3045, 6452, 0),
		new WorldPoint(3045, 6454, 0), new WorldPoint(3046, 6434, 0), new WorldPoint(3046, 6454, 0),
		new WorldPoint(3047, 6434, 0), new WorldPoint(3047, 6454, 0), new WorldPoint(3048, 6434, 0),
		new WorldPoint(3048, 6454, 0), new WorldPoint(3049, 6434, 0), new WorldPoint(3049, 6435, 0),
		new WorldPoint(3049, 6436, 0), new WorldPoint(3049, 6437, 0), new WorldPoint(3049, 6438, 0),
		new WorldPoint(3049, 6439, 0), new WorldPoint(3049, 6440, 0), new WorldPoint(3049, 6441, 0),
		new WorldPoint(3049, 6442, 0), new WorldPoint(3049, 6443, 0), new WorldPoint(3049, 6444, 0),
		new WorldPoint(3049, 6445, 0), new WorldPoint(3049, 6446, 0), new WorldPoint(3049, 6447, 0),
		new WorldPoint(3049, 6448, 0), new WorldPoint(3049, 6449, 0), new WorldPoint(3049, 6450, 0),
		new WorldPoint(3049, 6451, 0), new WorldPoint(3049, 6452, 0), new WorldPoint(3049, 6453, 0),
		new WorldPoint(3049, 6454, 0),
	};

	private static final Set<Long> WALL_KEYS = buildWallKeys();
	private static Set<Long> buildWallKeys()
	{
		Set<Long> keys = new HashSet<>();
		for (WorldPoint w : WALL_TILES) keys.add(((long) w.getX() << 32) | (w.getY() & 0xFFFFFFFFL));
		return keys;
	}

	/** True if the given TEMPLATE tile is an arena wall. */
	public static boolean isWall(WorldPoint templateTile)
	{
		return templateTile != null
			&& WALL_KEYS.contains(((long) templateTile.getX() << 32) | (templateTile.getY() & 0xFFFFFFFFL));
	}

	/** Geometric center of the arena (template coords). TODO: resolve from capture. */
	public static final WorldPoint ARENA_CENTER_TEMPLATE = null;   // TODO

	/*
	 * Arena bounding box in TEMPLATE coords. TODO: fill from capture.
	 */
	public static final int ARENA_X_MIN = -1;
	public static final int ARENA_X_MAX = -1;
	public static final int ARENA_Y_MIN = -1;
	public static final int ARENA_Y_MAX = -1;

	// --- Prep phase: ingredient gathering ----------------------------
	/*
	 * PREP sequence picks mushrooms and processes them into the two mixtures that weaken Duke
	 * before the fight. Captured clicks are GAME_OBJECT_FIRST_OPTION with option "Pick".
	 *
	 *   Arder mushrooms -- GameObject 49074, menu id 47528, stand tile (3030, 6452, 0)
	 *   Musca mushrooms -- GameObject 49075, menu id 47524, stand tile (3048, 6452, 0)
	 *
	 * The GameObject id (4907x) identifies the object; the menu id (475xx) is the identifier
	 * in the click's MenuEntry (the scene object id). Stand tiles are the capture's absolute
	 * world coords in region 12132 -- resolve through InstanceCoords at runtime if the arena
	 * turns out to be a dynamic instance (TODO: confirm instance vs fixed).
	 */
	public static final int GOBJ_ARDER_MUSHROOMS = 49074;
	public static final int GOBJ_MUSCA_MUSHROOMS = 49075;
	public static final int MENU_ID_ARDER_MUSHROOMS = 47528;
	public static final int MENU_ID_MUSCA_MUSHROOMS = 47524;
	public static final WorldPoint ARDER_MUSHROOMS_STAND = new WorldPoint(3030, 6452, 0);
	public static final WorldPoint MUSCA_MUSHROOMS_STAND = new WorldPoint(3048, 6452, 0);
	/**
	 * Human two-leg approach tiles: walk to this "pre" tile first, THEN click the mushroom,
	 * so the trip runs down the x=3030/3048 column instead of the server's diagonal.
	 */
	public static final WorldPoint ARDER_PRE_TILE = new WorldPoint(3030, 6441, 0);
	public static final WorldPoint MUSCA_PRE_TILE = new WorldPoint(3048, 6441, 0);
	public static final String PICK_OPTION = "Pick";

	// Picked item + the pick animation (used to confirm a pick started / detect idle).
	public static final int ITEM_ARDER_MUSHROOM = 28345;
	public static final int ITEM_MUSCA_MUSHROOM = 28341;
	// Ground products: use a mushroom with the pestle and mortar to get its powder.
	public static final int ITEM_ARDER_POWDER   = 28346;
	public static final int ITEM_MUSCA_POWDER   = 28342;
	public static final int ANIM_PICK_MUSHROOM  = 2282;
	/** Pestle and mortar -- grinds the picked mushrooms during prep. */
	public static final int ITEM_PESTLE_AND_MORTAR = 233;

	// Collection goals: enough to make 12 arder powder, 12 musca powder, and 12 salax salt.
	public static final int POWDER_GOAL = 12;
	public static final int SALT_GOAL   = 12;

	// Salt deposit click: GAME_OBJECT_FIRST_OPTION id=47522 option="Mine" target="Salt Deposit".
	// (Object id == menu id 47522, unlike the mushrooms whose scene id differs from GameObject id.)
	public static final String SALT_OPTION = "Mine";
	public static final String SALT_TARGET = "Salt Deposit";
	/** Salax salt -- yielded by Mining the salt deposit. */
	public static final int ITEM_SALAX_SALT = 28349;

	/**
	 * Powder yielded per mushroom ground (pestle + mortar), by Herblore level:
	 * 1-39:1, 40-49:2, 50-59:3, 60-69:4, 70-79:5, 80+:6.
	 */
	public static int herbloreMushroomYield(int herbloreLevel)
	{
		if (herbloreLevel >= 80) return 6;
		if (herbloreLevel >= 70) return 5;
		if (herbloreLevel >= 60) return 4;
		if (herbloreLevel >= 50) return 3;
		if (herbloreLevel >= 40) return 2;
		return 1;
	}

	/**
	 * Salax salt yielded per mine of the deposit, by Mining level:
	 * 1-69:3, 70-74:4, 75-79:5, 80+:6. (Informational -- collection counts the item directly.)
	 */
	public static int miningSaltYield(int miningLevel)
	{
		if (miningLevel >= 80) return 6;
		if (miningLevel >= 75) return 5;
		if (miningLevel >= 70) return 4;
		return 3;
	}

	/** Mushrooms to pick so grinding yields >= POWDER_GOAL powder at the given Herblore level. */
	public static int mushroomsNeeded(int herbloreLevel)
	{
		int y = herbloreMushroomYield(herbloreLevel);
		return (POWDER_GOAL + y - 1) / y;   // ceil
	}

	// Depleted (just-picked) states -- the object swaps to this id and is unpickable until it
	// regrows. Scan for the full pick id to know a mushroom is available again.
	//   Arder: full pick id 47528 -> depleted 47529.
	//   Musca: full pick id 47524 -> depleted 47525.
	public static final int GOBJ_ARDER_MUSHROOMS_DEPLETED = 47529;
	public static final int GOBJ_MUSCA_MUSHROOMS_DEPLETED = 47525;

	// Prep stations (GameObjects). TODO: capture stand tiles + the click option on each.
	public static final int GOBJ_FERMENTATION_VAT = 47536;   // two vats share this id, different scene tiles
	public static final int GOBJ_SALT_DEPOSIT     = 47522;
	/**
	 * Fermentation vat states (same scene tile swaps id like the mushrooms):
	 *   47536 = fillable  -> GAME_OBJECT_FIRST_OPTION option "Fill"
	 *   47539 = fermented -> GAME_OBJECT_FIRST_OPTION option "Collect-from"
	 */
	public static final int GOBJ_FERMENTATION_VAT_READY = 47539;
	public static final String FILL_OPTION = "Fill";
	public static final String COLLECT_FROM_OPTION = "Collect-from";
	public static final String FERMENTATION_VAT_TARGET = "Fermentation Vat";
	/**
	 * Arder-musca potion -- the collected product of a fermented vat. Two are needed to
	 * start the fight, which is why there are two vats (one potion each). The 12 arder
	 * powder + 12 musca powder + 12 salax salt are the inputs across both vats
	 * (likely 6/6/6 per vat -- TODO confirm the per-vat fill amounts + how "Fill" consumes).
	 */
	public static final int ITEM_ARDER_MUSCA_POTION = 28351;
	public static final int ARDER_MUSCA_POTION_GOAL = 2;
	/**
	 * With 2 arder-musca potions, go to a Duke camp tile (EAST_CAMP / WEST_CAMP) and
	 * "Feed" Duke Sucellus (NPC_FIRST_OPTION) to start the fight. Captured: NPC_FIRST_OPTION
	 * option "Feed" target "Duke Sucellus (level-758)" (the dormant NPC_DUKE_ENCOUNTER).
	 */
	public static final String FEED_OPTION = "Feed";
	/**
	 * Grinding: "Use" the pestle and mortar on a mushroom (WIDGET_TARGET_ON_WIDGET in the
	 * inventory) -> one mushroom becomes <herblore-yield> powder. Option is "Use".
	 */
	public static final String USE_OPTION = "Use";
	public static final WorldPoint SALT_DEPOSIT_STAND = new WorldPoint(3034, 6447, 0);

	// --- In-fight boss HP read ---------------------------------------
	/*
	 * Death / low-HP detection. Two options, both used by the sibling scripts:
	 *   1. NPC id transition: NPC_DUKE_AWAKE (12191) -> NPC_DUKE_DEAD (12192) / despawn.
	 *      Robust and needs no widget -- preferred default for Duke.
	 *   2. Boss-HP overlay widget (WidgetReader.BOSS_HP_COMPONENT, the shared DT2 health bar).
	 *      Duke reads it for the enrage-HP threshold; NPC transition remains the death cue.
	 * NOTE: InterfaceID.DUKE_SUCELLUS_SCOREBOARD (group 828 / 0x033c) is the POST-KILL
	 * scoreboard, NOT the in-fight HP bar -- do not use it for live HP.
	 */

	// --- Supplies (item ids) -----------------------------------------
	// NOTE: supply set below assumes a MELEE kill (super combat). If this rig ranges Duke
	// instead, swap the registered combat-boost potions in DukeSupply to RANGING /
	// DIVINE_RANGE exactly like Leviathan, and have DukeAutoMain request those kinds.
	public static final int FOOD_PRIMARY = ItemID.SHARK;
	public static final int FOOD_COMBO   = ItemID.TBWT_COOKED_KARAMBWAN;
	// Two-bite backup food: eat the HALF (already bitten) before opening a new FULL one.
	public static final int ITEM_PINEAPPLE_PIZZA      = 2301;
	public static final int ITEM_HALF_PINEAPPLE_PIZZA = 2303;

	public static final int[] PRAYER_POT_DOSES = new int[] {
		ItemID._1DOSEPRAYERRESTORE,
		ItemID._2DOSEPRAYERRESTORE,
		ItemID._3DOSEPRAYERRESTORE,
		ItemID._4DOSEPRAYERRESTORE
	};

	public static final int[] SUPER_COMBAT_DOSES = new int[] {
		ItemID._1DOSE2COMBAT,
		ItemID._2DOSE2COMBAT,
		ItemID._3DOSE2COMBAT,
		ItemID._4DOSE2COMBAT
	};

	// Divine super combat (1..4). PREFERRED over regular super combat when present; only
	// one of the two is ever carried, either (or neither) may be absent.
	public static final int[] DIVINE_SUPER_COMBAT_DOSES = new int[] {
		ItemID._1DOSEDIVINECOMBAT,
		ItemID._2DOSEDIVINECOMBAT,
		ItemID._3DOSEDIVINECOMBAT,
		ItemID._4DOSEDIVINECOMBAT
	};

	// Regular super-combat re-dose threshold: re-drink once the remaining Strength boost
	// (boosted - base) falls BELOW this. Divine ignores this (5-min timer).
	public static final int SUPER_COMBAT_REDRINK_MIN_BOOST = 1;

	public static final int[] STAMINA_DOSES = new int[] {
		ItemID._1DOSESTAMINA,
		ItemID._2DOSESTAMINA,
		ItemID._3DOSESTAMINA,
		ItemID._4DOSESTAMINA
	};
}
