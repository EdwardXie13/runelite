package net.runelite.client.plugins.dt2LeviathanAuto;

import java.util.concurrent.ThreadLocalRandom;
import net.runelite.client.plugins.autoBossCore.consume.SupplyProfile;
import net.runelite.client.plugins.autoBossCore.bank.BankProfile;
import net.runelite.api.coords.WorldPoint;

/**
 * Leviathan-specific supply profile. Mirrors Vardorvis's layout as a starting point.
 * Adjust once Leviathan-specific supply needs (e.g. antivenom+) are known.
 */
public final class LeviathanSupply
{
	private LeviathanSupply() {}

	/**
	 * Randomized HP eat threshold in [27, 35]. Note: with the autoBossCore
	 * opportunistic-eat default (always request shark + kara each tick, engine's
	 * overheal cap is the real gate), this field is informational only. See
	 * {@link net.runelite.client.plugins.autoBossCore.consume.ConsumptionEngine} classdoc.
	 */
	private static final int RANDOM_EAT_THRESHOLD = ThreadLocalRandom.current().nextInt(27, 36);

	public static final SupplyProfile PROFILE = SupplyProfile.builder()
		.food(
			SupplyProfile.FoodEntry.shark(LeviathanAutoIDs.FOOD_PRIMARY)
		)
		.comboFood(SupplyProfile.FoodEntry.karambwan(LeviathanAutoIDs.FOOD_COMBO))
		.potion(SupplyProfile.DrinkKind.PRAYER,       new SupplyProfile.PotionEntry(LeviathanAutoIDs.PRAYER_POT_DOSES, "Prayer potion"))
		.potion(SupplyProfile.DrinkKind.DIVINE_RANGE, new SupplyProfile.PotionEntry(LeviathanAutoIDs.DIVINE_RANGING_DOSES, "Divine ranging"))
		.potion(SupplyProfile.DrinkKind.RANGING, new SupplyProfile.PotionEntry(LeviathanAutoIDs.RANGING_DOSES, "Ranging"))
		.potion(SupplyProfile.DrinkKind.STAMINA,      new SupplyProfile.PotionEntry(LeviathanAutoIDs.STAMINA_DOSES, "Stamina"))
		.hpEatThreshold(RANDOM_EAT_THRESHOLD)
		.hpEatCriticalThreshold(26)
		.prayerSipJitterWindow(8)
		.prayerSipPokeFloor(50)
		.runEnergySipStam(25)
		.overhealHeadroom(0)
		.build();

	/**
	 * Banking config (POH -> rejuvenation pool -> Portal Nexus -> Lunar Isle booth). BankRun starts INSIDE
	 * the POH -- getting there is the boss's own job -- and stops once the bank is open.
	 */
	public static final BankProfile BANK = BankProfile.builder()
		.nexusOption("Lunar Isle")
		.restoreAtPool(true)
		.bankBoothTile(new WorldPoint(2099, 3920, 0))
		.build();
}
