package net.runelite.client.plugins.tithefarmplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.tithefarm.TitheFarmPlantOverlay;
import net.runelite.client.ui.overlay.OverlayManager;

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

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private StepOverlay overlay;

    TitheFarmPlusMain main;

    private boolean hasStarted = false;
    public long start;

    @Subscribe
    public void onGameTick(GameTick event) throws AWTException {
        toggleStatus();
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
    }

    private String stripTargetAnchors(String text) {
        Matcher m = Pattern.compile("ff>(.*?)</c").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private void toggleStatus() throws AWTException {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if(chatBoxMessage.equals("1") && !TitheFarmPlusMain.isRunning && !hasStarted) {
            start = System.currentTimeMillis();
            main = new TitheFarmPlusMain(client, clientThread, overlay);
            main.reset();

            TitheFarmPlusMain.isRunning = true;
            hasStarted = true;
            overlay.setCurrentStep("status is go");
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && TitheFarmPlusMain.isRunning && hasStarted) {
            main.stop();
            TitheFarmPlusMain.isRunning = false;
            hasStarted = false;
            overlay.setCurrentStep("status is stop");
            System.out.println("status is stop");
        }
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN && TitheFarmPlusMain.isRunning && hasStarted)
        {
            TitheFarmPlusMain.isRunning = false;
            hasStarted = false;
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        TitheFarmPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onChatMessage(final ChatMessage event) {
        String chatMessage = event.getMessage();
        if(chatMessage.equals("Watering can charges remaining: 10.0%"))
            TitheFarmPlusMain.fillWateringCan = true;
    }

    @Override
    protected void shutDown() throws Exception
    {
        TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.clear();
        TitheFarmPlusWorldPoints.patchNumByTile.clear();
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.clear();
        TitheFarmPlusWorldPoints.waterBarrelWorldPoint = null;
        TitheFarmPlusMain.isRunning = false;
        TitheFarmPlusMain.hasInit = false;
        TitheFarmPlusMain.fillWateringCan = false;
        overlayManager.remove(overlay);
    }
}
