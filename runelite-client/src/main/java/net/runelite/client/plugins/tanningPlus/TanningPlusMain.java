package net.runelite.client.plugins.tanningPlus;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.NPC;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.agilityPlus.MouseCoordCalculation;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TanningPlusMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    public static boolean isIdle = true;
    public long start;
    Robot robot = new Robot();

    public static boolean isRunning = false;

    private static final Set<Integer> dragonHides = ImmutableSet.of(
            ItemID.GREEN_DRAGONHIDE,
            ItemID.BLUE_DRAGONHIDE
    );

    private static final Set<Integer> dragonLeather = ImmutableSet.of(
            ItemID.GREEN_DRAGON_LEATHER,
            ItemID.BLUE_DRAGON_LEATHER
    );

    public static List<Item> inventoryItems = new ArrayList<>();

    private final WorldPoint BANK_TILE = new WorldPoint(2935, 3280, 0);
    private final WorldPoint TANNER_STAIR_TILE = new WorldPoint(2933, 3282, 1);
    private final WorldPoint BOTTOM_STAIR_TILE = new WorldPoint(2932, 3281, 0);

    Thread t;

    TanningPlusMain(Client client, ClientThread clientThread) throws AWTException {
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

            try {
                if (getRegionID() == 11571)
                    doTanning();
            } catch (Exception e) {
                System.out.println("stuff happened");
            }
        }
        System.out.println("Thread has stopped.");
    }

    private void doTanning() {
        if(isBankOpen() && readyForBank() && isIdle) {
            System.out.println("banking");
            bankingSequence();
        } else if(isBankOpen() && readyForTanner() && isIdle) {
            System.out.println("close bank");
            pressKey(KeyEvent.VK_ESCAPE, 100);
            delay(250);
        } else if(isAtWorldPoint(BANK_TILE) && !isBankOpen() && readyForTanner() && isIdle) {
            System.out.println("click bottom stairs");
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            setCameraZoom(640);
            delay(500);
            scheduledPointDelay(new Point(117, 283), 12);
            delay(1500);
        } else if((isAtWorldPoint(TANNER_STAIR_TILE) || client.getLocalPlayer().getWorldLocation().getPlane() == 1) && readyForTanner() && !isTanningOpen() && !isIdle) {
            isIdle = true;
        } else if((isAtWorldPoint(TANNER_STAIR_TILE) || client.getLocalPlayer().getWorldLocation().getPlane() == 1) && readyForTanner() && !isTanningOpen() && isIdle) {
            System.out.println("click tanner");
            delay(500);
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            setCameraZoom(520);
            delay(500);
            // assume tanner was clicked correctly
            scheduledNPCDelay(TanningPlusPlugin.tanner, 4);
            delay(2000);
            isIdle = true;
        } else if(isTanningOpen() && readyForTanner() && isIdle) {
            System.out.println("click dragonhide");
            delay(500);
            // green dragon hide
            scheduledPointDelay(new Point(189, 532), 12);
            delay(1000);
            isIdle = true;
        } else if(readyForBank() && client.getLocalPlayer().getWorldLocation().getPlane() == 1 && isIdle) {
            System.out.println("click top stairs");
            scheduledGameObjectDelay(TanningPlusPlugin.topStair, 6);
            delay(2000);
        } else if(readyForBank() && client.getLocalPlayer().getWorldLocation().getPlane() == 0 && !isIdle) {
            isIdle = true;
        } else if(isAtWorldPoint(BOTTOM_STAIR_TILE) && readyForBank() && isIdle) {
            System.out.println("click bank");
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            setCameraZoom(640);
            delay(750);
            scheduledGameObjectDelay(TanningPlusPlugin.bankChest, 5);
            delay(500);
        }
    }

    private boolean isBankOpen() {
        return client.getWidget(WidgetInfo.BANK_CONTAINER) != null;
    }

    private boolean isTanningOpen() {
        Widget tanningWindow = client.getWidget(21233752);
        if(tanningWindow != null) {
            return tanningWindow.getText().contains("like tanning?");
        }
        return false;
    }

    private boolean readyForTanner() {
        try {
            return inventoryItems.stream()
                    .filter(item -> dragonHides.contains(item.getId()))
                    .count() >= 20;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean readyForBank() {
        try {
            return inventoryItems.stream()
                    .filter(item -> dragonLeather.contains(item.getId()))
                    .count() >= 20;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void bankingSequence() {
        // deposit leather (click slot 1)
        // scheduledPointDelay(new Point(782, 768), 3);
        // 524, 793 bottom right bank

        // deposit slot 1
        scheduledPointDelay(new Point(782, 768), 3);
        delay(700);

        if(!hasEnoughStamina(20)) {
            // withdraw strange fruit
            scheduledPointDelay(new Point(476, 777), 3);
            delay(700);
            // eat
            scheduledPointDelay(new Point(782, 768), 3);
            delay(700);
        }

        // withdraw hides
        scheduledPointDelay(new Point(524, 793), 3);
        delay(700);
        pressKey(KeyEvent.VK_ESCAPE, 100);
        delay(1000);
        isIdle = true;
    }

    private boolean hasEnoughStamina(int stamina) {
        return client.getEnergy()/100 >= stamina;
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private void getObstacleCenter(GameObject gameObject, int sigma) {
        final Shape groundObjectConvexHull = gameObject.getConvexHull();
        if(groundObjectConvexHull == null) return;

        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, gameObject, sigma);
    }

    private void getNPCCenter(NPC npc, int sigma) {
        final Shape groundObjectConvexHull = npc.getConvexHull();
        if(groundObjectConvexHull == null) return;

        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, npc, sigma);
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

    private void scheduledNPCDelay(NPC npc, int sigma) {
        isIdle = false;
        try {
            getNPCCenter(npc, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    private void scheduledPointDelay(Point point, int sigma) {
        isIdle = false;
        try {
            MouseCoordCalculation.generateCoord(point, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
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
        delay(200);
        start = System.currentTimeMillis();
    }

    private boolean checkLastReset() {
        long end = System.currentTimeMillis();
        double sec = (end - start) / 1000F;

        return sec >= 5;
    }
}
