package net.runelite.client.plugins.dt2DukeAuto;

import java.util.concurrent.ThreadLocalRandom;
import net.runelite.client.plugins.autoBossCore.consume.SupplyProfile;

/**
 * Duke Sucellus supply profile. Default is a MELEE kill: shark + karambwan, prayer pot,
 * super combat (divine preferred), stamina. No antifire / antivenom.
 * <p>
 * If this rig RANGES Duke instead, swap the two combat-boost lines below for
 * {@code DrinkKind.DIVINE_RANGE} / {@code DrinkKind.RANGING} (see LeviathanSupply) and
 * have {@link DukeAutoMain} request those kinds via a rangingBoost tracker.
 */
public final class DukeSupply
{
	private DukeSupply() {}

	/**
	 * Randomized HP eat threshold in [45, 50]. Informational with the opportunistic-eat
	 * model (engine's overheal cap is the real gate): shark whenever hp + 20 <= maxHp,
	 * karambwan on top whenever hp + 20 + 18 <= maxHp.
	 */
	private static final int RANDOM_EAT_THRESHOLD = ThreadLocalRandom.current().nextInt(45, 51);

	public static final SupplyProfile PROFILE = SupplyProfile.builder()
		.food(
			SupplyProfile.FoodEntry.shark(DukeAutoIDs.FOOD_PRIMARY)
		)
		.comboFood(SupplyProfile.FoodEntry.karambwan(DukeAutoIDs.FOOD_COMBO))
		.potion(SupplyProfile.DrinkKind.PRAYER,              new SupplyProfile.PotionEntry(DukeAutoIDs.PRAYER_POT_DOSES, "Prayer potion"))
		.potion(SupplyProfile.DrinkKind.DIVINE_SUPER_COMBAT, new SupplyProfile.PotionEntry(DukeAutoIDs.DIVINE_SUPER_COMBAT_DOSES, "Divine super combat"))
		.potion(SupplyProfile.DrinkKind.SUPER_COMBAT,        new SupplyProfile.PotionEntry(DukeAutoIDs.SUPER_COMBAT_DOSES, "Super combat"))
		.potion(SupplyProfile.DrinkKind.STAMINA,             new SupplyProfile.PotionEntry(DukeAutoIDs.STAMINA_DOSES, "Stamina"))
		.hpEatThreshold(RANDOM_EAT_THRESHOLD)
		.hpEatCriticalThreshold(35)
		.prayerSipJitterWindow(8)
		.prayerSipPokeFloor(50)
		.runEnergySipStam(25)
		.overhealHeadroom(0)
		.build();
}
