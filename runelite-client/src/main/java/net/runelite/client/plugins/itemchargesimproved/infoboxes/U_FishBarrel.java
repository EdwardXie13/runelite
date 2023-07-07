package net.runelite.client.plugins.itemchargesimproved.infoboxes;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
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
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerItemContainer;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerWidget;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class U_FishBarrel extends ChargedItemInfoBox {
    private final int FISH_BARREL_SIZE = 28;

    public U_FishBarrel(
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
        super(ChargesItem.FISH_BARREL, ItemID.FISH_BARREL, client, client_thread, configs, items, infoboxes, chat_messages, notifier, config, plugin);

        this.config_key = ChargesImprovedConfig.fish_barrel;
        this.zero_charges_is_positive = true;
        this.triggers_items = new TriggerItem[]{
            new TriggerItem(ItemID.FISH_BARREL),
            new TriggerItem(ItemID.OPEN_FISH_BARREL)
        };
        this.triggers_item_containers = new TriggerItemContainer[]{
            new TriggerItemContainer(InventoryID.BANK.getId()).menuTarget("Open fish barrel").menuOption("Empty").fixedCharges(0),
            new TriggerItemContainer(InventoryID.BANK.getId()).menuTarget("Fish barrel").menuOption("Empty").fixedCharges(0),
            new TriggerItemContainer(InventoryID.INVENTORY.getId()).menuTarget("Open fish barrel").menuOption("Fill").increaseByDifference(),
            new TriggerItemContainer(InventoryID.INVENTORY.getId()).menuTarget("Fish barrel").menuOption("Fill").increaseByDifference(),
        };
        this.triggers_chat_messages = new TriggerChatMessage[]{
            new TriggerChatMessage("Your barrel is empty.").onItemClick().fixedCharges(0),
            new TriggerChatMessage("The barrel is full. It may be emptied at a bank.").onItemClick().fixedCharges(FISH_BARREL_SIZE),
            new TriggerChatMessage("(You catch .*)|(.* enabled you to catch an extra fish.)").extraConsumer(message -> {
                if (item_id == ItemID.OPEN_FISH_BARREL && getCharges() < FISH_BARREL_SIZE) {
                    increaseCharges(1);
                }
            })
        };
        this.triggers_widgets = new TriggerWidget[]{
            new TriggerWidget("The barrel is empty").fixedCharges(0),
            new TriggerWidget("The barrel contains:").extraConsumer(message -> {
                int charges = 0;

                final Matcher matcher = Pattern.compile(".*?(\\d+)").matcher(message);
                while (matcher.find()) {
                    charges += Integer.parseInt(matcher.group(1));
                }

                setCharges(charges);
            })
        };
    }
}
