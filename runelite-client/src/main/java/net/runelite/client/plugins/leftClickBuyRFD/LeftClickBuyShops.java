package net.runelite.client.plugins.leftClickBuyRFD;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

@PluginDescriptor(name = "Left Click Buy Shops")
public class LeftClickBuyShops extends Plugin {
    @Inject
    private Client client;

    public List<Item> inventoryItems = new ArrayList<>();

    public ItemContainer inventoryContainer = null;

    private int chocolateBarCount = 0;
    private int grapeCount = 0;

    @Subscribe
    public void onGameTick(GameTick event) {
        // RFD Chest
        if(Objects.equals(client.getLocalPlayer().getWorldLocation(), new WorldPoint(3218, 9623, 0))) {
            if (isBankOpen() &&
                    getCountInventoryItems() == 1 &&
                    containsItem(ItemID.COINS_995)) {
                pressKey(KeyEvent.VK_ESCAPE);
            } else if (isShopOpen() &&
                    ((getWidgetCount(19660816, 1) == 0 &&
                            getWidgetCount(19660816, 5) == 0) ||
                            getCountInventoryItems() == 28)) {
                pressKey(KeyEvent.VK_ESCAPE);

                if (chocolateBarCount == 0 && grapeCount == 0)
                    quickHop();
            }
        }
        // Piscarilius shop
        else if (client.getLocalPlayer().getWorldLocation().getRegionID() == 7226) {
            int rawTunaCount = Integer.MAX_VALUE;
            int rawBassCount = Integer.MAX_VALUE;
            int rawSwordfishCount = Integer.MAX_VALUE;
            if (isShopOpen()) {
                rawTunaCount = getWidgetCount(19660816, 7);
                rawBassCount = getWidgetCount(19660816, 9);
                rawSwordfishCount = getWidgetCount(19660816, 10);

                if (getCountInventoryItems() == 28 &&
                        (containsItem(ItemID.RAW_TUNA) ||
                        containsItem(ItemID.RAW_BASS) ||
                        containsItem(ItemID.RAW_SWORDFISH)
                        )
                )
                    pressKey(KeyEvent.VK_ESCAPE);
            }

            if (rawTunaCount < 10 && rawBassCount < 10 && rawSwordfishCount < 10) {
                pressKey(KeyEvent.VK_ESCAPE);
                quickHop();
            }
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded)
    {
        if(Objects.equals(client.getLocalPlayer().getWorldLocation(), new WorldPoint(3218, 9623, 0))) {
            swapMenus();
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY)) {
            inventoryContainer = event.getItemContainer();
        }
    }


    public void swapMenus() {
        MenuEntry entry = client.getMenuEntries()[client.getMenuEntries().length - 1];
        String target = entry.getTarget();
        if(target != null && target.contains("Chest") && containsItem(ItemID.COINS_995)) {
            if(getCountInventoryItems() == 28) {
                swapBank();
            } else if(getCountInventoryItems() < 28) {
                swapBuyFood();
            }
        }
    }

    private void swapBank() {
        MenuEntry[] menuEntries = client.getMenuEntries();
        for (int i = menuEntries.length - 1; i >= 0; --i) {
            MenuEntry entry = menuEntries[i];
            if (entry.getOption().equals("Bank"))
            {
                menuEntries[i] = menuEntries[menuEntries.length - 1];
                menuEntries[menuEntries.length - 1] = entry;

                client.setMenuEntries(menuEntries);
                break;
            }
        }
    }

    private void swapBuyFood() {
        MenuEntry[] menuEntries = client.getMenuEntries();
        for (int i = menuEntries.length - 1; i >= 0; --i) {
            MenuEntry entry = menuEntries[i];
            if (entry.getOption().equals("Buy-food"))
            {
                menuEntries[i] = menuEntries[menuEntries.length - 1];
                menuEntries[menuEntries.length - 1] = entry;

                client.setMenuEntries(menuEntries);
                break;
            }
        }
    }

    private void pressKey(int key) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyRelease);
    }

    private int getCountInventoryItems() {
        inventoryItems = Arrays.asList(inventoryContainer.getItems());
        return inventoryContainer.count();
    }

    private boolean containsItem(int item) {
        return inventoryItems.stream().anyMatch(
                items -> items.getId() == item
        );
    }

    private int getWidgetCount(int widgetId, int index) {
        try {
            return client.getWidget(widgetId).getChild(index).getItemQuantity();
        } catch(Exception ignored) {
            return 0;
        }
    }

    private boolean isBankOpen() {
        return client.getWidget(WidgetInfo.BANK_CONTAINER) != null;
    }

    private boolean isShopOpen() {
        return client.getWidget(WidgetInfo.SHOP_INVENTORY_ITEMS_CONTAINER) != null;
    }

    private void quickHop() {
        try {
            Robot robot = new Robot();

            // Press and hold Ctrl, Shift, and Left Arrow
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_SHIFT);
            robot.keyPress(KeyEvent.VK_LEFT);

            // Release Left Arrow, Shift, and Ctrl
            robot.keyRelease(KeyEvent.VK_LEFT);
            robot.keyRelease(KeyEvent.VK_SHIFT);
            robot.keyRelease(KeyEvent.VK_CONTROL);

        } catch (Exception ignored){
        }
    }
}
