package net.runelite.client.plugins.smithing;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("SmithingAnvil")
public interface SmithingAnvilConfig extends Config {
    @ConfigItem(
            keyName = "selectAnvilOption",
            name = "Anvil only option",
            description = "Anvil only option",
            position = 0
    )
    default AnvilOptions selectAnvilOption()
    {
        return AnvilOptions.NONE;
    }
}
