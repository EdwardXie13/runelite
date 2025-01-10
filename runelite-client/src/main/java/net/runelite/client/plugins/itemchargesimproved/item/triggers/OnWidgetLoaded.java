package net.runelite.client.plugins.itemchargesimproved.item.triggers;

import net.runelite.client.plugins.itemchargesimproved.store.ItemWithQuantity;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnWidgetLoaded extends TriggerBase {
    public final int groupId;
    public final int childId;
    public Optional<Integer> subChildId = Optional.empty();

    public Optional<Pattern> text = Optional.empty();
    public Optional<Consumer<Matcher>> matcherConsumer = Optional.empty();
    public Optional<Consumer<Integer>> itemQuantityConsumer = Optional.empty();
    public Optional<Consumer<ItemWithQuantity>> itemWithQuantityConsumer = Optional.empty();
    public Optional<Consumer<Integer>> textAsChargesConsumer = Optional.empty();
    public Optional<Boolean> setDynamically = Optional.empty();

    public OnWidgetLoaded(final int groupId, final int childId) {
        this.groupId = groupId;
        this.childId = childId;
    }

    public OnWidgetLoaded(final int groupId, final int childId, final int subChildId) {
        this.groupId = groupId;
        this.childId = childId;
        this.subChildId = Optional.of(subChildId);
    }

    public OnWidgetLoaded setDynamically() {
        this.setDynamically = Optional.of(true);
        return this;
    }

    public OnWidgetLoaded text(final String text) {
        this.text = Optional.of(Pattern.compile(text));
        return this;
    }

    public OnWidgetLoaded matcherConsumer(final Consumer<Matcher> consumer) {
        this.matcherConsumer = Optional.of(consumer);
        return this;
    }

    public OnWidgetLoaded textAsChargesConsumer(final Consumer<Integer> consumer) {
        this.textAsChargesConsumer = Optional.of(consumer);
        return this;
    }

    public OnWidgetLoaded itemQuantityConsumer(final Consumer<Integer> consumer) {
        this.itemQuantityConsumer = Optional.of(consumer);
        return this;
    }

    public OnWidgetLoaded itemWithQuantityConsumer(final Consumer<ItemWithQuantity> consumer) {
        this.itemWithQuantityConsumer = Optional.of(consumer);
        return this;
    }
}
