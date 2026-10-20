package net.runelite.client.plugins.dt2WhispererAuto;

/**
 * Constants for the Whisperer fight. Supply is a minimal MAGIC-kill starter (sharks +
 * prayer potion). Boss / projectile / sanity / hazard ids are captured; the arena region
 * and special-attack animations are still placeholders (-1) to be filled later.
 */
public final class WhispererAutoIDs
{
	private WhispererAutoIDs() {}

	// --- Boss NPCs ---------------------------------------------------
	// Asleep/awake pair (Duke-style), NOT a hard-mode variant. The boss sits DORMANT until
	// engaged, then flips to AWAKE for the actual fight.
	/** Whisperer, not yet awake (dormant). */
	public static final int NPC_WHISPERER_DORMANT = 12203;
	/** Whisperer, awake and fighting. */
	public static final int NPC_WHISPERER_AWAKE   = 12204;

	// DPS thrall: which Greater thrall to summon. 14287043 = magic (ghost), 14287044 = ranged (skeleton),
	// 14287045 = melee (zombie). Change this one value to switch the thrall's style for the Whisperer.
	public static final int THRALL_CAST_WIDGET = 14287043;   // magic ghost (Whisperer is a mage kill)
	/** Whisperer in her BIND form: she flips to this id to cast the post-phase bind, then shoots it. Its
	 *  presence is the reliable "she is binding" signal -- no 12205 after a phase end means no bind (so no
	 *  north reposition); a 12205 means she bound (hold through it, then reposition). */
	public static final int NPC_WHISPERER_BINDING = 12205;

	/** Tentacle NPC: a 3x3 NPC that spawns a few tiles out (on the cardinals or the diagonals) and
	 *  attacks inward; we classify each by its footprint direction and step the complementary way.
	 *  getWorldArea() reports the 3x3 extent (SW-anchored), so the dodge reads the whole footprint. */
	public static final int NPC_TENTACLE = 12208;
	/** Attack menu option for the Whisperer. Captured menu action:
	 *  NPC_SECOND_OPTION option='Attack' target='The Whisperer'. Used via
	 *  router.interactNpc(whisperer, MenuAction.NPC_SECOND_OPTION, WHISPERER_ATTACK_OPTION). */
	public static final String WHISPERER_ATTACK_OPTION = "Attack";
	/** Fallback ticks to keep attacking after a tentacle spawn is DETECTED before stepping off; the
	 *  live value is the config's tentacleDodgeDelay(). Observed timing: tentacles spawn (t0), the
	 *  attack strikes (~t2) and lands (~t3). The strike LOCKS onto our tile, so we must be off BY the
	 *  strike -- i.e. step on t1 (delay 1), which also lets us weave an attack on t0. delay 2 (stepping
	 *  on the strike tick) was too late. A hit is very punishing, so drop to 0 if delay 1 ever clips. */
	public static final int TENTACLE_DODGE_DELAY_TICKS = 1;
	/** Fallback minimum tiles to keep from the boss while dodging tentacles (we fight at range, so we
	 *  never dodge into melee); the live value is the config's tentacleMinGap(). ~4 keeps a human-like
	 *  gap and makes the dodge oscillate near the boss rather than drift away. */
	public static final int TENTACLE_MIN_GAP_TILES = 4;

	// --- Interactions -----------------------------------------------
	// "Disturb" the "Odd Figure" NPC. Captured menu action:
	//   NPC_FIRST_OPTION id=58255 option='Disturb' target='Odd Figure'
	// The id (58255) is a runtime NPC index, NOT a stable type id, so match on name + option.
	// TODO: wire when/why to disturb it (likely starts the encounter).
	public static final String NPC_ODD_FIGURE_NAME = "Odd Figure";
	public static final String ODD_FIGURE_OPTION   = "Disturb";

	// --- Projectiles -------------------------------------------------
	// The Whisperer alternates magic / ranged attacks AT us; we flick the matching overhead
	// one tick before each projectile's impact (see WhispererAutoMain#watchPrayerProjectiles).
	/** Ranged attack projectile -> Protect from Missiles. */
	public static final int PROJ_RANGE = 2444;
	/** Magic attack projectile -> Protect from Magic. */
	public static final int PROJ_MAGIC = 2445;
	/** Her other normal-phase attack projectile. In the post-seeds bind window, seeing this OR
	 *  {@link #PROJ_RANGE} (2443/2444) means she is attacking normally -> she did NOT bind. */
	public static final int PROJ_NORMAL_ALT = 2443;

	// --- Sanity -----------------------------------------------------
	/**
	 * Sanity readout widget. getText() reads like "Sanity: 100%"; parse the integer before
	 * the '%'. Central Whisperer mechanic -- displayed on the overlay now; sanity-driven
	 * behavior TODO (user will elaborate).
	 */
	public static final int SANITY_WIDGET_ID = 54657037;

	// --- Hazards (graphics objects) ---------------------------------
	/**
	 * Ground-hazard graphics objects to dodge. 2450 is the most important (user will
	 * elaborate on the exact mechanic / timing). Scanned via client.getGraphicsObjects().
	 */
	public static final int GFX_HAZARD_PRIMARY = 2450;
	public static final int[] GFX_HAZARD_IDS = { 2447, 2448, 2449, 2450 };

	// Tentacle mechanic: 2450 lands ON the player's tile; up to four 2449 bursts then
	// appear among the 8 surrounding tiles. We step to any clear (non-2449) neighbour.
	public static final int GFX_TENTACLE_CENTER = 2450;  // same as GFX_HAZARD_PRIMARY
	public static final int GFX_TENTACLE_BURST  = 2449;

	// --- Lost souls -------------------------------------------------
	/** Projectile that spawns the lost souls. */
	public static final int PROJ_LOST_SOUL_SPAWN = 668;
	/** Lost soul NPC in the overworld (real realm). */
	public static final int NPC_LOST_SOUL_OVERWORLD = 12211;
	/** Lost soul NPC in the shadow realm. */
	public static final int NPC_LOST_SOUL_SHADOW    = 12212;

	// Shadow-realm souls are told apart by their model recolor -- the devtools "C:" field,
	// NPC.getModelOverrides().getColorToReplaceWith() (a short[]). Yellow has NO recolor
	// (null/empty). TODO: wire soul handling (and a colour classifier) once the mechanic is defined.
	public static final short[] SOUL_COLOR_BLUE  = { -25790, -25801, -24787, -24816 };
	public static final short[] SOUL_COLOR_CYAN  = { -31934, -31945, -31955, -31984 };
	public static final short[] SOUL_COLOR_GREEN = {  26434,  26423,  26413,  26386 };

	// --- Pillars / floating column ----------------------------------
	/** Pillar NPC in the overworld (regular world). */
	public static final int NPC_PILLAR_OVERWORLD = 12210;
	/** Pillar NPC in the shadow realm. */
	public static final int NPC_PILLAR_SHADOW    = 12209;
	/** Projectile that attacks the pillar. */
	public static final int PROJ_COLUMN_ATTACK = 2471;
	/** Bind attack projectile. */
	public static final int PROJ_BIND = 2467;
	/** OUR Venator bow shot during the soul-siphon phase -- one projectile per shot. We count these to
	 *  detect the 4th (final) group shot, which ends the clear: swap the main weapon back + reactivate the
	 *  blackstone to return to the overworld. */
	public static final int PROJ_VENATOR_SHOT = 2291;

	// Shadow-realm pillar (12209) HP stage, read from its model-override model IDs (the
	// devtools "M:" field, NPC.getModelOverrides().getModelIds()). See pillarHpStage().
	public static final int PILLAR_MODEL_HP100 = 49311;  // full
	public static final int PILLAR_MODEL_HP40  = 49308;  // 40%
	public static final int PILLAR_MODEL_HP20  = 49309;  // 20%

	// --- Seeds (GameObjects) ----------------------------------------
	/** "Good" seed GameObject -- distinguishable only in the SHADOW realm. */
	public static final int GOBJ_GOOD_SEED = 47573;
	/** "Bad" seed GameObject -- distinguishable only in the SHADOW realm. */
	public static final int GOBJ_BAD_SEED  = 47574;
	/** OVERWORLD seed GameObject (47575): the PERSISTENT on-ground object for every seed (good + bad
	 *  alike, indistinguishable here). This is what stays down -- the 2459 graphics object is only the
	 *  transient spawn flash -- so we detect/latch the overworld seeds off this, not the graphic. */
	public static final int GOBJ_OVERWORLD_SEED = 47575;
	/** Clear the latched seed phase if no seed activity (seeds visible) for this many ticks. */
	public static final int SEED_PHASE_TIMEOUT_TICKS = 60;
	/** Minimum ticks to dwell in the shadow realm while scouting seeds before crossing back, so the
	 *  scene fully loads and we capture the whole ring (not a partial route). */
	public static final int SEED_SCOUT_DWELL_TICKS = 2;
	/** End the seed phase on its OWN signal: seeds gone this many ticks. Phases are independent -- we
	 *  never end one by detecting another (her seed-phase animation can itself read as a screech). */
	public static final int SEED_GONE_END_TICKS = 5;
	/** Walk safety: if we can't reach the current seed target within this many ticks, skip past it
	 *  instead of standing in the field (which would get us killed). */
	public static final int SEED_STUCK_TICKS = 8;

	// --- Shadow realm -----------------------------------------------
	/**
	 * Blackstone fragment, CHARGED/activatable form (id 28356). "Activate" this to cross realms at the
	 * required times; holding this id means it's ready ("pulses with dark energy"). Activating drains
	 * it to {@link #ITEM_BLACKSTONE_FRAGMENT_DRAINED} (28357), which can't be activated.
	 */
	public static final int ITEM_BLACKSTONE_FRAGMENT = 28356;
	/** Blackstone fragment, power-DRAINED form (id 28357): NOT activatable. 28356 becomes this after
	 *  it's used. Holding 28356 (vs 28357) is itself the signal that the fragment is ready to activate. */
	public static final int ITEM_BLACKSTONE_FRAGMENT_DRAINED = 28357;
	/** Graphics object (2459) that marks EVERY seed tile in the OVERWORLD (good and bad alike -- they
	 *  are indistinguishable here). Which tiles are good is only knowable from the shadow-realm
	 *  GameObjects ({@link #GOBJ_GOOD_SEED} 47573), so we scout there, translate, and walk the good
	 *  ones. We only count 2459 to keep the phase latched. Its appearance coincides with the fragment
	 *  charging usable (~3 ticks after the 10248 telegraph) and the "pulses with dark energy" chat. */
	public static final int GFX_OVERWORLD_SEED = 2459;
	/** Inventory menu option that activates the fragment (swaps realms). Captured menu action:
	 *  CC_OP id=2 itemId=28357 param0=0 param1=9764864 (inventory widget 149) option='Activate'. */
	public static final String BLACKSTONE_OPTION = "Activate";
	/** Chat line meaning the blackstone fragment is now activatable (ready to commune). */
	public static final String CHAT_BLACKSTONE_READY = "Your blackstone fragment pulses with dark energy";
	/**
	 * Appears after EVERY phase (a generic "phase ended" animation), so it is NOT a reliable
	 * marker for any specific phase/action -- do not gate on it. Kept only for reference.
	 */
	public static final int ANIM_ENTER_SHADOW_REALM = 10254;
	/** The shadow-realm region id (confirmed). The other arena region, 10595, is the overworld. */
	public static final int SHADOW_REALM_REGION = 9571;
	/**
	 * Realm overlay: in STATIC template/world coords the overworld arena is this many tiles
	 * EAST of the shadow arena, same Y (region bases: 10595 baseX 2624 - 9571 baseX 2368 = 256).
	 * The fight is instanced, so realm translation goes instance -> template -> +/-this on X ->
	 * template -> instance. See WhispererAutoMain#shiftRealm.
	 */
	public static final int REALM_X_OFFSET = 256;
	/** Instance planes per realm. Hardcoded for now (OW 0 / shadow 3); correct later if needed. */
	public static final int OVERWORLD_PLANE    = 0;
	public static final int SHADOW_REALM_PLANE = 3;

	// --- Whisperer animations ---------------------------------------
	// TODO: capture. Placeholders so references compile; -1 never matches a real anim.
	/** Phase-change animation. TENTATIVE (user to confirm). */
	public static final int ANIM_PHASE_CHANGE = 10247;
	/** The dormant Odd Figure (12203) plays this when disturbed/waking; we get knocked back to the start
	 *  tile and NPC 12204 (awake) spawns shortly after. */
	public static final int ANIM_ODD_FIGURE_WAKE = 10227;
	/** Corrupted-seeds phase ARMING (telegraph only). The shadow-realm commune still waits for
	 *  the blackstone "pulses with dark energy" chat line (CHAT_BLACKSTONE_READY) -- do NOT act
	 *  on this anim alone. */
	public static final int ANIM_SEED_PHASE_START = 10248;
	// --- Screech phase animations -----------------------------------
	/** Screech: charging the attack (also the pillar-HP read window). */
	public static final int ANIM_SCREECH_CHARGE   = 10251;
	/** Screech: the attack itself. */
	public static final int ANIM_SCREECH_ATTACK   = 10252;
	/** Screech: animation cooldown after the attack. */
	public static final int ANIM_SCREECH_COOLDOWN = 10250;
	/** Grace window (ticks): keep the screech phase considered active this many ticks after the
	 *  last screech animation, to bridge gaps between charge/attack/cooldown and detect the end. */
	public static final int SCREECH_GRACE_TICKS = 5;
	/** Soul-siphon START indicator: she is beginning the soul-siphon phase. */
	public static final int ANIM_SOUL_SIPHON_START  = 10256;
	/** Soul-siphon ACTIVE: she is performing the siphon (a timed event). */
	public static final int ANIM_SOUL_SIPHON_ACTIVE = 10257;
	/** Soul-siphon END: she finishes the siphon phase. Used as a backstop to end the phase + arm the
	 *  post-phase bind flee if we didn't already finish the clear (all 4 groups fired). */
	public static final int ANIM_SOUL_SIPHON_END = 10259;
	/** Whisperer death animation (the FINAL death, after the enrage phase). */
	public static final int ANIM_WHISPERER_DEATH = 10260;
	/** Active siphon lasts this many ticks, counted from the first detection of
	 *  ANIM_SOUL_SIPHON_ACTIVE (10257). */
	public static final int SOUL_SIPHON_DURATION_TICKS = 20;
	/** Enrage / screech telegraph. */
	public static final int ANIM_ENRAGE  = -1;
	/** Shadow-realm / tentacle special telegraph. */
	public static final int ANIM_SPECIAL = -1;

	// --- Arena --------------------------------------------------------
	/** The Whisperer arena spans two regions: 10595 = regular world, 9571 = shadow realm. */
	public static final int[] ARENA_REGIONS = { 9571, 10595 };
	/** Regular (overworld) region. */
	public static final int OVERWORLD_REGION = 10595;
	/** Primary region, used only as the capture-logger default (CaptureLogger takes one id).
	 *  Set the capture config to -1 to log across both regions while capturing. */
	public static final int ARENA_REGION = 9571;

	// --- Supplies (item ids) -----------------------------------------
	// Minimal magic-kill starter: sharks + prayer potion. TODO: add offensive boost
	// (e.g. imbued/saturated heart or magic potion) and any utility potions later.
	public static final int FOOD_PRIMARY       = 385;   // Shark

	public static final int[] PRAYER_POT_DOSES = { 143, 141, 139, 2434 };  // Prayer potion (1..4)

	// --- Combat ------------------------------------------------------
	/** Fallback attack cooldown in ticks, used before the weapon config is bound.
	 *  Per-weapon speed/range live in {@link WhispererWeapon} / the config. */
	public static final int WEAPON_COOLDOWN = 5;
}
