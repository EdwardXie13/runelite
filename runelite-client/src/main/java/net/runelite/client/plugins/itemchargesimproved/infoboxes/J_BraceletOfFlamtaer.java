package net.runelite.client.plugins.itemchargesimproved.infoboxes;

import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.itemchargesimproved.ChargedItemInfoBox;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.ChargesItem;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerChatMessage;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerWidget;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class J_BraceletOfFlamtaer extends ChargedItemInfoBox {
    public J_BraceletOfFlamtaer(
        final Client client,
        final ClientThread client_thread,
        final ConfigManager configs,
        final ItemManager items,
        final InfoBoxManager infoboxes,
        final ChatMessageManager chat_messages,
        final Notifier notifier,
        final ChargesImprovedConfig config,
        final Plugin plugin
    ) {
        super(ChargesItem.BRACELET_OF_FLAMTAER, ItemID.FLAMTAER_BRACELET, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.bracelet_of_flamtaer;
        this.needs_to_be_equipped_for_infobox = true;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.FLAMTAER_BRACELET),
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("Your Flamtaer bracelet helps you build the temple quicker. It has (?<charges>.+) charges? left."),
            new TriggerChatMessage("Your flamtaer bracelet has (?<charges>.+) charges? left."),
            new TriggerChatMessage("Your Flamtaer bracelet helps you build the temple quicker. It then crumbles to dust.").fixedCharges(80).notification("Your flamtaer bracelet crumbles to dust."),
        };
        this.triggers_widgets = new TriggerWidget[]{
            new TriggerWidget("The bracelet shatters. Your next Flamtaer bracelet will star afresh from (?<charges>.+) charges.")
        };
    }
}
