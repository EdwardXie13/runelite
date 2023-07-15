package net.runelite.client.plugins.tanningPlus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ObjectID;
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

import javax.inject.Inject;
import java.awt.AWTException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(name = "Tanning Plus", enabledByDefault = false)
@Slf4j
public class TanningPlusPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    TanningPlusMain thread;

    @Subscribe
    public void onGameTick(GameTick event) throws AWTException {
        toggleStatus();
    }

    private String stripTargetAnchors(String text) {
        Matcher m = Pattern.compile(">(.*?)<").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private void toggleStatus() throws AWTException {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if(chatBoxMessage.equals("1") && !TanningPlusMain.isRunning) {
            thread = new TanningPlusMain(client, clientThread);
            TanningPlusMain.isRunning = true;
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && TanningPlusMain.isRunning) {
            thread.t.interrupt();
            TanningPlusMain.isRunning = false;
            System.out.println("status is stop");
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN && TanningPlusMain.isRunning)
        {
            TanningPlusMain.isRunning = false;
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY)) {
            TanningPlusMain.inventoryItems = Arrays.asList(event.getItemContainer().getItems());
        }
    }
}
