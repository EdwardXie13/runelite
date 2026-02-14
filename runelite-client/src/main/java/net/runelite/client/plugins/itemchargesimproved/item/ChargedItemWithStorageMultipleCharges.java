package net.runelite.client.plugins.itemchargesimproved.item;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.item.storage.StorageItem;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class ChargedItemWithStorageMultipleCharges extends ChargedItemWithStorage {
    public ChargedItemWithStorageMultipleCharges(String configKey, int itemId, Client client, ClientThread clientThread, ConfigManager configManager, ItemManager itemManager, InfoBoxManager infoBoxManager, ChatMessageManager chatMessageManager, Notifier notifier, ChargesImprovedConfig config, Store store, final Gson gson) {
        super(configKey, itemId, client, clientThread, configManager, itemManager, infoBoxManager, chatMessageManager, notifier, config, store, gson);
    }

    @Override
    public String getCharges() {
        String individualCharges = "";

        int validItems = 0;
        for (final StorageItem storageItem : getStorage().values()) {
            if (storageItem.getQuantity() >= 0) {
                individualCharges += storageItem.getQuantity() + "/";
                validItems++;
            }
        }

        return validItems > 0 ? individualCharges.replaceAll("/$", "") : "?";
    }
}
