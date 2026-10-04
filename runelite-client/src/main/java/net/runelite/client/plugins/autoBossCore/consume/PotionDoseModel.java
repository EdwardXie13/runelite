package net.runelite.client.plugins.autoBossCore.consume;

import net.runelite.client.plugins.autoBossCore.state.MirrorState;

/**
 * Pure functions for projecting what the player can achieve given the doses currently in
 * the inventory. No state, no side-effects -- safe to call anywhere.
 */
public final class PotionDoseModel
{
	private PotionDoseModel() {}

	public static int countDoses(MirrorState mirror, SupplyProfile.PotionEntry potion)
	{
		if (potion == null) return 0;
		int total = 0;
		for (int i = 0; i < potion.doseIds.length; i++)
		{
			int count = mirror.countItem(potion.doseIds[i]);
			total += count * (i + 1);
		}
		return total;
	}

	public static int countSips(MirrorState mirror, SupplyProfile.PotionEntry potion)
	{
		return countDoses(mirror, potion);
	}

	public static int projectedPrayer(int current, int maxPrayer, int prayerLevel, int availableDoses)
	{
		if (availableDoses <= 0) return current;
		int perDose = 7 + prayerLevel / 4;
		return Math.min(maxPrayer, current + availableDoses * perDose);
	}

	public static int projectedHp(int current, int maxHp, int foodCount, int healPerFood)
	{
		if (foodCount <= 0) return current;
		return Math.min(maxHp, current + foodCount * healPerFood);
	}

	public static boolean canReachPrayerFloor(
		MirrorState mirror,
		SupplyProfile.PotionEntry prayerPotion,
		int targetFloor)
	{
		int doses = countDoses(mirror, prayerPotion);
		int projected = projectedPrayer(
			mirror.getPrayer(),
			mirror.getMaxPrayer(),
			mirror.getMaxPrayer(),
			doses
		);
		int effectiveFloor = Math.min(targetFloor, mirror.getMaxPrayer());
		return projected >= effectiveFloor;
	}
}
