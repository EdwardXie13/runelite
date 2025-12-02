package net.runelite.client.plugins.agilityPlusV2;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.plusUtils.Clicker;
import net.runelite.client.plugins.plusUtils.StepOverlay;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Robot;
import java.awt.Shape;

import static net.runelite.client.plugins.agilityAid.AgilityAidWorldPoints.*;

public class AgilityPlusMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    private final StepOverlay overlay;

    public static boolean isIdle = true;
    public static boolean xpDrop = false;
    public static boolean invUpdate = false;
    private Runnable pendingAction = null;
    public long start;
//    Robot robot = new Robot();
    private int healthyThreshold = 1;
    public static boolean isRunning = false;

    Clicker clicker;
    BreakScheduler scheduler;

    Thread t;

    AgilityPlusMain(Client client, ClientThread clientThread, StepOverlay overlay) throws AWTException {
        this.client = client;
        this.clientThread = clientThread;
        this.overlay = overlay;
        clicker = new Clicker(client);
        scheduler = new BreakScheduler();

        t = new Thread(this);
        System.out.println("New thread: " + t);
        t.start(); // Starting the thread
    }

    public void stop() {
        isRunning = false;
//        reset();
        t.interrupt(); // in case it's sleeping or waiting
    }

    // execution of thread starts from run() method
    public void run()
    {
        while (isRunning) {
//            if(getRegionID() == 9781)
//                doGnomeAgility();
//                if (getNext) {
//                    clicker.randomDelay(300, 500);
//                }

                if (checkIdle(100)) {
                    isIdle = true;
                }

//            else
                if(getRegionID() == 13878)
                    doCanfisAgility();
//            else if(getRegionID() == 12597 || getRegionID() == 12853)
//                doVarrockAgility();
//            else if(getRegionID() == 12084)
//                doFaladorAgility();
//            else if(getRegionID() == 10806)
//                doSeersAgility();
//            else if(getRegionID() == 10553 || getRegionID() == 10297)
//                doRellekaAgility();
//            else if(getRegionID() == 10547)
//                doArdyAgility();
        }
        System.out.println("Thread has stopped.");
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

//    private void doGnomeAgility() {
//        if(turnRunOn()) {
//            robot.delay(500);
//            scheduledPointDelay(new Point(804, 157), 4);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.GNOME_START) && isIdle) {
//            setCameraZoom(896);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.gnomeLogBalance, AgilityPlusWorldPoints.GNOME_START, false);
////            grabClickBox = true;
////            clickPointObject(AgilityPlusObjectIDs.gnomeLogBalance, 10);
//            robot.delay(1000);
//        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_LOG) && isIdle) {
//            setCameraZoom(400);
//            setCameraPitch(0);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.gnomeObstacleNet1_M, AgilityPlusWorldPoints.GNOME_AFTER_LOG, false);
//            robot.delay(500);
//        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB1) && isIdle) {
//            robot.delay(1000);
//            setCameraZoom(896);
//            setCameraPitch(150);
//            setCameraYaw(220);
//            robot.delay(1000);
//            clickPointObject(AgilityPlusObjectIDs.gnomeTreeBranch1, AgilityPlusWorldPoints.GNOME_AFTER_CLIMB1, false);
//            robot.delay(500);
//            setCameraPitch(512);
//            robot.delay(1000);
//        } else if (isAtWorldPoint(new WorldPoint(2471, 3424, 1)) && isIdle) {
//            robot.delay(1000);
//            setCameraZoom(752);
//            setCameraPitch(125);
//            setCameraYaw(225);
//            robot.delay(1000);
//            clickPointObject(AgilityPlusObjectIDs.gnomeTreeBranch1, new WorldPoint(2471, 3424, 1), false);
//            robot.delay(500);
//            setCameraPitch(512);
//            robot.delay(1000);
//        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB2) && isIdle) {
//            setCameraZoom(140);
//            setCameraPitch(20);
//            setCameraYaw(512);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.gnomeBalancingRope, AgilityPlusWorldPoints.GNOME_AFTER_CLIMB2, false);
//            robot.delay(500);
//        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_TIGHTROPE) && isIdle) {
//            setCameraZoom(520);
//            setCameraPitch(70);
//            setCameraYaw(322);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.gnomeTreeBranch2, AgilityPlusWorldPoints.GNOME_AFTER_TIGHTROPE, false);
//            robot.delay(500);
//        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_DROP) && isIdle) {
//            setCameraZoom(-47);
//            setCameraPitch(90);
//            setCameraYaw(1185);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.gnomeObstacleNet2_L, AgilityPlusWorldPoints.GNOME_AFTER_DROP, false);
//            robot.delay(500);
//        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_NET1) && AgilityPlusObjectIDs.gnomeObstaclePipeLeft != null && isIdle) {
//            setCameraZoom(495);
//            setCameraPitch(100);
//            setCameraYaw(1020);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.gnomeObstaclePipeLeft, AgilityPlusWorldPoints.GNOME_AFTER_NET1, false);
//            robot.delay(500);
//            setCameraPitch(512);
//            robot.delay(8000); //adjust as needed
//        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_NET1) && AgilityPlusObjectIDs.gnomeObstaclePipeRight != null && isIdle) {
//            setCameraZoom(285);
//            setCameraPitch(118);
//            setCameraYaw(757);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.gnomeObstaclePipeRight, AgilityPlusWorldPoints.GNOME_AFTER_NET1, false);
//            robot.delay(1000);
//            setCameraPitch(512);
//            robot.delay(8000); //adjust as needed
//        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_LEFT_PIPE) &&  isIdle) {
//            setCameraZoom(896);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.GNOME_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_RIGHT_PIPE) &&  isIdle) {
//            robot.delay(1000);
//            setCameraZoom(896);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.GNOME_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        }
//    }
//
//    private void doVarrockAgility() {
//        healthyThreshold = 4;
//        if(turnRunOn()) {
//            robot.delay(500);
//            scheduledPointDelay(new Point(804, 157), 4);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_FAIL1) && isIdle) {
//            robot.delay(1000);
//            setCameraZoom(1004);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.VARROCK_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(1500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_FAIL2) && isIdle) {
//            robot.delay(1000);
//            setCameraZoom(1004);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.VARROCK_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(1500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_START) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(830);
//            setCameraPitch(30);
//            setCameraYaw(512);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.varrockStart, AgilityPlusWorldPoints.VARROCK_START, false);
//        } else if(isNotHealthly()) {
//            System.out.println("waiting to heal");
//            robot.delay(1000);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_FIRST_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(500);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.varrockFirstRoofGap, AgilityPlusWorldPoints.VARROCK_FIRST_ROOF, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_SECOND_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(370);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.varrockSecondRoofGap, AgilityPlusWorldPoints.VARROCK_SECOND_ROOF, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_THIRD_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(500);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.varrockThirdRoofGap, AgilityPlusWorldPoints.VARROCK_THIRD_ROOF, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_FOURTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(495);
//            setCameraPitch(512);
//            setCameraYaw(512);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.varrockFourthRoofGap, AgilityPlusWorldPoints.VARROCK_FOURTH_ROOF, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_FIFTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(155);
//            setCameraPitch(512);
//            setCameraYaw(1024);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.varrockFifthRoofGap, AgilityPlusWorldPoints.VARROCK_FIFTH_ROOF, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_SIXTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(640);
//            setCameraPitch(72);
//            setCameraYaw(1536);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.varrockSixthRoofGap, AgilityPlusWorldPoints.VARROCK_SIXTH_ROOF, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_SEVENTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(320);
//            setCameraPitch(234);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.varrockSeventhRoofGap, AgilityPlusWorldPoints.VARROCK_SEVENTH_ROOF, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_EIGHTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(-45);
//            setCameraPitch(116);
//            setCameraYaw(1024);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.varrockEighthRoofGap, AgilityPlusWorldPoints.VARROCK_EIGHTH_ROOF, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.VARROCK_FINISH) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(1004);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.VARROCK_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        }
//    }
//
//    private void doFaladorAgility() {
//        healthyThreshold = 4;
//        if(turnRunOn()) {
//            robot.delay(500);
//            scheduledPointDelay(new Point(804, 157), 4);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_FAIL1) && isIdle) {
//            robot.delay(1000);
//            setCameraZoom(1004);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.FALADOR_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_FAIL2) && isIdle) {
//            robot.delay(1000);
//            setCameraZoom(1004);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.FALADOR_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_START) && isIdle) {
//            robot.delay(1500);
//            setCameraZoom(1004);
//            setCameraPitch(100);
//            setCameraYaw(1024);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.faladorStart, AgilityPlusWorldPoints.FALADOR_START, false);
//            robot.delay(500);
//        } else if(isNotHealthly()) {
//            System.out.println("waiting to heal");
//            robot.delay(1000);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_FIRST_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(635);
//            setCameraPitch(512);
//            setCameraYaw(1024);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.faladorFirstRoofGap, AgilityPlusWorldPoints.FALADOR_FIRST_ROOF, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_SECOND_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(230);
//            setCameraPitch(172);
//            setCameraYaw(1536);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.faladorSecondRoofGap, AgilityPlusWorldPoints.FALADOR_SECOND_ROOF, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_THIRD_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(574);
//            setCameraPitch(42);
//            setCameraYaw(1313);
//            robot.delay(500);
//            scheduledPointDelay(new Point(355, 630), 8);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_FOURTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(625);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.faladorFourthRoofGap, AgilityPlusWorldPoints.FALADOR_FOURTH_ROOF, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_FIFTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(-72);
//            setCameraPitch(103);
//            setCameraYaw(1536);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.faladorFifthRoofGap, AgilityPlusWorldPoints.FALADOR_FIFTH_ROOF, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_SIXTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(862);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.faladorSixthRoofGap, AgilityPlusWorldPoints.FALADOR_SIXTH_ROOF, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_SEVENTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(295);
//            setCameraPitch(214);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.faladorSeventhRoofGap, AgilityPlusWorldPoints.FALADOR_SEVENTH_ROOF, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_EIGHTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(174);
//            setCameraPitch(91);
//            setCameraYaw(1975);
//            robot.delay(500);
//            scheduledPointDelay(new Point(170, 783), 10);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_NINTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(410);
//            setCameraPitch(224);
//            setCameraYaw(1024);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.faladorNinthRoofGap, AgilityPlusWorldPoints.FALADOR_NINTH_ROOF, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_TENTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(730);
//            setCameraPitch(91);
//            setCameraYaw(1024);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.faladorTenthRoofGap, AgilityPlusWorldPoints.FALADOR_TENTH_ROOF, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_ELEVENTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(390);
//            setCameraPitch(202);
//            setCameraYaw(512);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.faladorEleventhRoofGap, AgilityPlusWorldPoints.FALADOR_ELEVENTH_ROOF, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_TWELFTH_ROOF) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(-72);
//            setCameraPitch(77);
//            setCameraYaw(642);
//            robot.delay(500);
//            scheduledPointDelay(new Point(135, 790), 10);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.FALADOR_FINISH) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(1004);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.FALADOR_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        }
//    }

    private void doCanfisAgility() {
        // Update idle based on movement
        updateIdleStatus();

        // Retry pending actions if necessary
        checkActionSuccess();

        // humanizer???
        clicker.randomDelayStDev(150,200,25);

        if(isAtWorldPoint(CANFIS_FAIL) && isIdle) {
            tryAction(this::canfisFail);
        }
        else if(isAtWorldPoint(CANFIS_START) && isIdle) {
            tryAction(this::canfisStart);
        }
        else if(isAtWorldPoint(CANFIS_BUSH) && isIdle) {
            tryAction(this::clickTreeFromBush);
        }
        else if(isAtWorldPoint(CANFIS_FIRST_ROOF) && AgilityPlusWorldPoints.MOG_CANFIS1 && xpDrop && isIdle) {
            tryAction(this::canfisPickupMOG1);
        }
        else if(isAtWorldPoint(CANFIS_FIRST_ROOF) && !AgilityPlusWorldPoints.MOG_CANFIS1 && xpDrop && isIdle) {
            tryAction(this::canfisNoMOG1);
        }
        else if(isAtWorldPoint(CANFIS_GRACEFULMARK1) && invUpdate && isIdle) {
            tryAction(this::canfisAtMog1);
        }
        else if (isAtWorldPoint(CANFIS_SECOND_ROOF) && AgilityPlusWorldPoints.MOG_CANFIS2 && xpDrop && isIdle) {
            tryAction(this::canfisPickupMOG2);
        }
        else if (isAtWorldPoint(CANFIS_SECOND_ROOF) && !AgilityPlusWorldPoints.MOG_CANFIS2 && xpDrop && isIdle) {
            tryAction(this::canfisNoMOG2);
        }
        else if(isAtWorldPoint(CANFIS_GRACEFULMARK2) && invUpdate && isIdle) {
            tryAction(this::canfisAtMog2);
        }
        else if (isAtWorldPoint(CANFIS_THIRD_ROOF) && AgilityPlusWorldPoints.MOG_CANFIS3 && xpDrop && isIdle) {
            tryAction(this::canfisPickupMOG3);
        }
        else if (isAtWorldPoint(CANFIS_THIRD_ROOF) && !AgilityPlusWorldPoints.MOG_CANFIS3 && xpDrop && isIdle) {
            tryAction(this::canfisNoMOG3);
        }
        else if(isAtWorldPoint(CANFIS_GRACEFULMARK3) && invUpdate && isIdle) {
            tryAction(this::canfisAtMog3);
        }
        else if (isAtWorldPoint(CANFIS_FOURTH_ROOF) && AgilityPlusWorldPoints.MOG_CANFIS4 && xpDrop && isIdle) {
            tryAction(this::canfisPickupMOG4);
        }
        else if (isAtWorldPoint(CANFIS_FOURTH_ROOF) && !AgilityPlusWorldPoints.MOG_CANFIS4 && xpDrop && isIdle) {
            tryAction(this::canfisNoMOG4);
        }
        else if (isAtWorldPoint(CANFIS_GRACEFULMARK4) && invUpdate && isIdle) {
            tryAction(this::canfisAtMog4);
        }
        else if (isAtWorldPoint(CANFIS_FIFTH_ROOF) && AgilityPlusWorldPoints.MOG_CANFIS5 && xpDrop && isIdle) {
            tryAction(this::canfisPickupMOG5);
        }
        else if (isAtWorldPoint(CANFIS_FIFTH_ROOF) && !AgilityPlusWorldPoints.MOG_CANFIS5 && xpDrop && isIdle) {
            tryAction(this::canfisNoMOG5);
        }
        else if (isAtWorldPoint(CANFIS_GRACEFULMARK5) && invUpdate && isIdle) {
            tryAction(this::canfisAtMog5);
        }
        else if (isAtWorldPoint(CANFIS_SIXTH_ROOF) && xpDrop && isIdle) {
            tryAction(this::canfisRoof6);
        }
        else if (isAtWorldPoint(CANFIS_SEVENTH_ROOF) && xpDrop && isIdle) {
            tryAction(this::canfisRoof7);
        }
//        else if(isNotHealthly()) {
//            overlay.setCurrentStep("waiting to heal");
//            robot.delay(1000);
//        }
    }

    private void canfisFail() {
        overlay.setCurrentStep("CANFIS_FAIL");
        while(!isMoving())
            clicker.clickWorldPoint(CANFIS_BUSH);
    }

    private void canfisStart() {
        overlay.setCurrentStep("CANFIS_START");
        Point toClick = new Point(461, 614);
        while(!isMoving()) {
            clicker.spamPoint(toClick);
        }
    }

    private void clickTreeFromBush() {
        overlay.setCurrentStep("CANFIS_BUSH");
        WorldPoint tile = new WorldPoint(3508, 3488, 0);
        if (isTileOnScreen(tile) && !isMoving()) {
            clicker.randomDelayStDev(1500,3000,100);
            clickPointObject(AgilityPlusObjectIDs.canfisTallTree, true);
        }
    }

    // canfis mog1
    private void canfisPickupMOG1() {
        overlay.setCurrentStep("CANFIS MOG1");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clicker.clickWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1);
        clicker.randomDelayStDev(100,200,25);
    }

    private void canfisNoMOG1() {
        overlay.setCurrentStep("CANFIS no MOG1");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clickPointObject(AgilityPlusObjectIDs.canfisFirstRoofGap, false);
        clicker.randomDelayStDev(100,200,25);
    }

    private void canfisAtMog1() {
        overlay.setCurrentStep("At MOG1");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clickPointObject(AgilityPlusObjectIDs.canfisFirstRoofGap, false);
        clicker.randomDelayStDev(100,200,25);
    }

    // canfis mog2
    private void canfisPickupMOG2() {
        overlay.setCurrentStep("CANFIS MOG2");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clicker.clickWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2);
        clicker.randomDelayStDev(100,200,25);
    }

    private void canfisNoMOG2() {
        overlay.setCurrentStep("CANFIS no MOG2");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clickPointObject(AgilityPlusObjectIDs.canfisSecondRoofGap, false);
        clicker.randomDelayStDev(100,200,25);
    }

    private void canfisAtMog2() {
        overlay.setCurrentStep("At MOG2");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clickPointObject(AgilityPlusObjectIDs.canfisSecondRoofGap, false);
        clicker.randomDelayStDev(100,200,25);
    }

    // canfis mog3
    private void canfisPickupMOG3() {
        overlay.setCurrentStep("CANFIS MOG3");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clicker.clickWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3);
        clicker.randomDelayStDev(100,200,25);
    }

    private void canfisNoMOG3() {
        overlay.setCurrentStep("CANFIS no MOG3");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clickPointObject(AgilityPlusObjectIDs.canfisThirdRoofGap, false);
        clicker.randomDelayStDev(100,200,25);
    }

    private void canfisAtMog3() {
        overlay.setCurrentStep("At MOG3");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clickPointObject(AgilityPlusObjectIDs.canfisThirdRoofGap, false);
        clicker.randomDelayStDev(100,200,25);
    }

    // canfis mog4
    private void canfisPickupMOG4() {
        overlay.setCurrentStep("CANFIS MOG4");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clicker.clickWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4);
        clicker.randomDelayStDev(100,200,25);
    }

    private void canfisNoMOG4() {
        overlay.setCurrentStep("CANFIS no MOG4");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clickPointObject(AgilityPlusObjectIDs.canfisFourthRoofGap, false);
        clicker.randomDelayStDev(100,200,25);
    }

    private void canfisAtMog4() {
        overlay.setCurrentStep("At MOG4");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clickPointObject(AgilityPlusObjectIDs.canfisFourthRoofGap, false);
        clicker.randomDelayStDev(100,200,25);
    }

    // canfis mog5
    private void canfisPickupMOG5() {
        overlay.setCurrentStep("CANFIS MOG5");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clicker.clickWorldPoint(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5);
        clicker.randomDelayStDev(100,200,25);
    }

    private void canfisNoMOG5() {
        overlay.setCurrentStep("CANFIS no MOG5");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clickPointObject(AgilityPlusObjectIDs.canfisFifthRoofGap, true);
        clicker.randomDelayStDev(100,200,25);
    }

    private void canfisAtMog5() {
        overlay.setCurrentStep("At MOG5");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clickPointObject(AgilityPlusObjectIDs.canfisFifthRoofGap, true);
        clicker.randomDelayStDev(100,200,25);
    }

    private void canfisRoof6() {
        overlay.setCurrentStep("CANFIS roof6");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clickPointObject(AgilityPlusObjectIDs.canfisSixthRoofGap, true);
        clicker.randomDelayStDev(100,200,25);
    }

    private void canfisRoof7() {
        overlay.setCurrentStep("CANFIS roof6");
        clicker.randomDelayStDev(100,200,25);
        if(!isMoving())
            clickPointObject(AgilityPlusObjectIDs.canfisSeventhRoofGap, false);
        clicker.randomDelayStDev(100,200,25);
    }

//    private void doSeersAgility() {
//        healthyThreshold = 5;
//        if(turnRunOn()) {
//            robot.delay(500);
//            scheduledPointDelay(new Point(804, 157), 4);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FAIL1) && isIdle) {
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FAIL2) && isIdle) {
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_START) && isIdle) {
//            robot.delay(500);
//            setCameraZoom(760);
//            setCameraPitch(80);
//            setCameraYaw(1024);
//            robot.delay(500);
//            scheduledPointDelay(new Point(478, 666), 10);
//            robot.delay(500);
//        } else if(isNotHealthly()) {
//            System.out.println("waiting to heal");
//            robot.delay(1000);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIRST_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_SEERS1) {
//            setCameraZoom(896);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_GRACEFULMARK1));
//            robot.delay(500);
//        } else if(isNearWorldTile(new WorldPoint(2721, 3494, 3), 3) && isIdle) {
//            setCameraZoom(896);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.seersFirstRoofGap, false);
//            robot.delay(1500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIRST_ROOF) && isIdle && !AgilityPlusWorldPoints.MOG_SEERS1) {
//            setCameraZoom(362);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.seersFirstRoofGap, false);
//            robot.delay(9000);
//        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1, 2) && isIdle) {
//            setCameraZoom(-47);
//            setCameraPitch(175);
//            setCameraYaw(1535);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.seersFirstRoofGap, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_SEERS2_1) {
//            setCameraZoom(483);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1));
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1) && isIdle) {
//            setCameraZoom(23);
//            setCameraPitch(52);
//            setCameraYaw(242);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.seersTightrope, false);
//            robot.delay(3500);
//        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1, 2) && isIdle && !AgilityPlusWorldPoints.MOG_SEERS2_1) {
//            setCameraZoom(590);
//            setCameraPitch(512);
//            setCameraYaw(1024);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.seersTightrope, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_SEERS2_2) {
//            setCameraZoom(680);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2));
//        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2, 2) && isIdle) {
//            setCameraZoom(245);
//            setCameraPitch(43);
//            setCameraYaw(85);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.seersTightrope, false);
//        } else if((isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle)) {
//            setCameraZoom(577);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(1000);
//            clickPointObject(AgilityPlusObjectIDs.seersTightrope, false);
//        } else if(isNearWorldTile(new WorldPoint(2710, 3490, 2), 3) && isIdle) {
//            setCameraZoom(896);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.seersTightrope, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_THIRD_ROOF) && !AgilityPlusWorldPoints.MOG_SEERS3 && isIdle) {
//            setCameraZoom(404);
//            setCameraPitch(48);
//            setCameraYaw(0);
//            robot.delay(500);
//            scheduledPointDelay(new Point(468, 799), 10);
//            robot.delay(1500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_THIRD_ROOF) && AgilityPlusWorldPoints.MOG_SEERS3 && isIdle) {
//            setCameraZoom(896);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_GRACEFULMARK3));
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3)  && isIdle) {
//            setCameraZoom(261);
//            setCameraPitch(44);
//            setCameraYaw(0);
//            robot.delay(500);
//            scheduledPointDelay(new Point(470, 759), 10);
//            robot.delay(1500);
//        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_FOURTH_ROOF, 4) && isIdle && client.getOculusOrbState() == 0) {
//            setCameraZoom(255);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.seersFourthRoofGap, false);
//            robot.delay(7000);
//        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_FOURTH_ROOF_RUN_POINT, 2) && isIdle) {
//            setCameraZoom(896);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.seersFourthRoofGap, false);
//        } else if(isAtWorldPoint(new WorldPoint(2704, 3470, 3)) && isIdle) {
//            setCameraZoom(896);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.seersFourthRoofGap, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIFTH_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_SEERS5) {
//            setCameraZoom(670);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_GRACEFULMARK5));
//        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5, 2) && isIdle) {
//            setCameraZoom(226);
//            setCameraPitch(48);
//            setCameraYaw(512);
//            robot.delay(500);
//            scheduledPointDelay(new Point(296, 768), 10);
//            robot.delay(1500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIFTH_ROOF) && isIdle && !AgilityPlusWorldPoints.MOG_SEERS5) {
//            setCameraZoom(704);
//            setCameraPitch(48);
//            setCameraYaw(512);
//            robot.delay(500);
//            scheduledPointDelay(new Point(324, 738), 10);
//            robot.delay(1500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FINISH) && isIdle && client.getOculusOrbState() == 0) {
//            robot.delay(1000);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        }
//    }
//
//    private void doRellekaAgility() {
//        healthyThreshold = 4;
//        if(turnRunOn()) {
//            robot.delay(500);
//            scheduledPointDelay(new Point(804, 157), 4);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FAIL1) && isIdle) {
//            System.out.println("relleka fail 1");
//            robot.delay(500);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FAIL2) && isIdle) {
//            System.out.println("relleka fail 2");
//            robot.delay(500);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        } else if(isNearWorldTile(AgilityPlusWorldPoints.RELLEKA_START, 2) && isIdle) {
//            System.out.println("relleka start");
//            robot.delay(500);
//            setCameraZoom(1004);
//            setCameraPitch(75);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.rellekaStartWall, false);
//            robot.delay(500);
//        } else if(isNotHealthly()) {
//            System.out.println("waiting to heal");
//            robot.delay(1000);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FIRST_ROOF) && isIdle && !AgilityPlusWorldPoints.MOG_RELLEKA1) {
//            System.out.println("relleka 1st roof");
//            setCameraZoom(390);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.rellekaFirstRoofGap, false);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FIRST_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_RELLEKA1) {
//            System.out.println("relleka pick up GM 1");
//            setCameraZoom(850);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1));
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1) && isIdle) {
//            System.out.println("relleka 1st roof");
//            setCameraZoom(460);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.rellekaFirstRoofGap, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_SECOND_ROOF) && isIdle) {
//            System.out.println("relleka 2ndd roof");
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_SECOND_ROOF_RUN_POINT));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_SECOND_ROOF_RUN_POINT) && isIdle) {
//            System.out.println("relleka 2nd roof run point");
//            setCameraZoom(1004);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            clickPointObject(AgilityPlusObjectIDs.rellekaSecondRoofGap, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_THIRD_ROOF) && isIdle && !AgilityPlusWorldPoints.MOG_RELLEKA3_1 && !AgilityPlusWorldPoints.MOG_RELLEKA3_2) {
//            System.out.println("relleka 3rd roof");
//            robot.delay(250);
//            setCameraZoom(540);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(250);
//            clickPointObject(AgilityPlusObjectIDs.rellekaThirdRoofGap, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_THIRD_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_RELLEKA3_1) {
//            System.out.println("relleka pick up GM 3_1");
//            robot.delay(250);
//            setCameraZoom(880);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_1));
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_THIRD_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_RELLEKA3_2) {
//            System.out.println("relleka pick up GM 3_2");
//            robot.delay(250);
//            setCameraZoom(880);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_2));
//            robot.delay(500);
//        } else if((isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_1) || isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_2)) && isIdle) {
//            System.out.println("relleka pick up GM 4_2");
//            robot.delay(250);
//            setCameraZoom(570);
//            setCameraPitch(512);
//            setCameraYaw(1024);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.rellekaThirdRoofGap, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FOURTH_ROOF) && isIdle && !AgilityPlusWorldPoints.MOG_RELLEKA4_1 && !AgilityPlusWorldPoints.MOG_RELLEKA4_2) {
//            System.out.println("relleka 4th roof");
//            robot.delay(500);
//            setCameraZoom(540);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.rellekaFourthRoofGap, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FOURTH_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_RELLEKA4_1) {
//            System.out.println("relleka pick up GM 4_1");
//            robot.delay(250);
//            setCameraZoom(715);
//            setCameraPitch(512);
//            setCameraYaw(1024);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_1));
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FOURTH_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_RELLEKA4_2) {
//            System.out.println("relleka pick up GM 4_2");
//            robot.delay(250);
//            setCameraZoom(715);
//            setCameraPitch(512);
//            setCameraYaw(1024);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_2));
//            robot.delay(500);
//        } else if((isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_1) || isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_2)) && isIdle) {
//            System.out.println("relleka 4th roof");
//            robot.delay(250);
//            setCameraZoom(570);
//            setCameraPitch(512);
//            setCameraYaw(1024);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.rellekaFourthRoofGap, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FIFTH_ROOF) && isIdle) {
//            System.out.println("relleka 5th roof");
//            robot.delay(250);
//            setCameraZoom(500);
//            setCameraPitch(512);
//            setCameraYaw(1536);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.rellekaFifthRoofGap, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_SIXTH_ROOF) && isIdle) {
//            System.out.println("relleka 6th roof");
//            robot.delay(1000);
//            setCameraZoom(450);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.rellekaSixthRoofGap, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.RELLEKA_FINISH) && isIdle) {
//            System.out.println("relleka finish");
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.RELLEKA_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        }
//    }
//
//    private void doArdyAgility() {
//        healthyThreshold = 4;
//        if(turnRunOn()) {
//            robot.delay(500);
//            scheduledPointDelay(new Point(804, 157), 4);
//            robot.delay(500);
//        } else if(isNearWorldTile(AgilityPlusWorldPoints.ARDY_FAIL1, 2) && isIdle) {
//            System.out.println("ardy fail 1"); // 2?
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.ARDY_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        } else if(isNearWorldTile(AgilityPlusWorldPoints.ARDY_FAIL2, 2) && isIdle) {
//            System.out.println("ardy fail 2"); // 7 dmg, 3???
//            robot.delay(500);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.ARDY_START));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_START) && isIdle) {
//            System.out.println("ardy start");
//            robot.delay(500);
//            setCameraZoom(855);
//            setCameraPitch(0);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.ardyStartWall, false);
//            robot.delay(500);
//        } else if(isNotHealthly()) {
//            System.out.println("waiting to heal");
//            robot.delay(1000);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_FIRST_ROOF) && isIdle) {
//            System.out.println("ardy 1st roof");
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.ARDY_FIRST_ROOF_RUN_POINT));
//            robot.delay(500);
//            client.setOculusOrbState(0);
//            client.setOculusOrbNormalSpeed(12);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_FIRST_ROOF_RUN_POINT) && isIdle) {
//            System.out.println("ardy 1st roof run point");
//            setCameraZoom(900);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.ardyFirstRoofGap, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_SECOND_ROOF) && isIdle) {
//            System.out.println("ardy 2nd roof");
//            robot.delay(500);
//            setCameraZoom(655);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.ardySecondRoofGap, false);
//            robot.delay(3500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_SECOND_ROOF_RUN_POINT) && isIdle) {
//            System.out.println("ardy 2nd roof run point");
//            robot.delay(300);
//            setCameraZoom(1004);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.ardySecondRoofGap, false);
//            robot.delay(500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_THIRD_ROOF) && isIdle && !AgilityPlusWorldPoints.MOG_ARDY) {
//            System.out.println("ardy 3rd roof no GM");
//            robot.delay(500);
//            setCameraZoom(695);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.ardyThirdRoofGap, false);
//            robot.delay(1500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_THIRD_ROOF) && isIdle && AgilityPlusWorldPoints.MOG_ARDY) {
//            System.out.println("ardy 3rd roof yes GM");
//            robot.delay(500);
//            setCameraZoom(1004);
//            setCameraPitch(512);
//            setCameraYaw(0);
//            robot.delay(500);
//            getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.ARDY_GRACEFULMARK));
//            robot.delay(1500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_FOURTH_ROOF) && isIdle) {
//            System.out.println("ardy 4th roof");
//            robot.delay(500);
//            setCameraZoom(40);
//            setCameraPitch(75);
//            setCameraYaw(0);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.ardyFourthRoofGap, false);
//            robot.delay(3500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_FIFTH_ROOF) && isIdle) {
//            System.out.println("ardy 5th roof");
//            robot.delay(500);
//            setCameraZoom(-72);
//            setCameraPitch(172);
//            setCameraYaw(249);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.ardyFifthRoofGap, false);
//            robot.delay(3500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_SIXTH_ROOF) && isIdle) {
//            System.out.println("ardy 6th roof");
//            robot.delay(1000);
//            setCameraZoom(890);
//            setCameraPitch(512);
//            setCameraYaw(512);
//            robot.delay(500);
//            clickPointObject(AgilityPlusObjectIDs.ardySixthRoofGap, false);
//            robot.delay(3500);
//        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_FINISH) && isIdle) {
//            System.out.println("ardy finish");
//            robot.delay(1500);
//            setCameraZoom(1004);
//            setCameraPitch(4);
//            setCameraYaw(570);
//            robot.delay(500);
//            scheduledPointDelay(new Point(500, 500), 15);
//            robot.delay(3500);
//        }
//    }

    private void setZoomPitchYaw(int zoom, int pitch, int yaw) {
        setCameraZoom(zoom);
        setCameraPitch(pitch);
        setCameraYaw(yaw);
    }

    private boolean isAtWorldPoint(WorldPoint worldPoint) {
        boolean playerX = client.getLocalPlayer().getWorldLocation().getX() == worldPoint.getX();
        boolean playerY = client.getLocalPlayer().getWorldLocation().getY() == worldPoint.getY();
        boolean playerPlane = client.getLocalPlayer().getWorldLocation().getPlane() == worldPoint.getPlane();
        return playerX && playerY && playerPlane;
    }

    private void setCameraYaw(int yaw) {
        if(client.getCameraYaw() == yaw)
            return;
        client.setCameraYawTarget(yaw);
    }

    private void setCameraPitch(int pitch) {
        if(client.getCameraPitch() == pitch)
            return;
        client.setCameraPitchTarget(pitch);
    }

    private void setCameraZoom(int zoom) {
        clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom));
    }

    private void clickPointObject(TileObject o, boolean delay) {
        if (o == null) return;

        Shape s = o.getClickbox();
        if (s == null) return;

        Point p = clicker.getCentroidFromShape(client, s);
        if (p != null && p.x > 0 && p.y > 0) {
            clicker.clickPoint(p);
        }

        if (delay)
            clicker.randomDelayStDev(300,500,50);
    }

//    public boolean isMoving()
//    {
//        return client.getLocalDestinationLocation() != null;
//    }

    public boolean isMoving()
    {
        Player p = client.getLocalPlayer();
        if (p == null)
            return false;

        // 1. Walking movement (destination exists)
        if (client.getLocalDestinationLocation() != null)
            return true;

        // 2. Animation-based movement (agility obstacles)
        int anim = p.getAnimation();

        // Animation -1 = idle
        if (anim != -1 && anim != 808) // 808 = idle look-around animation
            return true;

        return false;
    }

    public void updateIdleStatus()
    {
        if (isMoving())
        {
            // Movement started → reset all event signals
            overlay.setCurrentStep("moving to next");
            isIdle = false;
            xpDrop = false;
            invUpdate = false;
        }
        else
        {
            // Character standing still
            overlay.setCurrentStep("char idle");
            isIdle = true;
        }
    }

    private void tryAction(Runnable action)
    {
        if (!isIdle) {
            return;
        }

        isIdle = false;          // mark busy
        pendingAction = action;  // store for potential retry
        action.run();            // execute immediately
    }

    private void checkActionSuccess()
    {
        if (pendingAction == null)
            return;

        if (isMoving())
        {
            // Action succeeded → clear pending
            pendingAction = null;
            return;
        }

        if (isIdle && !isMoving())
        {
            // Click failed → retry
            overlay.setCurrentStep("retry");
            Runnable retry = pendingAction;
            pendingAction = null;
            tryAction(retry);
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

    private boolean isNearWorldTile(final WorldPoint target, final int range) {
        return this.client.getLocalPlayer().getWorldLocation().distanceTo2D(target) < range
                && client.getLocalPlayer().getWorldLocation().getPlane() == target.getPlane();
    }

    private boolean checkIdle(int ticks)
    {
        int idleClientTicks = client.getKeyboardIdleTicks();

        if (client.getMouseIdleTicks() < idleClientTicks)
        {
            idleClientTicks = client.getMouseIdleTicks();
        }

        return idleClientTicks >= ticks;
    }

    void reset() {
//        System.out.println("idle for too long, reset");
//        client.setOculusOrbState(0);
//        client.setOculusOrbNormalSpeed(12);
        isIdle = true;
//        robot.delay(200);
//        start = System.currentTimeMillis();
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

    public boolean isTileOnScreen(WorldPoint wp)
    {
        if (client == null || wp == null)
            return false;

        // Convert world → local
        LocalPoint lp = LocalPoint.fromWorld(client, wp);
        if (lp == null)
            return false;

        // Get perspective polygon
        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        return poly != null;
    }
}
