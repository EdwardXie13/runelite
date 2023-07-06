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
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerAnimation;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerChatMessage;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerWidget;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class W_SanguinestiStaff extends ChargedItemInfoBox {
    public W_SanguinestiStaff(
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
        super(ChargesItem.SANGUINESTI_STAFF, ItemID.SANGUINESTI_STAFF, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.sanguinesti_staff;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.SANGUINESTI_STAFF),
            new TriggerItem(ItemID.SANGUINESTI_STAFF_UNCHARGED).fixedCharges(0),
            new TriggerItem(ItemID.HOLY_SANGUINESTI_STAFF),
            new TriggerItem(ItemID.HOLY_SANGUINESTI_STAFF_UNCHARGED).fixedCharges(0),
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            //  new TriggerChatMessage("Your Sanguinesti staff has (?<charges>.+) charges remaining.").onItemClick(),
            //  new TriggerChatMessage("Your (Holy s|S)?anguinesti staff has (?<charges>.+) charges? remaining.").onItemClick(),
            new TriggerChatMessage("You apply an additional .+ charges? to your Sanguinesti staff. It now has (?<charges>.+) charges? in total."),
        };
        this.triggers_widgets = new TriggerWidget[]{
            //  new TriggerWidget("You apply an additional .+ charges? to the Sanguinesti staff. It now has (?<charges>.+) charges? in total."),
            new TriggerWidget("You apply an additional .+ charges? to your Sanguinesti staff. It now has (?<charges>.+) charges in total."),
            new TriggerWidget("You apply (?<charges>.+) charges to your Sanguinesti staff.")
        };
        this.triggers_animations = new TriggerAnimation[]{
            new TriggerAnimation(1167).decreaseCharges(1).equipped()
        };
    }
}
