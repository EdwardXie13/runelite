package net.runelite.client.plugins.rcPlusTrueBloods;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Item;
import net.runelite.api.Perspective;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.agilityPlusV2.MouseCoordCalculation;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RCPlusTrueBloodsMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    private WorldService worldService;
    public static boolean isIdle = true;
    public static boolean resetOculusOrb = false;
    public long start;
    public long botStartTimer;
    public int botStopTimer;
    public static boolean isRunning = false;
    public static int pouchUses = 7;
    Robot robot = new Robot();

    public static Set<Integer> trueBloodAltarRegions = ImmutableSet.of(
            13721, // Fairy ring cave
            13977,
            14232,
            12875
    );

    public static List<Item> inventoryItems = new ArrayList<>();

    Thread t;

    RCPlusTrueBloodsMain(Client client, ClientThread clientThread, WorldService worldService) throws AWTException {
        this.client = client;
        this.clientThread = clientThread;
        this.worldService = worldService;

        t = new Thread(this);
        System.out.println("New thread: " + t);
        start = System.currentTimeMillis();
        botStartTimer = System.currentTimeMillis();
        botStopTimer = generateStopTimer();
        System.out.println("botStopTimer: " + botStopTimer);
        t.start(); // Starting the thread
    }

    // execution of thread starts from run() method
    public void run()
    {
        while (isRunning) {
            if (checkIdle() && checkLastReset())
                reset();

            // 12 - 13.89hr run time
            if(checkBotTimer()) {
                System.out.println("stop the count");
                client.stopNow();
            }

            if (trueBloodAltarRegions.contains(getRegionID()))
                doBloodRunes();
            else if(getInstanceRegionID() == 7513)
                doHouse();
            else if(getRegionID() == 11571)
                doBanking();
        }
        System.out.println("Thread has stopped.");
    }

    private void doBloodRunes() {
        if (checkLevelUp()) {
            pressKey(KeyEvent.VK_SPACE, 100);
            robot.delay(500);
            isIdle = true;
        }
        // BLOOD_ESSENCE = 26390;
        // BLOOD_ESSENCE_ACTIVE = 26392;

        else if(isAtWorldPoint(RCPlusTrueBloodsWorldPoints.fairyRingDLS) && isIdle) {
            System.out.println("fairy ring cave");
//            robot.delay(250);
            setCameraZoom(400);
            changeCameraPitch(77);
            changeCameraYaw(0);
            robot.delay(250);
            scheduledGameObjectDelay(RCPlusTrueBloodsObjectIDs.caveEntrance16308, 10);
            robot.delay(500);
        } else if(isAtWorldPoint(RCPlusTrueBloodsWorldPoints.caveEntrance1) && isIdle) {
            System.out.println("1st cave");
            robot.delay(500);
            setCameraZoom(800);
            changeCameraPitch(14);
            changeCameraYaw(1798);
            robot.delay(500);
            scheduledGameObjectDelay(RCPlusTrueBloodsObjectIDs.caveEntrance5046, 10);
            robot.delay(100);
            panCameraToCave3FromCave2();
            robot.delay(1000);
        }
        else if(isAtWorldPoint(RCPlusTrueBloodsWorldPoints.caveEntrance2) && isIdle) {
            System.out.println("2nd cave");
            robot.delay(250);
            try {
                robot.delay(250);
                getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusTrueBloodsWorldPoints.caveEntrance2To3));
            } catch (Exception e) {
                setCameraZoom(800);
                changeCameraPitch(512);
                changeCameraYaw(0);
                robot.delay(250);
                panCameraToCave3FromCave2();
                robot.delay(500);
                getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusTrueBloodsWorldPoints.caveEntrance2To3));
            } finally {
                robot.delay(1000);
                client.setOculusOrbState(0);
                client.setOculusOrbNormalSpeed(12);
            }
        }
        else if(isAtWorldPoint(RCPlusTrueBloodsWorldPoints.caveEntrance2To3) && isIdle) {
            System.out.println("2 -> 3");
            robot.delay(250);
            setCameraZoom(900);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(500);
            scheduledGameObjectDelay(RCPlusTrueBloodsObjectIDs.cave43759, 8);
            isIdle = true;
        }
        else if(isAtWorldPoint(RCPlusTrueBloodsWorldPoints.caveEntrance3) && isIdle) {
            System.out.println("3rd cave");
//            robot.delay(500);
            setCameraZoom(-47);
            changeCameraPitch(75);
            changeCameraYaw(760);
            robot.delay(500);
            scheduledPointDelay(new Point(491, 537), 8);
            robot.delay(1000);
        } else if(isAtWorldPoint(RCPlusTrueBloodsWorldPoints.caveEntrance4) && isIdle) {
            System.out.println("4th cave");
            setCameraZoom(190);
            changeCameraPitch(420);
            changeCameraYaw(1450);
            robot.delay(250);
            scheduledPointDelay(new Point(78, 92), 6);
            robot.delay(500);
        } else if(isAtWorldPoint(RCPlusTrueBloodsWorldPoints.bloodAltarEntrance) && isIdle) {
            System.out.println("blood entrance. bind 1st inventory");
            robot.delay(500);
            setCameraZoom(715);
            changeCameraPitch(0);
            changeCameraYaw(512);
            robot.delay(500);
            scheduledPointDelay(new Point(490, 560), 8);
            robot.delay(2500);

            System.out.println("blood altar. bind 2nd 3rd inventory");
            // click slot 1
            scheduledPointDelay(new Point(782, 768), 4);
            robot.delay(500);
            // click altar
            scheduledPointDelay(new Point(490, 560), 8);
            robot.delay(500);
            // click slot 1
            scheduledPointDelay(new Point(782, 768), 4);
            robot.delay(500);
            // click altar
            scheduledPointDelay(new Point(490, 560), 8);
            robot.delay(500);
            // click slot 5 (tp)
            pouchUses++;
            System.out.println("trips = " + pouchUses);
            scheduledPointDelay(new Point(782, 804), 4);
            robot.delay(500);
        }
    }

    private void doHouse() {
        if(!hasEnoughStamina(40) && isAtLocalPoint(7616, 4800) && isIdle) {
            System.out.println("rejuv pool");
            robot.delay(500);
            setCameraZoom(610);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(250);
            scheduledPointDelay(new Point(55, 390), 6);
            robot.delay(500);
        } else if(hasEnoughStamina(40) && isAtLocalPoint(7616, 4800) && isIdle) {
            System.out.println("straight to fairy ring");
            robot.delay(500);
            setCameraZoom(420);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(250);
            scheduledPointDelay(new Point(60, 615), 8);
            robot.delay(2500);
        } else if(isAtLocalPoint(7104, 4800) && isIdle) {
            System.out.println("pool to fairy ring");
            robot.delay(1500);
            setCameraZoom(660);
            changeCameraPitch(512);
            changeCameraYaw(0);
            robot.delay(250);
            scheduledPointDelay(new Point(85, 690), 8);
            robot.delay(500);
        }
    }

    private void doBanking() {
        if(isAtWorldPoint(RCPlusTrueBloodsWorldPoints.craftingGuild) && isIdle) {
            System.out.println("click bank tile");
            robot.delay(250);
            setCameraZoom(577);
            changeCameraPitch(512);
            changeCameraYaw(1024);
            robot.delay(500);
            getWorldPointCoords(LocalPoint.fromWorld(client, RCPlusTrueBloodsWorldPoints.craftingGuildBank));
            robot.delay(500);
        } else if(isNPCContactPanelOpen() && isIdle) {
            System.out.println("contact dark mage");
            robot.delay(250);
            scheduledPointDelay(new Point(348, 386), 6);
            robot.delay(5500);
            isIdle = true;
        } else if(isTalkingToDarkMage() && isIdle) {
            System.out.println("talking to mage");
            boolean talkingDarkMage = true;
            while(talkingDarkMage) {
                if(isTalkingToDarkMage()) {
                    if(getDialogNPC().contains("resolve"))
                        talkingDarkMage = false;
                    pressKey(KeyEvent.VK_SPACE, 100);
                    robot.delay(300);
                } else if(canSelectOption()) {
                    if(canRepairPouch()) {
                        System.out.println("repair pouch");
                        pressKey(KeyEvent.VK_2, 100);
                        robot.delay(250);
                        pressKey(KeyEvent.VK_SPACE, 100);
                        robot.delay(250);
                        talkingDarkMage = false;
                        pouchUses = 0;
                        isIdle = true;
                    } else if(newEssencePouch()) {
                        System.out.println("cannot repair");
                        talkingDarkMage = false;
                        pouchUses = 0;
                        isIdle = true;
                    }
                }
            }
        }
//        else if(isAtWorldPoint(RCPlusTrueBloodsWorldPoints.craftingGuildBank) && pouchUses >= 7 && isIdle) {
//            System.out.println("press contact NPC");
//            pressKey(KeyEvent.VK_F3, 100);
//            robot.delay(500);
//            // press npc contact
//            scheduledPointDelay(new Point(843,864), 4);
//            robot.delay(500);
//            isIdle = true;
//        }
        // banking sequence
        // if inv contains blood runes, then we need to bank
        else if(isAtWorldPoint(RCPlusTrueBloodsWorldPoints.craftingGuildBank) && inventoryContains(565) && isIdle) {
            isIdle = false;
            robot.delay(500);
            setCameraZoom(1004);
            changeCameraPitch(512);
            changeCameraYaw(1024);
            robot.delay(500);
            scheduledPointDelay(new Point(488, 257), 6);

            while(client.getWidget(786433) == null)
                robot.delay(250);

            bankSequence();

            isIdle = true;
        }
        // if inv contains pure ess, we can tp
        else if(isAtWorldPoint(RCPlusTrueBloodsWorldPoints.craftingGuildBank) && inventoryContains(7936) && isIdle) {
            pressKey(KeyEvent.VK_ESCAPE, 100);
            robot.delay(250);
            scheduledPointDelay(new Point(905, 983), 4);
            robot.delay(5000);
            isIdle = true;
        }
    }

    private void bankSequence() {
        // click slot 2
        robot.delay(250);
        scheduledPointDelay(new Point(821, 768), 4);
        // withdraw ess
        robot.delay(500);
        scheduledPointDelay(new Point(521, 791), 4);
        // click slot 1
        robot.delay(500);
        scheduledPointDelay(new Point(779, 767), 4);
        // withdraw ess
        robot.delay(500);
        scheduledPointDelay(new Point(521, 791), 4);
        // click slot 1
        robot.delay(500);
        scheduledPointDelay(new Point(779, 767), 4);
        // withdraw ess
        robot.delay(500);
        scheduledPointDelay(new Point(521, 791), 4);
        // close window
        robot.delay(250);
        pressKey(KeyEvent.VK_ESCAPE, 100);
    }

    private boolean isAtLocalPoint(int x, int y) {
        LocalPoint localPoint = client.getLocalPlayer().getLocalLocation();
        return localPoint.getX() == x && localPoint.getY() == y;
    }

    private boolean turnRunOn() {
        Widget runWidget = client.getWidget(10485789);

        if(runWidget != null) {
            // 1065 is on
            // 1064 is off
            return runWidget.getSpriteId() == 1064;
        }

        return false;
    }

    private String getDialogNPC() {
        Widget dialogBox = client.getWidget(15138822);
        if(dialogBox != null) {
            return dialogBox.getText();
        }

        return "";
    }

    private boolean canSelectOption() {
        Widget dialogBox = client.getWidget(14352385);
        if(dialogBox != null) {
            Widget dialogBoxOption = dialogBox.getChild(0);
            if(dialogBoxOption != null) {
                return dialogBoxOption.getText().contains("Select");
            }
        }
        return false;
    }

    private boolean canRepairPouch() {
        Widget dialogBox = client.getWidget(14352385);
        if(dialogBox != null) {
            Widget dialogBoxOption = dialogBox.getChild(0);
            if(dialogBoxOption != null) {
                if(dialogBoxOption.getText().contains("Select")) {
                    return dialogBox.getChild(2).getText().contains("repair");
                }
            }
        }
        return false;
    }

    private boolean newEssencePouch() {
        Widget dialogBox = client.getWidget(14352385);
        if(dialogBox != null) {
            Widget dialogBoxOption = dialogBox.getChild(0);
            if(dialogBoxOption != null) {
                if(dialogBoxOption.getText().contains("Select")) {
                    return dialogBox.getChild(2).getText().contains("essence");
                }
            }
        }
        return false;
    }

    private boolean isTalkingToDarkMage() {
        Widget dialogTitle = client.getWidget(15138820);
        if(dialogTitle != null) {
            return dialogTitle.getText().contains("Dark");
        }

        return false;
    }

    private boolean isNPCContactPanelOpen() {
        Widget contactPanel = client.getWidget(4915201);
        if(contactPanel != null) {
            Widget contactPanelTitle = contactPanel.getChild(1);
            if(contactPanelTitle != null) {
                return contactPanelTitle.getText().contains("Choose a character");
            }
        }

        return false;
    }

    private boolean isLogoutPanelOpen() {
        Widget logoutPanel = client.getWidget(10551341);

        if(logoutPanel != null) {
            // 1030 means panel is open / selected
            return logoutPanel.getSpriteId() == 1030;
        }

        return false;
    }

    private boolean closeToLogout() {
        Widget timeWidget = client.getWidget(10616865);

        if(timeWidget != null) {
            String timeWidgetTime = timeWidget.getText();
            List<String> timeSplit = Arrays.asList(timeWidgetTime.split(":"));
            // at 5 hr mark
            return Integer.parseInt(timeSplit.get(0)) == 5 && Integer.parseInt(timeSplit.get(1)) == 50;
        }

        return false;
    }

//    private void panCameraToCave3FromCave2() {
//        client.setOculusOrbNormalSpeed(40);
//        client.setOculusOrbState(1);
//        pressKey(KeyEvent.VK_D, 1300);
//        pressKey(KeyEvent.VK_S, 1200);
//    }

    private void panCameraToCave3FromCave2() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        setCameraZoom(800);
        changeCameraPitch(512);
        changeCameraYaw(0);
        robot.delay(250);
        pressKey(KeyEvent.VK_D, 2700);
        pressKey(KeyEvent.VK_S, 400);
    }

    private boolean inventoryContains(int id) {
        return inventoryItems.stream().anyMatch(
                items -> items.getId() == id
        );
    }

    private boolean hasEnoughStamina(int stamina) {
        return client.getEnergy()/100 >= stamina;
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    public int getInstanceRegionID() {
        try {
            return WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation()).getRegionID();
        } catch (Exception e) {
            System.out.println("getRegion ID fail");
        }
        return 0;
    }

    private Point getCenterOfRectangle(Rectangle rectangle) {
        // +26 to the Y coordinate because calculations are taken from canvas, not window
        return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY() + 26);
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

    private Point generatePointsFromPoint(Point point, int sigma) {
        List<Point> points = new ArrayList<>();

        while(points.size() < 3) {
            Point newPoint = MouseCoordCalculation.randomCoord(point, sigma);
            points.add(MouseCoordCalculation.randomCoord(newPoint, sigma));
        }

        return MouseCoordCalculation.randomClusterPicker(points);
    }

    private void getObstacleCenter(GameObject gameObject, int sigma) {
        Shape gameObjectConvexHull = gameObject.getConvexHull();
        Rectangle gameObjectRectangle = gameObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(gameObjectRectangle);

        MouseCoordCalculation.generateCoord(client, obstacleCenter, gameObject, sigma);
    }

    private void getObstacleCenter(GroundObject groundObject, int sigma) {
        Shape groundObjectConvexHull = groundObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(client, obstacleCenter, groundObject, sigma);
    }

    private void getObstacleCenter(DecorativeObject decorativeObject, int sigma) {
        Shape groundObjectConvexHull = decorativeObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(client, obstacleCenter, decorativeObject, sigma);
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
//        north: 0
//        east:1536
//        south:1024
//        west:512
    }

    private void changeCameraPitch(int pitch) {
        if(client.getCameraPitch() == pitch)
            return;
        client.setCameraPitchTarget(pitch);
    }

    private void setCameraZoom(int zoom) {
        clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom));
    }

    private void panCameraOneDirection(int keyEvent, int ms) {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(keyEvent, ms);
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
            MouseCoordCalculation.generateCoord(client, generatedPoint, gameObject, sigma);
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
        return this.client.getLocalPlayer().getWorldLocation().distanceTo2D(target) < range
                && client.getLocalPlayer().getWorldLocation().getPlane() == target.getPlane();
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
        robot.delay(200);
        start = System.currentTimeMillis();
    }

    private boolean checkLastReset() {
        long end = System.currentTimeMillis();
        double sec = (end - start) / 1000F;

        return sec >= 90;
    }

    private boolean checkBotTimer() {
        long end = System.currentTimeMillis();
        double sec = (end - botStartTimer) / 1000F;

        return sec > botStopTimer;
    }

    private int generateStopTimer() {
        Random random = new Random();

        return random.nextInt(50000 - 43200 + 1) + 43200;
    }

    private void resetLoginTimerByWorldHopping() {
        System.out.println("timeToWorldHop");
        robot.delay(1000);

        worldHop();

        robot.delay(20000);
    }

    private void worldHop() {
        int currentWorld = client.getWorld();
        int newWorld = currentWorld + 1;

        WorldResult worldResult = worldService.getWorlds();

        World world = worldResult.findWorld(newWorld);
        System.out.println("the new world: " + world.toString());

        final net.runelite.api.World rsWorld = client.createWorld();
        rsWorld.setActivity(world.getActivity());
        rsWorld.setAddress(world.getAddress());
        rsWorld.setId(world.getId());
        rsWorld.setPlayerCount(world.getPlayers());
        rsWorld.setLocation(world.getLocation());
        rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

        robot.delay(500);
        client.hopToWorld(rsWorld);
    }
}
