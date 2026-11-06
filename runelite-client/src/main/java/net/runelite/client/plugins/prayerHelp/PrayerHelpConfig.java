package net.runelite.client.plugins.prayerHelp;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

import java.awt.event.KeyEvent;

@ConfigGroup("prayerHelp")
public interface PrayerHelpConfig extends Config
{
	@ConfigSection(
		name = "Flicking",
		description = "Start / stop the 4-tick prayer cadence and when it begins.",
		position = 0
	)
	String flickSection = "flickSection";

	@ConfigItem(
		keyName = "toggleKey",
		name = "Start/Stop hotkey",
		description = "Toggle the cadence. Turning it ON anchors the rotation to the current client tick.",
		section = flickSection,
		position = 0
	)
	default Keybind toggleKey() { return new Keybind(KeyEvent.VK_F8, 0); }

	@ConfigItem(
		keyName = "firstPrayerTick",
		name = "First prayer at tick",
		description = "Client ticks after Start before Prayer 1 activates. Ticks 1 .. this-minus-1 do nothing.",
		section = flickSection,
		position = 1
	)
	default int firstPrayerTick() { return 15; }

	@ConfigSection(
		name = "Protection prayers",
		description = "The 4-tick plan from the first-prayer tick on: that tick -> Prayer 1, +1 -> Prayer 2, ... then repeats. None = no change that tick.",
		position = 1
	)
	String prayerSection = "prayerSection";

	@ConfigItem(keyName = "p1", name = "Prayer 1", description = "Prayed on the first-prayer tick (then every 4 ticks).", section = prayerSection, position = 0)
	default ProtectionStyle p1() { return ProtectionStyle.RANGED; }

	@ConfigItem(keyName = "p2", name = "Prayer 2", description = "Prayed on first-prayer tick + 1 (then every 4 ticks).", section = prayerSection, position = 1)
	default ProtectionStyle p2() { return ProtectionStyle.MAGIC; }

	@ConfigItem(keyName = "p3", name = "Prayer 3", description = "Prayed on first-prayer tick + 2 (then every 4 ticks).", section = prayerSection, position = 2)
	default ProtectionStyle p3() { return ProtectionStyle.MELEE; }

	@ConfigItem(keyName = "p4", name = "Prayer 4", description = "Prayed on first-prayer tick + 3 (then every 4 ticks).", section = prayerSection, position = 3)
	default ProtectionStyle p4() { return ProtectionStyle.MAGIC; }
}
