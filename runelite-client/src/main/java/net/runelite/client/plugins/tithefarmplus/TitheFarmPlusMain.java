package net.runelite.client.plugins.tithefarmplus;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.tithefarmplus.TitheFarmPlusObjectIDs.PatchState;

import javax.inject.Inject;
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
import java.util.Random;

import static net.runelite.client.plugins.tithefarmplus.TitheFarmPlusWorldPoints.patchNumByTile;
import static net.runelite.client.plugins.tithefarmplus.TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint;

public class TitheFarmPlusMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    private final StepOverlay overlay;

    Robot robot = new Robot();
    public static boolean isRunning = false;
    public static boolean hasInit = false;
    private int currentPatch = 0;
    private boolean hasSetZoom = false;
    public static boolean fillWateringCan = false;
    private boolean logout = false;
    private static final int patches = 25;
    private boolean skipWalkToNext = false;
    private boolean pause = false;

    TitheClicker clicker;
    BreakScheduler scheduler;

    public static List<PatchState> patchStates = new ArrayList<>(Collections.nCopies(patches, PatchState.EMPTY));

    Thread t;

    TitheFarmPlusMain(Client client, ClientThread clientThread, StepOverlay overlay) throws AWTException {
        this.client = client;
        this.clientThread = clientThread;
        this.overlay = overlay;
        clicker = new TitheClicker(client);
        scheduler = new BreakScheduler();

        t = new Thread(this);
        System.out.println("New thread: " + t);
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
            setZoomPitchYaw(530, 512, 0);
            hasSetZoom = true;
        }

        if(!logout)
            closeToLogout();

        rotateCamera();

        if (isAtCurrentPatch(0) && isAllEmptyPatches() && logout) {
            overlay.setCurrentStep("logout time");
            logout();
            reset();
            t.interrupt();
            System.out.println("interrupted");
        }
        else if (isAtCurrentPatch(0) && isAllEmptyPatches() && pause) {
            int nextDelay = scheduler.getNextBreakDuration();
            overlay.setCurrentStep("delay " + nextDelay + "ms");
            // break delay into safe chunks
            for (long d = nextDelay; d > 0; d -= 60_000) {
                robot.delay((int) Math.min(d, 60_000));
            }

            pause = false;
        }
        else if (!pause) {
            if (patchStates.get(currentPatch) == PatchState.EMPTY && (isAtCurrentPatch(currentPatch) || skipWalkToNext)) {
                overlay.setCurrentStep("planting: " + currentPatch);
                clickFirstSlot();
                Point toClick = clicker.clickTile(patchNumByTile.get(currentPatch));
                clicker.clickPointHumanized(toClick, 70, 120, 20, 50);

                while (patchStates.get(currentPatch) == PatchState.EMPTY) {
                    robot.delay(new Random().nextInt(35) + 20);
                }
            } else if (patchStates.get(currentPatch) == PatchState.UNWATERED && (isAtCurrentPatch(currentPatch) || skipWalkToNext)) {
                overlay.setCurrentStep("watering: " + currentPatch);
                Point toClick = clicker.clickTile(patchNumByTile.get(currentPatch));
                clicker.clickPointHumanized(toClick, 70, 120, 20, 50);

                while (patchStates.get(currentPatch) == PatchState.UNWATERED) {
                    robot.delay(new Random().nextInt(35) + 20);
                }

                if (patchStates.get(currentPatch) == PatchState.WATERED) {
                    incrementPatch();

                    if (
                            currentPatch == 9 || currentPatch == 10 ||
                                    currentPatch == 12 || currentPatch == 13 ||
                                    currentPatch == 18 || currentPatch == 20 ||
                                    currentPatch == 22
                    ) {
                        skipWalkToNext = true;

                    } else {
                        moveToNextTile();
                        skipWalkToNext = false;
                    }
                }

            } else if (patchStates.get(currentPatch) == PatchState.WATERED && (isAtCurrentPatch(currentPatch) || skipWalkToNext)) {
                overlay.setCurrentStep("watered: " + currentPatch);
                while (patchStates.get(currentPatch) == PatchState.WATERED) {
                    robot.delay(new Random().nextInt(35) + 20);
                }
            } else if (patchStates.get(currentPatch) == PatchState.GROWN && isAtCurrentPatch(currentPatch)) {
                overlay.setCurrentStep("grown: " + currentPatch);
                Point toClick = clicker.clickTile(patchNumByTile.get(currentPatch));
                //                clicker.clickPoint(toClick);
                clicker.clickPointHumanized(toClick, 70, 120, 20, 50);

                while (patchStates.get(currentPatch) == PatchState.GROWN) {
                    robot.delay(new Random().nextInt(200) + 100);
                }

                if (patchStates.get(currentPatch) == PatchState.EMPTY) {
                    if (currentPatch == 23) {
                        if (fillWateringCan) {
                            clickWaterringCan();
                            WorldPoint barrelWP = TitheFarmPlusWorldPoints.returnWaterBarrel(patchNumByTile.get(currentPatch));
                            Point barrelPoint = worldPointToPoint(barrelWP);
                            clicker.clickPointHumanized(barrelPoint, 70, 120, 20, 50);
                            setZoomPitchYaw(420, 512, 0);
                            hasSetZoom = false;
                            robot.delay(3000);
                            fillWateringCan = false;
                        }
                    }
                    if (currentPatch == 24) {
//                            // generate randomStamina
//                            randomStamina = new Random().nextInt(16) + 20;
                        pause = true;
                        overlay.setCurrentStep("pause init");
                    }

                    incrementPatch();
                    moveToNextTile();
                }
            } else if (patchStates.get(currentPatch) == PatchState.DEAD && (isAtCurrentPatch(currentPatch) || skipWalkToNext)) {
                overlay.setCurrentStep("dead: " + currentPatch);
                Point toClick = clicker.clickTile(patchNumByTile.get(currentPatch));
                clicker.clickPoint(toClick);

                while (patchStates.get(currentPatch) == PatchState.DEAD) {
                    robot.delay(new Random().nextInt(10) + 15);
                }

                if (patchStates.get(currentPatch) == PatchState.EMPTY) {
                    incrementPatch();
                    moveToNextTile();
                }
            }
        }
    }

    private void rotateCamera() {
        if(currentPatch <= 7 || currentPatch == 23)
            changeCameraYaw(0);
        else if (currentPatch <= 22)
            changeCameraYaw(1024);
    }

    private boolean isAllEmptyPatches() {
        return patchStates.stream().filter(patch -> patch.equals(PatchState.EMPTY)).count() == patches;
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

    private void incrementPatch() {
        if(currentPatch == 24)
            currentPatch = 0;
        else
            currentPatch++;
    }

    private void moveToNextTile() {
        List<WorldPoint> newPatchWorldPoints = patchWalkTilesByWorldPoint.get(currentPatch);
        clicker.spamClickWorldPoint(newPatchWorldPoints.get(0));
    }

    private void clickFirstSlot() {
        clicker.clickPoint(new Point(782, 768));
    }

    private void clickWaterringCan() {
        clicker.clickPoint(new Point(782, 800));
    }

    private Point worldPointToPoint(WorldPoint worldPoint) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, LocalPoint.fromWorld(client, worldPoint), 1);
        if (poly == null)
        {
            return new Point(0,0);
        }

        Rectangle boundingBox = poly.getBounds();

        return getCenterOfRectangle(boundingBox);
    }

    private void clickPatch(WorldPoint worldPoint) {
        getWorldPointCoords(LocalPoint.fromWorld(client, worldPoint));
        robot.delay(100);
    }

    private void clickPatch(Tile tile) {
        WorldPoint tileWP = tile.getWorldLocation();
        getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(tileWP.getX() + 1, tileWP.getY() + 1, tileWP.getPlane())));
        robot.delay(100);
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
        clicker.clickPoint(obstacleCenter);
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

    private double getClientEnergy() {
        return client.getEnergy() / 100.0;
    }

    private boolean hasEnoughStamina(int energy) {
        return getClientEnergy() >= energy;
    }

    private void setZoomPitchYaw(int zoom, int pitch, int yaw) {
        setCameraZoom(zoom);
        setCameraPitch(pitch);
        setCameraYaw(yaw);
    }

    private void setCameraZoom(int zoom) {
        clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom));
    }

    private void setCameraPitch(int pitch) {
        if(client.getCameraPitch() == pitch)
            return;
        client.setCameraPitchTarget(pitch);
    }

    private void setCameraYaw(int yaw) {
        if(client.getCameraYaw() == yaw)
            return;
        client.setCameraYawTarget(yaw);
    }

    public void stop() {
        isRunning = false;
        t.interrupt(); // in case it's sleeping or waiting
        reset();
    }

    public void logout() {
        robot.keyPress(KeyEvent.VK_F12);
        robot.keyRelease(KeyEvent.VK_F12);
        robot.delay(1500);
        clicker.clickPoint(new Point(828, 971));
    }

    public void reset() {
        isRunning = false;
        hasInit = false;
        currentPatch = 0;
        hasSetZoom = false;
        fillWateringCan = false;
        logout = false;
        skipWalkToNext = false;
        pause = false;
    }
}
