package net.runelite.client.plugins.tithefarmplus;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.ScriptID;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.agilityPlus.MouseCoordCalculation;
import net.runelite.client.plugins.tithefarmplus.TitheFarmPlusObjectIDs.PatchState;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.runelite.client.plugins.tithefarmplus.TitheFarmPlusWorldPoints.patchNumByTile;
import static net.runelite.client.plugins.tithefarmplus.TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint;

public class TitheFarmPlusMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    public static boolean isIdle = true;
    public long start;
    Robot robot = new Robot();
    public static boolean isRunning = false;
    public static boolean hasInit = false;
    private int currentPatch = 0;
    private boolean hasSetZoom = false;
    public static boolean fillWateringCan = false;
    private boolean logout = false;

    public static List<PatchState> patchStates = new ArrayList<>(Collections.nCopies(20, PatchState.EMPTY));

    Thread t;

    TitheFarmPlusMain(Client client, ClientThread clientThread) throws AWTException {
        this.client = client;
        this.clientThread = clientThread;

        t = new Thread(this);
        System.out.println("New thread: " + t);
        start = System.currentTimeMillis();
        t.start(); // Starting the thread
    }

    public void run()
    {
        while (isRunning) {
            if (getRegionID() == 7222)
                doTitheFarm();
        }
        System.out.println("Thread has stopped.");
    }

    private void doTitheFarm() {
        // log out at 5:45 and patchStates.contains 16 empty
        if(!hasSetZoom) {
            setCameraZoom(530);
            client.setCameraPitchTarget(512);
            hasSetZoom = true;
        }

        // TODO: add logout time check / 1400 fruits
        if(!logout)
            closeToLogout();

        rotateCamera();

        if(isAtCurrentPatch(19) && countEmptyPatches() && logout) {
            isRunning = false;
            t.interrupt();
            System.out.println("interrupted");
        } else if(isAtCurrentPatch(0) && countEmptyPatches() && fillWateringCan) {
            panCameraToWaterBarrelFromPatch0();
        } else if(isAtWorldPoint(TitheFarmPlusWorldPoints.waterBarrelWorldPoint)) {
            robot.delay(1000);
            setCameraZoom(1004);
            robot.delay(500);
            scheduledPointDelay(new Point(782, 804), 3); // row 2 col 1
            robot.delay(250);
            scheduledPointDelay(new Point(163, 501), 5); // click barrel
            robot.delay(1000);
            panCameraToPatch0FromWaterBarrel();
        } else if(patchStates.get(currentPatch) == PatchState.EMPTY && isAtCurrentPatch(currentPatch)) {
            // click seed
            clickFirstSlot();
            shouldDelayBit();
            // click center of patch
            clickPatch(patchNumByTile.get(currentPatch));

            while(patchStates.get(currentPatch) == PatchState.EMPTY) {
                robot.delay(150);
            }
        } else if(patchStates.get(currentPatch) == PatchState.UNWATERED && isAtCurrentPatch(currentPatch)) {
            // click patch
            shouldDelayBit();
            clickPatch(patchNumByTile.get(currentPatch));

            while(patchStates.get(currentPatch) == PatchState.UNWATERED) {
                robot.delay(150);
            }

            if(patchStates.get(currentPatch) == PatchState.WATERED) {
                // increment patch
                incrementPatch();
                moveToNextTile();
            }

        } else if(patchStates.get(currentPatch) == PatchState.WATERED && isAtCurrentPatch(currentPatch)) {
            // wait until next stage
            while(patchStates.get(currentPatch) == PatchState.WATERED) {
                robot.delay(150);
            }
        } else if(patchStates.get(currentPatch) == PatchState.GROWN && isAtCurrentPatch(currentPatch)) {
            // click patch
            shouldDelayBit();
            clickPatch(patchNumByTile.get(currentPatch));

            while(patchStates.get(currentPatch) == PatchState.GROWN) {
                robot.delay(150);
            }
            // increment patch
            if(patchStates.get(currentPatch) == PatchState.EMPTY) {
                // increment patch
                incrementPatch();
                moveToNextTile();
            }
        } else if(patchStates.get(currentPatch) == PatchState.DEAD && isAtCurrentPatch(currentPatch)) {
            // click patch
            shouldDelayBit();
            clickPatch(patchNumByTile.get(currentPatch));

            while(patchStates.get(currentPatch) == PatchState.DEAD) {
                robot.delay(150);
            }
            // increment patch
            if(patchStates.get(currentPatch) == PatchState.EMPTY) {
                // increment patch
                incrementPatch();
                moveToNextTile();
            }
        }
        rotateCamera();
    }

    private boolean countEmptyPatches() {
        return patchStates.stream().filter(patch -> patch.equals(PatchState.EMPTY)).count() == 20;
    }

    private void panCameraToWaterBarrelFromPatch0() {
        panCameraOneDirection(KeyEvent.VK_W, 500);
        robot.delay(500);
        getWorldPointCoords(LocalPoint.fromWorld(client, TitheFarmPlusWorldPoints.waterBarrelWorldPoint));
        robot.delay(500);
        client.setOculusOrbState(0);
        client.setOculusOrbNormalSpeed(12);
        robot.delay(500);
    }

    private void panCameraToPatch0FromWaterBarrel() {
        setCameraZoom(530);
        robot.delay(500);
        panCameraOneDirection(KeyEvent.VK_S, 500);
        robot.delay(500);
        getWorldPointCoords(LocalPoint.fromWorld(client, patchWalkTilesByWorldPoint.get(0).get(0)));
        robot.delay(500);
        client.setOculusOrbState(0);
        client.setOculusOrbNormalSpeed(12);
        robot.delay(500);
        fillWateringCan = false;
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

    private void rotateCamera() {
        if(currentPatch <= 7 || currentPatch == 19)
            changeCameraYaw(0);
        else if (currentPatch <= 18)
            changeCameraYaw(1024);
    }

    private void incrementPatch() {
        if(currentPatch == 19)
            currentPatch = 0;
        else
            currentPatch++;
    }

    private void shouldDelayBit() {
        if(currentPatch == 3 || currentPatch == 4 || currentPatch == 11 || currentPatch == 12)
            robot.delay(400);
    }

    private void moveToNextTile() {
        List<WorldPoint> newPatchWorldPoints = patchWalkTilesByWorldPoint.get(currentPatch);
        clickPatch(newPatchWorldPoints.get(0));
    }

    private void clickFirstSlot() {
        scheduledPointDelay(new Point(782, 768), 3);
        robot.delay(250);
    }

    private void clickPatch(WorldPoint worldPoint) {
        getWorldPointCoords(LocalPoint.fromWorld(client, worldPoint));
        robot.delay(250);
    }

    private void clickPatch(Tile tile) {
        WorldPoint tileWP = tile.getWorldLocation();
        getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(tileWP.getX() + 1, tileWP.getY() + 1, tileWP.getPlane())));
        robot.delay(250);
    }

    private boolean isAtCurrentPatch(int currentPatch) {
        List<WorldPoint> patchTiles = patchWalkTilesByWorldPoint.get(currentPatch);
        return patchTiles.contains(client.getLocalPlayer().getWorldLocation());
    }

    private Point getCenterOfRectangle(Rectangle rectangle) {
        // +26 to the Y coordinate because calculations are taken from canvas, not window
        return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY() + 26);
    }

    private void getWorldPointCoords(final LocalPoint dest) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, dest, 1);
        if (poly == null)
        {
            return;
        }

        Rectangle boundingBox = poly.getBounds();

        Point obstacleCenter = getCenterOfRectangle(boundingBox);

        scheduledPointDelay(obstacleCenter, 3);
    }

    private void scheduledPointDelay(Point point, int sigma) {
        isIdle = false;
        try {
            MouseCoordCalculation.generateCoordNoRand(client, point, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
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
        robot.delay(500);
//        north: 0
//        east:1536
//        south:1024
//        west:512
    }

    private void setCameraZoom(int zoom) {
        clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom));
    }

    public int getRegionID() {
        return WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
    }

    private void closeToLogout() {
        Widget timeWidget = client.getWidget(10616865);

        if(timeWidget != null) {
            String timeWidgetTime = timeWidget.getText();
            List<String> timeSplit = Arrays.asList(timeWidgetTime.split(":"));
            // at 5 hr mark
            if(Integer.parseInt(timeSplit.get(0)) == 5 && Integer.parseInt(timeSplit.get(1)) == 50)
                logout = true;
        }
    }
}
