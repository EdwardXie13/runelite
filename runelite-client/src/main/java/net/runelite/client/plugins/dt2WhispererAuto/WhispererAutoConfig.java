package net.runelite.client.plugins.dt2WhispererAuto;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

import java.awt.event.KeyEvent;

@ConfigGroup("dt2WhispererAuto")
public interface WhispererAutoConfig extends Config
{
	@ConfigSection(name = "Automation", description = "Start / stop the strategy.", position = 0)
	String autoSection = "autoSection";

	@ConfigItem(
		keyName = "toggleKey",
		name = "Start/Stop hotkey",
		description = "Toggle Whisperer automation on/off.",
		section = autoSection,
		position = 0
	)
	default Keybind toggleKey() { return new Keybind(KeyEvent.VK_F7, 0); }

	// -----------------------------------------------------------------

	@ConfigSection(
		name = "Weapon",
		description = "Attack speed (ticks) and reach (tiles) for the weapon in use.",
		position = 5
	)
	String weaponSection = "weaponSection";

	@ConfigItem(
		keyName = "attackTickOverride",
		name = "Attack tick override",
		description = "Override the weapon's attack speed, in ticks. 0 = use the weapon default.",
		section = weaponSection,
		position = 1
	)
	default int attackTickOverride() { return 0; }

	@ConfigItem(
		keyName = "attackRangeOverride",
		name = "Attack range override",
		description = "Override the weapon's attack range, in tiles. 0 = use the weapon default.",
		section = weaponSection,
		position = 2
	)
	default int attackRangeOverride() { return 0; }

	@ConfigItem(
		keyName = "seedClockwise",
		name = "Seed path clockwise",
		description = "Direction to rotate around the good-seed ring during the seed crossing. "
			+ "Either direction only lands on good seeds; this just breaks the initial tie.",
		section = weaponSection,
		position = 3
	)
	default boolean seedClockwise() { return true; }

	@ConfigItem(
		keyName = "tentacleDodgeDelay",
		name = "Tentacle dodge delay (ticks)",
		description = "Ticks to keep attacking after tentacles spawn before stepping off. The attack "
			+ "strikes ~2 ticks after the spawn and locks your tile, so 1 (step on the next tick) both "
			+ "dodges in time and weaves one attack. 0 = step immediately (safest); raise only if 1 clips.",
		section = weaponSection,
		position = 4
	)
	default int tentacleDodgeDelay() { return 1; }

	@ConfigItem(
		keyName = "tentacleMinGap",
		name = "Tentacle dodge: min boss gap (tiles)",
		description = "Minimum tiles to keep between you and the Whisperer when dodging tentacles. The "
			+ "dodge won't step inside this (stays out of melee) and hugs it, oscillating near the boss "
			+ "instead of drifting away. ~4 mimics a human; raise to stay farther out.",
		section = weaponSection,
		position = 5
	)
	default int tentacleMinGap() { return 4; }

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
		description = "Dump projectiles, graphics, animations, npc spawns, varbits and hitsplats to the log while in the Whisperer arena.",
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
	default int arenaRegion() { return WhispererAutoIDs.ARENA_REGION; }

	@ConfigItem(
		keyName = "capturePrefix",
		name = "Log line prefix",
		description = "Prefix on every capture line.",
		section = captureSection,
		position = 2
	)
	default String capturePrefix() { return "[whisCap]"; }
}
