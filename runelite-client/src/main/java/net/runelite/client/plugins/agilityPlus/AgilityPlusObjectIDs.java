package net.runelite.client.plugins.agilityPlus;

import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
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

    public static void assignObjects(GameObjectSpawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();

        switch (id)
        {
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
        }
    }


    public static void assignObjects(GroundObjectSpawned event) {
        GroundObject obj = event.getGroundObject();

        int id = obj.getId();

        switch (id) {
            case LOG_BALANCE_23145:
                gnomeLogBalance = obj;
                break;
            case BALANCING_ROPE_23557:
                gnomeBalancingRope = obj;
                break;
        }
    }
}
