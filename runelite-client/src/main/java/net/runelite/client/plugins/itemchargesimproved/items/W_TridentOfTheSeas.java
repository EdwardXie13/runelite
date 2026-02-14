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

public class W_TridentOfTheSeas extends ChargedItem {
    public W_TridentOfTheSeas(
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
        super(ChargesImprovedConfig.trident_of_the_seas, ItemID.TRIDENT_OF_THE_SEAS, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.UNCHARGED_TRIDENT).fixedCharges(0),
            new TriggerItem(ItemID.UNCHARGED_TRIDENT_E).fixedCharges(0),
            new TriggerItem(ItemID.UNCHARGED_TOXIC_TRIDENT).fixedCharges(0),
            new TriggerItem(ItemID.UNCHARGED_TOXIC_TRIDENT_E).fixedCharges(0),
            new TriggerItem(ItemID.TRIDENT_OF_THE_SEAS),
            new TriggerItem(ItemID.TRIDENT_OF_THE_SEAS_FULL).fixedCharges(2500),
            new TriggerItem(ItemID.TRIDENT_OF_THE_SEAS_E),
            new TriggerItem(ItemID.TRIDENT_OF_THE_SWAMP),
            new TriggerItem(ItemID.TRIDENT_OF_THE_SWAMP_E),
        };

        this.triggers = new TriggerBase[] {
            // Ran out of charges.
            new OnChatMessage("Your Trident of the (seas|swamp)( \\(e\\))? has run out of charges.").notification().setFixedCharges(0),

            // Check, one charge left.
            new OnChatMessage("Your Trident of the (seas|swamp)( \\(e\\))? has one charge.").setFixedCharges(1),

            // Check for charges and warning when low.
            new OnChatMessage("Your Trident of the (seas|swamp)( \\([full|e]\\))? (only )?has (?<charges>.+) charges( left)?.").setDynamicallyCharges(),

            // Charge to full.
            new OnChatMessage("You add .* charges? to the Trident of the (seas|swamp)( \\(full\\))?. New total: (?<charges>.+)").setDynamicallyCharges(),

            // Attack.
            new OnGraphicChanged(1251).isEquipped().decreaseCharges(1)
        };
    }
}
