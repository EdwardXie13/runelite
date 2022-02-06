package net.runelite.client.plugins.neverLogout;


import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(NeverLogoutConfig.GROUP)
public interface NeverLogoutConfig extends Config
{
    String GROUP = "NeverLogout";

    @ConfigItem(
            keyName = "neverLogout",
            name = "Never Logout",
            description = "Configures whether to never logout",
            position = 1
    )
    default boolean neverLogout()
    {
        return true;
    }
}
