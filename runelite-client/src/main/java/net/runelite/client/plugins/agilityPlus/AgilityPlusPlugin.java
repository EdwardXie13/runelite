package net.runelite.client.plugins.agilityPlus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
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
import java.awt.AWTException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(name = "Agility Plus", enabledByDefault = false)
@Slf4j
public class AgilityPlusPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    AgilityPlusMain thread;

    private boolean hasStarted = false;

    @Subscribe
    public void onGameTick(GameTick event) throws AWTException {
        toggleStatus();
        checkOculusReset();
    }

    private String stripTargetAnchors(String text) {
        Matcher m = Pattern.compile("ff>(.*?)</c").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private void toggleStatus() throws AWTException {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if(chatBoxMessage.equals("1") && !AgilityPlusMain.isRunning && !hasStarted) {
            thread = new AgilityPlusMain(client, clientThread);
            AgilityPlusMain.isRunning = true;
            hasStarted = true;
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && AgilityPlusMain.isRunning && hasStarted) {
            thread.t.interrupt();
            AgilityPlusMain.isRunning = false;
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
    private void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN && AgilityPlusMain.isRunning && hasStarted)
        {
            AgilityPlusMain.isRunning = false;
            hasStarted = false;
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
    public void onItemSpawned(ItemSpawned itemSpawned)
    {
        final TileItem item = itemSpawned.getItem();
        final Tile tile = itemSpawned.getTile();

        if (item.getId() == ItemID.MARK_OF_GRACE)
        {
            WorldPoint MOG_TILE = tile.getWorldLocation();
            if(MOG_TILE.equals(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1))
                AgilityPlusWorldPoints.MOG_CANFIS1 = true;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2))
                AgilityPlusWorldPoints.MOG_CANFIS2 = true;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3))
                AgilityPlusWorldPoints.MOG_CANFIS3 = true;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4))
                AgilityPlusWorldPoints.MOG_CANFIS4 = true;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5))
                AgilityPlusWorldPoints.MOG_CANFIS5 = true;

            else if(MOG_TILE.equals(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1))
                AgilityPlusWorldPoints.MOG_SEERS1 = true;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1))
                AgilityPlusWorldPoints.MOG_SEERS2_1 = true;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2))
                AgilityPlusWorldPoints.MOG_SEERS2_2 = true;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3))
                AgilityPlusWorldPoints.MOG_SEERS3 = true;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5))
                AgilityPlusWorldPoints.MOG_SEERS5 = true;

            else if(MOG_TILE.equals(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1))
                AgilityPlusWorldPoints.MOG_RELLEKA1 = true;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_1))
                AgilityPlusWorldPoints.MOG_RELLEKA3_1 = true;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_2))
                AgilityPlusWorldPoints.MOG_RELLEKA3_2 = true;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_1))
                AgilityPlusWorldPoints.MOG_RELLEKA4_1 = true;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_2))
                AgilityPlusWorldPoints.MOG_RELLEKA4_2 = true;

            else if(MOG_TILE.equals(AgilityPlusWorldPoints.ARDY_GRACEFULMARK))
                AgilityPlusWorldPoints.MOG_ARDY = true;
        }
    }

    @Subscribe
    public void onItemDespawned(ItemDespawned itemDespawned)
    {
        final TileItem item = itemDespawned.getItem();
        final Tile tile = itemDespawned.getTile();

        if (item.getId() == ItemID.MARK_OF_GRACE)
        {
            WorldPoint MOG_TILE = tile.getWorldLocation();
            if(MOG_TILE.equals(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1))
                AgilityPlusWorldPoints.MOG_CANFIS1 = false;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2))
                AgilityPlusWorldPoints.MOG_CANFIS2 = false;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3))
                AgilityPlusWorldPoints.MOG_CANFIS3 = false;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4))
                AgilityPlusWorldPoints.MOG_CANFIS4 = false;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5))
                AgilityPlusWorldPoints.MOG_CANFIS5 = false;

            else if(MOG_TILE.equals(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1))
                AgilityPlusWorldPoints.MOG_SEERS1 = false;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1))
                AgilityPlusWorldPoints.MOG_SEERS2_1 = false;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2))
                AgilityPlusWorldPoints.MOG_SEERS2_2 = false;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3))
                AgilityPlusWorldPoints.MOG_SEERS3 = false;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5))
                AgilityPlusWorldPoints.MOG_SEERS5 = false;

            else if(MOG_TILE.equals(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK1))
                AgilityPlusWorldPoints.MOG_RELLEKA1 = false;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_1))
                AgilityPlusWorldPoints.MOG_RELLEKA3_1 = false;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK3_2))
                AgilityPlusWorldPoints.MOG_RELLEKA3_2 = false;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_1))
                AgilityPlusWorldPoints.MOG_RELLEKA4_1 = false;
            else if(MOG_TILE.equals(AgilityPlusWorldPoints.RELLEKA_GRACEFULMARK4_2))
                AgilityPlusWorldPoints.MOG_RELLEKA4_2 = false;

            else if(MOG_TILE.equals(AgilityPlusWorldPoints.ARDY_GRACEFULMARK))
                AgilityPlusWorldPoints.MOG_ARDY = false;
        }
    }
}
