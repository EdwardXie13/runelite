package net.runelite.client.plugins.rcPlusTrueBloods;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
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
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.AWTException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(name = "RC Plus True Bloods", enabledByDefault = false)
@Slf4j
public class RCPlusTrueBloodsPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private WorldService worldService;

    RCPlusTrueBloodsMain thread;
    

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

        if(chatBoxMessage.equals("1") && !RCPlusTrueBloodsMain.isRunning) {
            thread = new RCPlusTrueBloodsMain(client, clientThread, worldService);
            RCPlusTrueBloodsMain.isRunning = true;
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && RCPlusTrueBloodsMain.isRunning) {
            thread.t.interrupt();
            RCPlusTrueBloodsMain.isRunning = false;
            System.out.println("status is stop");
        }
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private void checkOculusReset() {
        if(RCPlusTrueBloodsMain.resetOculusOrb){
            client.setOculusOrbState(0);
            RCPlusTrueBloodsMain.resetOculusOrb = false;
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN && RCPlusTrueBloodsMain.isRunning)
        {
            RCPlusTrueBloodsMain.isRunning = false;
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        RCPlusTrueBloodsObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        RCPlusTrueBloodsObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY)) {
            RCPlusTrueBloodsMain.inventoryItems = Arrays.asList(event.getItemContainer().getItems());
        }
    }
}
