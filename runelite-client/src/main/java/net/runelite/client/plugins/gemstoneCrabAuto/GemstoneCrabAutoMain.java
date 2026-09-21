package net.runelite.client.plugins.gemstoneCrabAuto;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.plusUtils.Clicker;
import net.runelite.client.plugins.plusUtils.StepOverlay;

import java.awt.Canvas;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

/**
 * Worker + orchestration for the Gemstone Crab bot.
 *
 * Fight loop:
 *   - When the player is in one of GEMSTONE_CRAB_REGIONS and the crab is
 *     alive but we aren't currently attacking it, the worker fires
 *     clickOnCrab() every tick — same pattern as VorkathAuto's re-attack.
 *   - Crab animation 12481 (Plugin) is a fresh "attack now" trigger.
 *   - Crab animation 12482 (death, Plugin) fires clickOnCave() into
 *     GameObject 57631 to continue the loop.
 *
 * No prayer / potions / canContinue — only the master isRunning gate.
 */
@lombok.extern.slf4j.Slf4j
public class GemstoneCrabAutoMain implements Runnable {

    /** Shared error sink — slf4j + append to ~/gemstone-crab-errors.log. */
    public static void logCaught(String context, Throwable ex) {
        try { log.error("[gemstoneCrabAuto {}]", context, ex); } catch (Throwable ignore) {}
        try {
            java.io.File f = new java.io.File(
                    System.getProperty("user.home"), "gemstone-crab-errors.log");
            try (java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.FileWriter(f, true))) {
                pw.println("---- " + new java.util.Date() + " [" + context + "] ----");
                ex.printStackTrace(pw);
            }
        } catch (Throwable ignore) { /* best-effort */ }
    }

    // ---- Injected services -------------------------------------------------
    private final Client client;
    private final ClientThread clientThread;
    private final StepOverlay overlay;
    private final GemstoneCrabAutoPlugin plugin;
    private final EventBus eventBus;
    private final ItemManager itemManager;

    Clicker clicker;

    // ---- Master gates ------------------------------------------------------
    public volatile boolean isRunning = false;

    // ---- Tile-click retry infrastructure ---------------------------------
    // Copy of VorkathAutoMain's clickTileWithRetry executor+generation setup.
    // Runs the retry loop off the client thread so Thread.sleep(100) doesn't
    // freeze the game; bumping clickGeneration cancels any in-flight retry
    // when a new clickTileWithRetry call arrives for a different target.
    private final java.util.concurrent.ExecutorService clickExecutor =
        java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "gemstoneCrabAuto-clicker");
            t.setDaemon(true);
            return t;
        });
    private volatile int clickGeneration = 0;
    private static final int SPAM_MAX_ATTEMPTS = 2;    // dispatches per wave — one primary, one safety
    private static final long SPAM_DEADLINE_MS = 500;  // hard timeout per wave
    /** True while a live crab NPC is in the scene. Set/cleared by Plugin on
     *  NpcSpawned/NpcDespawned + animation 12482 (death). */
    public volatile boolean crabAlive = false;
    /** True from the crab death anim (12482) until a fresh crab spawns.
     *  Gates repeated clickOnCave attempts in the worker loop so we keep
     *  re-firing until travel is confirmed (or a new crab shows up). */
    public volatile boolean crabDied = false;
    /** True from player anim 11580 (cave travel initiated) until a fresh
     *  crab spawns. Parallel to VorkathAuto's boardedBoat chat-msg latch —
     *  once set, worker stops retrying the cave click. */
    public volatile boolean travelingCave = false;
    /** Earliest wall-clock time (System.currentTimeMillis) at which the
     *  worker is allowed to fire clickOnCave. Plugin sets this to (now +
     *  random 3-5s) on crab death anim so we have a human-ish pause before
     *  the first cave click; the crabDied retry branch waits until now
     *  crosses this timestamp. Reset to 0 on fresh crab spawn. */
    public volatile long caveClickAllowedAtMs = 0L;
    /** Currently-tracked arrival (post-cave). Null until we detect the
     *  player standing on one of the three ARRIVAL tiles while
     *  travelingCave is true. Cleared when the player moves off the
     *  arrival tile or on next crab spawn. */
    public volatile GemstoneCrabAutoWorldPoints.Arrival preWalkArrival = null;
    /** Game tick after which the pre-walk click is allowed to fire.
     *  Set to (currentTick + PRE_WALK_DELAY_TICKS) when preWalkArrival is
     *  first captured. */
    public volatile int preWalkFireTick = -1;

    // ---- Client-state mirrors — refreshed on the client thread. -----------
    private volatile int mirrorCurrentHP = 0;
    private volatile int mirrorMaxHP     = 0;
    private volatile boolean mirrorRunEnabled = false;
    /** True when the local player's `getInteracting()` target IS the gemstone
     *  crab NPC. Used as the "am I already attacking?" gate. */
    private volatile boolean mirrorPlayerAttackingCrab = false;
    /** Local player's template-frame WorldPoint, refreshed on the client
     *  thread by updateMirroredGameState. Worker thread reads this instead
     *  of touching getLocalPlayer().getWorldLocation() directly, which
     *  asserts client-thread-only. */
    private volatile WorldPoint mirrorPlayerTemplateLoc = null;

    /** Anti-spam: worker fires every ~600ms but we don't want two clicks in
     *  the same game tick (~600ms). 2-tick throttle same as Vorkath's. */
    private volatile int lastCrabClickTick = -100;
    private volatile int lastCaveClickTick = -100;

    // ---- Ctor --------------------------------------------------------------
    public GemstoneCrabAutoMain(Client client, ClientThread clientThread,
                                StepOverlay overlay, GemstoneCrabAutoPlugin plugin,
                                EventBus eventBus, ItemManager itemManager) {
        this.client = client;
        this.clientThread = clientThread;
        this.overlay = overlay;
        this.plugin = plugin;
        this.eventBus = eventBus;
        this.itemManager = itemManager;
        this.clicker = new Clicker(client);
    }

    public void reset() {
        crabAlive = false;
        crabDied = false;
        travelingCave = false;
        caveClickAllowedAtMs = 0L;
        preWalkArrival = null;
        preWalkFireTick = -1;
        mirrorPlayerAttackingCrab = false;
    }

    public void stop() { isRunning = false; }

    // ---- Client-thread mirror refresh --------------------------------------
    public void updateMirroredGameState() {
        try {
            mirrorCurrentHP  = client.getBoostedSkillLevel(Skill.HITPOINTS);
            mirrorMaxHP      = client.getRealSkillLevel(Skill.HITPOINTS);
            mirrorRunEnabled = client.getVarpValue(173) == 1;

            Player p = client.getLocalPlayer();
            if (p == null) {
                mirrorPlayerAttackingCrab = false;
                mirrorPlayerTemplateLoc = null;
                return;
            }
            try {
                Actor target = p.getInteracting();
                mirrorPlayerAttackingCrab = (target instanceof NPC)
                        && ((NPC) target).getId() == GemstoneCrabAutoNPCIDs.GEMSTONE_CRAB;
            } catch (Throwable t) {
                mirrorPlayerAttackingCrab = false;   // target mid-teardown
            }
            // Player location — refresh the worker-visible template-frame
            // mirror. Both getWorldLocation and toTemplate (via fromWorld /
            // fromLocalInstance) assert client-thread-only.
            try {
                WorldPoint raw = p.getWorldLocation();
                mirrorPlayerTemplateLoc = toTemplate(raw);
            } catch (Throwable t) {
                mirrorPlayerTemplateLoc = null;
            }
        } catch (Throwable t) {
            logCaught("updateMirroredGameState", t);
        }
    }

    // ---- Region helper -----------------------------------------------------
    /** True iff any current player-region equals one of the crab regions. */
    public boolean isInGemstoneCrabRegion() {
        try {
            int[] regions = client.getMapRegions();
            if (regions == null) return false;
            for (int r : regions) {
                for (int wanted : GemstoneCrabAutoWorldPoints.GEMSTONE_CRAB_REGIONS) {
                    if (r == wanted) return true;
                }
            }
        } catch (Throwable t) { /* skip */ }
        return false;
    }

    // ---- Worker loop -------------------------------------------------------
    @Override
    public void run() {
        try {
            while (isRunning) {
                try { tickOnce(); }
                catch (Throwable ex) { logCaught("worker tick", ex); }
                clicker.delay(600);
            }
        } catch (Throwable outer) {
            logCaught("worker outer", outer);
        }
    }

    /**
     * Per-tick decision:
     *   - Not in a crab region → idle, do nothing.
     *   - In region, crab alive, not attacking it → clickOnCrab().
     *   - Otherwise (attacking already, or crab dead) → idle; the animation
     *     handlers in Plugin drive the transitions (12481 re-attack,
     *     12482 death → clickOnCave).
     */
    private void tickOnce() {
        if (!isInGemstoneCrabRegion()) {
            overlay.setCurrentStep("not in crab region — idle");
            return;
        }
        if (crabAlive && !isPlayerAttackingCrab()) {
            overlay.setCurrentStep("crab alive, not attacking — click");
            clickOnCrab();
            return;
        }
        if (crabDied && !travelingCave
                && GemstoneCrabAutoObjectIDs.gemstoneCrabCave != null) {
            if (System.currentTimeMillis() < caveClickAllowedAtMs) {
                long waitMs = caveClickAllowedAtMs - System.currentTimeMillis();
                overlay.setCurrentStep("crab dead, cave-click delay " + waitMs + "ms");
                return;
            }
            overlay.setCurrentStep("crab dead, retry cave click");
            clickOnCave();
            return;
        }

        // Post-cave-travel pre-walk. Only fires while travelingCave is true
        // — the pre-entry tiles share coords with these arrival tiles, so
        // the latch is the only reliable side-of-cave signal. If the click
        // misses, the player stays on the arrival tile and the next
        // iteration re-fires; when the player moves off we clear state.
        if (travelingCave) {
            WorldPoint at = mirrorPlayerTemplateLoc;   // worker-safe mirror
            GemstoneCrabAutoWorldPoints.Arrival ar =
                    (at == null) ? null : GemstoneCrabAutoWorldPoints.matchArrival(at);
            System.out.println("[gemstoneCrabAuto.preWalk] traveling=true template=" + at
                + " matched=" + (ar == null ? "null" : ar.arrival)
                + " tracked=" + (preWalkArrival == null ? "null" : preWalkArrival.arrival)
                + " fireTick=" + preWalkFireTick + " nowTick=" + client.getTickCount());
            if (at == null) return;
            if (ar == null) {
                preWalkArrival = null;
                preWalkFireTick = -1;
                return;
            }
            int nowTick = client.getTickCount();
            if (preWalkArrival != ar) {
                preWalkArrival = ar;
                preWalkFireTick = nowTick + GemstoneCrabAutoWorldPoints.PRE_WALK_DELAY_TICKS;
                overlay.setCurrentStep("arrived at " + at
                        + " — pre-walk in " + GemstoneCrabAutoWorldPoints.PRE_WALK_DELAY_TICKS + " ticks");
                System.out.println("[gemstoneCrabAuto.preWalk] scheduled fireTick=" + preWalkFireTick);
                return;
            }
            if (nowTick >= preWalkFireTick) {
                overlay.setCurrentStep("pre-walk -> " + ar.preWalk);
                System.out.println("[gemstoneCrabAuto.preWalk] FIRE clickTile(" + ar.preWalk + ")");
                clickTile(ar.preWalk);
            }
        }
    }

    // ---- Interaction primitives — copied from VorkathAutoMain shape -------
    public void interactNpc(NPC target, MenuAction action, String option, String name) {
        if (target == null) return;
        clientThread.invoke(() -> {
            try {
                int idx = target.getIndex();
                String tName = (name != null) ? name : "<col=ffff>" + target.getName();
                client.menuAction(0, 0, action, idx, -1, option, tName);
            } catch (Throwable ex) {
                // NPC mid-teardown or menuAction rejected — drop silently
            }
        });
    }

    public void interactObject(GameObject target, MenuAction action, String option) {
        if (target == null) return;
        clientThread.invoke(() -> {
            try {
                ObjectComposition comp = client.getObjectDefinition(target.getId());
                String name = comp == null ? "" : comp.getName();
                net.runelite.api.Point sceneMin = target.getSceneMinLocation();
                if (sceneMin == null) return;
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
                // Object mid-teardown or menuAction rejected — drop silently
            }
        });
    }

    // ---- Public click entry points ----------------------------------------
    /** Attack the gemstone crab. 2-tick throttle to prevent worker-loop
     *  spam within a single game tick. No-op when we're already attacking
     *  the crab. */
    public void clickOnCrab() {
        if (isPlayerAttackingCrab()) return;
        int tick = client.getTickCount();
        if (tick - lastCrabClickTick < 2) return;
        lastCrabClickTick = tick;
        NPC crab = findNpc(GemstoneCrabAutoNPCIDs.GEMSTONE_CRAB);
        if (crab == null) return;
        // Target string matches the observed in-game menuAction verbatim:
        //   '<col=ffff00>Gemstone Crab<col=ff0000>  (level-160)'
        interactNpc(crab, MenuAction.NPC_SECOND_OPTION, "Attack",
                "<col=ffff00>Gemstone Crab<col=ff0000>  (level-160)");
    }

    /** Click the crab cave GameObject (57631). Fires on death anim (12482). */
    public void clickOnCave() {
        int tick = client.getTickCount();
        if (tick - lastCaveClickTick < 2) return;
        lastCaveClickTick = tick;
        GameObject cave = GemstoneCrabAutoObjectIDs.gemstoneCrabCave;
        if (cave == null) return;
        // Observed in-game menuAction option is 'Crawl-through', target
        // '<col=ffff>Cave' (interactObject synthesizes the '<col=ffff>' +
        // object-name target already, so no target override needed).
        interactObject(cave, MenuAction.GAME_OBJECT_FIRST_OPTION, "Crawl-through");
    }

    // ---- Accessors ---------------------------------------------------------
    public boolean isPlayerAttackingCrab() { return mirrorPlayerAttackingCrab; }

    // ---- Small helpers copied from VorkathAutoMain shape ------------------
    public LocalPoint fromTemplate(WorldPoint template) {
        if (template == null) return null;
        for (WorldPoint instanced : WorldPoint.toLocalInstance(client, template)) {
            LocalPoint lp = LocalPoint.fromWorld(client, instanced);
            if (lp != null) return lp;
        }
        return null;
    }

    public WorldPoint toTemplate(WorldPoint instanced) {
        if (instanced == null) return null;
        LocalPoint lp = LocalPoint.fromWorld(client, instanced);
        if (lp == null) return null;
        return WorldPoint.fromLocalInstance(client, lp);
    }

    public NPC findNpc(int id) {
        for (NPC npc : client.getNpcs()) {
            if (npc == null) continue;
            try {
                if (npc.getId() == id) return npc;
            } catch (Throwable t) { /* mid-teardown, skip */ }
        }
        return null;
    }
    public boolean hasNpc(int id) { return findNpc(id) != null; }

    // ---- Tile click: executor-backed retry copy of Vorkath's clickTileWithRetry ----
    /**
     * Walk to a template WorldPoint. Projection + viewport guard run on the
     * caller's thread, then the actual click is submitted to clickExecutor,
     * which fires up to SPAM_MAX_ATTEMPTS Canvas MouseEvent bursts spaced by
     * ~100ms, bailing as soon as getLocalDestinationLocation shows the walk
     * destination has matched the target. Thread.sleep(100) lives on the
     * executor thread — NOT the client thread — so it does not freeze the
     * game. Same shape as VorkathAutoMain.clickTileWithRetry.
     *
     * Public name kept as clickTile so existing callers (pre-walk branch)
     * need no edits.
     */
    public void clickTile(WorldPoint target) {
        LocalPoint lp = fromTemplate(target);
        if (lp == null) return;
        net.runelite.api.Point canvasPt =
                net.runelite.api.Perspective.localToCanvas(client, lp, client.getPlane());
        if (canvasPt == null) return;
        final int cx = canvasPt.getX();
        final int cy = canvasPt.getY();

        int vpX = client.getViewportXOffset();
        int vpY = client.getViewportYOffset();
        int vpW = client.getViewportWidth();
        int vpH = client.getViewportHeight();
        if (cx < vpX || cx >= vpX + vpW || cy < vpY || cy >= vpY + vpH) {
            System.out.println("[clickTile] " + target + " projects to (" + cx
                    + "," + cy + ") outside viewport [" + vpX + "," + vpY
                    + " .. " + (vpX + vpW) + "," + (vpY + vpH) + "] — skipping");
            return;
        }

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
                    try { Thread.sleep(100); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                }
            } catch (Throwable ex) {
                logCaught("clickTile executor", ex);
            }
        });
    }

    /** Worker-thread-safe walk-destination check. Returns false on any null /
     *  conversion failure — retry loop just fires another click. */
    private boolean destinationMatches(int targetX, int targetY) {
        try {
            LocalPoint dest = client.getLocalDestinationLocation();
            if (dest == null) return false;
            WorldPoint dt = WorldPoint.fromLocalInstance(client, dest);
            return dt != null && dt.getX() == targetX && dt.getY() == targetY;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Fire MOVED -> PRESSED -> RELEASED -> CLICKED at (cx,cy) by directly
     *  invoking the canvas's registered MouseListener / MouseMotionListeners
     *  (bypasses Component.processEvent -> requestFocus which would
     *  otherwise steal OS window focus). */
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
}
