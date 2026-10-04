package net.runelite.client.plugins.dt2VardorvisAuto;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

/**
 * Constants for Vardorvis's fight. NPCs / projectiles / animations / graphics IDs
 * were captured in-game; item IDs reference gameval constants.
 */
public final class VardorvisAutoIDs
{
	private VardorvisAutoIDs() {}

	// --- Boss NPCs ---------------------------------------------------
	/** Regular Vardorvis -- same id whether idle or actively fighting. */
	public static final int NPC_VARDORVIS          = 12223;
	/** Quest-encounter Vardorvis. Not what the auto fights. */
	public static final int NPC_VARDORVIS_QUEST    = 12224;
	/** Awakened (hard mode) Vardorvis. TODO: capture ID. */
	public static final int NPC_VARDORVIS_AWAKENED = -1;
	public static final int NPC_LARGE_TENDRIL  = 12225;   // spawns the flying axes
	public static final int NPC_HEAD           = 12226;   // projectile-throwing head
	public static final int NPC_FLYING_AXE     = 12227;

	// --- Projectiles -------------------------------------------------
	/** The head's projectile. Protect from Missiles must be ON before impact. */
	public static final int PROJ_HEAD = 2521;

	// --- Vardorvis animations ---------------------------------------
	/** Vardorvis begins preparing the "captcha" head-prompt mechanic. */
	public static final int ANIM_CAPTCHA_PREP   = 10342;
	/** The captcha is actively on screen. Automation suspends here. */
	public static final int ANIM_CAPTCHA_SHOWN  = 10343;

	/** Parent interface group id of the Vardorvis QTE / captcha mechanic. */
	public static final int QTE_INTERFACE_GROUP = 833;
	/** QTE_MODEL child widget ids (1..6). Click "Destroy" on each visible one within 5 ticks. */
	public static final int[] QTE_CHILD_WIDGET_IDS = {
		54591494, 54591495, 54591496, 54591497, 54591498, 54591499
	};
	/** Max QTE destroys to fire per tick. 3 clears a full 6-widget captcha in 2 ticks. */
	public static final int QTE_MAX_CLICKS_PER_TICK = 3;

	// --- Vardorvis active-state markers (GameObjects) ---------------
	/**
	 * GameObjects that only exist in the scene while Vardorvis is actively fighting.
	 * 26209 is the initial poke marker; the others are its later transformations.
	 * If ANY of these are present, Vardorvis is active -- otherwise he's dormant.
	 */
	public static final int[] GOBJ_ACTIVE_MARKERS = { 26209, 47599, 47600, 47601 };

	// --- Spike graphics ---------------------------------------------
	/** Pre-sprout telegraph tile. Harmless but marks where the spike will appear. */
	public static final int GFX_SPIKE_INDICATOR = 2510;
	/** Sprouted spike. Damaging if standing on it. */
	public static final int GFX_SPIKE_ACTIVE    = 2512;

	/** Ticks between seeing the indicator and the active spike. TODO: calibrate from log. */
	public static final int SPIKE_SPROUT_TICKS = 2;

	/** Ticks between Large Tendril (12225) spawn and the flying axe (12227) actually appearing + moving. */
	public static final int AXE_SPAWN_DELAY_TICKS = 3;

	/*
	 * ----- AXE MECHANICS NOTES ---------------------------------------
	 * - At most 3 axes are active at any one time.
	 * - Axes travel in a straight line from their spawn edge to the opposite side of the
	 *   arena: NE spawn -> SW, NW -> SE, W -> E, E -> W, N -> S, S -> N, etc.
	 * - Two axes CAN share the same line (e.g. one from W plus one from E could both exist).
	 *   Lines are not exclusive.
	 * - All axes pass through the arena's center at some tick during their flight, which
	 *   is why home camping has forced "move off home" windows -- it IS the center.
	 * - 3x3 size and SW-anchored, same as the tendril. The axe spawns at the tendril's
	 *   tile and moves (dir) tiles per tick.
	 * -----------------------------------------------------------------
	 */

	/**
	 * How many ticks of axe flight to project forward from the tendril spawn. Since the
	 * axe travels in a known straight line from the spawn edge toward the opposite side,
	 * we can mark the FULL path upfront. 15 covers a diagonal flight (sqrt(2)*11 ~= 15.5).
	 * Arena-bounds clip stops projection once the axe exits the arena.
	 */
	public static final int AXE_FLIGHT_PROJECT_TICKS = 15;

	// --- Arena --------------------------------------------------------
	/** Vardorvis instance region id. */
	public static final int ARENA_REGION = 4405;

	public static final WorldPoint HOME_TEMPLATE      = new WorldPoint(1125, 3423, 0);
	/** The single dodge tile. Two SE of home, outside the NW-corner axe path. */
	public static final WorldPoint SAFE_TILE_TEMPLATE = new WorldPoint(1127, 3421, 0);

	/*
	 * Arena bounding box in TEMPLATE coords. Vardorvis arena is 11x11, with the HOME tile
	 * at the NORTH-CENTER of the player-accessible area. Vardorvis himself sits just north
	 * of the arena. From home (1129, 3423):
	 *   X: 1124..1134  (home.x - 5 .. home.x + 5)
	 *   Y: 3413..3423  (home.y - 10 .. home.y)
	 */
	public static final int ARENA_X_MIN = 1124;
	public static final int ARENA_X_MAX = 1134;
	public static final int ARENA_Y_MIN = 3413;
	public static final int ARENA_Y_MAX = 3423;

	/** Arena geometric center (template coords). Axes always travel from their spawn
	 *  position toward the OPPOSITE side of the arena, passing through this point. Used
	 *  by direction prediction. HOME is at the north edge -- do NOT use it as the
	 *  classifier reference. */
	public static final WorldPoint ARENA_CENTER_TEMPLATE = new WorldPoint(1129, 3418, 0);

	/** Flying axe (12227) tile footprint size. */
	public static final int AXE_SIZE = 3;
	/** Vardorvis (12224) tile footprint size. */
	public static final int VARDORVIS_SIZE = 2;

	// --- Supplies (item ids) -----------------------------------------
	public static final int FOOD_PRIMARY   = ItemID.SHARK;
	public static final int FOOD_COMBO     = ItemID.TBWT_COOKED_KARAMBWAN;

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

	public static final int[] STAMINA_DOSES = new int[] {
		ItemID._1DOSESTAMINA,
		ItemID._2DOSESTAMINA,
		ItemID._3DOSESTAMINA,
		ItemID._4DOSESTAMINA
	};
}
