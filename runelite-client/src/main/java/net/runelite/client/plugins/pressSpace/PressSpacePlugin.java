package net.runelite.client.plugins.pressSpace;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
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

    List<Integer> cakeIngredients = new ArrayList<>(Arrays.asList(ItemID.BUCKET_OF_MILK, ItemID.POT_OF_FLOUR, ItemID.EGG));

    @Provides
    PressSpaceConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PressSpaceConfig.class);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        closeBank();
        craftBox();
        smithingDarts();
        smeltingSilverBolts();
        gatherIngredients();
        bountyHunterWorldHop();
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY)) {
            inventoryItems = Arrays.asList(event.getItemContainer().getItems());
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

    private void gatherIngredients() {
        Widget chatBox = client.getWidget(14352385);
        if (chatBox != null) {
            Widget[] chatBoxChildren = chatBox.getChildren();
            if(chatBoxChildren != null && Arrays.stream(chatBoxChildren).findAny().isPresent()) {
                // Cake Items
                if(containsItem(ItemID.CAKE_TIN)) {
                    Integer leastcakeIngredients = checkCakeIngredients();
                    if (leastcakeIngredients.intValue() == 1927) // milk
                        pressOtherKey('2');
                    else if (leastcakeIngredients.intValue() == 1944) // egg
                        pressOtherKey('3');
                    else if (leastcakeIngredients.intValue() == 1933) // flour
                        pressOtherKey('4');

                }
            }
        }
    }

    private Integer checkCakeIngredients() {
        Map<Integer, Integer> ingredientCounts = new HashMap<>();

        // Populate the map with item counts
        cakeIngredients.forEach(item -> {
            int count = getCountItems(item);
            ingredientCounts.put(item, count);
        });

        return Collections.min(ingredientCounts.entrySet(), Map.Entry.comparingByValue()).getKey();
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
