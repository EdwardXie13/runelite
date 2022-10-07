//package net.runelite.client.plugins.rcPlusBloods;
//
//import net.runelite.api.GameObject;
//import net.runelite.api.ObjectID;
//import net.runelite.api.coords.WorldPoint;
//import net.runelite.api.events.GameObjectDespawned;
//import net.runelite.api.events.GameObjectSpawned;
//
//public class RCPlusBloodsObjectIDs {
//    public static final int NORTH_RUNESTONE_8981 = 8981;
//    public static final int SOUTH_RUNESTONE_10796 = 10796;
//
//    public static GameObject northRunestone = null;
//    public static GameObject southRunestone = null;
//
//    public static void assignObjects(GameObjectSpawned event) {
//        GameObject obj = event.getGameObject();
//
//        int id = obj.getId();
//
//        switch (id) {
//            case NORTH_RUNESTONE_8981:
//                northRunestone = obj;
//                break;
//            case SOUTH_RUNESTONE_10796:
//                southRunestone = obj;
//                break;
//        }
//    }
//
//    public static void assignObjects(GameObjectDespawned event) {
//        GameObject obj = event.getGameObject();
//
//        int id = obj.getId();
//
//        switch (id) {
//            case NORTH_RUNESTONE_8981:
//                northRunestone = null;
//                break;
//            case SOUTH_RUNESTONE_10796:
//                southRunestone = null;
//                break;
//        }
//    }
//}
