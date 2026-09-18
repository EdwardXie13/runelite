package net.runelite.client.plugins.gemstoneCrabAuto;

import lombok.experimental.UtilityClass;
import net.runelite.api.*;
import net.runelite.api.events.*;

/**
 * NPC-ID constants + cached NPC reference for the Gemstone Crab bot.
 *
 * Only one NPC is tracked. All accessors are guarded against NPC teardown
 * races (getId can throw when an NPC is mid-composition-swap).
 */
@UtilityClass
public class GemstoneCrabAutoNPCIDs {
    public final int GEMSTONE_CRAB = 14779;

    public static NPC gemstoneCrab = null;

    /**
     * Populate the NPC cache from the currently-loaded scene. Call on plugin
     * startup so we don't miss a crab that spawned before we subscribed.
     * Safe to call repeatedly.
     */
    public static void scanScene(Client client) {
        for (NPC npc : client.getNpcs()) {
            if (npc == null) continue;
            int id;
            try { id = npc.getId(); } catch (Throwable t) { continue; }
            if (id == GEMSTONE_CRAB) {
                gemstoneCrab = npc;
            }
        }
        System.out.println("[gemstoneCrabAuto.scanScene] gemstoneCrab=" + gemstoneCrab);
    }

    public static void assignNPCs(Client client, NpcSpawned event) {
        NPC npc = event.getNpc();
        if (npc == null) return;
        int id;
        try { id = npc.getId(); } catch (Throwable t) { return; }
        if (id == GEMSTONE_CRAB) {
            gemstoneCrab = npc;
        }
    }

    public static void assignNPCs(Client client, NpcDespawned event) {
        NPC npc = event.getNpc();
        if (npc == null) return;
        int id;
        try { id = npc.getId(); } catch (Throwable t) { return; }
        if (id == GEMSTONE_CRAB) {
            gemstoneCrab = null;
        }
    }

    public static void setAllVarsNull() {
        gemstoneCrab = null;
    }
}
