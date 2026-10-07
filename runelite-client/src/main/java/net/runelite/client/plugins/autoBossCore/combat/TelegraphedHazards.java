package net.runelite.client.plugins.autoBossCore.combat;

import net.runelite.api.coords.WorldPoint;

/**
 * Generic helper for FALLING / telegraphed tile hazards: a spotanim appears on a tile while
 * the attack is still in flight (a rock falling, a spike rising, a blood pool forming), and
 * the tile only actually DAMAGES at impact -- not while the telegraph is still playing, and
 * not after it has resolved.
 *
 * <p>The naive approach of marking the tile dangerous for a fixed window from "now" is wrong
 * on both ends: it blocks the tile during the harmless telegraph, and it keeps blocking it
 * after the hazard has already landed. Instead, each boss registers every falling hazard it
 * sees with a TIME TO IMPACT; this computes the single impact tick from when the spotanim
 * spawned and marks the tile dangerous only for the brief impact window.
 *
 * <p>The impact tick is derived from the spotanim's start cycle, so re-scanning the same
 * hazard on later ticks yields the SAME impact tick (the window never slides forward). Once
 * impact is in the past nothing is added, so a landed hazard's tile reads safe immediately.
 */
public final class TelegraphedHazards
{
	private TelegraphedHazards() {}

	/** OSRS client cycles per game tick (0.6s / 20ms). */
	public static final int CYCLES_PER_TICK = 30;

	/**
	 * Register one falling/telegraphed hazard at its computed impact tick.
	 *
	 * @param dodger            hazard sink
	 * @param nowTick           current game tick
	 * @param clientGameCycle   {@code client.getGameCycle()} this tick
	 * @param tile              the hazard tile (same coord space the dodger/player use)
	 * @param gfxStartCycle     the spotanim's {@code getStartCycle()} (its spawn cycle)
	 * @param timeToImpactTicks ticks from spawn to impact for THIS hazard type (boss-supplied)
	 * @param activeTicks       ticks the tile damages starting at impact (>=1; usually 1)
	 * @param label             diagnostic label
	 * @return the absolute impact tick. If it is in the past, nothing was added (tile safe).
	 */
	public static int addFalling(TileDodger dodger, int nowTick, int clientGameCycle,
		WorldPoint tile, int gfxStartCycle, int timeToImpactTicks, int activeTicks,
		boolean markWholeFall, String label)
	{
		// floorDiv, NOT '/': the spotanim appears in the scene a tick or two BEFORE its
		// start cycle, so (clientGameCycle - gfxStartCycle) is negative then, and truncating
		// division toward zero would shift the spawn tick by one and make impactTick unstable.
		int ticksSinceStart = Math.floorDiv(clientGameCycle - gfxStartCycle, CYCLES_PER_TICK);
		int impactTick = nowTick - ticksSinceStart + timeToImpactTicks;
		if (tile != null && impactTick >= nowTick)
		{
			// markWholeFall: flag the tile for the ENTIRE observed fall (now -> impact) plus the
			// active window, so we ALWAYS dodge while a rock is in the air toward it -- robust even
			// when the GFX renders late and its computed impact is a tick or two off. When false,
			// flag only the impact window (pass-through during the fall stays allowed).
			int start = markWholeFall ? nowTick : impactTick;
			int end = impactTick + Math.max(1, activeTicks) - 1;
			for (int tt = start; tt <= end; tt++)
			{
				dodger.addHazard(Hazard.staticTile(tile, tt, label));
			}
		}
		return impactTick;
	}
}
