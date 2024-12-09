package net.runelite.client.plugins.seedlogger;

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.io.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@PluginDescriptor(
        name = "Seed Logger",
        description = "Logs the amount of each seed in your bank to a specified JSON file."
)
public class SeedLoggerPlugin extends Plugin {

    @Inject
    private Client client;

    private static final List<Integer> SEEDS = List.of(
            ItemID.GUAM_SEED,
            ItemID.HARRALANDER_SEED,
            ItemID.RANARR_SEED,
            ItemID.TOADFLAX_SEED,
            ItemID.IRIT_SEED,
            ItemID.AVANTOE_SEED,
            ItemID.KWUARM_SEED,
            ItemID.SNAPDRAGON_SEED,
            ItemID.CADANTINE_SEED,
            ItemID.LANTADYME_SEED,
            ItemID.TORSTOL_SEED
    );

    private static final List<Integer> GRIMY_HERBS = List.of(
            ItemID.GRIMY_GUAM_LEAF,
            ItemID.GRIMY_HARRALANDER,
            ItemID.GRIMY_RANARR_WEED,
            ItemID.GRIMY_TOADFLAX,
            ItemID.GRIMY_IRIT_LEAF,
            ItemID.GRIMY_AVANTOE,
            ItemID.GRIMY_KWUARM,
            ItemID.GRIMY_SNAPDRAGON,
            ItemID.GRIMY_CADANTINE,
            ItemID.GRIMY_CADANTINE,
            ItemID.GRIMY_TORSTOL
    );

    private static final List<Integer> CLEAN_HERBS = List.of(
            ItemID.GUAM_LEAF,
            ItemID.HARRALANDER,
            ItemID.RANARR_WEED,
            ItemID.TOADFLAX,
            ItemID.IRIT_LEAF,
            ItemID.AVANTOE,
            ItemID.KWUARM,
            ItemID.SNAPDRAGON,
            ItemID.CADANTINE,
            ItemID.CADANTINE,
            ItemID.TORSTOL
    );

    private static final List<Integer> UNF_POTS = List.of(
            ItemID.GUAM_POTION_UNF,
            ItemID.HARRALANDER_POTION_UNF,
            ItemID.RANARR_POTION_UNF,
            ItemID.TOADFLAX_POTION_UNF,
            ItemID.IRIT_POTION_UNF,
            ItemID.AVANTOE_POTION_UNF,
            ItemID.KWUARM_POTION_UNF,
            ItemID.SNAPDRAGON_POTION_UNF,
            ItemID.CADANTINE_POTION_UNF,
            ItemID.LANTADYME_POTION_UNF,
            ItemID.TORSTOL_POTION_UNF
    );

    private static final String DEFAULT_OUTPUT_FILE = "seedsInBank.json"; // Default output file name

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
        Map<String, Map<String, Object>> seedData = new LinkedHashMap<>();

        // Populate the LinkedHashMap with default values in the desired order
        List<String> orderedKeys = List.of("skillExperience", "guam", "harralander", "ranarr", "toadflax", "irit",
                "avantoe", "kwuarm", "snapdragon", "cadantine", "lantadyme", "torstol");
        for (String key : orderedKeys) {
            seedData.put(key, createDefaultSeedData(key));
        }

        final ItemContainer bankItems = client.getItemContainer(InventoryID.BANK);
        if (bankItems != null) {
            for (Item item : bankItems.getItems()) {
                int itemID = item.getId();
                int quantity = item.getQuantity();

                // Process seeds
                if (SEEDS.contains(itemID)) {
                    String seedName = getSeedName(itemID);
                    seedData.putIfAbsent(seedName, createDefaultSeedData(seedName));
                    seedData.get(seedName).put("currentSeeds", quantity);
                }
                // Process grimy herbs
                else if (GRIMY_HERBS.contains(itemID)) {
                    String seedName = getHerbName(itemID);
                    seedData.putIfAbsent(seedName, createDefaultSeedData(seedName));
                    seedData.get(seedName).put("grimyHerb", quantity);
                }
                // Process clean herbs
                else if (CLEAN_HERBS.contains(itemID)) {
                    String seedName = getHerbName(itemID);
                    seedData.putIfAbsent(seedName, createDefaultSeedData(seedName));
                    seedData.get(seedName).put("cleanHerb", quantity);
                }
                // Process unf pots
                else if (UNF_POTS.contains(itemID)) {
                    String seedName = getHerbName(itemID);
                    seedData.putIfAbsent(seedName, createDefaultSeedData(seedName));
                    seedData.get(seedName).put("unfPot", quantity);
                }
            }
        }

        // Add skill experience to the output
        Map<String, Integer> skillExperience = new LinkedHashMap<>();
        skillExperience.put("herbloreXp", client.getSkillExperience(Skill.HERBLORE));
        skillExperience.put("thievingXp", client.getSkillExperience(Skill.THIEVING));
        skillExperience.put("farmingXp", client.getSkillExperience(Skill.FARMING));

        // Add skill experience to the JSON output
        seedData.put("skillExperience", new LinkedHashMap<>());
        skillExperience.forEach((key, value) -> seedData.get("skillExperience").put(key, value));

        writeDataToJson(seedData);
    }

    private String getSeedName(int itemId) {
        switch (itemId) {
            case ItemID.GUAM_SEED:
                return "guam";
            case ItemID.HARRALANDER_SEED:
                return "harralander";
            case ItemID.RANARR_SEED:
                return "ranarr";
            case ItemID.TOADFLAX_SEED:
                return "toadflax";
            case ItemID.IRIT_SEED:
                return "irit";
            case ItemID.AVANTOE_SEED:
                return "avantoe";
            case ItemID.KWUARM_SEED:
                return "kwuarm";
            case ItemID.SNAPDRAGON_SEED:
                return "snapdragon";
            case ItemID.CADANTINE_SEED:
                return "cadantine";
            case ItemID.LANTADYME_SEED:
                return "lantadyme";
            case ItemID.TORSTOL_SEED:
                return "torstol";
            default:
                return "unknown";
        }
    }

    private String getHerbName(int itemId) {
        switch (itemId) {
            case ItemID.GRIMY_GUAM_LEAF:
            case ItemID.GUAM_LEAF:
            case ItemID.GUAM_POTION_UNF:
                return "guam";
            case ItemID.GRIMY_HARRALANDER:
            case ItemID.HARRALANDER:
            case ItemID.HARRALANDER_POTION_UNF:
                return "harralander";
            case ItemID.GRIMY_RANARR_WEED:
            case ItemID.RANARR_WEED:
            case ItemID.RANARR_POTION_UNF:
                return "ranarr";
            case ItemID.GRIMY_TOADFLAX:
            case ItemID.TOADFLAX:
            case ItemID.TOADFLAX_POTION_UNF:
                return "toadflax";
            case ItemID.GRIMY_IRIT_LEAF:
            case ItemID.IRIT_LEAF:
            case ItemID.IRIT_POTION_UNF:
                return "irit";
            case ItemID.GRIMY_AVANTOE:
            case ItemID.AVANTOE:
            case ItemID.AVANTOE_POTION_UNF:
                return "avantoe";
            case ItemID.GRIMY_KWUARM:
            case ItemID.KWUARM:
            case ItemID.KWUARM_POTION_UNF:
                return "kwuarm";
            case ItemID.GRIMY_SNAPDRAGON:
            case ItemID.SNAPDRAGON:
            case ItemID.SNAPDRAGON_POTION_UNF:
                return "snapdragon";
            case ItemID.GRIMY_CADANTINE:
            case ItemID.CADANTINE:
            case ItemID.CADANTINE_POTION_UNF:
                return "cadantine";
            case ItemID.GRIMY_LANTADYME:
            case ItemID.LANTADYME:
            case ItemID.LANTADYME_POTION_UNF:
                return "lantadyme";
            case ItemID.GRIMY_TORSTOL:
            case ItemID.TORSTOL:
            case ItemID.TORSTOL_POTION_UNF:
                return "torstol";
            default:
                return "unknown";
        }
    }

    private Map<String, Object> createDefaultSeedData(String seedName) {
        Map<String, Object> data = new LinkedHashMap<>();
        switch (seedName) {
            case "guam":
                data.put("seedRate", 0.0148809523809524);
                data.put("xp", 25.0);
                break;
            case "harralander":
                data.put("seedRate", 0.004854368932);
                data.put("xp", 67.5);
                break;
            case "ranarr":
                data.put("seedRate", 0.0037209302325581);
                data.put("xp", 87.5);
                break;
            case "toadflax":
                data.put("seedRate", 0.002257336343);
                data.put("xp", 180.0);
                break;
            case "irit":
                data.put("seedRate", 0.00153609831);
                data.put("xp", 100.0);
                break;
            case "avantoe":
                data.put("seedRate", 0.001055966209);
                data.put("xp", 112.5);
                break;
            case "kwuarm":
                data.put("seedRate", 0.0007199424046);
                data.put("xp", 125.0);
                break;
            case "snapdragon":
                data.put("seedRate", 0.0005392579810181191);
                data.put("xp", 142.5);
                break;
            case "cadantine":
                data.put("seedRate", 0.0003360215054);
                data.put("xp", 150.0);
                break;
            case "lantadyme":
                data.put("seedRate", 0.0002399808015);
                data.put("xp", 172.5);
                break;
            case "torstol":
                data.put("seedRate", 0.0001078518288);
                data.put("xp", 175.0);
                break;
            default:
                data.put("seedRate", 0.0);
                data.put("xp", 0.0);
                break;
        }
        data.put("currentSeeds", 0);
        data.put("grimyHerb", 0);
        data.put("cleanHerb", 0);
        data.put("unfPot", 0);
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