package net.runelite.client.plugins.itemchargesimproved.item.triggers;

import net.runelite.client.plugins.itemchargesimproved.item.storage.StorageItem;

import java.util.Optional;

public class OnItemPickup extends TriggerBase {
    public final StorageItem[] items;
    public Optional<Boolean> isByOne = Optional.empty();

    public OnItemPickup(final StorageItem[] items) {
        this.items = items;
    }

    public OnItemPickup isByOne() {
        this.isByOne = Optional.of(true);
        return this;
    }
}
