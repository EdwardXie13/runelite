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
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerReset;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class S_FaladorShield extends ChargedItemInfoBox {
    public S_FaladorShield(
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
        super(ChargesItem.FALADOR_SHIELD, ItemID.FALADOR_SHIELD, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.falador_shield;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.FALADOR_SHIELD_3),
            new TriggerItem(ItemID.FALADOR_SHIELD_4),
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("You have one remaining charge for today.").onItemClick().fixedCharges(1),
            new TriggerChatMessage("You have already used your charge for today.").onItemClick().fixedCharges(0),
            new TriggerChatMessage("You have already used all available recharges today. Try again tomorrow when the shield has recharged.").onItemClick().fixedCharges(0)
        };
        this.triggers_graphics = new TriggerGraphic[]{
            new TriggerGraphic(321).decreaseCharges(1)
        };
        this.triggers_resets = new TriggerReset[]{
            new TriggerReset(1).dynamicItem(ItemID.FALADOR_SHIELD_3),
            new TriggerReset(2).dynamicItem(ItemID.FALADOR_SHIELD_4)
        };
    }
}
