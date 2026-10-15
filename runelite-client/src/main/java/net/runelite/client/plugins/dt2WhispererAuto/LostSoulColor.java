package net.runelite.client.plugins.dt2WhispererAuto;

import java.util.Arrays;

/**
 * Shadow-realm lost-soul colors, identified by the NPC's model recolor array
 * ({@code NPC.getModelOverrides().getColorToReplaceWith()}, the devtools "C:" field).
 * YELLOW has no recolor (null / empty). UNKNOWN = a non-empty recolor that doesn't match
 * any known soul (capture it and add a case).
 */
public enum LostSoulColor
{
	YELLOW(null),
	BLUE(WhispererAutoIDs.SOUL_COLOR_BLUE),
	CYAN(WhispererAutoIDs.SOUL_COLOR_CYAN),
	GREEN(WhispererAutoIDs.SOUL_COLOR_GREEN),
	UNKNOWN(null);

	/** The recolor array that identifies this soul (null for YELLOW / UNKNOWN). */
	public final short[] colors;

	LostSoulColor(short[] colors) { this.colors = colors; }

	/** Classify a soul from its {@code getColorToReplaceWith()} array. */
	public static LostSoulColor fromColorOverride(short[] c)
	{
		if (c == null || c.length == 0) return YELLOW;
		if (Arrays.equals(c, WhispererAutoIDs.SOUL_COLOR_BLUE))  return BLUE;
		if (Arrays.equals(c, WhispererAutoIDs.SOUL_COLOR_CYAN))  return CYAN;
		if (Arrays.equals(c, WhispererAutoIDs.SOUL_COLOR_GREEN)) return GREEN;
		return UNKNOWN;
	}
}
