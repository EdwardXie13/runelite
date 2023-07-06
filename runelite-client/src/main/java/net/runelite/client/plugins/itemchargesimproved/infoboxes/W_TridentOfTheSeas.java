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

public class W_TridentOfTheSeas extends ChargedItemInfoBox {
    public W_TridentOfTheSeas(
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
        super(ChargesItem.TRIDENT_OF_THE_SEAS, ItemID.TRIDENT_OF_THE_SEAS, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.trident_of_the_seas;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.UNCHARGED_TRIDENT).fixedCharges(0),
            new TriggerItem(ItemID.UNCHARGED_TRIDENT_E).fixedCharges(0),
            new TriggerItem(ItemID.UNCHARGED_TOXIC_TRIDENT).fixedCharges(0),
            new TriggerItem(ItemID.UNCHARGED_TOXIC_TRIDENT_E).fixedCharges(0),
            new TriggerItem(ItemID.TRIDENT_OF_THE_SEAS),
            new TriggerItem(ItemID.TRIDENT_OF_THE_SEAS_FULL).fixedCharges(2500),
            new TriggerItem(ItemID.TRIDENT_OF_THE_SWAMP),
            new TriggerItem(ItemID.TRIDENT_OF_THE_SWAMP_E),
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("Your Trident of the (seas|swamp)( \\(e\\))? has one charge.").fixedCharges(1),
            new TriggerChatMessage("Your Trident of the (seas|swamp)( \\((full|e)\\))?( only)? has (?<charges>.+) charges( left)?."),
        };
        this.triggers_animations = new TriggerAnimation[]{
            new TriggerAnimation(1167).decreaseCharges(1).equipped()
        };
        this.triggers_widgets = new TriggerWidget[]{
            new TriggerWidget("You add .* charges? to the Trident of the (seas|swamp)( \\((full|e)\\))?. New total: (?<charges>.+)")
        };
    }
}
