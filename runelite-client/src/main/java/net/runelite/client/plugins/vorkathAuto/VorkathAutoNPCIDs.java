package net.runelite.client.plugins.vorkathAuto;

import lombok.experimental.UtilityClass;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.NpcID;

@UtilityClass
public class VorkathAutoNPCIDs {
    public final int SIRSAL_BANKER = NpcID.LUNAR_MOONCLAN_MONK_MAN;
    public final int TORFINN_RELLEKA = NpcID.TORFINN_COLLECT_RELLEKKA;
    public final int TORFINN_UNGAEL = NpcID.TORFINN_COLLECT_UNGAEL;
    public final int VORKATH_SLEEPING = NpcID.VORKATH_SLEEPING;
    public final int VORKATH_WAKING_UP = NpcID.VORKATH_SLEEPING_NOOP;
    public final int VORKATH = NpcID.VORKATH;

    public static NPC sirsalBanker = null;
    public static NPC torfinn = null;
    public static NPC vorkathSleeping = null;
    public static NPC vorkathWakingUp = null;
    public static NPC vorkath = null;

    /**
     * Populate the NPC caches from the currently-loaded scene. Call on plugin startup
     * so we don't miss caches for NPCs that spawned before we subscribed to NpcSpawned.
     * Safe to call repeatedly.
     */
    public static void scanScene(Client client) {
        for (NPC npc : client.getNpcs()) {
            if (npc == null) continue;
            int id;
            try { id = npc.getId(); } catch (Throwable t) { continue; }
            switch (id) {
                case VORKATH_SLEEPING:   vorkathSleeping = npc; break;
                case VORKATH_WAKING_UP:  vorkathWakingUp = npc; break;
                case VORKATH:            vorkath = npc; break;
                case TORFINN_RELLEKA:
                case TORFINN_UNGAEL:     torfinn = npc; break;
                case SIRSAL_BANKER:
                    try {
                        WorldPoint wp = WorldPoint.fromLocalInstance(client, npc.getLocalLocation());
                        if (wp != null && wp.equals(VorkathAutoWorldPoints.SIRSAL_BANKER_TILE)) {
                            sirsalBanker = npc;
                        }
                    } catch (Throwable t) { /* skip */ }
                    break;
            }
        }
        System.out.println("[scanScene] vorkathSleeping=" + vorkathSleeping
                + " vorkathWakingUp=" + vorkathWakingUp
                + " vorkath=" + vorkath);
    }

    public static void assignNPCs(Client client, NpcSpawned event) {
        NPC npc = event.getNpc();
        if (npc == null) return;
        int id;
        try { id = npc.getId(); } catch (Throwable t) { return; }

        // WorldPoint conversion only needed for SIRSAL_BANKER's tile-equality check.
        // Don't gate the Vorkath NPC caches on it — during instance load fromLocalInstance
        // can return null while the template is still attaching, and we'd miss the spawn.
        switch (id) {
            case SIRSAL_BANKER:
                try {
                    WorldPoint npcWp = WorldPoint.fromLocalInstance(client, npc.getLocalLocation());
                    if (npcWp != null && npcWp.equals(VorkathAutoWorldPoints.SIRSAL_BANKER_TILE)) {
                        sirsalBanker = npc;
                    }
                } catch (Throwable t) { /* skip */ }
                break;
            case TORFINN_RELLEKA:
            case TORFINN_UNGAEL:
                torfinn = npc;
                break;
            case VORKATH_SLEEPING:
                vorkathSleeping = npc;
                break;
            case VORKATH_WAKING_UP:
                vorkathWakingUp = npc;
                break;
            case VORKATH:
                vorkath = npc;
                break;
        }
    }

    public static void assignNPCs(Client client, NpcDespawned event) {
        NPC npc = event.getNpc();
        if (npc == null) return;
        int id;
        try { id = npc.getId(); } catch (Throwable t) { return; }

        // localLocation can be null / throw on a despawning NPC. We still want to reach the
        // Vorkath cases (which null the cached ref regardless of location), so only bail on
        // location failure for the non-Vorkath cases below.
        WorldPoint npcWp = null;
        try { npcWp = WorldPoint.fromLocalInstance(client, npc.getLocalLocation()); }
        catch (Throwable t) { /* leave null */ }
        if (npcWp == null
                && id != VORKATH_SLEEPING
                && id != VORKATH_WAKING_UP
                && id != VORKATH) {
            return;
        }

        switch (id) {
            case SIRSAL_BANKER:
                if (npcWp.equals(VorkathAutoWorldPoints.SIRSAL_BANKER_TILE)) {
                    sirsalBanker = null;
                }
                break;
            case TORFINN_RELLEKA:
            case TORFINN_UNGAEL:
                torfinn = null;
                break;
            case VORKATH_SLEEPING:
                vorkathSleeping = null;   // FIX: was 'npc' — kept stale ref across teardown, caused NPEs
                break;
            case VORKATH_WAKING_UP:
                vorkathWakingUp = null;   // FIX: same bug
                break;
            case VORKATH:
                vorkath = null;           // FIX: same bug
                break;
        }
    }

//    public static void assignObjects(Client client, WallObjectSpawned event) {
//        WallObject obj = event.getWallObject();
//        int id = obj.getId();
//
//        WorldPoint objWp = WorldPoint.fromLocalInstance(client, obj.getLocalLocation());
//        if (objWp == null)
//            return;
//
//        switch (id) {
//            case CAVE_TO_MYSTERIOUS_RUINS:
//                if (objWp.equals(VorkathAutoWorldPoints.CAVE_TO_MYSTERIOUS_RUINS_TILE)) {
//                    caveToMysteriousRuins = obj;
//                }
//                break;
//        }
//    }
//
//    public static void assignObjects(Client client, WallObjectDespawned event) {
//        WallObject obj = event.getWallObject();
//        int id = obj.getId();
//
//        WorldPoint objWp = WorldPoint.fromLocalInstance(client, obj.getLocalLocation());
//        if (objWp == null)
//            return;
//
//        switch (id) {
//            case CAVE_TO_MYSTERIOUS_RUINS:
//                if (objWp.equals(VorkathAutoWorldPoints.CAVE_TO_MYSTERIOUS_RUINS_TILE)) {
//                    caveToMysteriousRuins = null;
//                }
//                break;
//        }
//    }

    public void setAllVarsNull() {
        sirsalBanker = null;
    }
}
