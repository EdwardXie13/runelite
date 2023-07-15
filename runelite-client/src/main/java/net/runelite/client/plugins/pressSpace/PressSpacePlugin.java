package net.runelite.client.plugins.pressSpace;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.Set;

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
        smithingDarts();
    }

    private void smithingDarts() {
//        20447261 size == 4 means press space
        if(client.getWidget(WidgetInfo.SMITHING_INVENTORY_ITEMS_CONTAINER) != null) {
            Widget dartBox = client.getWidget(20447261);
            if(dartBox != null && Objects.requireNonNull(dartBox.getChildren()).length == 4) {
                pressKey(KeyEvent.VK_SPACE);
            }
        }
    }

    private int countItem(int item) {
        return client.getItemContainer(InventoryID.INVENTORY).count(item);
    }

    private boolean containsItem(int item) {
        return client.getItemContainer(InventoryID.INVENTORY).contains(item);
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
        else if (config.staminaPot() && (countItem(ItemID.SUPER_ENERGY4) == 27 || countItem(ItemID.SUPER_ENERGY3) == 27) && containsItem(ItemID.AMYLASE_CRYSTAL))
            return Recipe.Stamina.getId();
        else if(config.cutSapphire() && countItem(ItemID.UNCUT_SAPPHIRE) == 27 && containsItem(ItemID.CHISEL))
            return Recipe.CutSapphire.getId();
        else if(config.craftDragonhide() && containsItem(ItemID.NEEDLE) && containsItem(ItemID.THREAD) &&
                (countItem(ItemID.GREEN_DRAGON_LEATHER) == 26 ||
                        countItem(ItemID.BLUE_DRAGON_LEATHER) == 26 ||
                        countItem(ItemID.RED_DRAGON_LEATHER) == 26 ||
                        countItem(ItemID.BLACK_DRAGON_LEATHER) == 26)
        )
            return Recipe.Dragonhide.getId();
        else if(config.tanDragonhide() && containsItem(ItemID.NATURE_RUNE) && containsItem(ItemID.ASTRAL_RUNE) && containsItem(ItemID.JUG))
            return Recipe.Dragonhide.getId();
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
            } else if(recipe == Recipe.Dragonhide.getId()) {
                pressOtherKey(KeyEvent.VK_1);
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
