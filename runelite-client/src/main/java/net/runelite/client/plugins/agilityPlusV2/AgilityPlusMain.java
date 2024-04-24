package net.runelite.client.plugins.agilityPlusV2;

import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Perspective;
import net.runelite.api.ScriptID;
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
    public static boolean grabClickBox = false;

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
            if(checkIdle() && checkLastReset())
                reset();

            if(getRegionID() == 9781)
                doGnomeAgility();
            else if(getRegionID() == 13878)
                doCanfisAgility();
            else if(getRegionID() == 12597 || getRegionID() == 12853)
                doVarrockAgility();
            else if(getRegionID() == 12084)
                doFaladorAgility();
            else if(getRegionID() == 10806)
                doSeersAgility();
            else if(getRegionID() == 10553 || getRegionID() == 10297)
                doRellekaAgility();
            else if(getRegionID() == 10547)
                doArdyAgility();
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
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeLogBalance);
//            grabClickBox = true;
//            scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeLogBalance, 10);
            robot.delay(1000);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_LOG) && isIdle) {
            setCameraZoom(400);
            changeCameraPitch(0);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstacleNet1_M);
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB1) && isIdle) {
            robot.delay(1000);
            setCameraZoom(896);
            changeCameraPitch(150);
            changeCameraYaw(220);
            robot.delay(1000);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeTreeBranch1);
            robot.delay(500);
            changeCameraPitch(512);
            robot.delay(1000);
        } else if (isAtWorldPoint(new WorldPoint(2471, 3424, 1)) && isIdle) {
            robot.delay(1000);
            setCameraZoom(752);
            changeCameraPitch(125);
            changeCameraYaw(225);
            robot.delay(1000);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeTreeBranch1);
            robot.delay(500);
            changeCameraPitch(512);
            robot.delay(1000);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB2) && isIdle) {
            setCameraZoom(140);
            changeCameraPitch(20);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeBalancingRope);
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_TIGHTROPE) && isIdle) {
            setCameraZoom(520);
            changeCameraPitch(70);
            changeCameraYaw(322);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeTreeBranch2);
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_DROP) && isIdle) {
            setCameraZoom(-47);
            changeCameraPitch(90);
            changeCameraYaw(1185);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstacleNet2_L);
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_NET1) && AgilityPlusObjectIDs.gnomeObstaclePipeLeft != null && isIdle) {
            setCameraZoom(495);
            changeCameraPitch(100);
            changeCameraYaw(1020);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstaclePipeLeft);
            robot.delay(500);
            changeCameraPitch(512);
            robot.delay(8000); //adjust as needed
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_NET1) && AgilityPlusObjectIDs.gnomeObstaclePipeRight != null && isIdle) {
            setCameraZoom(285);
            changeCameraPitch(118);
            changeCameraYaw(757);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstaclePipeRight);
            robot.delay(1000);
            changeCameraPitch(512);
            robot.delay(8000); //adjust as needed
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_LEFT_PIPE) &&  isIdle) {
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraOneDirection(KeyEvent.VK_A, 600);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.GNOME_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_RIGHT_PIPE) &&  isIdle) {
            robot.delay(1000);
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraOneDirection(KeyEvent.VK_A, 700);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.GNOME_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        }
    }

    private void doVarrockAgility() {
        healthyThreshold = 4;
        if(turnRunOn()) {
            robot.delay(500);
            scheduledPointDelay(new Point(804, 157), 4);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_FAIL1) && isIdle) {
            robot.delay(1000);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraOneDirection(KeyEvent.VK_D, 500);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.VARROCK_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_FAIL2) && isIdle) {
            robot.delay(1000);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToVarrockStartFromFail2();
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.VARROCK_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_START) && isIdle) {
            robot.delay(500);
            setCameraZoom(830);
            changeCameraPitch(30);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledDecorativeObjectDelay(AgilityPlusObjectIDs.varrockStart);
        } else if(isNotHealthly()) {
            System.out.println("waiting to heal");
            robot.delay(1000);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_FIRST_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(500);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.varrockFirstRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_SECOND_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(370);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.varrockSecondRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_THIRD_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(500);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.varrockThirdRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_FOURTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(495);
            changeCameraPitch(512);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.varrockFourthRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_FIFTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(155);
            changeCameraPitch(512);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.varrockFifthRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_SIXTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(640);
            changeCameraPitch(72);
            changeCameraYaw(1536);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.varrockSixthRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_SEVENTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(320);
            changeCameraPitch(234);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.varrockSeventhRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_EIGHTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(-45);
            changeCameraPitch(116);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.varrockEighthRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_FINISH) && isIdle) {
            robot.delay(500);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToVarrockStartFromFinish();
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.VARROCK_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        }
    }

    private void doFaladorAgility() {
        healthyThreshold = 4;
        if(turnRunOn()) {
            robot.delay(500);
            scheduledPointDelay(new Point(804, 157), 4);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_FAIL1) && isIdle) {
            robot.delay(1000);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToFaladorStartFromFail1();
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.FALADOR_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_FAIL2) && isIdle) {
            robot.delay(1000);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToFaladorStartFromFail2();
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.FALADOR_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_START) && isIdle) {
            robot.delay(1500);
            setCameraZoom(1004);
            changeCameraPitch(100);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledDecorativeObjectDelay(AgilityPlusObjectIDs.faladorStart);
            robot.delay(500);
        } else if(isNotHealthly()) {
            System.out.println("waiting to heal");
            robot.delay(1000);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_FIRST_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(635);
            changeCameraPitch(512);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.faladorFirstRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_SECOND_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(230);
            changeCameraPitch(172);
            changeCameraYaw(1536);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.faladorSecondRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_THIRD_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(574);
            changeCameraPitch(42);
            changeCameraYaw(1313);
            robot.delay(500);
            scheduledPointDelay(new Point(355, 630), 8);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_FOURTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(625);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.faladorFourthRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_FIFTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(-72);
            changeCameraPitch(103);
            changeCameraYaw(1536);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.faladorFifthRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_SIXTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(862);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.faladorSixthRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_SEVENTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(295);
            changeCameraPitch(214);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.faladorSeventhRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_EIGHTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(174);
            changeCameraPitch(91);
            changeCameraYaw(1975);
            robot.delay(500);
            scheduledPointDelay(new Point(170, 783), 10);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_NINTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(410);
            changeCameraPitch(224);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.faladorNinthRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_TENTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(730);
            changeCameraPitch(91);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.faladorTenthRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_ELEVENTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(390);
            changeCameraPitch(202);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.faladorEleventhRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_TWELFTH_ROOF) && isIdle) {
            robot.delay(500);
            setCameraZoom(-72);
            changeCameraPitch(77);
            changeCameraYaw(642);
            robot.delay(500);
            scheduledPointDelay(new Point(135, 790), 10);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_FINISH) && isIdle) {
            robot.delay(500);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToFaladorStartFromFinish();
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.FALADOR_START));
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
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FAIL1) && isIdle) {
            robot.delay(1000);
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(0);
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
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToCanfisStart2();
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.CANFIS_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_START) && isIdle) {
            pressKey(KeyEvent.VK_DOWN, 2000);
            setCameraZoom(783);
            changeCameraYaw(42);
            robot.delay(500);
            scheduledPointDelay(new Point(70, 162), 10);
            robot.delay(500);
            changeCameraPitch(512);
            robot.delay(500);
        } else if(isNotHealthly()) {
            System.out.println("waiting to heal");
            robot.delay(1000);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIRST_ROOF) && AgilityPlusWorldPoints.MOG_CANFIS1 && isIdle) {
            setCameraZoom(758);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(1000);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1));
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIRST_ROOF) && !AgilityPlusWorldPoints.MOG_CANFIS1 && isIdle) {
            setCameraZoom(336);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFirstRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1) && isIdle) {
            setCameraZoom(679);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFirstRoofGap);
            robot.delay(2000);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.CANFIS_FIRST_ROOF_INFRONT, 2) && isIdle) {
            setCameraZoom(800);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFirstRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SECOND_ROOF) && AgilityPlusWorldPoints.MOG_CANFIS2 && isIdle) {
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(1000);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2));
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SECOND_ROOF) && !AgilityPlusWorldPoints.MOG_CANFIS2 && isIdle) {
            setCameraZoom(540);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSecondRoofGap);
            robot.delay(3500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2) && isIdle) {
            setCameraZoom(597);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSecondRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SECOND_ROOF_INFRONT) && isIdle) {
            setCameraZoom(921);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSecondRoofGap);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_THIRD_ROOF) && AgilityPlusWorldPoints.MOG_CANFIS3 && isIdle) {
            robot.delay(1000);
            setCameraZoom(751);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(1000);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3));
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_THIRD_ROOF) && !AgilityPlusWorldPoints.MOG_CANFIS3 && isIdle) {
            robot.delay(1000);
            setCameraZoom(483);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisThirdRoofGap);
            robot.delay(2000);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3) && isIdle) {
            setCameraZoom(595);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisThirdRoofGap);
        } else if (isAtWorldPoint(new WorldPoint(3487, 3499, 2)) && isIdle) {
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisThirdRoofGap);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FOURTH_ROOF) && AgilityPlusWorldPoints.MOG_CANFIS4 && isIdle) {
            setCameraZoom(751);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(1000);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4));
            robot.delay(1000);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FOURTH_ROOF) && !AgilityPlusWorldPoints.MOG_CANFIS4 && isIdle) {
            setCameraZoom(-47);
            changeCameraPitch(160);
            changeCameraYaw(0);
            robot.delay(1000);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFourthRoofGap);
            robot.delay(4700);
        } else if (isAtWorldPoint(new WorldPoint(3477, 3492, 3)) && isIdle) {
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFourthRoofGap);
            robot.delay(3000);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4) && isIdle) {
            setCameraZoom(230);
            changeCameraPitch(0);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFourthRoofGap);
            robot.delay(5000);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIFTH_ROOF) && AgilityPlusWorldPoints.MOG_CANFIS5 && isIdle) {
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(512);
            robot.delay(1000);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5));
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIFTH_ROOF) && !AgilityPlusWorldPoints.MOG_CANFIS5 && isIdle) {
            setCameraZoom(729);
            changeCameraPitch(512);
            changeCameraYaw(512);
            robot.delay(2500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFifthRoofGap);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5) && isIdle) {
            setCameraZoom(846);
            changeCameraPitch(512);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFifthRoofGap);
            robot.delay(2500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SIXTH_ROOF) && isIdle) {
            setCameraZoom(896);
            changeCameraPitch(512);
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
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSixthRoofGap);
            robot.delay(500);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SEVENTH_ROOF) && isIdle) {
            setCameraZoom(458);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(1000);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSeventhRoofGap);
            robot.delay(5000);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SEVENTH_ROOF_INFRONT) && isIdle) {
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSeventhRoofGap);
            robot.delay(1000);
        }

        // some random misclick tile
        else if (isAtWorldPoint(new WorldPoint(3515, 3486, 0)) && isIdle) {
            client.stopNow();
        }
    }

    private void doSeersAgility() {
        healthyThreshold = 5;
        if(turnRunOn()) {
            robot.delay(500);
            scheduledPointDelay(new Point(804, 157), 4);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FAIL1) && isIdle) {
            setCameraZoom(1004);
            changeCameraPitch(512);
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
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToSeersStartFromFail2();
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_START) && isIdle) {
            robot.delay(500);
            setCameraZoom(760);
            changeCameraPitch(80);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledPointDelay(new Point(478, 666), 10);
            robot.delay(500);
        } else if(isNotHealthly()) {
            System.out.println("waiting to heal");
            robot.delay(1000);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIRST_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_SEERS1) {
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_GRACEFULMARK1));
            robot.delay(500);
        } else if(isNearWorldTile(new WorldPoint(2721, 3494, 3), 3) && isIdle) {
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFirstRoofGap);
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIRST_ROOF) && isIdle && !AgilityPlusWorldPoints.MOG_SEERS1) {
            setCameraZoom(362);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFirstRoofGap);
            robot.delay(9000);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1, 2) && isIdle) {
            setCameraZoom(-47);
            changeCameraPitch(175);
            changeCameraYaw(1535);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFirstRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_SEERS2_1) {
            setCameraZoom(483);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1));
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1) && isIdle) {
            setCameraZoom(23);
            changeCameraPitch(52);
            changeCameraYaw(242);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope);
            robot.delay(3500);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1, 2) && isIdle && !AgilityPlusWorldPoints.MOG_SEERS2_1) {
            setCameraZoom(590);
            changeCameraPitch(512);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_SEERS2_2) {
            setCameraZoom(680);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2));
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2, 2) && isIdle) {
            setCameraZoom(245);
            changeCameraPitch(43);
            changeCameraYaw(85);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope);
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle)) {
            setCameraZoom(577);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(1000);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope);
        } else if(isNearWorldTile(new WorldPoint(2710, 3490, 2), 3) && isIdle) {
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_THIRD_ROOF) && !AgilityPlusWorldPoints.MOG_SEERS3 && isIdle) {
            setCameraZoom(404);
            changeCameraPitch(48);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledPointDelay(new Point(468, 799), 10);
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_THIRD_ROOF) && AgilityPlusWorldPoints.MOG_SEERS3 && isIdle) {
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_GRACEFULMARK3));
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3)  && isIdle) {
            setCameraZoom(261);
            changeCameraPitch(44);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledPointDelay(new Point(470, 759), 10);
            robot.delay(1500);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_FOURTH_ROOF, 4) && isIdle && client.getOculusOrbState() == 0) {
            setCameraZoom(255);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFourthRoofGap);
            robot.delay(7000);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_FOURTH_ROOF_RUN_POINT, 2) && isIdle) {
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFourthRoofGap);
        } else if(isAtWorldPoint(new WorldPoint(2704, 3470, 3)) && isIdle) {
            setCameraZoom(896);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFourthRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIFTH_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_SEERS5) {
            setCameraZoom(670);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_GRACEFULMARK5));
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5, 2) && isIdle) {
            setCameraZoom(226);
            changeCameraPitch(48);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledPointDelay(new Point(296, 768), 10);
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIFTH_ROOF) && isIdle && !AgilityPlusWorldPoints.MOG_SEERS5) {
            setCameraZoom(704);
            changeCameraPitch(48);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledPointDelay(new Point(324, 738), 10);
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FINISH) && isIdle && client.getOculusOrbState() == 0) {
            robot.delay(1000);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            panCameraToSeersStartFromFinish();
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        }
    }

    private void doRellekaAgility() {
        healthyThreshold = 4;
        if(turnRunOn()) {
            robot.delay(500);
            scheduledPointDelay(new Point(804, 157), 4);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FAIL1) && isIdle) {
            System.out.println("relleka fail 1");
            robot.delay(500);
            setCameraZoom(1004);
            changeCameraPitch(512);
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
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToRellekaStartFromFail2();
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.RELLEKA_START, 2) && isIdle) {
            System.out.println("relleka start");
            robot.delay(500);
            setCameraZoom(1004);
            changeCameraPitch(75);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledDecorativeObjectDelay(AgilityPlusObjectIDs.rellekaStartWall);
            robot.delay(500);
        } else if(isNotHealthly()) {
            System.out.println("waiting to heal");
            robot.delay(1000);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FIRST_ROOF) && isIdle && !AgilityPlusWorldPoints.MOG_RELLEKA1) {
            System.out.println("relleka 1st roof");
            setCameraZoom(390);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFirstRoofGap);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FIRST_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_RELLEKA1) {
            System.out.println("relleka pick up GM 1");
            setCameraZoom(850);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1));
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1) && isIdle) {
            System.out.println("relleka 1st roof");
            setCameraZoom(460);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFirstRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_SECOND_ROOF) && isIdle) {
            System.out.println("relleka 2ndd roof");
            setCameraZoom(1004);
            changeCameraPitch(512);
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
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaSecondRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_THIRD_ROOF) && isIdle && !AgilityPlusWorldPoints.MOG_RELLEKA3_1 && !AgilityPlusWorldPoints.MOG_RELLEKA3_2) {
            System.out.println("relleka 3rd roof");
            robot.delay(250);
            setCameraZoom(540);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(250);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaThirdRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_THIRD_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_RELLEKA3_1) {
            System.out.println("relleka pick up GM 3_1");
            robot.delay(250);
            setCameraZoom(880);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_1));
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_THIRD_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_RELLEKA3_2) {
            System.out.println("relleka pick up GM 3_2");
            robot.delay(250);
            setCameraZoom(880);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_2));
            robot.delay(500);
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_1) || isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_2)) && isIdle) {
            System.out.println("relleka pick up GM 4_2");
            robot.delay(250);
            setCameraZoom(570);
            changeCameraPitch(512);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaThirdRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FOURTH_ROOF) && isIdle && !AgilityPlusWorldPoints.MOG_RELLEKA4_1 && !AgilityPlusWorldPoints.MOG_RELLEKA4_2) {
            System.out.println("relleka 4th roof");
            robot.delay(500);
            setCameraZoom(540);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFourthRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FOURTH_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_RELLEKA4_1) {
            System.out.println("relleka pick up GM 4_1");
            robot.delay(250);
            setCameraZoom(715);
            changeCameraPitch(512);
            changeCameraYaw(1024);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_1));
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FOURTH_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_RELLEKA4_2) {
            System.out.println("relleka pick up GM 4_2");
            robot.delay(250);
            setCameraZoom(715);
            changeCameraPitch(512);
            changeCameraYaw(1024);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_2));
            robot.delay(500);
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_1) || isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_2)) && isIdle) {
            System.out.println("relleka 4th roof");
            robot.delay(250);
            setCameraZoom(570);
            changeCameraPitch(512);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFourthRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FIFTH_ROOF) && isIdle) {
            System.out.println("relleka 5th roof");
            robot.delay(250);
            setCameraZoom(500);
            changeCameraPitch(512);
            changeCameraYaw(1536);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaFifthRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_SIXTH_ROOF) && isIdle) {
            System.out.println("relleka 6th roof");
            robot.delay(1000);
            setCameraZoom(450);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.rellekaSixthRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FINISH) && isIdle) {
            System.out.println("relleka finish");
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(250);
            panCameraOneDirection(KeyEvent.VK_A, 1700);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        }
    }

    private void doArdyAgility() {
        healthyThreshold = 4;
        if(turnRunOn()) {
            robot.delay(500);
            scheduledPointDelay(new Point(804, 157), 4);
            robot.delay(500);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.ARDY_FAIL1, 2) && isIdle) {
            System.out.println("ardy fail 1"); // 2?
            robot.delay(500);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToArdyStartFromFail1();
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.ARDY_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isNearWorldTile(AgilityPlusWorldPoints.ARDY_FAIL2, 2) && isIdle) {
            System.out.println("ardy fail 2"); // 7 dmg, 3???
            robot.delay(500);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            panCameraToArdyStartFromFail2();
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.ARDY_START));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_START) && isIdle) {
            System.out.println("ardy start");
            robot.delay(500);
            setCameraZoom(855);
            changeCameraPitch(0);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledDecorativeObjectDelay(AgilityPlusObjectIDs.ardyStartWall);
            robot.delay(500);
        } else if(isNotHealthly()) {
            System.out.println("waiting to heal");
            robot.delay(1000);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_FIRST_ROOF) && isIdle) {
            System.out.println("ardy 1st roof");
            robot.delay(500);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            panCameraOneDirection(KeyEvent.VK_W, 600);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.ARDY_FIRST_ROOF_RUN_POINT));
            robot.delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_FIRST_ROOF_RUN_POINT) && isIdle) {
            System.out.println("ardy 1st roof run point");
            setCameraZoom(900);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.ardyFirstRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_SECOND_ROOF) && isIdle) {
            System.out.println("ardy 2nd roof");
            robot.delay(500);
            setCameraZoom(655);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.ardySecondRoofGap);
            robot.delay(3500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_SECOND_ROOF_RUN_POINT) && isIdle) {
            System.out.println("ardy 2nd roof run point");
            robot.delay(300);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.ardySecondRoofGap);
            robot.delay(500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_THIRD_ROOF) && isIdle && !AgilityPlusWorldPoints.MOG_ARDY) {
            System.out.println("ardy 3rd roof no GM");
            robot.delay(500);
            setCameraZoom(695);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.ardyThirdRoofGap);
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_THIRD_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_ARDY) {
            System.out.println("ardy 3rd roof yes GM");
            robot.delay(500);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.ARDY_GRACEFULMARK));
            robot.delay(1500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_FOURTH_ROOF) && isIdle) {
            System.out.println("ardy 4th roof");
            robot.delay(500);
            setCameraZoom(40);
            changeCameraPitch(75);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.ardyFourthRoofGap);
            robot.delay(3500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_FIFTH_ROOF) && isIdle) {
            System.out.println("ardy 5th roof");
            robot.delay(500);
            setCameraZoom(-72);
            changeCameraPitch(172);
            changeCameraYaw(249);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.ardyFifthRoofGap);
            robot.delay(3500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_SIXTH_ROOF) && isIdle) {
            System.out.println("ardy 6th roof");
            robot.delay(1000);
            setCameraZoom(890);
            changeCameraPitch(512);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.ardySixthRoofGap);
            robot.delay(3500);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_FINISH) && isIdle) {
            System.out.println("ardy finish");
            robot.delay(1500);
            setCameraZoom(1004);
            changeCameraPitch(4);
            changeCameraYaw(570);
            robot.delay(500);
            scheduledPointDelay(new Point(500, 500), 15);
            robot.delay(3500);
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

    private void changeCameraPitch(int pitch) {
        if(client.getCameraPitch() == pitch)
            return;
        client.setCameraPitchTarget(pitch);
    }

    private void setCameraZoom(int zoom) {
        clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom));
    }

    private void panCameraToVarrockStartFromFinish() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_A, 1000);
        pressKey(KeyEvent.VK_S, 100);
        robot.delay(1000);
    }

    private void panCameraToVarrockStartFromFail2() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_D, 2100);
        pressKey(KeyEvent.VK_W, 150);
        robot.delay(1000);
    }

    private void panCameraToFaladorStartFromFinish() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_D, 550);
        pressKey(KeyEvent.VK_W, 350);
        robot.delay(1000);
    }

    private void panCameraToFaladorStartFromFail1() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_A, 1000);
        pressKey(KeyEvent.VK_S, 500);
        robot.delay(1000);
    }

    private void panCameraToFaladorStartFromFail2() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_S, 1250);
        pressKey(KeyEvent.VK_D, 200);
        robot.delay(1000);
    }

    private void panCameraToCanfisStart() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_S, 900);
        pressKey(KeyEvent.VK_D, 1600);
    }

    private void panCameraToCanfisStart2() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_D, 300);
        pressKey(KeyEvent.VK_W, 400);
    }

    private void panCameraToSeersStartFromFail1() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_S, 500);
        pressKey(KeyEvent.VK_D, 800);
    }

    private void panCameraToSeersStartFromFail2() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_W, 300);
        pressKey(KeyEvent.VK_D, 1100);
    }

    private void panCameraToSeersStartFromFinish() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_D, 1700);
        pressKey(KeyEvent.VK_W, 1400);
        robot.delay(1000);
    }

    private void panCameraToRellekaStartFromFail2() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_A, 600);
        pressKey(KeyEvent.VK_W, 1100);
    }

    private void panCameraToArdyStartFromFail1() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_D, 550);
        pressKey(KeyEvent.VK_S, 800);
    }

    private void panCameraToArdyStartFromFail2() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_S, 1300);
        pressKey(KeyEvent.VK_D, 1150);
    }

    private void panCameraOneDirection(int keyEvent, int ms) {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(keyEvent, ms);
    }

    private void pressKey(int key, int ms) {
        robot.keyPress(key);
        robot.delay(ms);
        robot.keyRelease(key);
    }

    private void scheduledGroundObjectDelay(GroundObject groundObject) {
        isIdle = false;
        Shape convexHull = groundObject.getConvexHull();
//        MouseCoordCalculation.generatePointToClick(convexHull);
    }

    private void scheduledGameObjectDelay(GameObject gameObject) {
        isIdle = false;
        Shape convexHull = gameObject.getConvexHull();
//        MouseCoordCalculation.generatePointToClick(convexHull);
    }

    private void scheduledDecorativeObjectDelay(DecorativeObject decorativeObject) {
        isIdle = false;
        Shape convexHull = decorativeObject.getConvexHull();
//        MouseCoordCalculation.generatePointToClick(convexHull);
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
//        MouseCoordCalculation.generatePointToClick(poly);
    }

//    private void getWorldPointCoords(final LocalPoint dest) {
//        Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, dest, 1);
//        if (poly == null)
//        {
//            return;
//        }
//
//        Rectangle boundingBox = poly.getBounds();
//
//        Point obstacleCenter = getCenterOfRectangle(boundingBox);
//
//        scheduledPointDelay(obstacleCenter, 10);
//    }

    private boolean isNearWorldTile(final WorldPoint target, final int range) {
        return this.client.getLocalPlayer().getWorldLocation().distanceTo2D(target) < range
                && client.getLocalPlayer().getWorldLocation().getPlane() == target.getPlane();
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
