package net.runelite.client.plugins.theivingPlus;

import net.runelite.api.GameObject;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;

public class ThievingPlusObjectIDs {
    public static final int CAKE_STALL_ID = ObjectID.BAKERS_STALL_11730;

    public static GameObject cakeStall = null;


//    public static final int EMPTY_ROCK_ID = ObjectID.ROCKS_11391;
    public static void assignObjects(GameObjectSpawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id) {
            case CAKE_STALL_ID:
                WorldPoint cakeStallWp = event.getTile().getWorldLocation();
                if(cakeStallWp.equals(ThievingPlusWorldPoints.ARDY_EAST_BAKERY_STALL)) {
                    cakeStall = obj;
                }
                break;
        }
    }

    public static void assignObjects(GameObjectDespawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id) {
            case CAKE_STALL_ID:
                WorldPoint cakeStallWp = event.getTile().getWorldLocation();
                if(cakeStallWp.equals(ThievingPlusWorldPoints.ARDY_EAST_BAKERY_STALL)) {
                    cakeStall = null;
                }
                break;
        }
    }
}
