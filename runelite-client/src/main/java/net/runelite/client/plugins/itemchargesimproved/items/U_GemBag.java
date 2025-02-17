package net.runelite.client.plugins.itemchargesimproved.items;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItemWithStorage;
import net.runelite.client.plugins.itemchargesimproved.item.storage.StorageItem;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnChatMessage;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnItemContainerChanged;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnItemPickup;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnMenuEntryAdded;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnXpDrop;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

import static net.runelite.client.plugins.itemchargesimproved.store.ItemContainerId.INVENTORY;
import static net.runelite.client.plugins.itemchargesimproved.store.ItemContainerId.BANK;


public class U_GemBag extends ChargedItemWithStorage {
    public U_GemBag(
        final Client client,
        final ClientThread client_thread,
        final ConfigManager configs,
        final ItemManager items,
        final InfoBoxManager infoboxes,
        final ChatMessageManager chat_messages,
        final Notifier notifier,
        final ChargesImprovedConfig config,
        final Store store,
        final Gson gson
    ) {
        super(ChargesImprovedConfig.gem_bag, ItemID.GEM_BAG_12020, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);
        storage.setMaximumIndividualQuantity(60).storeableItems(
            new StorageItem(ItemID.UNCUT_SAPPHIRE).checkName("Sapphire").specificOrder(1),
            new StorageItem(ItemID.UNCUT_EMERALD).checkName("Emerald").specificOrder(2),
            new StorageItem(ItemID.UNCUT_RUBY).checkName("Ruby").specificOrder(3),
            new StorageItem(ItemID.UNCUT_DIAMOND).checkName("Diamond").specificOrder(4),
            new StorageItem(ItemID.UNCUT_DRAGONSTONE).checkName("Dragonstone").specificOrder(5)
        );

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.GEM_BAG_12020),
            new TriggerItem(ItemID.OPEN_GEM_BAG),
        };

        this.triggers = new TriggerBase[]{
            // Empty to bank or inventory.
            new OnChatMessage("The gem bag is( now)? empty.").emptyStorage(),

            // Empty and Check.
            new OnChatMessage("(Left in bag: )?Sapphires: (?<sapphires>.+) / Emeralds: (?<emeralds>.+) / Rubies: (?<rubies>.+) Diamonds: (?<diamonds>.+) / Dragonstones: (?<dragonstones>.+)").matcherConsumer(m -> {
                storage.put(ItemID.UNCUT_SAPPHIRE, Integer.parseInt(m.group("sapphires")));
                storage.put(ItemID.UNCUT_EMERALD, Integer.parseInt(m.group("emeralds")));
                storage.put(ItemID.UNCUT_RUBY, Integer.parseInt(m.group("rubies")));
                storage.put(ItemID.UNCUT_DIAMOND, Integer.parseInt(m.group("diamonds")));
                storage.put(ItemID.UNCUT_DRAGONSTONE, Integer.parseInt(m.group("dragonstones")));
            }),

            // Mining regular or gem rocks.
            new OnChatMessage("You just (found|mined) (a|an) (?<gem>.+)!").matcherConsumer(m -> {
                storage.add(getStorageItemFromName(m.group("gem")), 1);
            }).requiredItem(ItemID.OPEN_GEM_BAG),

            // Pickpocketing.
            new OnChatMessage("The following stolen loot gets added to your gem bag: Uncut (?<gem>.+) x (?<quantity>.+).").matcherConsumer(m -> {
                storage.add(getStorageItemFromName(m.group("gem")), Integer.parseInt(m.group("quantity")));
            }),

            // Stealing from stalls.
            new OnChatMessage("You steal an uncut (?<gem>.+) and add it to your gem bag.").matcherConsumer(m -> {
                storage.add(getStorageItemFromName(m.group("gem")), 1);
            }),

            // Fill from inventory.
            new OnItemContainerChanged(INVENTORY).fillStorageFromInventory().onMenuOption("Fill"),

            // Empty to bank.
            new OnItemContainerChanged(BANK).emptyStorageToBank().onMenuOption("Empty"),

            // Use gem on bag
            new OnItemContainerChanged(INVENTORY).fillStorageFromInventory().onUseChargedItemOnStorageItem(storage.getStoreableItems()),
            new OnItemContainerChanged(INVENTORY).fillStorageFromInventory().onUseStorageItemOnChargedItem(storage.getStoreableItems()),

            // Pick up.
            new OnItemPickup(storage.getStoreableItems()).isByOne().requiredItem(ItemID.OPEN_GEM_BAG).pickUpToStorage(),

            // Telegrab.
            new OnXpDrop(Skill.MAGIC).requiredItem(ItemID.OPEN_GEM_BAG).onMenuOption("Cast").onMenuTarget(
                "Uncut sapphire"
            ).addToStorage(ItemID.UNCUT_SAPPHIRE, 1),
            new OnXpDrop(Skill.MAGIC).requiredItem(ItemID.OPEN_GEM_BAG).onMenuOption("Cast").onMenuTarget(
                "Uncut emerald"
            ).addToStorage(ItemID.UNCUT_EMERALD, 1),
            new OnXpDrop(Skill.MAGIC).requiredItem(ItemID.OPEN_GEM_BAG).onMenuOption("Cast").onMenuTarget(
                "Uncut ruby"
            ).addToStorage(ItemID.UNCUT_RUBY, 1),
            new OnXpDrop(Skill.MAGIC).requiredItem(ItemID.OPEN_GEM_BAG).onMenuOption("Cast").onMenuTarget(
                "Uncut diamond"
            ).addToStorage(ItemID.UNCUT_DIAMOND, 1),
            new OnXpDrop(Skill.MAGIC).requiredItem(ItemID.OPEN_GEM_BAG).onMenuOption("Cast").onMenuTarget(
                "Uncut dragonstone"
            ).addToStorage(ItemID.UNCUT_DRAGONSTONE, 1),

            // Hide destroy.
            new OnMenuEntryAdded("Destroy").hide(),
        };
    }
}
