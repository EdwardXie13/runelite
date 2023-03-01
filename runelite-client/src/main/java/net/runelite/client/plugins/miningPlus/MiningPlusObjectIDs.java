package net.runelite.client.plugins.miningPlus;

import net.runelite.api.GameObject;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;

public class MiningPlusObjectIDs {
    public static final int COPPER_ROCK_ID = ObjectID.ROCKS_11161;
    public static final int IRON_ROCK_ID = ObjectID.ROCKS_11365;

    public static GameObject copperRockL = null;
    public static GameObject copperRockR = null;
    public static GameObject ironRockL = null;
    public static GameObject ironRockR = null;


//    public static final int EMPTY_ROCK_ID = ObjectID.ROCKS_11391;
    public static void assignObjects(GameObjectSpawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id) {
            case COPPER_ROCK_ID:
                WorldPoint copperRockWp = event.getTile().getWorldLocation();
                if(copperRockWp.equals(MiningPlusWorldPoints.FALADOR_LEFT_COPPER_ROCK)) {
                    copperRockL = obj;
                } else if(copperRockWp.equals(MiningPlusWorldPoints.FALADOR_RIGHT_COPPER_ROCK)) {
                    copperRockR = obj;
                }
                break;
            case IRON_ROCK_ID:
                WorldPoint ironRockWp = event.getTile().getWorldLocation();
                if(ironRockWp.equals(MiningPlusWorldPoints.FALADOR_LEFT_IRON_ROCK)) {
                    ironRockL = obj;
                } else if(ironRockWp.equals(MiningPlusWorldPoints.FALADOR_RIGHT_IRON_ROCK)) {
                    ironRockR = obj;
                }
                break;
        }
    }

    public static void assignObjects(GameObjectDespawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id) {
            case COPPER_ROCK_ID:
                WorldPoint copperRockWp = event.getTile().getWorldLocation();
                if(copperRockWp.equals(MiningPlusWorldPoints.FALADOR_LEFT_COPPER_ROCK)) {
                    copperRockL = null;
                } else if(copperRockWp.equals(MiningPlusWorldPoints.FALADOR_RIGHT_COPPER_ROCK)) {
                    copperRockR = null;
                }
                break;
            case IRON_ROCK_ID:
                WorldPoint ironRockWp = event.getTile().getWorldLocation();
                if(ironRockWp.equals(MiningPlusWorldPoints.FALADOR_LEFT_IRON_ROCK)) {
                    ironRockL = null;
                } else if(ironRockWp.equals(MiningPlusWorldPoints.FALADOR_RIGHT_IRON_ROCK)) {
                    ironRockR = null;
                }
                break;
        }
    }
}
