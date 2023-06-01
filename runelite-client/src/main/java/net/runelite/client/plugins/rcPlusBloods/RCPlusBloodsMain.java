package net.runelite.client.plugins.rcPlusBloods;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.Perspective;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.agilityPlus.MouseCoordCalculation;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RCPlusBloodsMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    public static boolean isIdle = true;
    public static boolean resetOculusOrb = false;
    public long start;

    public static Set<Integer> bloodAltarRegions = ImmutableSet.of(
            6716, // running to altar
            6972, // mine region
            6971, // altar outcrop region
            6715 // blood altar
    );

    public static List<Item> inventoryItems = new ArrayList<>();

    Thread t;

    RCPlusBloodsMain(Client client, ClientThread clientThread)
    {
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
        while (!Thread.interrupted()) {
            if (checkIdle() && checkLastReset())
                reset();

            try {
                if (bloodAltarRegions.contains(getRegionID()))
                    doBloodRunes();
            } catch (Exception e) {
                System.out.println("stuff happened");
            }
        }
        System.out.println("Thread has stopped.");
    }

    private void doBloodRunes() {
        if(checkLevelUp()) {
            pressKey(KeyEvent.VK_SPACE);
            delay(500);
            isIdle = true;
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.NORTH_RUNESTONE) && RCPlusBloodsPlugin.denseRunestoneSouthMineable && (determineStatus()==STATUS.NOT_READY || determineStatus()==STATUS.RETURN_TO_ROCK || determineStatus()==STATUS.READY_TO_BLOOD2) && hasEnoughStamina(5) && isIdle) {
            System.out.println("click south");
            isIdle = false;
            changeCameraYaw(0);
            setCameraZoom(290);
            delay(250);
            if(RCPlusBloodsPlugin.denseRunestoneNorthMineable)
                scheduledPointDelay(new Point(575, 399), 10);
            else
                scheduledPointDelay(new Point(563, 910), 10);
            delay(1000);
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.SOUTH_RUNESTONE_INSIDE) && RCPlusBloodsPlugin.denseRunestoneNorthMineable && (determineStatus()==STATUS.NOT_READY || determineStatus()==STATUS.RETURN_TO_ROCK || determineStatus()==STATUS.READY_TO_BLOOD2) && hasEnoughStamina(5) && isIdle) {
            System.out.println("click north");
            isIdle = false;
            changeCameraYaw(0);
            setCameraZoom(286);
            delay(250);
            if(RCPlusBloodsPlugin.denseRunestoneSouthMineable)
                scheduledPointDelay(new Point(574, 662), 10);
            else
                scheduledPointDelay(new Point(561, 158), 10);
            delay(1000);
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.NORTH_RUNESTONE) && determineStatus()==STATUS.READY_TO_VENERATE && hasEnoughStamina(60) && isIdle) {
            System.out.println("click rock");
            isIdle = false;
            changeCameraYaw(0);
            setCameraZoom(650);
            delay(250);
            panCameraOneDirection(KeyEvent.VK_W, 1000);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusBloodsWorldPoints.BEFORE_ROCKS));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
        } else if(isNearWorldTile(RCPlusBloodsWorldPoints.SOUTH_RUNESTONE_OUTSIDE, 2) && determineStatus()==STATUS.READY_TO_VENERATE && hasEnoughStamina(60) && isIdle) {
            System.out.println("click rock");
            isIdle = false;
            changeCameraYaw(0);
            setCameraZoom(650);
            delay(250);
            panCameraOneDirection(KeyEvent.VK_W, 1400);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusBloodsWorldPoints.BEFORE_ROCKS));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.BEFORE_ROCKS) && determineStatus()==STATUS.READY_TO_VENERATE && isIdle) {
            System.out.println("climb rock");
            isIdle = false;
            changeCameraYaw(0);
            setCameraZoom(896);
            delay(500);
            scheduledPointDelay(new Point(525, 303), 10);
            delay(2500);
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.AFTER_ROCKS) && determineStatus()==STATUS.READY_TO_VENERATE && hasEnoughStamina(17) && isIdle) {
            System.out.println("run to venerate");
            isIdle = false;
            changeCameraYaw(0);
            setCameraZoom(800);
            delay(500);
            panCameraOneDirection(KeyEvent.VK_W, 2100);
            panCameraOneDirection(KeyEvent.VK_A, 1900);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusBloodsWorldPoints.VENERATE_ALTAR_TILE));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(1500);
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.AFTER_ROCKS) && (determineStatus()==STATUS.NOT_READY || determineStatus()==STATUS.READY_TO_BLOOD2)) {
            System.out.println("climb the rock");
            changeCameraYaw(0);
            setCameraZoom(896);
            delay(500);
            scheduledPointDelay(new Point(497, 731), 12);
            delay(2500);
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.BEFORE_ROCKS) && (determineStatus()==STATUS.NOT_READY || determineStatus()==STATUS.READY_TO_BLOOD2) && hasEnoughStamina(15) && isIdle) {
            System.out.println("run to runestone");
            isIdle = false;
            changeCameraYaw(0);
            setCameraZoom(650);
            delay(500);
            if(RCPlusBloodsPlugin.denseRunestoneNorthMineable) {
                panCameraOneDirection(KeyEvent.VK_S, 1000);
                delay(500);
                getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusBloodsWorldPoints.NORTH_RUNESTONE));
            } else {
                panCameraOneDirection(KeyEvent.VK_S, 1400);
                delay(500);
                getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusBloodsWorldPoints.SOUTH_RUNESTONE_INSIDE));
            }
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            // if north up, pan north
            // if south up, pan south
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.VENERATE_ALTAR_TILE) && determineStatus()==STATUS.READY_TO_VENERATE && isIdle) {
            System.out.println("venerate rocks");
            isIdle = false;
            changeCameraYaw(0);
            setCameraZoom(720);
            delay(500);
            scheduledPointDelay(new Point(181, 385), 14);
            delay(2500);
            isIdle = true;
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.VENERATE_ALTAR_TILE) && determineStatus()==STATUS.READY_TO_RUN_BACK && hasEnoughStamina(17) && isIdle) {
            System.out.println("run back to rocks");
            isIdle = false;
            panCameraOneDirection(KeyEvent.VK_D, 3000);
            panCameraOneDirection(KeyEvent.VK_S, 400);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusBloodsWorldPoints.AFTER_ROCKS));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            chisel();
            System.out.println("done chisel run back to rocks");
            isIdle = true;
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.VENERATE_ALTAR_TILE) && determineStatus()==STATUS.READY_TO_BLOOD && hasEnoughStamina(60) && isIdle) {
            System.out.println("to the blood altar");
            isIdle = false;
            panCameraOneDirection(KeyEvent.VK_S, 3000);
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusBloodsWorldPoints.RUN_TO_BLOOD_ALTAR_SPOT));
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(1500);
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.RUN_TO_BLOOD_ALTAR) && determineStatus()==STATUS.READY_TO_BLOOD && isIdle) {
            System.out.println("bind first inventory");
            isIdle = false;
            changeCameraYaw(0);
            setCameraZoom(278);
            delay(500);
            scheduledPointDelay(new Point(53, 434), 10);
            delay(4000);
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.BLOOD_ALTAR_BIND_ESS) && isIdle) {
            if(determineStatus()==STATUS.READY_TO_BLOOD) {
                System.out.println("bind frags");
                scheduledPointDelay(new Point(174, 333), 14);
                delay(1000);
            } else if(determineStatus()==STATUS.READY_TO_BLOOD2) {
                System.out.println("bind frags 2");
                scheduledPointDelay(new Point(174, 333), 14);
                delay(1000);
            } else if(determineStatus()==STATUS.CHISEL_AT_BLOOD) {
                System.out.println("chisel blocks");
                changeCameraYaw(0);
                setCameraZoom(684);
                delay(500);
                chisel();
                delay(500);
            } else if(determineStatus()==STATUS.RETURN_TO_ROCK && hasEnoughStamina(30)) {
                System.out.println("run back to slide");
                setCameraZoom(700);
                delay(500);
                panCameraOneDirection(KeyEvent.VK_W, 1700);
                panCameraOneDirection(KeyEvent.VK_A, 1400);
                delay(500);
                getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusBloodsWorldPoints.BEFORE_RETURN_ROCKSLIDE));
                delay(500);
                client.setOculusOrbState(0);
                client.setOculusOrbNormalSpeed(12);
                delay(2000);
            }
            isIdle = true;
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.BEFORE_RETURN_ROCKSLIDE) && isIdle) {
            delay(1200);
            changeCameraYaw(0);
            setCameraZoom(896);
            delay(700);
            scheduledPointDelay(new Point(665, 510), 10);
            delay(1500);
        } else if(isAtWorldPoint(RCPlusBloodsWorldPoints.AFTER_RETURN_ROCKSLIDE) && hasEnoughStamina(15) && isIdle) {
            setCameraZoom(700);
            delay(500);
            panCameraOneDirection(KeyEvent.VK_D, 600);
            if(RCPlusBloodsPlugin.denseRunestoneNorthMineable) {
                delay(500);
                getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusBloodsWorldPoints.NORTH_RUNESTONE));
            } else {
                panCameraOneDirection(KeyEvent.VK_S, 200);
                delay(500);
                getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusBloodsWorldPoints.SOUTH_RUNESTONE_INSIDE));
            }
            delay(500);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            delay(500);
        }
    }

    private boolean hasEnoughStamina(int stamina) {
        return client.getEnergy() >= stamina;
    }

    private STATUS determineStatus() {
        // first slot empty, last slot block
        if(inventoryItems.get(0).equals(new Item(-1, 0)) && inventoryItems.get(27).equals(new Item(13446, 1))) {
            return STATUS.CHISEL_AT_BLOOD;
        }
        // first slot empty, last slot empty
        else if(inventoryItems.get(0).equals(new Item(-1, 0)) && inventoryItems.get(27).equals(new Item(-1, 0))) {
            return STATUS.RETURN_TO_ROCK;
        }
        // first slot blocks, last slot blocks
        else if(inventoryItems.get(0).equals(new Item(13446, 1)) && inventoryItems.get(27).equals(new Item(13446, 1))) {
            return STATUS.READY_TO_RUN_BACK;
        }
        // first slot frags, last slot block
        else if(inventoryItems.get(0).equals(new Item(7938, 1)) && inventoryItems.get(27).equals(new Item(13446, 1))) {
            return STATUS.READY_TO_BLOOD;
        }
        // first slot frags, last slot empty
        else if(inventoryItems.get(0).equals(new Item(7938, 1)) && inventoryItems.get(27).equals(new Item(-1, 0))) {
            return STATUS.READY_TO_BLOOD2;
        }
        // first and last slot check for full inv
        else if(inventoryItems.get(0).equals(new Item(13445, 1)) && inventoryItems.get(27).equals(new Item(13445, 1))) {
            return STATUS.READY_TO_VENERATE;
        }
        // first frags and last slot has block
        else if(inventoryItems.get(0).equals(new Item(7938, 1)) && inventoryItems.get(27).equals(new Item(13445, 1))) {
            return STATUS.READY_TO_VENERATE;
        }
        // last slot empty
        else if(inventoryItems.get(27).equals(new Item(-1, 0))) {
            return STATUS.NOT_READY;
        }

        return STATUS.UNDEFINED;
    }

    enum STATUS {
        READY_TO_VENERATE,
        READY_TO_RUN_BACK,
        READY_TO_BLOOD,
        READY_TO_BLOOD2,
        CHISEL_AT_BLOOD,
        RETURN_TO_ROCK,
        NOT_READY,
        UNDEFINED
    }

    private void chisel() {
        while(inventoryItems.get(27).equals(new Item(13446, 1))) {
            scheduledPointDelay(new Point(908, 948), 4);
            delay(13);
            scheduledPointDelay(new Point(908, 984), 4);
            delay(13);
        }
        scheduledPointDelay(new Point(908, 948), 4);
        delay(13);
        scheduledPointDelay(new Point(908, 984), 4);
        delay(13);

        System.out.println("done chisel");
        delay(500);
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

        MouseCoordCalculation.generateCoord(obstacleCenter, gameObject, sigma);
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

    private void pressKey(int key) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyRelease);
    }

    private void pressKey(int key, int ms) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyPress);

        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key);
        delay(ms);
        this.client.getCanvas().dispatchEvent(keyRelease);
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

        return sec >= 90;
    }
}
