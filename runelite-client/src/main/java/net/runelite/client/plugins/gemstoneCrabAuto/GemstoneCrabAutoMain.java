package net.runelite.client.plugins.gemstoneCrabAuto;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.plusUtils.Clicker;
import net.runelite.client.plugins.plusUtils.StepOverlay;

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

    // ---- Client-state mirrors — refreshed on the client thread. -----------
    private volatile int mirrorCurrentHP = 0;
    private volatile int mirrorMaxHP     = 0;
    private volatile boolean mirrorRunEnabled = false;
    /** True when the local player's `getInteracting()` target IS the gemstone
     *  crab NPC. Used as the "am I already attacking?" gate. */
    private volatile boolean mirrorPlayerAttackingCrab = false;

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
                return;
            }
            try {
                Actor target = p.getInteracting();
                mirrorPlayerAttackingCrab = (target instanceof NPC)
                        && ((NPC) target).getId() == GemstoneCrabAutoNPCIDs.GEMSTONE_CRAB;
            } catch (Throwable t) {
                mirrorPlayerAttackingCrab = false;   // target mid-teardown
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
}
