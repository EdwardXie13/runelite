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
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class U_SoulBearer extends ChargedItem {
    public U_SoulBearer(
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
        super(ChargesImprovedConfig.soul_bearer, ItemID.SOUL_BEARER, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.SOUL_BEARER),
            new TriggerItem(ItemID.DAMAGED_SOUL_BEARER).fixedCharges(0),
        };

        this.triggers = new TriggerBase[] {
            new OnChatMessage("You remove the runes from the soul bearer.").setFixedCharges(0),
            new OnChatMessage("(The|Your) soul bearer( now)? has one charge.").setFixedCharges(1),
            new OnChatMessage("Your soul bearer carries the ensouled heads to your bank. It has run out of charges.").notification().setFixedCharges(0),
            new OnChatMessage("The soul bearer has (?<charges>.+) charges?.").setDynamicallyCharges(),
            new OnChatMessage("You add .+ charges? to your soul bearer. It now has (?<charges>.+) charges?.").setDynamicallyCharges(),
            new OnChatMessage("Your soul bearer carries the ensouled heads to your bank. It has (?<charges>.+) charges? left.").setDynamicallyCharges()
        };
    }
}
