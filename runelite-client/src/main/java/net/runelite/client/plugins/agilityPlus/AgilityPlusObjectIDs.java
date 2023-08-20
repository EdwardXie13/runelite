package net.runelite.client.plugins.agilityPlus;

import lombok.experimental.UtilityClass;
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

@UtilityClass
public class AgilityPlusObjectIDs {
    //=== GNOME_STRONGHOLD ===
    public final int LOG_BALANCE_23145 = ObjectID.LOG_BALANCE_23145;
    public final int OBSTACLE_NET_23134 = ObjectID.OBSTACLE_NET_23134;
    public final int TREE_BRANCH_23559 = ObjectID.TREE_BRANCH_23559;
    public final int BALANCING_ROPE_23557 = ObjectID.BALANCING_ROPE_23557;
    public final int TREE_BRANCH_23560 = ObjectID.TREE_BRANCH_23560;
    public final int OBSTACLE_NET_23135 = ObjectID.OBSTACLE_NET_23135;
    public final int OBSTACLE_PIPE_23138 = ObjectID.OBSTACLE_PIPE_23138; //left pipe
    public final int OBSTACLE_PIPE_23139 = ObjectID.OBSTACLE_PIPE_23139; //right pipe

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

    //=== VARROCK ===
    public final int ROUGH_WALL_14412 = ObjectID.ROUGH_WALL_14412;
    public final int CLOTHES_LINE = ObjectID.CLOTHES_LINE;
    public final int GAP_14414 = ObjectID.GAP_14414;
    public final int Wall_14832 = ObjectID.WALL_14832;
    public final int GAP_14833 = ObjectID.GAP_14833;
    public final int GAP_14834 = ObjectID.GAP_14834;
    public final int GAP_14835 = ObjectID.GAP_14835;
    public final int LEDGE_14836 = ObjectID.LEDGE_14836;
    public final int EDGE = ObjectID.EDGE;

    public static DecorativeObject varrockStart = null;
    public static GameObject varrockFirstRoofGap = null; // 500 zoom
    public static GameObject varrockSecondRoofGap = null; // 370 zoom
    public static GameObject varrockThirdRoofGap = null; // 500 zoom
    public static GameObject varrockFourthRoofGap = null; // 495 zoom, west
    public static GameObject varrockFifthRoofGap = null; // 155 zoom, south
    public static GameObject varrockSixthRoofGap = null; // 640 zoom, east, 72 pitch
    public static GameObject varrockSeventhRoofGap = null; // 455 zoom, south, 512 pitch
                                                            // 320 zoom, north, 234 pitch
    public static GameObject varrockEighthRoofGap = null; // -45 zoom, south, 116 pitch

    //=== FALADOR ===
    public final int ROUGH_WALL_148989 = ObjectID.ROUGH_WALL_14898;
    public final int TIGHTROPE_14899 = ObjectID.TIGHTROPE_14899;
    public final int HAND_HOLDS_14901 = ObjectID.HAND_HOLDS_14901; // 230 zoom, east, 172 pitch
    public final int GAP_14903 = ObjectID.GAP_14903; // 255 zoom, east, 42 pitch
                                                            // 574 zoom, 1313 yaw, 42 pitch (355, 630, 10)
    public final int GAP_14904 = ObjectID.GAP_14904; // 625 zoom, north, 512 pitch
    public final int TIGHTROPE_14905 = ObjectID.TIGHTROPE_14905; // -72 zoom, east, 103 pitch
    public final int TIGHTROPE_14911 = ObjectID.TIGHTROPE_14911; // 862 zoom, north, 512 pitch
    public final int GAP_14919 = ObjectID.GAP_14919; // 295 zoom, north, 214 pitch
    public final int LEDGE_14920 = ObjectID.LEDGE_14920; // 174 zoom, 1975 yaw, 91 pitch (170, 783, 10))
    public final int LEDGE_14921 = ObjectID.LEDGE_14921; // 410 zoom, 1024 yaw, 224 pitch
    public final int LEDGE_14922 = ObjectID.LEDGE_14922; // 730 zoom, 1024 yaw, 91 pitch
    public final int LEDGE_14924 = ObjectID.LEDGE_14924; // 390 zoom, 512 yaw, 202 pitch
    public final int EDGE_14925 = ObjectID.EDGE_14925; // -72 zoom, 642 yaw, 77 pitch (135, 790, 10)

    public static DecorativeObject faladorStart = null;
    public static GroundObject faladorFirstRoofGap = null;
    public static GameObject faladorSecondRoofGap = null;
    public static GameObject faladorThirdRoofGap = null; // un used
    public static GameObject faladorFourthRoofGap = null;
    public static GameObject faladorFifthRoofGap = null;
    public static GroundObject faladorSixthRoofGap = null;
    public static GameObject faladorSeventhRoofGap = null;
    public static GameObject faladorEighthRoofGap = null; // un used
    public static GameObject faladorNinthRoofGap = null;
    public static GameObject faladorTenthRoofGap = null;
    public static GameObject faladorEleventhRoofGap = null;
    public static GameObject faladorTwelfthRoofGap = null; // unused

    //=== CANFIS ===
    public final int TALL_TREE_14843 = ObjectID.TALL_TREE_14843;
    public final int GAP_14844 = ObjectID.GAP_14844; // 1 roof
    public final int GAP_14845 = ObjectID.GAP_14845; // 2 roof
    public final int GAP_14848 = ObjectID.GAP_14848; // 3 roof
    public final int GAP_14846 = ObjectID.GAP_14846; // 4 roof
    public final int POLEVAULT = ObjectID.POLEVAULT; // 5 roof
    public final int GAP_14847 = ObjectID.GAP_14847; // 6 roof
    public final int GAP_14897 = ObjectID.GAP_14897; // 7 roof

    public static GameObject canfisTallTree = null;
    public static GameObject canfisFirstRoofGap = null;
    public static GameObject canfisSecondRoofGap = null;
    public static GameObject canfisThirdRoofGap = null;
    public static GameObject canfisFourthRoofGap = null;
    public static GameObject canfisFifthRoofGap = null; // poleVault
    public static GameObject canfisSixthRoofGap = null;
    public static GameObject canfisSeventhRoofGap = null;

    //=== SEERS ===
    public final int WALL_14927 = ObjectID.WALL_14927;
    public final int GAP_14928 = ObjectID.GAP_14928;
    public final int TIGHTROPE_14932 = ObjectID.TIGHTROPE_14932;
    public final int GAP_14929 = ObjectID.GAP_14929;
    public final int GAP_14930 = ObjectID.GAP_14930;
    public final int EDGE_14931 = ObjectID.EDGE_14931;

    public static DecorativeObject seersStartWall = null;
    public static GameObject seersFirstRoofGap = null;
    public static GroundObject seersTightrope = null;
    public static GameObject seersThirdRoofGap = null;
    public static GameObject seersFourthRoofGap = null;
    public static GameObject seersFifthRoofGap = null;

    //=== RELLEKA ===
    public final int ROUGH_WALL_14946 = ObjectID.ROUGH_WALL_14946;
    public final int GAP_14947 = ObjectID.GAP_14947;
    public final int TIGHTROPE_14987 = ObjectID.TIGHTROPE_14987;
    public final int GAP_14990 = ObjectID.GAP_14990;
    public final int GAP_14991 = ObjectID.GAP_14991;
    public final int TIGHTROPE_14992 = ObjectID.TIGHTROPE_14992;
    public final int PILE_OF_FISH = ObjectID.PILE_OF_FISH;

    public static DecorativeObject rellekaStartWall = null;
    public static GameObject rellekaFirstRoofGap = null;
    public static GameObject rellekaSecondRoofGap = null;
    public static GameObject rellekaThirdRoofGap = null;
    public static GameObject rellekaFourthRoofGap = null;
    public static GameObject rellekaFifthRoofGap = null;
    public static GameObject rellekaSixthRoofGap = null;

    //=== ARDY ===
    public final int GAP_15609 = ObjectID.GAP_15609;
    public final int PLANK_26635 = ObjectID.PLANK_26635;
    public final int GAP_15610 = ObjectID.GAP_15610;
    public final int GAP_15611 = ObjectID.GAP_15611;
    public final int STEEP_ROOF = ObjectID.STEEP_ROOF;
    public final int GAP_15612 = ObjectID.GAP_15612;
    public final int WOODEN_BEAMS = ObjectID.WOODEN_BEAMS;

    public static GameObject ardyFirstRoofGap = null;
    public static GroundObject ardySecondRoofGap = null;
    public static GameObject ardyThirdRoofGap = null;
    public static GameObject ardyFourthRoofGap = null;
    public static GameObject ardyFifthRoofGap = null;
    public static GameObject ardySixthRoofGap = null;
    public static DecorativeObject ardyStartWall = null;

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

            //=== VARROCK ===
            case CLOTHES_LINE:
                varrockFirstRoofGap = obj;
                break;
            case GAP_14414:
                varrockSecondRoofGap = obj;
                break;
            case Wall_14832:
                varrockThirdRoofGap = obj;
                break;
            case GAP_14833:
                varrockFourthRoofGap = obj;
                break;
            case GAP_14834:
                varrockFifthRoofGap = obj;
                break;
            case GAP_14835:
                varrockSixthRoofGap = obj;
                break;
            case LEDGE_14836:
                varrockSeventhRoofGap = obj;
                break;
            case EDGE:
                varrockEighthRoofGap = obj;
                break;

            //=== FALADOR ===
            case HAND_HOLDS_14901:
                faladorSecondRoofGap = obj;
                break;
            case GAP_14903:
                faladorThirdRoofGap = obj;
                break;
            case GAP_14904:
                faladorFourthRoofGap = obj;
                break;
            case TIGHTROPE_14905:
                faladorFifthRoofGap = obj;
                break;
            case GAP_14919:
                faladorSeventhRoofGap = obj;
                break;
            case LEDGE_14920:
                faladorEighthRoofGap = obj;
                break;
            case LEDGE_14921:
                faladorNinthRoofGap = obj;
                break;
            case LEDGE_14922:
                faladorTenthRoofGap = obj;
                break;
            case LEDGE_14924:
                faladorEleventhRoofGap = obj;
                break;
            case EDGE_14925:
                faladorTwelfthRoofGap = obj;
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

            //=== ARDY ===
            case GAP_15609:
                ardyFirstRoofGap = obj;
                break;
            case GAP_15610:
                ardyThirdRoofGap = obj;
                break;
            case GAP_15611:
                ardyFourthRoofGap = obj;
                break;
            case STEEP_ROOF:
                ardyFifthRoofGap = obj;
                break;
            case GAP_15612:
                ardySixthRoofGap = obj;
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

            //=== VARROCK ===
            case CLOTHES_LINE:
                varrockFirstRoofGap = null;
                break;
            case GAP_14414:
                varrockSecondRoofGap = null;
                break;
            case Wall_14832:
                varrockThirdRoofGap = null;
                break;
            case GAP_14833:
                varrockFourthRoofGap = null;
                break;
            case GAP_14834:
                varrockFifthRoofGap = null;
                break;
            case GAP_14835:
                varrockSixthRoofGap = null;
                break;
            case LEDGE_14836:
                varrockSeventhRoofGap = null;
                break;
            case EDGE:
                varrockEighthRoofGap = null;
                break;

            //=== FALADOR ===
            case HAND_HOLDS_14901:
                faladorSecondRoofGap = null;
                break;
            case GAP_14903:
                faladorThirdRoofGap = null;
                break;
            case GAP_14904:
                faladorFourthRoofGap = null;
                break;
            case TIGHTROPE_14905:
                faladorFifthRoofGap = null;
                break;
            case GAP_14919:
                faladorSeventhRoofGap = null;
                break;
            case LEDGE_14920:
                faladorEighthRoofGap = null;
                break;
            case LEDGE_14921:
                faladorNinthRoofGap = null;
                break;
            case LEDGE_14922:
                faladorTenthRoofGap = null;
                break;
            case LEDGE_14924:
                faladorEleventhRoofGap = null;
                break;
            case EDGE_14925:
                faladorTwelfthRoofGap = null;
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

            //=== ARDY ===
            case GAP_15609:
                ardyFirstRoofGap = null;
                break;
            case GAP_15610:
                ardyThirdRoofGap = null;
                break;
            case GAP_15611:
                ardyFourthRoofGap = null;
                break;
            case STEEP_ROOF:
                ardyFifthRoofGap = null;
                break;
            case GAP_15612:
                ardySixthRoofGap = null;
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

            //=== FALADOR ===
            case TIGHTROPE_14899:
                faladorFirstRoofGap = obj;
                break;
            case TIGHTROPE_14911:
                faladorSixthRoofGap = obj;
                break;

            //=== SEERS ===
            case TIGHTROPE_14932:
                seersTightrope = obj;
                break;

            //=== ARDY ===
            case PLANK_26635:
                ardySecondRoofGap = obj;
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

            //=== FALADOR ===
            case TIGHTROPE_14899:
                faladorFirstRoofGap = null;
                break;
            case TIGHTROPE_14911:
                faladorSixthRoofGap = null;
                break;

            //=== SEERS ===
            case TIGHTROPE_14932:
                seersTightrope = null;
                break;

            //=== ARDY ===
            case PLANK_26635:
                ardySecondRoofGap = null;
                break;
        }
    }

    public static void assignObjects(DecorativeObjectSpawned event) {
        DecorativeObject obj = event.getDecorativeObject();

        int id = obj.getId();

        switch (id) {
            //=== VARROCK ===
            case ROUGH_WALL_14412:
                varrockStart = obj;
                break;

            //=== FALADOR ===
            case ROUGH_WALL_148989:
                faladorStart = obj;
                break;

            //=== SEERS ===
            case WALL_14927:
                seersStartWall = obj;
                break;
            case ROUGH_WALL_14946:
                rellekaStartWall = obj;
                break;

            //=== ARDY ===
            case WOODEN_BEAMS:
                ardyStartWall = obj;
                break;
        }
    }

    public static void assignObjects(DecorativeObjectDespawned event) {
        DecorativeObject obj = event.getDecorativeObject();

        int id = obj.getId();

        switch (id) {
            //=== VARROCK ===
            case ROUGH_WALL_14412:
                varrockStart = null;
                break;

            //=== FALADOR ===
            case ROUGH_WALL_148989:
                faladorStart = null;
                break;

            //=== SEERS ===
            case WALL_14927:
                seersStartWall = null;
                break;
            case ROUGH_WALL_14946:
                rellekaStartWall = null;
                break;

            //=== ARDY ===
            case WOODEN_BEAMS:
                ardyStartWall = null;
                break;
        }
    }
}
