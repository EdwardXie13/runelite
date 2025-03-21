package net.runelite.client.plugins.pressSpace;

import com.google.inject.Provides;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

@PluginDescriptor(
        name = "Press Space",
        description = "press space on craft X window",
        tags = {"space"}
)
public class PressSpacePlugin extends Plugin {
    @Inject
    private Client client;

    private int recipe = 0;

    public List<Item> inventoryItems = new ArrayList<>();

    public Set<Integer> grimyHerbs = Set.of(
        ItemID.GRIMY_GUAM_LEAF,
        ItemID.GRIMY_MARRENTILL,
        ItemID.GRIMY_TARROMIN,
        ItemID.GRIMY_HARRALANDER,
        ItemID.GRIMY_RANARR_WEED,
        ItemID.GRIMY_IRIT_LEAF,
        ItemID.GRIMY_AVANTOE,
        ItemID.GRIMY_KWUARM,
        ItemID.GRIMY_CADANTINE,
        ItemID.GRIMY_DWARF_WEED,
        ItemID.GRIMY_TORSTOL,
        ItemID.GRIMY_LANTADYME,
        ItemID.GRIMY_TOADFLAX,
        ItemID.GRIMY_SNAPDRAGON
    );

    public Set<Integer> cleanHerbs = Set.of(
        ItemID.GUAM_LEAF,
        ItemID.MARRENTILL,
        ItemID.TARROMIN,
        ItemID.HARRALANDER,
        ItemID.RANARR_WEED,
        ItemID.IRIT_LEAF,
        ItemID.AVANTOE,
        ItemID.KWUARM,
        ItemID.CADANTINE,
        ItemID.DWARF_WEED,
        ItemID.TORSTOL,
        ItemID.LANTADYME,
        ItemID.TOADFLAX,
        ItemID.SNAPDRAGON
    );

    int countToWithdraw = 0;
    private boolean lockout = false;

    @Subscribe
    public void onGameTick(GameTick event) {
        closeBank();
        craftBox();
        smithingDarts();
        smeltingSilverBolts();
        bountyHunterWorldHop();
        withdrawSeedBox();
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY)) {
            inventoryItems = Arrays.asList(event.getItemContainer().getItems());
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (Optional.ofNullable(client.getWidget(8388611))
                .filter(Predicate.not(Widget::isHidden))
                .map(Widget::getText)
                .filter("Seed Box"::equals)
                .isPresent()) {
            String target = event.getTarget();
            List<Widget> seedBoxList = new ArrayList<>(
                    Arrays.asList(Objects.requireNonNull(client.getWidget(8388619)).getChildren())
            );
            // remove useless element
            seedBoxList.remove(seedBoxList.size() - 1);
            for(Widget widget : seedBoxList) {
                if (widget.getName().equals(target)) {
                    if (widget.getItemQuantity() > 1)
                        countToWithdraw = widget.getItemQuantity() - 1;
                    else
                        countToWithdraw = 0;
                }
            }

            //Swap withdraw X
            MenuEntry[] menuEntries = client.getMenuEntries();
            for (int i = menuEntries.length - 1; i >= 0; --i) {
                MenuEntry entry = menuEntries[i];
                if(entry.getOption().equals("Withdraw X")) {
                    entry.setType(MenuAction.CC_OP);

                    menuEntries[i] = menuEntries[menuEntries.length - 1];
                    menuEntries[menuEntries.length - 1] = entry;

                    client.setMenuEntries(menuEntries);
                    break;
                }
            }
        }
    }

    private void withdrawSeedBox() {
        if (Optional.ofNullable(client.getWidget(10616874))
                .filter(Predicate.not(Widget::isHidden))
                .map(Widget::getText)
                .filter("How many seeds?"::equals)
                .isPresent() &&
                countToWithdraw > 1 &&
                !lockout) {
            lockout = true;

            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    Robot robot = new Robot();
                    for (char ch : String.valueOf(countToWithdraw).toCharArray()) {
                        pressOtherKey(ch);
                        robot.delay(50);
                    }
                    robot.delay(50);
                    pressKey(KeyEvent.VK_ENTER);
                    robot.delay(100);
                } catch (Exception ignored) {
                } finally {
                    lockout = false;
                }
            });
        }
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

    private void smeltingSilverBolts() {
        Widget silverBoltBox = client.getWidget(393243);
        if(silverBoltBox != null && Objects.requireNonNull(silverBoltBox.getChildren()).length == 4) {
            pressKey(KeyEvent.VK_SPACE);
        }
    }

    private void bountyHunterWorldHop() {
        Widget dialogBox = client.getWidget(12648450);
        if(dialogBox != null && dialogBox.getText().contains("Bounty Hunter")) {
            pressKey(KeyEvent.VK_SPACE);
        }
    }

    private boolean doesInventoryContainCleanHerbs() {
        for(Integer id : cleanHerbs) {
            if(countItem(id, 14) && countItem(ItemID.VIAL_OF_WATER, 14))
                return true;
        }
        return false;
    }

    private boolean doesInventoryContainGrimyHerbs() {
        for(Integer id : grimyHerbs) {
            if(countItem(id, 28))
                return true;
        }
        return false;
    }

    private boolean doesInventoryContainRecipe() {
        for (Set<Pair<Integer, Integer>> recipe : Potions.potionRecipe) {
            if(doesInventoryContainRecipe(recipe))
                return true;
        }

        return false;
    }

    public boolean doesInventoryContainRecipe(Set<Pair<Integer, Integer>> set) {
        List<Boolean> values = new ArrayList<>();
        for (Pair<Integer, Integer> pair : set) {
            values.add(countItem(pair.getKey(), pair.getValue()));
        }
        return values.stream().allMatch(Boolean::booleanValue);
    }

    private int getCountItems(int item) {
        return (int) inventoryItems.stream()
                .filter(items -> items.getId() == item)
                .count();
    }

    private boolean countItem(int item, int count) {
        return inventoryItems.stream()
                .filter(items -> items.getId() == item)
                .count() >= count;
    }

    private boolean containsItem(int item) {
        return inventoryItems.stream().anyMatch(
                items -> items.getId() == item
        );
    }

    private int getRecipe() {
        if( doesInventoryContainGrimyHerbs() ||
            doesInventoryContainCleanHerbs() ||
            doesInventoryContainRecipe()
        ) {
            return 1;
        }
        // return -1 to not close bank but only press space
        else if(
                (containsItem(ItemID.ARROW_SHAFT) && containsItem(ItemID.FEATHER)) ||
                (containsItem(ItemID.SILVER_BOLTS_UNF) && containsItem(ItemID.FEATHER)) ||
                // Piscarilius cooking
                (containsItem(ItemID.RAW_TUNA) && containsItem(ItemID.COINS_995)) ||
                (containsItem(ItemID.RAW_BASS) && containsItem(ItemID.COINS_995)) ||
                (containsItem(ItemID.RAW_SWORDFISH) && containsItem(ItemID.COINS_995))
        ) {
            return -1;
        }
        else
            return 0;
    }

    private void closeBank() {
        recipe = getRecipe();
        if (isBankOpen() && recipe > 0)
            pressKey(KeyEvent.VK_ESCAPE);
    }

    private void craftBox() {
        Widget craftBox = client.getWidget(17694724);
        if (craftBox != null && craftBox.getText().equals("Choose a quantity, then click an item to begin.")) {
            // If at blast furnace
            if(client.getLocalPlayer().getWorldLocation().getRegionID() == 7757)
                pressKey(KeyEvent.VK_SPACE);
            else if(recipe == 0) {}

            else if (countItem(ItemID.MOLTEN_GLASS, 27) && countItem(ItemID.GLASSBLOWING_PIPE, 1)) {
                pressOtherKey('6');
            } else {
                pressKey(KeyEvent.VK_SPACE);
            }
        }
    }

    private boolean isBankOpen() {
        return client.getWidget(WidgetInfo.BANK_CONTAINER) != null;
    }

    private void pressOtherKey(int key) {
        KeyEvent keyTyped = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, (char) key);
        this.client.getCanvas().dispatchEvent(keyTyped);
    }

    private void pressKey(int key) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyRelease);
    }
}
