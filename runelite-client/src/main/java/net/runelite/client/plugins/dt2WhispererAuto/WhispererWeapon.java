package net.runelite.client.plugins.dt2WhispererAuto;

/**
 * Per-weapon attack profile: how fast the weapon attacks (ticks between attacks), how far it can reach
 * (tiles), and the inventory item id used to WIELD it (for the siphon weapon-swap). Selected in
 * {@link WhispererAutoConfig}; the config also allows numeric overrides for tuning without editing this
 * enum.
 * <p>
 * TODO: confirm the tick speeds and ranges in-game -- the values below are best-known defaults for magic
 * setups and are easy to override from the config panel meanwhile. The non-Shadow item ids are -1
 * (unknown) until captured; equipMainWeapon() no-ops for those.
 */
public enum WhispererWeapon
{
	STANDARD_SPELLS("Standard spells",   5, 10, -1),
	TRIDENT        ("Trident",           4, 10, 12899),   // Trident of the swamp
	SANGUINESTI    ("Sanguinesti staff", 4, 10, -1),
	SHADOW         ("Tumeken's shadow",  5, 10, 27275);

	/** Human-readable name shown in the config dropdown. */
	public final String label;
	/** Attack speed: ticks between attacks (weapon cooldown). */
	public final int cooldownTicks;
	/** Attack range in tiles (Chebyshev distance to the boss footprint). */
	public final int rangeTiles;
	/** Inventory item id to wield this weapon (captured "Wield" CC_OP). -1 = unknown / not wieldable. */
	public final int itemId;

	WhispererWeapon(String label, int cooldownTicks, int rangeTiles, int itemId)
	{
		this.label = label;
		this.cooldownTicks = cooldownTicks;
		this.rangeTiles = rangeTiles;
		this.itemId = itemId;
	}

	@Override
	public String toString() { return label; }
}
