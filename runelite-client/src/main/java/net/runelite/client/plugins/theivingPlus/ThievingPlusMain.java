package net.runelite.client.plugins.theivingPlus;

import com.google.common.collect.ImmutableList;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.agilityPlusV2.MouseCoordCalculation;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class ThievingPlusMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    public static boolean isIdle = true;
    public long start;
    public static boolean isRunning = false;
    Robot robot = new Robot();

    private static final List<Point> inventoryCoords = new ArrayList(ImmutableList.of(
            new Point(782, 768), // 1 row 1 col
            new Point(824, 768), // 1 row 2 col
            new Point(866, 768), // 1 row 3 col
            new Point(908, 768), // 1 row 4 col
            new Point(782, 804), // 2 row 1 col
            new Point(824, 804), // 2 row 2 col
            new Point(866, 804), // 2 row 3 col
            new Point(908, 804), // 2 row 4 col
            new Point(782, 840), // 3 row 1 col
            new Point(824, 840), // 3 row 2 col
            new Point(866, 840), // 3 row 3 col
            new Point(908, 840), // 3 row 4 col
            new Point(782, 876), // 4 row 1 col
            new Point(824, 876), // 4 row 2 col
            new Point(866, 876), // 4 row 3 col
            new Point(908, 876), // 4 row 4 col
            new Point(782, 912), // 5 row 1 col
            new Point(824, 912), // 5 row 2 col
            new Point(866, 912), // 5 row 3 col
            new Point(908, 912), // 5 row 4 col
            new Point(782, 948), // 6 row 1 col
            new Point(824, 948), // 6 row 2 col
            new Point(866, 948), // 6 row 3 col
            new Point(908, 948), // 6 row 4 col
            new Point(782, 984), // 7 row 1 col
            new Point(824, 984), // 7 row 2 col
            new Point(866, 984), // 7 row 3 col
            new Point(908, 984)  // 7 row 4 col
    ));

    Thread t;

    ThievingPlusMain(Client client, ClientThread clientThread) throws AWTException {
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
            else if(isAtWorldPoint(ThievingPlusWorldPoints.ARDY_CAKE_STALL_SPOT)) {
                doCakes();
            } else if (isAtWorldPoint(ThievingPlusWorldPoints.HOSIDIUS_FRUIT_STALL_SPOT)) {
                doFruits();
            }
        }
        System.out.println("Thread has stopped.");
    }

    private void doCakes() {
        System.out.println("doing cakes");
        if(ThievingPlusObjectIDs.cakeStall != null && isIdle) {
            System.out.println("stealing cakes");
            // click static spot
            scheduledPointDelay(new Point(278, 517), 4); // row 2 col 1
            robot.delay(1000);

            dropItemInInventory(new Point(782, 768));
            robot.mouseMove(278, 517);
        }
    }

    private void doFruits() {
        System.out.println("doing fruits");
        if(ThievingPlusObjectIDs.fruitStall != null && isIdle) {
            System.out.println("stealing fruits");
            // click static spot
            scheduledPointDelay(new Point(320, 539), 4); // row 2 col 1
            robot.delay(1000);

            dropItemInInventory(new Point(782, 768));
            robot.mouseMove(320, 539);
        }
    }

    private void dropItemInInventory(Point coord) {
        keyDown(KeyEvent.VK_SHIFT);
        scheduledPointClick(coord, 3);
        robot.delay(500);
        keyUp(KeyEvent.VK_SHIFT);
        isIdle = true;
    }

    private void scheduledPointDelay(Point point, int sigma) {
        isIdle = false;
        try {
            MouseCoordCalculation.generateCoordNoRand(point, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    private void getObstacleCenter(GameObject gameObject, int sigma) {
        Shape groundObjectConvexHull = gameObject.getConvexHull();
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

    private boolean checkLevelUp() {
        Widget levelUpMessage = client.getWidget(10617391);
        return !levelUpMessage.isSelfHidden();
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

    private void scheduledPointClick(Point canvasPoint, int sigma) {
        Point actualPoint = new Point(canvasPoint.x, canvasPoint.y);
        try {
            MouseCoordCalculation.generateCoord(client, actualPoint, sigma);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pressKey(int key) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyRelease);
    }

    private void pressKey(int key, int ms) {
        keyDown(key);
        robot.delay(ms);
        keyUp(key);
    }

    private void keyDown(int key) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyPress);
    }

    private void keyUp(int key) {
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyRelease);
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
        robot.delay(200);
        start = System.currentTimeMillis();
    }

    private boolean checkLastReset() {
        long end = System.currentTimeMillis();
        double sec = (end - start) / 1000F;

        return sec >= 5;
    }
}

