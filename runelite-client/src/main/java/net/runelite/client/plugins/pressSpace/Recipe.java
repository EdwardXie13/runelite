package net.runelite.client.plugins.pressSpace;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Recipe {
    None(0),
    GuamPot(1),
    AttackPot(2),
    BlowGlass(3),
    FireBstaff(4);

    private final int id;
}
