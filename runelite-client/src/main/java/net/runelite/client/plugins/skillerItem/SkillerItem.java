package net.runelite.client.plugins.skillerItem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.runelite.api.*;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@PluginDescriptor(
        name = "SkillerItem",
        description = "Logs the amount of each SkillerItem in your bank to a specified JSON file."
)
public class SkillerItem extends Plugin {
    @Inject
    private Client client;

    private static final List<Integer> SKILLER_ITEMS = List.of(
            ItemID.MOLTEN_GLASS,
            ItemID.SODA_ASH,
            ItemID.BUCKET_OF_SAND,
            ItemID.SEAWEED,
            ItemID.GIANT_SEAWEED
    );

    private static final String DEFAULT_OUTPUT_FILE = "skillerItemsInBank.json"; // Default output file name

    @Override
    protected void startUp() {
        // Any setup if needed
    }

    @Override
    protected void shutDown() {
        // Any teardown if needed
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        // Check if bank is opened
        if (event.getGroupId() == InterfaceID.BANK) {
            logItemsInBank();
        }
    }

    private void logItemsInBank() {
        Map<String, Object> outputData = new LinkedHashMap<>();

        // Skill experience
        Map<String, Integer> skillExperience = new LinkedHashMap<>();
        skillExperience.put("craftingXP", client.getSkillExperience(Skill.CRAFTING));
        skillExperience.put("smithingXP", client.getSkillExperience(Skill.SMITHING));
        outputData.put("skillExperience", skillExperience);

        // Skiller items
        Map<String, Integer> skillerItemsMap = new LinkedHashMap<>();
        for (int itemId : SKILLER_ITEMS) {
            skillerItemsMap.put(getItemName(itemId), 0); // default 0
        }

        final ItemContainer bankItems = client.getItemContainer(InventoryID.BANK);
        if (bankItems != null) {
            for (Item item : bankItems.getItems()) {
                int itemID = item.getId();
                int quantity = item.getQuantity();
                if (SKILLER_ITEMS.contains(itemID)) {
                    String itemName = getItemName(itemID);
                    skillerItemsMap.put(itemName, quantity);
                }
            }
        }

        outputData.put("skillerItems", skillerItemsMap);

        writeDataToJson(outputData);
    }

    private String getItemName(int itemId) {
        switch (itemId) {
            case ItemID.MOLTEN_GLASS:
                return "moltenGlass";
            case ItemID.SODA_ASH:
                return "sodaAsh";
            case ItemID.BUCKET_OF_SAND:
                return "bucketOfSand";
            case ItemID.SEAWEED:
                return "seaweed";
            case ItemID.GIANT_SEAWEED:
                return "giantSeaweed";
            default:
                return "unknown";
        }
    }

    private Map<String, Object> createDefaultSeedData(String seedName) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("moltenGlass", 0);
        data.put("sodaAsh", 0);
        data.put("bucketOfSand", 0);
        data.put("seaweed", 0);
        data.put("giantSeaweed", 0);
        return data;
    }

    private void writeDataToJson(Map<String, Object> data) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        try (Writer writer = new FileWriter(DEFAULT_OUTPUT_FILE)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
