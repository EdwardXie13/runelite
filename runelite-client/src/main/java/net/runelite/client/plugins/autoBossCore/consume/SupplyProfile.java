package net.runelite.client.plugins.autoBossCore.consume;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Declarative "what supplies does this boss use and at what thresholds" bundle.
 * <p>
 * One instance per boss, built once at plugin startup and handed to the
 * {@link ConsumptionEngine}. Nothing in here is behavior -- just data.
 */
public final class SupplyProfile
{
	public enum DrinkKind
	{
		PRAYER, ANTIFIRE, ANTIVENOM, SUPER_COMBAT, STAMINA, DIVINE_RANGE, DIVINE_MAGIC
	}

	public static final class FoodEntry
	{
		public final int itemId;
		public final int healAmount;
		public final String name;

		public FoodEntry(int itemId, int healAmount, String name)
		{
			this.itemId = itemId;
			this.healAmount = healAmount;
			this.name = name;
		}

		public static FoodEntry shark(int id)      { return new FoodEntry(id, 20, "Shark"); }
		public static FoodEntry karambwan(int id)  { return new FoodEntry(id, 18, "Karambwan"); }
		public static FoodEntry anglerfish(int id) { return new FoodEntry(id, 22, "Anglerfish"); }
	}

	public static final class PotionEntry
	{
		/** Item IDs for the different dose levels. Index 0 = 1-dose, index 3 = 4-dose. */
		public final int[] doseIds;
		public final String name;

		public PotionEntry(int[] doseIds, String name)
		{
			if (doseIds == null || doseIds.length != 4)
				throw new IllegalArgumentException("doseIds must be length 4: " + name);
			this.doseIds = doseIds.clone();
			this.name = name;
		}
	}

	private final List<FoodEntry> foods;
	private final FoodEntry comboFood;
	private final Map<DrinkKind, PotionEntry> potions;

	public final int hpEatThreshold;
	public final int hpEatCriticalThreshold;
	public final int prayerSipJitterWindow;
	public final int prayerSipPokeFloor;
	public final int runEnergySipStam;
	public final int overhealHeadroom;

	private SupplyProfile(Builder b)
	{
		this.foods = Collections.unmodifiableList(new ArrayList<>(b.foods));
		this.comboFood = b.comboFood;
		this.potions = Collections.unmodifiableMap(new EnumMap<>(b.potions));
		this.hpEatThreshold         = b.hpEatThreshold;
		this.hpEatCriticalThreshold = b.hpEatCriticalThreshold;
		this.prayerSipJitterWindow  = b.prayerSipJitterWindow;
		this.prayerSipPokeFloor     = b.prayerSipPokeFloor;
		this.runEnergySipStam       = b.runEnergySipStam;
		this.overhealHeadroom       = b.overhealHeadroom;
	}

	public List<FoodEntry> foods() { return foods; }
	public FoodEntry primaryFood() { return foods.isEmpty() ? null : foods.get(0); }
	public FoodEntry backupFood()  { return foods.size() < 2 ? null : foods.get(1); }
	public FoodEntry comboFood()   { return comboFood; }
	public PotionEntry potion(DrinkKind k) { return potions.get(k); }
	public boolean hasPotion(DrinkKind k)  { return potions.containsKey(k); }

	public static Builder builder() { return new Builder(); }

	public static final class Builder
	{
		private final List<FoodEntry> foods = new ArrayList<>();
		private FoodEntry comboFood = null;
		private final Map<DrinkKind, PotionEntry> potions = new EnumMap<>(DrinkKind.class);

		private int hpEatThreshold         = 60;
		private int hpEatCriticalThreshold = 35;
		private int prayerSipJitterWindow  = 8;
		private int prayerSipPokeFloor     = 50;
		private int runEnergySipStam       = 20;
		private int overhealHeadroom       = 0;

		public Builder food(FoodEntry... fs) { Collections.addAll(this.foods, fs); return this; }
		public Builder comboFood(FoodEntry f)  { this.comboFood = f; return this; }
		public Builder potion(DrinkKind k, PotionEntry p) { potions.put(k, p); return this; }
		public Builder hpEatThreshold(int v)         { this.hpEatThreshold = v; return this; }
		public Builder hpEatCriticalThreshold(int v) { this.hpEatCriticalThreshold = v; return this; }
		public Builder prayerSipJitterWindow(int v)  { this.prayerSipJitterWindow = v; return this; }
		public Builder prayerSipPokeFloor(int v)     { this.prayerSipPokeFloor = v; return this; }
		public Builder runEnergySipStam(int v)       { this.runEnergySipStam = v; return this; }
		public Builder overhealHeadroom(int v)       { this.overhealHeadroom = v; return this; }

		public SupplyProfile build() { return new SupplyProfile(this); }
	}
}
