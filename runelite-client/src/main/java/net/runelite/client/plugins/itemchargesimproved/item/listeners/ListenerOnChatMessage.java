package net.runelite.client.plugins.itemchargesimproved.item.listeners;

import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.Notifier;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedConfig;
import net.runelite.client.plugins.itemchargesimproved.ChargesImprovedPlugin;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItem;
import net.runelite.client.plugins.itemchargesimproved.item.ChargedItemBase;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.OnChatMessage;
import net.runelite.client.plugins.itemchargesimproved.item.triggers.TriggerBase;

import java.util.regex.Matcher;

import static net.runelite.client.plugins.itemchargesimproved.ChargesImprovedPlugin.getNumberFromCommaString;

public class ListenerOnChatMessage extends ListenerBase {
    public ListenerOnChatMessage(final Client client, final ItemManager itemManager, final ChargedItemBase chargedItem, final Notifier notifier, final ChargesImprovedConfig config) {
        super(client, itemManager, chargedItem, notifier, config);
    }

    public void trigger(final ChatMessage event) {
        for (final TriggerBase triggerBase : chargedItem.triggers) {
            if (!isValidTrigger(triggerBase, event)) continue;
            boolean triggerUsed = false;
            final OnChatMessage trigger = (OnChatMessage) triggerBase;

            final String message = ChargesImprovedPlugin.getCleanChatMessage(event);
            final Matcher matcher = trigger.message.matcher(message);
            matcher.find();

            if (trigger.setDynamically.isPresent() && (chargedItem instanceof ChargedItem)) {
                ((ChargedItem) chargedItem).setCharges(getNumberFromCommaString(matcher.group("charges")));
                triggerUsed = true;
            }

            if (trigger.increaseDynamically.isPresent() && (chargedItem instanceof ChargedItem)) {
                ((ChargedItem) chargedItem).increaseCharges(getNumberFromCommaString(matcher.group("charges")));
                triggerUsed = true;
            }

            if (trigger.decreaseDynamically.isPresent() && (chargedItem instanceof ChargedItem)) {
                ((ChargedItem) chargedItem).decreaseCharges(getNumberFromCommaString(matcher.group("charges")));
                triggerUsed = true;
            }

            if (trigger.useDifference.isPresent() && (chargedItem instanceof ChargedItem)) {
                ((ChargedItem) chargedItem).setCharges(getNumberFromCommaString(matcher.group("total")) - getNumberFromCommaString(matcher.group("used")));
                triggerUsed = true;
            }

            if (trigger.matcherConsumer.isPresent()) {
                trigger.matcherConsumer.get().accept(matcher);
                triggerUsed = true;
            }

            if (trigger.stringConsumer.isPresent()) {
                trigger.stringConsumer.get().accept(message);
                triggerUsed = true;
            }

            if (super.trigger(trigger)) {
                triggerUsed = true;
            }

            if (triggerUsed) return;
        }
    }

    public boolean isValidTrigger(final TriggerBase triggerBase, final ChatMessage event) {
        if (!(triggerBase instanceof OnChatMessage)) return false;
        final OnChatMessage trigger = (OnChatMessage) triggerBase;

        // Message check.
        final String message = ChargesImprovedPlugin.getCleanChatMessage(event);
        final Matcher matcher = trigger.message.matcher(message);
        if (!matcher.find()) {
            return false;
        }

        return super.isValidTrigger(trigger);
    }
}
