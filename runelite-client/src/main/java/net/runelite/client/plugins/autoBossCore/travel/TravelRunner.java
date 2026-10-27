package net.runelite.client.plugins.autoBossCore.travel;

import net.runelite.api.Client;
import net.runelite.client.plugins.autoBossCore.combat.ClickRouter;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;

/**
 * Tick-driven engine that walks a {@link TravelRoute}. The boss drives it from onGameTick exactly like
 * BankRun: {@link #begin(TravelRoute)}, then {@link #step(int)} each tick (true == the run owns the tick),
 * until {@link #arrived()}. Forward progress uses a monotonic cursor, but every tick {@link #relocate()}
 * re-derives the player's real position from leg {@link TravelLeg#regions()} and snaps the cursor back
 * (shoved backward) or restarts at leg 0 (displaced off-route entirely) -- so a random teleport, death
 * respawn or relog self-heals. A stuck watchdog re-rolls the active leg if it stalls too long.
 */
public final class TravelRunner
{
	private static final int STUCK_TICKS = 50;

	private final TravelContext ctx;
	private final LegScratch scratch = new LegScratch();

	private TravelRoute route;
	private int legIdx;
	private boolean arrived;
	private String label = "";

	private int legEnteredTick = 0;
	private int lastLegIdx = -1;

	public TravelRunner(Client client, MirrorState mirror, ClickRouter router)
	{
		this.ctx = new TravelContext(client, mirror, router);
	}

	public void begin(TravelRoute route)
	{
		this.route = route; this.legIdx = 0; this.arrived = false; this.lastLegIdx = -1;
		scratch.reset(); ctx.resetDebounce();
	}

	public void reset()
	{
		this.route = null; this.legIdx = 0; this.arrived = false; this.lastLegIdx = -1; scratch.reset();
	}

	public boolean active()    { return route != null && !arrived; }
	public boolean arrived()   { return arrived; }
	public String  label()     { return label; }
	public String  routeName() { return route == null ? null : route.name(); }

	/** Advance the run one tick. Returns true while the run OWNS the tick (travelling). */
	public boolean step(int tick)
	{
		if (route == null) return false;

		relocate();

		// Skip any already-satisfied prefix (resume-from-middle + normal forward progress).
		while (legIdx < route.size() && route.leg(legIdx).done(ctx, scratch))
		{
			legIdx++;
			scratch.reset();
		}
		if (legIdx >= route.size()) { arrived = true; label = "travel: arrived"; return false; }

		// Stuck watchdog: if we have sat on one leg too long with no completion, re-roll its latch so the
		// action re-issues (dropped click / blocked path). relocate() handles genuine displacement.
		if (lastLegIdx != legIdx) { legEnteredTick = tick; lastLegIdx = legIdx; }
		else if (tick - legEnteredTick > STUCK_TICKS) { scratch.reset(); legEnteredTick = tick; }

		TravelLeg leg = route.leg(legIdx);
		leg.act(ctx, tick, scratch);
		label = leg.label();
		return true;
	}

	/** State-aware self-location: fix the cursor if the player has been displaced off the expected leg. */
	private void relocate()
	{
		// Consistent with the cursor (current leg's region, or the next one to cover a transition tick)?
		if (regionMatches(legIdx) || regionMatches(legIdx + 1)) return;

		// Displaced: snap to the LATEST leg whose region matches where we actually are (resume as far along
		// as the position justifies rather than redoing earlier hops).
		for (int i = route.size() - 1; i >= 0; i--)
		{
			if (regionMatches(i))
			{
				if (i != legIdx) { legIdx = i; scratch.reset(); lastLegIdx = -1; }
				return;
			}
		}

		// Nowhere on the route (death respawn / relog) -> restart at the from-anywhere anchor (leg 0).
		if (legIdx != 0) { legIdx = 0; scratch.reset(); lastLegIdx = -1; }
	}

	private boolean regionMatches(int idx)
	{
		if (route == null || idx < 0 || idx >= route.size()) return false;
		int[] rs = route.leg(idx).regions();
		if (rs == null || rs.length == 0) return false;
		for (int r : rs) if (ctx.inRegion(r)) return true;
		return false;
	}
}
