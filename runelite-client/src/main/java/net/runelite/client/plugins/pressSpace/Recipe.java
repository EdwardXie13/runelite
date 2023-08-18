package net.runelite.client.plugins.pressSpace;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Recipe {
    None(0),
    Potions(1),
    Herbs(2),
    BlowGlass(3),
    FireBstaff(4),
    // INSERT 5 here
    CutSapphire(6),
    Dragonhide(7);

    private final int id;
}
