package net.runelite.client.plugins.agilityPlus;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.MenuEntry;
import net.runelite.api.Model;
import net.runelite.api.ObjectID;
import net.runelite.api.Player;
import net.runelite.api.Renderable;
import net.runelite.api.ScriptID;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.World;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
                // before log (new WorldPoint(2474, 3436, 0))
                doGnomeAgility();
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

    private void populateGnomeAgilityGameObjectPoints_OR_GetPoint(GameObject gameObject, int sigma) {
        Shape groundObjectConvexHull = gameObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, gameObject, sigma);
    }

    private void populateGnomeAgilityGroundObjectPoints_OR_GetPoint(GroundObject groundObject, int sigma) {
        Shape groundObjectConvexHull = groundObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, groundObject, sigma);
    }

    private Boolean isNearWorldTile(final WorldPoint target, final int range) {
        return this.client.getLocalPlayer().getWorldLocation().distanceTo2D(target) < range;
    }

    private Point getCenterOfRectangle(Rectangle rectangle) {
        // +26 to the Y coordinate because calculations are taken from canvas, not window
        return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY() + 26);
    }

    private void toggleStatus() {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if(chatBoxMessage.equals("go") && toggleStatus == STATUS.STOP)
            toggleStatus = STATUS.START;
        else if (chatBoxMessage.equals("stop") && toggleStatus == STATUS.START)
            toggleStatus = STATUS.STOP;
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
        if(client.getMapAngle() != yaw) {
            client.setCameraYawTarget(yaw);
        }
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
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

        service.schedule(() -> populateGnomeAgilityGroundObjectPoints_OR_GetPoint(groundObject, sigma), seconds, TimeUnit.SECONDS);
    }

    private void scheduledGameObjectDelay(GameObject gameObject, int sigma, int seconds) {
        isIdle = false;
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

        service.schedule(() -> populateGnomeAgilityGameObjectPoints_OR_GetPoint(gameObject, sigma), seconds, TimeUnit.SECONDS);
    }

    enum GNOME_ENUM {
        START_AREA,
        LOG_BALANCE_23145,
        OBSTACLE_NET_23134,
        TREE_BRANCH_23559,
        BALANCING_ROPE_23557,
        TREE_BRANCH_23560,
        OBSTACLE_NET_23135,
        OBSTACLE_PIPE_23138,
        OBSTACLE_PIPE_23139
    }

    enum STATUS{
        START,
        STOP
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }
//
//    @Subscribe
//    public void onGameObjectDespawned(GameObjectDespawned event)
//    {
//        onTileObject(event.getTile(), event.getGameObject(), null);
//    }
//
    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }
//
//    @Subscribe
//    public void onGroundObjectDespawned(GroundObjectDespawned event)
//    {
//        onTileObject(event.getTile(), event.getGroundObject(), null);
//    }
//
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
