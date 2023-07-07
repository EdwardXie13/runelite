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

public class W_PharaohsSceptre extends ChargedItemInfoBox {
    public W_PharaohsSceptre(
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
        super(ChargesItem.PHARAOHS_SCEPTRE, ItemID.PHARAOHS_SCEPTRE, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);
        this.config_key = ChargesImprovedConfig.pharaohs_sceptre;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_9045),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_9046),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_9047),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_9048),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_9049),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_9050),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_9051),

            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_13074),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_13075),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_13076),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_13077),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_13078),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_16176),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_21445),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_21446),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_26948),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_26950),
            new TriggerItem(ItemID.PHARAOHS_SCEPTRE_UNCHARGED).fixedCharges(0),
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("Your sceptre has (?<charges>.+) charges? left.").onItemClick()
        };
        this.triggers_widgets = new TriggerWidget[]{
            new TriggerWidget("Right, .+ artefacts gives you (?<charges>.+) charges. Now be on your way.").customWidget(TriggerWidget.WIDGET_NPC_GROUP_ID, TriggerWidget.WIDGET_NPC_CHILD_ID),
            new TriggerWidget("Right, you already had (?<charges>.+) charges?, and I don't give discounts. That means .+ artefacts gives you .+ charges?. Now be on your way.").customWidget(TriggerWidget.WIDGET_NPC_GROUP_ID, TriggerWidget.WIDGET_NPC_CHILD_ID),
            new TriggerWidget("Right, you already had .+ charges?, and I don't give discounts. That means .+ artefacts gives you (?<charges>.+) charges?. Now be on your way.").increaseDynamically().customWidget(TriggerWidget.WIDGET_NPC_GROUP_ID, TriggerWidget.WIDGET_NPC_CHILD_ID)
        };
        this.triggers_animations = new TriggerAnimation[]{
            new TriggerAnimation(2881).decreaseCharges(1)
        };
    }
}
