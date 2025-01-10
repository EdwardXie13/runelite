package net.runelite.client.plugins.itemchargesimproved.item.triggers;

import net.runelite.client.plugins.itemchargesimproved.store.AdvancedMenuEntry;

import java.util.Optional;
import java.util.function.Consumer;

public class OnMenuOptionClicked extends TriggerBase {
    public final String option;
    public Optional<Consumer<AdvancedMenuEntry>> menuOptionConsumer = Optional.empty();

    public OnMenuOptionClicked(final String option) {
        this.option = option;
    }

    public OnMenuOptionClicked menuOptionConsumer(final Consumer<AdvancedMenuEntry> consumer) {
        this.menuOptionConsumer = Optional.of(consumer);
        return this;
    }
}
