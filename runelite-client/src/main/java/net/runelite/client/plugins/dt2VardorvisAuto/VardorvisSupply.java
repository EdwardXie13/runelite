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
	 * Randomized HP eat threshold in [45, 50]. Note: with the current VardorvisAutoMain
	 * opportunistic-eat model (always request shark + kara each tick, engine's overheal
	 * cap is the real gate), this field is informational only. The actual eat behavior
	 * is: shark whenever hp + 20 <= maxHp, kara on top whenever hp + 20 + 18 <= maxHp.
	 * At maxHp 99 that means shark at hp <= 79 and combo at hp <= 61, so any eat in the
	 * 45..50 range is already comfortably in combo territory.
	 */
	private static final int RANDOM_EAT_THRESHOLD = ThreadLocalRandom.current().nextInt(45, 51);

	public static final SupplyProfile PROFILE = SupplyProfile.builder()
		.food(
			SupplyProfile.FoodEntry.shark(VardorvisAutoIDs.FOOD_PRIMARY)
		)
		.comboFood(SupplyProfile.FoodEntry.karambwan(VardorvisAutoIDs.FOOD_COMBO))
		.potion(SupplyProfile.DrinkKind.PRAYER,       new SupplyProfile.PotionEntry(VardorvisAutoIDs.PRAYER_POT_DOSES, "Prayer potion"))
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
