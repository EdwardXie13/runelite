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

import static net.runelite.client.plugins.itemchargesimproved.store.HitsplatTarget.ENEMY;

public class W_CrystalHalberd extends ChargedItem {
    public W_CrystalHalberd(
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
        super(ChargesImprovedConfig.crystal_halberd, ItemID.CRYSTAL_HALBERD, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, store, gson);
        this.items = new TriggerItem[]{
            new TriggerItem(ItemID.CRYSTAL_HALBERD),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_24125),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_110),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_110_I),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_210),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_210_I),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_310),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_310_I),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_410),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_410_I),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_510),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_510_I),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_610),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_610_I),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_710),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_710_I),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_810),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_810_I),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_910),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_910_I),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_FULL),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_FULL_I),
            new TriggerItem(ItemID.CRYSTAL_HALBERD_INACTIVE).fixedCharges(0),
            new TriggerItem(ItemID.NEW_CRYSTAL_HALBERD_FULL),
            new TriggerItem(ItemID.NEW_CRYSTAL_HALBERD_FULL_I),
            new TriggerItem(ItemID.NEW_CRYSTAL_HALBERD_FULL_16893),
            new TriggerItem(ItemID.NEW_CRYSTAL_HALBERD_FULL_I_16892),
        };
        this.triggers = new TriggerBase[]{
            new OnChatMessage("Your crystal halberd has (?<charges>.+) charges? remaining.").setDynamicallyCharges(),
            new OnHitsplatApplied(ENEMY).isEquipped().decreaseCharges(1),
        };
    }
}
