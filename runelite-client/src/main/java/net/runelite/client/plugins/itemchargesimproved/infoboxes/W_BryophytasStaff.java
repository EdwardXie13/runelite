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
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerGraphic;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerWidget;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class W_BryophytasStaff extends ChargedItemInfoBox {
    public W_BryophytasStaff(
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
        super(ChargesItem.BRYOPHYTAS_STAFF, ItemID.BRYOPHYTAS_STAFF, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.bryophytas_staff;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.BRYOPHYTAS_STAFF_UNCHARGED).fixedCharges(0),
            new TriggerItem(ItemID.BRYOPHYTAS_STAFF)
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("The nature staff has (?<charges>.+) charges?."),
            new TriggerChatMessage("Your staff saved you a nature rune.").increaseCharges(1),
        };
        this.triggers_widgets = new TriggerWidget[]{
            new TriggerWidget("Your Bryophyta's staff now has (?<charges>.+) charges?.")
        };
        this.triggers_graphics = new TriggerGraphic[]{
            new TriggerGraphic(112).decreaseCharges(1).equipped(),
            new TriggerGraphic(113).decreaseCharges(1).equipped(),
        };
    }
}
