package net.runelite.client.plugins.tithefarmplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
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

@PluginDescriptor(name = "Tithe Farm Plus", enabledByDefault = false)
@Slf4j
public class TitheFarmPlusPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    TitheFarmPlusMain thread;

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

        if(chatBoxMessage.equals("1") && !TitheFarmPlusMain.isRunning) {
            thread = new TitheFarmPlusMain(client, clientThread);
            TitheFarmPlusMain.isRunning = true;
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && TitheFarmPlusMain.isRunning) {
            thread.t.interrupt();
            TitheFarmPlusMain.isRunning = false;
            System.out.println("status is stop");
        }
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private void checkOculusReset() {
        if(TitheFarmPlusMain.resetOculusOrb){
            client.setOculusOrbState(0);
            TitheFarmPlusMain.resetOculusOrb = false;
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN && TitheFarmPlusMain.isRunning)
        {
            TitheFarmPlusMain.isRunning = false;
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        TitheFarmPlusObjectIDs.assignObjects(event);
    }
}
