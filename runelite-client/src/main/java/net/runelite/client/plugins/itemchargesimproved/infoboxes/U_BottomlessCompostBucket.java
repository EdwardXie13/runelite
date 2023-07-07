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

import javax.annotation.Nullable;

public class U_BottomlessCompostBucket extends ChargedItemInfoBox {
    @Nullable private String tooltip_extra;

    public U_BottomlessCompostBucket(
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
        super(ChargesItem.BOTTOMLESS_COMPOST_BUCKET, ItemID.BOTTOMLESS_COMPOST_BUCKET_22997, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.bottomless_compost_bucket;
        this.extra_config_keys = new String[]{"type"};
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.BOTTOMLESS_COMPOST_BUCKET).fixedCharges(0),
            new TriggerItem(ItemID.BOTTOMLESS_COMPOST_BUCKET_22997),
        };
        this.triggers_animations = new TriggerAnimation[]{
            new TriggerAnimation(8197).decreaseCharges(1),
            new TriggerAnimation(832).increaseCharges(2).onMenuOption("Take").unallowedItems(new int[]{ItemID.BUCKET})
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("Your bottomless compost bucket has a single use of (?<type>.+) ?compost remaining.").fixedCharges(1),
            new TriggerChatMessage("Your bottomless compost bucket has (?<charges>.+) uses of (?<type>.+) ?compost remaining."),
            new TriggerChatMessage("Your bottomless compost bucket doesn't currently have any compost in it!(?<type>.*)").fixedCharges(0)
        };
        this.triggers_widgets = new TriggerWidget[]{
            new TriggerWidget("Your bottomless compost bucket is currently holding one use of (?<type>.+?) ?compost.").fixedCharges(1),
            new TriggerWidget("Your bottomless compost bucket is currently holding (?<charges>.+) uses of (?<type>.+?) ?compost."),
            new TriggerWidget("You discard the contents of your bottomless compost bucket.(?<type>.*)").fixedCharges(0),
            new TriggerWidget("You fill your bottomless compost bucket with .* buckets? of (?<type>.+?) ?compost. Your bottomless compost bucket now contains a total of (?<charges>.+) uses.")
        };
    }

    @Override
    public String getTooltip() {
        return super.getTooltip() + tooltip_extra;
    }
    
    private String getCompostType() {
        return configs.getConfiguration(ChargesImprovedConfig.group, ChargesImprovedConfig.bottomless_compost_bucket_type);
    }

    @Override
    protected void onChargesUpdated() {
        super.onChargesUpdated();

        final @Nullable String type = getCompostType();
        this.tooltip_extra = super.getCharges() == 0 ? " (empty)" : super.getCharges() > 0 && type != null && type.length() > 0 ? " (" + type + ")" : " (unknown)";
    }
}
