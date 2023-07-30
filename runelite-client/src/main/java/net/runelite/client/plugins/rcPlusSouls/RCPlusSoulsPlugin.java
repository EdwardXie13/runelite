package net.runelite.client.plugins.rcPlusSouls;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
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

@PluginDescriptor(name = "RC Plus Souls", enabledByDefault = false)
@Slf4j
public class RCPlusSoulsPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    public int lastRcExp = 0;

    static boolean denseRunestoneSouthMineable;

    static boolean denseRunestoneNorthMineable;

    RCPlusSoulsMain thread;
    

    @Subscribe
    public void onGameTick(GameTick event) throws AWTException {
        lastRcExp = client.getSkillExperience(Skill.RUNECRAFT);
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

        if(chatBoxMessage.equals("1") && !RCPlusSoulsMain.isRunning) {
            thread = new RCPlusSoulsMain(client, clientThread);
            RCPlusSoulsMain.isRunning = true;
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && RCPlusSoulsMain.isRunning) {
            thread.t.interrupt();
            RCPlusSoulsMain.isRunning = false;
            System.out.println("status is stop");
        }
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private void checkOculusReset() {
        if(RCPlusSoulsMain.resetOculusOrb){
            client.setOculusOrbState(0);
            RCPlusSoulsMain.resetOculusOrb = false;
        }
    }

//    private void sendSlackMessage(String text) {
//        SlackMessage slackMessage = SlackMessage.builder()
//                .text(text)
//                .build();
//        SlackUtils.sendMessage(slackMessage);
//    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN && RCPlusSoulsMain.isRunning)
        {
            RCPlusSoulsMain.isRunning = false;
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        updateDenseRunestoneState();
    }

    private void updateDenseRunestoneState()
    {
        denseRunestoneSouthMineable = client.getVarbitValue(Varbits.DENSE_RUNESTONE_SOUTH_DEPLETED) == 0;
        denseRunestoneNorthMineable = client.getVarbitValue(Varbits.DENSE_RUNESTONE_NORTH_DEPLETED) == 0;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY)) {
            RCPlusSoulsMain.inventoryItems = Arrays.asList(event.getItemContainer().getItems());
        }
    }
}
