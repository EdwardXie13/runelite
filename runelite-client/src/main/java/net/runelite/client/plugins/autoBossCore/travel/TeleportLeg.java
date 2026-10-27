package net.runelite.client.plugins.autoBossCore.travel;

/**
 * Teleport to a region by casting a spell widget or breaking a tablet. Doubles as a route's "from-anywhere
 * anchor": placed first, it re-establishes a known position after a total displacement, because the runner
 * restarts at leg 0 when it can't localise the player anywhere on the route.
 */
final class TeleportLeg implements TravelLeg
{
	private static final int DEBOUNCE = 6;   // teleports have a cast time + run-out; don't re-spam fast

	enum Kind { SPELL, TAB }

	private final Kind kind;
	private final int widgetId;      // SPELL
	private final String option;
	private final String target;     // SPELL coloured target text
	private final int itemId;        // TAB
	private final int doneRegion;

	private TeleportLeg(Kind kind, int widgetId, String option, String target, int itemId, int doneRegion)
	{
		this.kind = kind; this.widgetId = widgetId; this.option = option;
		this.target = target; this.itemId = itemId; this.doneRegion = doneRegion;
	}

	static TeleportLeg spell(int widgetId, String option, String target, int doneRegion)
	{ return new TeleportLeg(Kind.SPELL, widgetId, option, target, -1, doneRegion); }

	static TeleportLeg tab(int itemId, String option, int doneRegion)
	{ return new TeleportLeg(Kind.TAB, -1, option, null, itemId, doneRegion); }

	public boolean done(TravelContext ctx, LegScratch s) { return ctx.inRegion(doneRegion); }

	public void act(TravelContext ctx, int tick, LegScratch s)
	{
		if (!ctx.actionReady(tick, DEBOUNCE)) return;
		if (kind == Kind.SPELL) ctx.router.castWidget(widgetId, option, target);
		else                    ctx.router.useInventoryItem(itemId, option);
		ctx.markAction(tick);
	}

	public int[] regions() { return new int[]{ doneRegion }; }
	public String label()  { return "travel: teleport -> region " + doneRegion; }
}
