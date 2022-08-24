package net.runelite.client.plugins.agilityPlus;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.ItemID;
import net.runelite.api.Perspective;
import net.runelite.api.Scene;
import net.runelite.api.ScriptID;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

;

@PluginDescriptor(
        name = "Agility Plus",
        description = "Show overlays for agility",
        tags = {"agility"}
)
public class AgilityPlusPlugin extends Plugin {
    public static boolean scheduledMove = false;
    public static boolean isIdle = true;

    ScheduledThreadPoolExecutor service = null;

    public final int MARK_OF_GRACE = ItemID.MARK_OF_GRACE;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    private STATUS toggleStatus = STATUS.STOP;

    @Subscribe
    public void onGameTick(GameTick event) {
        toggleStatus();

        if(toggleStatus == STATUS.START) {
            if(getRegionID() == 9781)
                doGnomeAgility();
            else if(getRegionID() == 13878)
                doCanfisAgility();
        }
    }

    private void doGnomeAgility() {
        setCameraZoom(400);

        if(isNearWorldTile(new WorldPoint(2474, 3436, 0), 4)) {
            changeCameraYaw(0);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeLogBalance, 10, 1);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_LOG) && isIdle) {
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstacleNet1_M, 15, 1);
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB1) && isIdle) {
            changeCameraYaw(315);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeTreeBranch1, 10, 1);
        }
        else if ((isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB2) || isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB2_MISCLICK_ZONE))  && isIdle) {
            changeCameraYaw(0);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeBalancingRope, 10, 1);
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_TIGHTROPE) && isIdle) {
            changeCameraYaw(225);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeTreeBranch2, 10, 1);
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_DROP) && isIdle)
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstacleNet2_L, 15, 1);
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_NET1) && isIdle) {
            changeCameraYaw(1536);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstaclePipeLeft, 10, 1);
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_NET2) && isIdle) {
            changeCameraYaw(1536);
            scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstaclePipeLeft, 10, 1);
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_LEFT_PIPE) && isIdle && finishedLap()) {
            // rotate camera west to see log balance
            changeCameraYaw(512);
            scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeLogBalance, 10, 2);
        }
    }

    private void doCanfisAgility() {
        if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_START) && isIdle) {
            setCameraZoom(729);
            changeCameraYaw(1438);
            service.schedule(() -> scheduledGameObjectPointDelay(new Point(47, 969), AgilityPlusObjectIDs.canfisTallTree, 8, 1), 500, TimeUnit.MILLISECONDS);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FAIL1) && isIdle) {
            setCameraZoom(-47);
            changeCameraYaw(1788);
            service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisTallTree, 8, 2), 2, TimeUnit.SECONDS);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FAIL2) && isIdle) {
            setCameraZoom(296);
            changeCameraYaw(0);
            // custom center 559, 49 (base of the tree)
            service.schedule(() -> scheduledGameObjectPointDelay(new Point(559, 49), AgilityPlusObjectIDs.canfisTallTree, 8, 2), 2, TimeUnit.SECONDS);
        }
        // 1 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIRST_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1) && isIdle) {
            setCameraZoom(758);
            changeCameraYaw(0);
            service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1), 500, TimeUnit.MILLISECONDS);
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIRST_ROOF) || isNearWorldTile(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1, 2)) && isIdle) {
            setCameraZoom(336);
            changeCameraYaw(0);
            service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFirstRoofGap, 10, 2), 500, TimeUnit.MILLISECONDS);
        }
        // 2 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SECOND_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2) && isIdle) {
            setCameraZoom(896);
            service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2), 500, TimeUnit.MILLISECONDS);
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SECOND_ROOF) || isNearWorldTile(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2, 2)) && isIdle) {
            setCameraZoom(532);
            service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSecondRoofGap, 10, 1), 500, TimeUnit.MILLISECONDS);
        }
        // 3 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_THIRD_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3) && isIdle) {
            setCameraZoom(751);
            service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3), 500, TimeUnit.MILLISECONDS);
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_THIRD_ROOF) || isNearWorldTile(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3, 2)) && isIdle) {
            setCameraZoom(473);
            service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisThirdRoofGap, 10, 1), 500, TimeUnit.MILLISECONDS);
        }
        // 4 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FOURTH_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4) && isIdle) {
            setCameraZoom(512);
            service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4), 500, TimeUnit.MILLISECONDS);
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FOURTH_ROOF) || isNearWorldTile(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4, 2)) && isIdle) {
            setCameraZoom(404);
            service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFourthRoofGap, 10, 1), 500, TimeUnit.MILLISECONDS);
        }
        // 5 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIFTH_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5) && isIdle) {
            setCameraZoom(876);
            changeCameraYaw(512);
            service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5), 500, TimeUnit.MILLISECONDS);
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIFTH_ROOF) || isNearWorldTile(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5, 2)) && isIdle) {
            setCameraZoom(719);
            changeCameraYaw(512);
            service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFifthRoofGap, 10, 1), 500, TimeUnit.MILLISECONDS);
        }
        // 6 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SIXTH_ROOF) && isIdle) {
            setCameraZoom(250);
            changeCameraYaw(1300);
            service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSixthRoofGap, 10, 1), 500, TimeUnit.MILLISECONDS);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SEVENTH_ROOF) && isIdle) {
            setCameraZoom(453);
            changeCameraYaw(0);
            service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSeventhRoofGap, 10, 1), 500, TimeUnit.MILLISECONDS);
        }
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

    private Point getCenterOfRectangle(Rectangle rectangle) {
        // +26 to the Y coordinate because calculations are taken from canvas, not window
        return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY() + 26);
    }

    private void toggleStatus() {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if(chatBoxMessage.equals("1") && toggleStatus == STATUS.STOP) {
            toggleStatus = STATUS.START;
            service = new ScheduledThreadPoolExecutor(1);
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && toggleStatus == STATUS.START) {
            toggleStatus = STATUS.STOP;
            service.shutdownNow();
            service = null;
            scheduledMove = false;
            isIdle = true;
            System.out.println("status is stop");
        }
    }

    private String stripTargetAnchors(String text) {
        Matcher m = Pattern.compile(">(.*?)<").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private boolean isAtWorldPoint(WorldPoint worldPoint) {
        boolean playerX = client.getLocalPlayer().getWorldLocation().getX() == worldPoint.getX();
        boolean playerY = client.getLocalPlayer().getWorldLocation().getY() == worldPoint.getY();
        return playerX && playerY;
    }

    private void changeCameraYaw(int yaw) {
        client.setCameraYawTarget(yaw);
//        north: 0
//        east:1536
//        south:1024
//        west:512
    }

    private void setCameraZoom(int zoom) {
        clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom));
    }

    private int getRegionID() {
        return this.client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private boolean finishedLap() {
        // lowest line in chatbox window 10616888
        Widget chatboxMessage = client.getWidget(10616888);
        if(chatboxMessage != null) {
            Widget[] chatboxMessageChildren = chatboxMessage.getChildren();
            if(chatboxMessageChildren != null) {
                String text = chatboxMessageChildren[0].getText();
                if (text != null) {
                    return text.contains("Agility lap count is") || text.contains("Congratulations, you've just advanced your Agility level");
                }
            }
        }
        return false;
    }

    private void scheduledGroundObjectDelay(GroundObject groundObject, int sigma, int seconds) {
        isIdle = false;
        service.schedule(() -> getObstacleCenter(groundObject, sigma), seconds, TimeUnit.SECONDS);
    }

    private void scheduledGameObjectDelay(GameObject gameObject, int sigma, int seconds) {
        isIdle = false;
        service.schedule(() -> getObstacleCenter(gameObject, sigma), seconds, TimeUnit.SECONDS);
    }

    private void scheduledGameObjectPointDelay(Point point, GameObject gameObject, int sigma, int seconds) {
        isIdle = false;
        Point generatedPoint = generatePointsFromPoint(point, sigma);
        service.schedule(() -> MouseCoordCalculation.generateCoord(generatedPoint, gameObject, sigma), seconds, TimeUnit.SECONDS);
    }

    //possible use would be a ground item that has no clickbox (doubt that will happen)
    private void scheduledGroundObjectPointDelay(Point point, GroundObject groundObject, int sigma, int seconds) {
        isIdle = false;
        service.schedule(() -> MouseCoordCalculation.generateCoord(point, groundObject, sigma), seconds, TimeUnit.SECONDS);
    }

    private void scheduledPointDelay(Point point, int sigma, int seconds) {
        isIdle = false;
        service.schedule(() -> MouseCoordCalculation.generateCoord(point, sigma), seconds, TimeUnit.SECONDS);
    }

    private void getWorldPointCoords(final LocalPoint dest) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, dest, 1);
        if (poly == null)
        {
            return;
        }

        Rectangle boundingBox = poly.getBounds();

        Point obstacleCenter = getCenterOfRectangle(boundingBox);

        scheduledPointDelay(obstacleCenter, 10, 1);
    }

    private boolean isNearWorldTile(final WorldPoint target, final int range) {
        return this.client.getLocalPlayer().getWorldLocation().distanceTo2D(target) < range;
    }

    private void checkGracefulmark(WorldPoint worldpoint) {
        if(doesWorldPointHaveGracefulMark(worldpoint))
            getWorldPointCoords(LocalPoint.fromWorld(client, worldpoint));
    }

    private boolean doesWorldPointHaveGracefulMark(WorldPoint worldpoint) {
        Tile[][][] sceneTiles = client.getScene().getTiles();

        int startX = sceneTiles[0][0][0].getWorldLocation().getX();
        int startY = sceneTiles[0][0][0].getWorldLocation().getY();

        int playerPlane = client.getLocalPlayer().getWorldLocation().getPlane();
        List<TileItem> itemsOnTile = sceneTiles[playerPlane][worldpoint.getX()-startX][worldpoint.getY()-startY].getGroundItems();
        List<Integer> tileItemIds = new ArrayList<>();
        if(itemsOnTile != null)
            itemsOnTile.forEach(tileItem -> tileItemIds.add(tileItem.getId()));

        return tileItemIds.contains(MARK_OF_GRACE);
    }

    enum STATUS{
        START,
        STOP
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged ev)
    {
        if (ev.getGameState() == GameState.LOGIN_SCREEN)
        {
            toggleStatus = STATUS.STOP;
            service.shutdown();
            service = null;
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onGroundObjectDespawned(GroundObjectDespawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }

//    @Subscribe
//    public void onWallObjectSpawned(WallObjectSpawned event)
//    {
//        onTileObject(event.getTile(), null, event.getWallObject());
//    }
//
//    @Subscribe
//    public void onWallObjectDespawned(WallObjectDespawned event)
//    {
//        onTileObject(event.getTile(), event.getWallObject(), null);
//    }
//
//    @Subscribe
//    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
//    {
//        onTileObject(event.getTile(), null, event.getDecorativeObject());
//    }
//
//    @Subscribe
//    public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
//    {
//        onTileObject(event.getTile(), event.getDecorativeObject(), null);
//    }
}
