package net.runelite.client.plugins.miningPlus;

import net.runelite.api.GameObject;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;

public class MiningPlusObjectIDs {
    public static final int COPPER_ROCK_ID = ObjectID.ROCKS_11161;

    public static GameObject copperRockL = null;
    public static GameObject copperRockR = null;

//    public static final int EMPTY_ROCK_ID = ObjectID.ROCKS_11391;
    public static void assignObjects(GameObjectSpawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id) {
            case COPPER_ROCK_ID:
                WorldPoint copperRockWp = event.getTile().getWorldLocation();
                if(copperRockWp.equals(MiningPlusWorldPoints.FALADOR_RIGHT_COPPER_ROCK)) {
                    copperRockL = obj;
                } else if(copperRockWp.equals(MiningPlusWorldPoints.FALADOR_LEFT_COPPER_ROCK)) {
                    copperRockR = obj;
                }
                break;
        }
    }

    public static void assignObjects(GameObjectDespawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id) {
            case COPPER_ROCK_ID:
                WorldPoint bankBoothWp = event.getTile().getWorldLocation();
                if(bankBoothWp.equals(new WorldPoint(3038, 9780, 0))) {
                    copperRockL = null;
                } else if(bankBoothWp.equals(new WorldPoint(3039, 9781, 0))) {
                    copperRockR = null;
                }
                break;
        }
    }
}
