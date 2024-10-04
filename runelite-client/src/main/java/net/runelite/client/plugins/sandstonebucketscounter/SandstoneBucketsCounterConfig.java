package net.runelite.client.plugins.sandstonebucketscounter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SandstoneBucketsCounterConfig.CONFIG_GROUP_NAME)
public interface SandstoneBucketsCounterConfig extends Config
{
	String CONFIG_GROUP_NAME = "sandstonebucketscounter";

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Overlay",
		description = "Toggles the counter overlay"
	)
	default boolean showOverlay()
	{
		return true;
	}
}
