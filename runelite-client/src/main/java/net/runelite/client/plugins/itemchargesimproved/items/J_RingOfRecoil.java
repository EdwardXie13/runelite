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
import net.runelite.client.plugins.itemchargesimproved.item.triggers.*;
import net.runelite.client.plugins.itemchargesimproved.store.HitsplatTarget;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class J_RingOfRecoil extends ChargedItem {
    public J_RingOfRecoil(
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
        super(ChargesImprovedConfig.ring_of_recoil, ItemID.RING_OF_RECOIL, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.RING_OF_RECOIL).needsToBeEquipped(),
        };

        this.triggers = new TriggerBase[]{
            // Check.
            new OnChatMessage("You can inflict one more point of damage before a ring will shatter.").setFixedCharges(1),

            // Check.
            new OnChatMessage("You can inflict (?<charges>.+) more points of damage before a ring will shatter.").setDynamicallyCharges(),

            // Trying to break when full.
            new OnChatMessage("The ring is fully charged. There would be no point in breaking it.").onItemClick().setFixedCharges(40),

            // Shattered.
            new OnChatMessage("Your Ring of Recoil has shattered.").notification().setFixedCharges(40),

            // Take damage.
            new OnHitsplatApplied(HitsplatTarget.SELF).moreThanZeroDamage().isEquipped().decreaseCharges(1),

            // Check from break dialog.
            new OnWidgetLoaded(219, 1, 0).text("Status: (?<charges>.+) damage points? left.").setDynamically(),

            // Break.
            new OnChatMessage("The ring shatters. Your next ring of recoil will start afresh from (?<charges>.+) damage points?.").setDynamicallyCharges(),
        };
    }
}
