package net.runelite.client.plugins.dt2VardorvisAuto;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

import java.awt.event.KeyEvent;

@ConfigGroup("dt2VardorvisAuto")
public interface VardorvisAutoConfig extends Config
{
	@ConfigSection(name = "Automation", description = "Start / stop the strategy.", position = 0)
	String autoSection = "autoSection";

	@ConfigItem(
		keyName = "toggleKey",
		name = "Start/Stop hotkey",
		description = "Toggle Vardorvis automation on/off.",
		section = autoSection,
		position = 0
	)
	default Keybind toggleKey() { return new Keybind(KeyEvent.VK_F7, 0); }

	// -----------------------------------------------------------------

	@ConfigSection(
		name = "Capture mode",
		description = "Developer tooling for logging boss mechanic IDs.",
		position = 10,
		closedByDefault = true
	)
	String captureSection = "captureSection";

	@ConfigItem(
		keyName = "captureMode",
		name = "Enable capture logging",
		description = "Dump projectiles, graphics, animations, npc spawns, varbits and hitsplats to the log while in the Vardorvis arena.",
		section = captureSection,
		position = 0
	)
	default boolean captureMode() { return false; }

	@ConfigItem(
		keyName = "arenaRegion",
		name = "Arena region id",
		description = "Only log events while the player is in this region. -1 = log everywhere.",
		section = captureSection,
		position = 1
	)
	default int arenaRegion() { return VardorvisAutoIDs.ARENA_REGION; }

	@ConfigItem(
		keyName = "capturePrefix",
		name = "Log line prefix",
		description = "Prefix on every capture line.",
		section = captureSection,
		position = 2
	)
	default String capturePrefix() { return "[vardCap]"; }
}
