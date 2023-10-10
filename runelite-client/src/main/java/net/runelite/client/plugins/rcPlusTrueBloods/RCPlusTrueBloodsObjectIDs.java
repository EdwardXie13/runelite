package net.runelite.client.plugins.rcPlusTrueBloods;

import lombok.experimental.UtilityClass;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;

@UtilityClass
public class RCPlusTrueBloodsObjectIDs {
    public final int CAVE_ENTRANCE_16308 = ObjectID.CAVE_ENTRANCE_16308;
    public final int CAVE_ENTRANCE_5046 = ObjectID.CAVE_ENTRANCE_5046;
    public final int CAVE_43759 = ObjectID.CAVE_43759;
    public final int trueBloodAltarEntranceID = 25380;
    public final int ALTAR_43479 = ObjectID.ALTAR_43479;

    public static GameObject caveEntrance16308 = null;
    public static GameObject caveEntrance5046 = null;
    public static GameObject cave43759 = null;
    public static GameObject trueBloodAltarEntrance = null;
    public static GameObject trueBloodAltar = null;

    public static void assignObjects(GameObjectSpawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();
        switch (id) {
            case CAVE_ENTRANCE_16308:
                if(obj.getWorldLocation().equals(new WorldPoint(3447, 9821, 0)))
                    caveEntrance16308 = obj;
                break;
            case CAVE_ENTRANCE_5046:
                if(obj.getWorldLocation().equals(new WorldPoint(3467, 9820, 0)))
                    caveEntrance5046 = obj;
                break;
            case CAVE_43759:
                cave43759 = obj;
                break;
            case trueBloodAltarEntranceID:
                trueBloodAltarEntrance = obj;
                break;
            case ALTAR_43479:
                trueBloodAltar = obj;
                break;
        }
    }

    public static void assignObjects(GameObjectDespawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();
        switch (id) {
            case CAVE_ENTRANCE_16308:
                if(obj.getWorldLocation().equals(new WorldPoint(3447, 9821, 0)))
                    caveEntrance16308 = null;
                break;
            case CAVE_ENTRANCE_5046:
                if(obj.getWorldLocation().equals(new WorldPoint(3467, 9820, 0)))
                    caveEntrance5046 = null;
                break;
            case CAVE_43759:
                cave43759 = null;
                break;
            case trueBloodAltarEntranceID:
                trueBloodAltarEntrance = null;
                break;
            case ALTAR_43479:
                trueBloodAltar = null;
                break;
        }
    }
}
