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

public class S_KharedstMemoirs extends ChargedItemInfoBox {
    public S_KharedstMemoirs(
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
        super(ChargesItem.KHAREDSTS_MEMOIRS, ItemID.KHAREDSTS_MEMOIRS, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.kharedsts_memoirs;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.KHAREDSTS_MEMOIRS),
            new TriggerItem(ItemID.BOOK_OF_THE_DEAD)
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("(Kharedst's Memoirs?)|(The Book of the Dead) now has (?<charges>.+) (memories|memory) remaining."),
            new TriggerChatMessage("(Kharedst's Memoirs?)|(The Book of the Dead) holds no charges?.").fixedCharges(0)
        };
        this.triggers_widgets = new TriggerWidget[]{
            new TriggerWidget("On the inside of the cover a message is displayed in dark ink. It reads: (?<charges>.+) memories? remain."),
            new TriggerWidget("(Kharedst's Memoirs?)|(The Book of the Dead) now has (?<charges>.+) charges.")
        };
    }
}
