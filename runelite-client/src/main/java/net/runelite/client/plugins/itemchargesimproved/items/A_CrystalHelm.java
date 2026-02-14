package net.runelite.client.plugins.itemchargesimproved.items;

import com.google.gson.Gson;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItem;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnChatMessage;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnHitsplatApplied;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.Store;

import static net.runelite.client.plugins.itemchargesimproved.store.HitsplatTarget.SELF;

public class A_CrystalHelm extends ChargedItem {
    public A_CrystalHelm(
        final Client client,
        final ClientThread client_thread,
        final ConfigManager configs,
        final ItemManager items,
        final InfoBoxManager infoboxes,
        final ChatMessageManager chat_messages,
        final Notifier notifier,
        final ChargesImprovedConfig config,
        final Store store,
        final Gson gson
    ) {
        super(ChargesImprovedConfig.crystal_helm, ItemID.CRYSTAL_HELM, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.CRYSTAL_HELM),
            new TriggerItem(ItemID.CRYSTAL_HELM_27705),
            new TriggerItem(ItemID.CRYSTAL_HELM_27717),
            new TriggerItem(ItemID.CRYSTAL_HELM_27729),
            new TriggerItem(ItemID.CRYSTAL_HELM_27741),
            new TriggerItem(ItemID.CRYSTAL_HELM_27753),
            new TriggerItem(ItemID.CRYSTAL_HELM_27765),
            new TriggerItem(ItemID.CRYSTAL_HELM_27777),
            new TriggerItem(ItemID.CRYSTAL_HELM_INACTIVE).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_HELM_INACTIVE_27707).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_HELM_INACTIVE_27719).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_HELM_INACTIVE_27731).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_HELM_INACTIVE_27743).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_HELM_INACTIVE_27755).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_HELM_INACTIVE_27767).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_HELM_INACTIVE_27779).fixedCharges(0)
        };

        this.triggers = new TriggerBase[]{
            new OnChatMessage("Your crystal helm has (?<charges>.+) charges? remaining").setDynamicallyCharges().onItemClick(),
            new OnHitsplatApplied(SELF).isEquipped().decreaseCharges(1)
        };
    }
}
