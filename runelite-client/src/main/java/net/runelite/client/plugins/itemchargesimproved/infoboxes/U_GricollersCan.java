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
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class U_GricollersCan extends ChargedItemInfoBox {
    public U_GricollersCan(
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
        super(ChargesItem.GRICOLLERS_CAN, ItemID.GRICOLLERS_CAN, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.gricollers_can;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.GRICOLLERS_CAN),
        };
        this.triggers_animations = new TriggerAnimation[]{
            new TriggerAnimation(2293).decreaseCharges(1).unallowedItems(new int[]{
                ItemID.WATERING_CAN1, ItemID.WATERING_CAN2,
                ItemID.WATERING_CAN3, ItemID.WATERING_CAN4,
                ItemID.WATERING_CAN5, ItemID.WATERING_CAN6,
                ItemID.WATERING_CAN6, ItemID.WATERING_CAN7,
                ItemID.WATERING_CAN8
            }),
            new TriggerAnimation(2293).decreaseCharges(1).onItemClick()
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("Watering can charges remaining: (?<charges>.+)%").onItemClick(),
            new TriggerChatMessage("You water .*").onItemClick()
        };
    }
}
