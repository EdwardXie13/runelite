package net.runelite.client.plugins.autoBossCore.consume;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
		PRAYER, ANTIFIRE, ANTIVENOM, SUPER_COMBAT, DIVINE_SUPER_COMBAT, STAMINA, RANGING, DIVINE_RANGE, DIVINE_MAGIC
	}

	public static final class FoodEntry
	{
		public final int itemId;
		public final String name;
		/** True if this food can be eaten on the SAME tick as a regular food (combo eat) -- karambwan. */
		public final boolean combo;
		private final HealModel heal;

		/** Flat-heal food (shark/karambwan): restores {@code healAmount}, capped at the player's max HP. */
		public FoodEntry(int itemId, int healAmount, String name)
		{
			this(itemId, name, flat(healAmount), false);
		}

		/** Food with a dynamic, level-scaled heal / overheal ceiling (e.g. anglerfish). */
		public FoodEntry(int itemId, String name, HealModel heal)
		{
			this(itemId, name, heal, false);
		}

		/** Full constructor: {@code combo} marks combo-eligible food (karambwan). */
		public FoodEntry(int itemId, String name, HealModel heal, boolean combo)
		{
			this.itemId = itemId;
			this.name = name;
			this.heal = heal;
			this.combo = combo;
		}

		/** HP this food restores at the given max HP (Hitpoints level). */
		public int healAmount(int maxHp) { return heal.healAmount(maxHp); }
		/** Highest HP this food may take us to -- max HP for normal food, higher for overheal food. */
		public int ceiling(int maxHp)    { return heal.ceiling(maxHp); }

		public static FoodEntry shark(int id)      { return new FoodEntry(id, 20, "Shark"); }
		public static FoodEntry karambwan(int id)  { return new FoodEntry(id, "Karambwan", flat(18), true); }
		/** Manta ray: flat +22 heal, no overheal (item 391). */
		public static FoodEntry mantaRay(int id)   { return new FoodEntry(id, 22, "Manta ray"); }
		/** Tuna potato: flat +22 heal, no overheal (item 7060). */
		public static FoodEntry tunaPotato(int id) { return new FoodEntry(id, 22, "Tuna potato"); }
		/** Sea turtle: flat +21 heal, no overheal (item 397). */
		public static FoodEntry seaTurtle(int id)  { return new FoodEntry(id, 21, "Sea turtle"); }
		/** Pineapple pizza: flat +11 heal (per bite), no overheal (item 2301). */
		public static FoodEntry pineapplePizza(int id)     { return new FoodEntry(id, 11, "Pineapple pizza"); }
		/** 1/2 pineapple pizza: flat +11 heal, no overheal (item 2303). */
		public static FoodEntry halfPineapplePizza(int id) { return new FoodEntry(id, 11, "1/2 pineapple pizza"); }
		/** Anglerfish OVERHEALS: a level-scaled heal that can push HP above max (to maxHp + overheal boost). */
		public static FoodEntry anglerfish(int id) { return new FoodEntry(id, "Anglerfish", ANGLERFISH); }

		/** OSRS anglerfish: restores a level-scaled amount and may overheal above max. Boost by Hitpoints-level
		 *  bracket (2/4/6/8/13) plus floor(level/10); e.g. 99 HP -> +22 (heals to 121). VERIFY the exact table. */
		private static final HealModel ANGLERFISH = new HealModel()
		{
			@Override public int healAmount(int maxHp) { return anglerBoost(maxHp); }
			@Override public int ceiling(int maxHp)    { return maxHp + anglerBoost(maxHp); }
		};

		/**
		 * Every food the engine recognises, keyed by item id. Lets the engine eat ANY known food
		 * found in the inventory -- e.g. a boss drop -- even when it was not brought as a supply.
		 * Combo-eligible foods (karambwan) are flagged so a scavenged one is used for combo eating.
		 */
		private static final Map<Integer, FoodEntry> KNOWN = buildKnown();

		private static Map<Integer, FoodEntry> buildKnown()
		{
			Map<Integer, FoodEntry> m = new HashMap<>();
			for (FoodEntry f : new FoodEntry[]{
				shark(385), seaTurtle(397), mantaRay(391), tunaPotato(7060),
				pineapplePizza(2301), halfPineapplePizza(2303),
				karambwan(3144), anglerfish(13441),
			})
			{
				m.put(f.itemId, f);
			}
			return m;
		}

		/** Recognised food for this item id, or {@code null} if the item is not a known food. */
		public static FoodEntry known(int itemId) { return KNOWN.get(itemId); }
		/** All foods the engine recognises (brought or scavenged). */
		public static Collection<FoodEntry> knownFoods() { return KNOWN.values(); }

		/** Per-food heal model: the amount restored AND the HP ceiling, each a function of the player's max HP
		 *  (Hitpoints level). Flat food returns a constant heal and ceiling == maxHp; overheal food (anglerfish)
		 *  returns a level-scaled heal and a ceiling above maxHp. */
		public interface HealModel
		{
			int healAmount(int maxHp);
			int ceiling(int maxHp);
		}

		private static HealModel flat(int amount)
		{
			return new HealModel()
			{
				@Override public int healAmount(int maxHp) { return amount; }
				@Override public int ceiling(int maxHp)    { return maxHp; }
			};
		}

		private static int anglerBoost(int level)
		{
			int bracket = level < 25 ? 2 : level < 50 ? 4 : level < 75 ? 6 : level < 93 ? 8 : 13;
			return bracket + level / 10;
		}
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
