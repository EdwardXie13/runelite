package net.runelite.client.plugins.plankmakeplus;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.agilityPlusV2.MouseCoordCalculation;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PlankmakePlusMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    public static boolean isIdle = true;
    public long start;

    public static boolean isRunning = false;

    public static List<Item> inventoryItems = new ArrayList<>();

    Random random = new Random();

    int mahoganyLogsID = ItemID.MAHOGANY_LOGS;
    int mahoganyPlankID = ItemID.MAHOGANY_PLANK;

    Thread t;

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
            new Point(782, 984) // 7 row 1 col
//            new Point(824, 984), // 7 row 2 col
//            new Point(866, 984), // 7 row 3 col
//            new Point(908, 984)  // 7 row 4 col
    ));

    PlankmakePlusMain(Client client, ClientThread clientThread) {
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

            if (isAtWorldPoint(new WorldPoint(3094, 3489, 0))
                    || isAtWorldPoint(new WorldPoint(3185, 3444, 0))) {
//                System.out.println("do plankmake");
                doPlankmake();
            }
        }
        System.out.println("Thread has stopped.");
    }

    private void doPlankmake() {
        // click bank
        if(!isBankOpen() && checkInventoryCount(mahoganyPlankID, 25) && isIdle) {
            System.out.println("clickBank");
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            setCameraZoom(1004);
            delay(500);
            scheduledPointDelay(new Point(760, 533), 8);
            delay(1000);
            isIdle = true;
        }
        // bank is open and bank sequence
        else if(isBankOpen() && checkInventoryCount(mahoganyPlankID, 25) && isIdle) {
            System.out.println("banking");
            delay(300);
            bankingSequence();
        }
        // bank is open and ready to plank
        else if(isBankOpen() && checkInventoryCount(mahoganyLogsID, 25) && isIdle) {
            System.out.println("closing bank");
            delay(300);
            pressKey(KeyEvent.VK_ESCAPE);
        }
        // click plankmake if have atleast 1 log
        else if(!isBankOpen() && checkInventoryCount(mahoganyLogsID, 25) && isIdle) {
            System.out.println("press plankmake");
            delay(300);
            pressKey(KeyEvent.VK_F6);
            delay(300);
            // + 20, +20 from top left of sprite
            for (Point inventoryCoord : inventoryCoords) {
                scheduledPointDelay(new Point(907, 893), 4);
                delay(300);
                scheduledPointDelay(inventoryCoord, 3); // row 1 col 1
                delay(random.nextInt(2000 - 1700) + 1700);
            }

            delay(random.nextInt(700 - 400) + 400);
            isIdle = true;
        }
    }

    private void bankingSequence() {
        // deposit leather (click slot 1)
        // scheduledPointDelay(new Point(782, 768), 3);
        // 524, 793 bottom right bank

        // deposit slot 1
        scheduledPointDelay(new Point(782, 768), 3);
        delay(700);

        // withdraw hides
        scheduledPointDelay(new Point(524, 793), 3);
        delay(1000);
        isIdle = true;
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private boolean checkInventoryCount(int itemID, int count) {
        try {
             return inventoryItems.stream()
                    .filter(item -> item.getId() == itemID)
                    .count() >= count;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

    private void scheduledPointDelay(Point point, int sigma) {
        isIdle = false;
        try {
            MouseCoordCalculation.generateCoord(client, point, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    private boolean isBankOpen() {
        return client.getWidget(WidgetInfo.BANK_CONTAINER) != null;
    }

    private void pressKey(int key) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyPress);
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
        isIdle = true;
        delay(200);
        start = System.currentTimeMillis();
    }

    private boolean checkLastReset() {
        long end = System.currentTimeMillis();
        double sec = (end - start) / 1000F;

        return sec >= 5;
    }
}
