package net.runelite.client.plugins.vorkathAuto;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Perspective;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.plusUtils.Clicker;
import net.runelite.client.plugins.plusUtils.StepOverlay;
import org.apache.commons.lang3.tuple.Triple;

import java.util.concurrent.atomic.AtomicBoolean;
import java.awt.*;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.Deque;
import java.util.List;
import java.util.stream.IntStream;

@lombok.extern.slf4j.Slf4j
public class VorkathAutoMain implements Runnable {
    /** Shared error sink — slf4j + append to ~/vorkath-errors.log. Used by
     *  the outer worker catch, Plugin @Subscribe catches, and installed as the
     *  default uncaught exception handler in startUp so nothing goes silent. */
    public static void logCaught(String context, Throwable ex) {
        try { log.error("[VorkathAuto {}]", context, ex); } catch (Throwable ignore) {}
        try {
            java.io.File f = new java.io.File(
                    System.getProperty("user.home"), "vorkath-errors.log");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.FileWriter(f, true))) {
                pw.println("---- " + new java.util.Date() + " [" + context + "] ----");
                ex.printStackTrace(pw);
            }
        } catch (Throwable ignore) { /* best-effort */ }
    }

    private final Client client;
    private final ClientThread clientThread;
    private final StepOverlay overlay;
    private final VorkathAutoPlugin plugin;
    private final EventBus eventBus;

    // ============================================================================
    // [SHARED] — fields both non-combat and combat read
    // ============================================================================
    public static boolean doVorkath = false;                             // [SHARED] the master combat gate
    // CopyOnWriteArrayList: read from Main.run() worker thread, written from Plugin's
    // ItemContainerChanged on the client thread. CoW iterators snapshot on creation so
    // for-each loops on the worker never throw ConcurrentModificationException even when
    // the player drinks a potion / eats mid-iteration.
    public static final List<Item> currentInventory = new java.util.concurrent.CopyOnWriteArrayList<>(); // [SHARED]
    /** Plugin @Subscribe writes, worker reads. Cross-tile+id key uniqueness is
     *  enforced by the writer (Plugin.on*Item*). */
    public static final List<GroundEntry> currentGroundItems = new java.util.concurrent.CopyOnWriteArrayList<>(); // [SHARED]

    /** Ground item tracked in our own map — no reliance on GroundItemsPlugin's package-private types. */
    public static final class GroundEntry {
        public final WorldPoint tile;
        public final int itemId;
        public volatile int quantity;
        /** Client tick when this entry was first added. Used by aggregateGroundItems
         *  to distinguish "unconfirmed new drops from the just-completed kill" (spawn
         *  tick >= lastVorkathDeathTick, gated on isLootReady) from "older leftover
         *  loot from a previous kill" (always safe to pick up). Prevents mid-fight
         *  pre-pickup of a hide that endgame would then drop to make room for a
         *  bigger drop from this same kill. */
        public final int spawnTick;
        public GroundEntry(WorldPoint t, int id, int q, int spawnTick) {
            tile = t; itemId = id; quantity = q; this.spawnTick = spawnTick;
        }
        @Override public String toString() { return "GE(" + itemId + "x" + quantity + "@" + tile + "t=" + spawnTick + ")"; }
    }

    // ---------- Loot constants + mode enum (formerly VorkathAutoLoot.java) ----------
    // Nested to avoid a separate-file compile ordering issue in some IDE builds.

    /** Guaranteed Vorkath drop, unstackable — 2 per kill = 2 inventory slots. Mandatory. */
    public static final int LOOT_SUPERIOR_DRAGON_BONES = ItemID.SUPERIOR_DRAGON_BONES;

    /** Guaranteed Vorkath drop, unnoted here — 2 per kill = 2 slots. Ranks by GPS. */
    public static final int LOOT_BLUE_DRAGONHIDE       = ItemID.BLUE_DRAGONHIDE;

    /** Inventory items that must NEVER be dropped. Setup: one rune pouch + one slayer staff. */
    public static final Set<Integer> LOOT_PROTECTED_ITEM_IDS = Set.of(
        ItemID.RUNE_POUCH,      // regular rune pouch (12791)
        ItemID.SLAYERS_STAFF    // slayer staff (4170)
    );

    /**
     * Loot decision mode:
     *   CONSERVATIVE: still killing. Respect MIN_* supply floors.
     *   AGGRESSIVE:   TPing out anyway. Drop anything except protected items.
     */
    public enum LootMode { CONSERVATIVE, AGGRESSIVE }
    public static final List<Item> currentBank      = new java.util.concurrent.CopyOnWriteArrayList<>(); // [SHARED]
    public static final List<Item> currentEquipment = new java.util.concurrent.CopyOnWriteArrayList<>(); // [SHARED]

    // ============================================================================
    // [NON_COMBAT] — travel / bank / POH
    // ============================================================================
    public static boolean needRechargeStamina = true;      // [NON_COMBAT]
    public static boolean isIdle = true;                   // [NON_COMBAT]
    public static int essenceRemaining = 0;                // [NON_COMBAT]
    private boolean awaitingMovement = false;              // [NON_COMBAT]
    public volatile boolean isTalkingToBanker = false;     // [NON_COMBAT]
    public static boolean isTeleportingPOH = false;        // [NON_COMBAT]
    public static TileObject pendingClickboxObject = null; // [NON_COMBAT]
    public static Shape pendingClickboxShape = null;       // [NON_COMBAT]

    // [NON_COMBAT] hardcoded inventory / bank pixel positions
    public final Point invSlot1 = new Point(772, 763);
    public final Point invSlot2 = new Point(816, 763);
    public final Point invSlot3 = new Point(859, 763);

    // [SHARED] region ids used by both non-combat (travel) and combat (isInVorkathRegion)
    public final int LUNAR_ISLE_REGION = 8253; // maybe 8509?
    public final int RELLEKA_REGION = 10553;
    public final int UNGEAL_REGION = 9023;
    public final int POH_REGION = 7534;

//    public final Point invSlot5 = new Point(775, 799);
    public final Point invSlot28 = new Point(900, 977);
    public final Point equipmentRingSlot = new Point(895, 920); // equipment ring slot
    public final Point depositAllSlot = new Point(540, 823);

    public final Point superCombatBank = new Point(472, 715); // super combat potion(4)
    public final Point extendedAntifireBank = new Point(523, 715); // extended antifire potion(4)
    public final Point extendedAntiVenom = new Point(472, 750); // extended anti venom(4)
    public final Point prayerPotionBank = new Point(523, 750); // prayer potion (4)
    public final Point karambwanBank = new Point(472, 788);
    public final Point sharkBank = new Point(523, 788);

    // [SHARED] potion item-id lists — combat sips, non-combat counts/withdraws
    private static final List<Integer> DIVINE_SUPER_COMBAT_IDS = List.of(
        ItemID.DIVINE_SUPER_COMBAT_POTION1,
        ItemID.DIVINE_SUPER_COMBAT_POTION2,
        ItemID.DIVINE_SUPER_COMBAT_POTION3,
        ItemID.DIVINE_SUPER_COMBAT_POTION4
    );

    private static final List<Integer> SUPER_COMBAT_IDS = List.of(
        ItemID.SUPER_COMBAT_POTION1,
        ItemID.SUPER_COMBAT_POTION2,
        ItemID.SUPER_COMBAT_POTION3,
        ItemID.SUPER_COMBAT_POTION4
    );

    private static final List<Integer> EXTENDED_SUPER_ANTIFIRE_IDS = List.of(
        ItemID.EXTENDED_SUPER_ANTIFIRE1,
        ItemID.EXTENDED_SUPER_ANTIFIRE2,
        ItemID.EXTENDED_SUPER_ANTIFIRE3,
        ItemID.EXTENDED_SUPER_ANTIFIRE4
    );

    private static final List<Integer> EXTENDED_ANTIFIRE_IDS = List.of(
        ItemID.EXTENDED_ANTIFIRE1,
        ItemID.EXTENDED_ANTIFIRE2,
        ItemID.EXTENDED_ANTIFIRE3,
        ItemID.EXTENDED_ANTIFIRE4
    );

    private static final List<Integer> EXTENDED_ANTIVENOM_IDS = List.of(
        ItemID.EXTENDED_ANTIVENOM1,
        ItemID.EXTENDED_ANTIVENOM2,
        ItemID.EXTENDED_ANTIVENOM3,
        ItemID.EXTENDED_ANTIVENOM4
    );

    private static final List<Integer> PRAYER_POTION_IDS = List.of(
        ItemID.PRAYER_POTION1,
        ItemID.PRAYER_POTION2,
        ItemID.PRAYER_POTION3,
        ItemID.PRAYER_POTION4
    );

    // [NON_COMBAT] Bank withdraw dose-preference lists. Order = smallest dose
    // FIRST — one full trip only ever needs one sip of super combat / antifire /
    // antivenom, so a (2) is functionally identical to a (4). Preferring the
    // smaller dose leaves the (4)s in the bank for future trips (avoids the
    // failure mode where the bank has plenty of doses split across (2/3) and
    // we skip the withdraw because no (4) exists). (1) is intentionally
    // omitted — a single dose gives no in-fight sip headroom. buildVorkath-
    // Withdraw() resolves these against currentBank at withdraw time.
    private static final List<Integer> SUPER_COMBAT_WITHDRAW_PREF = List.of(
        ItemID.SUPER_COMBAT_POTION2,
        ItemID.SUPER_COMBAT_POTION3,
        ItemID.SUPER_COMBAT_POTION4);
    // SUPER extended antifire — Vorkath's dragonbreath needs the super variant
    // to be fully nulled; regular extended antifire lets breath through and
    // breaks the fight setup. superAntifireExpiryTick / onSippedSuperAntifire
    // are the actual gates the fight uses, so the withdraw list must match.
    private static final List<Integer> EXTENDED_ANTIFIRE_WITHDRAW_PREF = List.of(
//        ItemID.EXTENDED_SUPER_ANTIFIRE2,
        ItemID.EXTENDED_SUPER_ANTIFIRE3,
        ItemID.EXTENDED_SUPER_ANTIFIRE4);
    private static final List<Integer> EXTENDED_ANTIVENOM_WITHDRAW_PREF = List.of(
//        ItemID.EXTENDED_ANTIVENOM2,
        ItemID.EXTENDED_ANTIVENOM3,
        ItemID.EXTENDED_ANTIVENOM4);

    private Runnable pendingAction = null;          // [NON_COMBAT]
    private WorldPoint lastLocation = null;          // [NON_COMBAT]
    private long lastMovementTime = 0;               // [NON_COMBAT]
    public long start;                               // [NON_COMBAT]
    private final int STAMINA_THRESHOLD = 20;        // [NON_COMBAT]
    public volatile boolean isRunning = false;       // [SHARED] top-level loop control — instance field, Plugin owns lifecycle
    public int breakCounter = 0;                     // [NON_COMBAT]
    public static boolean doWooxWalk = false;        // [COMBAT]

    // ============================================================================
    // [COMBAT] — fields used inside the doVorkath fight only
    // ============================================================================

    // Debounce so we don't spam-click the same prayer toggle multiple times.
    private static final int PRAY_COOLDOWN_TICKS = 2;
    private int lastPrayGameTick = -PRAY_COOLDOWN_TICKS;

    // Vorkath-alive tracker. When false, ALL prayers are forced off during reconcilePrayers.
    @Getter
    private volatile boolean vorkathAlive = false;

    // Tick deadline for re-issuing walk clicks to LOOT_STACK_TILE during Vorkath's
    // death animation. onVorkathDeath sets this to (currentTick + DEATH_ANIM_WALK_TICKS).
    // tickWalkToLootStack() re-clicks each onGameTick until the deadline, so path
    // deviations (blocked-by-mob, dropped click) still converge on the center tile
    // before the drop pile spawns. -1 = inactive.
    private volatile int walkingToLootStackUntilTick = -1;

    // Set true by onKillCountMessage() when the "Your Vorkath kill count is:"
    // chat event fires — the server prints it on the tick the drop lands, so
    // it's a reliable signal that the loot pile has spawned. Both the endgame
    // loot-pass trigger and the trip-end TP gate wait on this so we never
    // leave the arena (or plan the loot pass) before the pile is on the ground.
    // Reset in onVorkathSpawn (next kill's start) — never on death, or a chat
    // event arriving same-tick as the reset would be lost.
    private volatile boolean killCountSeenThisKill = false;

    // Tick the kill-count chat message actually fired. ChatMessage and
    // ItemSpawned events land on the same server tick but the client-thread
    // delivery order isn't guaranteed — if chat fires first the worker can see
    // killCountSeen=true + empty currentGroundItems (leftovers already drained,
    // new drops not yet added) and TP prematurely. Adding TICKS_AFTER_KC_FOR_-
    // DROPS to this tick before treating loot as ready gives the ItemSpawned
    // events time to populate currentGroundItems first.
    private volatile int killCountSeenTick = -1;
    private static final int TICKS_AFTER_KC_FOR_DROPS = 2;

    // Tick onVorkathDeath fired — powers the LOOT_SAFETY_TICKS deadline that
    // releases the kill-count gate if RuneLite ever drops the chat event.
    private volatile int lastVorkathDeathTick = -1;

    // Ticks past onVorkathDeath after which isLootReady() returns true even if
    // the kill-count message never fires. Insurance against a dropped chat
    // event so we never stall on the loot tile forever. ~6s at 600ms/tick.
    private static final int LOOT_SAFETY_TICKS = 10;

    // Debounce for clickOnTp — the outer worker loop iterates every ~1ms,
    // and Teleport-to-House takes ~5 ticks server-side to resolve. Without
    // this the loop queues thousands of clientThread.invoke(menuAction) TP
    // widget clicks during the pre-arrival window; those keep draining and
    // firing against the POH spellbook after we've already arrived. Setting
    // this to the actual TP resolution time (~5 ticks) plus slack means one
    // click per intent, no queued backlog.
    private volatile int lastTpClickTick = -1000;
    private static final int TP_DEBOUNCE_TICKS = 2;

    // In-flight guard for startEndgameLootPass. buildEndgameLootPlan hits
    // itemManager.getItemPrice / getItemComposition, which assert client-
    // thread — we hop over via clientThread.invoke. The worker loop iterates
    // every ~1ms; without this it queues hundreds of pending plan builds
    // while the previous one is still running (or waiting to run). CAS'd
    // true when we submit, cleared inside the invoke after work completes.
    private final AtomicBoolean endgamePlanInFlight = new AtomicBoolean(false);

    // Desired ("expected") prayer states — reconcilePrayers compares to actual and toggles.
    // @Setter dropped so we can gate writes on isRunning below. @Getter kept.
    @Getter
    private volatile boolean magePrayOn = false;
    @Getter
    private volatile boolean pietyOn = false;

    /** Set desired mage-prot state. No-op when the plugin isn't running so
     *  Plugin-side event handlers can't flip prayer intent while we're stopped. */
    public void setMagePrayOn(boolean v) {
        if (!isRunning) return;
        magePrayOn = v;
    }
    /** Set desired piety state. Same isRunning gate as setMagePrayOn. */
    public void setPietyOn(boolean v) {
        if (!isRunning) return;
        pietyOn = v;
    }

    // Getters (mostly for debugging / cross-checks).
    // Setters used by Plugin handlers.
    // === Woox walk state (moved from Plugin) ===
    @Getter
    @Setter
    private volatile boolean vorkathAcidAnim = false;
    @Getter
    @Setter
    private volatile int currentWalkX = -1;
    // Horizontal woox-walk fallback — set only when bestColumnForWooxWalk returned
    // -1 (every column has acid) AND longestCleanRowForWooxWalk found a run of >=3.
    // Oscillates east↔west between the two endpoints on their shared row. Nulled
    // out when the acid phase ends (same lifetime as currentWalkX).
    private volatile WorldPoint horizontalWooxA = null;   // west endpoint
    private volatile WorldPoint horizontalWooxB = null;   // east endpoint

    /** Set the horizontal woox-walk endpoints. Passing (null, null) clears the
     *  mode; passing a valid pair puts wooxWalkStep in east-west mode until acid
     *  ends. Also clears currentWalkX so the two modes never mix. */
    public void setHorizontalWooxWalk(WorldPoint west, WorldPoint east) {
        horizontalWooxA = west;
        horizontalWooxB = east;
        if (west != null || east != null) currentWalkX = -1;
    }
    // Edge-detect for woox-walk end-of-acid recovery: true iff vorkathAcidAnim was true
    // on the previous wooxWalkStep tick. Used to fire clickOnVorkath() once when the acid
    // animation ends while the player is stranded on a non-4061 row.
    private volatile boolean wasAcidLastTick = false;
    /** True if we toggled RUN off for the current acid cycle; used to toggle it back on when acid ends. */
    private volatile boolean runToggledOffForAcid = false;
    /** OSRS run varp — 1 = run enabled, 0 = walk. */
    private static final int VARP_RUN_ENERGY_STATE = 173;

    // === Main weapon detection ===
    // The equipped 2h/main weapon affects the woox-walk SOUTH safe row Y:
    //   - Dragon hunter lance → Y = 4059 (attack range 2)
    //   - Osmumten's fang     → Y = 4058 (attack range 1)
    // Computed live from currentEquipment on every call (no cache — a cached field
    // in a static container can go stale across plugin start/stop cycles).
    // Default when no known weapon is worn: FANG (larger step south, safer).
    public enum MainWeapon { LANCE, FANG }

    // Latch: once we fire a 1481 dodge for a given projectile wave, don't fire another
    // for that same wave. Reset when 1481 is no longer in flight.
    private volatile boolean dodge1481Fired = false;     // legacy, unused after latch removal
    private volatile boolean has1481LastTick = false;
    private volatile boolean dodged1481ThisWave = false; // remember to re-attack once 1481 leaves

    // Food cooldown: OSRS enforces a 3-tick eat cooldown. Set inside eatFood(), read by onFoodCooldown().
    private volatile int lastEatTick = -100;
    // Per-buff sip guards. Server can take ~1 game tick to broadcast a varbit change back,
    // so the mirror may still show 'buff off' immediately after we sip. These block
    // re-firing the same sip within 3 ticks even if the mirror hasn't caught up yet.
    private volatile int lastAntifireSipTick  = -100;
    private volatile int lastAntivenomSipTick = -100;
    private volatile int lastPrayerSipTick    = -100;
    // Attack click cooldown — same pattern as sip cooldowns. Blocks re-firing the
    // Vorkath attack while the mirrored getInteracting() is still stale from the last click.
    private volatile int lastAttackClickTick  = -100;
    // Global cooldown inside clickOnVorkath to prevent packet spam if multiple callers
    // (run loop / wooxWalkStep / reAttackIfBroken / recovery) fire in the same tick.
    private volatile int lastVorkathClickTick = -100;
    // Deferred single-tick action fired during Vorkath's 8058 wake-up animation (or on the first tick
    // he becomes 8061 if 8058 was skipped). Set by preFightTopOff() when only one combo/sip remains,
    // so we can poke early and overlap the last consumption with the wake-up.
    private volatile Runnable pendingPostPokeAction = null;

    // ==== Client-state mirrors ====
    // Client APIs like getVarbitValue / getBoostedSkillLevel must be called on the client thread.
    // The worker loop (Main.run) reads these instead — Plugin.onClientTick pushes fresh values
    // every ~16ms via updateMirroredGameState(). Read staleness is at most 1 client tick.
    private volatile int mirrorSuperAntifireVarbit = 0;
    private volatile int mirrorAntifireVarbit      = 0;   // regular antifire (fang loadout)
    private volatile int mirrorPoisonVarp          = 0;
    private volatile boolean mirrorRunEnabled       = false;  // OSRS run varp mirrored from client thread
    private volatile int mirrorCurrentHP           = 0;
    private volatile int mirrorMaxHP               = 0;
    private volatile int mirrorCurrentPrayer       = 0;
    private volatile int mirrorMaxPrayer           = 0;
    // Combat skill boosts — mirrored so worker-thread code (consume(),
    // pendingPostPokeAction Runnable) can compute superCombatBoostMin() safely.
    private volatile int mirrorAttackBoost         = 0;
    private volatile int mirrorStrengthBoost       = 0;
    private volatile int mirrorDefenceBoost        = 0;
    private volatile boolean mirrorPlayerAttackingVorkath = false;
    private volatile boolean mirror395InFlight              = false;   // gate attack while spawn spider spec is airborne
    // Volatile mirror of "is the wake-up NPC (id 8058) in the scene?", maintained
    // by Plugin's NpcSpawned / NpcDespawned handlers on the client thread. The
    // worker reads this instead of calling hasNpc(8058) so we never iterate
    // client.getNpcs() off-thread during the composition-swap tick — that
    // iteration is what makes downstream plugins iterating the scene during
    // render see a briefly-null composition slot and NPE.
    public volatile boolean mirrorHasWakeupNpc = false;

    // Single-thread executor for movement-click spam. Kept off the client thread so
    // robot.delay(100) inside clickPoint doesn't freeze the game. Cancellation is a
    // monotonic generation counter — bumping it makes any in-flight spam loop exit.
    private final java.util.concurrent.ExecutorService clickExecutor =
        java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "VorkathAuto-clicker");
            t.setDaemon(true);
            return t;
        });
    private volatile int clickGeneration = 0;
    private static final int SPAM_MAX_ATTEMPTS   = 2;      // cap dispatches per wave — one primary, one safety
    private static final long SPAM_DEADLINE_MS   = 500;    // hard timeout per wave

    // Zombified spawn (8063) alive tracker. When true, the engine short-circuits so the
    // player can attack the spawn manually without the plugin's clicks hijacking their mouse.
    public volatile boolean zombifiedSpawnAlive = false;
    /** Set on the 395 (spider spec) rising edge, cleared on spawn death. Covers
     *  the short gap between the 395 projectile clearing and the zombified spawn
     *  appearing — during which mirror395InFlight is false and zombifiedSpawnAlive
     *  is not yet true, letting the main-loop C branch sneak in an auto-attack on
     *  Vorkath. Longer-lived than either individual signal. */
    public volatile boolean inSpawnPhase = false;
    /** Set true by equipSlayerStaff() when the staff isn't in inventory (nothing
     *  to wield). Read by clickOnZombifiedSpawn() to route to the manual-cast
     *  branch instead of the auto-attack branch. Recomputed each equipSlayerStaff
     *  call, so a mid-trip inventory change is picked up on the next spawn cycle. */
    public volatile boolean manualCast = false;

    // === Buff-management thresholds (tune these) ===
    public int HP_THRESHOLD              = 40;   // eat shark below this → P2 combo
    public int SHARK_HEAL_HP             = 20;   // shark restores 20 HP — used by opportunisticTopOff
    public int KARAMBWAN_HEAL_HP         = 18;   // karambwan restores 18 HP
                                                //  the shark+pot+karambwan triple-combo overheal check
    public int NORMAL_PRAYER_THRESHOLD   = 50;

    // [SHARED] MIN_* thresholds — read by hasEnoughSupplies (non-combat entry decision)
    public int MIN_BUFF_TIME_TICKS      = 200;
    public int MIN_SUPER_ANTIFIRE_DOSES = 1;
    public int MIN_ANTIVENOM_DOSES      = 1;
    public int MIN_PRAYER_DOSES         = 1;
    // Bank-entry / pre-poke prayer floor. Checked as EFFECTIVE prayer
    // (currentPrayer + doses × per-pot restore). One Vorkath kill drains
    // roughly 20-40 prayer with piety + mage prot; 30 covers a solid
    // one-kill runway. NOT checked mid-fight — a low-HP Vorkath finishes
    // before draining much prayer, so the mid-fight threshold is unreliable.
    public int MIN_PRAYER_POINTS_START  = 30;
    public int MIN_SHARKS               = 2;
    public int MIN_KARAMBWAN            = 0;
    public int BUFF_EXPIRING_SOON_TICKS  = 10;  // sip when < 60s remaining   // prayer at/below → P3 sip (lower priority)
    public boolean antifireExpireMessage = false;
    public boolean antivenomExpireMessage = false;

    // === Attack lock tracking (Plugin's AnimationChanged handler feeds lastAttackTick) ===
    // Attack lock is auto-maintained by the client on a 5-tick cycle once attackNpc fires.
    // Only actions (sip/eat/move) break it. Track last attack purely for diagnostics.
    private volatile int lastAttackTick = -100;
    /** Tick when we last fired a poke click at sleeping Vorkath (8059). Used to
     *  retry the poke if 8058 wake-up doesn't appear within a few ticks — the
     *  click can silently miss (e.g. 8059 not in scene yet, target teardown). */
    private volatile int lastPokeTick = -100;

    // === Woox-walk direction tracker: true while walking south (away from Vorkath) ===
    // Queue actions only run during walking-away ticks (P0-P3 gate on this during acid).
    private volatile boolean wooxWalkingAway = true;

    // [NON_COMBAT] chat-driven flag — Plugin sets on "You board the boat" chat message
    public volatile boolean boardedBoat = false;

    // [SHARED] buff timers — combat writes on sip, non-combat reads for hasEnoughSupplies
    private static final int SUPER_ANTIFIRE_DURATION_TICKS = 600;   // extended super antifire: 6 min
    private static final int ANTIFIRE_DURATION_TICKS       = 1200;  // extended antifire: 12 min
    private static final int ANTIVENOM_DURATION_TICKS      = 600;   // extended super anti-venom+: 6 min
    private static final int DIVINE_SUPER_COMBAT_DURATION_TICKS   = 500;   // divine super combat: 5 min

    private volatile int superAntifireExpiryTick = 0;
    private volatile int antifireExpiryTick      = 0;
    private volatile int antivenomExpiryTick     = 0;
    private volatile int superCombatExpiryTick   = 0;

    Clicker clicker;
    BreakScheduler scheduler;
    private final ItemManager itemManager;

    public static final Map<TileObject, Shape> clickboxCache = new HashMap<>();

    VorkathAutoMain(Client client, ClientThread clientThread, StepOverlay overlay, VorkathAutoPlugin plugin,
                    EventBus eventBus, ItemManager itemManager) {
        this.client = client;
        this.clientThread = clientThread;
        this.overlay = overlay;
        this.plugin = plugin;
        this.eventBus = eventBus;
        this.itemManager = itemManager;
        clicker = new Clicker(client);
        scheduler = new BreakScheduler();
        // Thread creation moved to Plugin.startMain() so lifecycle is explicit.
    }

    /**
     * Signal the worker to exit its run loop. Called by Plugin.stopMain() — Plugin also
     * interrupts the thread so any Thread.sleep in waitFor / waitForRegion exits promptly.
     * Also clears doVorkath so onGameTick paths (loot, evaluateActionsThisTick,
     * preFightTopOff triggers, etc.) that don't depend on the worker are silenced too.
     */
    public void stop() {
        isRunning = false;
        doVorkath = false;
        pendingPostPokeAction = null;
        clearLootPass();
        reset();
    }

    public void reset() {
        isIdle = true;
        clearLootPass();
    }

    /**
     * Live main-weapon lookup — reads currentEquipment on every call, no cache.
     * LANCE iff a Dragon hunter lance is worn; otherwise FANG (safe default).
     *
     * currentEquipment is a static CopyOnWriteArrayList populated by
     * onItemContainerChanged for InventoryID.EQUIPMENT. Because it's static it can
     * survive plugin restarts with stale data, so caching the derived weapon was
     * unsafe — reads here always reflect the latest container payload.
     */
    public MainWeapon getMainWeapon() {
        for (Item it : currentEquipment) {
            if (it == null) continue;
            if (it.getId() == ItemID.DRAGON_HUNTER_LANCE) return MainWeapon.LANCE;
        }
        return MainWeapon.FANG;
    }

    /** Woox-walk south safe row: lance = 4059, fang = 4058. Recomputed each call. */
    public int southSafeRowY() {
        return (getMainWeapon() == MainWeapon.LANCE) ? 4059 : 4058;
    }

    // execution of thread starts from run() method
    public void run()
    {
        while (isRunning) {
            if (client.getGameState() != GameState.LOGGED_IN
                    || client.getLocalPlayer() == null) {
                try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                continue;
            }

            // Global gate: doVorkath is only valid while the Vorkath template region is loaded.
            // getRegionID() in an instance returns the fake instance grid — use getMapRegions()
            // which lists the template region ids of the chunks pulled in to build the instance.
            if (doVorkath && !isInVorkathRegion()) {
                overlay.setCurrentStep("STOP DOING VORKATH");
                doVorkath = false;
                vorkathAlive = false;         // clear so reconcilePrayers forces prayers off
                zombifiedSpawnAlive = false;  // scene NPCs are gone, clear stale combat state
                pendingPostPokeAction = null; // don't fire a leftover sip in POH
                clearLootPass();              // no scene NPCs / no valid ground refs — drop plan
                currentGroundItems.clear();   // WorldPoint refs no longer valid outside arena
            }

            // Update idle based on movement
            updateIdleStatus();

            // Retry pending actions if necessary
            checkActionSuccess();

            if (doVorkath) {
                overlay.setCurrentStep("DO VORKATH");
                // Two-tier supplies gate:
                //   fullSupplies  = hasEnoughSupplies() — bank-level MIN_* thresholds. Unlocks
                //                   the pre-fight top-off state machine (needs spare pots to sip).
                //   canContinue   = canContinueFight() — mid-fight survivability. HP above
                //                   HP_THRESHOLD AND some antifire coverage. Optimistic: even
                //                   if hasEnoughSupplies dropped, keep pushing for the kill on
                //                   Vorkath if we can still safely take a breath.
                boolean fullSupplies = hasEnoughSupplies();
                boolean canContinue  = canContinueFight();

                // A) Post-poke pending action fires during wake-up (8058) or first tick alive.
                //    Runs whenever queued — the sip/eat it holds was budgeted before the poke.
                if (pendingPostPokeAction != null && (mirrorHasWakeupNpc || vorkathAlive)) {
                    overlay.setCurrentStep("post-poke pending action");
                    pendingPostPokeAction.run();
                    pendingPostPokeAction = null;
                    clicker.delay(600);
                }
                // B) Sleeping-Vorkath window: pre-fight top-off / feasibility / poke
                //    when fullSupplies OR canContinue (we can push one more kill).
                //    Endgame branch (sort-loot + TP) fires only when BOTH fail —
                //    that's the "sacrifice only when we are truly done" trigger.
                else if (!vorkathAlive && !mirrorHasWakeupNpc) {
                    if (fullSupplies || canContinue) {
                        preFightTopOff();
                    } else {
                        // Trip-end path (no supplies + can't continue). Two gates
                        // must pass before we TP:
                        //  (1) isLootReady() — the kill-count chat msg has fired
                        //      (or safety deadline hit). Drop pile is confirmed on
                        //      the ground, not just about to spawn. Planning against
                        //      an unspawned pile builds an empty/partial plan we'd
                        //      have to redo when the drop lands.
                        //  (2) currentGroundItems drained + no pending steps — the
                        //      endgame pass has actually picked everything up, so
                        //      TP-ing now doesn't strand loot on the tile.
                        if (!isLootReady()) {
                            overlay.setCurrentStep("wait for kill-count msg — loot spawning");
                            clicker.delay(600);
                        } else {
                            // No kill happened this session — nothing on the ground is
                            // legitimately ours to loot. Mirrors the isLootReady() <=0
                            // fast-path: bypass the async plan/reconcile roundtrip that
                            // would otherwise deadlock on stale/phantom entries (climbed
                            // over ice chunks with low supplies, never fought Vorkath).
                            // Drop the list synchronously so the TP-out gate below fires
                            // this same tick, not after a client-thread invoke returns.
                            if (lastVorkathDeathTick <= 0 && !currentGroundItems.isEmpty()) {
                                currentGroundItems.clear();
                            }
                            // Endgame loot pass: sacrifices droppable inv slots to grab any
                            // remaining valuable ground items (>= blue hide value). Runs only
                            // in this branch — we're about to TP out, so surplus supplies are
                            // fair game. Rebuild whenever the queue drained and ground items
                            // remain; tickLootPass then executes it one action per two ticks.
                            if (!currentGroundItems.isEmpty() && pendingLootSteps.isEmpty()) {
                                overlay.setCurrentStep("plan endgame loot pass");
                                startEndgameLootPass();
                            }
                            if (currentGroundItems.isEmpty() && pendingLootSteps.isEmpty()) {
                                overlay.setCurrentStep("actually TP out");
                                clickOnTp();
                            }
                        }
                    }
                }
                // C) Vorkath alive: attack if we're not already interacting. This runs on
                //    fullSupplies OR the optimistic canContinue path — the user wants us
                //    to keep pushing the kill as long as HP is safe and antifire holds.
                else if (vorkathAlive
                        && (fullSupplies || canContinue)
                        && !zombifiedSpawnAlive
                        && !vorkathAcidAnim
                        && !isPlayerAttackingVorkath()
                        && (client.getTickCount() - lastAttackClickTick) >= 3) {
                    overlay.setCurrentStep(fullSupplies
                        ? "attack Vorkath (not interacting)"
                        : "attack Vorkath (low supplies — pushing kill)");
                    lastAttackClickTick = client.getTickCount();
                    clickOnVorkath();
                    clicker.delay(600);
                }
                // D) Vorkath alive but we can no longer safely fight (HP low OR no
                //    antifire coverage) — bail out.
                else if (vorkathAlive && !fullSupplies && !canContinue) {
                    overlay.setCurrentStep("unsafe to continue — TP out");
                    clickOnTp();
                }
            } else if (!isMoving()) {
                if (boardedBoat && !isInsideInstance()) {
                    while (plugin.getLocalPlayerLocation() == null
                            || plugin.getLocalPlayerLocation().getRegionID() != UNGEAL_REGION) {
                        overlay.setCurrentStep("boat travel");
                        clicker.delay(100);
                    }
                    boardedBoat = false;
                }
                // LUNAR ISLE
                else if(getRegionID() == LUNAR_ISLE_REGION) {
                    overlay.setCurrentStep("at lunar isle");

                    if (isWorldPointInArea(myWorldPoint(), VorkathAutoWorldPoints.LUNAR_ISLE_TELEPORT_ZONE) && isIdle) {
                        detachCameraPoint(VorkathAutoWorldPoints.LUNAR_ISLE_BANK_INFRONT, 4160, 1024, 896);
                        clicker.randomDelayStDev(150, 250, 25);
                        // Was clicker.clickWorldPoint — that route moves the OS mouse via
                        // Robot. clickTileWithRetry dispatches canvas MouseEvents directly
                        // so the real cursor isn't hijacked when we start the bank walk.
                        clickTileWithRetry(VorkathAutoWorldPoints.LUNAR_ISLE_BANK_INFRONT);
                        clicker.randomDelayStDev(1800, 2000, 25);
//                        setZoomPitchYaw(450, 4160, 0);
//                        tryAction(this::clickBank);
                    } else if (isAtWorldPoint(VorkathAutoWorldPoints.LUNAR_ISLE_BANK_INFRONT) && isIdle) {
                        // if bank is open
                        clicker.randomDelayStDev(350, 450, 25);
                        if (isBankOpen()) {
                            // rotate camera so sirsal banker is bottom right
                            setZoomPitchYaw(896, 4160, 8192);
                            // break check
                            long breakMs = scheduler.isBreak(System.currentTimeMillis());
                            if (breakMs > 0) {
                                breakCounter++;
                                overlay.setCurrentStep("delay " + breakMs + "ms" + "(" + breakCounter + ")");
                                // break delay into safe chunks
                                for (long d = breakMs; d > 0; d -= 60_000) {
                                    clicker.delay((int) Math.min(d, 60_000));
                                }
                            } else {
                                overlay.setCurrentStep("break not needed" + "(" + breakCounter + ")");
                            }

                            if (hasItem(currentInventory, ItemID.SUPERIOR_DRAGON_BONES)) {
                                System.out.println("click deposit");
                                depositAll();
                            }

                            else if (!isReadyForVorkath()) {
                                // check if bank even has enough items
                                if (!checkBankForItemCounts()) {
                                    stop();
                                }

                                // withdraw all the items
                                bulkWithdrawBatched(2);
                            }

                            else {
                                while(isBankOpen()) {
                                    overlay.setCurrentStep("trying to close bank");
                                    clicker.pressKey(KeyEvent.VK_ESCAPE);
                                    clicker.randomDelayStDev(150, 250, 25);
                                }
                            }
                        } else if (!isBankOpen()) {
                            overlay.setCurrentStep("bank not open");

//                            if (!isReadyForVorkath() && isIdle && !hasItem(currentInventory, ItemID.SUPERIOR_DRAGON_BONES)) {
                            if (!isReadyForVorkath() && isIdle) {
                                overlay.setCurrentStep("click bank");
                                clickOnBankBooth();
//                                setZoomPitchYaw(896, 4160, 8192);
//                                clickBank();
//                                clicker.randomDelayStDev(500,650,25);
                            }
                            else if(isReadyForVorkath() && isIdle) {
                                overlay.setCurrentStep("ready for vorkath");
                                clicker.randomDelayStDev(300, 350, 25);
                                // TALK TO SIRSAL BANKER
                                if (!isTalkingToBanker) {
                                    overlay.setCurrentStep("talk to sirsal banker");
                                    clickOnSirsalBanker();
                                } else {
                                    overlay.setCurrentStep("chatting with sirsal banker");
                                    sirsalBankerChat();
                                }
                            }
                        }
                    }
                }
                else if (getRegionID() == RELLEKA_REGION) {
                    overlay.setCurrentStep("at relleka");
                    if (isDestinationTile(VorkathAutoWorldPoints.TORFINN_RELLEKA_TILE) || isAtWorldPoint(VorkathAutoWorldPoints.TORFINN_RELLEKA_TILE)) {
                        if (VorkathAutoNPCIDs.torfinn != null) {
                            overlay.setCurrentStep("speed click Torfinn");
                            boardedBoat = false;
                            clickOnTorfinn();

                            long torfinnStartMs = System.currentTimeMillis();
                            int torfinnIter = 0;
                            while (isRunning && !boardedBoat) {
                                clicker.delay(100);
                                torfinnIter++;
                                if (torfinnIter % 20 == 0) {
                                    overlay.setCurrentStep("retry click Torfinn");
                                    clickOnTorfinn();
                                }
                                if (System.currentTimeMillis() - torfinnStartMs > 10_000) {
                                    overlay.setCurrentStep("Torfinn timeout — bail to reeval");
                                    System.out.println("[Torfinn] boardedBoat never flipped after "
                                        + torfinnIter + " retries; bailing to state re-eval");
                                    break;
                                }
                            }
                        }
                    } else if (isAtWorldPoint(VorkathAutoWorldPoints.LUNAR_ISLE_EXILE_TILE)) {
                        overlay.setCurrentStep("moving to Torfinn");
                        detachCameraPoint(VorkathAutoWorldPoints.TORFINN_RELLEKA_TILE, 4160, 1024, 896);
                        clicker.randomDelayStDev(150, 250, 25);
                        // Same swap as the Lunar bank walk above — no Robot mouse move.
                        clickTileWithRetry(VorkathAutoWorldPoints.TORFINN_RELLEKA_TILE);
                        clicker.randomDelayStDev(150, 250, 25);
                        setZoomPitchYaw(600, 4160, 0);
                    }
                }

                else if (getRegionID() == UNGEAL_REGION) {
                    overlay.setCurrentStep("at Ungael");
                    isTalkingToBanker = false;
                    needRechargeStamina = true;

                    if (VorkathAutoObjectIDs.vorkathIceChunksOutside != null) {
                        overlay.setCurrentStep("not inside Vorkath zone");
                        if (isAtWorldPoint(VorkathAutoWorldPoints.TORFINN_UNGAEL_TILE) || isAtWorldPoint(VorkathAutoWorldPoints.BEFORE_VORKATH_ICE_CHUNKS_TILE)) {
                            overlay.setCurrentStep("click ice chunks");
                            clickOnIceChunks();
                        }
                    }
                }

                else if (isInVorkathRegion()){
                    overlay.setCurrentStep("inside Vorkath zone");
                    doVorkath = true;
                }
//
                // POH TRANSITION SCREEN
                else if(getRegionID() == 11826) {
                    overlay.setCurrentStep("AT HOUSE");
                }

                else if (isInPOHRegion()) {
                    overlay.setCurrentStep("in POH");

                    clicker.randomDelayStDev(150, 250, 25);

                    if (VorkathAutoObjectIDs.restorationPoolPOH == null
                            || VorkathAutoWorldPoints.INFRONT_OF_POOL == null) {
                        overlay.setCurrentStep("in POH — waiting for scene load");
                    } else if (isAtWorldPoint(VorkathAutoWorldPoints.INFRONT_OF_POOL)) {
                        setZoomPitchYaw(484, 4160, 0);
                        tryAction(this::clickOnPortalNexus);
                    } else if (needRechargeStamina) {
                        tryAction(this::clickOnRestorationPool);
                    }
                }
            }
        }
        System.out.println("Thread has stopped.");
    }

    public int getRegionID() {
        // Sentinel -1 when player location isn't known yet. plugin.localPlayerLocation
        // is populated once per ClientTick from client.getLocalPlayer().getWorldLocation(),
        // and either can be null during login, region loads, and instance transitions —
        // classic climb-in-then-NPE window. Every caller compares against known region
        // ids (UNGEAL_REGION, 11826, ...) so -1 naturally means "none of those".
        WorldPoint wp = plugin.getLocalPlayerLocation();
        return wp == null ? -1 : wp.getRegionID();
    }

    public boolean isInsideInstance()
    {
        return client.isInInstancedRegion();
    }

    private void depositAll() {
        overlay.setCurrentStep("banking");
//        action=CC_OP id=1 itemId=-1 param0=-1 param1=786479 option='Deposit inventory' target=''
        clicker.randomDelayStDev(250,350,25);
//        clicker.clickPoint(depositAllSlot);
        clientThread.invoke(() -> client.menuAction(
                -1, 786479, MenuAction.CC_OP, 1, -1, "Deposit inventory' target", ""
        ));
        clicker.randomDelayStDev(250,350,25);
    }

    /** Shared supply check — same criteria for bank vs inventory, differing
     *  only in which container is scanned. True if `source` has:
     *   - at least one (2/3/4) super combat / extended antifire / extended antivenom
     *   - >= 3 prayer potions (4)
     *   - >= 5 cooked karambwans
     *   - >= 16 sharks
     *  Counts changed from strict-equals to >= (was `== 16` etc): the strict
     *  form false-negatives when the bank has more than the trip needs (e.g.
     *  200 sharks stockpiled). Rune pouch is inventory-specific and stays in
     *  isReadyForVorkath. */
    private boolean hasVorkathSupplies(List<Item> source) {
        return hasAnyDose(source, SUPER_COMBAT_WITHDRAW_PREF)
            && hasAnyDose(source, EXTENDED_ANTIFIRE_WITHDRAW_PREF)
            && hasAnyDose(source, EXTENDED_ANTIVENOM_WITHDRAW_PREF)
            && getItemCount(source, ItemID.PRAYER_POTION4)   >= 3
            && getItemCount(source, ItemID.COOKED_KARAMBWAN) >= 5
            && getItemCount(source, ItemID.SHARK)            >= 16;
    }

    private boolean checkBankForItemCounts() {
        return hasVorkathSupplies(currentBank);
    }

    private boolean isReadyForVorkath() {
        // Inventory needs the same supply set as the bank, plus a rune pouch
        // (contents not verified — the pouch is opaque; user maintains counts).
        return hasVorkathSupplies(currentInventory)
            && hasItem(currentInventory, ItemID.RUNE_POUCH);
    }

    private void setZoomPitchYaw(int zoom, int pitch, int yaw) {
        setCameraZoom(zoom);
        setCameraPitch(pitch);
        setCameraYaw(yaw);
        clicker.randomDelayStDev(150,300,25);
    }

    private WorldPoint myWorldPoint() {
        WorldPoint wp = plugin.getLocalPlayerLocation();
        return new WorldPoint(
            wp.getX(),
            wp.getY(),
            wp.getPlane()
        );
    }

    private boolean isAtWorldPoint(WorldPoint worldPoint) {
        if (worldPoint == null) return false;
        WorldPoint myWp = toTemplate(plugin.getLocalPlayerLocation());
        if (myWp == null) return false;   // player loc unavailable (mid-climb, instance transition, etc.)
        return myWp.getX() == worldPoint.getX()
            && myWp.getY() == worldPoint.getY()
            && myWp.getPlane() == worldPoint.getPlane();
    }

    public static boolean isWorldPointInArea(WorldPoint wp, List<WorldPoint> area)
    {
        if (area == null || area.isEmpty())
            return false;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        int plane = area.get(0).getPlane();

        // compute bounds
        for (WorldPoint p : area)
        {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
        }

        return wp.getPlane() == plane
                && wp.getX() >= minX && wp.getX() <= maxX
                && wp.getY() >= minY && wp.getY() <= maxY;
    }

    private void setCameraYaw(int yaw) {
        if(client.getCameraYaw() == yaw)
            return;
        client.setCameraYawTarget(yaw);
    }

    private void setCameraPitch(int pitch) {
        if(client.getCameraPitch() == pitch)
            return;
        client.setCameraPitchTarget(pitch);
    }

    private void setCameraZoom(int zoom)
    {
        clientThread.invokeLater(() ->
                client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom)
        );
    }

    public boolean isMoving()
    {
        WorldPoint current = plugin.getLocalPlayerLocation();

        // First time
        if (lastLocation == null) {
            lastLocation = current;
            return false;
        }

        // Detect tile movement
        if (!current.equals(lastLocation)) {
            lastLocation = current;
            lastMovementTime = System.currentTimeMillis();
            return true; // walking or transitioning between tiles
        }

        // Smooth movement without tile change (model movement)
        if (System.currentTimeMillis() - lastMovementTime < 600)
            return true;

        // Teleports or cave-entering animation (~2796)
        Player p = client.getLocalPlayer();
        int anim = p.getAnimation();
        if (anim == 2796 || anim == 3265 || anim == 3266 || anim == 4069 || anim == 4071 || anim == 7305 || anim == 4412 || anim == 4413 || anim == 791) {
            if (anim == 4069 || anim == 4071)
                isTeleportingPOH = true;
            else if (anim == 7305)
                needRechargeStamina = false;
            return true;
        }

        return false;
    }

    public void updateIdleStatus()
    {
        if (isMoving())
        {
            // Movement started → reset all event signals
            overlay.setCurrentStep("moving to next");
            isIdle = false;
        }
        else if (!vorkathAlive) {
            // Character standing still
            overlay.setCurrentStep("char idle");
            isIdle = true;
        }
    }

    private void tryAction(Runnable action)
    {
        if (!isIdle)
            return;

        pendingAction = action;
        isIdle = false;
        awaitingMovement = true;   // Expect movement or animation
        action.run();

        clicker.delay(200);
    }

    private void checkActionSuccess()
    {
        if (pendingAction == null)
            return;

        // 1. SUCCESS: movement or animation detected
        if (isMoving())
        {
            pendingAction = null;
            awaitingMovement = false;
            return;
        }

        // 2. FAILURE: no movement AFTER we expected it
        if (awaitingMovement && isIdle && !isMoving() && essenceRemaining!=0)
        {
            overlay.setCurrentStep("retry");

            awaitingMovement = false;
            Runnable retry = pendingAction;
            pendingAction = null;

            tryAction(retry);
        }
    }

    public boolean hasItem(List<Item> items, int itemId)
    {
        List<Item> snapshot = new ArrayList<>(items);

        for (Item i : snapshot)
        {
            if (i != null && i.getId() == itemId)
                return true;
        }
        return false;
    }

    public int getItemCount(List<Item> items, int itemId)
    {
        int count = 0;
        List<Item> snapshot = new ArrayList<>(items);

        for (Item i : snapshot)
        {
            if (i != null && i.getId() == itemId)
                count += i.getQuantity();
        }

        return count;
    }

    private boolean isBankOpen() {
        return plugin.getIsBankOpen();
    }

    private void sirsalBankerChat() {
        overlay.setCurrentStep("chatting with banker");
        Widget chatboxLeft = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
        Widget chatboxRight = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT);

        if (chatboxLeft != null) {
            String s = chatboxLeft.getText();
            if (s.contains("What are you doing here, Fremennik?!")) {
                clicker.pressKey(KeyEvent.VK_SPACE);
            } else if (s.contains("No you do not! Begone!")) {
                clicker.pressKey(KeyEvent.VK_SPACE);
            }
        } else if (chatboxRight != null) {
            String s = chatboxRight.getText();
            if (s.contains("Hi, I...") || s.contains("I have a seal of pass...")) {
                clicker.pressKey(KeyEvent.VK_SPACE);
            }
        }
    }

    private void detachCameraPoint(WorldPoint wp, int pitch, int yaw, int zoom) {
        setZoomPitchYaw(zoom, pitch, yaw);
        centerCameraOnTile(wp);
    }

    public void centerCameraOnTile(WorldPoint tile)
    {
        client.setCameraMode(1);

        LocalPoint lp = LocalPoint.fromWorld(client, tile);
        if (lp == null)
            return;  // Tile not in loaded scene

        client.setCameraFocalPointX(lp.getX());
        client.setCameraFocalPointZ(lp.getY());
    }

    // USED LATER FOR DROPPING STUFF
//    getSlotOfItem(currentInventory, ItemID.BLOOD_RUNE)
//                .forEach(i -> {
//        clicker.clickPoint(inventoryCoords.get(i));
//        clicker.randomDelayStDev(250,350,25);
//    });

    // ------------------------------------------------------------------
    // Prayer flicking (moved from VorkathAutoPlugin). Triple stores:
    //   left   = menu target string (colored prayer name)
    //   middle = packed widget id
    //   right  = Prayer enum value (for isPrayerActive checks)
    // ------------------------------------------------------------------
    private final Map<String, Triple<String, Integer, Prayer>> prayerMap = new HashMap<>(Map.of(
            "mage",  Triple.of("<col=ff9040>Protect from Magic</col>",    35454997, Prayer.PROTECT_FROM_MAGIC),
            "range", Triple.of("<col=ff9040>Protect from Missiles</col>", 35454998, Prayer.PROTECT_FROM_MISSILES),
            "piety", Triple.of("<col=ff9040>Piety</col>",                 35455011, Prayer.PIETY)
    ));

    /**
     * Toggle a prayer: activates it if off, deactivates it if on.
     */
    public void pray(String key) {
        Triple<String, Integer, Prayer> info = prayerMap.get(key);
        if (info == null) return;
        clientThread.invoke(() -> client.menuAction(
                -1, info.getMiddle(), MenuAction.CC_OP, 1, -1, "Activate", info.getLeft()
        ));
    }

    /** Activate quick prayers via the orb widget. */
    public void activateQuickPrayers() {
        clientThread.invoke(() -> client.menuAction(
                -1, 10485780, MenuAction.CC_OP, 1, -1, "Activate", "Quick-prayers"
        ));
    }

    /** Activate run via the orb widget. */
    public void activateRun() {
        clientThread.invoke(() -> client.menuAction(
                -1, 10485788, MenuAction.CC_OP, 1, -1, "Toggle Run", ""
        ));
    }

    // ================= Potion buff timers =================

    /** Call after sipping super antifire — arms the local timer for ~3 minutes. */
    public void onSippedSuperAntifire() {
        superAntifireExpiryTick = client.getTickCount() + SUPER_ANTIFIRE_DURATION_TICKS;
        antifireExpireMessage = false;   // reset the chat-driven expiry flag on fresh sip
    }
    /** Call after sipping regular antifire — arms the local timer for ~6 minutes. */
    public void onSippedAntifire() {
        antifireExpiryTick = client.getTickCount() + ANTIFIRE_DURATION_TICKS;
        antifireExpireMessage = false;
    }
    /** Call after sipping anti-venom+ — arms the local timer for ~4 minutes. */
    public void onSippedAntivenom() {
        antivenomExpiryTick = client.getTickCount() + ANTIVENOM_DURATION_TICKS;
        antivenomExpireMessage = false;
    }
    /** Call after sipping super combat — arms the local timer for ~5 minutes. */
    public void onSippedSuperCombat()   { superCombatExpiryTick   = client.getTickCount() + DIVINE_SUPER_COMBAT_DURATION_TICKS; }

    // ---- Super combat rebuff trigger ----
    // Divine and regular are never held together — plugin sips whichever is in
    // inv. Divine keys off the 5-min timer (potion snaps off at expiry); regular
    // keys off boost magnitude (stats decay 1/min so timer is meaningless — we
    // rebuff when the smallest of A/S/D boost drops below this threshold).
    public static final int REGULAR_SUPER_COMBAT_REBUFF_BOOST = 10;

    /** Min boost across ATTACK, STRENGTH, DEFENCE (boosted - real). 0 when none.
     *  Worker-thread safe — reads mirrors updated on the client thread. */
    public int superCombatBoostMin() {
        return Math.min(mirrorAttackBoost, Math.min(mirrorStrengthBoost, mirrorDefenceBoost));
    }

    /** True when the super combat potion in inventory (divine XOR regular) says
     *  it's time to rebuff. Divine: 5-min timer expired. Regular: min boost < 10.
     *  Returns false when no super combat is in inventory (nothing to sip). */
    public boolean needsSuperCombatRebuff() {
        if (countDoses(DIVINE_SUPER_COMBAT_IDS) > 0) {
            return !isBuffedSuperCombat();
        }
        if (countDoses(SUPER_COMBAT_IDS) > 0) {
            return superCombatBoostMin() < REGULAR_SUPER_COMBAT_REBUFF_BOOST;
        }
        return false;
    }

    /** Drink the lowest-dose super combat potion in inv. Divine takes priority if
     *  both are present (edge case; user promised only one type at a time). Clears
     *  nothing on its own — the chat handler dispatches onSippedSuperCombat(). */
    public void sipSuperCombat() {
        if (countDoses(DIVINE_SUPER_COMBAT_IDS) > 0) {
            sipLowestDose(DIVINE_SUPER_COMBAT_IDS, "Drink");
        } else if (countDoses(SUPER_COMBAT_IDS) > 0) {
            sipLowestDose(SUPER_COMBAT_IDS, "Drink");
        }
    }

    /** True while super antifire is active. */
    public boolean isBuffedSuperAntifire() { return client.getTickCount() < superAntifireExpiryTick; }
    /** True while regular antifire is active. */
    public boolean isBuffedAntifire()      { return client.getTickCount() < antifireExpiryTick; }
    /** True while anti-venom+ immunity is active. */
    public boolean isBuffedAntivenom()     { return client.getTickCount() < antivenomExpiryTick; }
    /** True while super combat is active. */
    public boolean isBuffedSuperCombat()   { return client.getTickCount() < superCombatExpiryTick; }

    /** Ticks remaining on super antifire (0 if expired). */
    public int superAntifireTicksLeft() { return Math.max(0, superAntifireExpiryTick - client.getTickCount()); }
    public int antifireTicksLeft()      { return Math.max(0, antifireExpiryTick      - client.getTickCount()); }
    public int antivenomTicksLeft()     { return Math.max(0, antivenomExpiryTick     - client.getTickCount()); }
    public int superCombatTicksLeft()   { return Math.max(0, superCombatExpiryTick   - client.getTickCount()); }

    // ---- Sync helpers called from Plugin's VarbitChanged / GameTick handlers ----

    /**
     * Called by Plugin when it observes the SUPER_ANTIFIRE varbit is nonzero. Arms the local
     * timer if it wasn't already tracking (handles the "plugin started mid-buff" case).
     */
    public void syncSuperAntifireActive() {
        int fresh = client.getTickCount() + SUPER_ANTIFIRE_DURATION_TICKS;
        if (superAntifireExpiryTick < fresh - 50) superAntifireExpiryTick = fresh;
    }
    public void syncSuperAntifireExpired() { superAntifireExpiryTick = 0; }

    public void syncAntifireActive() {
        int fresh = client.getTickCount() + ANTIFIRE_DURATION_TICKS;
        if (antifireExpiryTick < fresh - 50) antifireExpiryTick = fresh;
    }
    public void syncAntifireExpired() { antifireExpiryTick = 0; }

    public void syncAntivenomActive() {
        int fresh = client.getTickCount() + ANTIVENOM_DURATION_TICKS;
        if (antivenomExpiryTick < fresh - 50) antivenomExpiryTick = fresh;
    }
    public void syncAntivenomExpired() { antivenomExpiryTick = 0; }

    /**
     * Called every game tick — checks whether Att/Str/Def are all boosted above real level,
     * arming the super-combat timer if so and clearing it once boosts wear off.
     */
    public void syncSuperCombatFromSkills() {
        boolean actuallyBoosted =
               client.getBoostedSkillLevel(Skill.ATTACK)   > client.getRealSkillLevel(Skill.ATTACK)
            && client.getBoostedSkillLevel(Skill.STRENGTH) > client.getRealSkillLevel(Skill.STRENGTH)
            && client.getBoostedSkillLevel(Skill.DEFENCE)  > client.getRealSkillLevel(Skill.DEFENCE);
        if (actuallyBoosted && superCombatExpiryTick < client.getTickCount()) {
            superCombatExpiryTick = client.getTickCount() + DIVINE_SUPER_COMBAT_DURATION_TICKS;
        } else if (!actuallyBoosted && superCombatExpiryTick != 0) {
            superCombatExpiryTick = 0;
        }
    }


    // ================= Woox walk / prayer-tracking public API =================

    /** Called by Plugin when Vorkath's death animation (7949) is observed. */
    public void onVorkathDeath() {
        vorkathAlive = false;
        magePrayOn = false;
        pietyOn = false;
        // Clear any queued post-poke action from the previous kill — its wake-up
        // window has already passed (or wasn't going to fire); leaving it set
        // would make the next preFightTopOff loop into the poke-retry guard
        // instead of sipping fresh doses.
        pendingPostPokeAction = null;
        // Walk to the center loot tile during Vorkath's death animation. The
        // drop-pile spawn tile is chosen from the player's position when the anim
        // finishes — arriving on LOOT_STACK_TILE means all drops pile on one tile,
        // eliminating the need for cross-tile pickup logic. First click fires now;
        // tickWalkToLootStack() re-clicks each onGameTick until the deadline so a
        // deflected path still converges before the pile spawns.
        // Anchor for the LOOT_SAFETY_TICKS deadline in isLootReady() —
        // covers the case where the kill-count chat event is never delivered.
        lastVorkathDeathTick = client.getTickCount();
        walkingToLootStackUntilTick = client.getTickCount() + DEATH_ANIM_WALK_TICKS;
        walkToLootStack();
        // No explicit loot trigger here — tickLootPass() opportunistically
        // re-plans on any safe tick that has visible ground items.
    }

    /** Called by Plugin when the NPC changes INTO Vorkath's fighting form (8061). */
    public void onVorkathSpawn() {
        vorkathAlive = true;
        magePrayOn = true;
        pietyOn = true;
        // Reset the previous kill's loot-ready signal. Done here (not in
        // onVorkathDeath) so the chat event for the just-finished kill,
        // which may arrive same-tick as the death anim, is never clobbered.
        killCountSeenThisKill = false;
        killCountSeenTick = -1;
    }

    /** Called by Plugin.onChatMessage when "Your Vorkath kill count is:" fires.
     *  The server prints that line the same tick the drop lands, so this is our
     *  "loot pile is on the ground" signal — both startEndgameLootPass and
     *  the trip-end TP gate wait on it. */
    public void onKillCountMessage() {
        killCountSeenThisKill = true;
        killCountSeenTick = client.getTickCount();
    }

    /** True once the loot pile is confirmed on the ground for the current
     *  kill: kill-count chat message seen, or LOOT_SAFETY_TICKS past death
     *  anim (safety valve for a dropped chat event). Gates startEndgameLootPass
     *  and the trip-end TP so we never plan or leave before the drop lands.
     *
     *  Special case: lastVorkathDeathTick <= 0 means no death has been
     *  recorded (fresh plugin start, or player entered the arena without
     *  ever killing Vorkath — e.g. climbed over ice chunks with low
     *  supplies). Nothing to wait for; return true so site A doesn't
     *  deadlock on the loot-ready gate. */
    public boolean isLootReady() {
        if (lastVorkathDeathTick <= 0) return true;
        int now = client.getTickCount();
        // Safety valve — always fires eventually even if chat is dropped or the
        // ItemSpawned events never come through.
        if (now >= lastVorkathDeathTick + LOOT_SAFETY_TICKS) return true;
        // Chat gate + grace ticks. Chat and ItemSpawned fire on the same server
        // tick but RuneLite doesn't guarantee client-thread delivery order — if
        // chat lands first, worker sees killCountSeen=true and, if leftover
        // loot was already drained, an empty currentGroundItems. TP would fire
        // before this kill's drops are added. TICKS_AFTER_KC_FOR_DROPS holds
        // "ready" back until ItemSpawned has had ~2 ticks to populate the list.
        return killCountSeenThisKill
            && killCountSeenTick > 0
            && now >= killCountSeenTick + TICKS_AFTER_KC_FOR_DROPS;
    }

    /**
     * Reconcile actual prayer state with the tracked "expected" state. Vorkath-dead
     * force-clears both expected flags first so a lingering projectile trigger cannot
     * re-enable prayers after death. Fires at most one toggle per call (gated by the
     * pray cooldown) except for the quick-prayers fast path.
     */
    public void reconcilePrayers() {
        // Bail when the plugin isn't running — this method is what actually
        // clicks the prayer widget via pray() / activateQuickPrayers, and
        // Plugin.onClientTick calls it every client tick. Without this gate
        // we'd toggle prayers on and off while the user has us stopped.
        if (!isRunning) return;

        // Force prayers off if Vorkath is dead OR we're outside the fight region
        // (e.g., teleported to POH). vorkathAlive may lag until the death anim fires,
        // so the region check is the real-time signal.
        if (!vorkathAlive || !isInVorkathRegion()) {
            magePrayOn = false;
            pietyOn = false;
        }

        // Force prayers off across the entire 395 (spider-spec) / zombified-spawn
        // window. Vorkath doesn't attack us during this phase — the spawn does,
        // and its poison damage bypasses prot-from-magic — so piety + mage are
        // pure prayer drain. Use inSpawnPhase (Plugin sets it on the 395 rising
        // edge and clears it on spawn death) rather than
        // `mirror395InFlight || zombifiedSpawnAlive`: the OR of the two has a
        // few-tick gap between the 395 projectile clearing and the zombified
        // spawn NPC actually appearing, during which prayers were flicking
        // back on for a tick and then off again. inSpawnPhase spans the entire
        // window. The restore branch a few lines down flips prayers back on
        // for the next Vorkath auto-attack once inSpawnPhase clears.
        boolean spawnPhase = inSpawnPhase;
        if (spawnPhase) {
            magePrayOn = false;
            pietyOn = false;
        }

        // Prayer conservation: at or below NORMAL_PRAYER_THRESHOLD, force piety
        // OFF only when we can't sip our way out (no prayer doses in inv). With
        // doses available, one sip restores ~28 pray — far more than piety drains
        // between reconciles — so flipping off at 50 caused flapping (piety-off
        // at 50 → sip → prayer 78 → piety-on → drain to 50 → off again, every
        // ~5 ticks). The conservation logic was written for the truly-out case
        // where no sip is possible; there it correctly extends survivable time
        // ~4x by keeping only mage prot up. With doses, no extension needed.
        //
        // Symmetric restore: once a prayer sip (or dose refill) puts us into
        // "safe" territory AND we're mid-fight, flip pietyOn back to true.
        // Guarded on vorkathAlive + region so a dead-Vorkath / out-of-region
        // sip doesn't re-enable piety after the force-off at the top of this
        // method.
        boolean noDoses = countDoses(PRAYER_POTION_IDS) == 0;
        if (mirrorCurrentPrayer <= NORMAL_PRAYER_THRESHOLD && noDoses) {
            pietyOn = false;
        } else if (vorkathAlive && isInVorkathRegion() && !spawnPhase) {
            // Do not restore piety during 395/spawn — the force-off above just
            // set it, and this branch would immediately override it.
            pietyOn = true;
        }

        if ((client.getTickCount() - lastPrayGameTick) < PRAY_COOLDOWN_TICKS) return;

        boolean mageActual = client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC);
        boolean pietyActual = client.isPrayerActive(Prayer.PIETY);

        // Prayer-points gate: at 0 pray points the game refuses activation, so
        // any pray("mage") / pray("piety") / activateQuickPrayers() click is
        // wasted. Skip ACTIVATION attempts when we're at 0; deactivation still
        // fires normally (turning off never needs points). Desired flags stay
        // as-is, so as soon as a prayer sip restores points the toggle succeeds
        // naturally on the next reconcile.
        boolean noPrayerPoints = mirrorCurrentPrayer <= 0;

        // Fast path: both need turning ON and both currently OFF — quick prayers.
        if (magePrayOn && pietyOn && !mageActual && !pietyActual) {
            if (noPrayerPoints) return;   // can't activate — skip
            lastPrayGameTick = client.getTickCount();
            activateQuickPrayers();
            return;
        }

        // Individual toggles (one per tick due to cooldown). Turning ON with
        // 0 prayer is blocked; turning OFF still fires (actual=true, desired=false).
        if (mageActual != magePrayOn) {
            boolean turningOn = magePrayOn && !mageActual;
            if (turningOn && noPrayerPoints) {
                // fall through to piety check — this branch can't act
            } else {
                lastPrayGameTick = client.getTickCount();
                pray("mage");
                return;
            }
        }
        if (pietyActual != pietyOn) {
            boolean turningOn = pietyOn && !pietyActual;
            if (turningOn && noPrayerPoints) {
                return;   // nothing else to do this tick
            }
            lastPrayGameTick = client.getTickCount();
            pray("piety");
            return;
        }
    }

    /**
     * Woox walk state machine — call once per game tick. Gated on vorkathAcidAnim
     * (starts / stops the walk) and currentWalkX (safe column). Fires a click-with-retry
     * appropriate for the player's current row.
     */
    public void wooxWalkStep() {
        if (!doVorkath) return;
        if (!isInVorkathRegion()) return;   // real-time region check — doVorkath may lag by a tick
        if (!vorkathAcidAnim) {
            // Acid just ended — if the player got stranded on a non-4061 row (mid-walk
            // at 4060 or south at 4059), click Vorkath to walk back to the attack row.
            // Edge-triggered on wasAcidLastTick so we don't spam clicks every tick the
            // player is south for unrelated reasons.
            if (wasAcidLastTick) {
                Player p = client.getLocalPlayer();
                if (p != null) {
                    WorldPoint pos = toTemplate(p.getWorldLocation());
                    if (pos != null && pos.getY() != 4061) {
                        // Walk straight north on the player's current column. Works whether
                        // Vorkath is alive (we'll auto-attack from 4061) or dead (we're back
                        // in position for the next kill without walking from the south row).
                        clickTileWithRetry(new WorldPoint(pos.getX(), 4061, 0));
                    }
                }
            }
            wasAcidLastTick = false;
            // Acid ended — restore run if we toggled it off during the walk.
            if (runToggledOffForAcid) {
                if (!mirrorRunEnabled) activateRun();
                runToggledOffForAcid = false;
            }
            // Also clear the horizontal fallback pair so a stale one can't drive
            // the next acid cycle before the plugin has a chance to recompute.
            horizontalWooxA = null;
            horizontalWooxB = null;
            return;
        }
        wasAcidLastTick = true;

        // Horizontal fallback branch — takes precedence over currentWalkX because
        // it's only set when currentWalkX was -1. Oscillates east↔west along a
        // single row. On-endpoint: click the OTHER endpoint. Off-row / mid-row:
        // sidestep to the nearer endpoint first.
        if (horizontalWooxA != null && horizontalWooxB != null) {
            Player pH = client.getLocalPlayer();
            if (pH == null) return;
            WorldPoint playerH = toTemplate(pH.getWorldLocation());
            if (playerH == null) return;

            // Toggle run off once per acid cycle so path stops at the endpoint.
            if (!runToggledOffForAcid && mirrorRunEnabled) {
                activateRun();
                runToggledOffForAcid = true;
            }

            int rowY = horizontalWooxA.getY();
            int wx = horizontalWooxA.getX();
            int ex = horizontalWooxB.getX();

            // If a walk is already in flight, let it finish. Re-clicking the
            // "closer" endpoint while in-transit cancels the current walk and
            // ping-pongs us between two adjacent tiles. (This is the bug the
            // vertical woox implicitly avoids by having no branch for transit
            // rows.) Only decide when we're standing still.
            net.runelite.api.coords.LocalPoint destLp = client.getLocalDestinationLocation();
            if (destLp != null) return;

            if (playerH.getY() != rowY) {
                // Off the fallback row and standing still — bootstrap to closer endpoint.
                int dW = Math.abs(playerH.getX() - wx) + Math.abs(playerH.getY() - rowY);
                int dE = Math.abs(playerH.getX() - ex) + Math.abs(playerH.getY() - rowY);
                clickTileWithRetry((dW <= dE) ? horizontalWooxA : horizontalWooxB);
                return;
            }

            // On-row AND standing still. If we're at an endpoint, flip to the
            // other. If mid-row (very rare — only happens if the walk was
            // interrupted by something else clicking our destination away),
            // walk to the FARTHER endpoint so we cover ground before the next
            // acid-tick lands where we're standing.
            if (playerH.getX() == wx) {
                clickTileWithRetry(horizontalWooxB);
            } else if (playerH.getX() == ex) {
                clickTileWithRetry(horizontalWooxA);
            } else {
                int dW = Math.abs(playerH.getX() - wx);
                int dE = Math.abs(playerH.getX() - ex);
                clickTileWithRetry((dW >= dE) ? horizontalWooxA : horizontalWooxB);
            }
            return;
        }

        if (currentWalkX < 0) return;

        Player p = client.getLocalPlayer();
        if (p == null) return;

        WorldPoint player = toTemplate(p.getWorldLocation());
        if (player == null) return;

        // Off-column: sidestep back onto the safe column first (default to the north row).
        if (player.getX() != currentWalkX) {
            clickTileWithRetry(new WorldPoint(currentWalkX, 4061, 0));
            return;
        }

        // ── On-column from here down. Toggle run off first (once per acid) and
        // then FALL THROUGH to fire the movement click on the same tick. Previously
        // the toggle branch returned early and the actual walk-south fired one tick
        // later — a visible pre-woox pause. Both a toggle-run click and a walk-tile
        // click can queue in the same tick, so combining them is safe.
        if (!runToggledOffForAcid && mirrorRunEnabled) {
            activateRun();
            runToggledOffForAcid = true;
        }

        // On the north row (4061): click well south of the safe row to walk south.
        // Click destination 4057 is south of both lance-safe (4059) and fang-safe
        // (4058); pathing stops at the safe tile on its own — 4057 just guarantees
        // the click is past whichever weapon we have equipped.
        if (player.getY() == 4061) {
            clickTileWithRetry(new WorldPoint(currentWalkX, 4057, 0));
//            wooxWalkingAway = true;   // heading south, away from Vorkath — queue may interleave
        }

        // On the south safe row (lance=4059, fang=4058): click Vorkath to walk back north.
        else if (player.getY() <= southSafeRowY()) {
            NPC vorkath = findNpc(8061);
            if (vorkath != null) {
//                clickVorkathWithRetry(vorkath);
                clickOnVorkath();
            }
//            wooxWalkingAway = false;  // heading north, back toward Vorkath — no queue interleave
        }
    }


    // ================= Attack lock / direction accessors =================

    public int getLastAttackTick() { return lastAttackTick; }
    /** Plugin's AnimationChanged handler for the local player calls this on any attack anim. */
    public void setLastAttackTick(int t) { this.lastAttackTick = t; }

    public boolean isWooxWalkingAway() { return wooxWalkingAway; }
    public void setWooxWalkingAway(boolean v) { this.wooxWalkingAway = v; }

    public int countDoses(List<Integer> idsByDose) {
        int total = 0;
        for (Item item : currentInventory) {
            if (item == null) continue;
            int idx = idsByDose.indexOf(item.getId());
            if (idx >= 0) total += (idx + 1);
        }
        return total;
    }


    private boolean isInVorkathRegion() {
        // Vorkath's fight is instanced; we're only truly "in the fight" when the client
        // is in an instance AND that instance was built from the Ungael template chunk.
        // Objects like vorkathIceChunksInside don't despawn reliably, so this region-based
        // check is the only trustworthy "am I still here" signal.
        // GameState gate — getMapRegions() is briefly stale/empty during LOADING
        // transitions; treat mid-load as "not in region" to be safe.
        if (client.getGameState() != GameState.LOGGED_IN) return false;
        if (!client.isInInstancedRegion()) return false;
        int[] regions = client.getMapRegions();
        if (regions == null) return false;
        for (int r : regions) {
            if (r == UNGEAL_REGION) return true;
        }
        return false;
    }

    /** Same shape as isInVorkathRegion but for POH. isInsideInstance() alone
     *  can't distinguish POH from Vorkath, and restorationPoolPOH is a stale-
     *  cache risk — this region-based check is the trustworthy "in POH" signal. */
    private boolean isInPOHRegion() {
        if (client.getGameState() != GameState.LOGGED_IN) return false;
        if (!client.isInInstancedRegion()) return false;
        int[] regions = client.getMapRegions();
        if (regions == null) return false;
        for (int r : regions) {
            if (r == POH_REGION) return true;
        }
        return false;
    }

    /**
     * Hard reset — called when the player dies. Kills every flag, timer, latch, and
     * pending action so the plugin idles until the user manually restarts.
     */
    public void handleDeath() {
        // Halt the top-level loop
        doVorkath = false;
        isRunning = false;
        doWooxWalk = false;
        clearLootPass();

        // Vorkath fight state
        vorkathAlive = false;
        vorkathAcidAnim = false;
        currentWalkX = -1;
        wooxWalkingAway = true;

        // Prayer intent
        magePrayOn = false;
        pietyOn = false;

        // Buff timers — respawn wipes buffs, so zero everything
        superAntifireExpiryTick = 0;
        antifireExpiryTick      = 0;
        antivenomExpiryTick     = 0;
        superCombatExpiryTick   = 0;
        antifireExpireMessage   = false;
        antivenomExpireMessage  = false;

        // Zombified spawn state
        zombifiedSpawnAlive = false;

        // Projectile latches
        dodge1481Fired    = false;
        has1481LastTick   = false;
        dodged1481ThisWave = false;

        // Pre-fight state
        pendingPostPokeAction = null;
        lastEatTick = -100;

        // Travel / chat flags
        boardedBoat = false;

        System.out.println("[DEATH] all state cleared, plugin halted");
    }

    public boolean hasEnoughSupplies() {
        if (countDoses(PRAYER_POTION_IDS) < MIN_PRAYER_DOSES) return false;
        // Effective prayer floor: currentPrayer + doses × per-pot restore. One
        // dose restores 7 + maxPrayer/4 (game formula). At 99 prayer that's ~31
        // points per dose. Fails if we don't have enough total prayer runway to
        // start a fresh kill even after sipping. Worker-thread safe: reads
        // prayer mirrors.
        int prayerRestore = 7 + (mirrorMaxPrayer / 4);
        int effectivePrayer = mirrorCurrentPrayer
                            + countDoses(PRAYER_POTION_IDS) * prayerRestore;
        if (effectivePrayer < MIN_PRAYER_POINTS_START) return false;
        // 2 sharks is the true minimum — a gamble to eke out one more kill. Also
        // keeps hasEnoughSupplies honest regardless of the buff-time shortcut
        // path below (which otherwise doesn't check shark count).
        if (getItemCount(currentInventory, ItemID.SHARK) < MIN_SHARKS) return false;

        // Antifire is checked as the UNION of both extended pots: super antifire is
        // a strict upgrade of regular, so a fang loadout with only super pots is
        // still fine, and a lance loadout with a mix counts every dose. Buff-time
        // takes the longer of the two active buffs for the same reason.
        int antifireBuffTicks = Math.max(superAntifireTicksLeft(), antifireTicksLeft());
        if (antifireBuffTicks >= MIN_BUFF_TIME_TICKS
            && antivenomTicksLeft() >= MIN_BUFF_TIME_TICKS) {
            return true;
        }

        int antifireDoses = countDoses(EXTENDED_SUPER_ANTIFIRE_IDS)
                          + countDoses(EXTENDED_ANTIFIRE_IDS);
        return antifireDoses                                           >= MIN_SUPER_ANTIFIRE_DOSES
            && countDoses(EXTENDED_ANTIVENOM_IDS)                      >= MIN_ANTIVENOM_DOSES
            && getItemCount(currentInventory, ItemID.SHARK)            >= MIN_SHARKS
            && getItemCount(currentInventory, ItemID.COOKED_KARAMBWAN) >= MIN_KARAMBWAN;
    }

    /**
     * Optimistic mid-fight survivability check — looser than hasEnoughSupplies().
     * hasEnoughSupplies is the BANK-ENTRY decision (do we have full MIN_*
     * thresholds to start a fresh fight?). canContinueFight is the IN-FIGHT
     * decision: keep pushing for the kill even if hasEnoughSupplies dropped.
     *
     * Hard gates (any failing → TP out):
     *   - HP above HP_THRESHOLD (sharks/karambwan are optional; if HP drops
     *     because we can't heal, this gate catches it).
     *   - Antifire: at least MIN_BUFF_TIME_TICKS of active buff (~2 min) OR at
     *     least one extended-antifire dose (either kind). Missing this is fatal —
     *     Vorkath's dragonbreath one-shots without antifire.
     *   - Antivenom: at least MIN_BUFF_TIME_TICKS of active buff OR one dose.
     *     Poison stack from a single hit will drain us before we can react.
     *   - Prayer: currentPrayer > 0 OR at least one prayer-pot dose. If we bottom
     *     out on points with no way to restore, we lose protection prayers and
     *     take full damage from every hit.
     * Missing sharks/karambwan alone is uncomfortable but not fatal — no gate.
     */
    public boolean canContinueFight() {
        if (getCurrentHP() <= HP_THRESHOLD) return false;

        int antifireBuffTicks = Math.max(superAntifireTicksLeft(), antifireTicksLeft());
        boolean antifireOK = antifireBuffTicks >= MIN_BUFF_TIME_TICKS
            || countDoses(EXTENDED_SUPER_ANTIFIRE_IDS) + countDoses(EXTENDED_ANTIFIRE_IDS) > 0;
        if (!antifireOK) return false;

        boolean antivenomOK = antivenomTicksLeft() >= MIN_BUFF_TIME_TICKS
            || countDoses(EXTENDED_ANTIVENOM_IDS) > 0;
        if (!antivenomOK) return false;

        boolean prayerOK = getCurrentPrayer() > 0
            || countDoses(PRAYER_POTION_IDS) > 0;
        if (!prayerOK) return false;

        return true;
    }

    public void degradePrayerIfNeeded() {
        if (countDoses(PRAYER_POTION_IDS) == 0 && getCurrentPrayer() < 20) {
            setPietyOn(false);
        }
    }

    /** Current HP (boosted skill level of Hitpoints). */
    public int getCurrentHP() { return client.getBoostedSkillLevel(Skill.HITPOINTS); }
    /** Current prayer points (boosted skill level of Prayer). */
    public int getCurrentPrayer() { return client.getBoostedSkillLevel(Skill.PRAYER); }

    public void eatShark() {
        eatFood(ItemID.SHARK, "Eat");
    }

    public void comboEatSharkAndKarambwan() {
        eatFood(ItemID.SHARK, "Eat");
        eatFood(ItemID.COOKED_KARAMBWAN, "Eat");
    }

    public void comboEatSharkAndSip(String key) {
        int tick = client.getTickCount();
        switch (key) {
            case "antivenom":
                eatFood(ItemID.SHARK, "Eat");
                sipLowestDose(EXTENDED_ANTIVENOM_IDS, "Drink");
                lastAntivenomSipTick = tick;
                break;
            case "superantifire":
            case "antifire":
                eatFood(ItemID.SHARK, "Eat");
                sipLowestDose(activeAntifireIds(), "Drink");
                lastAntifireSipTick = tick;
                break;
            case "prayer":
                eatFood(ItemID.SHARK, "Eat");
                sipLowestDose(PRAYER_POTION_IDS, "Drink");
                lastPrayerSipTick = tick;
                break;
        }
    }

    /**
     * Triple combo: shark + pot + karambwan in that order — all three actions
     * resolve on the same server tick. Shark starts the 3-tick eat cooldown,
     * the potion is a drink and independent, karambwan uses the combo-food
     * bypass to eat under the shark's cooldown. Net heal: SHARK_HEAL_HP +
     * KARAMBWAN_HEAL_HP (~38); the caller MUST have already checked HP room
     * for both foods (see sipWithSharkAndKarambwanIfRoom).
     */
    public void comboEatSharkSipAndKarambwan(String key) {
        int tick = client.getTickCount();

        switch (key) {
            case "antivenom":
                eatFood(ItemID.SHARK, "Eat");
                sipLowestDose(EXTENDED_ANTIVENOM_IDS, "Drink");
                eatFood(ItemID.COOKED_KARAMBWAN, "Eat");
                lastAntivenomSipTick = tick;
                break;
            case "superantifire":
            case "antifire":
                eatFood(ItemID.SHARK, "Eat");
                sipLowestDose(activeAntifireIds(), "Drink");
                eatFood(ItemID.COOKED_KARAMBWAN, "Eat");
                lastAntifireSipTick = tick;
                break;
            case "prayer":
                eatFood(ItemID.SHARK, "Eat");
                sipLowestDose(PRAYER_POTION_IDS, "Drink");
                eatFood(ItemID.COOKED_KARAMBWAN, "Eat");
                lastPrayerSipTick = tick;
                break;
        }
    }

    // ============================================================================
    // Unified consume() — one entry point for all shark/pot/karambwan action packets.
    // ============================================================================

    public enum ConsumeKind { SHARK, KARA, PRAYER, ANTIFIRE, ANTIVENOM, SUPER_COMBAT }

    public void consume(EnumSet<ConsumeKind> required, EnumSet<ConsumeKind> opportunistic) {
        if (required == null)      required      = EnumSet.noneOf(ConsumeKind.class);
        if (opportunistic == null) opportunistic = EnumSet.noneOf(ConsumeKind.class);

        // Acid-phase gate. All eat/sip is blocked during woox walk regardless of
        // caller intent — the walk owns movement and an inv action mid-walk risks
        // eating an acid tile.
        if (vorkathAcidAnim) return;

        // Worker-thread safe: mirrors are updated from client thread every ~16ms.
        int hp     = mirrorCurrentHP;
        int maxHp  = mirrorMaxHP;
        int tick   = client.getTickCount();
        boolean foodCd = onFoodCooldown();

        // ---- Foods: pick REQUIRED first (unconditional), then OPPORTUNISTIC
        //      subject to overheal + cooldown/inventory reality.
        boolean fireShark = false, fireKara = false;
        int healSum = 0;

        if (required.contains(ConsumeKind.SHARK)
                && getItemCount(currentInventory, ItemID.SHARK) > 0
                && !foodCd) {
            fireShark = true; healSum += SHARK_HEAL_HP;
        }
        // KARA in `required` fires even on food cooldown — karambwan bypasses
        // the eat cooldown per OSRS mechanics.
        if (required.contains(ConsumeKind.KARA)
                && getItemCount(currentInventory, ItemID.COOKED_KARAMBWAN) > 0) {
            fireKara = true; healSum += KARAMBWAN_HEAL_HP;
        }

        if (!fireShark && opportunistic.contains(ConsumeKind.SHARK)
                && !foodCd
                && getItemCount(currentInventory, ItemID.SHARK) > 0
                && hp + healSum + SHARK_HEAL_HP <= maxHp) {
            fireShark = true; healSum += SHARK_HEAL_HP;
        }
        if (!fireKara && opportunistic.contains(ConsumeKind.KARA)
                && getItemCount(currentInventory, ItemID.COOKED_KARAMBWAN) > 0
                && hp + healSum + KARAMBWAN_HEAL_HP <= maxHp) {
            fireKara = true;
        }

        // ---- Sips: required = fire if in inv; opportunistic = fire if in inv
        //      AND the "low" predicate says the sip is useful (timer at 0 or
        //      <= ~5 sec of runway left).
        boolean firePrayer    = required.contains(ConsumeKind.PRAYER)
                              && countDoses(PRAYER_POTION_IDS) > 0;
        boolean fireAntifire  = required.contains(ConsumeKind.ANTIFIRE)
                              && countDoses(activeAntifireIds()) > 0;
        boolean fireAntivenom = required.contains(ConsumeKind.ANTIVENOM)
                              && countDoses(EXTENDED_ANTIVENOM_IDS) > 0;
        boolean fireSuperCombat = required.contains(ConsumeKind.SUPER_COMBAT)
                                && (countDoses(DIVINE_SUPER_COMBAT_IDS) > 0
                                    || countDoses(SUPER_COMBAT_IDS) > 0);

        if (!firePrayer && opportunistic.contains(ConsumeKind.PRAYER)
                && countDoses(PRAYER_POTION_IDS) > 0
                && isPrayerSipOpportunistic()) firePrayer = true;
        if (!fireAntifire && opportunistic.contains(ConsumeKind.ANTIFIRE)
                && countDoses(activeAntifireIds()) > 0
                && isAntifireSipOpportunistic()) fireAntifire = true;
        if (!fireAntivenom && opportunistic.contains(ConsumeKind.ANTIVENOM)
                && countDoses(EXTENDED_ANTIVENOM_IDS) > 0
                && isAntivenomSipOpportunistic()) fireAntivenom = true;
        if (!fireSuperCombat && opportunistic.contains(ConsumeKind.SUPER_COMBAT)
                && needsSuperCombatRebuff()) fireSuperCombat = true;

        if (!fireShark && !fireKara && !firePrayer && !fireAntifire && !fireAntivenom && !fireSuperCombat) {
            return;   // nothing to do this tick
        }

        // ---- OSRS single-drink-per-tick guard.
        // A menuAction(Drink) sent while the previous Drink is still animating
        // CANCELS the pending one — the server sees the new click and drops the
        // old. So firing multiple drinks in one consume() call causes all but
        // one to be silently dropped. Fire the highest-priority drink now, drop
        // the rest; the caller's loop (preFightTopOff / evaluateActionsThisTick)
        // will re-enter next tick and fire the next drink after this one's
        // signal (varbit / chat latch) clears.
        // Shark + 1 drink + kara in the same tick is still safe — Eat and Drink
        // use separate server-side action slots, and kara bypasses the eat cd.
        // Priority: PRAYER > ANTIFIRE > ANTIVENOM > SUPER_COMBAT.
        int drinkCount = (firePrayer ? 1 : 0) + (fireAntifire ? 1 : 0)
                       + (fireAntivenom ? 1 : 0) + (fireSuperCombat ? 1 : 0);
        if (drinkCount > 1) {
            if (firePrayer) {
                fireAntifire = false; fireAntivenom = false; fireSuperCombat = false;
            } else if (fireAntifire) {
                fireAntivenom = false; fireSuperCombat = false;
            } else if (fireAntivenom) {
                fireSuperCombat = false;
            }
        }

        // ---- Fire in the fixed order. Same-tick resolution: each eatFood /
        //      sipLowestDose call queues an inv action on the client thread; the
        //      server processes the whole packet on the next server tick.
        if (fireShark)       eatFood(ItemID.SHARK, "Eat");
        if (firePrayer)     { sipLowestDose(PRAYER_POTION_IDS, "Drink");    lastPrayerSipTick    = tick; }
        if (fireAntifire)   { sipLowestDose(activeAntifireIds(), "Drink");  lastAntifireSipTick  = tick; }
        if (fireAntivenom)  { sipLowestDose(EXTENDED_ANTIVENOM_IDS, "Drink"); lastAntivenomSipTick = tick; }
        if (fireSuperCombat) sipSuperCombat();
        if (fireKara)        eatFood(ItemID.COOKED_KARAMBWAN, "Eat");
    }

    /** True when the prayer level has room for a full pot restore (no overrestore).
     *  Worker-thread safe — reads prayer mirrors. */
    private boolean isPrayerSipOpportunistic() {
        int pray    = mirrorCurrentPrayer;
        int maxPray = mirrorMaxPrayer;
        int restore = 7 + (maxPray / 4);
        return pray + restore <= maxPray;
    }

    /** Opportunistic antifire uses the same chat-message signal as the P2 required
     *  path — the "your antifire has expired" flag. No pre-emptive sipping, no
     *  varbit threshold: the flag is true only after antifire actually ended.
     *  Rides along on any other action tick while the flag is set. */
    private boolean isAntifireSipOpportunistic() {
        return antifireExpireMessage;
    }

    /** Opportunistic antivenom uses the same chat-message signal as the P2
     *  required path — the "your antivenom has expired" flag. No pre-emptive
     *  sipping, no varp threshold: the flag is true only after immunity actually
     *  ended. In consume() this means antivenom rides along on any other action
     *  tick when the message has fired but the tick hasn't routed through P2 yet. */
    private boolean isAntivenomSipOpportunistic() {
        return antivenomExpireMessage;
    }

    public void sipExtendedSuperAntifire() {
        sipLowestDose(EXTENDED_SUPER_ANTIFIRE_IDS, "Drink");
        lastAntifireSipTick = client.getTickCount();
    }

    public void sipExtendedAntifire() {
        sipLowestDose(EXTENDED_ANTIFIRE_IDS, "Drink");
        lastAntifireSipTick = client.getTickCount();
    }

    // ── Weapon-aware antifire helpers ─────────────────────────────────────────
    // Lance needs full immunity (extended SUPER antifire — negates dragon breath
    // entirely). Fang runs with prot-mage up, so extended (regular) antifire is
    // enough. The trio below routes every supply/varbit/sip decision through the
    // currently-equipped main weapon (live, via getMainWeapon()).

    /** Extended super antifire (LANCE) or extended antifire (FANG) dose list. */
    public List<Integer> activeAntifireIds() {
        return (getMainWeapon() == MainWeapon.LANCE)
            ? EXTENDED_SUPER_ANTIFIRE_IDS
            : EXTENDED_ANTIFIRE_IDS;
    }

    /** Latest mirror of the varbit that matches the active antifire (0 = expired). */
    public int activeAntifireVarbit() {
        return (getMainWeapon() == MainWeapon.LANCE)
            ? mirrorSuperAntifireVarbit
            : mirrorAntifireVarbit;
    }

    /** Sip the lowest-dose active antifire pot (super or regular per weapon). */
    public void sipActiveAntifire() {
        if (getMainWeapon() == MainWeapon.LANCE) sipExtendedSuperAntifire();
        else                                     sipExtendedAntifire();
    }

    public void sipExtendedAntivenom() {
        sipLowestDose(EXTENDED_ANTIVENOM_IDS, "Drink");
        lastAntivenomSipTick = client.getTickCount();
    }

    public void sipPrayerPotion() {
        sipLowestDose(PRAYER_POTION_IDS, "Drink");
        lastPrayerSipTick = client.getTickCount();
    }

    /**
     * Opportunistic top-off. Fires on the rising edge of Vorkath's acid animation
     * (7957) OR the zombified spawn's spider special (projectile 395). Eats a shark
     * only if the heal (SHARK_HEAL_HP) won't overheal past the real HP level, and
     * drinks a prayer potion only if the restore (7 + realPrayer/4) won't overrestore
     * past the real prayer level. Each check is independent — partial top-offs fire.
     */
    public void opportunisticTopOff() {
        if (!doVorkath || !isInVorkathRegion()) return;   // gate — no top-off outside the fight
        // Nothing REQUIRED — everything is opportunistic. consume() picks each
        // action only if it fits (HP room / inv / cooldown / sip timer low /
        // combat rebuff trigger).
        consume(EnumSet.noneOf(ConsumeKind.class),
                EnumSet.of(ConsumeKind.SHARK, ConsumeKind.KARA,
                           ConsumeKind.PRAYER, ConsumeKind.ANTIFIRE, ConsumeKind.ANTIVENOM,
                           ConsumeKind.SUPER_COMBAT));
    }

    /**
     * Dodge Vorkath's 1481 projectile.
     * 1) Read the 1481's landing tile from its target point (converted to template).
     * 2) Compute the 3x3 danger zone centered on that landing.
     * 3) If the player is ALREADY outside the danger zone AND either has no active walk
     *    destination or is walking to a tile also outside the danger zone, return false —
     *    no dodge needed. Caller can keep attacking / sipping.
     * 4) Otherwise score every tile in VORKATH_STANDING_AREA (row-4061 preference,
     *    distance from player), click the best safe tile, and return true.
     * Returning a boolean lets the caller decide whether to reAttack / skip P1..P3.
     */
    public boolean dodge1481() {
        // 1. Find the 1481 projectile and its landing tile.
        WorldPoint landing = null;
        for (Projectile proj : client.getProjectiles()) {
            if (proj.getId() == 1481) {
                landing = toTemplate(proj.getTargetPoint());
                break;
            }
        }
        if (landing == null) return false;

        // 2. Build 3x3 danger zone centered on landing tile.
        Set<WorldPoint> danger = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                danger.add(new WorldPoint(landing.getX() + dx, landing.getY() + dy, 0));
            }
        }

        Player me = client.getLocalPlayer();
        if (me == null) return false;
        WorldPoint playerPos = toTemplate(me.getWorldLocation());
        if (playerPos == null) return false;

        // 3. Already-safe short-circuit.
        //    - If walking somewhere: if that destination is safe, we're handled — do NOT
        //      re-click just because playerPos is still on the old (danger) tile mid-walk.
        //      Re-clicking here caused the visible jitter on 1481.
        //    - If not walking (destLp == null): decide based on current tile only.
        LocalPoint destLp = client.getLocalDestinationLocation();
        if (destLp != null) {
            WorldPoint destWp = WorldPoint.fromLocalInstance(client, destLp);
            boolean destSafe = destWp == null || !danger.contains(destWp);
            if (destSafe) return false;   // already walking to safety, don't redirect
        } else {
            if (!danger.contains(playerPos)) return false;   // stationary and safe
        }

        // 4. Score every safe tile and pick the lowest:
        //      rowPenalty     (y == 4061 is the attack row)      weight 1000
        //      distFromPlayer (Manhattan to player)               weight 10
        //      distFromStack  (Manhattan to LOOT_STACK_TILE)      weight 1
        //    Any row-4061 tile beats any non-row-4061 tile; among row-4061 tiles
        //    we prefer the tile that requires the LEAST movement (minimum-move
        //    dodge — just step out of the 3x3 danger zone). distFromStack is a
        //    mild tie-break so when two tiles are equally close to the player we
        //    prefer the one closer to the center pile. Previous weights
        //    (distFromStack * 10, distFromPlayer * 1) had the pull inverted: a
        //    tile 3 closer to stack always beat a tile 5 closer to player, so
        //    the dodge over-moved toward center col instead of just stepping
        //    aside.
        int playerX = playerPos.getX();
        int playerY = playerPos.getY();
        int stackX  = LOOT_STACK_TILE.getX();
        int stackY  = LOOT_STACK_TILE.getY();

        WorldPoint best = null;
        int bestScore = Integer.MAX_VALUE;
        for (WorldPoint tile : VorkathAutoWorldPoints.VORKATH_STANDING_AREA) {
            if (danger.contains(tile)) continue;

            int rowPenalty     = (tile.getY() == 4061) ? 0 : 1;
            int distFromStack  = Math.abs(tile.getX() - stackX)
                               + Math.abs(tile.getY() - stackY);
            int distFromPlayer = Math.abs(tile.getX() - playerX)
                               + Math.abs(tile.getY() - playerY);
            int score = rowPenalty * 1000 + distFromPlayer * 10 + distFromStack;

            if (score < bestScore) {
                bestScore = score;
                best = tile;
            }
        }

        if (best == null) return false;   // fully boxed in — no safe tile in the standing area
        clickTileWithRetry(best);
        return true;
    }

    // ================= Priority-driven decision engine =================

    /**
     * Evaluate what to do this game tick and fire the highest-priority action.
     * Priority ladder:
     *   P0 — MOVE (1481 dodge; acid-start walk is owned by wooxWalkStep)
     *   P1 — Prayer critical (<= CRITICAL_PRAYER_THRESHOLD)
     *   P2 — HP + essential combo (HP < HP_THRESHOLD)
     *   P3 — Single-sip essentials (antifire > antivenom > prayer normal)
     * After firing, if attack lock was broken this tick AND we are NOT in acid walk,
     * re-attack Vorkath to re-establish the auto-attack loop. During acid, wooxWalkStep
     * owns the re-attack via the north-arrival Vorkath click.
     *
     * clicks fire first and this engine interleaves after them on walking-away ticks.
     */
    public void evaluateActionsThisTick() {
        // --- Acid interleave gate: only run queue on walking-away ticks during acid ---
        if (!doVorkath) return;
        if (!isInVorkathRegion()) return;   // real-time region check — doVorkath may lag by a tick

        if (vorkathAcidAnim) return;   // block all sips/eats during acid — too risky to interleave
        if (zombifiedSpawnAlive) return;   // hand control to the human to attack the spawn

        boolean lockBroken = false;

        // --- P0: MOVE — 1481 dodge (acid-start MOVE is handled by wooxWalkStep) ---
        boolean has1481 = false;
        for (Projectile proj : client.getProjectiles()) {
            if (proj.getId() == 1481) { has1481 = true; break; }
        }
        // 1481 mechanic (independent of woox walk): step aside, and once we've arrived
        // at the safe tile, re-attack Vorkath. Trigger is arrival, NOT projectile-leaves-flight.
        //
        // If we dodged on a prior tick and the walk has completed (destination cleared),
        // re-attack now regardless of whether 1481 is still in flight.
        if (dodged1481ThisWave && client.getLocalDestinationLocation() == null) {
            dodged1481ThisWave = false;
            reAttackIfBroken(true);
            return;   // give the re-attack its own tick before P1..P3 fire
        }

        if (has1481) {
            // dodge1481 returns true only when it actually fired a click. Do NOT call
            // reAttackIfBroken here — attacking would redirect the walk to Vorkath's tile
            // and cancel our escape. Re-attack fires above on the tick we arrive.
            boolean dodged = dodge1481();
            has1481LastTick = true;
            if (dodged) {
                dodged1481ThisWave = true;
                return;   // dodging is the whole tick — skip potions / eats
            }
            // else: already safe → fall through to P1..P3 (attack + sips continue)
        } else if (has1481LastTick) {
            has1481LastTick = false;
        }

        // --- Read state ---
        int hp   = getCurrentHP();
        int pray = getCurrentPrayer();

        boolean hpLow     = hp   <= HP_THRESHOLD;
        boolean prayerLow = pray <= NORMAL_PRAYER_THRESHOLD;

        // --- P1: CRITICAL HP → shark + karambwan combo (top survival priority) ---
        // ~38 HP restored — from 39 HP puts us at ~77, above HP_THRESHOLD.
        // Essential potions naturally drain from the queue on subsequent P3 ticks.
        // Buff urgency: fire on the chat expiry flag AND respect the sip cooldown.
        // The expire-flag latch stays true until the "You drink some of your ..."
        // chat msg arrives via onSippedAntifire / onSippedAntivenom — that's 1-2
        // ticks AFTER the sip click lands. Without the cooldown gate, this branch
        // fired again on the next gameTick (flag still latched), draining an
        // entire (4)-dose potion in ~3 back-to-back sips. Requiring
        // (tick - lastSipTick) >= 3 gives the confirmation chat time to fire and
        // clear the latch. If the sip actually missed and the flag stays true,
        // we retry after the 3-tick window — bounded retry, no runaway spam.
        int nowTick = client.getTickCount();
        boolean superAntifireNeeded = antifireExpireMessage
                                   && (nowTick - lastAntifireSipTick) >= 3;
        boolean antivenomNeeded    = antivenomExpireMessage
                                   && (nowTick - lastAntivenomSipTick) >= 3;

        if (hpLow) {
            // P1: need shark (critical HP). Pack in any expired/near-expired
            // essential sip AND karambwan opportunistically. consume() handles
            // overheal / cooldown / inv / vorkathAcidAnim internally.
            EnumSet<ConsumeKind> req = EnumSet.of(ConsumeKind.SHARK);
            EnumSet<ConsumeKind> opp = EnumSet.of(ConsumeKind.KARA,
                    ConsumeKind.PRAYER, ConsumeKind.ANTIFIRE, ConsumeKind.ANTIVENOM);
            if (superAntifireNeeded) req.add(ConsumeKind.ANTIFIRE);
            if (antivenomNeeded)     req.add(ConsumeKind.ANTIVENOM);
            if (prayerLow)           req.add(ConsumeKind.PRAYER);
            consume(req, opp);
            lockBroken = true;
        }
        // --- P2: essential sips, combo'd with a shark if HP has room ---
        // A sip already costs a tick; if we can also fit a shark (HP room, shark
        // in bag, food cooldown clear) take the free heal via comboEatSharkAndSip.
        // sipWithSharkIfRoom() falls back to a plain sip when the combo isn't safe.
        else if (superAntifireNeeded) {
            consume(EnumSet.of(ConsumeKind.ANTIFIRE),
                    EnumSet.of(ConsumeKind.SHARK, ConsumeKind.KARA,
                               ConsumeKind.PRAYER, ConsumeKind.ANTIVENOM));
            lockBroken = true;
        } else if (antivenomNeeded) {
            consume(EnumSet.of(ConsumeKind.ANTIVENOM),
                    EnumSet.of(ConsumeKind.SHARK, ConsumeKind.KARA,
                               ConsumeKind.PRAYER, ConsumeKind.ANTIFIRE));
            lockBroken = true;
        } else if (prayerLow) {
            consume(EnumSet.of(ConsumeKind.PRAYER),
                    EnumSet.of(ConsumeKind.SHARK, ConsumeKind.KARA,
                               ConsumeKind.ANTIFIRE, ConsumeKind.ANTIVENOM));
            lockBroken = true;
        }
        // --- P3: super combat rebuff (mid-fight, standalone) ---
        // Fires only when P1/P2 didn't fire, so it doesn't double-cost a tick.
        // needsSuperCombatRebuff() dispatches: divine → 5-min timer expired,
        // regular → boost min < 10. Rides shark/kara opportunistically if HP
        // has room.
        else if (needsSuperCombatRebuff()) {
            consume(EnumSet.of(ConsumeKind.SUPER_COMBAT),
                    EnumSet.of(ConsumeKind.SHARK, ConsumeKind.KARA));
            lockBroken = true;
        }

        reAttackIfBroken(lockBroken);
    }

    /**
     * If any action this tick broke the attack lock AND we're NOT in the acid walk
     * (where wooxWalkStep owns the re-attack), re-issue attackNpc(vorkath) to restart
     * the 5-tick auto-attack cycle.
     */
    private void reAttackIfBroken(boolean lockBroken) {
        if (!lockBroken) return;
        if (vorkathAcidAnim) return;   // acid walker owns re-attack
        if (zombifiedSpawnAlive) return;   // spawn phase — human is fighting the spawn
        if (mirror395InFlight) return;     // spawn spider spec airborne — wait it out

        // force=true — a dodge broke the auto-attack lock, we know we need to
        // re-attack this tick. Without force, the 2-tick clickOnVorkath throttle
        // silently swallowed the re-attack when the dodge arrival happened within
        // 2 ticks of our previous auto-attack click, delaying the resume by a tick.
        clickOnVorkath(true);
    }


    /** Fire an attack (menuAction NPC_SECOND_OPTION "Attack") on the given NPC. */
    public void attackNpc(NPC npc, String target) {
        if (npc == null) return;
        clientThread.invoke(() -> {
            try {
                int idx = npc.getIndex();   // client-thread required
                client.menuAction(0, 0, MenuAction.NPC_SECOND_OPTION, idx, -1, "Attack", target);
            } catch (Throwable ex) {
                // NPC mid-teardown — drop
            }
        });
    }

    /**
     * Convert an instanced-region WorldPoint back to its template WorldPoint so it
     * lines up with hardcoded template coordinates (e.g. VORKATH_STANDING_AREA).
     */
    public WorldPoint toTemplate(WorldPoint instanced) {
        if (instanced == null) return null;
        LocalPoint lp = LocalPoint.fromWorld(client, instanced);
        if (lp == null) return null;
        return WorldPoint.fromLocalInstance(client, lp);
    }

    /**
     * Template WorldPoint -> LocalPoint via the current instance layout.
     */
    private LocalPoint fromTemplate(WorldPoint template) {
        if (template == null) return null;
        for (WorldPoint instanced : WorldPoint.toLocalInstance(client, template)) {
            LocalPoint lp = LocalPoint.fromWorld(client, instanced);
            if (lp != null) return lp;
        }
        return null;
    }

    /**
     * True when the player's current walk destination equals the given template tile.
     * Reads getLocalDestinationLocation() which the client sets same-tick on a click.
     */
    private boolean isDestinationTile(WorldPoint expectedTemplate) {
        LocalPoint dest = client.getLocalDestinationLocation();
        if (dest == null) return false;
        WorldPoint destTemplate = WorldPoint.fromLocalInstance(client, dest);
        if (destTemplate == null) return false;
        return destTemplate.getX() == expectedTemplate.getX()
            && destTemplate.getY() == expectedTemplate.getY();
    }

    /**
     * Click a tile in the standing area and verify same-tick that the walk destination
     * matches. If not, fire the click again immediately. If both attempts fail, the
     * state machine will re-enter this branch on the next game tick.
     */
    /**
     * Walk to the target tile by dispatching synthetic MouseEvents on the executor
     * thread. Retries every ~50ms until the game's walk destination matches the target
     * or {@link #SPAM_MAX_ATTEMPTS} / {@link #SPAM_DEADLINE_MS} run out. Each new call
     * bumps clickGeneration so any still-running spam from a prior tick exits cleanly.
     * dispatchClick is near-instant (no 100ms Robot hover) so this is safe off the
     * client thread — no game freeze.
     */
    private void clickTileWithRetry(WorldPoint target) {
        LocalPoint lp = fromTemplate(target);
        if (lp == null) return;
        net.runelite.api.Point canvasPt = Perspective.localToCanvas(client, lp, client.getPlane());
        if (canvasPt == null) return;

        final int cx = canvasPt.getX();
        final int cy = canvasPt.getY();
        final int targetX = target.getX();
        final int targetY = target.getY();
        final int myGen = ++clickGeneration;

        clickExecutor.submit(() -> {
            try {
                long deadline = System.currentTimeMillis() + SPAM_DEADLINE_MS;
                int attempts = 0;
                while (System.currentTimeMillis() < deadline
                        && clickGeneration == myGen
                        && attempts < SPAM_MAX_ATTEMPTS) {
                    if (destinationMatches(targetX, targetY)) return;   // already walking there
                    dispatchClick(cx, cy);
                    attempts++;
                    try { Thread.sleep(100); }   // pace between the two dispatches
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                }
            } catch (Throwable ex) {
                // ExecutorService silently swallows exceptions from bare Runnables —
                // any NPE from dispatchClick / destinationMatches would vanish without
                // this. Route to the shared file sink so the trace is captured.
                logCaught("clickTileWithRetry executor", ex);
            }
        });
    }

    /**
     * Fire one MOVED -> PRESSED -> RELEASED -> CLICKED MouseEvent burst at the
     * canvas pixel.
     *
     * IMPORTANT: we do NOT use Canvas.dispatchEvent here. That routes through
     * Component.processEvent -> processMouseEvent, which for MOUSE_PRESSED calls
     * requestFocus() on the focusable canvas. On Windows the OS window manager
     * responds by raising the game window to the foreground — that's why
     * RuneLite was becoming the active window on every click. Instead we iterate
     * the canvas's registered MouseListener / MouseMotionListener collections
     * and invoke the specific handler methods directly. RuneLite and Jagex both
     * register their input handlers via the standard add*Listener paths, so the
     * game still sees every event, but AWT's focus-transfer code path never
     * runs and the OS window keeps its current activation state.
     */
    private void dispatchClick(int canvasX, int canvasY) {
        Canvas c = client.getCanvas();
        if (c == null) return;
        long t = System.currentTimeMillis();
        java.awt.event.MouseMotionListener[] motion = c.getMouseMotionListeners();
        java.awt.event.MouseListener[] mouse = c.getMouseListeners();

        MouseEvent moved = new MouseEvent(c, MouseEvent.MOUSE_MOVED, t, 0,
                canvasX, canvasY, 0, false);
        for (java.awt.event.MouseMotionListener l : motion) {
            try { l.mouseMoved(moved); }
            catch (Throwable ex) { logCaught("dispatchClick moved", ex); }
        }

        MouseEvent pressed = new MouseEvent(c, MouseEvent.MOUSE_PRESSED, t + 1,
                InputEvent.BUTTON1_DOWN_MASK, canvasX, canvasY, 1, false, MouseEvent.BUTTON1);
        for (java.awt.event.MouseListener l : mouse) {
            try { l.mousePressed(pressed); }
            catch (Throwable ex) { logCaught("dispatchClick pressed", ex); }
        }

        MouseEvent released = new MouseEvent(c, MouseEvent.MOUSE_RELEASED, t + 2,
                InputEvent.BUTTON1_DOWN_MASK, canvasX, canvasY, 1, false, MouseEvent.BUTTON1);
        for (java.awt.event.MouseListener l : mouse) {
            try { l.mouseReleased(released); }
            catch (Throwable ex) { logCaught("dispatchClick released", ex); }
        }

        MouseEvent clicked = new MouseEvent(c, MouseEvent.MOUSE_CLICKED, t + 3,
                InputEvent.BUTTON1_DOWN_MASK, canvasX, canvasY, 1, false, MouseEvent.BUTTON1);
        for (java.awt.event.MouseListener l : mouse) {
            try { l.mouseClicked(clicked); }
            catch (Throwable ex) { logCaught("dispatchClick clicked", ex); }
        }
    }

    /**
     * Worker-thread-safe destination check. Reads getLocalDestinationLocation() and
     * converts to template coords. Returns false on any null / conversion failure —
     * the spam loop will just fire another click, which is harmless.
     */
    private boolean destinationMatches(int targetX, int targetY) {
        try {
            LocalPoint dest = client.getLocalDestinationLocation();
            if (dest == null) return false;
            WorldPoint dt = WorldPoint.fromLocalInstance(client, dest);
            return dt != null && dt.getX() == targetX && dt.getY() == targetY;
        } catch (Throwable t) {
            return false;   // client-thread state read raced — try again next iteration
        }
    }

    /**
     * Click Vorkath's clickbox center and verify same-tick that the walk destination
     * is the front row of the standing area (currentWalkX, 4061). Retry if not.
     */
    /**
     * Compute a canvas click point that lands on Vorkath's body reliably.
     * The convex hull's pole-of-inaccessibility can drift into the sky between his
     * wingtips depending on the animation frame — clicking there misses him entirely
     * and lands on the ground / air behind. Instead, project his footprint CENTER on
     * the ground plane: WorldLocation is the SW corner for multi-tile NPCs, so center =
     * SW + size/2. That ground pixel sits directly under his body, so the click ray from
     * the camera hits his model first regardless of wing pose.
     */
    private java.awt.Point vorkathClickPoint(NPC vorkath) {
        WorldPoint wp = vorkath.getWorldLocation();
        if (wp == null) return null;
        net.runelite.api.NPCComposition comp = vorkath.getComposition();
        int size = comp != null ? comp.getSize() : 1;
        WorldPoint centerWp = new WorldPoint(wp.getX() + size / 2, wp.getY() + size / 2, wp.getPlane());
        LocalPoint centerLp = LocalPoint.fromWorld(client, centerWp);
        if (centerLp == null) return null;
        net.runelite.api.Point cp = Perspective.localToCanvas(client, centerLp, client.getPlane());
        if (cp == null) return null;
        return new java.awt.Point(cp.getX(), cp.getY());
    }

    /**
     * Prayer flick: activate now, deactivate on the next game tick. No-op if already active.
     */
    public void flickPray(String key) {
        Triple<String, Integer, Prayer> info = prayerMap.get(key);
        if (info == null) return;
        if (client.isPrayerActive(info.getRight())) return;

        final int widgetId = info.getMiddle();
        final Prayer prayer = info.getRight();
        final String target = info.getLeft();

        clientThread.invoke(() -> client.menuAction(
                -1, widgetId, MenuAction.CC_OP, 1, -1, "Activate", target
        ));

        final Object listener = new Object() {
            @Subscribe
            public void onGameTick(GameTick evt) {
                if (client.isPrayerActive(prayer)) {
                    clientThread.invoke(() -> client.menuAction(
                            -1, widgetId, MenuAction.CC_OP, 1, -1, "Deactivate", target
                    ));
                }
                eventBus.unregister(this);
            }
        };
        eventBus.register(listener);
    }

    public int findBankSlot(int itemId)
    {
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank == null)
        {
            return -1;
        }
        Item[] items = bank.getItems();
        for (int i = 0; i < items.length; i++)
        {
            Item item = items[i];
            if (item != null && item.getId() == itemId && item.getQuantity() > 0)
            {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Batched bulk withdraw — fires N items per game tick, respecting map
    // iteration order. Each item's full `quantity` of menuActions fires on
    // that item's assigned tick. Use LinkedHashMap for guaranteed order —
    // Map.of()'s iteration order is unspecified.
    // ------------------------------------------------------------------
    private final java.util.Queue<Runnable> pendingBatches = new ArrayDeque<>();
    private Object bulkBatchListener = null;

    /** Return the first id in `preferenceOrder` present in the bank, or -1.
     *  Used to pick the smallest-enough dose of super combat / antifire /
     *  antivenom at withdraw time. Reads the mirrored currentBank list —
     *  safe from the worker thread. (findBankSlot calls client.getItem-
     *  Container which asserts client-thread; we don't need the slot index
     *  here, just presence, so the currentBank scan is both correct and
     *  cheaper.) */
    private int pickAvailableDose(List<Integer> preferenceOrder) {
        int picked = -1;
        java.util.List<Integer> foundIds = new java.util.ArrayList<>();
        for (int id : preferenceOrder) {
            if (hasItem(currentBank, id)) {
                foundIds.add(id);
                if (picked < 0) picked = id;   // first hit wins (smallest preferred)
            }
        }
        // Diagnostic — investigating cases where the visible bank has a smaller
        // dose but pickAvailableDose skipped it. Prints the preference list,
        // which ids were actually found in currentBank, and the chosen one.
        // If a dose the user sees in the bank isn't in foundIds, currentBank
        // is stale relative to the visible bank widget.
        log.info("[pickAvailableDose] pref={} found={} picked={} bankSize={}",
            preferenceOrder, foundIds, picked, currentBank.size());
        return picked;
    }

    /** True if `source` (bank OR inventory) has any of `ids`. Used by
     *  hasVorkathSupplies to accept a (2/3/4)-dose potion interchangeably
     *  instead of demanding a specific dose — matches the dose-preference
     *  lists used by buildVorkathWithdraw. */
    private boolean hasAnyDose(List<Item> source, List<Integer> ids) {
        for (int id : ids) {
            if (hasItem(source, id)) return true;
        }
        return false;
    }

    /** Build a fresh withdraw plan against the current bank. The three flexible
     *  potions (super combat, extended antifire, extended antivenom) resolve to
     *  the smallest available dose per SUPER_COMBAT_WITHDRAW_PREF etc — a (2)
     *  is picked when the bank has one, else (3), else (4). Skipped entirely
     *  when the bank has none. Fixed items are added unconditionally; the bank
     *  scan in bulkWithdrawBatched will just no-op on any that are missing. */
    public LinkedHashMap<Integer, BankWithdrawItem> buildVorkathWithdraw() {
        LinkedHashMap<Integer, BankWithdrawItem> plan = new LinkedHashMap<>();

        int sc = pickAvailableDose(SUPER_COMBAT_WITHDRAW_PREF);
        if (sc >= 0) plan.put(sc, new BankWithdrawItem(1, 2, "Withdraw-1", ""));

        int af = pickAvailableDose(EXTENDED_ANTIFIRE_WITHDRAW_PREF);
        if (af >= 0) plan.put(af, new BankWithdrawItem(1, 2, "Withdraw-1", ""));

        int av = pickAvailableDose(EXTENDED_ANTIVENOM_WITHDRAW_PREF);
        if (av >= 0) plan.put(av, new BankWithdrawItem(1, 2, "Withdraw-1", ""));

        plan.put(ItemID.PRAYER_POTION4,    new BankWithdrawItem(3, 2, "Withdraw-1",  ""));
        plan.put(ItemID.COOKED_KARAMBWAN,  new BankWithdrawItem(1, 3, "Withdraw-5",  ""));
        plan.put(ItemID.SHARK,             new BankWithdrawItem(1, 5, "Withdraw-All",""));

        return plan;
    }

    /**
     * Withdraw items in strict insertion order, itemsPerTick items per game tick.
     * Accepts LinkedHashMap specifically because Map.of() and HashMap do NOT guarantee
     * iteration order and this method relies on it.
     */
    public void bulkWithdrawBatched(int itemsPerTick) {
        if (itemsPerTick < 1) itemsPerTick = 1;
        // Build the plan against the CURRENT bank so a (2/3)-dose potion is
        // used when no (4) is available. Fixed items (prayer, karambwan, shark)
        // still resolve to a single id — see buildVorkathWithdraw.
        LinkedHashMap<Integer, BankWithdrawItem> plan = buildVorkathWithdraw();
        java.util.List<Map.Entry<Integer, BankWithdrawItem>> entries = new java.util.ArrayList<>(plan.entrySet());

        for (int i = 0; i < entries.size(); i += itemsPerTick) {
            int end = Math.min(i + itemsPerTick, entries.size());
            final java.util.List<Map.Entry<Integer, BankWithdrawItem>> batch =
                new java.util.ArrayList<>(entries.subList(i, end));

            pendingBatches.add(() -> {
                for (Map.Entry<Integer, BankWithdrawItem> entry : batch) {
                    final int itemId = entry.getKey();
                    final BankWithdrawItem spec = entry.getValue();
                    final int slot = findBankSlot(itemId);
                    if (slot < 0) continue;
                    for (int j = 0; j < spec.quantity; j++) {
                        clientThread.invoke(() -> client.menuAction(
                                slot,
                                WidgetInfo.BANK_ITEM_CONTAINER.getId(),
                                MenuAction.CC_OP,
                                spec.identifier,
                                itemId,
                                "",
                                ""
//                                spec.option,
//                                spec.targetName
                        ));
                    }
                }
            });
        }

        ensureBatchListenerRunning();
    }

    /**
     * Register the per-tick batch drainer if not already registered.
     * Listener unregisters itself once the queue is empty.
     */
    private void ensureBatchListenerRunning() {
        if (bulkBatchListener != null) return;
        bulkBatchListener = new Object() {
            @Subscribe
            public void onGameTick(GameTick evt) {
                Runnable batch = pendingBatches.poll();
                if (batch != null) batch.run();
                if (pendingBatches.isEmpty()) {
                    eventBus.unregister(this);
                    bulkBatchListener = null;
                }
            }
        };
        eventBus.register(bulkBatchListener);
    }

    public int findFirstSlot(int itemId) {
        return IntStream.range(0, currentInventory.size())
                .filter(i -> currentInventory.get(i) != null)
                .filter(i -> currentInventory.get(i).getId() == itemId)
                .findFirst()
                .orElse(-1);
    }

    public int findLowestDoseSlot(List<Integer> orderedIds) {
        return IntStream.range(0, currentInventory.size())
                .filter(i -> currentInventory.get(i) != null)
                .filter(i -> orderedIds.contains(currentInventory.get(i).getId()))
                .boxed()
                .min(Comparator.comparingInt(i -> orderedIds.indexOf(currentInventory.get(i).getId())))
                .orElse(-1);
    }

    public void sipLowestDose(List<Integer> orderedIds, String option) {
        int slot = findLowestDoseSlot(orderedIds);
        if (slot < 0) return;
        Item item = currentInventory.get(slot);
        if (item == null) return;
        final int itemId = item.getId();

        clientThread.invoke(() -> {
            ItemComposition comp = client.getItemDefinition(itemId);
            client.menuAction(
                    slot,
                    9764864,
                    MenuAction.CC_OP,
                    2,                             // typically "Drink" on potions — VERIFY with MenuOptionClicked
                    itemId,
                    option,                        // "Drink"
                    "<col=ff9040>" + comp.getName() + "</col>"  // VERIFY color from log
            );
        });
    }

    public void eatFood(int itemId, String option) {
        int slot = findFirstSlot(itemId);
        if (slot < 0) return;
        Item item = currentInventory.get(slot);
        if (item == null) return;

        clientThread.invoke(() -> {
            ItemComposition comp = client.getItemDefinition(itemId);
            client.menuAction(
                    slot,
                    9764864,
                    MenuAction.CC_OP,
                    2,                             // typically "Drink" on potions — VERIFY with MenuOptionClicked
                    itemId,
                    option,                        // "Drink"
                    "<col=ff9040>" + comp.getName() + "</col>"  // VERIFY color from log
            );
        });
        lastEatTick = client.getTickCount();   // 3-tick food cooldown starts now
    }

    public GameObject findGameObject(int objectId) {
        Tile[][][] tiles = client.getScene().getTiles();
        int plane = client.getPlane();
        for (int x = 0; x < Constants.SCENE_SIZE; x++) {           // Constants.SCENE_SIZE == 104
            for (int y = 0; y < Constants.SCENE_SIZE; y++) {
                Tile tile = tiles[plane][x][y];
                if (tile == null) continue;
                for (GameObject go : tile.getGameObjects()) {
                    if (go != null && go.getId() == objectId) {
                        return go;
                    }
                }
            }
        }
        return null;
    }

    public void interactNpc(NPC target, MenuAction action, String option, String name) {
        if (target == null) return;
        // getIndex()/getName() require client thread. Do the whole thing under invoke
        // so the reads AND the menuAction run on the right thread.
        clientThread.invoke(() -> {
            try {
                int idx = target.getIndex();
                String tName = (name != null) ? name : "<col=ffff>" + target.getName();
                client.menuAction(0, 0, action, idx, -1, option, tName);
            } catch (Throwable ex) {
                // NPC mid-teardown or menuAction rejected — silently drop
            }
        });
    }

    public void interactObject(GameObject target, MenuAction action, String option) {
        if (target == null) return;   // callers pass VorkathAutoObjectIDs.* refs,
                                      // any of which can be null pre-scene-load or
                                      // after a mid-fight scene teardown.
        clientThread.invoke(() -> {
            try {
                ObjectComposition comp = client.getObjectDefinition(target.getId());
                String name = comp == null ? "" : comp.getName();

                net.runelite.api.Point sceneMin = target.getSceneMinLocation();
                if (sceneMin == null) return;   // stale ref after scene refresh
                client.menuAction(
                        sceneMin.getX(),
                        sceneMin.getY(),
                        action,
                        target.getId(),
                        -1,
                        option,
                        "<col=ffff>" + name
                );
            } catch (Throwable ex) {
                // Object mid-teardown or menuAction rejected — drop silently,
                // consistent with interactNpc.
            }
        });
    }

    private void clickOnSirsalBanker() {
        interactNpc(VorkathAutoNPCIDs.sirsalBanker, MenuAction.NPC_FIRST_OPTION, "Talk-to", null);
        clicker.randomDelayStDev(300, 350, 25);
    }

    private void clickOnTorfinn() {
        interactNpc(VorkathAutoNPCIDs.torfinn, MenuAction.NPC_THIRD_OPTION, "Ungael", null);
        clicker.randomDelayStDev(100, 150, 25);
    }

    // ================= Pre-fight top-off =================

    /**
     * Refresh the client-thread-only reads into volatile mirrors so the worker loop
     * can consume them safely. Called from Plugin.onClientTick — no worker allowed here.
     */
    public void updateMirroredGameState() {
        mirrorSuperAntifireVarbit = client.getVarbitValue(Varbits.SUPER_ANTIFIRE);
        mirrorAntifireVarbit      = client.getVarbitValue(Varbits.ANTIFIRE);
        mirrorPoisonVarp          = client.getVarpValue(VarPlayer.POISON);
        mirrorRunEnabled          = client.getVarpValue(VARP_RUN_ENERGY_STATE) == 1;
        mirrorCurrentHP           = client.getBoostedSkillLevel(Skill.HITPOINTS);
        mirrorMaxHP               = client.getRealSkillLevel(Skill.HITPOINTS);
        mirrorCurrentPrayer       = client.getBoostedSkillLevel(Skill.PRAYER);
        mirrorMaxPrayer           = client.getRealSkillLevel(Skill.PRAYER);
        mirrorAttackBoost         = client.getBoostedSkillLevel(Skill.ATTACK)   - client.getRealSkillLevel(Skill.ATTACK);
        mirrorStrengthBoost       = client.getBoostedSkillLevel(Skill.STRENGTH) - client.getRealSkillLevel(Skill.STRENGTH);
        mirrorDefenceBoost        = client.getBoostedSkillLevel(Skill.DEFENCE)  - client.getRealSkillLevel(Skill.DEFENCE);

        try {
            Player p = client.getLocalPlayer();
            if (p == null) {
                mirrorPlayerAttackingVorkath = false;
            } else {
                var target = p.getInteracting();
                mirrorPlayerAttackingVorkath = (target instanceof NPC) && ((NPC) target).getId() == 8061;
            }
        } catch (Throwable t) {
            mirrorPlayerAttackingVorkath = false;   // target or its composition mid-teardown
        }

        boolean has395 = false;
        try {
            for (Projectile pr : client.getProjectiles()) {
                if (pr != null && pr.getId() == 395) { has395 = true; break; }
            }
        } catch (Throwable t) { /* keep false */ }
        mirror395InFlight = has395;
    }

    /** OSRS food cooldown is 3 ticks. True while we can't eat another shark yet. */
    public boolean onFoodCooldown() {
        return client.getTickCount() - lastEatTick < 3;
    }

    /**
     * Feasibility gate: can we top off HP + prayer + buffs AND still have MIN_* left
     * for the fight itself? Called before consuming a single potion/food so we never
     * waste a dose that leaves us short. If false, caller should TP out (future: loot
     * check first). Shark math uses floor to avoid over-heal — matches the runtime
     * rule of "keep eating till one more would overshoot."
     */
    public boolean canAffordTopOffAndFight() {
        int curHp   = mirrorCurrentHP;
        int maxHp   = mirrorMaxHP;
        int sharksToTop = Math.max(0, (maxHp - curHp) / SHARK_HEAL_HP);

        int curPray = mirrorCurrentPrayer;
        int maxPray = mirrorMaxPrayer;
        int prayRestore = 7 + (maxPray / 4);   // 7 + floor(0.25 * prayer level)
        int prayerSipsToTop = 0;
        int simPray = curPray;
        while (simPray + prayRestore <= maxPray) {
            prayerSipsToTop++;
            simPray += prayRestore;
        }

        // In practice the bank phase withdraws exactly one antifire type for the
        // active weapon, so it's safe (and simpler) to treat super and regular as
        // interchangeable for the affordability math: if EITHER varbit is active
        // we're covered, and the dose count is the union of both extended pots.
        // Count a sip as pending when the buff runway is below MIN_BUFF_TIME_TICKS,
        // not only when the varbit is fully zero. Matches the pre-fight sip gate:
        // if that gate would fire, this math must reserve the dose too, or
        // afford=true when we're one sip away from being under the safety floor.
        int affordAntifireBuffTicks  = Math.max(superAntifireTicksLeft(), antifireTicksLeft());
        int affordAntivenomBuffTicks = antivenomTicksLeft();
        int antifireSipsToTop  = affordAntifireBuffTicks  < MIN_BUFF_TIME_TICKS ? 1 : 0;
        int antivenomSipsToTop = affordAntivenomBuffTicks < MIN_BUFF_TIME_TICKS ? 1 : 0;

        int currentSharks         = getItemCount(currentInventory, ItemID.SHARK);
        int currentPrayerDoses    = countDoses(PRAYER_POTION_IDS);
        int currentAntifireDoses  = countDoses(EXTENDED_SUPER_ANTIFIRE_IDS)
                                  + countDoses(EXTENDED_ANTIFIRE_IDS);
        int currentAntivenomDoses = countDoses(EXTENDED_ANTIVENOM_IDS);

        // The MIN reserve makes sense for prayer + sharks (consumed every fight),
        // but antifire and antivenom are BUFFS — an active buff with enough runway
        // covers the next fight without needing a dose in inventory. Without this,
        // finishing the last dose triggers a false "can't afford" the moment the
        // buff is fully active (varbit=30, 5+ min left), TP-ing us out mid-trip.
        // MIN_BUFF_TIME_TICKS (~2 min) is the same runway threshold hasEnough-
        // Supplies / canContinueFight already use.
        int minAntifireDoses    = affordAntifireBuffTicks  >= MIN_BUFF_TIME_TICKS ? 0 : MIN_SUPER_ANTIFIRE_DOSES;
        int minAntivenomDoses   = affordAntivenomBuffTicks >= MIN_BUFF_TIME_TICKS ? 0 : MIN_ANTIVENOM_DOSES;

        boolean sharkOK    = currentSharks         - sharksToTop         >= MIN_SHARKS;
        boolean prayerOK   = currentPrayerDoses    - prayerSipsToTop     >= MIN_PRAYER_DOSES;
        boolean antifireOK = currentAntifireDoses  - antifireSipsToTop   >= minAntifireDoses;
        boolean venomOK    = currentAntivenomDoses - antivenomSipsToTop  >= minAntivenomDoses;

        // Per-check diagnostic — surfaces which specific gate failed so we can
        // tell apart "actually out" vs "countDoses mis-detected" vs "buff runway
        // not what we think". Only prints on failure to keep noise down.
        if (!sharkOK || !prayerOK || !antifireOK || !venomOK) {
            System.out.println("[canAfford] FAIL — "
                + "sharks=" + currentSharks + "-" + sharksToTop + ">=" + MIN_SHARKS + "?" + sharkOK
                + " prayer=" + currentPrayerDoses + "-" + prayerSipsToTop + ">=" + MIN_PRAYER_DOSES + "?" + prayerOK
                + " antifire=" + currentAntifireDoses + "-" + antifireSipsToTop + ">=" + minAntifireDoses + "?" + antifireOK
                + " (superBuffLeft=" + superAntifireTicksLeft() + " regularBuffLeft=" + antifireTicksLeft() + " min=" + MIN_BUFF_TIME_TICKS + ")"
                + " venom=" + currentAntivenomDoses + "-" + antivenomSipsToTop + ">=" + minAntivenomDoses + "?" + venomOK
                + " (venomBuffLeft=" + affordAntivenomBuffTicks + ")");
        }

        return sharkOK && prayerOK && antifireOK && venomOK;
    }

    /**
     * Sleeping-Vorkath top-off state machine. Called once per worker-loop iteration in the
     * !vorkathAlive && !hasNpc(8058) window. Re-evaluates state each call — stateless.
     *
     * Priority order for sips: prayer > super antifire > antivenom. Each iteration fires
     * at most one action (single sip, single eat, or shark+sip combo). Combos require food
     * cooldown clear. After eating we delay 1800ms (3 ticks) so the next iteration sees a
     * clear cooldown; after single sips we delay 600ms.
     *
     * Optimization: if only one action remains (≤1 sip and ≤1 shark eat), we poke Vorkath
     * NOW and stash the last action in pendingPostPokeAction — it fires during the 8058
     * wake-up animation, saving a tick.
     */
    public void preFightTopOff() {
        // Guard: if we already queued a post-poke action, we've committed to a
        // poke this cycle — don't run preFightTopOff again. Between the poke
        // click and the 8058 wake-up spawning, the run loop's B branch keeps
        // firing (state is still !vorkathAlive && !hasNpc(8058)). Without this
        // guard, a second preFightTopOff iteration would sip an item that Path 3
        // already queued for the wake-up (because varbits haven't updated), and
        // that item ends up sipped twice: once here + once by the queued action.
        if (pendingPostPokeAction != null) {
            int now = client.getTickCount();
            final int POKE_RETRY_TICKS = 4;
            System.out.println("[preFightTopOff] guard: pending!=null, mirrorHasWakeupNpc=" + mirrorHasWakeupNpc
                + " hasNpc(8059)=" + hasNpc(8059) + " vorkathAlive=" + vorkathAlive
                + " ticksSinceLastPoke=" + (now - lastPokeTick));
            if ((now - lastPokeTick) >= POKE_RETRY_TICKS && !mirrorHasWakeupNpc) {
                overlay.setCurrentStep("poke stuck — retrying");
                clickOnVorkath();
            } else {
                overlay.setCurrentStep("waiting for 8058 wake-up (post-poke queued)");
            }
            clicker.delay(600);
            return;
        }

        int curHp   = mirrorCurrentHP;
        int maxHp   = mirrorMaxHP;
        int curPray = mirrorCurrentPrayer;
        int maxPray = mirrorMaxPrayer;
        int prayRestore = 7 + (maxPray / 4);

        boolean canEatShark     = curHp + SHARK_HEAL_HP <= maxHp
                                && getItemCount(currentInventory, ItemID.SHARK) > 0;
        int tick = client.getTickCount();
        boolean canSipPrayer    = curPray + prayRestore <= maxPray
                                && countDoses(PRAYER_POTION_IDS) > 0
                                && (tick - lastPrayerSipTick) >= 3;
        // Sip antifire when the buff is expired OR the runway is below
        // MIN_BUFF_TIME_TICKS. Old gate was varbit==0 which only tripped on
        // full expiry — a varbit of 8 (~160 game ticks left) sneaks the poke
        // through and the fight enters with barely 2 min of antifire, which
        // very likely expires mid-kill and eats a max hit. Use the same
        // threshold canAffordTopOffAndFight uses for its dose-reserve gate
        // so the two decisions agree.
        int preFightAntifireBuffTicks  = Math.max(superAntifireTicksLeft(), antifireTicksLeft());
        int preFightAntivenomBuffTicks = antivenomTicksLeft();
        boolean canSipAntifire  = preFightAntifireBuffTicks < MIN_BUFF_TIME_TICKS
                                && countDoses(activeAntifireIds()) > 0
                                && (tick - lastAntifireSipTick) >= 3;
        boolean canSipAntivenom = preFightAntivenomBuffTicks < MIN_BUFF_TIME_TICKS
                                && countDoses(EXTENDED_ANTIVENOM_IDS) > 0
                                && (tick - lastAntivenomSipTick) >= 3;
        // Super combat rebuff: divine → 5-min timer expired; regular → boost < 10.
        // needsSuperCombatRebuff() also returns false when neither potion is in inv.
        boolean canSipSuperCombat = needsSuperCombatRebuff();

        System.out.println("[PreFight] HP=" + curHp + "/" + maxHp
                + " pray=" + curPray + "/" + maxPray + " (restore=" + prayRestore + ")"
                + " antifireVarbit=" + activeAntifireVarbit()
                + " poison=" + mirrorPoisonVarp
                + " canShark=" + canEatShark
                + " canPrayer=" + canSipPrayer
                + " canAntifire=" + canSipAntifire
                + " canAntivenom=" + canSipAntivenom
                + " canSuperCombat=" + canSipSuperCombat
                + " boostMin=" + superCombatBoostMin()
                + " afford=" + canAffordTopOffAndFight());

        // Super combat rebuff counts toward "is a sip due" but is still NOT a
        // required supply — hasEnoughSupplies() / canAffordTopOffAndFight() do
        // not require it in inventory, so a run with no super combat still starts
        // fights. What it DOES do here: gate Path 1 so we don't poke while a
        // rebuff is available and unsipped. "Nice to have if you have it," but
        // if you have it, sip it before poking.
        int sipsRemaining = (canSipPrayer ? 1 : 0)
                          + (canSipAntifire ? 1 : 0)
                          + (canSipAntivenom ? 1 : 0)
                          + (canSipSuperCombat ? 1 : 0);
        int sharksToTop = Math.max(0, (maxHp - curHp) / SHARK_HEAL_HP);

        // 1. All topped — poke and start the fight. Three gates:
        //  (a) sipsRemaining == 0 && !canEatShark — nothing to sip/eat right now
        //  (b) canAffordTopOffAndFight() — supplies OR buff runway sustains it
        //  (c) buffs actually active — !canSipX is not the same as "buff on":
        //      it also flips false when the internal sip cooldown blocks a retry
        //      after a missed sip click. Without this gate, a dropped antifire
        //      click sets lastAntifireSipTick (cooldown starts) without landing
        //      the buff (varbit stays 0), sipsRemaining hits 0 for 3 ticks, and
        //      Path 1 pokes Vorkath into a no-antifire fight. Same shape for
        //      antivenom (poisonVarp) and super combat (boost decay).
        // Runway-based: a low buff (varbit>0 but ~160 ticks left) is NOT
        // acceptably buffed for a fresh poke. Same MIN_BUFF_TIME_TICKS
        // threshold as the sip gate above, so once canSipAntifire fires and
        // the sip lands, this flips back to true and the poke can proceed.
        boolean antifireBuffed    = preFightAntifireBuffTicks  >= MIN_BUFF_TIME_TICKS;
        boolean antivenomBuffed   = preFightAntivenomBuffTicks >= MIN_BUFF_TIME_TICKS;
        // Super combat: needsSuperCombatRebuff() returns true when a pot is in
        // inv AND the buff is stale (divine timer expired, or regular boost <
        // 10). Returns false when either no pot is in inv OR the buff is fresh —
        // both "safe to poke" outcomes. Handles divine + regular interchange-
        // ably per its own dispatch. Covers the same dropped-sip class of bug
        // as antifire/antivenom (though super combat has no explicit sip cool-
        // down, defense-in-depth against future changes).
        boolean superCombatBuffed = !needsSuperCombatRebuff();
        boolean buffsActive = antifireBuffed && antivenomBuffed && superCombatBuffed;
        if (sipsRemaining == 0 && !canEatShark && canAffordTopOffAndFight() && buffsActive) {
            overlay.setCurrentStep("topped off — poke");
            clickOnVorkath();
            clicker.delay(600);
            return;
        }
        // Diagnostic — if we fell through here because a buff isn't active but
        // the sip is on cooldown, tell the operator so a stuck sip is visible.
        if (sipsRemaining == 0 && !canEatShark && canAffordTopOffAndFight() && !buffsActive) {
            overlay.setCurrentStep("stuck: buff missed — wait for cooldown to retry");
            System.out.println("[preFightTopOff] buff not active but sipsRemaining=0 — likely dropped sip."
                + " antifireBuffed=" + antifireBuffed + " antivenomBuffed=" + antivenomBuffed
                + " superCombatBuffed=" + superCombatBuffed + " boostMin=" + superCombatBoostMin()
                + " lastAntifireSipTick=" + lastAntifireSipTick
                + " lastAntivenomSipTick=" + lastAntivenomSipTick);
            clicker.delay(600);
            return;
        }

        // 2. Feasibility gate — top-off math failed. We can't start another
        // fight, so treat this like the endgame branch: sacrifice supplies for
        // remaining ground loot, then TP out. Mirrors site-A trip-end exactly:
        //  (1) isLootReady() — wait for the kill-count chat msg (+2 grace ticks)
        //      or safety deadline. Otherwise TP fires between death and the
        //      ItemSpawned events for this kill's drops, especially when
        //      leftover loot from mid-fight was already drained.
        //  (2) currentGroundItems empty + no pending steps — endgame pass has
        //      actually finished picking things up.
        //  (3) lastVorkathDeathTick <= 0 fast-path — no kill this session, drop
        //      the stale ground list so the TP gate releases immediately.
        if (!canAffordTopOffAndFight()) {
            if (!isLootReady()) {
                overlay.setCurrentStep("site B: wait for kill-count msg — loot spawning");
                clicker.delay(600);
                return;
            }
            if (lastVorkathDeathTick <= 0 && !currentGroundItems.isEmpty()) {
                currentGroundItems.clear();
            }
            if (!currentGroundItems.isEmpty() && pendingLootSteps.isEmpty()) {
                overlay.setCurrentStep("site B: plan endgame loot pass");
                startEndgameLootPass();
            }
            if (currentGroundItems.isEmpty() && pendingLootSteps.isEmpty()) {
                overlay.setCurrentStep("site B: actually TP out");
                clickOnTp();
            } else {
                overlay.setCurrentStep("site B: draining loot before TP");
            }
            clicker.delay(600);
            return;
        }

        // 3. Post-poke optimization: 1 action left → poke now, queue that action
        //    to fire during 8058 wake-up. Saves ~1 tick. Runs even when ground
        //    loot is present — the wake-up sip (inventory click) and a loot
        //    pickup click (tile click) are independent actions and both fire in
        //    the same tick per OSRS mechanics. canLootRightNow no longer blocks
        //    on pendingPostPokeAction, so tickLootPass drains pickups in parallel.
        if (sipsRemaining <= 1 && sharksToTop <= 1) {
            Runnable finalAction = buildFinalPostPokeAction(canSipPrayer, canSipAntifire, canSipAntivenom, canEatShark);
            if (finalAction != null) {
                overlay.setCurrentStep("pre-poke: 1 action pending");
                pendingPostPokeAction = finalAction;
                clickOnVorkath();
                clicker.delay(600);
                return;
            }
        }

        // 4. Regular consumption — highest-priority sip, combo with shark if HP has room + cooldown clear
        // Fire ALL needed sips as REQUIRED in one consume() call. Each sip's
        // 'needed' signal here is the local canSip* gate (varbit / doses /
        // internal cooldown), not the chat-message latch consume() uses for
        // opportunistic — so this branch does the right thing even when the
        // chat flag hasn't fired yet.
        //
        // Opportunistic set: shark (auto shark eat if HP room), kara (rides
        // along on any food/sip tick), super combat (rebuff if needed).
        //
        // Delay 1800ms (3 ticks) whenever a shark went in with the packet so
        // we wait past the food cooldown before the NEXT preFightTopOff pass;
        // otherwise 600ms is enough for the queued drinks to drain.
        EnumSet<ConsumeKind> req = EnumSet.noneOf(ConsumeKind.class);
        if (canSipPrayer)      req.add(ConsumeKind.PRAYER);
        if (canSipAntifire)    req.add(ConsumeKind.ANTIFIRE);
        if (canSipAntivenom)   req.add(ConsumeKind.ANTIVENOM);
        if (canSipSuperCombat) req.add(ConsumeKind.SUPER_COMBAT);
        EnumSet<ConsumeKind> opp = EnumSet.of(ConsumeKind.SHARK, ConsumeKind.KARA);

        if (!req.isEmpty()) {
            consume(req, opp);
            clicker.delay(1800);
        } else if (canEatShark && !onFoodCooldown()) {
            // No essential sip due but HP has room for a shark; take the free heal
            // (with kara + super combat as ride-alongs).
            consume(EnumSet.of(ConsumeKind.SHARK),
                    EnumSet.of(ConsumeKind.KARA, ConsumeKind.SUPER_COMBAT));
            clicker.delay(1800);
        } else {
            // Nothing actionable this iteration — short pause, re-check.
            clicker.delay(600);
        }
    }

    /** Build the Runnable that will fire during the 8058 wake-up. Null if nothing to do. */
    /**
     * Build the single post-poke action. pendingPostPokeAction always holds ONE
     * atomic action — but that action can be an eat+sip COMBO because OSRS lets
     * you fire a food and a potion drink in the same tick (still 1 game action
     * from the player's perspective).
     * Priority: prayer > antifire > antivenom > shark; combo the sip with a shark
     * eat if HP has room and food cooldown is clear.
     */
    private Runnable buildFinalPostPokeAction(boolean prayer, boolean antifire, boolean antivenom, boolean shark) {
        String sipKey = prayer ? "prayer" : antifire ? "antifire" : antivenom ? "antivenom" : null;

        if (sipKey == null && shark) return this::eatShark;
        if (sipKey == null)         return null;

        final String key = sipKey;
        final ConsumeKind sipCk = "prayer".equals(key)    ? ConsumeKind.PRAYER
                                : "antivenom".equals(key) ? ConsumeKind.ANTIVENOM
                                                            : ConsumeKind.ANTIFIRE;
        if (shark && !onFoodCooldown()) {
            // Fire the required sip + opportunistic shark/kara at the tick the
            // Runnable runs (during 8058 wake-up). HP room / inv / overheal are
            // re-checked inside consume() at fire time.
            return () -> consume(EnumSet.of(sipCk),
                                 EnumSet.of(ConsumeKind.SHARK, ConsumeKind.KARA,
                                            ConsumeKind.SUPER_COMBAT));
        }
        return () -> consume(EnumSet.of(sipCk),
                             EnumSet.of(ConsumeKind.SUPER_COMBAT));
    }

    /**
     * Combo a shark with the requested sip when there's HP room AND a shark AND
     * we're not on food cooldown; otherwise fall back to a plain single sip. Same
     * idea as opportunisticTopOff — a sip already costs a tick, so if HP has room
     * for a shark we take the free heal. Used from P2 (single-sip essentials)
     * where HP is above HP_THRESHOLD (so the P1 combo branch didn't fire) but
     * still below max by at least SHARK_HEAL_HP.
     */
    /**
     * Preferred path when an essential sip is due AND HP has room for both a
     * shark and a karambwan without overhealing. Fires the triple; falls back
     * to sipWithSharkIfRoom (shark+sip) when karambwan is missing or HP room
     * is only wide enough for a shark; that in turn falls back to singleSip.
     * Room check: hp + SHARK_HEAL_HP + KARAMBWAN_HEAL_HP <= maxHp.
     */
    private void sipWithSharkAndKarambwanIfRoom(String key) {
        int hp    = getCurrentHP();
        int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
        boolean canTriple = hp + SHARK_HEAL_HP + KARAMBWAN_HEAL_HP <= maxHp
                         && getItemCount(currentInventory, ItemID.SHARK) > 0
                         && getItemCount(currentInventory, ItemID.COOKED_KARAMBWAN) > 0
                         && !onFoodCooldown();
        // TODO: consider whether to also fire the triple when the sip itself
        //       is not strictly needed (e.g., top-off tick with karambwan
        //       available). Right now the triple only fires from paths that
        //       ALREADY decided to sip an essential; overhealing is prevented
        //       by canTriple's HP-room check.
        if (canTriple) comboEatSharkSipAndKarambwan(key);
        else           sipWithSharkIfRoom(key);
    }

    private void sipWithSharkIfRoom(String key) {
        int hp    = getCurrentHP();
        int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
        boolean canShark = hp + SHARK_HEAL_HP <= maxHp
                        && getItemCount(currentInventory, ItemID.SHARK) > 0
                        && !onFoodCooldown();
        if (canShark) comboEatSharkAndSip(key);
        else          singleSip(key);
    }

    /** Fire a single sip by key — used by the post-poke pending action when no shark combo. */
    private void singleSip(String key) {
        if ("prayer".equals(key))              sipPrayerPotion();
        else if ("superantifire".equals(key) || "antifire".equals(key))
                                               sipActiveAntifire();
        else if ("antivenom".equals(key))      sipExtendedAntivenom();
    }

    // ================= NPC lookup helpers =================

    /**
     * First NPC in the scene with the given id, or null if none present. Iterates safely:
     * client.getNpcs() can contain nulls, and an NPC mid-teardown can throw from getId().
     */
    public NPC findNpc(int id) {
        for (NPC npc : client.getNpcs()) {
            if (npc == null) continue;
            try {
                if (npc.getId() == id) return npc;
            } catch (Throwable t) {
                // NPC in transient state — skip
            }
        }
        return null;
    }

    /** True iff at least one NPC in the scene has the given id. */
    public boolean hasNpc(int id) {
        return findNpc(id) != null;
    }

    /**
     * True iff the local player is currently interacting with Vorkath (NPC 8061).
     * Reads the mirror updated in updateMirroredGameState() — worker-thread safe.
     * Staleness bound: 1 client tick (~16ms), well within the game's 600ms tick.
     */
    public boolean isPlayerAttackingVorkath() {
        return mirrorPlayerAttackingVorkath;
    }

    /**
     * Public no-delay Vorkath attack. Fires a NPC_SECOND_OPTION attack menuAction on
     * the current Vorkath (8061). Safe to call from an @Subscribe handler on the
     * client thread — no Thread.sleep, no Robot. Used to resume the fight after the
     * zombified spawn (8063) despawns.
     */
    public void attackVorkathNow() {
        NPC vorkath = findNpc(8061);
        if (vorkath == null) return;
        attackNpc(vorkath, "<col=ffff00>Vorkath<col=ff0000>  (level-732)");
    }

    private void clickOnVorkath() { clickOnVorkath(false); }

    /** force=true bypasses the 2-tick lastVorkathClickTick throttle. Only
     *  reAttackIfBroken uses this — its trigger is "the auto-attack lock was
     *  broken this tick and we know we need to re-attack right now", not
     *  "loop wants to spam clicks". The throttle exists for the latter case
     *  where the worker loop iterates every ~1ms and could fire redundant
     *  clicks in the same game tick. */
    private void clickOnVorkath(boolean force) {
        if (vorkathAlive) {
            // Spawn-phase gate. Two independent signals — either blocks the click:
            //   1) slayer staff on → we're mid-spawn with the staff wielded
            //   2) zombifiedSpawnAlive → the spawn NPC is up (works even if we
            //      never equipped a staff, i.e., no staff in inventory)
            // equipMainWeapon (fired on projectile-146 rising edge) restores the
            // main weapon and re-opens this branch.
            if (isSlayerStaffEquipped()) return;   // still mid-spawn (staff on) — wait for swap-back
            if (zombifiedSpawnAlive) return;
            if (mirror395InFlight) return;
            if (inSpawnPhase) return;
            if (isPlayerAttackingVorkath()) return;
            int tick = client.getTickCount();
            if (!force && tick - lastVorkathClickTick < 2) return;
            lastVorkathClickTick = tick;
            // Belt+suspenders swap-back — no-op when the main weapon is already
            // worn (the isSlayerStaffEquipped return above handles the mid-spawn
            // case, so we only reach here with the swap already done or no staff
            // at all). Skip entirely in manual-cast mode: those trips don't
            // carry a staff, so there was nothing to swap in the first place.
            if (!manualCast) equipMainWeapon();
            overlay.setCurrentStep("vorkath alive, attack");
            NPC vorkath = findNpc(8061);
            interactNpc(vorkath, MenuAction.NPC_SECOND_OPTION, "Attack", "<col=ffff00>Vorkath<col=ff0000>  (level-732)");
        }
        else {
            // Semantic: poking = "start a fight". If we can't sustain another
            // fight (out of supplies), don't poke — the trip-end / site B branch
            // wants us to plan endgame loot + TP, not wake Vorkath back up.
            // Protects all callers uniformly: reAttackIfBroken (has no vorkath-
            // Alive gate) and the preFightTopOff pending-guard retry both hit
            // this dead branch and would otherwise re-poke while we're supposed
            // to be dropping sharks / picking up loot.
            if (!canAffordTopOffAndFight()) {
                overlay.setCurrentStep("vorkath dead, poke suppressed — !canAfford");
                return;
            }
            overlay.setCurrentStep("vorkath dead, poke");
            NPC sleepingHere = findNpc(8059);
            System.out.println("[clickOnVorkath poke] sleepingHere=" + sleepingHere);
            if (sleepingHere != null) {
                lastPokeTick = client.getTickCount();
                interactNpc(sleepingHere, MenuAction.NPC_FIRST_OPTION, "Poke", null);
            }
        }
        // NOTE: removed trailing clicker.randomDelayStDev(...) — that was a Thread.sleep on
        // the CALLER thread, which for 3 of the 5 callers is the client thread (wooxWalkStep,
        // reAttackIfBroken, wooxWalk recovery). Every call blocked client rendering 100-150ms.
        // The game paces actions per tick; explicit humanization here is unnecessary.
    }

    // ============================================================================
    // Spawn-phase (395 spec) sequence — reference flow for the caller / follow-up:
    //
    //   1. 395 projectile spawns (rising edge already triggers opportunisticTopOff())
    //   2. Opportunistic top-off consumes ONE action (shark OR prayer pot)
    //   3. Zombified spawn (NPC 8063) becomes alive shortly after
    //   4. equipSlayerStaff() — swap to slayer staff for magic damage (if not worn)
    //   5. clickOnZombifiedSpawn() — attack the spawn
    //   6. On projectile ID 146 (rising edge) — equipMainWeapon() to swap back
    //   7. After spawn despawns (8063 gone) — attackVorkathNow() [already wired
    //      in Plugin.onNpcDespawned and Plugin.onAnimationChanged for anim 7891]
    //
    // TODO: wire this whole sequence from a single dispatcher (Plugin.onGameTick
    //       or an @Subscribe on the 395 projectile / 8063 spawn). Right now steps
    //       2, 5, and 7 are called from separate handlers — consolidate once the
    //       weapon-swap projectile ID (step 6) is known.
    // ============================================================================

    /** Center loot tile (template coords). Vorkath's drop-pile chooses one of
     *  3 tiles based on player position when death anim finishes; standing here
     *  guarantees the pile spawns on a single tile → simpler pickup. */
    static final WorldPoint LOOT_STACK_TILE = new WorldPoint(2272, 4061, 0);

    /**
     * Walk toward LOOT_STACK_TILE. Called from onVorkathDeath() so we arrive on
     * the center tile before Vorkath's death animation finishes — all drops then
     * pile on this tile instead of being split across 3 possible spawn spots.
     * Uses clickTileWithRetry (non-blocking, executor-scheduled).
     */
    void walkToLootStack() {
        // Skip the click if we're already standing on the pile tile — the death-anim
        // walk is a no-op then, and tickWalkToLootStack() would immediately clear its
        // deadline on the next tick anyway.
        Player me = client.getLocalPlayer();
        if (me != null) {
            WorldPoint here = toTemplate(me.getWorldLocation());
            if (here != null
                    && here.getX() == LOOT_STACK_TILE.getX()
                    && here.getY() == LOOT_STACK_TILE.getY()) {
                walkingToLootStackUntilTick = -1;
                return;
            }
        }
        clickTileWithRetry(LOOT_STACK_TILE);
    }

    /** Ticks to keep re-issuing the walk click after death anim (7949) fires.
     *  Vorkath's death animation is ~5 game ticks; 6 gives one tick of slack so
     *  a late arrival still lands before the pile spawn resolves. */
    private static final int DEATH_ANIM_WALK_TICKS = 6;

    /**
     * Called every onGameTick. While walkingToLootStackUntilTick is in the future
     * and the player isn't already on LOOT_STACK_TILE, re-issue a walk click. This
     * covers deflected paths (a Karambwan or ranger blocking a tile) so we still
     * converge on the center tile before Vorkath's drop pile spawn resolves.
     * No-op outside the window. Runs on client thread (invoked from Plugin.onGameTick).
     */
    public void tickWalkToLootStack() {
        if (walkingToLootStackUntilTick < 0) return;
        int now = client.getTickCount();
        if (now > walkingToLootStackUntilTick) {
            walkingToLootStackUntilTick = -1;
            return;
        }
        Player p = client.getLocalPlayer();
        if (p == null) return;
        WorldPoint here = toTemplate(p.getWorldLocation());
        if (here != null
                && here.getX() == LOOT_STACK_TILE.getX()
                && here.getY() == LOOT_STACK_TILE.getY()) {
            // Arrived — stop spamming; leaves the pile logic to pick from a single tile.
            walkingToLootStackUntilTick = -1;
            return;
        }
        // Skip re-click if a walk to this tile is already in flight (destinationMatches
        // is checked again inside clickTileWithRetry's executor loop, so this is belt+
        // suspenders — avoids the log spam / clickGeneration churn on quiet ticks).
        clickTileWithRetry(LOOT_STACK_TILE);
    }

    /**
     * Break the Vorkath auto-attack lock during the 395 (spider-spec) window.
     * Clicks the player's own tile — the walk packet reaches the server, resolves
     * to zero movement, and clears the pending NPC interaction. Non-blocking
     * single dispatch; called across successive ticks by Plugin.onGameTick for
     * redundancy (pendingFloorClicksFor395).
     */
    /** Break the Vorkath attack lock by walk-clicking the south end of the
     *  player's current column. Called on the "You have been frozen!" chat
     *  event — the spawn's binding projectile has landed so the player can't
     *  actually move, but the walk packet still resolves at the server and
     *  cancels the pending Vorkath interaction. y=4056 is well south of both
     *  safe rows (fang 4058 / lance 4059) so if the player somehow isn't
     *  frozen (edge case), pathing routes them safely south of the attack row
     *  instead of into the acid pattern — pathing stops at the safe tile on
     *  its own. */
    void clickSouthAcidOnCurrentColumn() {
        Player p = client.getLocalPlayer();
        if (p == null) return;
        WorldPoint player = toTemplate(p.getWorldLocation());
        if (player == null) return;
        WorldPoint south = new WorldPoint(player.getX(), 4056, 0);
        System.out.println("[clickSouthAcidOnCurrentColumn] frozen — clicking " + south
            + " (from player " + player + ")");
        clickTileWithRetry(south);
    }

    void clickFloorForSpec() {
        Player p = client.getLocalPlayer();
        if (p == null) return;
        WorldPoint player = toTemplate(p.getWorldLocation());
        if (player == null) return;
        // Reverted to same-tile click for testing — the inSpawnPhase gate in
        // clickOnVorkath now covers the projectile→spawn gap that used to let
        // the C branch sneak in a re-attack. If same-tile turns out to still
        // not cancel the attack lock, restore the sidestep version:
        //   int stepX = (player.getX() > 2269) ? player.getX() - 1 : player.getX() + 1;
        //   WorldPoint sidestep = new WorldPoint(stepX, player.getY(), 0);
        //   clickTileWithRetry(sidestep);
        System.out.println("[clickFloorForSpec] clicking own tile " + player);
        clickTileWithRetry(player);
    }

    void clickOnZombifiedSpawn() {
        // Spawn (8063) is fought with the same NPC_SECOND_OPTION "Attack" menu
        // as Vorkath. Pass name=null so interactNpc reads getName() on the client
        // thread (avoids hardcoding a wrong combat-level suffix).
        NPC spawn = findNpc(8063);
        System.out.println("[clickOnZombifiedSpawn] spawn=" + spawn
            + " hasStaff=" + hasSlayerStaff() + " manualCast=" + manualCast);
        if (spawn == null) return;

        if (manualCast) {
            // Manual-cast branch — no staff in inventory, so we can't wield.
            // Fire the manual magic-cast sequence instead of the normal
            // wield-and-attack path.
            // TODO(user): implement manualCastOnSpawn() — spellbook click →
            //             target spawn, or a menuAction sequence, whatever
            //             the manual-cast flow needs. Called ONCE per spawn.
            manualCastOnSpawn(spawn);
            return;
        }

        // Normal branch — swap to slayer staff (or no-op if already worn) then
        // attack the spawn.
        equipSlayerStaff();
//        [MENU]  action=NPC_SECOND_OPTION id=48788 itemId=-1 param0=0 param1=0 option='Attack' target='<col=ffff00>Zombified Spawn<col=ff00>  (level-64)'
        interactNpc(spawn, MenuAction.NPC_SECOND_OPTION, "Attack", "<col=ffff00>Zombified Spawn<col=ff00>  (level-64)");
    }

    /**
     * Manual-cast path for the zombified spawn (8063). Fired when the plugin
     * has no slayer staff to wield (manualCast == true). Called once per
     * spawn from clickOnZombifiedSpawn.
     *
     * TODO(user): implement. Options:
     *   - Click a spell icon on the magic tab widget, then click the spawn.
     *   - Use menuAction with the spell's widgetId + identifier to cast
     *     directly at the spawn NPC (single click, no tab open needed).
     *   - Whatever autocast/spellbook setup the account is using.
     * Called from client thread if needed; wrap game-state mutations in
     * clientThread.invoke.
     */
    void manualCastOnSpawn(NPC spawn) {
        System.out.println("[manualCastOnSpawn] TODO — implement manual spell cast on spawn=" + spawn);
//        action=WIDGET_TARGET_ON_NPC id=46869 itemId=-1 param0=0 param1=0 option='Cast' target='<col=00ff00>Crumble Undead</col><col=ffffff> -> <col=ffff00>Zombified Spawn<col=ff00>  (level-64)
        // no-op stub; nothing happens until this is filled in.
        int idx = spawn.getIndex();

        clientThread.invoke(() ->
                client.menuAction(
                0,
                0,
                MenuAction.WIDGET_TARGET_ON_NPC,
                idx,
                -1,
                "Cast",
                "<col=00ff00>Crumble Undead</col><col=ffffff> -> <col=ffff00>Zombified Spawn<col=ff00>  (level-64)"
        ));
    }

    /**
     * Step 4 of the spawn-phase sequence: wield the slayer staff so we hit the
     * zombified spawn (8063) with magic. No-op if it is already the equipped weapon.
     *
     * TODO:
     *   - Confirm the exact slayer staff variant (Slayer's staff 4170, Slayer's
     *     staff (e) 21255, Trident-of-swamp, etc.) — plumb the ItemID into a field.
     *   - Follow the eatFood/sipLowestDose pattern below (MenuAction.CC_OP on the
     *     inventory widget 9764864). The Wield identifier param is typically 3 —
     *     VERIFY once with MenuOptionClicked, then hardcode.
     *   - Consider a lastEquipSwapTick cooldown so we don't spam the wield click
     *     while the animation locks the player.
     */
    void equipSlayerStaff() {
        if (isSlayerStaffEquipped()) {
            // Already wielded → no manual cast needed.
            manualCast = false;
            return;
        }
        int slot = findFirstSlot(ItemID.SLAYERS_STAFF);
        if (slot < 0) {
            // No staff in inventory → set the manual-cast flag so
            // clickOnZombifiedSpawn routes to the manual-cast branch when the
            // 8063 spawn appears. Recomputed on every equipSlayerStaff call.
            manualCast = true;
            System.out.println("[equipSlayerStaff] no staff — manualCast=true");
            clientThread.invoke(() -> client.menuAction(
                    -1,
                    14286881,
                    MenuAction.WIDGET_TARGET,
                    0,
                    -1,
                    "Cast",
                    "<col=00ff00>Crumble Undead</col>"
            ));
            return;
        }
        Item item = currentInventory.get(slot);
        if (item == null) {
            manualCast = true;
            return;
        }
        // Staff present and we'll wield now → clear the flag.
        manualCast = false;

        clientThread.invoke(() -> client.menuAction(
                slot,
                9764864,
                MenuAction.CC_OP,
                3,
                ItemID.SLAYERS_STAFF,
                "Wield",
                "<col=ff9040>Slayer's staff</col>"
        ));
    }

    /**
     * Step 6 of the spawn-phase sequence: swap back to the main weapon once the
     * spawn is dead / the trigger projectile (ID 146) fires. Mirrors
     * equipSlayerStaff(). Uses the mainWeapon field (LANCE or FANG)
     * auto-detected on startup and refreshed on every EQUIPMENT container change.
     *
     * Trigger: Plugin should call this on the rising edge of projectile ID 146
     * (was395PresentLastTick pattern — same shape as opportunisticTopOff hookup).
     *
     * TODO:
     *   - Fill in the CC_OP wield click (same shape as equipSlayerStaff()).
     *   - VERIFY the Wield identifier (probably 3) with MenuOptionClicked once.
     *   - Wire the rising-edge-on-146 trigger in VorkathAutoPlugin.onGameTick.
     */
    void equipMainWeapon() {
        if (isMainWeaponEquipped()) return;   // already worn — skip redundant wield
        int weaponId = (getMainWeapon() == MainWeapon.LANCE)
            ? ItemID.DRAGON_HUNTER_LANCE
            : ItemID.OSMUMTENS_FANG;

        int slot = findFirstSlot(weaponId);
        if (slot < 0) return;
        Item item = currentInventory.get(slot);
        if (item == null) return;

        clientThread.invoke(() -> client.menuAction(
                slot,
                9764864,
                MenuAction.CC_OP,
                3,
                weaponId,   // was ItemID.SLAYERS_STAFF (copy-paste bug) — must match the item being wielded
                "Wield",
                weaponId == ItemID.DRAGON_HUNTER_LANCE
                    ? "<col=ff9040>Dragon hunter lance</col>"
                    : "<col=ff9040>Osmumten's fang</col>"
        ));
    }

    /**
     * O(N) scan of currentEquipment for the slayer staff. Same shape as
     * detectMainWeapon() — robust to a short/sparse Items array (nulls in unused
     * slots) and doesn't hardcode the WEAPON slot index. currentEquipment has at
     * most ~14 entries so the scan is effectively free. Used by equipSlayerStaff()
     * to skip the wield click when the staff is already worn.
     */
    private boolean isSlayerStaffEquipped() {
        for (Item it : currentEquipment) {
            if (it == null) continue;
            if (it.getId() == ItemID.SLAYERS_STAFF) return true;
        }
        return false;
    }

    /** True iff a slayer staff is in inventory (usable for the spawn-phase
     *  weapon swap). When false, the spawn phase attacks with the main weapon
     *  instead — slower kills on the spawn but no dependency on the staff. */
    public boolean hasSlayerStaff() {
        return findFirstSlot(ItemID.SLAYERS_STAFF) >= 0;
    }

    /**
     * O(N) scan for the currently-selected main weapon (LANCE or FANG, from the
     * mainWeapon field). Same shape as isSlayerStaffEquipped(). Used by
     * equipMainWeapon() and by clickOnVorkath() so we don't try to attack Vorkath
     * while the slayer staff is still worn from the spawn phase.
     */
    private boolean isMainWeaponEquipped() {
        int weaponId = (getMainWeapon() == MainWeapon.LANCE)
            ? ItemID.DRAGON_HUNTER_LANCE
            : ItemID.OSMUMTENS_FANG;
        for (Item it : currentEquipment) {
            if (it == null) continue;
            if (it.getId() == weaponId) return true;
        }
        return false;
    }

    private void clickOnIceChunks() {
        interactObject(VorkathAutoObjectIDs.vorkathIceChunksOutside, MenuAction.GAME_OBJECT_FIRST_OPTION, "Climb-over");
        clicker.randomDelayStDev(500, 650, 25);
    }

    private void clickOnRestorationPool() {
        interactObject(VorkathAutoObjectIDs.restorationPoolPOH, MenuAction.GAME_OBJECT_FIRST_OPTION, "Drink");
        clicker.randomDelayStDev(500, 650, 25);
    }

    private void clickOnPortalNexus() {
        interactObject(VorkathAutoObjectIDs.portalNexus, MenuAction.GAME_OBJECT_FIRST_OPTION, "Lunar Isle");
        clicker.randomDelayStDev(500, 650, 25);
    }

    private void clickOnBankBooth() {
        interactObject(VorkathAutoObjectIDs.lunarIsleBankBooth, MenuAction.GAME_OBJECT_SECOND_OPTION, "Bank");
        clicker.randomDelayStDev(500, 650, 25);
    }

    private void clickOnTp() {
        overlay.setCurrentStep("click TP out");
        int now = client.getTickCount();

        if (now - lastTpClickTick < TP_DEBOUNCE_TICKS) return;

        if (!isInVorkathRegion()) return;
        lastTpClickTick = now;
        clientThread.invoke(() -> client.menuAction(
                -1, 14286882, MenuAction.CC_OP, 1, -1, "Cast", "<col=00ff00>Teleport to House</col>"));
    }

    public void walkHere(Point pt) {
        clientThread.invoke(() -> client.menuAction(
                (int) pt.getX(), (int) pt.getY()+22, MenuAction.WALK, 0, 0, "Walk here", ""));
    }

    // ================================================================================
    //                                    LOOT
    // Foundation only — helpers + data model + one-step-per-tick executor + trigger
    // stub. The decision logic (rank + swap) will be added in a follow-up chunk once
    // this plumbing is confirmed.
    // ================================================================================

    /** A single action in a loot plan: pick up from ground, drop from inventory, or consume. */
    static final class LootStep {
        enum Kind { PICKUP, DROP, CONSUME }
        final Kind kind;
        final int itemId;
        final int slot;              // DROP only; -1 otherwise
        final WorldPoint tile;       // PICKUP only; null otherwise
        final String consumeKey;     // CONSUME only; one of "shark","karambwan","prayer","antifire","antivenom"

        private LootStep(Kind k, int id, int s, WorldPoint t, String ck) {
            kind = k; itemId = id; slot = s; tile = t; consumeKey = ck;
        }
        static LootStep pickup(int id, WorldPoint t) { return new LootStep(Kind.PICKUP, id, -1, t, null); }
        static LootStep drop(int id, int slot)       { return new LootStep(Kind.DROP,   id, slot, null, null); }
        static LootStep consume(String key)          { return new LootStep(Kind.CONSUME, -1, -1, null, key); }

        @Override public String toString() {
            switch (kind) {
                case PICKUP:  return "PICKUP id=" + itemId + " tile=" + tile;
                case DROP:    return "DROP id="   + itemId + " slot=" + slot;
                case CONSUME: return "CONSUME "   + consumeKey;
                default:      return kind.toString();
            }
        }
    }

    /** Ordered queue of steps to execute one per game tick. */
    private final Deque<LootStep> pendingLootSteps = new ArrayDeque<>();
    /** Rate-limit executor to one action per two ticks so consecutive drops read human. */
    private volatile int lastLootActionTick = -100;

    /** Items that must NEVER be dropped from inventory (rune pouch, slayer staff,
     *  and superior dragon bones once picked up — bones are mandatory keeps). */
    public boolean isProtectedInInventory(int id) {
        return LOOT_PROTECTED_ITEM_IDS.contains(id)
            || id == LOOT_SUPERIOR_DRAGON_BONES;
    }

    /** Stackable → 1 slot regardless of quantity; unstackable → quantity slots. */
    public boolean isStackable(int id) {
        try {
            ItemComposition comp = itemManager.getItemComposition(id);
            return comp != null && comp.isStackable();
        } catch (Throwable t) { return false; }
    }

    /** GE price per slot for a specific inventory item slot. Uses ItemManager live price. */
    public long gpsForInventory(Item item) {
        if (item == null || item.getId() < 0) return 0;
        int unit  = itemManager.getItemPrice(item.getId());
        int slots = isStackable(item.getId()) ? 1 : Math.max(item.getQuantity(), 1);
        return slots > 0 ? (long) unit * (long) item.getQuantity() / slots : 0;
    }

    /**
     * CONSERVATIVE while we can genuinely start another fight — respect MIN_*
     * floors, don't strand the next kill. AGGRESSIVE otherwise — we're TPing
     * out anyway, so drop anything except protected items.
     *
     * Must include canAffordTopOffAndFight() to stay aligned with the site A/B
     * trip-end trigger. Without it, buffs-active states pass the first two gates
     * (hasEnoughSupplies fast-paths on buff runway, canContinueFight passes on
     * HP+antifire) so mode returns CONSERVATIVE, but canAffordTopOffAndFight can
     * still fail (e.g., 0 antifire doses in inventory with a valid buff). Site B
     * then builds an endgame plan with DROP steps, executeDrop refuses because
     * mode is CONSERVATIVE, plan stalls, currentGroundItems never drains — the
     * classic "stuck at draining before TP" deadlock.
     */
    public LootMode currentLootMode() {
        return (hasEnoughSupplies() && canContinueFight() && canAffordTopOffAndFight())
            ? LootMode.CONSERVATIVE
            : LootMode.AGGRESSIVE;
    }

    /** Aggregated view of ground items grouped by itemId across tiles. */
    static final class GroundGroup {
        final int itemId;
        final boolean stackable;
        long totalPrice;       // sum of gePrice (unit*qty) across tiles
        int totalQuantity;
        /** Ordered list of (tile, quantity) — one entry per tile the item appears on. */
        final List<TileQty> tiles = new ArrayList<>();
        /**
         * -1 = compute from defaults. 0 = the pickup merges into an existing
         * inventory slot of the same stackable item (see aggregateGroundItems).
         * Any picked value overrides the default calc.
         */
        int slotCostOverride = -1;
        GroundGroup(int id, boolean s) { itemId = id; stackable = s; }
        /** Slot cost if we picked up ALL of this group. Stackable merges to 1;
         *  0 when we already have that stackable in inventory. */
        int slotCost()  {
            if (slotCostOverride >= 0) return slotCostOverride;
            return stackable ? 1 : totalQuantity;
        }
        /** gp per slot for ranking. Free-slot pickups (cost 0) rank at MAX so they always win. */
        long gps()      {
            int sc = slotCost();
            if (sc == 0) return Long.MAX_VALUE;
            return totalPrice / sc;
        }
    }

    static final class TileQty {
        final WorldPoint tile;
        final int quantity;
        TileQty(WorldPoint t, int q) { tile = t; quantity = q; }
    }

    /**
     * Group currentGroundItems by itemId, summing across tiles for stackables.
     * Prices are looked up live via ItemManager.getItemPrice(id).
     *
     * Slot-cost adjustment: if a stackable ground group has the SAME itemId
     * already present in currentInventory, picking it up merges into the existing
     * slot — 0 additional slots consumed. slotCost is set to 0 (gps → MAX) so
     * these entries always win the ranking. Applies to noted items (which are
     * stackable) and to intrinsically stackable items (coins, bolts, runes, …).
     */
    public List<GroundGroup> aggregateGroundItems() {
        Map<Integer, GroundGroup> byId = new HashMap<>();
        // Hide-specific pre-chat-msg deferral: after a kill lands but before
        // isLootReady() flips (kill-count chat + ItemSpawned grace ticks), any
        // hides on the ground are candidates for pickup only if inventory has
        // room to spare. But the endgame planner doesn't know the FULL pile
        // yet — a rare / bone from this kill might still be en route. Filling
        // free slots with hides now guarantees a pickup→drop→re-pickup churn
        // once the fresh rares land and endgame needs the slots back.
        //
        // Solution: skip hides until isLootReady. Rares / bones already on the
        // ground still get picked up in the interim (humanlike pacing preserved).
        // Once the chat msg confirms the full pile, hides re-enter the plan and
        // fill whatever slots remain after all higher-value drops are placed.
        boolean hidesGated = !isLootReady() && lastVorkathDeathTick > 0;
        for (GroundEntry ge : currentGroundItems) {
            if (ge == null || ge.quantity <= 0) continue;
            if (hidesGated && ge.itemId == LOOT_BLUE_DRAGONHIDE) continue;
            GroundGroup g = byId.computeIfAbsent(ge.itemId, k -> new GroundGroup(k, isStackable(k)));
            long unit = itemManager.getItemPrice(ge.itemId);
            g.totalPrice    += unit * ge.quantity;
            g.totalQuantity += ge.quantity;
            g.tiles.add(new TileQty(ge.tile, ge.quantity));
        }
        // Mark stackable groups that would merge into an existing inventory slot.
        for (GroundGroup g : byId.values()) {
            if (!g.stackable) continue;
            for (Item it : currentInventory) {
                if (it != null && it.getId() == g.itemId) {
                    g.slotCostOverride = 0;
                    break;
                }
            }
        }

        // Sort each group's tiles by Chebyshev distance to the player so
        // pickup planners visit the nearest pile first. Without this, we
        // walk to whichever pile got added to currentGroundItems earliest
        // — most visible when filling slots with blue dragonhide from
        // multiple drop tiles: we'd walk past a pile at our feet to grab
        // one across the room.
        //
        // Chebyshev (max of |dx|, |dy|) matches OSRS walk cost — orthogonal
        // and diagonal steps both cost 1 tick. Ties preserve insertion
        // order (sort is stable). Player location read on this thread — the
        // aggregate is called from client-thread paths (planners inside
        // startLootPass and the executor's client-thread invoke wrappers),
        // so getLocalPlayer() is safe here.
        Player me = client.getLocalPlayer();
        WorldPoint playerTemplate = (me != null) ? toTemplate(me.getWorldLocation()) : null;
        if (playerTemplate != null) {
            final int px = playerTemplate.getX();
            final int py = playerTemplate.getY();
            for (GroundGroup g : byId.values()) {
                if (g.tiles.size() < 2) continue;   // nothing to reorder
                g.tiles.sort((a, b) -> {
                    WorldPoint aT = toTemplate(a.tile);
                    WorldPoint bT = toTemplate(b.tile);
                    int aD = (aT == null) ? Integer.MAX_VALUE
                        : Math.max(Math.abs(aT.getX() - px), Math.abs(aT.getY() - py));
                    int bD = (bT == null) ? Integer.MAX_VALUE
                        : Math.max(Math.abs(bT.getX() - px), Math.abs(bT.getY() - py));
                    return Integer.compare(aD, bD);
                });
            }
        }
        return new ArrayList<>(byId.values());
    }

    // ---------- Decision layer ----------

    /** Snapshot of a single non-protected inventory slot for the swap planner. */
    static final class InvSlotView {
        final int slot;
        final int itemId;
        final int quantity;
        final long gps;
        boolean released;     // set true once the plan has queued a drop/consume for it
        InvSlotView(int s, int i, int q, long g) { slot = s; itemId = i; quantity = q; gps = g; }
    }

    /**
     * Decide the loot pickup / swap plan given current inventory + ground state.
     * Ranks by gp-per-slot. Returns an ordered list of LootSteps ready for the
     * executor to drain one at a time.
     *
     *   1. Mandatory pickups for superior dragon bones on the ground.
     *   2. Rank remaining ground groups by gps DESC.
     *   3. For each: if free slot → pickup. Else find lowest-gps non-protected
     *      inventory item and, if candidate gps > it, plan Consume-or-Drop + Pickup.
     *   4. CONSERVATIVE mode refuses swaps that violate MIN_* supply floors.
     */
    /** True if this pickup is safe to walk to right now without breaking
     *  the mid-fight positioning contract.
     *
     *  Mid-fight (vorkathAlive) — the pickup must be inside the 4x7 standing
     *  area (FANG's, a superset of LANCE's). Walking off breaks the attack
     *  lock AND moves us out of range for the next auto-attack. Endgame
     *  ignores this bound since we're leaving anyway.
     *
     *  Between kills (!vorkathAlive but we're staying to fight another) —
     *  no attack lock to preserve, and drops from the just-finished kill
     *  (bones especially) land at Vorkath's footprint (y=4064-ish) which is
     *  outside the 4x7 box. Skip the filter so we can grab them before the
     *  next spawn. Same reasoning applies as endgame — we're free to move.
     *
     *  IMPORTANT: the bounds are TEMPLATE coords (2269-2275, 4058-4061).
     *  Callers pass GroundEntry.tile which is instance-live in Vorkath's
     *  arena (from Tile.getWorldLocation()), so we convert to template first
     *  via toTemplate. Skipping the conversion makes the bounds always fail
     *  and disables mid-fight looting entirely. */
    private boolean isInMidFightLootArea(WorldPoint tile) {
        if (tile == null) return false;
        // Between-kills / post-death: no positioning contract to preserve.
        if (!vorkathAlive) return true;
        WorldPoint t = toTemplate(tile);
        if (t == null) return false;
        int x = t.getX(), y = t.getY();
        return x >= 2269 && x <= 2275 && y >= 4058 && y <= 4061;
    }

    public List<LootStep> buildLootPlan() {
        List<LootStep> plan = new ArrayList<>();
        LootMode mode = currentLootMode();

        // Free-slot count — the ONLY budget the planner respects. We never drop or
        // consume supplies to make room; overflow loot stays on the ground for a
        // later wake-up pass (supplies deplete naturally through combat).
        int simFree = 0;
        for (Item it : currentInventory) if (it == null || it.getId() < 0) simFree++;

        // Pickup value floor. Blue dragonhide is our 'last-resort' loot — the
        // lowest-value item we'd ever intentionally hold in inventory. So any
        // other pickup that costs a slot must beat (or match) it per slot,
        // otherwise we're worse off than just leaving the slot open for the
        // hides / bones we'll grab on the next wake-up pass. Bones bypass this
        // floor via the mandatory branch below; stackable free-merge groups
        // score gps = Long.MAX_VALUE via slotCost=0 and always pass.
        long pickupFloorGps = itemManager.getItemPrice(LOOT_BLUE_DRAGONHIDE);
        if (pickupFloorGps < 0) pickupFloorGps = 0;

        List<GroundGroup> groups = aggregateGroundItems();

        // Blue dragonhide: last-resort filler, always skipped in mid-fight. The
        // previous "skip unless AGGRESSIVE" rule caused a low-supplies kill to
        // grab hides mid-fight → endgame then dropped them to make room for a
        // higher-value drop from the same kill, an inhumane-looking pick/drop
        // cycle. Endgame picks up hides on its own (via buildEndgameLootPlan,
        // sorted gps-desc so hides fall to the end after all higher-value drops
        // are placed).
        groups.removeIf(g -> g.itemId == LOOT_BLUE_DRAGONHIDE);

        // Split bones from rest, rank rest by gps desc.
        GroundGroup bones = null;
        List<GroundGroup> rest = new ArrayList<>();
        for (GroundGroup g : groups) {
            if (g.itemId == LOOT_SUPERIOR_DRAGON_BONES) bones = g;
            else rest.add(g);
        }
        rest.sort(Comparator.comparingLong(GroundGroup::gps).reversed());

        // Mid-fight is PICK-UP ONLY. Never drop anything — not sharks, not
        // karambwans, not pots, not previously-picked-up loot. Overflow bones
        // stay on the ground; the endgame plan grabs them once supplies run
        // out AND Vorkath is dead. "Sacrifice only when we are done" applies
        // to ALL kinds of drops, including loot-for-loot swaps.
        if (bones != null) {
            for (TileQty tq : bones.tiles) {
                if (!isInMidFightLootArea(tq.tile)) continue;   // outside 4x7 — defer to endgame
                for (int i = 0; i < tq.quantity; i++) {
                    if (simFree > 0) {
                        plan.add(LootStep.pickup(bones.itemId, tq.tile));
                        simFree--;
                    }
                }
            }
        }

        // Ranked rest — same rule: pickup iff free slot, else leave on ground.
        // Stackable groups that would merge into an existing inventory slot cost 0
        // (slotCostOverride) — those always pass the fit check via gps() → MAX.
        for (GroundGroup g : rest) {
            long gps = g.gps();
            if (gps <= 0) continue;
            if (gps < pickupFloorGps) {
                // Below the blue-dragonhide floor — not worth burning a slot on.
                // Free-merge stackables have gps=Long.MAX_VALUE so they never
                // trip this. Blue dragonhide itself passes at gps==floor.
                continue;
            }

            if (g.stackable) {
                if (g.slotCost() == 0 || simFree > 0) {
                    for (TileQty tq : g.tiles) {
                        if (!isInMidFightLootArea(tq.tile)) continue;   // outside 4x7 — defer to endgame
                        plan.add(LootStep.pickup(g.itemId, tq.tile));
                    }
                    if (g.slotCost() != 0 && simFree > 0) simFree--;
                }
                // No free slot → skip; endgame plan will handle this group later.
            } else {
                outer:
                for (TileQty tq : g.tiles) {
                    if (!isInMidFightLootArea(tq.tile)) continue;   // outside 4x7 — defer to endgame
                    for (int i = 0; i < tq.quantity; i++) {
                        if (simFree > 0) {
                            plan.add(LootStep.pickup(g.itemId, tq.tile));
                            simFree--;
                        } else {
                            break outer;   // no space; leave for endgame
                        }
                    }
                }
            }
        }

        batchPlanByKind(plan);
        return plan;
    }

    /**
     * Iterate droppable (sorted asc by gps) for a slot whose gps < candidateGps AND
     * whose drop wouldn't violate a CONSERVATIVE-mode min-floor. On success, add
     * the Consume-if-beneficial or Drop step to `plan` and return true.
     */
    private boolean trySwap(long candidateGps, List<InvSlotView> droppable,
                            List<LootStep> plan, LootMode mode) {
        for (InvSlotView v : droppable) {
            if (v.released) continue;
            if (v.gps >= candidateGps) return false;   // sorted — nothing beyond this can beat it either
            if (mode == LootMode.CONSERVATIVE && violatesSupplyFloor(v, droppable)) continue;
            String consumeKey = consumeKeyIfBeneficial(v.itemId);
            if (consumeKey != null) plan.add(LootStep.consume(consumeKey));
            else                    plan.add(LootStep.drop(v.itemId, v.slot));
            v.released = true;
            return true;
        }
        return false;
    }

    /**
     * True iff releasing this slot would take remaining doses/units of the same
     * supply category below its MIN_* floor. Non-supply items → false.
     */
    private boolean violatesSupplyFloor(InvSlotView v, List<InvSlotView> droppable) {
        int id = v.itemId;
        if (id == ItemID.SHARK) {
            int remaining = 0;
            for (InvSlotView x : droppable) if (!x.released && x.itemId == ItemID.SHARK) remaining++;
            return (remaining - 1) < MIN_SHARKS;
        }
        if (id == ItemID.COOKED_KARAMBWAN) {
            int remaining = 0;
            for (InvSlotView x : droppable) if (!x.released && x.itemId == ItemID.COOKED_KARAMBWAN) remaining++;
            return (remaining - 1) < MIN_KARAMBWAN;
        }
        if (PRAYER_POTION_IDS.contains(id)) {
            int totalDoses = 0;
            for (InvSlotView x : droppable) {
                if (x.released) continue;
                int idx = PRAYER_POTION_IDS.indexOf(x.itemId);
                if (idx >= 0) totalDoses += (idx + 1);
            }
            int thisDoses = PRAYER_POTION_IDS.indexOf(id) + 1;
            return (totalDoses - thisDoses) < MIN_PRAYER_DOSES;
        }
        if (EXTENDED_SUPER_ANTIFIRE_IDS.contains(id) || EXTENDED_ANTIFIRE_IDS.contains(id)) {
            int totalDoses = 0;
            for (InvSlotView x : droppable) {
                if (x.released) continue;
                int idx = EXTENDED_SUPER_ANTIFIRE_IDS.indexOf(x.itemId);
                if (idx < 0) idx = EXTENDED_ANTIFIRE_IDS.indexOf(x.itemId);
                if (idx >= 0) totalDoses += (idx + 1);
            }
            int thisDoses = 0;
            int idx = EXTENDED_SUPER_ANTIFIRE_IDS.indexOf(id);
            if (idx < 0) idx = EXTENDED_ANTIFIRE_IDS.indexOf(id);
            if (idx >= 0) thisDoses = idx + 1;
            return (totalDoses - thisDoses) < MIN_SUPER_ANTIFIRE_DOSES;
        }
        if (EXTENDED_ANTIVENOM_IDS.contains(id)) {
            int totalDoses = 0;
            for (InvSlotView x : droppable) {
                if (x.released) continue;
                int idx = EXTENDED_ANTIVENOM_IDS.indexOf(x.itemId);
                if (idx >= 0) totalDoses += (idx + 1);
            }
            int thisDoses = EXTENDED_ANTIVENOM_IDS.indexOf(id) + 1;
            return (totalDoses - thisDoses) < MIN_ANTIVENOM_DOSES;
        }
        return false;   // not a supply — floor doesn't apply
    }

    /**
     * Return a consume key when a supply item would provide benefit if consumed
     * (heals, restores prayer, extends buff). Non-supply / non-beneficial → null
     * (caller Drops instead).
     */
    private String consumeKeyIfBeneficial(int itemId) {
        if (itemId == ItemID.SHARK) {
            int hp = getCurrentHP();
            int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
            return (hp + SHARK_HEAL_HP <= maxHp) ? "shark" : null;
        }
        if (itemId == ItemID.COOKED_KARAMBWAN) {
            int hp = getCurrentHP();
            int maxHp = client.getRealSkillLevel(Skill.HITPOINTS);
            return (hp + 18 <= maxHp) ? "karambwan" : null;   // karambwan heals ~18
        }
        if (PRAYER_POTION_IDS.contains(itemId)) {
            int pray = getCurrentPrayer();
            int maxPray = client.getRealSkillLevel(Skill.PRAYER);
            int restore = 7 + (maxPray / 4);
            return (pray + restore <= maxPray) ? "prayer" : null;
        }
        if (EXTENDED_SUPER_ANTIFIRE_IDS.contains(itemId) || EXTENDED_ANTIFIRE_IDS.contains(itemId)) {
            return "antifire";   // user rule: always drink (extends buff or wasted, but never worse than drop)
        }
        if (EXTENDED_ANTIVENOM_IDS.contains(itemId)) {
            return "antivenom";  // same rule
        }
        return null;
    }


    // ============================================================================
    // Endgame loot pass — fires only when supplies are out AND Vorkath is dead.
    // Sacrifices non-protected inventory items to grab remaining ground loot on
    // the way to TP. Never runs during a live fight (that's buildLootPlan's job).
    // ============================================================================

    /** Snapshot non-protected inventory slots as an InvSlotView list, sorted
     *  ascending by gp-per-slot. Used by both the endgame planner and the
     *  mid-fight bones-swap path. */
    private List<InvSlotView> buildDroppableList() {
        List<InvSlotView> droppable = new ArrayList<>();
        for (int i = 0; i < currentInventory.size(); i++) {
            Item it = currentInventory.get(i);
            if (it == null || it.getId() < 0) continue;
            if (isProtectedInInventory(it.getId())) continue;
            droppable.add(new InvSlotView(i, it.getId(), it.getQuantity(), gpsForInventory(it)));
        }
        droppable.sort(Comparator.comparingLong(v -> v.gps));
        return droppable;
    }

    /**
     * Stable-sort a plan so all CONSUME steps fire first, then all DROP, then
     * all PICKUP. Reads more human — nobody sips → drops → picks up interleaved;
     * humans sip everything, dump everything, then vacuum. Also reduces the
     * chance of a mid-swap mechanic (bomb, 395) firing between a drop and its
     * paired pickup and leaving inventory in a mid-cycle state.
     */
    private void batchPlanByKind(List<LootStep> plan) {
        plan.sort(Comparator.comparingInt(s -> kindOrder(s.kind)));
    }

    private static int kindOrder(LootStep.Kind k) {
        switch (k) {
            case CONSUME: return 0;
            case DROP:    return 1;
            case PICKUP:  return 2;
            default:      return 3;
        }
    }

    /**
     * Build the endgame loot plan. Unlike buildLootPlan (which never touches
     * inventory to make room), this WILL drop non-protected slots to pick up
     * higher-per-slot ground loot.
     *
     * Rules:
     *   - Only called from the "supplies out + Vorkath dead" branch, so the
     *     supplies we'd drop won't matter — we're TPing after this pass.
     *   - Value floor: only pick up ground items worth >= unnoted blue dragonhide
     *     per slot. Same floor buildLootPlan uses.
     *   - Never drop protected items (LOOT_PROTECTED_ITEM_IDS + superior bones).
     *   - Never drop a slot whose gps >= the candidate ground item's gps — that
     *     would be a net loss. Sorted asc walk short-circuits.
     *   - Direct drop, no sip-first. Endgame prioritizes speed to TP.
     */
    public List<LootStep> buildEndgameLootPlan() {
        List<LootStep> plan = new ArrayList<>();
        long pickupFloorGps = itemManager.getItemPrice(LOOT_BLUE_DRAGONHIDE);
        if (pickupFloorGps < 0) pickupFloorGps = 0;

        int simFree = 0;
        for (Item it : currentInventory) if (it == null || it.getId() < 0) simFree++;

        List<InvSlotView> droppable = buildDroppableList();

        // Ground groups ranked desc — highest value first so swaps go toward the
        // best trades.
        List<GroundGroup> groups = aggregateGroundItems();
        groups.sort(Comparator.comparingLong(GroundGroup::gps).reversed());

        for (GroundGroup g : groups) {
            long gps = g.gps();
            if (gps < pickupFloorGps) break;   // sorted desc — no more valuable items

            if (g.stackable) {
                int cost = g.slotCost();
                if (cost == 0 || simFree > 0) {
                    for (TileQty tq : g.tiles) plan.add(LootStep.pickup(g.itemId, tq.tile));
                    if (cost != 0) simFree--;
                } else if (releaseSlotForCandidate(gps, droppable, plan)) {
                    for (TileQty tq : g.tiles) plan.add(LootStep.pickup(g.itemId, tq.tile));
                    // Net simFree unchanged: dropped 1, picked up 1.
                }
                continue;
            }
            // Unstackable: one slot per unit.
            outer:
            for (TileQty tq : g.tiles) {
                for (int i = 0; i < tq.quantity; i++) {
                    if (simFree > 0) {
                        plan.add(LootStep.pickup(g.itemId, tq.tile));
                        simFree--;
                    } else if (releaseSlotForCandidate(gps, droppable, plan)) {
                        plan.add(LootStep.pickup(g.itemId, tq.tile));
                    } else {
                        break outer;   // no more swap targets — done for this group
                    }
                }
            }
        }

        batchPlanByKind(plan);
        return plan;
    }

    /** True if the item is one of our tracked supplies (food or pot). Mid-fight
     *  loot-for-loot swaps use this to explicitly NEVER drop a supply. */
    private boolean isSupplyItem(int id) {
        if (id == ItemID.SHARK)              return true;
        if (id == ItemID.COOKED_KARAMBWAN)   return true;
        if (EXTENDED_SUPER_ANTIFIRE_IDS.contains(id)) return true;
        if (EXTENDED_ANTIFIRE_IDS.contains(id))       return true;
        if (EXTENDED_ANTIVENOM_IDS.contains(id))      return true;
        if (DIVINE_SUPER_COMBAT_IDS.contains(id))     return true;
        if (SUPER_COMBAT_IDS.contains(id))            return true;
        if (PRAYER_POTION_IDS.contains(id))           return true;
        return false;
    }

    /**
     * Mid-fight loot-for-loot swap. Drops the lowest-gps NON-SUPPLY, non-protected
     * slot whose gps is strictly less than the candidate. Never touches sharks,
     * karambwans, or any potion. Returns true iff a drop was queued.
     *
     * Use case: we have a blue hide (~1500 gp) in inventory from an earlier
     * pickup, a bone (~12k gp) is on the ground, inventory is full. Drop the
     * hide, grab the bone. Or a lower-value loot for a rare drop, etc.
     */
    private boolean releaseLootSlotForCandidate(long candidateGps, List<InvSlotView> droppable, List<LootStep> plan) {
        for (InvSlotView v : droppable) {
            if (v.released) continue;
            if (v.gps >= candidateGps) return false;   // sorted asc — beyond, all are >=
            if (isSupplyItem(v.itemId)) continue;      // never sacrifice supplies mid-fight
            plan.add(LootStep.drop(v.itemId, v.slot));
            v.released = true;
            return true;
        }
        return false;
    }

    /**
     * Pick a droppable slot to release, using discard tiers (lowest tier tried
     * first). Returns true iff a drop (and possibly a preceding consume) was
     * queued.
     *
     *   Tier 1: sharks — direct drop
     *   Tier 2: karambwans — direct drop
     *   Tier 3: antifire / antivenom / super combat — SIP then drop (squeeze
     *           one last dose out of what would otherwise be tossed)
     *   Tier 4: prayer potions + anything else non-protected — direct drop
     *           (prayer is per user preference: don't waste a tick on a sip
     *           we won't use)
     *
     * Within a tier, pick the lowest-gps candidate whose gps is strictly less
     * than the ground item's gps. Never drops something worth as much or more
     * than the pickup — that would be a net loss.
     */
    private boolean releaseSlotForCandidate(long candidateGps, List<InvSlotView> droppable, List<LootStep> plan) {
        return releaseSlotForCandidate(candidateGps, droppable, plan, 5);
    }

    /** Overload with tier cap. Callers pass a maxTier to restrict which items
     *  are eligible for release. E.g., mid-fight bones swap passes 2 to allow
     *  only sharks and karambwans; endgame passes 5 to allow anything non-
     *  protected including spare pots. Tier order per endgameDropTier(). */
    private boolean releaseSlotForCandidate(long candidateGps, List<InvSlotView> droppable, List<LootStep> plan, int maxTier) {
        for (int tier = 1; tier <= maxTier; tier++) {
            InvSlotView chosen = null;
            for (InvSlotView v : droppable) {
                if (v.released) continue;
                if (v.gps >= candidateGps) continue;
                if (endgameDropTier(v.itemId) != tier) continue;
                if (chosen == null || v.gps < chosen.gps) chosen = v;
            }
            if (chosen == null) continue;
            // Potions (tier 4) get a consume-first pass — sipping the pot before
            // dropping it extends the active buff / heals if there's HP room.
            // Blue hides (tier 3) never yield anything useful from consumeKey-
            // IfBeneficial (it returns null for non-supplies), so gating on
            // tier == 4 keeps the semantic clear even though the null check
            // would also make tier == 3 a no-op.
            if (tier == 4) {
                String key = consumeKeyIfBeneficial(chosen.itemId);
                if (key != null) plan.add(LootStep.consume(key));
            }
            plan.add(LootStep.drop(chosen.itemId, chosen.slot));
            chosen.released = true;
            return true;
        }
        return false;
    }

    /** Discard-priority tier for endgame swap decisions. See releaseSlotForCandidate.
     *  Tier order = drop preference (1 = drop first).
     *    Tier 1: sharks — food, replaceable at bank
     *    Tier 2: karambwans — food, replaceable at bank
     *    Tier 3: unnoted blue dragonhide — pickup loot, cheap; drop before
     *            potions so we don't waste a sip-then-drop-then-repick cycle
     *            (was dropping potions, picking up hides, then dropping hides
     *            to re-pick the potions we just dropped).
     *    Tier 4: super combat / antifire / antivenom — potions with consume-first
     *            benefit if beneficial (extends buff or heals via combo).
     *    Tier 5: prayer pots + anything else non-protected — direct drop, last resort. */
    private int endgameDropTier(int itemId) {
        if (itemId == ItemID.SHARK)              return 1;
        if (itemId == ItemID.COOKED_KARAMBWAN)   return 2;
        if (itemId == LOOT_BLUE_DRAGONHIDE)      return 3;
        if (EXTENDED_SUPER_ANTIFIRE_IDS.contains(itemId)
         || EXTENDED_ANTIFIRE_IDS.contains(itemId)
         || EXTENDED_ANTIVENOM_IDS.contains(itemId)
         || DIVINE_SUPER_COMBAT_IDS.contains(itemId)
         || SUPER_COMBAT_IDS.contains(itemId))    return 4;
        // Prayer pots and everything else non-protected — direct drop, last resort.
        return 5;
    }

    /** Public entry — build endgame plan and enqueue for tickLootPass to drain.
     *  Callers are on the worker thread (site A trip-end, site B feasibility
     *  gate); buildEndgameLootPlan calls itemManager which asserts client-
     *  thread, so hop over. The AtomicBoolean guard drops repeat submits from
     *  the 1ms worker loop while the previous invocation is still in flight.
     *
     *  Two extra steps inside the invoke prevent the trip-end deadlock where
     *  site A keeps re-firing this branch on a stale/junk ground list:
     *    (1) reconcileGroundItemsWithScene() removes entries whose ItemDespawned
     *        was never delivered (region unload, timer-expiry via scene refresh,
     *        pickup-event race). Fixes the "I see no items but the plugin
     *        thinks there are" case.
     *    (2) if the built plan is empty after reconciliation, the remaining
     *        ground items are all below floor / not takeable. In the endgame
     *        context we're leaving anyway — drop the ground-items list so the
     *        outer site-A TP-out gate (currentGroundItems.isEmpty()) releases
     *        and we actually teleport out. */
    public void startEndgameLootPass() {
        if (!endgamePlanInFlight.compareAndSet(false, true)) return;
        clientThread.invoke(() -> {
            try {
                reconcileGroundItemsWithScene();
                List<LootStep> plan = buildEndgameLootPlan();
                pendingLootSteps.clear();
                if (plan.isEmpty()) {
                    // Empty plan means one of two things — treat them differently:
                    //  (a) isLootReady=true: we've seen the full pile and there's
                    //      genuinely nothing worth grabbing. Clear so site A's
                    //      TP-out gate releases instead of re-entering us here.
                    //  (b) isLootReady=false: hides on ground got filtered by
                    //      aggregateGroundItems's pre-chat-msg gate. The pile is
                    //      real — we're just deferring hide pickup until the
                    //      chat msg confirms the full drop. Do NOT clear, or the
                    //      TP fires while hides are still sitting there.
                    if (isLootReady()) {
                        currentGroundItems.clear();
                    }
                } else {
                    enqueueLootPlan(plan);
                }
            } catch (Throwable t) {
                System.out.println("[startEndgameLootPass] " + t);
                t.printStackTrace(System.out);
            } finally {
                endgamePlanInFlight.set(false);
            }
        });
    }

    /** Drop currentGroundItems entries whose (tile, itemId) has no matching
     *  TileItem in the current scene. RuneLite doesn't always deliver an
     *  ItemDespawned event for every removal path (region unload, timer
     *  expiry via scene refresh, pickup-event race), so the mirrored list
     *  drifts. Walk the scene tiles and reconcile. Client-thread only —
     *  touches Scene / Tile / TileItem, all of which assert client-thread. */
    private void reconcileGroundItemsWithScene() {
        Scene scene = client.getScene();
        if (scene == null) return;
        Tile[][][] tiles = scene.getTiles();
        if (tiles == null) return;
        int plane = client.getPlane();
        if (plane < 0 || plane >= tiles.length) return;

        // Snapshot "what's actually on the ground" as (wp,itemId) keys.
        java.util.Set<String> actual = new java.util.HashSet<>();
        Tile[][] planeTiles = tiles[plane];
        for (int x = 0; x < planeTiles.length; x++) {
            Tile[] col = planeTiles[x];
            if (col == null) continue;
            for (int y = 0; y < col.length; y++) {
                Tile t = col[y];
                if (t == null) continue;
                java.util.List<TileItem> items = t.getGroundItems();
                if (items == null || items.isEmpty()) continue;
                WorldPoint wp = t.getWorldLocation();
                if (wp == null) continue;
                for (TileItem ti : items) {
                    if (ti == null) continue;
                    actual.add(wp.getX() + "," + wp.getY() + "," + wp.getPlane() + "," + ti.getId());
                }
            }
        }

        java.util.List<GroundEntry> stale = new java.util.ArrayList<>();
        for (GroundEntry ge : currentGroundItems) {
            if (ge == null) { stale.add(null); continue; }
            String key = ge.tile.getX() + "," + ge.tile.getY() + "," + ge.tile.getPlane() + "," + ge.itemId;
            if (!actual.contains(key)) stale.add(ge);
        }
        if (!stale.isEmpty()) currentGroundItems.removeAll(stale);
    }

    /**
     * Public entry point — build the plan and enqueue it. Picks the plan flavor
     * based on state:
     *   - !vorkathAlive && !canAffordTopOffAndFight() → endgame plan
     *     (supply sacrifices allowed to grab remaining ground loot before TP).
     *     canAffordTopOffAndFight is the single "can we start a fresh fight"
     *     gate: MIN doses of every supply after top-off math AND MIN_BUFF_TIME
     *     of buff runway. If it's false we're TP-ing out; dropping cheap slots
     *     for pickups is fine.
     *   - otherwise → regular plan (pickup-only, no drops).
     *
     * Must stay in sync with Path 1's poke gate in preFightTopOff, which also
     * uses canAffordTopOffAndFight — before, endgame also fired on
     * `!hasEnough && !canContinue` while Path 1 pokes purely on `canAfford`.
     * When those disagreed, we'd drop supplies to make room for ground loot on
     * one tick and then poke into a fresh fight on the next, wasting sharks.
     *
     * Called by tickLootPass's opportunistic re-plan on every ClientTick, plus
     * anywhere else that wants a fresh plan.
     */
    public void startLootPass() {
        if (!vorkathAlive && !canAffordTopOffAndFight()) {
            enqueueLootPlan(buildEndgameLootPlan());
        } else {
            enqueueLootPlan(buildLootPlan());
        }
    }

    // ---------- Executor ----------

    /**
     * Safe windows for looting — every unsafe phase returns false here so the
     * executor and opportunistic re-planner share one gate.
     *   - acid phase (7957): woox walk owns movement
     *   - 395 in flight: spawn-spec paced sequence
     *   - zombified spawn alive: fighting spawn
     *   - 1481 bomb in flight / just-dodged wave: dodge owns positioning
     * Mid-fight looting IS allowed (vorkathAlive is NOT a block) because after the
     * first kill the pile lives on LOOT_STACK_TILE = (2272, 4061), inside the
     * standing area and on the attack row — pickup is a right-click take with no
     * walk that could break the attack lock or drag us off row 4061.
     */
    public boolean canLootRightNow() {
        // Region gate — never loot outside the Vorkath instance.
        if (!doVorkath) return false;
        if (!isInVorkathRegion()) return false;

        // Vorkath alive is NOT a block — see contract above.
        if (vorkathAcidAnim) return false;
        if (mirror395InFlight) return false;
        if (zombifiedSpawnAlive) return false;

        // Bomb-dodge gate. Three signals, any one blocks:
        //   has1481LastTick   — cached from previous tick's projectile scan
        //   dodged1481ThisWave — dodge already fired, we're mid-walk to safety
        //   live projectile scan — belt+suspenders in case handler ordering ever
        //     lets tickLootPass run before evaluateActionsThisTick on the tick
        //     1481 spawns; the scan is a client-thread read of a small list.
        if (has1481LastTick) return false;
        if (dodged1481ThisWave) return false;
        for (Projectile p : client.getProjectiles()) {
            if (p.getId() == 1481) return false;
        }
        // NOTE: no longer gated on pendingPostPokeAction. The pending sip fires
        // during 8058 wake-up (inventory action) and a loot pickup click (tile
        // click) run in parallel — OSRS lets both resolve in the same tick.
        return true;
    }
    
    public void tickLootPass() {
        if (!canLootRightNow()) return;

        int tick = client.getTickCount();

        // Attack-cycle timing gate: while Vorkath is alive, only act inside the
        // "just attacked" window. Lance/fang attack speed is 4 ticks — anim
        // fires on tick T (hit lands), next attack fires T+4. A loot click at
        // T+2 or T+3 would walk us off the standing tile close to the next
        // attack and miss it entirely. Clicking at T+0 or T+1 leaves 2-3 ticks
        // for the pickup/drop to resolve; the server then auto-re-locks us to
        // Vorkath and the T+4 attack fires normally.
        //
        // Skip this gate when Vorkath is dead (poke path) — no attack cycle to
        // interleave with, drain the queue at full rate.
        if (vorkathAlive) {
            int sinceAttack = tick - lastAttackTick;
            if (sinceAttack < 0 || sinceAttack > 1) return;
        }

        if (pendingLootSteps.isEmpty() && !currentGroundItems.isEmpty()) {
            startLootPass();
        }
        if (pendingLootSteps.isEmpty()) return;

        if (tick - lastLootActionTick < 1) return;   // 1 action per tick (~0.6s) — human-spam pace
        LootStep step = pendingLootSteps.poll();
        if (step == null) return;
        lastLootActionTick = tick;
        switch (step.kind) {
            case PICKUP:  executePickup(step.itemId, step.tile); break;
            case DROP:    executeDrop(step.slot, step.itemId);   break;
            case CONSUME: executeConsume(step.consumeKey);       break;
        }

        if (step.kind == LootStep.Kind.PICKUP && vorkathAlive) {
            clickOnVorkath();
        }
    }

    /** Cancel any in-flight loot pass — called from reset / death / region-leave. */
    public void clearLootPass() { pendingLootSteps.clear(); }

    /** Enqueue a plan for the executor. Idempotent-ish: replaces any prior queue. */
    public void enqueueLootPlan(List<LootStep> plan) {
        pendingLootSteps.clear();
        if (plan != null) pendingLootSteps.addAll(plan);
    }

    private void executePickup(int itemId, WorldPoint sceneTile) {
        if (!canLootRightNow()) return;
        if (sceneTile == null) return;
        int baseX = client.getBaseX();
        int baseY = client.getBaseY();
        int sx = sceneTile.getX() - baseX;
        int sy = sceneTile.getY() - baseY;
        if (sx < 0 || sy < 0 || sx > 103 || sy > 103) {
            return;
        }
        final int px = sx;
        final int py = sy;
        clientThread.invoke(() -> {
            try {
                if (!canLootRightNow()) return;
                ItemComposition comp = itemManager.getItemComposition(itemId);
                client.menuAction(px, py, MenuAction.GROUND_ITEM_THIRD_OPTION, itemId, -1, "Take", comp.getName());
            } catch (Throwable ex) { logCaught("Main.execute", ex); }
        });
    }

    private void executeDrop(int slot, int itemId) {
        if (!canLootRightNow()) return;

        if (currentLootMode() == LootMode.CONSERVATIVE) return;

        clientThread.invoke(() -> {
            try {
                if (!canLootRightNow()) return;

                if (currentLootMode() == LootMode.CONSERVATIVE) return;

                ItemComposition comp = itemManager.getItemComposition(itemId);

                client.menuAction(slot, 9764864, MenuAction.CC_OP_LOW_PRIORITY, 7, itemId,
                    "Drop", "<col=ff9040>" + comp.getName() + "</col>");
            } catch (Throwable ex) { logCaught("Main.execute", ex); }
        });
    }

    private void executeConsume(String key) {
        if (key == null) return;
        if (!canLootRightNow()) return;   // last-line safety — never fire outside the arena
        switch (key) {
            case "shark":     eatShark(); break;
            case "karambwan": eatFood(ItemID.COOKED_KARAMBWAN, "Eat"); break;
            case "prayer":    sipPrayerPotion(); break;
            case "antifire":  sipActiveAntifire(); break;
            case "antivenom": sipExtendedAntivenom(); break;
            default: break;
        }
    }
}
