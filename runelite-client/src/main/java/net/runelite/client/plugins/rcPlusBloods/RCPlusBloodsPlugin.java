package net.runelite.client.plugins.rcPlusBloods;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.agilityPlus.MouseCoordCalculation;

import javax.inject.Inject;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(name = "RC Plus Bloods", enabledByDefault = false)
@Slf4j
public class RCPlusBloodsPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    static boolean denseRunestoneSouthMineable;

    static boolean denseRunestoneNorthMineable;

    private STATUS toggleStatus = STATUS.STOP;

    public static boolean hasStarted = false;

    RCPlusBloodsMain thread;

    enum STATUS{
        START,
        STOP
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        toggleStatus();
        checkOculusReset();
    }

    private String stripTargetAnchors(String text) {
        Matcher m = Pattern.compile(">(.*?)<").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private void toggleStatus() {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if(chatBoxMessage.equals("1") && toggleStatus == STATUS.STOP && !hasStarted) {
            toggleStatus = STATUS.START;
            hasStarted = true;
            thread = new RCPlusBloodsMain(client, clientThread);
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && toggleStatus == STATUS.START && hasStarted) {
            toggleStatus = STATUS.STOP;
            thread.t.interrupt();
            hasStarted = false;
            System.out.println("status is stop");
        }
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private void checkOculusReset() {
        if(RCPlusBloodsMain.resetOculusOrb){
            client.setOculusOrbState(0);
            RCPlusBloodsMain.resetOculusOrb = false;
        }
    }

    private boolean isNearWorldTile(final WorldPoint target, final int range) {
        return this.client.getLocalPlayer().getWorldLocation().distanceTo2D(target) < range;
    }

    private boolean isAtWorldPoint(WorldPoint worldPoint) {
        boolean playerX = client.getLocalPlayer().getWorldLocation().getX() == worldPoint.getX();
        boolean playerY = client.getLocalPlayer().getWorldLocation().getY() == worldPoint.getY();
        return playerX && playerY;
    }
    private void scheduledGameObjectDelay(GameObject gameObject, int sigma) {
        RCPlusBloodsMain.isIdle = false;
        try {
            getObstacleCenter(gameObject, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            RCPlusBloodsMain.isIdle = true;
        }
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


    @Subscribe
    private void onGameStateChanged(GameStateChanged ev)
    {
        if (ev.getGameState() == GameState.LOGIN_SCREEN && hasStarted)
        {
            toggleStatus = STATUS.STOP;
            thread.t.interrupt();
            hasStarted = false;
            System.out.println("status is stop (login screen)");
        }
    }

//    @Subscribe
//    public void onGameObjectSpawned(GameObjectSpawned event)
//    {
//        RCPlusBloodsObjectIDs.assignObjects(event);
//    }
//
//    @Subscribe
//    public void onGameObjectDespawned(GameObjectDespawned event)
//    {
//        RCPlusBloodsObjectIDs.assignObjects(event);
//    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        updateDenseRunestoneState();
    }

    private void updateDenseRunestoneState()
    {
        denseRunestoneSouthMineable = client.getVarbitValue(Varbits.DENSE_RUNESTONE_SOUTH_DEPLETED) == 0;
        denseRunestoneNorthMineable = client.getVarbitValue(Varbits.DENSE_RUNESTONE_NORTH_DEPLETED) == 0;
    }
}
