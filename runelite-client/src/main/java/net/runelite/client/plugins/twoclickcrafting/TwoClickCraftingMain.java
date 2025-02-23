package net.runelite.client.plugins.twoclickcrafting;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.agilityPlusV2.MouseCoordCalculation;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TwoClickCraftingMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    public static boolean isIdle = true;
    public long start;

    public static boolean isRunning = false;

    public static List<Item> inventoryItems = new ArrayList<>();

    private static final Set<Integer> dragonHideBodies = ImmutableSet.of(
            ItemID.GREEN_DHIDE_BODY,
            ItemID.BLUE_DHIDE_BODY,
            ItemID.RED_DHIDE_BODY,
            ItemID.BLACK_DHIDE_BODY
    );

    private static final Set<Integer> dragonLeather = ImmutableSet.of(
            ItemID.GREEN_DRAGON_LEATHER,
            ItemID.BLUE_DRAGON_LEATHER,
            ItemID.RED_DRAGON_LEATHER,
            ItemID.BLACK_DRAGON_LEATHER
    );

    Thread t;

    TwoClickCraftingMain(Client client, ClientThread clientThread) {
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
            if (checkIdle() && checkLastReset())
                reset();

            if (isAtWorldPoint(new WorldPoint(3094, 3489, 0))
                || isAtWorldPoint(new WorldPoint(3185, 3444, 0)))
                doPotionMaking();
        }
        System.out.println("Thread has stopped.");
    }

    private void doPotionMaking() {
        if(!isBankOpen() && getInventoryCount() == 14) {
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            setCameraZoom(977);
            delay(500);
            scheduledPointDelay(new Point(691, 539), 10);
        } else if(isBankOpen() && getInventoryCount() == 14) {
            delay(300);
            bankingSequence();
        } else if(!isBankOpen() && checkInventoryCount(ImmutableSet.of(ItemID.VIAL_OF_WATER), 14)) {
            delay(300);
            scheduledPointDelay(new Point(824, 876), 3); // row 4 col 2
            delay(300);
            scheduledPointDelay(new Point(866, 876), 3); // row 4 col 3
            delay(1000);
        }
    }

    private void doCrafting() {
        if(!isBankOpen() && checkInventoryCount(dragonHideBodies, 8) && isIdle) {
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            setCameraZoom(977);
            delay(500);
            scheduledPointDelay(new Point(691, 539), 10);
        } else if(isBankOpen() && checkInventoryCount(dragonHideBodies, 8) && isIdle) {
            delay(300);
            bankingSequence();
        } else if(!isBankOpen() && checkInventoryCount(dragonLeather, 26) && isIdle) {
            delay(300);
            scheduledPointDelay(new Point(782, 768), 3); // row 1 col 1
            delay(300);
            scheduledPointDelay(new Point(782, 804), 3); // row 2 col 1
            delay(300);
        }
    }

    private int getInventoryCount() {
        return inventoryItems.size();
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

    private void bankingSequence() {
        // deposit leather (click slot 1)
        // scheduledPointDelay(new Point(782, 768), 3);
        // 524, 793 bottom right bank

        // deposit slot 1
        scheduledPointDelay(new Point(782, 768), 3);
        delay(300);

        // withdraw lower right - 1
        scheduledPointDelay(new Point(473, 793), 3);
        delay(300);

        // withdraw lower right
        scheduledPointDelay(new Point(524, 793), 3);
        delay(1000);
        isIdle = true;
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
