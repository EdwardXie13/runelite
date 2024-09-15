package net.runelite.client.plugins.theivingPlus;

import net.runelite.api.GameObject;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;

public class ThievingPlusObjectIDs {
    public static final int CAKE_STALL_ID = ObjectID.BAKERS_STALL_11730;
    public static final int FRUIT_STALL_ID = ObjectID.FRUIT_STALL_28823;

    public static GameObject cakeStall = null;
    public static GameObject fruitStall = null;

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
            case FRUIT_STALL_ID:
                WorldPoint fruitStallWp = event.getTile().getWorldLocation();
                if(fruitStallWp.equals(ThievingPlusWorldPoints.HOSIDIUS_FRUIT_STALL)) {
                    fruitStall = obj;
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
            case FRUIT_STALL_ID:
                WorldPoint fruitStallWp = event.getTile().getWorldLocation();
                if(fruitStallWp.equals(ThievingPlusWorldPoints.HOSIDIUS_FRUIT_STALL)) {
                    fruitStall = null;
                }
                break;
        }
    }
}
