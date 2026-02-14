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
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItemWithStatus;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnChatMessage;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnMenuEntryAdded;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnXpDrop;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

public class U_BoneCrusher extends ChargedItemWithStatus {
    public U_BoneCrusher(
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
        super(ChargesImprovedConfig.bonecrusher, ItemID.BONECRUSHER, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.BONECRUSHER),
            new TriggerItem(ItemID.BONECRUSHER_NECKLACE)
        };

        this.triggers = new TriggerBase[] {
            // Check.
            new OnChatMessage("The bonecrusher( necklace)? has no charges.").setFixedCharges(0),
            new OnChatMessage("The bonecrusher( necklace)? has one charge.").setFixedCharges(1),
            new OnChatMessage("(The|Your) bonecrusher( necklace)? has (?<charges>.+) charges?( left)?. It is active").setDynamicallyCharges().activate(),
            new OnChatMessage("(The|Your) bonecrusher( necklace)? has (?<charges>.+) charges?( left)?. It has been deactivated").setDynamicallyCharges().deactivate(),
            new OnChatMessage("(The|Your) bonecrusher( necklace)? has (?<charges>.+) charges?( left)?.").setDynamicallyCharges(),
            // Uncharge.
            new OnChatMessage("You remove all the charges from the bonecrusher( necklace)?.").setFixedCharges(0),
            // Ran out.
            new OnChatMessage("Your bonecrusher( necklace)? has run out of charges.").notification().setFixedCharges(0),
            // Activate.
            new OnChatMessage("The bonecrusher( necklace)? has been deactivated").deactivate(),
            // Deactivate.
            new OnChatMessage("The bonecrusher( necklace)? is active").activate(),
            // Automatic bury.
            new OnXpDrop(Skill.PRAYER).isActivated().decreaseCharges(1),
            // Hide destroy.
            new OnMenuEntryAdded("Destroy").hide(),
        };
    }
}
