package net.runelite.client.plugins.theivingPlus;

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
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.AWTException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(name = "Thieving Plus", enabledByDefault = false)
@Slf4j
public class ThievingPlusPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;
    
    ThievingPlusMain thread;

    private int currentInvCount = 0;

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

        if(chatBoxMessage.equals("1") && !ThievingPlusMain.isRunning) {
            thread = new ThievingPlusMain(client, clientThread);
            ThievingPlusMain.isRunning = true;
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && ThievingPlusMain.isRunning) {
            thread.t.interrupt();
            ThievingPlusMain.isRunning = false;
            System.out.println("status is stop");
        }
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN && ThievingPlusMain.isRunning)
        {
            ThievingPlusMain.isRunning = false;
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        ThievingPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        ThievingPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onItemContainerChanged(final ItemContainerChanged event) {
        if (event.getContainerId() != InventoryID.INVENTORY.getId()) {
            return;
        }

        int newCount = client.getItemContainer(InventoryID.INVENTORY.getId()).count();
        if(newCount > currentInvCount) {
            currentInvCount = newCount;
            ThievingPlusMain.isIdle = true;
        } else if(newCount < currentInvCount) {
            currentInvCount = newCount;
        }
    }
}
