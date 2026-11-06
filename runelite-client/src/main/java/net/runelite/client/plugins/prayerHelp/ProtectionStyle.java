package net.runelite.client.plugins.prayerHelp;

import net.runelite.api.Prayer;

/**
 * A protection-prayer style the player can pick in {@link PrayerHelpConfig}'s dropdowns.
 * NONE leaves a slot unused -- that cadence tick makes NO prayer change.
 */
public enum ProtectionStyle
{
	NONE("None", null),
	MELEE("Melee", Prayer.PROTECT_FROM_MELEE),
	RANGED("Ranged", Prayer.PROTECT_FROM_MISSILES),
	MAGIC("Magic", Prayer.PROTECT_FROM_MAGIC);

	private final String label;
	private final Prayer prayer;

	ProtectionStyle(String label, Prayer prayer) { this.label = label; this.prayer = prayer; }

	/** The overhead protection prayer this style maps to, or null for NONE. */
	public Prayer prayer() { return prayer; }

	/** RuneLite renders an enum config item as a dropdown, using toString() as each option's label. */
	@Override
	public String toString() { return label; }
}
