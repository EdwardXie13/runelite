package net.runelite.client.plugins.bloodRuneTrue;

import lombok.experimental.UtilityClass;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;

@UtilityClass
public class BloodRuneTrueObjectIDs {
    public final int FAIRY_RING_29495_POH = 29228;
    public final int MYSTERIOUS_RUINS = 25380;

    public final int FAIRY_RING_POH = FAIRY_RING_29495_POH;
    public final int POOL_OF_REVITALISATION = ObjectID.POOL_OF_REVITALISATION;
    public final int POOL_OF_REJUVENATION = ObjectID.POOL_OF_REJUVENATION;
    public final int FANCY_POOL_OF_REJUVENATION = ObjectID.FANCY_POOL_OF_REJUVENATION;
    public final int ORNATE_POOL_OF_REJUVENATION = ObjectID.ORNATE_POOL_OF_REJUVENATION;
    public final int FAIRY_RING_CAVE_ENTRANCE = ObjectID.CAVE_ENTRANCE_16308;
    public final int SECOND_CAVE_ENTRANCE = ObjectID.CAVE_ENTRANCE_5046;
    public final int CAVE_SHORTCUT = ObjectID.CAVE_43759;
    public final int CAVE_TO_MYSTERIOUS_RUINS = ObjectID.CAVE_43762;
    public final int BLOOD_MYSTERIOUS_RUINS = MYSTERIOUS_RUINS;
    public final int TRUE_BLOOD_ALTAR = ObjectID.ALTAR_43479;
    public final int CASTLE_WARS_BANK_CHEST = ObjectID.BANK_CHEST_4483;

    public static GameObject fairyRingPOH = null;
    public static GameObject restorationPoolPOH = null;
    public static GameObject fairyRingCaveEntrance = null;
    public static GameObject caveEntrance1 = null;
    public static GameObject caveShortcut = null;
    public static WallObject caveToMysteriousRuins = null;
    public static GameObject bloodMysteriousRuins = null;
    public static GameObject trueBloodAltar = null;
    public static GameObject castleWarsBankChest = null;

    public static void assignObjects(Client client, GameObjectSpawned event) {
        GameObject obj = event.getGameObject();
        int id = obj.getId();

        WorldPoint objWp = WorldPoint.fromLocalInstance(client, obj.getLocalLocation());
        if (objWp == null)
            return;

        switch (id) {
            case FAIRY_RING_POH:
                fairyRingPOH = obj;
                break;

            case POOL_OF_REVITALISATION:
            case POOL_OF_REJUVENATION:
            case FANCY_POOL_OF_REJUVENATION:
            case ORNATE_POOL_OF_REJUVENATION:
                BloodRuneTrueWorldPoints.INFRONT_OF_POOL = new WorldPoint(objWp.getX()-2, objWp.getY(), 0);
                restorationPoolPOH = obj;
                break;

            case FAIRY_RING_CAVE_ENTRANCE:
                if (objWp.equals(BloodRuneTrueWorldPoints.FAIRY_RING_CAVE_ENTRANCE)) {
                    fairyRingCaveEntrance = obj;
                }
                break;

            case SECOND_CAVE_ENTRANCE:
                if (objWp.equals(BloodRuneTrueWorldPoints.SECOND_CAVE_ENTRANCE_TILE)) {
                    caveEntrance1 = obj;
                }
                break;

            case CAVE_SHORTCUT:
                if (objWp.equals(BloodRuneTrueWorldPoints.CAVE_SHORTCUT_TILE)) {
                    caveShortcut = obj;
                }
                break;

            case BLOOD_MYSTERIOUS_RUINS:
                bloodMysteriousRuins = obj;
                break;

            case TRUE_BLOOD_ALTAR:
                trueBloodAltar = obj;
                break;

            case CASTLE_WARS_BANK_CHEST:
                if (objWp.equals(BloodRuneTrueWorldPoints.CASTLE_WARS_BANK_CHEST)) {
                    castleWarsBankChest = obj;
                }
                break;
        }
    }

    public static void assignObjects(Client client, GameObjectDespawned event) {
        GameObject obj = event.getGameObject();
        int id = obj.getId();

        WorldPoint objWp = WorldPoint.fromLocalInstance(client, obj.getLocalLocation());
        if (objWp == null)
            return;

        switch (id) {
            case FAIRY_RING_POH:
                fairyRingPOH = null;
                break;

            case POOL_OF_REVITALISATION:
            case POOL_OF_REJUVENATION:
            case FANCY_POOL_OF_REJUVENATION:
            case ORNATE_POOL_OF_REJUVENATION:
                restorationPoolPOH = null;
                break;

            case FAIRY_RING_CAVE_ENTRANCE:
                if (objWp.equals(BloodRuneTrueWorldPoints.FAIRY_RING_CAVE_ENTRANCE)) {
                    fairyRingCaveEntrance = null;
                }
                break;

            case SECOND_CAVE_ENTRANCE:
                if (objWp.equals(BloodRuneTrueWorldPoints.SECOND_CAVE_ENTRANCE_TILE)) {
                    caveEntrance1 = null;
                }
                break;

            case CAVE_SHORTCUT:
                if (objWp.equals(BloodRuneTrueWorldPoints.CAVE_SHORTCUT_TILE)) {
                    caveShortcut = null;
                }
                break;

            case BLOOD_MYSTERIOUS_RUINS:
                bloodMysteriousRuins = null;
                break;

            case TRUE_BLOOD_ALTAR:
                trueBloodAltar = null;
                break;

            case CASTLE_WARS_BANK_CHEST:
                if (objWp.equals(BloodRuneTrueWorldPoints.CASTLE_WARS_BANK_CHEST)) {
                    castleWarsBankChest = null;
                }
                break;
        }
    }

    public static void assignObjects(Client client, WallObjectSpawned event) {
        WallObject obj = event.getWallObject();
        int id = obj.getId();

        WorldPoint objWp = WorldPoint.fromLocalInstance(client, obj.getLocalLocation());
        if (objWp == null)
            return;

        switch (id) {
            case CAVE_TO_MYSTERIOUS_RUINS:
                if (objWp.equals(BloodRuneTrueWorldPoints.CAVE_TO_MYSTERIOUS_RUINS_TILE)) {
                    caveToMysteriousRuins = obj;
                }
                break;
        }
    }

    public static void assignObjects(Client client, WallObjectDespawned event) {
        WallObject obj = event.getWallObject();
        int id = obj.getId();

        WorldPoint objWp = WorldPoint.fromLocalInstance(client, obj.getLocalLocation());
        if (objWp == null)
            return;

        switch (id) {
            case CAVE_TO_MYSTERIOUS_RUINS:
                if (objWp.equals(BloodRuneTrueWorldPoints.CAVE_TO_MYSTERIOUS_RUINS_TILE)) {
                    caveToMysteriousRuins = null;
                }
                break;
        }
    }

    public void setAllVarsNull() {
        fairyRingPOH = null;
        restorationPoolPOH = null;
        fairyRingCaveEntrance = null;
        caveEntrance1 = null;
        caveShortcut = null;
        caveToMysteriousRuins = null;
        bloodMysteriousRuins = null;
        trueBloodAltar = null;
        castleWarsBankChest = null;
    }
}
