package net.runelite.client.plugins.itemchargesimproved.triggers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TriggerMenuOption {
    @Nonnull public final String option;

    @Nullable public Integer fixed_charges;

    public TriggerMenuOption(@Nonnull final String option) {
        this.option = option;
    }

    public TriggerMenuOption fixedCharges(final int charges) {
        this.fixed_charges = charges;
        return this;
    }
}
