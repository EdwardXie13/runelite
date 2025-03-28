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
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItem;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.Charges;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class U_TeleportCrystal extends ChargedItem {
    public U_TeleportCrystal(
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
        super(ChargesImprovedConfig.teleport_crystal, ItemID.TELEPORT_CRYSTAL, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.TELEPORT_CRYSTAL).fixedCharges(0),
            new TriggerItem(ItemID.TELEPORT_CRYSTAL_1).fixedCharges(1),
            new TriggerItem(ItemID.TELEPORT_CRYSTAL_2).fixedCharges(2),
            new TriggerItem(ItemID.TELEPORT_CRYSTAL_3).fixedCharges(3),
            new TriggerItem(ItemID.TELEPORT_CRYSTAL_4).fixedCharges(4),
            new TriggerItem(ItemID.TELEPORT_CRYSTAL_5).fixedCharges(5),
            new TriggerItem(ItemID.ETERNAL_TELEPORT_CRYSTAL).fixedCharges(Charges.UNLIMITED),
        };
    }
}
