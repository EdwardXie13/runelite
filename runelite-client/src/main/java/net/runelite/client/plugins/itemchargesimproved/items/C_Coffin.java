package net.runelite.client.plugins.itemchargesimproved.items;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
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
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.ItemContainerId;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class C_Coffin extends ChargedItemWithStorage {
    public C_Coffin(
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
        super(ChargesImprovedConfig.coffin, ItemID.GOLD_COFFIN, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);
        this.storage = storage.storeableItems(
            new StorageItem(ItemID.LOAR_REMAINS).checkName("Loar"),
            new StorageItem(ItemID.PHRIN_REMAINS).checkName("Phrin"),
            new StorageItem(ItemID.RIYL_REMAINS).checkName("Riyl"),
            new StorageItem(ItemID.ASYN_REMAINS).checkName("Asyn"),
            new StorageItem(ItemID.FIYR_REMAINS).checkName("Fiyr"),
            new StorageItem(ItemID.URIUM_REMAINS).checkName("Urium")
        );

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.BROKEN_COFFIN).fixedCharges(0),
            new TriggerItem(ItemID.BRONZE_COFFIN).maxCharges(3),
            new TriggerItem(ItemID.OPEN_BRONZE_COFFIN).maxCharges(3),
            new TriggerItem(ItemID.STEEL_COFFIN).maxCharges(8),
            new TriggerItem(ItemID.OPEN_STEEL_COFFIN).maxCharges(8),
            new TriggerItem(ItemID.BLACK_COFFIN).maxCharges(14),
            new TriggerItem(ItemID.OPEN_BLACK_COFFIN).maxCharges(14),
            new TriggerItem(ItemID.SILVER_COFFIN).maxCharges(20),
            new TriggerItem(ItemID.OPEN_SILVER_COFFIN).maxCharges(20),
            new TriggerItem(ItemID.GOLD_COFFIN).maxCharges(28),
            new TriggerItem(ItemID.OPEN_GOLD_COFFIN).maxCharges(28),
        };

        this.triggers = new TriggerBase[] {
            // Add remains to coffin.
            new OnChatMessage("You put the (?<remains>.+) remains into your open coffin.").matcherConsumer(m -> {
                storage.add(getStorageItemFromName(m.group("remains")), 1);
            }),

            // Check.
            new OnChatMessage("Loar (?<loar>.+) / Phrin (?<phrin>.+) / Riyl (?<riyl>.+) / Asyn (?<asyn>.+) / Fiyr (?<fiyr>.+) / Urium (?<urium>.+)").matcherConsumer(m -> {
                storage.clear();
                storage.put(ItemID.LOAR_REMAINS, Integer.parseInt(m.group("loar")));
                storage.put(ItemID.PHRIN_REMAINS, Integer.parseInt(m.group("phrin")));
                storage.put(ItemID.RIYL_REMAINS, Integer.parseInt(m.group("riyl")));
                storage.put(ItemID.ASYN_REMAINS, Integer.parseInt(m.group("asyn")));
                storage.put(ItemID.FIYR_REMAINS, Integer.parseInt(m.group("fiyr")));
                storage.put(ItemID.URIUM_REMAINS, Integer.parseInt(m.group("urium")));
            }),

            // Try to empty already empty.
            new OnChatMessage("Your coffin is empty.").onItemClick().emptyStorage(),

            // Fill from inventory.
            new OnItemContainerChanged(ItemContainerId.INVENTORY).fillStorageFromInventory().onMenuOption("Fill"),
        };
    }
}
