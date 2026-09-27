package net.runelite.client.plugins.gemstoneCrabAuto;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.plusUtils.StepOverlay;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

/**
 * Gemstone Crab automation plugin.
 *
 * @Subscribe handlers on the client thread drive transitions; the worker
 * loop in GemstoneCrabAutoMain re-attacks any tick the crab is alive and
 * we're not currently interacting with it.
 *
 * Chat toggle: type "1" in chatbox to start, "2" to stop.
 */
@PluginDescriptor(name = "GemstoneCrabAuto", enabledByDefault = false)
@Slf4j
public class GemstoneCrabAutoPlugin extends Plugin {
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private OverlayManager overlayManager;
    @Inject private StepOverlay overlay;
    @Inject private EventBus eventBus;
    @Inject private ItemManager itemManager;

    @Getter
    private volatile WorldPoint localPlayerLocation;

    GemstoneCrabAutoMain main;
    private Thread mainThread;
    private boolean hasStarted = false;

    // ---- Animation IDs ----------------------------------------------------
    private static final int CRAB_ANIM_DEATH        = 12482;   // crab death → click cave
    private static final int CRAB_ANIM_ATTACK       = 12481;   // crab active/attacking → re-attack
    private static final int PLAYER_ANIM_CAVE_TRAVEL = 11580;   // player: cave travel started (latch)

    // ---- Lifecycle --------------------------------------------------------
    @Override
    protected void startUp() throws Exception {
        Thread.setDefaultUncaughtExceptionHandler((t, ex) ->
            GemstoneCrabAutoMain.logCaught("uncaught thread=" + t.getName(), ex));
        try {
            java.io.File f = new java.io.File(
                    System.getProperty("user.home"), "gemstone-crab-errors.log");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.FileWriter(f, true))) {
                pw.println("==== " + new java.util.Date()
                        + " [plugin startUp] path=" + f.getAbsolutePath() + " ====");
            }
            log.info("[gemstoneCrabAuto] error log at: {}", f.getAbsolutePath());
        } catch (Throwable t) {
            log.error("[gemstoneCrabAuto] failed to write startup marker", t);
        }
        GemstoneCrabAutoNPCIDs.setAllVarsNull();
        GemstoneCrabAutoObjectIDs.setAllVarsNull();
        overlayManager.add(overlay);
        if (main == null) {
            main = new GemstoneCrabAutoMain(client, clientThread, overlay, this,
                    eventBus, itemManager);
            System.out.println("initialized gemstoneCrabAuto");
        }
    }

    @Override
    protected void shutDown() throws Exception {
        stopMain();
        GemstoneCrabAutoNPCIDs.setAllVarsNull();
        GemstoneCrabAutoObjectIDs.setAllVarsNull();
        overlayManager.remove(overlay);
    }

    private void startMain() {
        System.out.println("[gc.startMain] ENTER threadAlive="
            + (mainThread != null && mainThread.isAlive())
            + " main==null?" + (main == null));
        if (mainThread != null && mainThread.isAlive()) {
            System.out.println("[gc.startMain] BAIL — thread already alive");
            return;
        }
        try {
            if (main == null) {
                main = new GemstoneCrabAutoMain(client, clientThread, overlay, this,
                        eventBus, itemManager);
                System.out.println("[gc.startMain] constructed new GemstoneCrabAutoMain");
            }
            main.reset();
            clientThread.invoke(() -> GemstoneCrabAutoNPCIDs.scanScene(client));
            main.isRunning = true;
            mainThread = new Thread(main, "gemstoneCrabAuto-worker");
            mainThread.setDaemon(true);
            mainThread.start();
            overlay.setCurrentStep("status is go");
            System.out.println("gemstoneCrabAuto status is go");
        } catch (Throwable ex) {
            System.out.println("[gc.startMain] EXCEPTION: " + ex);
            ex.printStackTrace(System.out);
        }
    }

    private void stopMain() {
        if (main != null) main.stop();
        if (mainThread != null) {
            mainThread.interrupt();
            try { mainThread.join(2000); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            mainThread = null;
        }
        overlay.setCurrentStep("status is stop");
        System.out.println("gemstoneCrabAuto status is stop");
    }

    private void toggleStatus() {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_MESSAGE_LINES);
        if (chatboxInput == null) return;
        String txt = chatboxInput.getText();
        if (txt == null) return;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("ff>(.*?)</c").matcher(txt);
        String msg = m.find() ? m.group(1) : "";
        if ("1".equals(msg) || "2".equals(msg)) {
            System.out.println("[gc.toggleStatus] msg=" + msg + " raw=" + txt
                + " main==null?" + (main == null)
                + " isRunning=" + (main == null ? "n/a" : String.valueOf(main.isRunning))
                + " hasStarted=" + hasStarted);
        }
        if (msg.equals("1") && (main == null || !main.isRunning) && !hasStarted) {
            System.out.println("[gc.toggleStatus] START gate passed — calling startMain()");
            startMain();
            hasStarted = true;
        } else if (msg.equals("2") && main != null && main.isRunning && hasStarted) {
            System.out.println("[gc.toggleStatus] STOP gate passed — calling stopMain()");
            stopMain();
            hasStarted = false;
        }
    }

    // ---- @Subscribe hooks --------------------------------------------------
    @Subscribe
    public void onClientTick(ClientTick event) {
        try {
            if (main != null) main.updateMirroredGameState();
            toggleStatus();
            Player p = client.getLocalPlayer();
            localPlayerLocation = p != null ? p.getWorldLocation() : null;
        } catch (Throwable ex) {
            GemstoneCrabAutoMain.logCaught("Plugin.onClientTick", ex);
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();
        if (npc == null) return;
        try {
            int id;
            try { id = npc.getId(); } catch (Throwable t) { return; }
            GemstoneCrabAutoNPCIDs.assignNPCs(client, event);
            if (main != null && main.isRunning && id == GemstoneCrabAutoNPCIDs.GEMSTONE_CRAB) {
                main.crabAlive = true;
                // Fresh crab in scene — reset the post-death latches so
                // a new fight starts from clean state.
                main.crabDied = false;
                main.travelingCave = false;
                main.caveClickAllowedAtMs = 0L;
                // Reset per-travel pre-walk state so a same-arrival-tile
                // second travel doesn't inherit a stale (already-elapsed)
                // preWalkFireTick and fire without the intended delay.
                main.preWalkArrival = null;
                main.preWalkFireTick = -1;
                // Attack immediately — shaves one tick off vs the worker.
                main.clickOnCrab();
            }
        } catch (Throwable ex) {
            GemstoneCrabAutoMain.logCaught("Plugin.onNpcSpawned", ex);
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();
        if (npc == null) return;
        try {
            int id;
            try { id = npc.getId(); } catch (Throwable t) { return; }
            GemstoneCrabAutoNPCIDs.assignNPCs(client, event);
            if (id == GemstoneCrabAutoNPCIDs.GEMSTONE_CRAB && main != null) {
                main.crabAlive = false;
            }
        } catch (Throwable ex) {
            GemstoneCrabAutoMain.logCaught("Plugin.onNpcDespawned", ex);
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        try {
            if (main == null || !main.isRunning) return;

            // Player branch — cave-travel latch. Anim 11580 fires as we
            // begin the cave transition; once seen, worker stops retrying
            // clickOnCave (same shape as VorkathAuto's boardedBoat latch
            // for the Torfinn boat).
            if (event.getActor() instanceof Player) {
                if (event.getActor() != client.getLocalPlayer()) return;
                int panim;
                try { panim = ((Player) event.getActor()).getAnimation(); }
                catch (Throwable t) { return; }
                // Diagnostic: every local-player anim change shows up here.
                // Use it to confirm the actual cave-travel animation id.
                if (panim != -1) {
                    System.out.println("[gemstoneCrabAuto] localPlayer anim=" + panim
                        + " travelingCave(before)=" + main.travelingCave
                        + " crabDied=" + main.crabDied);
                }
                if (panim == PLAYER_ANIM_CAVE_TRAVEL) {
                    main.travelingCave = true;
                    main.crabDied = false;
                    System.out.println("[gemstoneCrabAuto] travelingCave LATCHED (anim 11580)");
                }
                return;
            }

            if (!(event.getActor() instanceof NPC)) return;
            NPC npc = (NPC) event.getActor();
            int id, anim;
            try { id = npc.getId(); anim = npc.getAnimation(); }
            catch (Throwable t) { return; }   // NPC mid-teardown
            if (id != GemstoneCrabAutoNPCIDs.GEMSTONE_CRAB) return;

            if (anim == CRAB_ANIM_DEATH) {
                // Crab dead — latch crabDied and schedule the FIRST cave
                // click for 3-5s from now. Worker's crabDied retry branch
                // idles until then and takes over afterward. Human-ish
                // pause before we click the cave; also gives the death
                // animation time to finish visually.
                main.crabAlive = false;
                main.crabDied = true;
                main.travelingCave = false;
                long delayMs = java.util.concurrent.ThreadLocalRandom.current()
                        .nextLong(3000, 60001);
                main.caveClickAllowedAtMs = System.currentTimeMillis() + delayMs;
                System.out.println("[gemstoneCrabAuto] crab died — cave click in "
                        + delayMs + "ms");
            } else if (anim == CRAB_ANIM_ATTACK) {
                // Crab is engaging / attacking — make sure we're on it.
                main.crabAlive = true;
                main.clickOnCrab();
            }
        } catch (Throwable ex) {
            GemstoneCrabAutoMain.logCaught("Plugin.onAnimationChanged", ex);
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        try {
            GemstoneCrabAutoObjectIDs.assignObjects(client, event);
        } catch (Throwable ex) {
            GemstoneCrabAutoMain.logCaught("Plugin.onGameObjectSpawned", ex);
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        try {
            GemstoneCrabAutoObjectIDs.assignObjects(client, event);
        } catch (Throwable ex) {
            GemstoneCrabAutoMain.logCaught("Plugin.onGameObjectDespawned", ex);
        }
    }
}
