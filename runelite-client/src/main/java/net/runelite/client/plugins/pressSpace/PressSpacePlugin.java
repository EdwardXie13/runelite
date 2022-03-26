package net.runelite.client.plugins.pressSpace;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemID;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.Robot;
import java.awt.event.KeyEvent;

@PluginDescriptor(
        name = "Press Space",
        description = "press space on craft X window",
        tags = {"space"}
)
public class PressSpacePlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private PressSpaceConfig config;

    private int recipe;

    @Provides
    PressSpaceConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PressSpaceConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        closeBank();
        craftBox();
    }

    private int countItem(int item) {
        return client.getItemContainer(InventoryID.INVENTORY).count(item);
    }

    private int getRecipe() {
        if (config.guamPotUnf() && countItem(ItemID.VIAL_OF_WATER) == 14 && countItem(ItemID.GUAM_LEAF) == 14)
            return Recipe.GuamPot.getId();
        else if (config.attackPot() && countItem(ItemID.GUAM_POTION_UNF) == 14 && countItem(ItemID.EYE_OF_NEWT) == 14)
            return Recipe.AttackPot.getId();
        else if (config.glassBlowing() && countItem(ItemID.MOLTEN_GLASS) == 27 && countItem(ItemID.GLASSBLOWING_PIPE) == 1)
            return Recipe.BlowGlass.getId();
        else if (config.fireBattlestaff() && countItem(ItemID.BATTLESTAFF) == 14 && countItem(ItemID.FIRE_ORB) == 14)
            return Recipe.FireBstaff.getId();
        else
            return Recipe.None.getId();
    }

    private void closeBank() {
        recipe = getRecipe();
        if (isBankOpen() && recipe != 0)
            pressKey(KeyEvent.VK_ESCAPE);
    }

    private void craftBox() {
        Widget craftBox = client.getWidget(17694724);
        if (craftBox != null && craftBox.getText().equals("Choose a quantity, then click an item to begin.")) {
            if(recipe == 0) return;
            if (recipe == Recipe.BlowGlass.getId() && getWidget()) {
                pressOtherKey(KeyEvent.VK_6);
            } else {
                pressKey(KeyEvent.VK_SPACE);
            }
        }
    }

    private boolean isBankOpen() {
        return client.getWidget(WidgetInfo.BANK_CONTAINER) != null;
    }

    private boolean getWidget() {
        return client.getWidget(17694733).getChild(5).getText().equals("6");
    }

    private void pressOtherKey(int key) {
        try {
            Robot robot = new Robot();
            robot.keyPress(key);
        } catch (Exception ignored){
        }
    }

    private void pressKey(int key) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyRelease);
    }
}
