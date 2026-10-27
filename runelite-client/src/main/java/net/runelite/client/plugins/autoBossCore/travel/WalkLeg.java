package net.runelite.client.plugins.autoBossCore.travel;

import net.runelite.api.coords.WorldPoint;

/** Pure positioning: walk to a tile and finish when standing on it. No target interaction. */
final class WalkLeg implements TravelLeg
{
	private static final int WALK_DEBOUNCE = 4;
	private final WorldPoint tile;

	WalkLeg(WorldPoint tile) { this.tile = tile; }

	public boolean done(TravelContext ctx, LegScratch s) { return ctx.atTile(tile); }

	public void act(TravelContext ctx, int tick, LegScratch s)
	{
		if (ctx.actionReady(tick, WALK_DEBOUNCE)) { ctx.router.walkTo(tile); ctx.markAction(tick); }
	}

	public int[] regions() { return new int[]{ tile.getRegionID() }; }
	public String label()  { return "travel: walk -> " + tile.getX() + "," + tile.getY(); }
}
