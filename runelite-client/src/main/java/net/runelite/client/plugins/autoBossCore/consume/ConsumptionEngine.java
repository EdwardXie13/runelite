package net.runelite.client.plugins.autoBossCore.consume;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
	// Generic tie-break ordering ONLY -- NOT a gate on what may be consumed. autoBossCore
	// stays boss-agnostic: a boss consumes whatever DrinkKinds it requests (see pickDrink),
	// and this list only orders the universal drinks when several are fireable on one tick.
	// Any requested kind NOT listed here is still consumed (divine variants first).
	public static final List<DrinkKind> DEFAULT_DRINK_PRIORITY = Collections.unmodifiableList(Arrays.asList(
		DrinkKind.PRAYER,
		DrinkKind.ANTIFIRE,
		DrinkKind.ANTIVENOM,
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
				int projected = Math.min(primary.ceiling(mirror.getMaxHp()), mirror.getHp() + primary.healAmount(mirror.getMaxHp()));
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
	/**
	 * Request a JITTERED prayer-potion sip on {@code b}. {@code base} is the standalone sip point --
	 * the prayer level below which we sip when NOT otherwise busy (a lone sip interrupts the attack,
	 * so it is rationed). {@code jitterWindow} randomizes the exact point for human-like variation.
	 * When {@code freeWindow} is true we are eating / in downtime, so a sip costs no extra attack
	 * delay -- we then top prayer up for free up to the no-waste ceiling instead. Shared by every boss
	 * so prayer sipping is consistent and jittery everywhere.
	 */
	public static void requestPrayerSip(Request.Builder b, MirrorState mirror, int base, int jitterWindow, boolean freeWindow)
	{
		int pray = mirror.getPrayer();
		int maxPray = mirror.getMaxPrayer();
		int jw = Math.max(0, jitterWindow);
		if (freeWindow)
		{
			// Free top-up: sip whenever there is room for a full dose (no-waste ceiling), jittered.
			int topUp = Math.max(1, maxPray - (7 + maxPray / 4)
				- java.util.concurrent.ThreadLocalRandom.current().nextInt(0, jw + 1));
			if (pray <= topUp) b.requireSip(DrinkKind.PRAYER);
		}
		else
		{
			// Standalone: jittered point around base, with a hard floor so we never run dry.
			int floor = Math.max(1, base - jw);
			int sipAt = Math.max(1, base - java.util.concurrent.ThreadLocalRandom.current().nextInt(0, jw + 1));
			if (pray <= floor) b.requireSip(DrinkKind.PRAYER);
			else if (pray <= sipAt) b.opportunisticSip(DrinkKind.PRAYER);
		}
	}

	/**
	 * Candidate foods for this fight: brought supplies, the combo food, and every recognised
	 * (scavengeable) food, deduped by item id. Brought and scavenged food are treated identically --
	 * anything in the inventory that the engine recognises is fair game.
	 */
	private List<FoodEntry> candidateFoods()
	{
		Map<Integer, FoodEntry> m = new LinkedHashMap<>();
		for (FoodEntry f : profile.foods()) m.putIfAbsent(f.itemId, f);
		if (profile.comboFood() != null) m.putIfAbsent(profile.comboFood().itemId, profile.comboFood());
		for (FoodEntry f : FoodEntry.knownFoods()) m.putIfAbsent(f.itemId, f);
		return new ArrayList<>(m.values());
	}

	/** Pick the best non-karambwan food in the inventory that fits HP room (highest fitting heal). */
	protected FoodEntry pickPrimary()
	{
		int hp = mirror.getHp();
		int maxHp = mirror.getMaxHp();
		int headroom = profile.overhealHeadroom;

		// Any recognised non-combo food available (brought or scavenged, no distinction) that fits room.
		FoodEntry best = null;
		int bestHeal = -1;
		for (FoodEntry f : candidateFoods())
		{
			if (f.combo || mirror.countItem(f.itemId) <= 0) continue;
			int heal = f.healAmount(maxHp);
			if (hp + heal > f.ceiling(maxHp) - headroom) continue;   // would overheal
			if (heal > bestHeal) { best = f; bestHeal = heal; }
		}
		if (best != null) return best;

		// Everything overheals -- at critical HP eat the SMALLEST available anyway (waste the least),
		// better to overheal a few than die on a 1hp miss.
		if (hp <= profile.hpEatCriticalThreshold)
		{
			FoodEntry smallest = null;
			int smallestHeal = Integer.MAX_VALUE;
			for (FoodEntry f : candidateFoods())
			{
				if (f.combo || mirror.countItem(f.itemId) <= 0) continue;
				int heal = f.healAmount(maxHp);
				if (heal < smallestHeal) { smallest = f; smallestHeal = heal; }
			}
			if (smallest != null) return smallest;
		}
		return null;
	}

	/** Combo food (karambwan) stacks on the same tick as the primary; fires only if there's still room. */
	protected FoodEntry pickCombo(int projectedHp)
	{
		int maxHp = mirror.getMaxHp();
		int headroom = profile.overhealHeadroom;
		for (FoodEntry f : candidateFoods())
		{
			if (!f.combo || mirror.countItem(f.itemId) <= 0) continue;
			if (projectedHp + f.healAmount(maxHp) > f.ceiling(maxHp) - headroom) continue;
			return f;
		}
		return null;
	}

	// -----------------------------------------------------------------
	// Drink selection.
	// -----------------------------------------------------------------

	protected DrinkKind pickDrink(Request req, boolean eating)
	{
		List<DrinkKind> order = hooks.priorityOverride();
		if (order == null) order = DEFAULT_DRINK_PRIORITY;

		// Required pool wins over opportunistic, and a required drink fires even on a food
		// tick ("required" means the boss NEEDS it now, e.g. an expiring antifire).
		DrinkKind required = firstFireable(req.required, order, false, eating);
		if (required != null) return required;

		// Opportunistic pool next (non-prayer opp drinks suppressed on a food tick).
		return firstFireable(req.opportunistic, order, true, eating);
	}

	/**
	 * First fireable kind from {@code want}. Kinds that appear in {@code order} are tried
	 * in that order; then ANY remaining requested kind (not in {@code order}) is tried,
	 * divine variants first. This is what keeps autoBossCore generic: a boss requests
	 * whatever potions it carries and they are consumed without having to appear in the
	 * engine's default priority list -- the list only breaks ties among universal drinks.
	 */
	private DrinkKind firstFireable(Set<DrinkKind> want, List<DrinkKind> order,
		boolean opportunistic, boolean eating)
	{
		for (DrinkKind k : order)
		{
			if (!want.contains(k)) continue;
			if (opportunistic && eating && k != DrinkKind.PRAYER) continue;
			if (!canFire(k)) continue;
			return k;
		}
		// Requested-but-unlisted kinds. Prefer DIVINE_* (a divine potion is carried in place
		// of its regular counterpart, never alongside it), then enum-declaration order.
		List<DrinkKind> rest = new ArrayList<>();
		for (DrinkKind k : want) if (!order.contains(k)) rest.add(k);
		rest.sort((a, b) ->
		{
			boolean da = a.name().startsWith("DIVINE");
			boolean db = b.name().startsWith("DIVINE");
			if (da != db) return da ? -1 : 1;
			return Integer.compare(a.ordinal(), b.ordinal());
		});
		for (DrinkKind k : rest)
		{
			if (opportunistic && eating && k != DrinkKind.PRAYER) continue;
			if (!canFire(k)) continue;
			return k;
		}
		return null;
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
