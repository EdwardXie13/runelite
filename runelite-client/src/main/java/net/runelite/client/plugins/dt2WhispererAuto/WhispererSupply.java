package net.runelite.client.plugins.dt2WhispererAuto;

import java.util.concurrent.ThreadLocalRandom;
import net.runelite.client.plugins.autoBossCore.consume.SupplyProfile;

/**
 * Whisperer-specific supply profile. Minimal MAGIC-kill starter: sharks for food and a
 * prayer potion. TODO: add an offensive magic boost (imbued/saturated heart or magic
 * potion) and any utility potions (stamina, etc.) once the loadout is settled; wire a
 * combo food and/or a DIVINE_MAGIC entry here and in {@link WhispererAutoIDs} then.
 */
public final class WhispererSupply
{
	private WhispererSupply() {}

	/**
	 * HP eat threshold centred on 40 with a small +/-2 jitter ([38, 42]) for human-like variation -- the engine's
	 * opportunistic-eat default (request shark each tick, overheal cap is the real gate) drives actual eating. Stays
	 * above the critical floor (35). See
	 * {@link net.runelite.client.plugins.autoBossCore.consume.ConsumptionEngine} classdoc.
	 */
	private static final int RANDOM_EAT_THRESHOLD = ThreadLocalRandom.current().nextInt(38, 43);

	public static final SupplyProfile PROFILE = SupplyProfile.builder()
		.food(
			SupplyProfile.FoodEntry.shark(WhispererAutoIDs.FOOD_PRIMARY)
		)
		.potion(SupplyProfile.DrinkKind.PRAYER, new SupplyProfile.PotionEntry(WhispererAutoIDs.PRAYER_POT_DOSES, "Prayer potion"))
		.potion(SupplyProfile.DrinkKind.ANCIENT_BREW, SupplyProfile.ancientBrew())   // prayer restore: drained before the prayer potion
		.hpEatThreshold(RANDOM_EAT_THRESHOLD)
		.hpEatCriticalThreshold(35)
		.prayerSipJitterWindow(8)
		.prayerSipPokeFloor(50)
		.runEnergySipStam(25)
		.overhealHeadroom(0)
		.build();
}
