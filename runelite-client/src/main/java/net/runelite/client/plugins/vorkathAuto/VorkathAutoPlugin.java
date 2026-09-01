package net.runelite.client.plugins.vorkathAuto;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.bloodRuneTrue.BloodRuneTrueMain;
import net.runelite.client.plugins.plusUtils.StepOverlay;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.client.plugins.agilityAid.AgilityAidWorldPoints.CANFIS_BUSH;

@PluginDescriptor(name = "VorkathPlus", enabledByDefault = false)
@Slf4j
public class VorkathAutoPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private StepOverlay overlay;

    @Inject
    private EventBus eventBus;

    @Inject
    private ItemManager itemManager;

    @Getter
    private volatile WorldPoint localPlayerLocation;

    private volatile boolean isBankOpen;

    VorkathAutoMain main;
    private Thread mainThread;

    private boolean hasStarted = false;

    private boolean deactivatedMageForAcid = false;

    private final Set<WorldPoint> acidTargetPoints = new HashSet<>();
    private boolean acidWalkComputed = false;

    private final Set<Integer> spawnHitsplatFired = new HashSet<>();

    private boolean was1471PresentLastTick = false;
    private boolean was395PresentLastTick   = false;   // rising-edge for opportunistic top-off
    private static final int PACE_WAIT_TICKS = 8;      // idle wait ticks between each paced action in the 395/146 sequences
    private int spec395Step = 0;                       // 395 state machine — see 395 handler below for step meanings
    private boolean wasAcidAnimLastTick     = false;   // rising-edge for opportunistic top-off
    private boolean was146PresentLastTick   = false;   // rising-edge for equipMainWeapon (spawn-phase swap-back)
    private int spec146Step = 0;                       // state machine — same shape as spec395Step; fires equipMainWeapon at SPEC395_EQUIP_STEP

    private final List<Integer> TRACKED_IDS = Arrays.asList(
            VorkathAutoObjectIDs.POOL_OF_REVITALISATION,
            VorkathAutoObjectIDs.POOL_OF_REJUVENATION,
            VorkathAutoObjectIDs.FANCY_POOL_OF_REJUVENATION,
            VorkathAutoObjectIDs.ORNATE_POOL_OF_REJUVENATION,
            VorkathAutoObjectIDs.PORTAL_NEXUS,
            VorkathAutoObjectIDs.LUNAR_ISLE_BANK_BOOTH
    );

//    @Subscribe
//    public void onStatChanged(StatChanged statChanged) {
//        if (statChanged.getSkill() != AGILITY) {
//            return;
//        }
//        VorkathAutoMain.xpDrop = true;
//        overlay.setCurrentStep("xpDrop = true");
//        log.info("xpDrop = true");
//    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        ItemContainer inv = event.getItemContainer();
        if (inv == null) return;

        int containerId = event.getContainerId();

        if (containerId == InventoryID.INVENTORY.getId())
        {
            VorkathAutoMain.currentInventory.clear();
            Collections.addAll(VorkathAutoMain.currentInventory, inv.getItems());
        }
        else if (containerId == InventoryID.BANK.getId())
        {
            VorkathAutoMain.currentBank.clear();
            Collections.addAll(VorkathAutoMain.currentBank, inv.getItems());
        }
        else if (event.getContainerId() == InventoryID.EQUIPMENT.getId()) {
            VorkathAutoMain.currentEquipment.clear();
            Collections.addAll(VorkathAutoMain.currentEquipment, inv.getItems());
            // No cached main-weapon detection to refresh — getMainWeapon() reads
            // currentEquipment live on every call.
        }
    }

    // ---------- Ground item tracker ----------
    // Mirror TileItem spawn/despawn/quantity events into VorkathAutoMain.currentGroundItems
    // so the loot planner can iterate without touching GroundItemsPlugin internals.

    @Subscribe
    public void onItemSpawned(net.runelite.api.events.ItemSpawned event) {
        try {
            net.runelite.api.TileItem item = event.getItem();
            if (item == null) return;
            net.runelite.api.Tile tile = event.getTile();
            if (tile == null) return;
            WorldPoint wp = tile.getWorldLocation();
            if (wp == null) return;
            int id = item.getId();
            int qty = item.getQuantity();
            for (VorkathAutoMain.GroundEntry ge : VorkathAutoMain.currentGroundItems) {
                if (ge.itemId == id && ge.tile.equals(wp)) {
                    ge.quantity += qty;
                    return;
                }
            }
            VorkathAutoMain.currentGroundItems.add(
                    new VorkathAutoMain.GroundEntry(wp, id, qty, client.getTickCount()));
        } catch (Throwable ex) { VorkathAutoMain.logCaught("Plugin.item-tracker", ex); }
    }

    @Subscribe
    public void onItemDespawned(net.runelite.api.events.ItemDespawned event) {
        try {
            net.runelite.api.TileItem item = event.getItem();
            if (item == null) return;
            net.runelite.api.Tile tile = event.getTile();
            if (tile == null) return;
            WorldPoint wp = tile.getWorldLocation();
            if (wp == null) return;
            int id = item.getId();
            int qty = item.getQuantity();
            VorkathAutoMain.GroundEntry hit = null;
            for (VorkathAutoMain.GroundEntry ge : VorkathAutoMain.currentGroundItems) {
                if (ge.itemId == id && ge.tile.equals(wp)) { hit = ge; break; }
            }
            if (hit != null) {
                hit.quantity -= qty;
                if (hit.quantity <= 0) VorkathAutoMain.currentGroundItems.remove(hit);
            }
        } catch (Throwable ex) { VorkathAutoMain.logCaught("Plugin.item-tracker", ex); }
    }

    @Subscribe
    public void onItemQuantityChanged(net.runelite.api.events.ItemQuantityChanged event) {
        try {
            net.runelite.api.TileItem item = event.getItem();
            if (item == null) return;
            net.runelite.api.Tile tile = event.getTile();
            if (tile == null) return;
            WorldPoint wp = tile.getWorldLocation();
            if (wp == null) return;
            int id = item.getId();
            int delta = event.getNewQuantity() - event.getOldQuantity();
            VorkathAutoMain.GroundEntry hit = null;
            for (VorkathAutoMain.GroundEntry ge : VorkathAutoMain.currentGroundItems) {
                if (ge.itemId == id && ge.tile.equals(wp)) { hit = ge; break; }
            }
            if (hit != null) {
                hit.quantity += delta;
                if (hit.quantity <= 0) VorkathAutoMain.currentGroundItems.remove(hit);
            } else if (event.getNewQuantity() > 0) {
                VorkathAutoMain.currentGroundItems.add(
                        new VorkathAutoMain.GroundEntry(wp, id, event.getNewQuantity(), client.getTickCount()));
            }
        } catch (Throwable ex) { VorkathAutoMain.logCaught("Plugin.item-tracker", ex); }
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        if (!(event.getActor() instanceof NPC)) return;
        NPC npc = (NPC) event.getActor();
        int id = npc.getId();

        if (id == 8063) {
            // Zombified spawn has no health bar, so we can't use ratio. It dies in a single
            // hit — fire on the first hitsplat this specific spawn instance receives.
            if (spawnHitsplatFired.add(npc.getIndex())) {
                main.setMagePrayOn(true);
                main.setPietyOn(true);
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() == ChatMessageType.SPAM) {
            String message = event.getMessage();

            if (message.contains("You board the boat")) {
                main.boardedBoat = true;
            } else if (message.contains("You drink some of your extended super antivenom+ potion")) {
                // OSRS item name uses hyphenated "anti-venom" and there is no
                // "extended super anti-venom+" item — the previous string
                // "extended super antivenom+ potion" never matched, so
                // onSippedAntivenom() never fired and antivenomExpiryTick
                // stayed 0. Broke the buff-runway signal in hasEnoughSupplies
                // and made canAffordTopOffAndFight demand a dose reserve when
                // the buff was actually fine.
                main.onSippedAntivenom();
            } else if (message.contains("You drink some of your extended super antifire potion")) {
                main.onSippedSuperAntifire();
            } else if (message.contains("You drink some of your extended antifire potion")) {
                main.onSippedAntifire();
            } else if (message.contains("You drink some of your divine combat potion")) {
                main.onSippedSuperCombat();
            } else if (message.contains("You drink some of your super combat potion")) {
                main.onSippedSuperCombat();
            }
        } else if (event.getType() == ChatMessageType.DIALOG) {
            String message = event.getMessage();

            if (message.contains("Hi, I...")) {
                main.isTalkingToBanker = true;
            }
        } else if (event.getType() == ChatMessageType.GAMEMESSAGE) {
            String message = event.getMessage();

            if (message.contains("Oh dear, you are dead!")) {
                main.handleDeath();
                deactivatedMageForAcid = false;
                was1471PresentLastTick = false;
                was395PresentLastTick   = false;
                wasAcidAnimLastTick     = false;
                was146PresentLastTick   = false;
                spec395Step              = 0;
                spec146Step              = 0;
                spawnHitsplatFired.clear();
                acidTargetPoints.clear();
                acidWalkComputed = false;
//                hasStarted = false;
            } else if (
//                    message.contains("Your venom resistance is about to wear off") ||
                    message.contains("Your venom resistance has worn off, but you are still resistant to poison")
            ) {
                main.antivenomExpireMessage = true;
            } else if(
//                    message.contains("Your super antifire potion is about to expire") ||
                    message.contains("Your super antifire potion has expired") ||
//                    message.contains("Your antifire potion is about to expire") ||
                    message.contains("Your antifire potion has expired")
            ) {
                main.antifireExpireMessage = true;
            } else if (message.contains("Your Vorkath kill count is:")) {
                // Server prints this the same tick the drop lands — main uses
                // it to gate startEndgameLootPass and the trip-end TP so we
                // never plan or leave before the loot pile is on the ground.
                main.onKillCountMessage();
            } else if (message.contains("You have been frozen!")) {
                // Spawn spec landed — click south of our current column to
                // break the Vorkath attack lock. Player is bound so no actual
                // movement happens; the walk packet just cancels the pending
                // interaction so the next clickOnZombifiedSpawn / equipSlayer-
                // Staff isn't fighting an inflight NPC action. Replaces the
                // old clickFloorForSpec on the 395 rising edge — the projectile
                // fires ~1 tick before the actual freeze lands, so cancelling
                // then sometimes left us re-locking onto Vorkath.
                main.clickSouthAcidOnCurrentColumn();
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked e) {
        System.out.println("[MENU] "
                + " action=" + e.getMenuAction()
                + " id="     + e.getId()
                + " itemId=" + e.getItemId()
                + " param0=" + e.getParam0()
                + " param1=" + e.getParam1()
                + " option='" + e.getMenuOption() + "'"
                + " target='" + e.getMenuTarget() + "'");
    }

    @Override
    protected void startUp() throws Exception {
        // Install a JVM-wide uncaught-exception handler that appends to
        // ~/vorkath-errors.log. Catches anything the JVM's default handler
        // would otherwise print to stderr (client-thread invoke lambdas,
        // background threads, event-bus dispatch fallouts) so we have a
        // trace even when the exception doesn't route through one of our
        // @Subscribe or worker-loop catches.
        Thread.setDefaultUncaughtExceptionHandler((t, ex) ->
            VorkathAutoMain.logCaught("uncaught thread=" + t.getName(), ex));
        // Startup marker — writes to the same file so you can (a) verify the
        // path resolves correctly on this OS, (b) see when the plugin was
        // last started (bookend for reading the trace log). If this write
        // fails, no error catch will ever land in the file — check perms.
        try {
            java.io.File f = new java.io.File(
                    System.getProperty("user.home"), "vorkath-errors.log");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.FileWriter(f, true))) {
                pw.println("==== " + new java.util.Date()
                        + " [plugin startUp] path=" + f.getAbsolutePath() + " ====");
            }
            log.info("[VorkathAuto] error log at: {}", f.getAbsolutePath());
        } catch (Throwable t) {
            log.error("[VorkathAuto] failed to write startup marker", t);
        }
        VorkathAutoObjectIDs.setAllVarsNull();
        overlayManager.add(overlay);

        if (main == null) {
            main = new VorkathAutoMain(client, clientThread, overlay, this, eventBus, itemManager);
            System.out.println("initialized vorkatAuto");
        }
    }

    @Override
    protected void shutDown() throws Exception {
        stopMain();
        VorkathAutoObjectIDs.setAllVarsNull();
        overlayManager.remove(overlay);
    }

    /** Start the worker thread. Idempotent — no-op if already running. */
    private void startMain() {
        if (mainThread != null && mainThread.isAlive()) return;
        if (main == null) {
            main = new VorkathAutoMain(client, clientThread, overlay, this, eventBus, itemManager);
        }
        main.reset();
        // Populate NPC caches from currently-loaded scene — misses spawn events fired
        // before the plugin started, e.g. sleeping Vorkath (8059) already in the scene.
        clientThread.invoke(() -> VorkathAutoNPCIDs.scanScene(client));
        main.isRunning = true;
        mainThread = new Thread(main, "VorkathAuto-worker");
        mainThread.setDaemon(true);
        mainThread.start();
        overlay.setCurrentStep("status is go");
        System.out.println("status is go");
    }

    /** Stop the worker thread cleanly. Idempotent. */
    private void stopMain() {
        if (main != null) main.stop();
        if (mainThread != null) {
            mainThread.interrupt();
            try { mainThread.join(2000); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            mainThread = null;
        }
        overlay.setCurrentStep("status is stop");
        System.out.println("status is stop");
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        try {
            main.wooxWalkStep();
            main.evaluateActionsThisTick();
            main.degradePrayerIfNeeded();
            main.syncSuperCombatFromSkills();
            // Re-issue the walk-to-loot-stack click every tick until we arrive or
            // the death-anim window expires (see VorkathAutoMain.tickWalkToLootStack).
            main.tickWalkToLootStack();
        } catch (Throwable ex) {
            // Three sinks — matches VorkathAutoMain.run()'s outer catch.
            // slf4j to RuneLite log; overlay stamp (visible in-game briefly);
            // and ~/vorkath-errors.log for guaranteed persistence. Plugin
            // handlers run on the client thread — NPEs here would otherwise
            // silently die when RuneLite's log config filters stdout.
            log.error("[VorkathAuto handler] uncaught", ex);
            if (overlay != null) {
                overlay.setCurrentStep("ERR[H]: " + ex.getClass().getSimpleName()
                        + (ex.getMessage() != null ? ": " + ex.getMessage() : ""));
            }
            try {
                java.io.File f = new java.io.File(
                        System.getProperty("user.home"), "vorkath-errors.log");
                try (java.io.PrintWriter pw = new java.io.PrintWriter(
                        new java.io.FileWriter(f, true))) {
                    pw.println("---- " + new java.util.Date() + " [handler] ----");
                    ex.printStackTrace(pw);
                }
            } catch (Throwable ignore) { /* file sink best-effort */ }
        }
    }

    @Subscribe
    public void onClientTick(ClientTick event) throws AWTException {
        try {
        if (main != null) main.updateMirroredGameState();   // push client-thread-only reads to worker-safe mirrors
        toggleStatus();
        getShapeForTileObject();
        getClickboxes();

        if (isDestinationTile(VorkathAutoWorldPoints.TORFINN_RELLEKA_TILE) || isAtWorldPoint(VorkathAutoWorldPoints.TORFINN_RELLEKA_TILE)) {
            resetZoomPitchYaw(600, 4160, 0);
        } else if (isDestinationTile(VorkathAutoWorldPoints.LUNAR_ISLE_BANK_INFRONT)) {
            resetZoomPitchYaw(896, 4160, 8192);
        }

        Player p = client.getLocalPlayer();
        localPlayerLocation = p != null ? p.getWorldLocation() : null;

        Widget bank = client.getWidget(WidgetInfo.BANK_CONTAINER);
        isBankOpen = (bank != null && !bank.isHidden());

        // --- Special projectile handling ---
        boolean has395 = false, has1483 = false, has1471 = false, has146 = false;
        for (Projectile proj : client.getProjectiles()) {
            int id = proj.getId();
            if (id == 395) has395 = true;
            else if (id == 1483) has1483 = true;
            else if (id == 1471) has1471 = true;
            else if (id == 146) has146 = true;
        }

        // Is Vorkath (NPC id 8061) currently in his acid-spit animation (7957)?
        // client.getNpcs() can hold null entries and NPCs mid-teardown can throw from
        // getId()/getAnimation() (obfuscated composition is null) — guard against both.
        boolean vorkathAcidAnim = false;
        for (NPC npc : client.getNpcs()) {
            if (npc == null) continue;
            try {
                if (npc.getId() == 8061 && npc.getAnimation() == 7957) {
                    vorkathAcidAnim = true;
                    break;
                }
            } catch (Throwable t) {
                // NPC in transient state — skip
            }
        }
        main.setVorkathAcidAnim(vorkathAcidAnim);

        // --- Woox walk column: latch on animation 7957, compute exactly once per wave ---
        if (vorkathAcidAnim && !acidWalkComputed) {
            // All 1483s are already in-flight when 7957 begins — snapshot them now.
            for (Projectile proj : client.getProjectiles()) {
                if (proj.getId() == 1483) {
                    WorldPoint wp = main.toTemplate(proj.getTargetPoint());
                    if (wp != null) acidTargetPoints.add(wp);
                }
            }

            if (!acidTargetPoints.isEmpty()) {
                List<WorldPoint> acidList = new ArrayList<>(acidTargetPoints);
                WorldPoint player = main.toTemplate(client.getLocalPlayer().getWorldLocation());

                // Weapon-specific standing area: LANCE only needs 3 rows (south
                // safe row is 4059); FANG needs all 4 (south safe row is 4058).
                // Passing the wider area than needed lets acid outside the walk
                // envelope falsely poison a column.
                java.util.List<WorldPoint> standingArea =
                    (main.getMainWeapon() == VorkathAutoMain.MainWeapon.LANCE)
                    ? VorkathAutoWorldPoints.VORKATH_STANDING_AREA_LANCE
                    : VorkathAutoWorldPoints.VORKATH_STANDING_AREA_FANG;

                int walkX = VorkathAutoWorldPoints.bestColumnForWooxWalk(
                    standingArea, acidList, player);

                if (walkX == -1) {
                    // Every column has acid — try the horizontal-row fallback
                    // (longest clean run in a single row, min length 3, north-
                    // preferred on ties).
                    WorldPoint[] pair = VorkathAutoWorldPoints.longestCleanRowForWooxWalk(
                        standingArea, acidList);
                    if (pair != null) {
                        WorldPoint west = pair[0].getX() <= pair[1].getX() ? pair[0] : pair[1];
                        WorldPoint east = pair[0].getX() <= pair[1].getX() ? pair[1] : pair[0];
                        main.setHorizontalWooxWalk(west, east);
                        main.setCurrentWalkX(-1);
                    } else {
                        // No column AND no row of length >=3 — fully boxed in.
                        // Do nothing; wooxWalkStep will no-op and dodge1481 /
                        // other survival paths will handle it.
                        main.setHorizontalWooxWalk(null, null);
                        main.setCurrentWalkX(-1);
                    }
                } else {
                    // Normal vertical column — clear any horizontal fallback
                    // from a prior tick.
                    main.setHorizontalWooxWalk(null, null);
                    main.setCurrentWalkX(walkX);
                }
                VorkathAutoMain.doWooxWalk = true;

                acidWalkComputed = true;   // latch
            }
        }

        // Unlatch and clear the set once the acid animation ends.
        if (!vorkathAcidAnim && acidWalkComputed) {
            acidTargetPoints.clear();
            acidWalkComputed = false;
            main.setCurrentWalkX(-1);
            main.setHorizontalWooxWalk(null, null);
        }

        // --- Projectile-driven expected-state updates ---

        // 395: turn OFF mage AND piety.
        if (has395) {
            main.setMagePrayOn(false);
            main.setPietyOn(false);
        }

        // 1483: turn OFF mage; remember to re-activate after Vorkath's acid anim ends.
        if (has1483) {
            main.setMagePrayOn(false);
            deactivatedMageForAcid = true;
        }

        // Re-activate mage once the acid animation is no longer playing.
        if (deactivatedMageForAcid && !vorkathAcidAnim) {
            main.setMagePrayOn(true);
            deactivatedMageForAcid = false;
        }

        // 1471: prayer-disable projectile — the tick it disappears, want quick prayers back.
        if (was1471PresentLastTick && !has1471) {
            main.setMagePrayOn(true);
            main.setPietyOn(true);
        }
        was1471PresentLastTick = has1471;

        // 395 (spawn spider spec) — paced sequence, human-plausible.
        // Two actions separated by PACE_WAIT_TICKS idle ticks on each side:
        //   tick 0 (rising edge):           start counter
        //   tick 1..PACE_WAIT_TICKS:        idle wait
        //   tick PACE_WAIT_TICKS+1:         opportunisticTopOff()
        //   tick PACE_WAIT_TICKS+2..2*PACE_WAIT_TICKS+1: idle wait
        //   tick 2*PACE_WAIT_TICKS+2:       equipSlayerStaff()
        // With PACE_WAIT_TICKS = 4 that means top-off on tick 5, equip on tick 10.
        // clickOnZombifiedSpawn (fires on 8063 spawn) also calls equipSlayerStaff()
        // as a backstop for the rare case pacing didn't finish before the spawn.
        if (!was395PresentLastTick && has395) {
            // Rising edge — mark spawn phase so clickOnVorkath is blocked until
            // the spawn dies. Attack-lock break moved to the "You have been
            // frozen!" chat handler: same-tile click here was firing before the
            // player was frozen, sometimes leaving us still targeting Vorkath
            // when the spawn appeared. The frozen chat msg is the reliable
            // "spawn spec landed, cancel attack lock NOW" signal.
            main.inSpawnPhase = true;   // block clickOnVorkath until spawn dies
            spec395Step = 1;
        } else if (spec395Step > 0) {
            if (spec395Step == PACE_WAIT_TICKS + 1) {
                main.opportunisticTopOff();
                spec395Step++;
            } else if (spec395Step == 2) {
                main.equipSlayerStaff();
                spec395Step = 0;
            } else {
                spec395Step++;                       // idle wait tick
            }
        }

        // Step 6 of the spawn-phase sequence — paced swap-back, same PACE_WAIT_TICKS
        // beat as the 395 sequence:
        //   tick 0 (146 rising edge):       start counter
        //   tick 1..PACE_WAIT_TICKS:        idle wait
        //   tick PACE_WAIT_TICKS+1:         equipMainWeapon()
        // With PACE_WAIT_TICKS = 4 that's equipMainWeapon on tick 5. No explicit
        // attack after — attackVorkathNow() already fires from onNpcDespawned(8063)
        // and the death-anim 7891 handler when the spawn dies.
        if (!was146PresentLastTick && has146) {
            // 146 rising edge — invulnerability drops, Vorkath is hittable now.
            // Attack immediately (same tick prayers toggle back). Then the paced
            // machine below equips the main weapon after PACE_WAIT_TICKS — the
            // attack lands with the staff still on for one auto-attack cycle,
            // then the swap catches up. Not ideal DPS on the first hit, but
            // matches how humans re-engage: click first, swap gear during the
            // walk-in / animation window.
            main.attackVorkathNow();
            spec146Step = 1;
        } else if (spec146Step > 0) {
            if (spec146Step == PACE_WAIT_TICKS + 1) {
                // Skip weapon swap on manual-cast trips — no staff was ever
                // equipped, so equipMainWeapon would be an idempotent no-op
                // at best (main weapon is worn) and a wasted click at worst.
                if (!main.manualCast) main.equipMainWeapon();
                spec146Step = 0;
            } else {
                spec146Step++;                       // idle wait tick
            }
        }

        wasAcidAnimLastTick   = vorkathAcidAnim;
        was395PresentLastTick = has395;
        was146PresentLastTick = has146;

        // --- Reconcile desired prayer state against actual state ---
        main.reconcilePrayers();

        // --- Advance the loot-pickup state machine (one action per two ticks) ---
        main.tickLootPass();
        } catch (Throwable ex) {
            // Three sinks — matches VorkathAutoMain.run()'s outer catch.
            // slf4j to RuneLite log; overlay stamp (visible in-game briefly);
            // and ~/vorkath-errors.log for guaranteed persistence. Plugin
            // handlers run on the client thread — NPEs here would otherwise
            // silently die when RuneLite's log config filters stdout.
            log.error("[VorkathAuto handler] uncaught", ex);
            if (overlay != null) {
                overlay.setCurrentStep("ERR[H]: " + ex.getClass().getSimpleName()
                        + (ex.getMessage() != null ? ": " + ex.getMessage() : ""));
            }
            try {
                java.io.File f = new java.io.File(
                        System.getProperty("user.home"), "vorkath-errors.log");
                try (java.io.PrintWriter pw = new java.io.PrintWriter(
                        new java.io.FileWriter(f, true))) {
                    pw.println("---- " + new java.util.Date() + " [handler] ----");
                    ex.printStackTrace(pw);
                }
            } catch (Throwable ignore) { /* file sink best-effort */ }
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        // NPC / Player accessors here can throw during entity teardown
        // (composition mid-swap on NPC transform, actor cleanup on despawn) —
        // wrap the whole body so a mid-teardown exception lands in our file
        // sink instead of RuneLite's EventBus internal catch. Prime NPE
        // candidate for the invisible post-acid crashes.
        try {
            // Player branch — teleport-out detection.
            if (event.getActor() instanceof Player) {
                Player p = (Player) event.getActor();
                if (p == client.getLocalPlayer()) {
                    int anim = p.getAnimation();
                    // dueling ring
                    if (anim == 4069) {
                        VorkathAutoMain.isTeleportingPOH = true;
                    } else if (anim == 8288 || anim == 9471) {
                        main.setLastAttackTick(client.getTickCount());
                    }
                }
                return;
            }

            // NPC branch — Vorkath death detection. Animation 7949 fires the tick he dies,
            // ahead of the transform / despawn / kill-count chat message. Turn off mage prayer
            // and piety here.
            if (event.getActor() instanceof NPC) {
                NPC npc = (NPC) event.getActor();
                int id;
                int anim;
                try { id = npc.getId(); anim = npc.getAnimation(); }
                catch (Throwable t) { return; }   // NPC mid-teardown, skip
                if ((id == 8061 || id == 8059) && anim == 7949) {
                    main.onVorkathDeath();
                }
                // TODO: attack the spawn (8063) on animation ID 7889.
                // Zombified spawn (8063) death animation — fires BEFORE onNpcDespawned.
                // Attack Vorkath immediately so we don't wait for the despawn tick.
                if (id == 8063 && anim == 7891) {
                    // Spawn dead — clear the spawn-phase gates and mark prayers
                    // to come back on. DO NOT attackVorkathNow() here: Vorkath
                    // is still invulnerable until projectile 146 fires. The 146
                    // rising-edge handler below does the actual re-attack at the
                    // exact tick Vorkath becomes hittable (same tick prayers
                    // toggle back).
                    main.zombifiedSpawnAlive = false;
                    main.inSpawnPhase = false;
                    main.setMagePrayOn(true);
                    main.setPietyOn(true);
                }
            }
        } catch (Throwable ex) {
            VorkathAutoMain.logCaught("Plugin.onAnimationChanged", ex);
        }
    }


    private String stripTargetAnchors(String text) {
        Matcher m = Pattern.compile("ff>(.*?)</c").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private void toggleStatus() {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        if (chatboxInput == null) return;

        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if (chatBoxMessage.equals("1") && (main == null || !main.isRunning) && !hasStarted) {
            startMain();
            hasStarted = true;
        } else if (chatBoxMessage.equals("2") && main != null && main.isRunning && hasStarted) {
            stopMain();
            hasStarted = false;
        }
    }

    public boolean getIsBankOpen() {
        return isBankOpen;
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        int vid = event.getVarbitId();
        int vpId = event.getVarpId();
        int val = event.getValue();

        if (vid == Varbits.SUPER_ANTIFIRE) {
            if (val > 0) main.syncSuperAntifireActive(); else main.syncSuperAntifireExpired();
        } else if (vid == Varbits.ANTIFIRE) {
            if (val > 0) main.syncAntifireActive(); else main.syncAntifireExpired();
        }
        // Anti-venom+ / Zryk's / Sanfew-serum grant venom immunity, which shows up on
        // VarPlayer.POISON as a value <= -38. Values between -38 and 0 are plain antipoison
        // (poison immunity but NOT venom immunity), so those do NOT count here.
        if (vpId == VarPlayer.POISON) {
            if (val <= -38) main.syncAntivenomActive();
            else main.syncAntivenomExpired();
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN && main != null && main.isRunning && hasStarted)
        {
            stopMain();
            hasStarted = false;
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        VorkathAutoObjectIDs.assignObjects(client, event);
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        VorkathAutoObjectIDs.assignObjects(client, event);
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();
        if (npc == null) return;
        try {
            int spawnId = npc.getId();
            if (spawnId == 8058) {
                // Client-thread event → safe to set the worker-visible mirror.
                main.mirrorHasWakeupNpc = true;
            }
            if (spawnId == 8063) {
                main.zombifiedSpawnAlive = true;
                // Step 4 + 5 of the spawn-phase sequence: kick off the attack the
                // moment 8063 appears. clickOnZombifiedSpawn() internally calls
                // equipSlayerStaff() first (no-op if already worn) then queues the
                // attack menuAction on the same tick.
                main.clickOnZombifiedSpawn();
            }
            VorkathAutoNPCIDs.assignNPCs(client, event);
        } catch (Throwable t) {
            // NPC mid-teardown — skip
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();
        if (npc == null) return;
        int id;
        int idx;
        try {
            id = npc.getId();
            idx = npc.getIndex();
        } catch (Throwable t) {
            return;   // NPC mid-teardown — safe to skip; other handlers cover cleanup
        }
        // Death detection for the zombified spawn (8063). Fires for BOTH paths:
        //   1) Player killed it (attack-kill) — hitsplat handler already restored prayers,
        //      but re-restoring here is idempotent.
        //   2) Spawn contact-exploded on player — no hitsplat landed on the spawn, so the
        //      hitsplat handler never fired. This is the only place prayers get restored.
        if (id == 8058) {
            // Client-thread event; safe to clear the worker-visible mirror.
            main.mirrorHasWakeupNpc = false;
        }
        if (id == 8063) {
            boolean killedByAttack = spawnHitsplatFired.remove(idx);
            main.zombifiedSpawnAlive = false;
            main.inSpawnPhase = false;   // redundant with 7891 handler; belt+suspenders
            main.setMagePrayOn(true);
            main.setPietyOn(true);
            main.attackVorkathNow();   // resume the fight — no auto-attack lock after the spawn phase
        }
    }

    @Subscribe
    public void onNpcChanged(NpcChanged event) {
        NPC now = event.getNpc();
        NPCComposition old = event.getOld();
        if (now == null || old == null) return;
        try {
            if (now.getId() == 8061 && old.getId() != 8061) {
                main.onVorkathSpawn();
            }
        } catch (Throwable t) {
            // composition mid-teardown — skip
        }
    }

    private boolean isEquipmentOpen() {
        Widget menu = client.getWidget(WidgetInfo.EQUIPMENT_INVENTORY_ITEMS_CONTAINER);
        return (menu != null && !menu.isHidden());
    }

    private boolean isLunarBookOpen() {
        Widget chatModal = client.getWidget(14286848);
        if (chatModal != null) {
            return !chatModal.isHidden();
        }
        return false;
    }

    public void getShapeForTileObject() {
        if (VorkathAutoMain.pendingClickboxObject == null)
            return;

        try
        {
            Shape s = VorkathAutoMain.pendingClickboxObject.getClickbox();
            if (s != null)
            {
                VorkathAutoMain.pendingClickboxShape = s;
            }
        }
        catch (Exception ignored)
        {
            // Client thread guarantees safety, but be defensive
        }
        finally
        {
            VorkathAutoMain.pendingClickboxObject = null;
        }
    }

    private boolean isInventoryHidden() {
        Widget inv = client.getWidget(9764864);
        return inv != null && inv.isHidden();
    }

    private void getClickboxes() {
        BloodRuneTrueMain.clickboxCache.clear();

        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        int plane = client.getPlane();

        for (int x = 0; x < 104; x++)
        {
            for (int y = 0; y < 104; y++)
            {
                Tile tile = tiles[plane][x][y];
                if (tile == null) continue;

                // GameObjects
                for (GameObject obj : tile.getGameObjects())
                {
                    if (obj != null && TRACKED_IDS.contains(obj.getId()))
                        tryStoreClickbox(obj);
                }

                // WallObjects
                WallObject w = tile.getWallObject();
                if (w != null && TRACKED_IDS.contains(w.getId()))
                    tryStoreClickbox(w);

                // DecorativeObjects
                DecorativeObject d = tile.getDecorativeObject();
                if (d != null && TRACKED_IDS.contains(d.getId()))
                    tryStoreClickbox(d);

                // GroundObjects
                GroundObject g = tile.getGroundObject();
                if (g != null && TRACKED_IDS.contains(g.getId()))
                    tryStoreClickbox(g);
            }
        }
    }

    private void tryStoreClickbox(TileObject obj) {
        Shape s = obj.getClickbox();
        if (s != null)
        {
            BloodRuneTrueMain.clickboxCache.put(obj, s);
        }
    }

    private int[] destination() {
        LocalPoint localDestination = client.getLocalDestinationLocation();
        if(localDestination == null) {
            return new int[] {-1, -1};
        }

        WorldPoint worldDestination = WorldPoint.fromLocal(client, localDestination);
        return new int[] {worldDestination.getX(), worldDestination.getY()};
    }

    private boolean isDestinationTile(WorldPoint wp) {
        int[] dest = destination();
        return dest[0] == wp.getX() && dest[1] == wp.getY();
    }

    private boolean isAtWorldPoint(WorldPoint worldPoint) {
        if (worldPoint == null) return false;
        Player p = client.getLocalPlayer();
        if (p == null) return false;   // null during climb / login / instance transition
        WorldPoint wp = p.getWorldLocation();
        if (wp == null) return false;
        return wp.getX() == worldPoint.getX()
            && wp.getY() == worldPoint.getY()
            && wp.getPlane() == worldPoint.getPlane();
    }

    private void setCameraZoom(int zoom) {
        clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom));
    }

    private void resetZoomPitchYaw(int zoom, int pitch, int yaw) {
        setZoomPitchYaw(zoom, pitch, yaw);
        client.setCameraMode(0);
    }

    private void setZoomPitchYaw(int zoom, int pitch, int yaw) {
        setCameraZoom(zoom);
        setCameraPitch(pitch);
        setCameraYaw(yaw);
    }

    private void setCameraYaw(int yaw) {
        if(client.getCameraYaw() == yaw)
            return;
        client.setCameraYawTarget(yaw);
//        north: 0
//        east:1536
//        south:1024
//        west:4160
    }

    private void setCameraPitch(int pitch) {
        if(client.getCameraPitch() == pitch)
            return;
        client.setCameraPitchTarget(pitch);
    }
}
