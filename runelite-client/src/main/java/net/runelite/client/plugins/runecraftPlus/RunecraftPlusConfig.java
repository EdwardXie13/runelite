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

@ConfigGroup(RunecraftPlusConfig.GROUP)
public interface RunecraftPlusConfig extends Config
{
	String GROUP = "runecraftPlus";

	@ConfigItem(
			keyName = "showDenseRunestoneIndicator",
			name = "Show dense runestone indicator",
			description = "Configures whether to display an indicator when dense runestone is ready to be mined",
			position = 1
	)
	default boolean showDenseRunestoneIndicator() {return true;}

	@ConfigItem(
			keyName = "showDenseRunestoneClickbox",
			name = "Show dense runestone click box",
			description = "Configures whether to display a click box when dense runestone is ready to be mined",
			position = 2
	)
	default boolean showDenseRunestoneClickbox()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showClickbox",
			name = "Show click box",
			description = "Configures whether to display a click box",
			position = 3
	)
	default boolean showClickbox()
	{
		return true;
	}
}