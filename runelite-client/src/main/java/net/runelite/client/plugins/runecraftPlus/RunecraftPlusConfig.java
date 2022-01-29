/*
 * Copyright (c) 2017, Seth <Sethtroll3@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
			name = "Show dense runestone click box",
			description = "Configures whether to display a click box when dense runestone is ready to be mined",
			position = 1
	)
	default boolean showDenseRunestoneClickbox()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showClickbox",
			name = "Show click box",
			description = "Configures whether to display a click box",
			position = 2
	)
	default boolean showClickbox()
	{
		return true;
	}

	@ConfigItem(
			keyName = "rotateCamera",
			name = "Rotate Camera",
			description = "Configures whether to auto-rotate camera",
			position = 3
	)
	default boolean rotateCamera()
	{
		return true;
	}

	@ConfigItem(
			keyName = "disableEmoteMenu",
			name = "Disable Emote Menu",
			description = "Configures whether to allow Emote Menu to be clicked",
			position = 4,
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
			position = 5,
			section = addOnSection
	)
	default boolean disableMusicMenu()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showStatus",
			name = "Show Status",
			description = "Configures whether to show status bar",
			position = 6,
			section = addOnSection
	)
	default boolean showStatus()
	{
		return false;
	}
}