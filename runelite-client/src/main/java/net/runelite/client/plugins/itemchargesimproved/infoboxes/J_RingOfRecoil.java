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
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerHitsplat;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerWidget;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public class J_RingOfRecoil extends ChargedItemInfoBox {
    public J_RingOfRecoil(
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
        super(ChargesItem.RING_OF_RECOIL, ItemID.RING_OF_RECOIL, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.ring_of_recoil;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.RING_OF_RECOIL),
        };
        this.triggers_widgets = new TriggerWidget[]{
            new TriggerWidget("The ring is fully charged. There would be no point in breaking it.").fixedCharges(40)
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("You can inflict (?<charges>.+) more points? of damage before a ring will shatter"),
            new TriggerChatMessage("Your Ring of Recoil has shattered.").fixedCharges(40)
        };
        this.triggers_hitsplats = new TriggerHitsplat[]{
            new TriggerHitsplat(1).equipped().onSelf()
        };
    }
}
