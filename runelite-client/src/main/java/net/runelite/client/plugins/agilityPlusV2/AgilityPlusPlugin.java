package net.runelite.client.plugins.agilityPlusV2;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.plusUtils.Clicker;
import net.runelite.client.plugins.plusUtils.StepOverlay;
import net.runelite.client.plugins.tithefarmplus.TitheFarmPlusMain;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.AWTException;
import java.awt.Point;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.api.Skill.AGILITY;

@PluginDescriptor(name = "Agility Plus2", enabledByDefault = false)
@Slf4j
public class AgilityPlusPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private StepOverlay overlay;


    AgilityPlusMain main;

    private boolean hasStarted = false;

    @Subscribe
    public void onStatChanged(StatChanged statChanged) {
        if (statChanged.getSkill() != AGILITY) {
            return;
        }
        AgilityPlusMain.xpDrop = true;
        overlay.setCurrentStep("xpDrop = true");
        log.info("xpDrop = true");
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        // Only detect inventory changes
        if (event.getContainerId() == InventoryID.INVENTORY.getId())
        {
            AgilityPlusMain.invUpdate = true;
            overlay.setCurrentStep("invUpdate = true");
            log.info("invUpdate = true");
        }
    }

    @Override
    protected void startUp() throws Exception {
        AgilityPlusObjectIDs.setAllVarsNull();
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        AgilityPlusObjectIDs.setAllVarsNull();
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onClientTick(ClientTick event) throws AWTException {
        toggleStatus();
    }

    private String stripTargetAnchors(String text) {
        Matcher m = Pattern.compile("ff>(.*?)</c").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private void toggleStatus() throws AWTException {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        if (chatboxInput == null) return;

        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if(chatBoxMessage.equals("1") && !AgilityPlusMain.isRunning && !hasStarted) {
            main = new AgilityPlusMain(client, clientThread, overlay);
            main.reset();

            AgilityPlusMain.isRunning = true;
            hasStarted = true;
            overlay.setCurrentStep("status is go");
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && AgilityPlusMain.isRunning && hasStarted) {
            main.stop();
            AgilityPlusMain.isRunning = false;
            hasStarted = false;
            overlay.setCurrentStep("status is stop");
            System.out.println("status is stop");
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

    public boolean getHasStarted() {
        return hasStarted;
    }
}
