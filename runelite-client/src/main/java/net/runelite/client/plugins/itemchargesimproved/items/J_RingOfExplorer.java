package net.runelite.client.plugins.itemchargesimproved.items;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Varbits;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItemWithStorageMultipleCharges;
import net.runelite.client.plugins.itemchargesimproved.item.storage.StorageItem;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.*;
import net.runelite.client.plugins.itemchargesimproved.store.Charges;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

class ExplorersRingStorageItemId {
    public static final int TELEPORTS = -1000;
    public static final int ALCHEMY = -1001;
    public static final int ENERGY_RESTORES = -1002;
}

public class J_RingOfExplorer extends ChargedItemWithStorageMultipleCharges {
    public J_RingOfExplorer(
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
        super(ChargesImprovedConfig.explorers_ring, ItemID.EXPLORERS_RING_1, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);
        storage = storage.storeableItems(
            new StorageItem(ExplorersRingStorageItemId.ALCHEMY).displayName("Alchemy charges"),
            new StorageItem(ExplorersRingStorageItemId.TELEPORTS).displayName("Teleports"),
            new StorageItem(ExplorersRingStorageItemId.ENERGY_RESTORES).displayName("Energy restores")
        ).showIndividualCharges();

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.EXPLORERS_RING_1),
            new TriggerItem(ItemID.EXPLORERS_RING_2),
            new TriggerItem(ItemID.EXPLORERS_RING_3),
            new TriggerItem(ItemID.EXPLORERS_RING_4),
        };

        this.triggers = new TriggerBase[]{
            // Use.
            new OnVarbitChanged(Varbits.EXPLORER_RING_ALCHS).consumer(() -> updateStorage()),
            new OnVarbitChanged(Varbits.EXPLORER_RING_RUNENERGY).consumer(() -> updateStorage()),
            new OnVarbitChanged(Varbits.EXPLORER_RING_TELEPORTS).consumer(() -> updateStorage()),

            // Check.
            new OnMenuOptionClicked("Check").onItemClick().consumer(() -> updateStorage()),

            new OnResetDaily().specificItem(ItemID.EXPLORERS_RING_1).consumer(() -> {
                storage.clear();
                storage.put(ExplorersRingStorageItemId.ALCHEMY, 30);
                storage.put(ExplorersRingStorageItemId.ENERGY_RESTORES, 2);
                storage.put(ExplorersRingStorageItemId.TELEPORTS, 0);
            }),

            new OnResetDaily().specificItem(ItemID.EXPLORERS_RING_2).consumer(() -> {
                storage.clear();
                storage.put(ExplorersRingStorageItemId.ALCHEMY, 30);
                storage.put(ExplorersRingStorageItemId.ENERGY_RESTORES, 3);
                storage.put(ExplorersRingStorageItemId.TELEPORTS, 3);
            }),

            new OnResetDaily().specificItem(ItemID.EXPLORERS_RING_3).consumer(() -> {
                storage.clear();
                storage.put(ExplorersRingStorageItemId.ALCHEMY, 30);
                storage.put(ExplorersRingStorageItemId.ENERGY_RESTORES, 4);
                storage.put(ExplorersRingStorageItemId.TELEPORTS, Charges.UNLIMITED);
            }),

            new OnResetDaily().specificItem(ItemID.EXPLORERS_RING_4).consumer(() -> {
                storage.clear();
                storage.put(ExplorersRingStorageItemId.ALCHEMY, 30);
                storage.put(ExplorersRingStorageItemId.ENERGY_RESTORES, 3);
                storage.put(ExplorersRingStorageItemId.TELEPORTS, Charges.UNLIMITED);
            }),
        };
    }

    private void updateStorage() {
        storage.clear();

        // Alchemy.
        storage.put(ExplorersRingStorageItemId.ALCHEMY, 30 - client.getVarbitValue(Varbits.EXPLORER_RING_ALCHS));

        // Energy restores.
        final int energyRestoresUsed = client.getVarbitValue(Varbits.EXPLORER_RING_RUNENERGY);
        switch (itemId) {
            case ItemID.EXPLORERS_RING_1:
                storage.put(ExplorersRingStorageItemId.ENERGY_RESTORES, 2 - energyRestoresUsed);
                break;
            case ItemID.EXPLORERS_RING_2:
                storage.put(ExplorersRingStorageItemId.ENERGY_RESTORES, 3 - energyRestoresUsed);
                break;
            case ItemID.EXPLORERS_RING_3:
                storage.put(ExplorersRingStorageItemId.ENERGY_RESTORES, 4 - energyRestoresUsed);
                break;
            case ItemID.EXPLORERS_RING_4:
                storage.put(ExplorersRingStorageItemId.ENERGY_RESTORES, 3 - energyRestoresUsed);
                break;
        }

        // Teleports.
        final int teleportsUsed = client.getVarbitValue(Varbits.EXPLORER_RING_TELEPORTS);
        switch (itemId) {
            case ItemID.EXPLORERS_RING_1:
                storage.put(ExplorersRingStorageItemId.TELEPORTS, 0);
                break;
            case ItemID.EXPLORERS_RING_2:
                storage.put(ExplorersRingStorageItemId.TELEPORTS, 3 - teleportsUsed);
                break;
            case ItemID.EXPLORERS_RING_3:
            case ItemID.EXPLORERS_RING_4:
                storage.put(ExplorersRingStorageItemId.TELEPORTS, Charges.UNLIMITED);
                break;
        }
    }
}
