package net.runelite.client.plugins.autoBossCore.consume;

/**
 * Tracks the attack delay that eating imposes, shared by every boss strategy.
 * <p>
 * In OSRS, eating a food blocks your next attack for a few ticks; eating a combo food
 * (karambwan) on the SAME tick adds more (shark + karambwan = 5t). A potion sip adds no
 * attack delay. A boss feeds each consumption {@link ConsumptionEngine.Result} here via
 * {@link #onConsumed} and gates its attack / attack-weave on {@link #attackBlocked} so it
 * never burns a wasted click (and desyncs its attack timer) during the eat delay.
 */
public final class EatDelay
{
	/** Ticks a single food (e.g. shark) blocks the next attack. */
	public static final int EAT_DELAY_TICKS = 3;
	/** Extra ticks when a combo food (karambwan) is eaten the same tick (shark + kara = 5t). */
	public static final int COMBO_EAT_EXTRA_TICKS = 2;

	private int blockedUntil = -100;

	/** Attack delay (ticks) a consumption result imposes: 0 if no food was eaten. */
	public static int delayFor(ConsumptionEngine.Result r)
	{
		if (r == null || r.primaryEat == null) return 0;
		return EAT_DELAY_TICKS + (r.comboEat != null ? COMBO_EAT_EXTRA_TICKS : 0);
	}

	/** Record that {@code r} was consumed at {@code tick}; extends the attack block if it ate food. */
	public void onConsumed(int tick, ConsumptionEngine.Result r)
	{
		int d = delayFor(r);
		if (d > 0) blockedUntil = Math.max(blockedUntil, tick + d);
	}

	/** True while an eat / combo-eat attack delay is still in effect at {@code tick}. */
	public boolean attackBlocked(int tick) { return tick < blockedUntil; }

	/** The tick the current block ends (exclusive); -100 when clear. */
	public int blockedUntil() { return blockedUntil; }

	/** Clear the block (on death / fight reset). */
	public void reset() { blockedUntil = -100; }
}
