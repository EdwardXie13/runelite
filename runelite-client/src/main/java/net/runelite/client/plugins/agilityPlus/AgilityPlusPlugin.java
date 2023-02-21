package net.runelite.client.plugins.agilityPlus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(name = "Agility Plus", enabledByDefault = false)
@Slf4j
public class AgilityPlusPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    private STATUS toggleStatus = STATUS.STOP;

    private boolean hasStarted = false;

    AgilityPlusMain thread;

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
            thread = new AgilityPlusMain(client, clientThread);
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
        if(AgilityPlusMain.resetOculusOrb){
            client.setOculusOrbState(0);
            AgilityPlusMain.resetOculusOrb = false;
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged ev)
    {
        if (ev.getGameState() == GameState.LOGIN_SCREEN && hasStarted)
        {
            toggleStatus = STATUS.STOP;
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
    @Subscribe
    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent event)
    {
        int[] intStack = client.getIntStack();
        int intStackSize = client.getIntStackSize();

        // for Gnome agility 1st floor weird hitbox
        if ("lookPreservePitch".equals(event.getEventName()) && isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB1))
            intStack[intStackSize - 1] = 230;
    }

    private boolean isAtWorldPoint(WorldPoint worldPoint) {
        boolean playerX = client.getLocalPlayer().getWorldLocation().getX() == worldPoint.getX();
        boolean playerY = client.getLocalPlayer().getWorldLocation().getY() == worldPoint.getY();
        boolean playerPlane = client.getLocalPlayer().getWorldLocation().getPlane() == worldPoint.getPlane();
        return playerX && playerY && playerPlane;
    }
}
