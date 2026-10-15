package net.runelite.client.plugins.autoBossCore.combat;

import java.util.List;
import java.util.function.Predicate;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

/**
 * Predicts what happens if we click-ATTACK a target from a given tile: do we attack in place, or does the
 * engine pull us in first -- and if so, exactly which tile we end up attacking from.
 *
 * <p>Model (matches live OSRS behaviour):
 * <ol>
 *   <li>If our current tile is already an attack position (in range AND line of sight), we attack in place --
 *       {@code willMove == false}.</li>
 *   <li>Otherwise the engine paths us TOWARD the target (not toward the range edge) over the collision map,
 *       and each tick moves our full {@code tilesPerTick} (2 running, 1 walking) before re-checking whether we
 *       can attack. So we land on the first post-step tile that is in range + LoS. This is why, with run ON, a
 *       weapon of range 10 fired from the 11th tile lands us on the 9th (overshoot past the range edge), while
 *       with run OFF it lands on the 10th. The per-tick step reproduces that exactly.</li>
 * </ol>
 *
 * <p>Obstacles: the path comes from {@link TilePathPredictor}'s BFS over {@link WorldArea#canTravelInDirection}
 * (walls, directional blocks, the diagonal-corner rule) with an extra {@code blocksMove} gate for things not in
 * the collision map -- e.g. the Whisperer's floating columns, which are NPCs. LoS uses the scene's own
 * {@link WorldArea#hasLineOfSightTo} (walls, and any NPC the server baked into the flags) AND an optional
 * {@code blocksLos} occluder gate for NPCs it doesn't -- again the columns. The target itself is deliberately
 * NOT blocked (the Whisperer blocks neither movement nor shots), so the path walks straight through her.
 *
 * <p>Caveats inherited from the BFS: NPCs aren't in the collision map (pass their footprints via
 * {@code blocksMove}); on equal-length routes the server may pick a different tie, so a predicted tile can
 * diverge on ties -- minor for a short arena hop re-checked every tick.
 */
public final class AttackPredictor
{
	private AttackPredictor() {}

	/** Outcome of {@link #predict}. */
	public static final class Result
	{
		/** True if attacking would MOVE us (a pull-in); false = attack from where we stand. */
		public final boolean willMove;
		/** The tile we would actually attack from ({@code == from} when {@code !willMove}); null if no attack
		 *  position was found within the search bound. */
		public final WorldPoint attackFrom;
		/** Movement ticks before the first attack (0 when already attackable). */
		public final int ticksToAttack;
		/** True if an attack position was found (path reached one within {@code maxNodes}). */
		public final boolean reachable;

		Result(boolean willMove, WorldPoint attackFrom, int ticksToAttack, boolean reachable)
		{
			this.willMove = willMove;
			this.attackFrom = attackFrom;
			this.ticksToAttack = ticksToAttack;
			this.reachable = reachable;
		}

		@Override
		public String toString()
		{
			return "AttackPredict{willMove=" + willMove + " from=" + attackFrom
				+ " ticks=" + ticksToAttack + " reachable=" + reachable + "}";
		}
	}

	/**
	 * @param wv           the top-level world view (collision + scene).
	 * @param from         our current tile (mirror / player-tile space).
	 * @param target       the target's footprint.
	 * @param rangeTiles   our weapon's attack range (Chebyshev tiles).
	 * @param tilesPerTick 2 if run is on AND we have energy, else 1.
	 * @param blocksMove   extra per-tile movement blockers not in the collision map (column footprints); null = none.
	 * @param blocksLos    extra per-tile LoS occluders not in the scene flags (column footprints); null = none.
	 * @param inBounds     limits the search to the arena; null = unbounded (use a small maxNodes).
	 * @param maxNodes     BFS node cap.
	 */
	public static Result predict(
		WorldView wv, WorldPoint from, WorldArea target, int rangeTiles, int tilesPerTick,
		Predicate<WorldPoint> blocksMove, Predicate<WorldPoint> blocksLos,
		Predicate<WorldPoint> inBounds, int maxNodes)
	{
		if (wv == null || from == null || target == null) return new Result(false, from, 0, false);
		int tpt = Math.max(1, tilesPerTick);

		// Already attackable -> no pull.
		if (isAttackPos(wv, from, target, rangeTiles, blocksLos))
			return new Result(false, from, 0, true);

		// Path TOWARD the target (its nearest footprint tile), routing around movement blockers. The target is
		// passable unless the caller lists it, so the path walks right up to / under it; we stop early below.
		// TilePathPredictor's predicate is "true == WALKABLE", so invert blocksMove (true == blocked).
		WorldPoint goal = nearestFootprintTile(from, target);
		Predicate<WorldPoint> walkable = blocksMove == null ? null : (p -> !blocksMove.test(p));
		List<WorldPoint> path = TilePathPredictor.path(wv, from, goal, walkable, inBounds, maxNodes);
		if (path.isEmpty()) return new Result(true, null, 0, false);

		// Walk the path tilesPerTick per tick; land on the FIRST post-step tile that is an attack position.
		// (This is where run-overshoot appears: tpt=2 skips the range-edge tile and lands one closer.)
		int last = path.size() - 1;
		int idx = 0, ticks = 0;
		while (idx < last)
		{
			idx = Math.min(idx + tpt, last);
			ticks++;
			WorldPoint pos = path.get(idx);
			if (isAttackPos(wv, pos, target, rangeTiles, blocksLos))
				return new Result(true, pos, ticks, true);
		}
		// Reached the target without passing an attack position (shouldn't normally happen -- an adjacent tile
		// is in range). Report the path end so the caller sees "would move, lands here".
		return new Result(true, path.get(last), ticks, true);
	}

	/** A tile is an attack position if it's in range (Chebyshev) AND has LoS -- scene flags plus our occluders. */
	private static boolean isAttackPos(WorldView wv, WorldPoint tile, WorldArea target, int range,
		Predicate<WorldPoint> blocksLos)
	{
		if (target.distanceTo(tile) > range) return false;
		if (!new WorldArea(tile, 1, 1).hasLineOfSightTo(wv, target)) return false;
		if (blocksLos != null && losBlockedByOccluder(tile, target, blocksLos)) return false;
		return true;
	}

	/** The target footprint tile nearest {@code from} (clamp {@code from} into the footprint box). */
	private static WorldPoint nearestFootprintTile(WorldPoint from, WorldArea t)
	{
		int gx = clamp(from.getX(), t.getX(), t.getX() + t.getWidth() - 1);
		int gy = clamp(from.getY(), t.getY(), t.getY() + t.getHeight() - 1);
		return new WorldPoint(gx, gy, t.getPlane());
	}

	/**
	 * Approximate LoS occlusion by injected blockers (NPC footprints the scene flags don't carry): walk a
	 * supercover grid line from {@code tile} to the target's nearest footprint tile and return true if any
	 * INTERMEDIATE cell is a blocker. Endpoints are excluded. This supplements {@link WorldArea#hasLineOfSightTo}
	 * (which handles walls / baked NPCs); if the blockers are already in the flags this just never fires.
	 */
	private static boolean losBlockedByOccluder(WorldPoint tile, WorldArea target, Predicate<WorldPoint> blocksLos)
	{
		WorldPoint tgt = nearestFootprintTile(tile, target);
		int x0 = tile.getX(), y0 = tile.getY(), x1 = tgt.getX(), y1 = tgt.getY(), plane = tile.getPlane();
		int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
		int steps = Math.max(dx, dy);
		if (steps <= 1) return false;                 // adjacent / same tile -> nothing between
		for (int i = 1; i < steps; i++)               // intermediate samples only
		{
			int cx = x0 + Math.round((float) (x1 - x0) * i / steps);
			int cy = y0 + Math.round((float) (y1 - y0) * i / steps);
			WorldPoint c = new WorldPoint(cx, cy, plane);
			if (c.getX() == x0 && c.getY() == y0) continue;
			if (c.getX() == x1 && c.getY() == y1) continue;
			if (blocksLos.test(c)) return true;
		}
		return false;
	}

	private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
