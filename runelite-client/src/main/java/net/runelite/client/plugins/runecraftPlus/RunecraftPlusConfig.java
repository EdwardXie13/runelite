package net.runelite.client.plugins.runecraftPlus;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("runecraftPlus")
public interface RunecraftPlusConfig extends Config
{
	@ConfigSection(
			name = "Add On Section",
			description = "Settings for Runecraft QOL",
			position = 17
	)
	String addOnSection = "Runecraft Plus Add Ons";

	@ConfigItem(
			keyName = "showDenseRunestoneClickbox",
			name = "Normal Zeah click box",
			description = "Display a click box when dense runestone is ready to be mined",
			position = 1
	)
	default boolean showDenseRunestoneClickbox()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showZeahClickbox",
			name = "Advanced Zeah click box",
			description = "Display click box",
			position = 2
	)
	default boolean showZeahClickbox()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showNatureClickbox",
			name = "Abyss click box",
			description = "Display click box",
			position = 3
	)
	default AbyssRifts showAbyssClickBox()
	{
		return AbyssRifts.NONE;
	}

//	@ConfigItem(
//			keyName = "rotateCamera",
//			name = "Rotate Camera",
//			description = "Configures whether to auto-rotate camera",
//			position = 3
//	)
//	default boolean rotateCamera()
//	{
//		return true;
//	}

//	@ConfigItem(
//			keyName = "checkEnergy",
//			name = "Check Energy",
//			description = "Configures whether to check for enough energy",
//			position = 4
//	)
//	default boolean checkEnergy()
//	{
//		return true;
//	}

	@ConfigItem(
			keyName = "disableEmoteMenu",
			name = "Disable Emote Menu",
			description = "Configures whether to allow Emote Menu to be clicked",
			position = 5,
			section = addOnSection
	)
	default boolean disableEmoteMenu()
	{
		return true;
	}

	@ConfigItem(
			keyName = "disableMusicMenu",
			name = "Disable Music Menu",
			description = "Configures whether to allow Music Menu to be clicked",
			position = 6,
			section = addOnSection
	)
	default boolean disableMusicMenu()
	{
		return true;
	}
}