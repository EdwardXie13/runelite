package net.runelite.client.plugins.runecraftPlus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AbyssRifts {
    NONE("OFF", 0),
    CHAOS("CHAOS", 1),
    COSMIC("COSMIC", 2),
    DEATH("DEATH", 3),
    NATURE("NATURE", 4),
    LAW("LAW", 5),
    WRATH("WRATH", 6);

    private final String name;
    private final int id;
}
