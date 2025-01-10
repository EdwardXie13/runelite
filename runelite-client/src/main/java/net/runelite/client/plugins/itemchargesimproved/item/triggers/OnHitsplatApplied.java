package net.runelite.client.plugins.itemchargesimproved.item.triggers;

import net.runelite.client.plugins.itemchargesimproved.store.HitsplatGroup;
import net.runelite.client.plugins.itemchargesimproved.store.HitsplatTarget;

import java.util.Optional;

public class OnHitsplatApplied extends TriggerBase {
    public final HitsplatTarget hitsplatTarget;
    public HitsplatGroup hitsplatGroup = HitsplatGroup.REGULAR;

    public Optional<Boolean> moreThanZeroDamage = Optional.empty();

    public OnHitsplatApplied(final HitsplatTarget hitsplatTarget) {
        this.hitsplatTarget = hitsplatTarget;
    }

    public OnHitsplatApplied moreThanZeroDamage() {
        this.moreThanZeroDamage = Optional.of(true);
        return this;
    }
}
