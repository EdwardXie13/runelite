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

public class W_Arclight extends ChargedItemInfoBox {
    private final int ARCLIGHT_ATTACK_STAB = 386;
    private final int ARCLIGHT_ATTACK_SLASH = 390;

    public W_Arclight(
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
        super(ChargesItem.ARCLIGHT, ItemID.ARCLIGHT, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.arclight;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.ARCLIGHT),
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("Your arclight has (?<charges>.+) charges? left."),
            new TriggerChatMessage("Your arclight can perform (?<charges>.+) more attacks."),
            new TriggerChatMessage("Your arclight has degraded.").fixedCharges(0)
        };
        this.triggers_hitsplats = new TriggerHitsplat[]{
            new TriggerHitsplat(1).equipped().onEnemy().onAnimations(new int[]{ARCLIGHT_ATTACK_STAB, ARCLIGHT_ATTACK_SLASH})
        };
        this.triggers_widgets = new TriggerWidget[]{
            new TriggerWidget("You combine Darklight with the shards and it seems to glow in your hands creating Arclight. Your Arclight has (?<charges>.+) charges.")
        };
    }
}
