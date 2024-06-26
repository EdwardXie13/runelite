package net.runelite.client.plugins.rcPlus;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.Perspective;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.agilityPlusV2.MouseCoordCalculation;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RCPlusMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    public static boolean isIdle = true;
    public static boolean resetOculusOrb = false;
    public long start;
    public static boolean isRunning = false;
    Robot robot = new Robot();

    public static Set<Integer> fireAltarRegions = ImmutableSet.of(12344, 13130, 13106, 10315, 12600);
    public static List<Item> inventoryItems = new ArrayList<>();

    Thread t;

    RCPlusMain(Client client, ClientThread clientThread) throws AWTException {
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

            if (fireAltarRegions.contains(getRegionID()))
                    doFireAltar();
        }
        System.out.println("Thread has stopped.");
    }

    private void doFireAltar() {
        if(checkLevelUp()) {
            pressKey(KeyEvent.VK_SPACE, 100);
            delay(500);
        } else if(isBankOpen() && !readyForAltar() && isIdle) {
            bankingSequence();
        } else if(isBankOpen() && readyForAltar() && isIdle) {
            pressKey(KeyEvent.VK_ESCAPE, 100);
            delay(500);
        } else if(isNearWorldTile(RCPlusWorldPoints.FEROX_ENCLAVE_BANK_TILE, 2) && !isBankOpen() && !readyForAltar() && isIdle) {
            scheduledGameObjectDelay(RCPlusObjectIDs.feroxEnclaveBank, 10);
            delay(1500);
            if(isBankOpen())
                bankingSequence();
        } else if(isNearWorldTile(RCPlusWorldPoints.FEROX_ENCLAVE_BANK_TILE, 2) && !isBankOpen() && readyForAltar() && !hasEnoughStamina() && isIdle) {
            client.setCameraPitchTarget(60);
            changeCameraYaw(1934);
            setCameraZoom(45);
            delay(1000);
            scheduledPointDelay(new Point(437, 553), 24);
            delay(1500);
        } else if(getRegionID() == 13130 && isIdle) { // is in FFA portal
            // TP to duel arena
            isIdle = false;
            client.setCameraPitchTarget(512);
            delay(250);
            pressKey(KeyEvent.VK_F4, 100);
            delay(250);
            scheduledPointDelay(new Point(899, 924), 6);
            delay(500);
            pressKey(KeyEvent.VK_ESCAPE, 100);
            delay(3000);
            isIdle = true;
            // need to fix double click to TP
        } else if(isNearWorldTile(RCPlusWorldPoints.FEROX_ENCLAVE_BANK_TILE, 2) && !isBankOpen() && readyForAltar() && hasEnoughStamina() && isIdle) {
            // TP to duel arena
            pressKey(KeyEvent.VK_F4, 100);
            delay(250);
            scheduledPointDelay(new Point(899, 924), 6);
            delay(500);
            pressKey(KeyEvent.VK_ESCAPE, 100);
            delay(5000);
        } else if(getRegionID() == 13106 && isNearWorldTile(new WorldPoint(3315, 3236, 0), 4)) {
            pressKey(KeyEvent.VK_ESCAPE, 100);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            setCameraZoom(500);
            delay(500);
            panCameraOneDirection(KeyEvent.VK_W, 1000);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusWorldPoints.FIRE_ALTAR_MYSTERIOUS_RUINS));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(500);
            changeCameraYaw(0);
            setCameraZoom(700);
            delay(2500);
        } else if(isNearWorldTile(RCPlusWorldPoints.FIRE_ALTAR_MYSTERIOUS_RUINS, 3) && isIdle) {
            isIdle = false;
            delay(500);
            scheduledPointDelay(new Point(627, 278), 12);
            delay(2000);
            isIdle = true;
        }
//        else if(isAtWorldPoint(RCPlusWorldPoints.FIRE_ALTAR_MYSTERIOUS_RUINS_TWO) && isIdle) {
//            isIdle = false;
//            delay(500);
//            scheduledPointDelay(new Point(331, 815), 12);
//            delay(2000);
//            isIdle = true;
//        }
        else if(isNearWorldTile(RCPlusWorldPoints.FIRE_ALTAR_ENTRANCE, 2) && isIdle) {
            changeCameraYaw(1024);
            setCameraZoom(896);
            delay(500);
            panCameraToFireAltar();
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(2584, 4840, 0)));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(500);
            setCameraZoom(778);
            delay(1000);
        } else if(isNearWorldTile(new WorldPoint(2584, 4840, 0), 2) && isIdle) {
            if(readyForAltar()) {
                isIdle = false;
                scheduledPointDelay(new Point(341, 228), 18);
                delay(500);
//                scheduledPointDelay(new Point(782, 804), 3);
//                delay(300);
//                scheduledPointDelay(new Point(341, 228), 18);
                isIdle = true;
            } else {
                isIdle = false;
                delay(250);
                pressKey(KeyEvent.VK_F4, 100);
                delay(250);
                scheduledPointDelay(new Point(899, 924), 6);
                delay(500);
                pressKey(KeyEvent.VK_ESCAPE, 100);
                delay(3000);
                isIdle = true;
            }
        } else if(isNearWorldTile(new WorldPoint(3151, 3634, 0), 4)) {
            changeCameraYaw(0);
            setCameraZoom(780);
            delay(500);
            panCameraToFeroxBank();
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusWorldPoints.FEROX_ENCLAVE_BANK_TILE));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(500);
//            changeCameraYaw(randomChangeCameraYawExceptWest());
            setCameraZoom(896);
            delay(9000);
        }
    }

    private boolean isBankOpen() {
        return client.getWidget(WidgetInfo.BANK_CONTAINER) != null;
    }

    private boolean hasEnoughStamina() {
        double energy = client.getEnergy() / 100.0;
        return energy >= 40.0;
    }

    private boolean readyForAltar() {
        if(!isRingOfDuelingEquipped()) return false;
        try {
            return inventoryItems.stream()
                    .filter(item -> item.getId() == ItemID.RUNE_ESSENCE || item.getId() == ItemID.PURE_ESSENCE)
                    .count() >= 20;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private int randomChangeCameraYawExceptWest() {
        try {
            Random random = new Random();
            int num = random.nextInt(10000);

            if (num < 2000) {
                return 0;
            } else if (num < 5000){
                return 1024;
            } else {
                return 1536;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    private void bankingSequence() {
        // cheese by always having slot 1 empty so the bound runes go there
        // deposit non pouches (click slot 1)
//        scheduledPointDelay(new Point(782, 768), 3);

        scheduledPointDelay(new Point(540, 827), 3);
        delay(700);

        //does have duel ring?
        if(!isRingOfDuelingEquipped()) {
            scheduledPointDelay(new Point(428, 721), 3);
            delay(700);
            scheduledPointDelay(new Point(782, 768), 3);
            delay(700);
        }

//        // withdraw ess
//        scheduledPointDelay(new Point(476, 721), 3);
//        delay(500);
//        // fill small pouch
//        scheduledPointDelay(new Point(782, 804), 3);
//        delay(500);
        // withdraw ess
        scheduledPointDelay(new Point(476, 721), 3);
        delay(700);
        pressKey(KeyEvent.VK_ESCAPE, 100);
        delay(1000);
        isIdle = true;
    }

    private boolean isRingOfDuelingEquipped() {
        List<Item> equipItems = new ArrayList<>(Arrays.asList(client.getItemContainer(InventoryID.EQUIPMENT).getItems()));
        Set<Integer> rod = new HashSet<>(Arrays.asList(2552, 2554, 2556, 2558, 2560, 2562, 2564, 2566));

        for(Item i : equipItems) {
            if(rod.contains(i.getId()))
                return true;
        }

        return false;
    }

    private void panCameraToFireAltar() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_A, 600);
        pressKey(KeyEvent.VK_W, 400);
    }

    private void panCameraToFeroxBank() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_A, 1400);
        pressKey(KeyEvent.VK_S, 100);
    }

    private void panCameraOneDirection(int keyEvent, int ms) {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(keyEvent, ms);
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private void getObstacleCenter(GameObject gameObject, int sigma) {
        final Shape groundObjectConvexHull = gameObject.getConvexHull();
        if(groundObjectConvexHull == null) return;

        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(client, obstacleCenter, gameObject, sigma);
    }

    private Point getCenterOfRectangle(Rectangle rectangle) {
        // +26 to the Y coordinate because calculations are taken from canvas, not window
        return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY() + 26);
    }

    private boolean isAtWorldPoint(WorldPoint worldPoint) {
        boolean playerX = client.getLocalPlayer().getWorldLocation().getX() == worldPoint.getX();
        boolean playerY = client.getLocalPlayer().getWorldLocation().getY() == worldPoint.getY();
        return playerX && playerY;
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
        } catch (Exception e) {
            e.getStackTrace(); }
    }

    private boolean checkLevelUp() {
        Widget levelUpMessage = client.getWidget(10617391);
        return !levelUpMessage.isSelfHidden();
    }

    private void pressKey(int key, int ms) {
        robot.keyPress(key);
        robot.delay(ms);
        robot.keyRelease(key);
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
        return this.client.getLocalPlayer().getWorldLocation().distanceTo2D(target) < range;
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
}
