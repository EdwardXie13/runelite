package net.runelite.client.plugins.itemchargesimproved.item.triggers;

import java.util.Optional;

public class OnResetDaily extends TriggerBase {
    public Optional<Integer> resetSpecificItem = Optional.empty();

    public OnResetDaily specificItem(final int itemId) {
        this.resetSpecificItem = Optional.of(itemId);
        return this;
    }
}
