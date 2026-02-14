package net.runelite.client.plugins.itemchargesimproved.item.listeners;

import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.Notifier;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItem;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItemBase;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItemWithStorage;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnItemContainerChanged;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerItem;
import net.runelite.client.plugins.itemchargesimproved.store.AdvancedMenuEntry;

public class ListenerOnItemContainerChanged extends ListenerBase {
    public ListenerOnItemContainerChanged(final Client client, final ItemManager itemManager, final ChargedItemBase chargedItem, final Notifier notifier, final ChargesImprovedConfig config) {
        super(client, itemManager, chargedItem, notifier, config);
    }

    public void trigger(final ItemContainerChanged event) {
        // Get quantity from amount in item container.
        for (final TriggerItem triggerItem : chargedItem.items) {
            if (triggerItem.quantityCharges.isPresent()) {
               for (final Item item : event.getItemContainer().getItems()) {
                    if (item.getId() == triggerItem.itemId) {
                        ((ChargedItem) chargedItem).setCharges(item.getQuantity());
                        break;
                    }
                }
            }
        }

        for (final TriggerBase triggerBase : chargedItem.triggers) {
            if (!isValidTrigger(triggerBase, event)) continue;
            boolean triggerUsed = false;
            final OnItemContainerChanged trigger = (OnItemContainerChanged) triggerBase;

            // Fill storage from inventory all items.
            if (trigger.fillStorageFromInventory.isPresent()) {
                ((ChargedItemWithStorage) chargedItem).storage.fillFromInventory();
                triggerUsed = true;
            }

            // Empty storage to bank.
            if (trigger.emptyStorageToBank.isPresent()) {
                ((ChargedItemWithStorage) chargedItem).storage.emptyToBank();
                triggerUsed = true;
            }

            // Empty storage to inventory.
            if (trigger.emptyStorageToInventory.isPresent()) {
                ((ChargedItemWithStorage) chargedItem).storage.emptyToInventory();
                triggerUsed = true;
            }

            // Update storage directly from item container.
            if (trigger.updateStorage.isPresent()) {
                ((ChargedItemWithStorage) chargedItem).storage.updateFromItemContainer(event.getItemContainer());
                triggerUsed = true;
            }

            if (trigger.onItemContainerDifference.isPresent()) {
                trigger.onItemContainerDifference.get().accept(chargedItem.store.getInventoryItemsDifference());
                triggerUsed = true;
            }

            if (super.trigger(trigger)) {
                triggerUsed = true;
            }

            if (triggerUsed) return;
        }
    }

    public boolean isValidTrigger(final TriggerBase triggerBase, final ItemContainerChanged event) {
        if (!(triggerBase instanceof OnItemContainerChanged)) return false;
        final OnItemContainerChanged trigger = (OnItemContainerChanged) triggerBase;

        // Item container type check.
        final ItemContainer itemContainer = event.getItemContainer();
        if (
            itemContainer == null || itemContainer.getId() != trigger.itemContainerId) {
            return false;
        }

        // Fill storage from inventory check.
        if (trigger.fillStorageFromInventory.isPresent() && !(chargedItem instanceof ChargedItemWithStorage)) {
            return false;
        }

        return super.isValidTrigger(trigger);
    }
}
