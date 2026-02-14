package net.runelite.client.plugins.itemchargesimproved.item.listeners;

import net.runelite.api.Client;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItem;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItemBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnVarbitChanged;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;

public class ListenerOnVarbitChanged extends ListenerBase {
    public ListenerOnVarbitChanged(final Client client, final ItemManager itemManager, final ChargedItemBase chargedItem, final Notifier notifier, final ChargesImprovedConfig config) {
        super(client, itemManager, chargedItem, notifier, config);
    }

    public void trigger(final VarbitChanged event) {
        for (final TriggerBase triggerBase : chargedItem.triggers) {
            if (!isValidTrigger(triggerBase, event)) continue;
            final OnVarbitChanged trigger = (OnVarbitChanged) triggerBase;
            boolean triggerUsed = false;

            // Set dynamically.
            if (trigger.setDynamically.isPresent() && chargedItem instanceof ChargedItem) {
                ((ChargedItem) chargedItem).setCharges(event.getValue());
            }

            // Varbit value consumer.
            if (trigger.varbitValueConsumer.isPresent()) {
                trigger.varbitValueConsumer.get().accept(event.getValue());
                triggerUsed = true;
            }

            if (super.trigger(trigger)) {
                triggerUsed = true;
            }

            if (triggerUsed) return;
        }
    }

    public boolean isValidTrigger(final TriggerBase triggerBase, final VarbitChanged event) {
        if (!(triggerBase instanceof OnVarbitChanged)) return false;
        final OnVarbitChanged trigger = (OnVarbitChanged) triggerBase;

        // Varbit id check.
        if (event.getVarbitId() != trigger.varbitId) {
            return false;
        }

        // Varbit value check.
        if (trigger.varbitValue.isPresent() && event.getValue() != trigger.varbitValue.get()) {
            return false;
        }

        return super.isValidTrigger(trigger);
    }
}
