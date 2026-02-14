package net.runelite.client.plugins.itemchargesimproved.item.listeners;

import net.runelite.api.Client;
import net.runelite.client.Notifier;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItemBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnMenuOptionClicked;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;
import net.runelite.client.plugins.itemchargesimproved.store.AdvancedMenuEntry;

public class ListenerOnMenuOptionClicked extends ListenerBase {
    public ListenerOnMenuOptionClicked(final Client client, final ItemManager itemManager, final ChargedItemBase chargedItem, final Notifier notifier, final ChargesImprovedConfig config) {
        super(client, itemManager, chargedItem, notifier, config);
    }

    public void trigger(final AdvancedMenuEntry event) {
        for (final TriggerBase triggerBase : chargedItem.triggers) {
            if (!isValidTrigger(triggerBase, event)) continue;
            final OnMenuOptionClicked trigger = (OnMenuOptionClicked) triggerBase;
            boolean triggerUsed = false;

            if (trigger.menuOptionConsumer.isPresent()) {
                trigger.menuOptionConsumer.get().accept(event);
                triggerUsed = true;
            }

            if (super.trigger(trigger)) {
                triggerUsed = true;
            }

            if (triggerUsed) return;
        }
    }

    public boolean isValidTrigger(final TriggerBase triggerBase, final AdvancedMenuEntry event) {
        if (!(triggerBase instanceof OnMenuOptionClicked)) return false;
        final OnMenuOptionClicked trigger = (OnMenuOptionClicked) triggerBase;

        // Option check.
        if (!event.option.equals(trigger.option)) {
            return false;
        }

        return super.isValidTrigger(trigger);
    }
}
