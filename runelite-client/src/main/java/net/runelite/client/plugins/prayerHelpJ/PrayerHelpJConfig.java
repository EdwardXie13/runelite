package net.runelite.client.plugins.prayerHelpJ;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

import java.awt.event.KeyEvent;

@ConfigGroup("prayerHelpJ")
public interface PrayerHelpJConfig extends Config
{
	@ConfigItem(
		keyName = "toggleKey",
		name = "Start/Stop hotkey",
		description = "Arm / disarm the Jad prayer helper. While armed, each Jad's attack animation flips the overhead.",
		position = 0
	)
	default Keybind toggleKey() { return new Keybind(KeyEvent.VK_F8, 0); }
}
