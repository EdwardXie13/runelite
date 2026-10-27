package net.runelite.client.plugins.autoBossCore.travel;

import net.runelite.api.coords.WorldPoint;

import java.util.function.Predicate;

/**
 * Declarative completion test for a leg: "what does DONE actually mean for this interaction?" Kept separate
 * from the leg so the same leg kind (e.g. "interact with NPC") can finish on a region change, a bank
 * interface opening, a varbit flip, etc. -- the caller decides.
 */
@FunctionalInterface
public interface DoneSignal
{
	boolean met(TravelContext ctx);

	/** Done once the given map region is loaded. */
	static DoneSignal region(int regionId)        { return ctx -> ctx.inRegion(regionId); }

	/** Done once the player is standing on the exact tile. */
	static DoneSignal atTile(WorldPoint tile)      { return ctx -> ctx.atTile(tile); }

	/** Done once the bank interface is open. */
	static DoneSignal bankOpen()                   { return TravelContext::bankOpen; }

	/** Escape hatch for anything else (dialogue state, varbit, inventory, ...). */
	static DoneSignal custom(Predicate<TravelContext> p) { return p::test; }

	static DoneSignal and(DoneSignal a, DoneSignal b)    { return ctx -> a.met(ctx) && b.met(ctx); }
	static DoneSignal or(DoneSignal a, DoneSignal b)     { return ctx -> a.met(ctx) || b.met(ctx); }
}
