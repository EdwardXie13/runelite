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

public class A_CrystalBody extends ChargedItem {
    public A_CrystalBody(
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
        super(ChargesImprovedConfig.crystal_body, ItemID.CRYSTAL_BODY, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);

        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.CRYSTAL_BODY),
            new TriggerItem(ItemID.CRYSTAL_BODY_27697),
            new TriggerItem(ItemID.CRYSTAL_BODY_27709),
            new TriggerItem(ItemID.CRYSTAL_BODY_27721),
            new TriggerItem(ItemID.CRYSTAL_BODY_27733),
            new TriggerItem(ItemID.CRYSTAL_BODY_27745),
            new TriggerItem(ItemID.CRYSTAL_BODY_27757),
            new TriggerItem(ItemID.CRYSTAL_BODY_27769),
            new TriggerItem(ItemID.CRYSTAL_BODY_INACTIVE).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_BODY_INACTIVE_27699).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_BODY_INACTIVE_27711).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_BODY_INACTIVE_27723).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_BODY_INACTIVE_27735).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_BODY_INACTIVE_27747).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_BODY_INACTIVE_27759).fixedCharges(0),
            new TriggerItem(ItemID.CRYSTAL_BODY_INACTIVE_27771).fixedCharges(0)
        };

        this.triggers = new TriggerBase[] {
            new OnChatMessage("Your crystal body has (?<charges>.+) charges? remaining").setDynamicallyCharges().onItemClick(),
            new OnHitsplatApplied(SELF).isEquipped().decreaseCharges(1)
        };
    }
}
