package net.runelite.client.plugins.agilityPlus;

import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;

public class AgilityPlusObjectIDs {
    //=== GNOME_STRONGHOLD ===
    public static final int LOG_BALANCE_23145 = ObjectID.LOG_BALANCE_23145;
    public static final int OBSTACLE_NET_23134 = ObjectID.OBSTACLE_NET_23134;
    public static final int TREE_BRANCH_23559 = ObjectID.TREE_BRANCH_23559;
    public static final int BALANCING_ROPE_23557 = ObjectID.BALANCING_ROPE_23557;
    public static final int TREE_BRANCH_23560 = ObjectID.TREE_BRANCH_23560;
    public static final int OBSTACLE_NET_23135 = ObjectID.OBSTACLE_NET_23135;
    public static final int OBSTACLE_PIPE_23138 = ObjectID.OBSTACLE_PIPE_23138; //left pipe
    public static final int OBSTACLE_PIPE_23139 = ObjectID.OBSTACLE_PIPE_23139; //right pipe

    public static GroundObject gnomeLogBalance = null;
    public static GameObject gnomeObstacleNet1_L = null;
    public static GameObject gnomeObstacleNet1_M = null; // should use
    public static GameObject gnomeObstacleNet1_R = null;
    public static GameObject gnomeTreeBranch1 = null;
    public static GroundObject gnomeBalancingRope = null;
    public static GameObject gnomeTreeBranch2 = null;
    public static GameObject gnomeObstacleNet2_L = null; // should use
    public static GameObject gnomeObstacleNet2_M = null; // should use
    public static GameObject gnomeObstacleNet2_R = null;
    public static GameObject gnomeObstaclePipeLeft = null;
    public static GameObject gnomeObstaclePipeRight = null;

    //=== CANFIS ===
    public static final int TALL_TREE_14843 = ObjectID.TALL_TREE_14843;
    public static final int GAP_14844 = ObjectID.GAP_14844; // 1 roof
    public static final int GAP_14845 = ObjectID.GAP_14845; // 2 roof
    public static final int GAP_14848 = ObjectID.GAP_14848; // 3 roof
    public static final int GAP_14846 = ObjectID.GAP_14846; // 4 roof
    public static final int POLEVAULT = ObjectID.POLEVAULT; // 5 roof
    public static final int GAP_14847 = ObjectID.GAP_14847; // 6 roof
    public static final int GAP_14897 = ObjectID.GAP_14897; // 7 roof

    public static GameObject canfisTallTree = null;
    public static GameObject canfisFirstRoofGap = null;
    public static GameObject canfisSecondRoofGap = null;
    public static GameObject canfisThirdRoofGap = null;
    public static GameObject canfisFourthRoofGap = null;
    public static GameObject canfisFifthRoofGap = null; // poleVault
    public static GameObject canfisSixthRoofGap = null;
    public static GameObject canfisSeventhRoofGap = null;

    public static void assignObjects(GameObjectSpawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id)
        {
            //=== GNOME_STRONGHOLD ===
            case OBSTACLE_NET_23134:
                if(obj.getWorldLocation().equals(new WorldPoint(2471, 3425, 0)))
                    gnomeObstacleNet1_L = obj;
                else if(obj.getWorldLocation().equals(new WorldPoint(2473, 3425, 0)))
                    gnomeObstacleNet1_M = obj;
                else if(obj.getWorldLocation().equals(new WorldPoint(2475, 3425, 0)))
                    gnomeObstacleNet1_R = obj;
                break;
            case TREE_BRANCH_23559:
                gnomeTreeBranch1 = obj;
                break;
            case TREE_BRANCH_23560:
                gnomeTreeBranch2 = obj;
                break;
            case OBSTACLE_NET_23135:
                if(obj.getWorldLocation().equals(new WorldPoint(2483, 3426, 0)))
                    gnomeObstacleNet2_L = obj;
                else if(obj.getWorldLocation().equals(new WorldPoint(2485, 3426, 0)))
                    gnomeObstacleNet2_M = obj;
                else if(obj.getWorldLocation().equals(new WorldPoint(2487, 3426, 0)))
                    gnomeObstacleNet2_R = obj;
                break;
            case OBSTACLE_PIPE_23138:
                if(obj.getWorldLocation().equals(new WorldPoint(2484, 3431, 0)))
                    gnomeObstaclePipeLeft = obj;
                break;
            case OBSTACLE_PIPE_23139:
                if(obj.getWorldLocation().equals(new WorldPoint(2487, 3431, 0)))
                    gnomeObstaclePipeRight = obj;
                break;

            //=== CANFIS ===
            case TALL_TREE_14843:
                canfisTallTree = obj;
                break;
            case GAP_14844:
                canfisFirstRoofGap = obj;
                break;
            case GAP_14845:
                canfisSecondRoofGap = obj;
                break;
            case GAP_14848:
                canfisThirdRoofGap = obj;
                break;
            case GAP_14846:
                canfisFourthRoofGap = obj;
                break;
            case POLEVAULT:
                canfisFifthRoofGap = obj;
                break;
            case GAP_14847:
                canfisSixthRoofGap = obj;
                break;
            case GAP_14897:
                canfisSeventhRoofGap = obj;
                break;
        }
    }


    public static void assignObjects(GroundObjectSpawned event) {
        GroundObject obj = event.getGroundObject();

        int id = obj.getId();

        switch (id) {
            //=== GNOME_STRONGHOLD ===
            case LOG_BALANCE_23145:
                gnomeLogBalance = obj;
                break;
            case BALANCING_ROPE_23557:
                gnomeBalancingRope = obj;
                break;
        }
    }

    public static void assignObjects(GameObjectDespawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id)
        {
            //=== GNOME_STRONGHOLD ===
            case OBSTACLE_NET_23134:
                if(obj.getWorldLocation().equals(new WorldPoint(2471, 3425, 0)))
                    gnomeObstacleNet1_L = null;
                else if(obj.getWorldLocation().equals(new WorldPoint(2473, 3425, 0)))
                    gnomeObstacleNet1_M = null;
                else if(obj.getWorldLocation().equals(new WorldPoint(2475, 3425, 0)))
                    gnomeObstacleNet1_R = null;
                break;
            case TREE_BRANCH_23559:
                gnomeTreeBranch1 = null;
                break;
            case TREE_BRANCH_23560:
                gnomeTreeBranch2 = null;
                break;
            case OBSTACLE_NET_23135:
                if(obj.getWorldLocation().equals(new WorldPoint(2483, 3426, 0)))
                    gnomeObstacleNet2_L = null;
                else if(obj.getWorldLocation().equals(new WorldPoint(2485, 3426, 0)))
                    gnomeObstacleNet2_M = null;
                else if(obj.getWorldLocation().equals(new WorldPoint(2487, 3426, 0)))
                    gnomeObstacleNet2_R = null;
                break;
            case OBSTACLE_PIPE_23138:
                if(obj.getWorldLocation().equals(new WorldPoint(2484, 3431, 0)))
                    gnomeObstaclePipeLeft = null;
                break;
            case OBSTACLE_PIPE_23139:
                if(obj.getWorldLocation().equals(new WorldPoint(2487, 3431, 0)))
                    gnomeObstaclePipeRight = null;
                break;

            //=== CANFIS ===
            case TALL_TREE_14843:
                canfisTallTree = null;
                break;
            case GAP_14844:
                canfisFirstRoofGap = null;
                break;
            case GAP_14845:
                canfisSecondRoofGap = null;
                break;
            case GAP_14848:
                canfisThirdRoofGap = null;
                break;
            case GAP_14846:
                canfisFourthRoofGap = null;
                break;
            case POLEVAULT:
                canfisFifthRoofGap = null;
                break;
            case GAP_14847:
                canfisSixthRoofGap = null;
                break;
            case GAP_14897:
                canfisSeventhRoofGap = null;
                break;
        }
    }

    public static void assignObjects(GroundObjectDespawned event) {
        GroundObject obj = event.getGroundObject();

        int id = obj.getId();

        switch (id) {
            //=== GNOME_STRONGHOLD ===
            case LOG_BALANCE_23145:
                gnomeLogBalance = null;
                break;
            case BALANCING_ROPE_23557:
                gnomeBalancingRope = null;
                break;
        }
    }
}
