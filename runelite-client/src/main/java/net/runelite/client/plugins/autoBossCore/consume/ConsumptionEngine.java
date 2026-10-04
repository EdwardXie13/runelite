package net.runelite.client.plugins.autoBossCore.consume;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static net.runelite.client.plugins.autoBossCore.consume.SupplyProfile.DrinkKind;
import static net.runelite.client.plugins.autoBossCore.consume.SupplyProfile.FoodEntry;

/**
 * Per-tick "what should the player consume" decision engine.
 * <p>
 * Takes a {@link Request} that describes what the boss strategy would like to happen this
 * tick (required sips, opportunistic sips, want-to-eat), returns a {@link Result} naming
 * at most one drink, up to one primary food, and optionally one combo food on top. The
 * strategy is responsible for firing the matching clicks (via {@link
 * net.runelite.client.plugins.autoBossCore.combat.ClickRouter}) in the right order.
 * <p>
 * <b>Core top-off philosophy.</b> {@link Request.Builder} defaults BOTH {@code wantEat}
 * and {@code allowComboEat} to {@code true}. Every tick, the engine is implicitly asked
 * to top the player off with shark + karambwan. The REAL gate is the overheal cap on
 * each food, so a boss plugin can simply build a request and the engine fires every
 * food/drink that still has room:
 * <pre>
 *     Request req = Request.builder()
 *         .requireSip(DrinkKind.PRAYER)  // or opportunisticSip(...)
 *         .build();
 *     Result r = engine.evaluate(req);   // shark + kara + prayer pot if all fit
 * </pre>
 * A boss that explicitly does NOT want food on a given tick calls
 * {@link Request.Builder#suppressEat()}.
 * <p>
 * Rules enforced:
 * <ul>
 *   <li>At most one drink per tick (OSRS gamepack rejects two in rapid succession).</li>
 *   <li>Drink priority: {@link #DEFAULT_DRINK_PRIORITY}, overridable via {@link Hooks}.</li>
 *   <li>If any required drink is pending, a required drink wins over opportunistic.</li>
 *   <li>On a food tick, opportunistic drinks are suppressed EXCEPT {@link DrinkKind#PRAYER}
 *       (prayer is overrestore-safe, so it can safely stack on a food click).</li>
 *   <li>Primary food fires only if HP + heal + {@code overhealHeadroom} ≤ maxHp.</li>
 *   <li>Combo food fires only on top of a primary food, with the projected HP checked
 *       against the overheal cap as well.</li>
 * </ul>
 * <p>
 * The engine is a concrete class, not abstract -- all variation points are either
 * {@link Hooks} overrides or just not using the engine for that case.
 */
@Slf4j
public class ConsumptionEngine
{
	public static final List<DrinkKind> DEFAULT_DRINK_PRIORITY = Collections.unmodifiableList(Arrays.asList(
		DrinkKind.PRAYER,
		DrinkKind.ANTIFIRE,
		DrinkKind.ANTIVENOM,
		DrinkKind.DIVINE_RANGE,
		DrinkKind.DIVINE_MAGIC,
		DrinkKind.SUPER_COMBAT,
		DrinkKind.STAMINA
	));

	// -----------------------------------------------------------------
	// Hooks -- boss-specific overrides without needing to subclass.
	// -----------------------------------------------------------------

	public interface Hooks
	{
		/** Return non-null to override the drink priority order for this tick. */
		default List<DrinkKind> priorityOverride() { return null; }

		/** Veto a specific drink this tick (e.g. "don't sip stamina mid-execution phase"). */
		default boolean allowSip(DrinkKind k, MirrorState m) { return true; }
	}

	// -----------------------------------------------------------------
	// Request / Result.
	// -----------------------------------------------------------------

	public static final class Request
	{
		public final Set<DrinkKind> required;
		public final Set<DrinkKind> opportunistic;
		public final boolean wantEat;
		public final boolean allowComboEat;

		private Request(Set<DrinkKind> req, Set<DrinkKind> opp, boolean eat, boolean combo)
		{
			this.required      = req;
			this.opportunistic = opp;
			this.wantEat       = eat;
			this.allowComboEat = combo;
		}

		public static Builder builder() { return new Builder(); }

		public static final class Builder
		{
			private final Set<DrinkKind> req  = EnumSet.noneOf(DrinkKind.class);
			private final Set<DrinkKind> opp  = EnumSet.noneOf(DrinkKind.class);
			// Core default: optimistically top the player off every tick. The engine's
			// overheal cap is what actually gates each food, so flipping these to false
			// is only needed when a boss needs to SUPPRESS eating for some mechanic.
			private boolean eat   = true;
			private boolean combo = true;

			public Builder requireSip(DrinkKind k)       { req.add(k); return this; }
			public Builder requireSip(DrinkKind... ks)   { Collections.addAll(req, ks); return this; }
			public Builder opportunisticSip(DrinkKind k) { opp.add(k); return this; }
			public Builder opportunisticSip(DrinkKind... ks) { Collections.addAll(opp, ks); return this; }
			public Builder wantEat(boolean v)            { this.eat = v; return this; }
			public Builder allowComboEat(boolean v)      { this.combo = v; return this; }

			/** Convenience: disable BOTH primary and combo eat for this tick. */
			public Builder suppressEat() { this.eat = false; this.combo = false; return this; }

			public Request build()
			{
				return new Request(
					EnumSet.copyOf(req),
					EnumSet.copyOf(opp.isEmpty() ? EnumSet.noneOf(DrinkKind.class) : opp),
					eat,
					combo
				);
			}
		}
	}

	public static final class Result
	{
		public final DrinkKind drink;         // null = no drink
		public final FoodEntry primaryEat;    // null = no primary eat
		public final FoodEntry comboEat;      // null = no combo eat
		public final String rationale;

		public Result(DrinkKind d, FoodEntry pe, FoodEntry ce, String r)
		{
			this.drink = d; this.primaryEat = pe; this.comboEat = ce; this.rationale = r;
		}

		public boolean hasAction() { return drink != null || primaryEat != null; }

		public static final Result NOOP = new Result(null, null, null, "noop");
	}

	// -----------------------------------------------------------------
	// Fields.
	// -----------------------------------------------------------------

	protected final SupplyProfile profile;
	protected final MirrorState mirror;
	protected final Hooks hooks;

	public ConsumptionEngine(SupplyProfile profile, MirrorState mirror)
	{
		this(profile, mirror, new Hooks() {});
	}

	public ConsumptionEngine(SupplyProfile profile, MirrorState mirror, Hooks hooks)
	{
		this.profile = profile;
		this.mirror  = mirror;
		this.hooks   = hooks == null ? new Hooks() {} : hooks;
	}

	// -----------------------------------------------------------------
	// Public API.
	// -----------------------------------------------------------------

	public Result evaluate(Request req)
	{
		// --- 1. Food plan -----------------------------------------
		FoodEntry primary = null, combo = null;
		if (req.wantEat)
		{
			primary = pickPrimary();
			if (primary != null && req.allowComboEat)
			{
				int projected = Math.min(mirror.getMaxHp(), mirror.getHp() + primary.healAmount);
				combo = pickCombo(projected);
			}
		}
		boolean eating = primary != null;

		// --- 2. Drink plan ----------------------------------------
		DrinkKind drink = pickDrink(req, eating);

		String rationale = buildRationale(req, drink, primary, combo, eating);
		Result r = new Result(drink, primary, combo, rationale);
		if (r.hasAction()) log.debug("[consume] {}", rationale);
		return r;
	}

	// -----------------------------------------------------------------
	// Food selection.
	// -----------------------------------------------------------------

	/** Pick the first food in the primary/backup chain that fits HP room. */
	protected FoodEntry pickPrimary()
	{
		int hp = mirror.getHp();
		int maxHp = mirror.getMaxHp();
		int headroom = profile.overhealHeadroom;

		FoodEntry primary = profile.primaryFood();
		if (primary != null && mirror.countItem(primary.itemId) > 0
			&& hp + primary.healAmount <= maxHp - headroom)
		{
			return primary;
		}

		FoodEntry backup = profile.backupFood();
		if (backup != null && mirror.countItem(backup.itemId) > 0
			&& hp + backup.healAmount <= maxHp - headroom)
		{
			return backup;
		}

		// If both overheal, allow the smaller one when HP is critical -- better to overheal
		// a few than die on a 1hp miss.
		if (hp <= profile.hpEatCriticalThreshold)
		{
			if (backup != null && mirror.countItem(backup.itemId) > 0) return backup;
			if (primary != null && mirror.countItem(primary.itemId) > 0) return primary;
		}
		return null;
	}

	/** Combo food stacks on top of primary; only fires if there's still HP room. */
	protected FoodEntry pickCombo(int projectedHp)
	{
		FoodEntry combo = profile.comboFood();
		if (combo == null || mirror.countItem(combo.itemId) <= 0) return null;
		int headroom = profile.overhealHeadroom;
		if (projectedHp + combo.healAmount > mirror.getMaxHp() - headroom) return null;
		return combo;
	}

	// -----------------------------------------------------------------
	// Drink selection.
	// -----------------------------------------------------------------

	protected DrinkKind pickDrink(Request req, boolean eating)
	{
		// Build the candidate list honoring required > opportunistic.
		List<DrinkKind> candidates = new ArrayList<>();

		// Required pool first, in priority order.
		List<DrinkKind> order = hooks.priorityOverride();
		if (order == null) order = DEFAULT_DRINK_PRIORITY;

		for (DrinkKind k : order)
		{
			if (!req.required.contains(k)) continue;
			if (!canFire(k)) continue;
			candidates.add(k);
		}
		if (!candidates.isEmpty())
		{
			// A required drink wins over everything. Firing one even on a food tick is fine --
			// "required" means the boss NEEDS this now (expiring antifire, etc).
			return candidates.get(0);
		}

		// Opportunistic pool next.
		for (DrinkKind k : order)
		{
			if (!req.opportunistic.contains(k)) continue;
			if (!canFire(k)) continue;

			// Food-tick combo rule: non-prayer opp drinks suppressed when we are also eating.
			if (eating && k != DrinkKind.PRAYER) continue;

			candidates.add(k);
		}
		return candidates.isEmpty() ? null : candidates.get(0);
	}

	/** Can we fire this drink: profile has it, we have doses, hook allows. */
	protected boolean canFire(DrinkKind k)
	{
		SupplyProfile.PotionEntry pot = profile.potion(k);
		if (pot == null) return false;
		if (PotionDoseModel.countDoses(mirror, pot) <= 0) return false;
		if (!hooks.allowSip(k, mirror)) return false;
		return true;
	}

	// -----------------------------------------------------------------
	// Logging.
	// -----------------------------------------------------------------

	private String buildRationale(Request req, DrinkKind drink, FoodEntry pe, FoodEntry ce, boolean eating)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("hp=").append(mirror.getHp()).append('/').append(mirror.getMaxHp());
		sb.append(" pray=").append(mirror.getPrayer()).append('/').append(mirror.getMaxPrayer());
		if (!req.required.isEmpty())      sb.append(" req=").append(req.required);
		if (!req.opportunistic.isEmpty()) sb.append(" opp=").append(req.opportunistic);
		if (req.wantEat)                  sb.append(" wantEat");
		if (req.allowComboEat)            sb.append(" allowCombo");
		sb.append(" ->");
		sb.append(" drink=").append(drink);
		if (pe != null) sb.append(" eat=").append(pe.name);
		if (ce != null) sb.append(" combo=").append(ce.name);
		return sb.toString();
	}
}
