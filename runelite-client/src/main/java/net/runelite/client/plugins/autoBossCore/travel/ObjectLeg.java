package net.runelite.client.plugins.autoBossCore.travel;

import net.runelite.api.GameObject;
import net.runelite.api.MenuAction;
import net.runelite.api.coords.WorldPoint;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Interact with a scene object (bank booth, portal nexus, cave entrance, ...). Same human model as
 * {@link NpcLeg}: walk the approach hint only while the object is not in the loaded scene, then latch +
 * wait a per-object random beat and fire the option. An optional tile filter pins a SPECIFIC object when
 * several share an id (e.g. the exact Lunar booth). Completion is the caller's {@link DoneSignal}.
 */
final class ObjectLeg implements TravelLeg
{
	private static final int WALK_DEBOUNCE = 4;
	private static final int ACT_DEBOUNCE  = 3;

	private final int[] ids;
	private final WorldPoint tileFilter;    // nullable: require the object on this exact tile
	private final MenuAction action;
	private final String option;
	private final WorldPoint approachHint;  // nullable
	private final int jitterMin, jitterMax;
	private final DoneSignal signal;

	ObjectLeg(int[] ids, WorldPoint tileFilter, MenuAction action, String option,
	          WorldPoint approachHint, int jitterMin, int jitterMax, DoneSignal signal)
	{
		this.ids = ids; this.tileFilter = tileFilter; this.action = action; this.option = option;
		this.approachHint = approachHint; this.jitterMin = jitterMin; this.jitterMax = jitterMax; this.signal = signal;
	}

	public boolean done(TravelContext ctx, LegScratch s) { return signal.met(ctx); }

	public void act(TravelContext ctx, int tick, LegScratch s)
	{
		GameObject go = ctx.object(ids, tileFilter);
		if (go == null)                                         // not in scene -> walk the hint to load it
		{
			s.clearLatch();
			walkHint(ctx, tick);
			return;
		}
		if (!s.latched()) s.latch(tick, rollJitter());
		if (tick - s.latchTick() < s.jitter())
		{
			walkHint(ctx, tick);
			return;
		}
		if (ctx.actionReady(tick, ACT_DEBOUNCE))
		{
			ctx.router.interactGameObject(go, action, option);
			ctx.markAction(tick);
		}
	}

	private void walkHint(TravelContext ctx, int tick)
	{
		if (approachHint != null && ctx.actionReady(tick, WALK_DEBOUNCE))
		{
			ctx.router.walkTo(approachHint);
			ctx.markAction(tick);
		}
	}

	private int rollJitter()
	{
		return jitterMax <= jitterMin ? Math.max(0, jitterMin)
			: ThreadLocalRandom.current().nextInt(jitterMin, jitterMax + 1);
	}

	public int[] regions()
	{
		WorldPoint r = tileFilter != null ? tileFilter : approachHint;
		return r != null ? new int[]{ r.getRegionID() } : new int[0];
	}
	public String label() { return "travel: " + option + " object"; }
}
