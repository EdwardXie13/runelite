package net.runelite.client.plugins.agilityPlus;

import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
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

    //=== SEERS ===
    public static final int WALL_14927 = ObjectID.WALL_14927;
    public static final int GAP_14928 = ObjectID.GAP_14928;
    public static final int TIGHTROPE_14932 = ObjectID.TIGHTROPE_14932;
    public static final int GAP_14929 = ObjectID.GAP_14929;
    public static final int GAP_14930 = ObjectID.GAP_14930;
    public static final int EDGE_14931 = ObjectID.EDGE_14931;

    public static DecorativeObject seersStartWall = null;
    public static GameObject seersFirstRoofGap = null;
    public static GroundObject seersTightrope = null;
    public static GameObject seersThirdRoofGap = null;
    public static GameObject seersFourthRoofGap = null;
    public static GameObject seersFifthRoofGap = null;

    //=== RELLEKA ===
    public static final int ROUGH_WALL_14946 = ObjectID.ROUGH_WALL_14946;
    public static final int GAP_14947 = ObjectID.GAP_14947;
    public static final int TIGHTROPE_14987 = ObjectID.TIGHTROPE_14987;
    public static final int GAP_14990 = ObjectID.GAP_14990;
    public static final int GAP_14991 = ObjectID.GAP_14991;
    public static final int TIGHTROPE_14992 = ObjectID.TIGHTROPE_14992;
    public static final int PILE_OF_FISH = ObjectID.PILE_OF_FISH;

    public static DecorativeObject rellekaStartWall = null;
    public static GameObject rellekaFirstRoofGap = null;
    public static GameObject rellekaSecondRoofGap = null;
    public static GameObject rellekaThirdRoofGap = null;
    public static GameObject rellekaFourthRoofGap = null;
    public static GameObject rellekaFifthRoofGap = null;
    public static GameObject rellekaSixthRoofGap = null;

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

            //=== SEERS ===
            case GAP_14928:
                seersFirstRoofGap = obj;
                break;
            case GAP_14929:
                seersThirdRoofGap = obj;
                break;
            case GAP_14930:
                seersFourthRoofGap = obj;
                break;
            case EDGE_14931:
                seersFifthRoofGap = obj;
                break;

            //=== RELLEKA ===
            case GAP_14947:
                rellekaFirstRoofGap = obj;
                break;
            case TIGHTROPE_14987:
                rellekaSecondRoofGap = obj;
                break;
            case GAP_14990:
                rellekaThirdRoofGap = obj;
                break;
            case GAP_14991:
                rellekaFourthRoofGap = obj;
                break;
            case TIGHTROPE_14992:
                rellekaFifthRoofGap = obj;
                break;
            case PILE_OF_FISH:
                rellekaSixthRoofGap = obj;
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

            //=== SEERS ===
            case GAP_14928:
                seersFirstRoofGap = null;
                break;
            case GAP_14929:
                seersThirdRoofGap = null;
                break;
            case GAP_14930:
                seersFourthRoofGap = null;
                break;
            case EDGE_14931:
                seersFifthRoofGap = null;
                break;

            //=== RELLEKA ===
            case GAP_14947:
                rellekaFirstRoofGap = null;
                break;
            case TIGHTROPE_14987:
                rellekaSecondRoofGap = null;
                break;
            case GAP_14990:
                rellekaThirdRoofGap = null;
                break;
            case GAP_14991:
                rellekaFourthRoofGap = null;
                break;
            case TIGHTROPE_14992:
                rellekaFifthRoofGap = null;
                break;
            case PILE_OF_FISH:
                rellekaSixthRoofGap = null;
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

            //=== SEERS ===
            case TIGHTROPE_14932:
                seersTightrope = obj;
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

            //=== SEERS ===
            case TIGHTROPE_14932:
                seersTightrope = null;
                break;
        }
    }

    public static void assignObjects(DecorativeObjectSpawned event) {
        DecorativeObject obj = event.getDecorativeObject();

        int id = obj.getId();

        switch (id) {
            //=== SEERS ===
            case WALL_14927:
                seersStartWall = obj;
                break;
            case ROUGH_WALL_14946:
                rellekaStartWall = obj;
                break;
        }
    }

    public static void assignObjects(DecorativeObjectDespawned event) {
        DecorativeObject obj = event.getDecorativeObject();

        int id = obj.getId();

        switch (id) {
            //=== SEERS ===
            case WALL_14927:
                seersStartWall = null;
                break;
            case ROUGH_WALL_14946:
                rellekaStartWall = null;
                break;
        }
    }
}
