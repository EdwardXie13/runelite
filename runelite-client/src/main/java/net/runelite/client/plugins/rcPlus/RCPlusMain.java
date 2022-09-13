package net.runelite.client.plugins.rcPlus;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RCPlusMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    public static boolean isIdle = true;
    public static boolean resetOculusOrb = false;
    public long start;
                                                                // falador area,            air altar
    public static Set<Integer> airAltarRegions = ImmutableSet.of(12084, 12083, 11827, 11828, 11339);

    Thread t;

    RCPlusMain(Client client, ClientThread clientThread)
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

            if(airAltarRegions.contains(getRegionID())) {
                doAirAltar();
            }
//            else if() {
//
//            }

        }
        System.out.println("Thread has stopped.");
    }

    private void doAirAltar() {
        if(isNearWorldTile(RCPlusWorldPoints.INFRONT_OF_BANK_BOOTH_TILE3, 2) && readyForAltar() && client.getOculusOrbState() == 0 && isIdle) {
            setCameraZoom(750);
            panCameraToAirAltarPath();
            delay(200);
            getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusWorldPoints.FROM_BANK_TO_PATH));
            delay(500);
            client.setOculusOrbState(0);
            delay(1000);
            //pan to altar
            //click the altar
        } else if(isNearWorldTile(RCPlusWorldPoints.INFRONT_OF_BANK_BOOTH_TILE3, 2) && !readyForAltar() && isIdle) {
            //click bank
            changeCameraYaw(0);
            setCameraZoom(876);
            delay(500);
            if(isAtWorldPoint(RCPlusWorldPoints.INFRONT_OF_BANK_BOOTH_TILE2))
                scheduledGameObjectDelay(RCPlusObjectIDs.airFaladorBankBooth2, 10);
            else if(isAtWorldPoint(RCPlusWorldPoints.INFRONT_OF_BANK_BOOTH_TILE3))
                scheduledGameObjectDelay(RCPlusObjectIDs.airFaladorBankBooth3, 10);
            else if(isAtWorldPoint(RCPlusWorldPoints.INFRONT_OF_BANK_BOOTH_TILE4))
                scheduledGameObjectDelay(RCPlusObjectIDs.airFaladorBankBooth4, 10);
            //bank function
            delay(2000);
            bankingSequence(false, false, false);
            //deposit
            //refill pouches if needed
            //close bank
            isIdle = true;
            delay(500);
        } else if(isNearWorldTile(RCPlusWorldPoints.FROM_BANK_TO_PATH, 2)  && isIdle) {
            setCameraZoom(750);
            delay(200);
            panCameraToAirAltar();
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusWorldPoints.CENTER_AIR_MYSTERIOUS_RUINS));
            delay(1000);
            client.setOculusOrbState(0);
        } else if(isNearWorldTile(RCPlusWorldPoints.CENTER_AIR_MYSTERIOUS_RUINS, 2) && readyForAltar() && isIdle) {
            //doesnt work, doesnt detect in range
            setCameraZoom(800);
            delay(200);
            scheduledGameObjectDelay(RCPlusObjectIDs.airMysteriousRuins, 8);
            delay(1000);
        } else if(isNearWorldTile(RCPlusWorldPoints.INSIDE_AIR_ALTAR, 2) && readyForAltar() && isIdle) {
            setCameraZoom(493);
            delay(500);
            bindRunesSequence(false, false, false, false);
            // click exit portal
            scheduledGameObjectDelay(RCPlusObjectIDs.airAltarPortal, 8);
            delay(3000);
        } else if(isAtWorldPoint(RCPlusWorldPoints.EXIT_AIR_ALTAR_WORLDPOINT) && !readyForAltar() && isIdle) {
            // pan camera to FROM_AIR_ALTAR_TO_PATH
            setCameraZoom(750);
            panCameraToBankPath();
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusWorldPoints.FROM_AIR_ALTAR_TO_PATH));
            delay(1000);
            client.setOculusOrbState(0);
        } else if(isNearWorldTile(RCPlusWorldPoints.FROM_AIR_ALTAR_TO_PATH, 2) && !readyForAltar() && isIdle) {
            setCameraZoom(750);
            panCameraToBank();
            delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusWorldPoints.INFRONT_OF_BANK_BOOTH_TILE3));
            delay(1000);
            client.setOculusOrbState(0);
        }
    }

    private List<Item> getInventoryItems() {
        ItemContainer inventoryContainer = client.getItemContainer(InventoryID.INVENTORY);
        return new ArrayList<>(Arrays.asList(inventoryContainer.getItems()));
    }

    private boolean readyForAltar() {
        try {
            return getInventoryItems().stream()
                    .filter(item -> item.getId() == ItemID.RUNE_ESSENCE || item.getId() == ItemID.PURE_ESSENCE)
                    .count() >= 20;
        } catch (Exception e) {
            return false;
        }
    }

    private void bankingSequence(boolean checkHP, boolean checkTps, boolean hasPouches) {
//        List<Item> inventoryItems = getInventoryItems();
        if(checkHP) {
            // eat food
            System.out.println("eating food");
        }

        if(checkTps) {
            System.out.println("replenishTps");
        }

        if(hasPouches) {
            System.out.println("filling pouches");
            // count pouches
            // withdraw ess and refill per pouch

        }
        // cheese by always having slot 1 empty so the bound runes go there
        // deposit non pouches (click slot 1)
        scheduledPointClick(new Point(782, 768), 3);
        delay(500);
        // withdraw ess
        scheduledPointClick(new Point(524, 757), 3);
        delay(500);
        pressKey(KeyEvent.VK_ESCAPE);
    }

    private void bindRunesSequence(boolean smallPouch, boolean mediumPouch, boolean largePouch, boolean giantPouch) {
        scheduledGameObjectDelay(RCPlusObjectIDs.airAltarAltar, 6);
        delay(2000);

        if(smallPouch) {
            System.out.println("binding small pouch");
        }

        if(mediumPouch) {
            System.out.println("binding medium pouch");
        }

        if(largePouch) {
            System.out.println("binding large pouch");
        }

        if(giantPouch) {
            System.out.println("binding giant pouch");
        }

        delay(4000);
    }

//    private Point getRunesInInventoryLocation(List<Item> inventory) {
//        List<Item> foodSet = inventory.stream().filter(
//                (item) -> item.getId() == ItemID.CAKE ||
//                        item.getId() == ItemID._23_CAKE ||
//                        item.getId() == ItemID.SLICE_OF_CAKE
//        ).collect(Collectors.toList());
//
//        int indexOfFood = Integer.MAX_VALUE;
//
//        for(Item food : foodSet) {
//            int idx = inventory.indexOf(food);
//            if(idx < indexOfFood)
//                indexOfFood = idx;
//        }
//
//        Point rowByCol = new Point((indexOfFood /4), (indexOfFood % 4));
//
//        Point foodPoint = new Point((798 + (rowByCol.y * 42)), (764 + (rowByCol.x * 36)));
//        System.out.println("location of food: " + foodPoint.x + ", " + foodPoint.y);
//
//        return foodPoint;
//    }

//    private void eatFood() {
//        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
//        if(inventoryWidget != null) {
//            ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
//            if(inventory.contains(ItemID.SALMON) || inventory.contains(ItemID.CAKE) || inventory.contains(ItemID._23_CAKE) || inventory.contains(ItemID.SLICE_OF_CAKE)) {
//                List<Item> inventoryList = new ArrayList<>(Arrays.asList(inventory.getItems()));
//                inventoryList.stream().findFirst().get(
//            }
//        } else {
//            pressKey(KeyEvent.VK_ESCAPE);
//        }
//    }
    
    private void panCameraToAirAltarPath() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_S, 2800);
    }

    private void panCameraToAirAltar() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_S, 1300);
        pressKey(KeyEvent.VK_A, 1500);
    }

    private void panCameraToBankPath() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_D, 1600);
        pressKey(KeyEvent.VK_W, 3300);
    }

    private void panCameraToBank() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_D, 500);
        pressKey(KeyEvent.VK_W, 900);
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private Point generatePointsFromPoint(Point point, int sigma) {
        List<Point> points = new ArrayList<>();

        while(points.size() < 3) {
            Point newPoint = MouseCoordCalculation.randomCoord(point, sigma);
            points.add(MouseCoordCalculation.randomCoord(newPoint, sigma));
        }

        return MouseCoordCalculation.randomClusterPicker(points);
    }

    private void getObstacleCenter(GameObject gameObject, int sigma) {
        Shape groundObjectConvexHull = gameObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, gameObject, sigma);
    }

    private void getObstacleCenter(GroundObject groundObject, int sigma) {
        Shape groundObjectConvexHull = groundObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, groundObject, sigma);
    }

    private void getObstacleCenter(DecorativeObject decorativeObject, int sigma) {
        Shape groundObjectConvexHull = decorativeObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, decorativeObject, sigma);
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
        } catch (Exception e) { e.getStackTrace(); }
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

    private void scheduledGroundObjectDelay(GroundObject groundObject, int sigma) {
        isIdle = false;
        try {
            getObstacleCenter(groundObject, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
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

    private void scheduledDecorativeObjectDelay(DecorativeObject decorativeObject, int sigma) {
        isIdle = false;
        try {
            getObstacleCenter(decorativeObject, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    private void scheduledGameObjectPointDelay(Point point, GameObject gameObject, int sigma) {
        isIdle = false;
        try {
            Point generatedPoint = generatePointsFromPoint(point, sigma);
            MouseCoordCalculation.generateCoord(generatedPoint, gameObject, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    //possible use would be a ground item that has no clickbox (doubt that will happen)
    private void scheduledGroundObjectPointDelay(Point point, GroundObject groundObject, int sigma) {
        isIdle = false;
        try {
            MouseCoordCalculation.generateCoord(point, groundObject, sigma);
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

    private void scheduledPointClick(Point canvasPoint, int sigma) {
        Point actualPoint = new Point(canvasPoint.x, canvasPoint.y);
        try {
            MouseCoordCalculation.generateCoord(actualPoint, sigma);
        } catch (Exception e) {
            e.printStackTrace();
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

        return sec >= 20;
    }
}
