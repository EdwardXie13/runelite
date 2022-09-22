package net.runelite.client.plugins.agilityPlus;

import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.ItemID;
import net.runelite.api.Perspective;
import net.runelite.api.ScriptID;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class AgilityPlusMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    public static boolean isIdle = true;
    public static boolean resetOculusOrb = false;
    public long start;

    Thread t;

    AgilityPlusMain(Client client, ClientThread clientThread)
    {
        this.client = client;
        this.clientThread = clientThread;

        t = new Thread(this);
        System.out.println("New thread: " + t);
        start = System.currentTimeMillis();
        t.start(); // Starting the thread
    }

    // execution of thread starts from run() method
    public void run()
    {
        while (!Thread.interrupted()) {
            if (checkIdle() && checkLastReset())
                reset();

            if(isNotHealthly()) {
                delay(1000);
                return;
            }

            if(getRegionID() == 9781)
                doGnomeAgility();
            else if(getRegionID() == 13878)
                doCanfisAgility();
            else if(getRegionID() == 10806)
                doSeersAgility();
            else if(getRegionID() == 10553 || getRegionID() == 10297)
                doRellekaAgility();
        }
        System.out.println("Thread has stopped.");
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private void doGnomeAgility() {
        if(checkLevelUp()) {
            pressKey(KeyEvent.VK_SPACE);
            delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_LOG) && isIdle) {
            setCameraZoom(400);
            changeCameraYaw(0);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstacleNet1_M, 15);
            delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB1) && isIdle) {
            changeCameraYaw(206);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeTreeBranch1, 10);
            delay(500);
            pressKey(KeyEvent.VK_UP, 2000);
            delay(500);
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB2) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            delay(500);
            panCameraOneDirection(KeyEvent.VK_D, 200);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.GNOME_AFTER_CLIMB2_MISCLICK_ZONE));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(500);
        }
        else if(isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB2_MISCLICK_ZONE) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeBalancingRope, 10);
            delay(500);
            pressKey(KeyEvent.VK_DOWN, 2000);
            delay(500);
        }

        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_TIGHTROPE) && isIdle) {
            setCameraZoom(706);
            changeCameraYaw(312);
            delay(500);
            scheduledGameObjectPointDelay(new Point(825, 799), AgilityPlusObjectIDs.gnomeTreeBranch2, 10);
            delay(500);
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_DROP) && isIdle) {
            setCameraZoom(600);
            changeCameraYaw(37);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstacleNet2_L, 15);
            delay(500);
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_NET1) && isIdle) {
            setCameraZoom(550);
            changeCameraYaw(1024);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstaclePipeLeft, 15);
            delay(500);
            pressKey(KeyEvent.VK_UP, 2000);
            delay(7000); //adjust as needed
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_LEFT_PIPE) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            delay(500);
            panCameraOneDirection(KeyEvent.VK_A, 600);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.GNOME_START));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(500);
        }
        else if(isAtWorldPoint(AgilityPlusWorldPoints.GNOME_START) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeLogBalance, 10);
            delay(500);
            pressKey(KeyEvent.VK_DOWN, 2000);
            delay(500);
        }
    }

    private void doCanfisAgility() {
        if(checkLevelUp()) {
            pressKey(KeyEvent.VK_SPACE);
            delay(500);
        }
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_START) && isIdle) {
            delay(1000);
            setCameraZoom(660);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(3506, 3488, 0)));
            delay(500);
            pressKey(KeyEvent.VK_DOWN, 2000);
            delay(500);
//            delay(1500);
//            setCameraZoom(896);
//            changeCameraYaw(1257);
//            delay(500);
//            scheduledGameObjectPointDelay(new Point(116, 876), AgilityPlusObjectIDs.canfisTallTree, 10);
//            delay(1000);
//            pressKey(KeyEvent.VK_UP, 2000);
//            delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FAIL1) && isIdle) {
            delay(1000);
            setCameraZoom(896);
            delay(500);
            panCameraToCanfisTree();
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(3506, 3488, 0)));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(500);
            pressKey(KeyEvent.VK_DOWN, 2000);
            delay(500);

        } else if(isAtWorldPoint(new WorldPoint(3506, 3488, 0)) && isIdle) {
            delay(500);
            setCameraZoom(896);
            changeCameraYaw(1520);
            delay(1500);
            scheduledGameObjectPointDelay(new Point(84, 369), AgilityPlusObjectIDs.canfisTallTree, 10);
            delay(500);
            pressKey(KeyEvent.VK_UP, 2000);
            delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FAIL2) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            delay(500);
            panCameraOneDirection(KeyEvent.VK_W, 550);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(3506, 3488, 0)));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(500);
            pressKey(KeyEvent.VK_DOWN, 2000);
            delay(500);
      }
        // 1 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIRST_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1) && isIdle) {
            setCameraZoom(758);
            changeCameraYaw(0);
            delay(1000);
            checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIRST_ROOF) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1) && isIdle) {
            setCameraZoom(336);
            changeCameraYaw(0);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFirstRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1) && isIdle) {
            setCameraZoom(679);
            changeCameraYaw(0);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFirstRoofGap, 10);
        }
        // 2 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SECOND_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            delay(1000);
            checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SECOND_ROOF) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2) && isIdle) {
            setCameraZoom(540);
            changeCameraYaw(0);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSecondRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2) && isIdle) {
            setCameraZoom(597);
            changeCameraYaw(0);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSecondRoofGap, 10);
        }
        // 3 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_THIRD_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3) && isIdle) {
            delay(1000);
            setCameraZoom(751);
            changeCameraYaw(0);
            delay(1000);
            checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_THIRD_ROOF) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3) && isIdle) {
            delay(1000);
            setCameraZoom(483);
            changeCameraYaw(0);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisThirdRoofGap, 10);
            delay(2000);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3) && isIdle) {
            setCameraZoom(595);
            changeCameraYaw(0);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisThirdRoofGap, 10);
        } else if(isAtWorldPoint(new WorldPoint(3487, 3499, 2)) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisThirdRoofGap, 10);
        }
        // 4 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FOURTH_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4) && isIdle) {
            setCameraZoom(751);
            changeCameraYaw(0);
            delay(1000);
            checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FOURTH_ROOF) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            delay(500);
            panCameraOneDirection(KeyEvent.VK_S, 300);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(3478, 3493, 3)));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
        } else if(isAtWorldPoint(new WorldPoint(3478, 3493, 3)) && isIdle) {
            setCameraZoom(840);
            changeCameraYaw(0);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFourthRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4) && isIdle) {
            setCameraZoom(658);
            changeCameraYaw(250);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFourthRoofGap, 8);
        }
        // 5 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIFTH_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(512);
            delay(1000);
            checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5);
            delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIFTH_ROOF) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5) && isIdle) {
            setCameraZoom(729);
            changeCameraYaw(512);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFifthRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5) && isIdle) {
            setCameraZoom(846);
            changeCameraYaw(512);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFifthRoofGap, 10);
            delay(500);
        }
        // 6 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SIXTH_ROOF) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            delay(500);
            panCameraOneDirection(KeyEvent.VK_D, 900);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(3502, 3476, 3)));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
        } else if(isNearWorldTile(new WorldPoint(3502, 3476, 3), 3) && isIdle) {
            setCameraZoom(896);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSixthRoofGap, 10);
            delay(500);
        }
        // 7 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SEVENTH_ROOF) && isIdle) {
            setCameraZoom(458);
            changeCameraYaw(0);
            delay(1000);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSeventhRoofGap, 8);
            delay(1000);
//            pressKey(KeyEvent.VK_DOWN, 2000);
//            delay(500);
        } else if(isAtWorldPoint(new WorldPoint(3515, 3486, 0)) && isIdle) {
            client.stopNow();
        }
    }

    private void doSeersAgility() {
        //yaw 29 from finish
        //zoom all way out
        if(checkLevelUp()) {
            pressKey(KeyEvent.VK_SPACE);
            delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FAIL1) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(800);
            delay(500);
            panCameraToSeersStartFromFail1();
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(500);
            pressKey(KeyEvent.VK_DOWN, 2000);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FAIL2) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(800);
            delay(500);
            panCameraToSeersStartFromFail2();
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(500);
            pressKey(KeyEvent.VK_DOWN, 2000);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIRST_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1)) {
            changeCameraYaw(0);
            setCameraZoom(896);
            delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1);
        } else if(isNearWorldTile(new WorldPoint(2721, 3494, 3), 3) && isIdle) {
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFirstRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIRST_ROOF) && isIdle && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1)) {
            changeCameraYaw(0);
            setCameraZoom(365);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFirstRoofGap, 10);
            delay(9000);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1, 2) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(444);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFirstRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1)) {
            changeCameraYaw(0);
            setCameraZoom(483);
            delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1);
        }
        else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1) && isIdle) {
            changeCameraYaw(1024);
            setCameraZoom(660);
            delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10);
        }
        else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1, 2) && isIdle && !isAtWorldPoint(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1)) {
            changeCameraYaw(1024);
            setCameraZoom(590);
            delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2)) {
            changeCameraYaw(0);
            setCameraZoom(680);
            delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2, 2) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(581);
            delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10);
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle)) {
            changeCameraYaw(0);
            setCameraZoom(582);
            delay(1000);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10);
        }
        // right before 2nd roof Tightrope (case of misclick)
        else if(isNearWorldTile(new WorldPoint(2710, 3490, 2), 3) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(896);
            delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_THIRD_ROOF) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3) && isIdle) {
            setCameraZoom(542);
            changeCameraYaw(0);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersThirdRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_THIRD_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(896);
            delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3)  && isIdle) {
            setCameraZoom(550);
            changeCameraYaw(0);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersThirdRoofGap, 10);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_FOURTH_ROOF, 4) && isIdle && client.getOculusOrbState() == 0) {
            changeCameraYaw(0);
            setCameraZoom(600);
            panCameraToSeersFourthRoofGap();
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(2702, 3470, 3)));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_FOURTH_ROOF_RUN_POINT, 2) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(896);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFourthRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIFTH_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5)) {
            changeCameraYaw(0);
            setCameraZoom(670);
            delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5, 2) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(562);
            delay(500);
            scheduledGameObjectPointDelay(new Point(895, 575), AgilityPlusObjectIDs.seersFifthRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIFTH_ROOF) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(896);
            changeCameraYaw(512);
            delay(500);
            scheduledGameObjectPointDelay(new Point(350, 742), AgilityPlusObjectIDs.seersFifthRoofGap, 12);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FINISH) && isIdle && client.getOculusOrbState() == 0) {
            changeCameraYaw(0);
            panCameraToSeersStartFromFinish();
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(500);
            pressKey(KeyEvent.VK_DOWN, 2000);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_START, 4) && isIdle) {
            setCameraZoom(768);
            changeCameraYaw(1928);
            delay(500);
            scheduledDecorativeObjectDelay(AgilityPlusObjectIDs.seersStartWall, 10);
            delay(500);
            pressKey(KeyEvent.VK_UP, 2000);
        } else {
            delay(500);
        }
    }

    private void doRellekaAgility() {
        if(checkLevelUp()) {
            pressKey(KeyEvent.VK_SPACE);
            delay(500);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.RELLEKA_START, 2) && isIdle) {
            setCameraZoom(1004);
            changeCameraYaw(1024);
            delay(500);
            scheduledDecorativeObjectDelay(AgilityPlusObjectIDs.rellekaStartWall, 8);
            pressKey(KeyEvent.VK_UP, 2000);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FIRST_ROOF) && isIdle && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3)) {
            changeCameraYaw(0);
            setCameraZoom(404);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFirstRoofGap, 10);
        }

        else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FIRST_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3)) {
            changeCameraYaw(0);
            setCameraZoom(896);
            delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3);
            delay(500);
        }

        else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(404);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFirstRoofGap, 10);
        }

        else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_SECOND_ROOF) && isIdle) {
            changeCameraYaw(1024);
            setCameraZoom(340);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaSecondRoofGap, 8);
            delay(500);
        } else if(isAtWorldPoint(new WorldPoint(2622, 3658, 3)) && isIdle) {
            changeCameraYaw(1024);
            setCameraZoom(896);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaSecondRoofGap, 10);
            delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_THIRD_ROOF) && isIdle && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1_1) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1_2)) {
            delay(500);
            changeCameraYaw(1024);
            setCameraZoom(547);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaThirdRoofGap, 10);
            delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_THIRD_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1_1)) {
            delay(500);
            changeCameraYaw(1024);
            setCameraZoom(896);
            delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1_1);
            delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_THIRD_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1_2)) {
            delay(500);
            changeCameraYaw(1024);
            setCameraZoom(896);
            delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1_2);
            delay(1500);
        } else if(isNearWorldTile(new WorldPoint(2629, 3652, 3), 2) && isIdle) {
            changeCameraYaw(1024);
            setCameraZoom(591);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaThirdRoofGap, 10);
        }

        else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FOURTH_ROOF) && isIdle && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK2_1) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK2_2)) {
            changeCameraYaw(1024);
            setCameraZoom(547);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFourthRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FOURTH_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK2_1)) {
            changeCameraYaw(1024);
            setCameraZoom(738);
            delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK2_1);
            delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FOURTH_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK2_2)) {
            changeCameraYaw(1024);
            setCameraZoom(768);
            delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK2_2);
            delay(1500);
        } else if(isNearWorldTile(new WorldPoint(2642,3650, 3), 2) && isIdle) {
            changeCameraYaw(1024);
            setCameraZoom(591);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFourthRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FIFTH_ROOF) && isIdle) {
            changeCameraYaw(1536);
            setCameraZoom(510);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFifthRoofGap, 8);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_SIXTH_ROOF) && isIdle) {
            delay(1000);
            changeCameraYaw(1536);
            setCameraZoom(432);
            delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaSixthRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FINISH) && isIdle) {
            changeCameraYaw(1024);
            setCameraZoom(896);
            delay(500);
            panCameraOneDirection(KeyEvent.VK_D, 1700);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_START));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            pressKey(KeyEvent.VK_DOWN, 2000);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FAIL1) && isIdle) {
            changeCameraYaw(1024);
            setCameraZoom(896);
            delay(2000);
            panCameraOneDirection(KeyEvent.VK_S, 1200);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_START));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            pressKey(KeyEvent.VK_DOWN, 2000);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FAIL2) && isIdle) {
            changeCameraYaw(1024);
            setCameraZoom(896);
            delay(2000);
            panCameraOneDirection(KeyEvent.VK_D, 500);
            delay(500);
            panCameraOneDirection(KeyEvent.VK_S, 1200);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_START));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            pressKey(KeyEvent.VK_DOWN, 2000);
        }
    }

    private Point generatePointsFromPoint(Point point, int sigma) {
        List<Point> points = new ArrayList<>();

        while(points.size() < 3) {
            Point newPoint = MouseCoordCalculation.randomCoord(point, sigma);
            points.add(MouseCoordCalculation.randomCoord(newPoint, sigma));
        }

        return MouseCoordCalculation.randomClusterPicker(points);
    }

    private void getObstacleCenter(GameObject gameObject, int sigma) {
        Shape groundObjectConvexHull = gameObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, gameObject, sigma);
    }

    private void getObstacleCenter(GroundObject groundObject, int sigma) {
        Shape groundObjectConvexHull = groundObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, groundObject, sigma);
    }

    private void getObstacleCenter(DecorativeObject decorativeObject, int sigma) {
        Shape groundObjectConvexHull = decorativeObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, decorativeObject, sigma);
    }

    private Point getCenterOfRectangle(Rectangle rectangle) {
        // +26 to the Y coordinate because calculations are taken from canvas, not window
        return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY() + 26);
    }

    private boolean isAtWorldPoint(WorldPoint worldPoint) {
        boolean playerX = client.getLocalPlayer().getWorldLocation().getX() == worldPoint.getX();
        boolean playerY = client.getLocalPlayer().getWorldLocation().getY() == worldPoint.getY();
        boolean playerPlane = client.getLocalPlayer().getWorldLocation().getPlane() == worldPoint.getPlane();
        return playerX && playerY && playerPlane;
    }

    private void changeCameraYaw(int yaw) {
        if(client.getCameraYaw() == yaw)
            return;
        client.setCameraYawTarget(yaw);
//        north: 0
//        east:1536
//        south:1024
//        west:512
    }

    private void setCameraZoom(int zoom) {
        clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom));
    }

    private void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) { e.getStackTrace(); }
    }

    private void panCameraToCanfisTree() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_S, 800);
        pressKey(KeyEvent.VK_D, 1500);
    }

    private void panCameraToSeersStartFromFail1() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_S, 550);
        pressKey(KeyEvent.VK_D, 650);
    }

    private void panCameraToSeersStartFromFail2() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_D, 1200);
    }

    private void panCameraToSeersStartFromFinish() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_D, 1630);
        pressKey(KeyEvent.VK_W, 1200);
        delay(1000);
    }

    private void panCameraToSeersFourthRoofGap() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        delay(1000);
        pressKey(KeyEvent.VK_A, 600);
    }

    private void panCameraOneDirection(int keyEvent, int ms) {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(keyEvent, ms);
    }

    private boolean checkLevelUp() {
        Widget levelUpMessage = client.getWidget(10617391);
        return !levelUpMessage.isSelfHidden();
    }

//    private void moveMouse(int x, int y) {
//        MouseEvent mousePress = new MouseEvent(this.client.getCanvas(), MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, y, 1, false);
//        this.client.getCanvas().dispatchEvent(mousePress);
//
//        MouseEvent mouseRelease = new MouseEvent(this.client.getCanvas(), MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, x, y, 1, false);
//        this.client.getCanvas().dispatchEvent(mouseRelease);
//    }

    private void pressKey(int key) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyRelease);
    }

    private void pressKey(int key, int ms) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyPress);

        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key);
        delay(ms);
        this.client.getCanvas().dispatchEvent(keyRelease);
    }

    private void scheduledGroundObjectDelay(GroundObject groundObject, int sigma) {
        isIdle = false;
        try {
            getObstacleCenter(groundObject, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    private void scheduledGameObjectDelay(GameObject gameObject, int sigma) {
        isIdle = false;
        try {
            getObstacleCenter(gameObject, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    private void scheduledDecorativeObjectDelay(DecorativeObject decorativeObject, int sigma) {
        isIdle = false;
        try {
            getObstacleCenter(decorativeObject, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    private void scheduledGameObjectPointDelay(Point point, GameObject gameObject, int sigma) {
        isIdle = false;
        try {
            Point generatedPoint = generatePointsFromPoint(point, sigma);
            MouseCoordCalculation.generateCoord(generatedPoint, gameObject, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    //possible use would be a ground item that has no clickbox (doubt that will happen)
    private void scheduledGroundObjectPointDelay(Point point, GroundObject groundObject, int sigma) {
        isIdle = false;
        try {
            MouseCoordCalculation.generateCoord(point, groundObject, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    private void scheduledPointDelay(Point point, int sigma) {
        isIdle = false;
        try {
            MouseCoordCalculation.generateCoord(point, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    private void getWorldPointCoords(final LocalPoint dest) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, dest, 1);
        if (poly == null)
        {
            return;
        }

        Rectangle boundingBox = poly.getBounds();

        Point obstacleCenter = getCenterOfRectangle(boundingBox);

        scheduledPointDelay(obstacleCenter, 10);
    }

    private boolean isNearWorldTile(final WorldPoint target, final int range) {
        return this.client.getLocalPlayer().getWorldLocation().distanceTo2D(target) < range
                && client.getLocalPlayer().getWorldLocation().getPlane() == target.getPlane();
    }

    private void checkGracefulmark(WorldPoint worldpoint) {
        if(doesWorldPointHaveGracefulMark(worldpoint))
            getWorldPointCoords(LocalPoint.fromWorld(client, worldpoint));
    }

    private boolean doesWorldPointHaveGracefulMark(WorldPoint worldpoint) {
        Tile[][][] sceneTiles = client.getScene().getTiles();
        if(sceneTiles == null) return false;

        int startX = sceneTiles[0][0][0].getWorldLocation().getX();
        int startY = sceneTiles[0][0][0].getWorldLocation().getY();

        int playerPlane = client.getLocalPlayer().getWorldLocation().getPlane();
        List<TileItem> itemsOnTile = sceneTiles[playerPlane][worldpoint.getX()-startX][worldpoint.getY()-startY].getGroundItems();
        List<Integer> tileItemIds = new ArrayList<>();
        if(itemsOnTile != null)
            itemsOnTile.forEach(tileItem -> tileItemIds.add(tileItem.getId()));

        return tileItemIds.contains(ItemID.MARK_OF_GRACE);
    }

    private boolean checkIdle()
    {
        int idleClientTicks = client.getKeyboardIdleTicks();

        if (client.getMouseIdleTicks() < idleClientTicks)
        {
            idleClientTicks = client.getMouseIdleTicks();
        }

        return idleClientTicks >= 1600;
    }

    private void reset() {
        System.out.println("idle for too long, reset");
        isIdle = true;
        resetOculusOrb = true;
        delay(200);
        start = System.currentTimeMillis();
    }

    private boolean checkLastReset() {
        long end = System.currentTimeMillis();
        double sec = (end - start) / 1000F;

        return sec >= 10;
    }

    private boolean isNotHealthly() {
        Widget hpOrbWidget = client.getWidget(10485769);
        if(hpOrbWidget != null) {
            String hpOrbText = hpOrbWidget.getText();
            if(hpOrbText != null) {
                if(!hpOrbText.isEmpty()) {
                    return Integer.parseInt(hpOrbText) < 10;
                }
            }
        }
        return false;
    }
//
////    private void eatFood() {
////        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
////        if(inventoryWidget != null) {
////            ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
////            if(inventory.contains(ItemID.SALMON) || inventory.contains(ItemID.CAKE) || inventory.contains(ItemID._23_CAKE) || inventory.contains(ItemID.SLICE_OF_CAKE)) {
////                List<Item> inventoryList = new ArrayList<>(Arrays.asList(inventory.getItems()));
////                inventoryList.stream().findFirst().get(
////            }
////        } else {
////            pressKey(KeyEvent.VK_ESCAPE);
////        }
////    }
//
}
