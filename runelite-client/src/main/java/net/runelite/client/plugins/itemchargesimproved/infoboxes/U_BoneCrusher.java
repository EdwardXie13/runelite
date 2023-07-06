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

public class U_BoneCrusher extends ChargedItemInfoBox {
    public U_BoneCrusher(
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
        super(ChargesItem.BONE_CRUSHER, ItemID.BONECRUSHER, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.bone_crusher;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.BONECRUSHER),
            new TriggerItem(ItemID.BONECRUSHER_NECKLACE)
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("The bonecrusher( necklace)? has no charges.").fixedCharges(0),
            new TriggerChatMessage("Your bonecrusher( necklace)? has run out of charges.").fixedCharges(0),
            new TriggerChatMessage("The bonecrusher( necklace)? has (?<charges>.+) charges?."),
        };
        this.triggers_widgets = new TriggerWidget[]{
            new TriggerWidget("You remove all the charges from the bonecrusher( necklace)?.").customWidget(11, 2).fixedCharges(0),
            new TriggerWidget("The bonecrusher( necklace)? has (?<charges>.+) charges? left."),
        };
    }
}
