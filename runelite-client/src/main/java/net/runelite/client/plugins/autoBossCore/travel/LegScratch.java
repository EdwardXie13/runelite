package net.runelite.client.plugins.autoBossCore.travel;

/**
 * Per-run mutable scratch for the CURRENTLY active leg, owned by the {@link TravelRunner} (not the leg --
 * legs live inside reusable {@code static final} {@link TravelRoute} constants, so they must stay
 * stateless). Holds the "first tick the target became actionable" latch and the rolled human jitter for
 * that latch. Cleared whenever the runner moves to a different leg, so each leg re-rolls fresh.
 */
public final class LegScratch
{
	private int latchTick = -1;
	private int jitter = 0;

	public boolean latched()               { return latchTick >= 0; }
	public void    latch(int tick, int j)  { this.latchTick = tick; this.jitter = j; }
	public int     latchTick()             { return latchTick; }
	public int     jitter()                { return jitter; }
	public void    clearLatch()            { this.latchTick = -1; }
	public void    reset()                 { this.latchTick = -1; this.jitter = 0; }
}
