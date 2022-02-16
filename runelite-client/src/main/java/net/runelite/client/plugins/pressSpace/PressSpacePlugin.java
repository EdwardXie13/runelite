package net.runelite.client.plugins.pressSpace;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;

@PluginDescriptor(
        name = "Press Space",
        description = "press space on craft X window",
        tags = {"space"}
)
public class PressSpacePlugin extends Plugin {
    @Inject
    private Client client;

    public static Robot robot;

    @Override
    protected void startUp() throws Exception {
        robot = new Robot();
    }

    @Override
    protected void shutDown() throws Exception {
        robot = null;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        craftBox();
    }

    private void closeBank() {

    }

    private void craftBox() {
        Widget craftBox = client.getWidget(17694725);
        if(craftBox != null && craftBox.getText().equals("How many do you wish to make?"))
            robot.keyPress(KeyEvent.VK_SPACE);
    }
}
