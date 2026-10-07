package net.runelite.client.plugins.autoBossCore.combat;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

/**
 * Tile-exact walking-path predictor for SHORT in-instance runs.
 *
 * <p>Breadth-first search over the LIVE client collision map, one tile per edge, using
 * {@link WorldArea#canTravelInDirection(WorldView, int, int, Predicate)} as the move test --
 * the game's own collision algorithm, which already handles directional walls and the
 * diagonal corner rule and reads {@code wv.getCollisionMaps()} each call. We therefore never
 * hand-roll collision; we only drive the BFS and reconstruct the route.
 *
 * <p>Scope: this is for small, open instance arenas (a boss room), NOT overworld travel. BFS
 * cost is uniform so the route is shortest in TILES (a diagonal is one tile, matching OSRS).
 * Bound the search with {@code inBounds} (keep it to the arena, which also keeps every query
 * inside the loaded scene) and {@code maxNodes}.
 *
 * <p>Caveats: (1) NPCs are not in the collision map -- block their footprints via
 * {@code passable}. (2) BFS returns A shortest route; where several equal-length routes exist
 * the game server may pick a different one, so predicted tiles can diverge on ties. For a
 * short arena hop re-evaluated every tick this is minor.
 *
 * <p>Adapted in spirit from the BSD-2-Clause Skretzo/shortest-path plugin's tile BFS, but run
 * over live scene collision instead of its bundled overworld collision-map dump.
 */
public final class TilePathPredictor
{
	private TilePathPredictor() {}

	// N, E, S, W, then diagonals. Orthogonal-first keeps straight runs natural; order only
	// breaks ties between equal-length routes.
	private static final int[][] DIRS =
		{{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

	/**
	 * Shortest tile path from {@code start} to {@code goal} (both inclusive) as the game would
	 * walk it over the current collision map, or an empty list if {@code goal} is unreachable
	 * within {@code maxNodes}.
	 *
	 * @param passable extra per-tile gate layered on collision: return {@code false} to block
	 *                 a tile (e.g. an NPC footprint). {@code null} == everything passable.
	 * @param inBounds limits which tiles the search may enter (e.g. the arena box). {@code null}
	 *                 == unbounded (only do this with a small {@code maxNodes}).
	 */
	public static List<WorldPoint> path(WorldView wv, WorldPoint start, WorldPoint goal,
		Predicate<WorldPoint> passable, Predicate<WorldPoint> inBounds, int maxNodes)
	{
		if (wv == null || start == null || goal == null) return Collections.emptyList();
		if (start.getPlane() != goal.getPlane()) return Collections.emptyList();
		if (start.equals(goal)) return Collections.singletonList(start);

		Predicate<WorldPoint> pass = passable == null ? (p -> true) : passable;
		Map<WorldPoint, WorldPoint> prev = new HashMap<>();
		ArrayDeque<WorldPoint> q = new ArrayDeque<>();
		q.add(start);
		prev.put(start, start);
		int visited = 0;
		boolean found = false;

		while (!q.isEmpty() && visited++ < maxNodes)
		{
			WorldPoint cur = q.poll();
			WorldArea area = new WorldArea(cur, 1, 1);
			for (int[] d : DIRS)
			{
				WorldPoint nxt = new WorldPoint(cur.getX() + d[0], cur.getY() + d[1], cur.getPlane());
				if (prev.containsKey(nxt)) continue;
				if (inBounds != null && !inBounds.test(nxt)) continue;
				if (!pass.test(nxt)) continue;
				if (!area.canTravelInDirection(wv, d[0], d[1], pass)) continue;
				prev.put(nxt, cur);
				if (nxt.equals(goal)) { found = true; q.clear(); break; }
				q.add(nxt);
			}
		}
		if (!found) return Collections.emptyList();

		LinkedList<WorldPoint> path = new LinkedList<>();
		for (WorldPoint c = goal; !c.equals(start); c = prev.get(c)) path.addFirst(c);
		path.addFirst(start);
		return path;
	}

	/**
	 * The tile the walker stands on {@code ticksAhead} ticks from now, moving {@code tilesPerTick}
	 * tiles each tick along {@code path} (clamped at the goal). {@code path} includes the start at
	 * index 0, so tick 0 is the start tile. Null if the path is empty.
	 */
	public static WorldPoint tileAfterTicks(List<WorldPoint> path, int ticksAhead, int tilesPerTick)
	{
		if (path == null || path.isEmpty()) return null;
		int idx = Math.max(0, ticksAhead) * Math.max(1, tilesPerTick);
		if (idx > path.size() - 1) idx = path.size() - 1;
		return path.get(idx);
	}
}
