package net.runelite.client.plugins.autoBossCore.combat;

import net.runelite.api.coords.WorldPoint;

import java.util.Optional;
import java.util.Set;

/**
 * Hazard scheduler. Boss strategies push {@link Hazard}s as they see them; the dodger
 * answers "do I need to move right now, and if so where to."
 * <p>
 * The scheduler model (vs reactive) matters for 1-tick projectiles / fast axes --
 * knowing a hazard N ticks ahead lets the walk click be sent on the right tick, where a
 * reactive dodger misses by one.
 */
public interface TileDodger
{
	/** Register a hazard. Idempotent -- adding the same npc twice is a no-op. */
	void addHazard(Hazard h);

	/** Called from the plugin's own GameTick handler. Ages out expired entries. */
	void onGameTick(int tickCounter);

	/** Drop everything -- useful on arena entry / exit. */
	void clearAll();

	/**
	 * Compute a dodge target.
	 * @param from              player's current tile
	 * @param allowed           allowed destination set (an arena bounding box, say). null = anywhere.
	 * @param planningHorizon   how many ticks forward to consider when scoring hazards
	 * @return {@code Optional.of(safeTile)} if the player is at risk and a dodge exists;
	 *         {@code Optional.empty()} if nothing to do (either safe or no safe tile found).
	 */
	Optional<WorldPoint> computeDodge(WorldPoint from, Set<WorldPoint> allowed, int planningHorizon);

	/**
	 * True if the given tile is projected to be dangerous within the next
	 * {@code horizon} ticks (inclusive). Used by strategies that pre-plan movement
	 * to a specific home tile rather than just reacting to the dodger's pick.
	 */
	boolean isTileDangerous(WorldPoint tile, int horizon);

	/**
	 * True if {@code tile} is dangerous AT ONE SPECIFIC absolute tick (not a range).
	 * Enables axe-skipping and other tile-swap moves: a tile may be currently hazardous
	 * but safe at tick+1 (the axe moves off it), so stepping there is legal.
	 */
	boolean isTileDangerousAt(WorldPoint tile, int absoluteTick);

	/** Debug: return the set of all tiles flagged as hazardous at the specific tick. */
	java.util.Set<WorldPoint> dumpDangerSetAt(int absoluteTick);
}
