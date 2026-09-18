package net.runelite.client.plugins.gemstoneCrabAuto;

import lombok.experimental.UtilityClass;
import net.runelite.api.*;
import net.runelite.api.events.*;

/**
 * GameObject IDs and cached refs for the Gemstone Crab bot.
 * Same shape as VorkathAutoObjectIDs — refs are populated on
 * GameObjectSpawned and cleared on GameObjectDespawned.
 */
@UtilityClass
public class GemstoneCrabAutoObjectIDs {
    /** The cave GameObject the crab retreats into on death. */
    public final int GEMSTONE_CRAB_CAVE = 57631;

    public static GameObject gemstoneCrabCave = null;

    public static void assignObjects(Client client, GameObjectSpawned event) {
        GameObject obj = event.getGameObject();
        if (obj == null) return;
        try {
            if (obj.getId() == GEMSTONE_CRAB_CAVE) {
                gemstoneCrabCave = obj;
            }
        } catch (Throwable t) { /* mid-teardown, skip */ }
    }

    public static void assignObjects(Client client, GameObjectDespawned event) {
        GameObject obj = event.getGameObject();
        if (obj == null) return;
        try {
            if (obj.getId() == GEMSTONE_CRAB_CAVE) {
                gemstoneCrabCave = null;
            }
        } catch (Throwable t) { /* skip */ }
    }

    public static void setAllVarsNull() {
        gemstoneCrabCave = null;
    }
}
