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
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnChatMessage;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnGraphicChanged;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class S_DragonfireShield extends ChargedItem {
    public S_DragonfireShield(
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
        super(ChargesImprovedConfig.dragonfire_shield, ItemID.DRAGONFIRE_SHIELD, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.DRAGONFIRE_SHIELD_11284).fixedCharges(0),
            new TriggerItem(ItemID.DRAGONFIRE_SHIELD),
            new TriggerItem(ItemID.DRAGONFIRE_WARD_22003).fixedCharges(0),
            new TriggerItem(ItemID.DRAGONFIRE_WARD)
        };

        this.triggers = new TriggerBase[]{
            // Check.
            new OnChatMessage("The shield has (?<charges>.+) charges?.").setDynamicallyCharges().onItemClick(),

            // Uncharge.
            new OnChatMessage("You vent the shield's remaining charges harmlessly into the air.").setFixedCharges(0),

            // Charge collected.
            new OnChatMessage("Your dragonfire (shield|ward) glows more brightly.").increaseCharges(1),

            // Already full.
            new OnChatMessage("Your dragonfire shield is already fully charged.").setFixedCharges(50),

            // Attack.
            new OnGraphicChanged(1165).isEquipped().decreaseCharges(1),
        };
    }
}
