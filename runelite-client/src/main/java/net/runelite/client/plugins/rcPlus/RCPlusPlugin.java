package net.runelite.client.plugins.rcPlus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.agilityPlus.MouseCoordCalculation;

import javax.inject.Inject;
import java.awt.AWTException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(name = "RC Plus", enabledByDefault = false)
@Slf4j
public class RCPlusPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    RCPlusMain thread;

    @Subscribe
    public void onGameTick(GameTick event) throws AWTException {
        toggleStatus();
        checkOculusReset();
    }

    private String stripTargetAnchors(String text) {
        Matcher m = Pattern.compile(">(.*?)<").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private void toggleStatus() throws AWTException {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if(chatBoxMessage.equals("1") && !RCPlusMain.isRunning) {
            thread = new RCPlusMain(client, clientThread);
            RCPlusMain.isRunning = true;
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && RCPlusMain.isRunning) {
            thread.t.interrupt();
            RCPlusMain.isRunning = false;
            System.out.println("status is stop");
        }
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private void checkOculusReset() {
        if(RCPlusMain.resetOculusOrb){
            client.setOculusOrbState(0);
            RCPlusMain.resetOculusOrb = false;
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
    private void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN && RCPlusMain.isRunning)
        {
            RCPlusMain.isRunning = false;
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        RCPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        RCPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY)) {
            RCPlusMain.inventoryItems = Arrays.asList(event.getItemContainer().getItems());
        }
    }
}
