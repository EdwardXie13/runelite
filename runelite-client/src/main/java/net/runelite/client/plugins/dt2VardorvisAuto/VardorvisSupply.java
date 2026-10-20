package net.runelite.client.plugins.dt2VardorvisAuto;

import java.util.concurrent.ThreadLocalRandom;
import net.runelite.client.plugins.autoBossCore.consume.SupplyProfile;

/**
 * Vardorvis-specific supply profile. Melee boss -- shark + kara, prayer pot, super combat,
 * stamina. No antifire / antivenom needed.
 */
public final class VardorvisSupply
{
	private VardorvisSupply() {}

	/**
	 * HP eat threshold centred on 40 with a small +/-2 jitter ([38, 42]) for human-like variation.
	 * Note: with the opportunistic-eat model (request shark + kara each tick, the engine's overheal
	 * cap is the real gate) this field is informational only; actual eating tops off whenever a food
	 * still fits under the cap. Stays above the critical floor (35).
	 */
	private static final int RANDOM_EAT_THRESHOLD = ThreadLocalRandom.current().nextInt(38, 43);

	public static final SupplyProfile PROFILE = SupplyProfile.builder()
		.food(
			SupplyProfile.FoodEntry.shark(VardorvisAutoIDs.FOOD_PRIMARY)
		)
		.comboFood(SupplyProfile.FoodEntry.karambwan(VardorvisAutoIDs.FOOD_COMBO))
		.potion(SupplyProfile.DrinkKind.PRAYER,       new SupplyProfile.PotionEntry(VardorvisAutoIDs.PRAYER_POT_DOSES, "Prayer potion"))
		.potion(SupplyProfile.DrinkKind.DIVINE_SUPER_COMBAT, new SupplyProfile.PotionEntry(VardorvisAutoIDs.DIVINE_SUPER_COMBAT_DOSES, "Divine super combat"))
		.potion(SupplyProfile.DrinkKind.SUPER_COMBAT, new SupplyProfile.PotionEntry(VardorvisAutoIDs.SUPER_COMBAT_DOSES, "Super combat"))
		.potion(SupplyProfile.DrinkKind.STAMINA,      new SupplyProfile.PotionEntry(VardorvisAutoIDs.STAMINA_DOSES, "Stamina"))
		.hpEatThreshold(RANDOM_EAT_THRESHOLD)
		.hpEatCriticalThreshold(35)
		.prayerSipJitterWindow(8)
		.prayerSipPokeFloor(50)
		.runEnergySipStam(25)
		.overhealHeadroom(0)
		.build();
}
