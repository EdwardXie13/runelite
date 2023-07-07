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

public class W_SkullSceptre extends ChargedItemInfoBox {
    public W_SkullSceptre(
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
        super(ChargesItem.SKULL_SCEPTRE, ItemID.SKULL_SCEPTRE, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.skull_sceptre;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.SKULL_SCEPTRE),
            new TriggerItem(ItemID.SKULL_SCEPTRE_I)
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("Your Skull Sceptre has (?<charges>.+) charges? left."),
        };
        this.triggers_widgets = new TriggerWidget[]{
            new TriggerWidget("Concentrating deeply, you divine that the sceptre has (?<charges>.+) charges? left."),
            new TriggerWidget("You charge the Skull Sceptre with .+ fragments?. It now contains( the maximum number of charges,)? (?<charges>.+?)( charges?)?\\.")
        };
    }
}
