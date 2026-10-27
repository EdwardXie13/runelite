package net.runelite.client.plugins.autoBossCore.travel;

/** Hold until a condition is met, taking no action (e.g. wait out a cutscene / loading gate). */
final class GateLeg implements TravelLeg
{
	private final DoneSignal signal;
	private final int[] regions;

	GateLeg(DoneSignal signal, int[] regions) { this.signal = signal; this.regions = regions; }

	public boolean done(TravelContext ctx, LegScratch s) { return signal.met(ctx); }
	public void act(TravelContext ctx, int tick, LegScratch s) { /* wait */ }
	public int[] regions() { return regions == null ? new int[0] : regions; }
	public String label()  { return "travel: wait"; }
}
