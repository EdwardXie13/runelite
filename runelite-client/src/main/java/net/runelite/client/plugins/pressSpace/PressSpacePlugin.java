package net.runelite.client.plugins.pressSpace;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemID;
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
        closeBank();
        craftBox();
    }

    private int countItem(int item) {
        return client.getItemContainer(InventoryID.INVENTORY).count(item);
    }

    private void closeBank() {
        if(
            (countItem(ItemID.VIAL_OF_WATER) == 14 && countItem(ItemID.GUAM_LEAF) == 14) ||
            (countItem(ItemID.GUAM_POTION_UNF) == 14 && countItem(ItemID.EYE_OF_NEWT) == 14) ||
            (countItem(ItemID.BATTLESTAFF) == 14 && countItem(ItemID.FIRE_ORB) == 14)
        ) {
            robot.keyPress(KeyEvent.VK_ESCAPE);
        }
    }

    private void craftBox() {
        Widget craftBox = client.getWidget(17694724);
        if(craftBox != null && craftBox.getText().equals("Choose a quantity, then click an item to begin."))
            robot.keyPress(KeyEvent.VK_SPACE);
    }
}
