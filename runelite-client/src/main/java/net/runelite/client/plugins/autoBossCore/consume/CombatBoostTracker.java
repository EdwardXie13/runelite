package net.runelite.client.plugins.autoBossCore.consume;

import net.runelite.client.plugins.autoBossCore.consume.SupplyProfile.DrinkKind;

/**
 * Generic re-dose planner for a single combat-boost potion slot (e.g. ranging, super combat).
 * autoBossCore owns no boss-specific potion knowledge: the boss passes its own divine kind,
 * regular kind, watched skill levels and regular re-dose threshold; this class only encodes
 * the two UNIVERSAL game facts about how those two potion families behave over time.
 *
 * <ul>
 *   <li><b>Divine variant</b> -- holds the boost FLAT for 5 minutes (no decay), then lets it
 *       decay normally. A "boosted &lt;= base" trigger therefore fires minutes too late, so
 *       the divine re-dose is driven by a 5-minute TIMER since the last divine sip (plus a
 *       safety net: sip now if we somehow have no boost at all).</li>
 *   <li><b>Regular variant</b> -- decays from the moment it's drunk, so it re-doses when the
 *       REMAINING boost falls below a boss-specified threshold.</li>
 * </ul>
 *
 * Only one of the two is ever carried (never both), so requesting both each tick is safe --
 * {@link ConsumptionEngine#canFire} drops whichever isn't in the inventory, and the engine
 * prefers the divine kind when present.
 */
public final class CombatBoostTracker
{
	/** Divine potion lock duration: 5 minutes = 500 game ticks. */
	public static final int DIVINE_DURATION_TICKS = 500;
	/** Refresh a few ticks early so the opportunistic sip lands before any decay begins. */
	private static final int DIVINE_REFRESH_SLACK = 10;

	private int lastDivineSipTick = Integer.MIN_VALUE;
	private boolean initialized = false;

	/**
	 * Add the appropriate combat-boost sip request(s) for this tick.
	 *
	 * @param b               request builder being assembled this tick
	 * @param now             current game tick (monotonic within a session)
	 * @param boostedLevel    current boosted level of the watched skill
	 * @param realLevel       base (real) level of the watched skill
	 * @param divineKind      the divine DrinkKind for this slot (e.g. DIVINE_RANGE)
	 * @param regularKind     the regular DrinkKind for this slot (e.g. RANGING)
	 * @param regularMinBoost re-dose the regular potion once (boosted - real) is BELOW this
	 *                        (boss-specified threshold; e.g. 1 = re-dose only when fully gone)
	 */
	public void requestSips(ConsumptionEngine.Request.Builder b, int now,
		int boostedLevel, int realLevel,
		DrinkKind divineKind, DrinkKind regularKind, int regularMinBoost)
	{
		// First observation: if we're already boosted, assume the potion was just drunk and
		// start the timer now (don't waste a dose refreshing a fresh boost). If not boosted,
		// leave the timer unset so the divine branch sips immediately.
		if (!initialized)
		{
			initialized = true;
			if (boostedLevel > realLevel) lastDivineSipTick = now;
		}

		boolean divineExpired = (now - lastDivineSipTick) >= (DIVINE_DURATION_TICKS - DIVINE_REFRESH_SLACK);
		boolean noBoostAtAll  = boostedLevel <= realLevel;
		if (divineExpired || noBoostAtAll) b.opportunisticSip(divineKind);

		if (boostedLevel - realLevel < regularMinBoost) b.opportunisticSip(regularKind);
	}

	/** Call when the engine reports it actually drank the divine kind -- restarts the timer. */
	public void onDivineSipped(int now)
	{
		lastDivineSipTick = now;
		initialized = true;
	}

	/** Reset between fights so the next fight re-evaluates from scratch. */
	public void reset()
	{
		lastDivineSipTick = Integer.MIN_VALUE;
		initialized = false;
	}
}
