package net.runelite.client.plugins.itemchargesimproved.item.listeners;

import net.runelite.api.Client;
import net.runelite.api.events.GraphicChanged;
import net.runelite.client.Notifier;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItemBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnGraphicChanged;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;

public class ListenerOnGraphicChanged extends ListenerBase {
    public ListenerOnGraphicChanged(final Client client, final ItemManager itemManager, final ChargedItemBase chargedItem, final Notifier notifier, final ChargesImprovedConfig config) {
        super(client, itemManager, chargedItem, notifier, config);
    }

    public void trigger(final GraphicChanged event) {
        for (final TriggerBase triggerBase : chargedItem.triggers) {
            if (!isValidTrigger(triggerBase, event)) continue;
            final OnGraphicChanged trigger = (OnGraphicChanged) triggerBase;
            boolean triggerUsed = false;

            if (super.trigger(trigger)) {
                triggerUsed = true;
            }

            if (triggerUsed) return;
        }
    }

    public boolean isValidTrigger(final TriggerBase triggerBase, final GraphicChanged event) {
        if (!(triggerBase instanceof OnGraphicChanged)) return false;
        final OnGraphicChanged trigger = (OnGraphicChanged) triggerBase;

        // Player check.
        if (event.getActor() != client.getLocalPlayer()) {
            return false;
        }

        // Graphic id check.
        graphicIdCheck: if (trigger.graphicId != null) {
            for (final int graphicId : trigger.graphicId) {
                if (event.getActor().hasSpotAnim(graphicId)) {
                    break graphicIdCheck;
                }
            }

            return false;
        }

        return super.isValidTrigger(trigger);
    }
}
