package net.runelite.client.plugins.twoclickcrafting;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;

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

@PluginDescriptor(name = "TwoClickCrafting", enabledByDefault = false)
@Slf4j
public class TwoClickCraftingPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    static TwoClickCraftingMain thread;

    @Subscribe
    public void onGameTick(GameTick event) throws AWTException {
        toggleStatus();
    }

    private String stripTargetAnchors(String text) {
        Matcher m = Pattern.compile("ff>(.*?)</c").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private void toggleStatus() throws AWTException {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if(chatBoxMessage.equals("1") && !TwoClickCraftingMain.isRunning) {
            thread = new TwoClickCraftingMain(client, clientThread);
            TwoClickCraftingMain.isRunning = true;
            System.out.println("status is go (two click crafting");
        } else if (chatBoxMessage.equals("2") && TwoClickCraftingMain.isRunning) {
            thread.t.interrupt();
            TwoClickCraftingMain.isRunning = false;
            System.out.println("status is stop (two click crafting)");
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN && TwoClickCraftingMain.isRunning)
        {
            TwoClickCraftingMain.isRunning = false;
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY)) {
            TwoClickCraftingMain.inventoryItems = Arrays.asList(event.getItemContainer().getItems());
        }
    }

}
