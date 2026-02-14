package net.runelite.client.plugins.itemchargesimproved.item.listeners;

import net.runelite.api.Client;
import net.runelite.client.Notifier;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItemBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnResetDaily;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;

public class ListenerOnResetDaily extends ListenerBase {
    public ListenerOnResetDaily(final Client client, final ItemManager itemManager, final ChargedItemBase chargedItem, final Notifier notifier, final ChargesImprovedConfig config) {
        super(client, itemManager, chargedItem, notifier, config);
    }

    public boolean trigger() {
        for (final TriggerBase triggerBase : chargedItem.triggers) {
            if (!isValidTrigger(triggerBase)) continue;
            final OnResetDaily trigger = (OnResetDaily) triggerBase;
            boolean triggerUsed = false;

            if (super.trigger(trigger)) {
                triggerUsed = true;
            }

            if (triggerUsed) return true;
        }

        return false;
    }

    public boolean isValidTrigger(final TriggerBase triggerBase) {
        if (!(triggerBase instanceof OnResetDaily)) return false;
        final OnResetDaily trigger = (OnResetDaily) triggerBase;

        if (trigger.resetSpecificItem.isPresent() && !chargedItem.store.itemInPossession(trigger.resetSpecificItem.get())) {
            return false;
        }

        return super.isValidTrigger(trigger);
    }
}
