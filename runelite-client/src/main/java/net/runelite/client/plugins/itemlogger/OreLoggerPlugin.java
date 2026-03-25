package net.runelite.client.plugins.itemlogger;


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
import java.util.Optional;

@PluginDescriptor(
        name = "Ore Logger",
        description = "Logs the amount of each ore and in your bank to a specified JSON file."
)
public class OreLoggerPlugin extends Plugin {

    @Inject
    private Client client;

    private static final List<Integer> ORES = List.of(
            ItemID.IRON_ORE,
            ItemID.MITHRIL_ORE,
            ItemID.ADAMANTITE_ORE,
            ItemID.COAL
    );

    private static final List<Integer> BARS = List.of(
            ItemID.STEEL_BAR,
            ItemID.MITHRIL_BAR,
            ItemID.ADAMANTITE_BAR
    );

    private static final List<Integer> GEAR = List.of(
            ItemID.STEEL_PLATELEGS,
            ItemID.STEEL_PLATESKIRT,
            ItemID.STEEL_KITESHIELD,

            ItemID.MITHRIL_PLATELEGS,
            ItemID.MITHRIL_PLATESKIRT,
            ItemID.MITHRIL_KITESHIELD,

            ItemID.ADAMANT_PLATELEGS,
            ItemID.ADAMANT_PLATESKIRT,
            ItemID.ADAMANT_KITESHIELD
    );

    private static final String DEFAULT_OUTPUT_FILE = "oresInBank.json"; // Default output file name

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
            logSeedsInBank();
        }
    }

    private void logSeedsInBank() {
        Map<String, Map<String, Object>> oreData = new LinkedHashMap<>();

        // Populate the LinkedHashMap with default values in the desired order
        List<String> orderedKeys = List.of("skillExperience", "steel", "mith", "addy", "coal");
        for (String key : orderedKeys) {
            oreData.put(key, createDefaultOreData(key));
        }

        final ItemContainer bankItems = client.getItemContainer(InventoryID.BANK);
        if (bankItems != null) {
            for (Item item : bankItems.getItems()) {
                int itemID = item.getId();
                int quantity = item.getQuantity();

                // Process ores
                if (ORES.contains(itemID)) {
                    String name = getOreName(itemID);
                    Map<String, Object> data = oreData.computeIfAbsent(name, this::createDefaultOreData);
                    data.put("currentOres", Optional.of(quantity).orElse(0));
                }
                // Process bars
                else if (BARS.contains(itemID)) {
                    String name = getBarName(itemID);
                    Map<String, Object> data = oreData.computeIfAbsent(name, this::createDefaultOreData);
                    data.put("currentBars", Optional.of(quantity).orElse(0));
                }
                // Process gear
                else if (GEAR.contains(itemID)) {
                    String name = getGearName(itemID);
                    int convertedOutput = getConvertedGearCost(itemID) * quantity;
                    Map<String, Object> data = oreData.computeIfAbsent(name, this::createDefaultOreData);
                    data.merge("convertedBars", convertedOutput, (oldVal, newVal) -> (int) oldVal + (int) newVal);
                }
            }
        }

        // Add skill experience to the output
        Map<String, Integer> skillExperience = new LinkedHashMap<>();
        skillExperience.put("smithingXp", client.getSkillExperience(Skill.SMITHING));

        // Add skill experience to the JSON output
        oreData.put("skillExperience", new LinkedHashMap<>());
        skillExperience.forEach((key, value) -> oreData.get("skillExperience").put(key, value));

        writeDataToJson(oreData);
    }

    private String getOreName(int itemId) {
        switch (itemId) {
            case ItemID.IRON_ORE:
                return "steel";
            case ItemID.MITHRIL_ORE:
                return "mith";
            case ItemID.ADAMANTITE_ORE:
                return "addy";
            case ItemID.COAL:
                return "coal";
            default:
                return "unknown";
        }
    }

    private String getBarName(int itemId) {
        switch (itemId) {
            case ItemID.STEEL_BAR:
                return "steel";
            case ItemID.MITHRIL_BAR:
                return "mith";
            case ItemID.ADAMANTITE_BAR:
                return "addy";
            default:
                return "unknown";
        }
    }

    private Integer getConvertedGearCost(int itemId) {
        switch (itemId) {
            case ItemID.STEEL_PLATELEGS:
            case ItemID.STEEL_PLATESKIRT:
            case ItemID.STEEL_KITESHIELD:
            case ItemID.MITHRIL_PLATELEGS:
            case ItemID.MITHRIL_PLATESKIRT:
            case ItemID.MITHRIL_KITESHIELD:
            case ItemID.ADAMANT_PLATELEGS:
            case ItemID.ADAMANT_PLATESKIRT:
            case ItemID.ADAMANT_KITESHIELD:
                return 2;
            default:
                return 0;
        }
    }

    private String getGearName(int itemId) {
        switch (itemId) {
            case ItemID.STEEL_PLATELEGS:
            case ItemID.STEEL_PLATESKIRT:
            case ItemID.STEEL_KITESHIELD:
                return "steel";
            case ItemID.MITHRIL_PLATELEGS:
            case ItemID.MITHRIL_PLATESKIRT:
            case ItemID.MITHRIL_KITESHIELD:
                return "mith";
            case ItemID.ADAMANT_PLATELEGS:
            case ItemID.ADAMANT_PLATESKIRT:
            case ItemID.ADAMANT_KITESHIELD:
                return "addy";
            default:
                return "unknown";
        }
    }

    private Map<String, Object> createDefaultOreData(String seedName) {
        Map<String, Object> data = new LinkedHashMap<>();
        switch (seedName) {
            case "steel":
                data.put("smeltXP", 17.5);
                data.put("barXp", 37.5);
                data.put("oreCost", 25.0);
                data.put("coalRequired", 1);
                data.put("currentOres", 0);
                data.put("currentBars", 0);
                data.put("convertedBars", 0);
                break;
            case "mith":
                data.put("smeltXP", 30.0);
                data.put("barXp", 50.0);
                data.put("oreCost", 243.0);
                data.put("coalRequired", 2);
                data.put("currentOres", 0);
                data.put("currentBars", 0);
                data.put("convertedBars", 0);
                break;
            case "addy":
                data.put("smeltXP", 30.0);
                data.put("barXp", 50.0);
                data.put("oreCost", 27.0);
                data.put("coalRequired", 3);
                data.put("currentOres", 0);
                data.put("currentBars", 0);
                data.put("convertedBars", 0);
                break;
            case "coal":
                data.put("oreCost", 67);
                data.put("currentOres", 0);
                break;
            default:
                data.put("smeltXP", 0.0);
                data.put("barXp", 0.0);
                data.put("oreCost", 0.0);
                data.put("currentOres", 0);
                data.put("currentBars", 0);
                data.put("convertedBars", 0);
                break;
        }
        return data;
    }

    private void writeDataToJson(Map<String, Map<String, Object>> seedData) {
        // Custom Gson instance without using BigDecimal for numbers
        Gson gson = new GsonBuilder()
                .setPrettyPrinting() // Keep the JSON pretty printed
                .create();

        try (Writer writer = new FileWriter(DEFAULT_OUTPUT_FILE)) {
            gson.toJson(seedData, writer); // Write the data to JSON file without string conversion
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}