package net.runelite.client.plugins.tanningPlus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.AWTException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
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

    private STATUS toggleStatus = STATUS.STOP;

    private boolean hasStarted = false;

    TanningPlusMain thread;

    public static NPC tanner = null;

    public static GameObject bankChest = null;
    public static GameObject bottomStair = null;
    public static GameObject topStair = null;

    enum STATUS{
        START,
        STOP
    }

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

        if(chatBoxMessage.equals("1") && toggleStatus == STATUS.STOP && !hasStarted) {
            toggleStatus = STATUS.START;
            hasStarted = true;
            thread = new TanningPlusMain(client, clientThread);
            TanningPlusMain.pause = false;
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && toggleStatus == STATUS.START && hasStarted) {
            toggleStatus = STATUS.STOP;
            thread.t.interrupt();
            hasStarted = false;
            TanningPlusMain.pause = true;
            System.out.println("status is stop");
        }
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN && hasStarted)
        {
            toggleStatus = STATUS.STOP;
            thread.t.interrupt();
            hasStarted = false;
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event)
    {
        final NPC npc = event.getNpc();

        if (npc.getId() == NpcID.TANNER)
        {
            tanner = npc;
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        final NPC npc = event.getNpc();

        if (npc.getId() == NpcID.TANNER)
        {
            tanner = null;
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        final GameObject gameObject = event.getGameObject();
        int gameobjectID = gameObject.getId();
        if(gameobjectID == ObjectID.BANK_CHEST_14886) {
            bankChest = gameObject;
        } else if(gameobjectID == ObjectID.STAIRCASE_9582) {
            bottomStair = gameObject;
        } else if(gameobjectID == ObjectID.STAIRCASE_9584) {
            topStair = gameObject;
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        final GameObject gameObject = event.getGameObject();
        int gameobjectID = gameObject.getId();
        if(gameobjectID == ObjectID.BANK_CHEST_14886) {
            bankChest = null;
        } else if(gameobjectID == ObjectID.STAIRCASE_9582) {
            bottomStair = null;
        } else if(gameobjectID == ObjectID.STAIRCASE_9584) {
            topStair = null;
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY)) {
            TanningPlusMain.inventoryItems = Arrays.asList(event.getItemContainer().getItems());
        }
    }
}
