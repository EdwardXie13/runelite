package net.runelite.client.plugins.pressSpace;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("pressSpace")
public interface PressSpaceConfig extends Config {
    @ConfigItem(
            keyName = "guamPotUnf",
            name = "Guam Pot unf",
            description = "close bank guam + vial of water"
    )
    default boolean guamPotUnf()
    {
        return true;
    }

    @ConfigItem(
            keyName = "attackPot",
            name = "Attack pot",
            description = "close bank unf guam pot + eye of newt"
    )
    default boolean attackPot()
    {
        return true;
    }

    @ConfigItem(
            keyName = "craftMoltenGlass",
            name = "Glass blow",
            description = "close bank molten glass + pipe"
    )
    default boolean glassBlowing()
    {
        return true;
    }

    @ConfigItem(
            keyName = "fireBattlestaff",
            name = "Fire Battlestaff",
            description = "close bank fire orb + battlestaff"
    )
    default boolean fireBattlestaff()
    {
        return true;
    }

    @ConfigItem(
            keyName = "staminaPot",
            name = "Stamina pot",
            description = "close bank amylase + super energy3/4"
    )
    default boolean staminaPot()
    {
        return true;
    }

    @ConfigItem(
            keyName = "sapphire",
            name = "Sapphire",
            description = "close bank chisel + Sapphire"
    )
    default boolean cutSapphire()
    {
        return true;
    }

    @ConfigItem(
            keyName = "dragonhide",
            name = "Dragonhide",
            description = "close bank needle + thread + dragonhide"
    )
    default boolean craftDragonhide()
    {
        return true;
    }
}
