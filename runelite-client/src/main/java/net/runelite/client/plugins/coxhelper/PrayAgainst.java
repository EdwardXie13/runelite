package net.runelite.client.plugins.coxhelper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Prayer;

@AllArgsConstructor
@Getter(AccessLevel.PACKAGE)
public enum PrayAgainst {
    MELEE(Prayer.PROTECT_FROM_MELEE),
    MAGIC(Prayer.PROTECT_FROM_MAGIC),
    RANGED(Prayer.PROTECT_FROM_MISSILES);

    private final Prayer prayer;
}