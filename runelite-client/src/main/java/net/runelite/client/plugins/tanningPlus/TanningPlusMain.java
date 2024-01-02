package net.runelite.client.plugins.tanningPlus;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.agilityPlusV2.MouseCoordCalculation;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TanningPlusMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    public static boolean isIdle = true;
    public long start;

    public static boolean isRunning = false;

    public static List<Item> inventoryItems = new ArrayList<>();

    Random random = new Random();

    private static final Set<Integer> dragonLeather = ImmutableSet.of(
            ItemID.GREEN_DRAGON_LEATHER,
            ItemID.BLUE_DRAGON_LEATHER,
            ItemID.RED_DRAGON_LEATHER,
            ItemID.BLACK_DRAGON_LEATHER
    );

    private static final Set<Integer> dragonHide = ImmutableSet.of(
            ItemID.GREEN_DRAGONHIDE,
            ItemID.BLUE_DRAGONHIDE,
            ItemID.RED_DRAGONHIDE,
            ItemID.BLACK_DRAGONHIDE
    );

    Thread t;

    TanningPlusMain(Client client, ClientThread clientThread) {
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
                    || isAtWorldPoint(new WorldPoint(3185, 3444, 0)))
                doTanning();
        }
        System.out.println("Thread has stopped.");
    }

    private void doTanning() {
        // click bank
        if(!isBankOpen() && checkInventoryCount(dragonLeather, 25) && isIdle) {
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            setCameraZoom(1004);
            delay(500);
            scheduledPointDelay(new Point(760, 533), 8);
            delay(1000);
            isIdle = true;
        }
        // bank is open and bank sequence
        else if(isBankOpen() && checkInventoryCount(dragonLeather, 25) && isIdle) {
            delay(300);
            bankingSequence();
        }
        // click tan if have atleast 5 hide
        else if(!isBankOpen() && checkInventoryCount(dragonHide, 5) && isIdle) {
            System.out.println("press tan");
            delay(300);
            pressKey(KeyEvent.VK_F6);
            delay(300);
            // + 20, +20 from top left of sprite
            if(checkInventoryCount(dragonHide, 25))
                scheduledPointDelay(new Point(843, 864), 4); // row 1 col 1
            else
                try {
                    MouseCoordCalculation.mouseClick();
                } catch (Exception e) {
                    e.printStackTrace();
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

    private boolean checkInventoryCount(Set<Integer> itemSet, int count) {
        try {
            return inventoryItems.stream()
                    .filter(item -> itemSet.contains(item.getId()))
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
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key);
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
