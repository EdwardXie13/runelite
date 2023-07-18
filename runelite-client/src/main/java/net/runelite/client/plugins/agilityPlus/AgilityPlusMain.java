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

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Robot;
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
    Robot robot = new Robot();
    private int healthyThreshold = 1;
    public static boolean isRunning = false;

    Thread t;

    AgilityPlusMain(Client client, ClientThread clientThread) throws AWTException {
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
        while (isRunning) {
            if (checkIdle() && checkLastReset())
                reset();

            if(isNotHealthly())
                robot.delay(1000);
            else if (getRegionID() == 9781)
                doGnomeAgility();
            else if (getRegionID() == 13878)
                doCanfisAgility();
            else if (getRegionID() == 10806)
                doSeersAgility();
            else if (getRegionID() == 10553 || getRegionID() == 10297)
                doRellekaAgility();
        }
        System.out.println("Thread has stopped.");
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private void doGnomeAgility() {
        if(turnRunOn()) {
            robot.delay(500);
            scheduledPointDelay(new Point(804, 157), 4);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.GNOME_START) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeLogBalance, 10);
            robot.delay(500);
            client.setCameraPitchTarget(0);
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_LOG) && isIdle) {
            setCameraZoom(400);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstacleNet1_M, 15);
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB1) && isIdle) {
            robot.delay(1000);
            setCameraZoom(896);
            client.setCameraPitchTarget(150);
            changeCameraYaw(220);
            robot.delay(1000);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeTreeBranch1, 10);
            robot.delay(500);
            client.setCameraPitchTarget(512);
            robot.delay(1000);
        }
        else if (isAtWorldPoint(new WorldPoint(2471, 3424, 1)) && isIdle) {
            robot.delay(1000);
            setCameraZoom(752);
            client.setCameraPitchTarget(125);
            changeCameraYaw(225);
            robot.delay(1000);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeTreeBranch1, 10);
            robot.delay(500);
            client.setCameraPitchTarget(512);
            robot.delay(1000);
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB2) && isIdle) {
            setCameraZoom(140);
            client.setCameraPitchTarget(20);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeBalancingRope, 10);
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_TIGHTROPE) && isIdle) {
            setCameraZoom(520);
            client.setCameraPitchTarget(70);
            changeCameraYaw(322);
            robot.delay(500);
            scheduledGameObjectPointDelay(new Point(825, 799), AgilityPlusObjectIDs.gnomeTreeBranch2, 10);
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_DROP) && isIdle) {
            setCameraZoom(-47);
            client.setCameraPitchTarget(90);
//            client.setCameraPitchTarget(205);
            changeCameraYaw(1185);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstacleNet2_L, 15);
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_NET1) && isIdle) {
            setCameraZoom(495);
            client.setCameraPitchTarget(100);
            changeCameraYaw(1020);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstaclePipeLeft, 15);
            robot.delay(500);
            client.setCameraPitchTarget(512);
            robot.delay(8000); //adjust as needed
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_LEFT_PIPE) && isIdle) {
            setCameraZoom(896);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraOneDirection(KeyEvent.VK_A, 600);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.GNOME_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        }
    }

    private void doCanfisAgility() {
        healthyThreshold = 9;
        if(turnRunOn()) {
            robot.delay(500);
            scheduledPointDelay(new Point(804, 157), 4);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_START) && isIdle) {
            pressKey(KeyEvent.VK_DOWN, 2000);
            setCameraZoom(783);
            changeCameraYaw(42);
            robot.delay(500);
            scheduledPointDelay(new Point(70, 162), 10);
            robot.delay(500);
            client.setCameraPitchTarget(512);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FAIL1) && isIdle) {
            robot.delay(1000);
            setCameraZoom(896);
            robot.delay(500);
            panCameraToCanfisStart();
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.CANFIS_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FAIL2) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToCanfisStart2();
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.CANFIS_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
      }
        // 1 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIRST_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1) && isIdle) {
            setCameraZoom(758);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(1000);
            checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIRST_ROOF) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1) && isIdle) {
            setCameraZoom(336);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFirstRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1) && isIdle) {
            setCameraZoom(679);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFirstRoofGap, 10);
            robot.delay(2000);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.CANFIS_FIRST_ROOF_INFRONT, 2) && isIdle) {
            setCameraZoom(800);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFirstRoofGap, 10);
            robot.delay(500);
        }
        // 2 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SECOND_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            robot.delay(1000);
            checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SECOND_ROOF) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2) && isIdle) {
            setCameraZoom(540);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSecondRoofGap, 10);
            robot.delay(3500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2) && isIdle) {
            setCameraZoom(597);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSecondRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SECOND_ROOF_INFRONT) && isIdle) {
            setCameraZoom(921);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSecondRoofGap, 10);
        }
        // 3 roof
        else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_THIRD_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3) && isIdle) {
            robot.delay(1000);
            setCameraZoom(751);
            changeCameraYaw(0);
            robot.delay(1000);
            checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_THIRD_ROOF) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3) && isIdle) {
            robot.delay(1000);
            setCameraZoom(483);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisThirdRoofGap, 10);
            robot.delay(2000);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3) && isIdle) {
            setCameraZoom(595);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisThirdRoofGap, 10);
        } else if (isAtWorldPoint(new WorldPoint(3487, 3499, 2)) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisThirdRoofGap, 10);
        }
        // 4 roof
        else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FOURTH_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4) && isIdle) {
            setCameraZoom(751);
            changeCameraYaw(0);
            robot.delay(1000);
            checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4);
            robot.delay(1000);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FOURTH_ROOF) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4) && isIdle) {
//            setCameraZoom(896);
            setCameraZoom(-47);
            client.setCameraPitchTarget(160);
            changeCameraYaw(0);
            robot.delay(1000);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFourthRoofGap, 10);
            robot.delay(4700);
        } else if (isAtWorldPoint(new WorldPoint(3477, 3492, 3)) && isIdle) {
            setCameraZoom(1004);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFourthRoofGap, 10);
            robot.delay(3000);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4) && isIdle) {
            setCameraZoom(230);
            client.setCameraPitchTarget(0);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFourthRoofGap, 8);
            robot.delay(5000);
        }
        // 5 roof
        else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIFTH_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5) && isIdle) {
            setCameraZoom(896);
            client.setCameraPitchTarget(512);
            changeCameraYaw(512);
            robot.delay(1000);
            checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5);
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIFTH_ROOF) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5) && isIdle) {
            setCameraZoom(729);
            client.setCameraPitchTarget(512);
            changeCameraYaw(512);
            robot.delay(2500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFifthRoofGap, 10);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5) && isIdle) {
            setCameraZoom(846);
            client.setCameraPitchTarget(512);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFifthRoofGap, 10);
            robot.delay(2500);
        }
        // 6 roof
        else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SIXTH_ROOF) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraOneDirection(KeyEvent.VK_D, 900);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(3502, 3476, 3)));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
        } else if (isNearWorldTile(new WorldPoint(3502, 3476, 3), 3) && isIdle) {
            setCameraZoom(896);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSixthRoofGap, 10);
            robot.delay(500);
        }
        // 7 roof
        else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SEVENTH_ROOF) && isIdle) {
            setCameraZoom(458);
            changeCameraYaw(0);
            robot.delay(1000);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSeventhRoofGap, 8);
            robot.delay(5000);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SEVENTH_ROOF_INFRONT) && isIdle) {
            setCameraZoom(896);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSeventhRoofGap, 10);
            robot.delay(1000);
        }

        // some random misclick tile
        else if (isAtWorldPoint(new WorldPoint(3515, 3486, 0)) && isIdle) {
            client.stopNow();
        }
    }

    private void doSeersAgility() {
        healthyThreshold = 9;
        if(turnRunOn()) {
            robot.delay(500);
            scheduledPointDelay(new Point(804, 157), 4);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FAIL1) && isIdle) {
            setCameraZoom(1004);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToSeersStartFromFail1();
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FAIL2) && isIdle) {
            setCameraZoom(1004);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToSeersStartFromFail2();
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIRST_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1)) {
            setCameraZoom(896);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1);
            robot.delay(500);
        } else if(isNearWorldTile(new WorldPoint(2721, 3494, 3), 3) && isIdle) {
            setCameraZoom(896);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFirstRoofGap, 10);
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIRST_ROOF) && isIdle && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1)) {
            setCameraZoom(362);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFirstRoofGap, 10);
            robot.delay(9000);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1, 2) && isIdle) {
            setCameraZoom(-47);
            client.setCameraPitchTarget(175);
            changeCameraYaw(1535);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFirstRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1)) {
            setCameraZoom(483);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1) && isIdle) {
            setCameraZoom(23);
            client.setCameraPitchTarget(52);
            changeCameraYaw(242);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10);
            robot.delay(3500);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1, 2) && isIdle && !isAtWorldPoint(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1)) {
            changeCameraYaw(1024);
            setCameraZoom(590);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2)) {
            changeCameraYaw(0);
            client.setCameraPitchTarget(512);
            setCameraZoom(680);
            robot.delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2, 2) && isIdle) {
            setCameraZoom(245);
            client.setCameraPitchTarget(43);
            changeCameraYaw(85);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10);
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle)) {
            changeCameraYaw(0);
            client.setCameraPitchTarget(512);
            setCameraZoom(577);
            robot.delay(1000);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10);
        }
        // right before 2nd roof Tightrope (case of misclick)
        else if(isNearWorldTile(new WorldPoint(2710, 3490, 2), 3) && isIdle) {
            setCameraZoom(896);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_THIRD_ROOF) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3) && isIdle) {
            setCameraZoom(404);
            client.setCameraPitchTarget(48);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledPointDelay(new Point(468, 799), 10);
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_THIRD_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(896);
            robot.delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3)  && isIdle) {
            setCameraZoom(261);
            client.setCameraPitchTarget(44);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledPointDelay(new Point(470, 759), 10);
            robot.delay(1500);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_FOURTH_ROOF, 4) && isIdle && client.getOculusOrbState() == 0) {
            setCameraZoom(255);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFourthRoofGap, 5);
            robot.delay(7000);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_FOURTH_ROOF_RUN_POINT, 2) && isIdle) {
            setCameraZoom(896);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFourthRoofGap, 10);
        } else if(isAtWorldPoint(new WorldPoint(2704, 3470, 3)) && isIdle) {
            setCameraZoom(896);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFourthRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIFTH_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5)) {
            setCameraZoom(670);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5, 2) && isIdle) {
            setCameraZoom(226);
            client.setCameraPitchTarget(48);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledPointDelay(new Point(296, 768), 10);
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIFTH_ROOF) && isIdle) {
            setCameraZoom(704);
            client.setCameraPitchTarget(48);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledPointDelay(new Point(324, 738), 10);
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FINISH) && isIdle && client.getOculusOrbState() == 0) {
            robot.delay(1500);
            setCameraZoom(1004);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            panCameraToSeersStartFromFinish();
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_START) && isIdle) {
            robot.delay(500);
            setCameraZoom(760);
            client.setCameraPitchTarget(80);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledPointDelay(new Point(478, 666), 10);
            robot.delay(500);
        }
    }

    private void doRellekaAgility() {
        healthyThreshold = 9;
        if(turnRunOn()) {
            robot.delay(500);
            scheduledPointDelay(new Point(804, 157), 4);
            robot.delay(500);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.RELLEKA_START, 2) && isIdle) {
            System.out.println("relleka start");
            robot.delay(500);
            setCameraZoom(1004);
            client.setCameraPitchTarget(75);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledDecorativeObjectDelay(AgilityPlusObjectIDs.rellekaStartWall, 10);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FIRST_ROOF) && isIdle && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1)) {
            System.out.println("relleka 1st roof");
            robot.delay(500);
            changeCameraYaw(0);
            client.setCameraPitchTarget(512);
            setCameraZoom(390);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFirstRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FIRST_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1)) {
            System.out.println("relleka pick up GM 1");
            robot.delay(500);
            changeCameraYaw(0);
            client.setCameraPitchTarget(512);
            setCameraZoom(850);
            robot.delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1) && isIdle) {
            System.out.println("relleka 1st roof");
            changeCameraYaw(0);
            client.setCameraPitchTarget(512);
            setCameraZoom(460);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFirstRoofGap, 10);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_SECOND_ROOF) && isIdle) {
            System.out.println("relleka 2ndd roof");
            robot.delay(1000);
            setCameraZoom(1004);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            panCameraOneDirection(KeyEvent.VK_S, 550);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_SECOND_ROOF_RUN_POINT));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_SECOND_ROOF_RUN_POINT) && isIdle) {
            System.out.println("relleka 2nd roof run point");
            robot.delay(500);
            setCameraZoom(1004);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaSecondRoofGap, 10);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_THIRD_ROOF) && isIdle && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_1) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_2)) {
            System.out.println("relleka 3rd roof");
            robot.delay(500);
            setCameraZoom(540);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaThirdRoofGap, 10);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_THIRD_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_1)) {
            System.out.println("relleka pick up GM 3_1");
            robot.delay(500);
            setCameraZoom(880);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_1);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_THIRD_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_2)) {
            System.out.println("relleka pick up GM 3_2");
            robot.delay(500);
            setCameraZoom(880);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_2);
            robot.delay(500);
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_1) || isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_2)) && isIdle) {
            System.out.println("relleka pick up GM 4_2");
            robot.delay(500);
            setCameraZoom(570);
            client.setCameraPitchTarget(512);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaThirdRoofGap, 10);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FOURTH_ROOF) && isIdle && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_1) && !doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_2)) {
            System.out.println("relleka 4th roof");
            robot.delay(500);
            setCameraZoom(540);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFourthRoofGap, 10);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FOURTH_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_1)) {
            System.out.println("relleka pick up GM 4_1");
            robot.delay(500);
            setCameraZoom(715);
            client.setCameraPitchTarget(512);
            changeCameraYaw(1024);
            robot.delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_1);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FOURTH_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_2)) {
            System.out.println("relleka pick up GM 4_2");
            robot.delay(500);
            setCameraZoom(715);
            client.setCameraPitchTarget(512);
            changeCameraYaw(1024);
            robot.delay(500);
            checkGracefulmark(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_2);
            robot.delay(500);
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_1) || isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_2)) && isIdle) {
            System.out.println("relleka 4th roof");
            robot.delay(500);
            setCameraZoom(570);
            client.setCameraPitchTarget(512);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFourthRoofGap, 10);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FIFTH_ROOF) && isIdle) {
            System.out.println("relleka 5th roof");
            // TODO: multiple ways
            // 1. face east
            robot.delay(500);
            setCameraZoom(500);
            client.setCameraPitchTarget(512);
            changeCameraYaw(1536);
            robot.delay(500);
//            // 2. face south
//            changeCameraYaw(1024);
//            setCameraZoom(500);
//            // 3. pan to tile (2647, 3662, 3) then click the gap
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFifthRoofGap, 8);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_SIXTH_ROOF) && isIdle) {
            System.out.println("relleka 6th roof");
            robot.delay(1000);
            setCameraZoom(450);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaSixthRoofGap, 10);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FINISH) && isIdle) {
            System.out.println("relleka finish");
            robot.delay(500);
            setCameraZoom(1004);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraOneDirection(KeyEvent.VK_A, 1700);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FAIL1) && isIdle) {
            System.out.println("relleka fail 1");
            robot.delay(500);
            setCameraZoom(1004);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraOneDirection(KeyEvent.VK_W, 1200);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FAIL2) && isIdle) {
            System.out.println("relleka fail 2");
            robot.delay(500);
            setCameraZoom(1004);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToRellekaStartFromFail2();
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
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

        MouseCoordCalculation.generateCoord(client, obstacleCenter, gameObject, sigma);
    }

    private void getObstacleCenter(GroundObject groundObject, int sigma) {
        Shape groundObjectConvexHull = groundObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(client, obstacleCenter, groundObject, sigma);
    }

    private void getObstacleCenter(DecorativeObject decorativeObject, int sigma) {
        Shape groundObjectConvexHull = decorativeObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(client, obstacleCenter, decorativeObject, sigma);
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

    private synchronized void panCameraToCanfisStart() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_S, 900);
        pressKey(KeyEvent.VK_D, 1600);
    }

    private synchronized void panCameraToCanfisStart2() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_D, 300);
        pressKey(KeyEvent.VK_W, 400);
    }

    private synchronized void panCameraToSeersStartFromFail1() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_S, 500);
        pressKey(KeyEvent.VK_D, 800);
    }

    private synchronized void panCameraToSeersStartFromFail2() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_W, 300);
        pressKey(KeyEvent.VK_D, 1100);
    }

    private synchronized void panCameraToSeersStartFromFinish() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_D, 1700);
        pressKey(KeyEvent.VK_W, 1400);
        robot.delay(1000);
    }

    private synchronized void panCameraToRellekaStartFromFail2() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_A, 600);
        pressKey(KeyEvent.VK_W, 1100);
    }

    private synchronized void panCameraOneDirection(int keyEvent, int ms) {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(keyEvent, ms);
    }

    private synchronized void pressKey(int key, int ms) {
        robot.keyPress(key);
        robot.delay(ms);
        robot.keyRelease(key);
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
            MouseCoordCalculation.generateCoord(client, generatedPoint, gameObject, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    private void scheduledPointDelay(Point point, int sigma) {
        isIdle = false;
        try {
            MouseCoordCalculation.generateCoord(client, point, sigma);
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
        client.setOculusOrbState(0);
        client.setOculusOrbNormalSpeed(12);
        isIdle = true;
        resetOculusOrb = true;
        robot.delay(200);
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
                    return Integer.parseInt(hpOrbText) < healthyThreshold;
                }
            }
        }
        return false;
    }

    private boolean turnRunOn() {
        Widget runWidget = client.getWidget(10485789);
        Widget stamina = client.getWidget(10485788);

        if(runWidget != null && stamina != null) {
            // 1065 is on
            // 1064 is off
            int runWidgetSpriteId =  runWidget.getSpriteId();
            String staminaEnergy = stamina.getText();
            return runWidgetSpriteId == 1064 && Integer.parseInt(staminaEnergy) == 100;
        }

        return false;
    }
}
