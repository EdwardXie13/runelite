package net.runelite.client.plugins.dt2WhispererAuto;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Traced OVERWORLD arena boundary + interior (from the arena wall tool's whispererArena.json). Plane 0.
 * Used to keep dodge movement from clicking outside the walkable arena.
 * <p>
 * The interior is flood-filled once from the arena centre (lazy, cached) -- the set of non-wall tiles the
 * fill reaches, i.e. the walkable floor. SAFETY: the clamp is a no-op unless our OWN tile is recognised as
 * interior, so if the live coordinate space ever differs from these traced tiles (e.g. an instanced
 * overworld reporting different numbers) the feature quietly disables itself instead of breaking movement.
 */
final class WhispererArena
{
	static final int PLANE = 0;

	/** The fight start tile (where P stands): we walk here, then wake the Whisperer. In the same traced
	 *  overworld space as the walls. */
	static final WorldPoint START_TILE = new WorldPoint(2656, 6367, PLANE);

	/** The Whisperer's CENTRE tile (she is stationary): 5 tiles due north of the player start. Her 3x3
	 *  footprint is therefore x 2655-2657, y 6368-6370. Reference for her overworld position. */
	static final WorldPoint WHISPERER_CENTRE = new WorldPoint(2656, 6369, PLANE);

	private static final int[][] WALLS = {
		{2642,6361}, {2642,6362}, {2642,6363}, {2642,6364}, {2642,6365}, {2642,6366}, {2642,6367}, {2642,6368},
		{2642,6369}, {2642,6370}, {2643,6360}, {2643,6371}, {2643,6378}, {2644,6360}, {2644,6371}, {2644,6372},
		{2644,6373}, {2644,6374}, {2644,6375}, {2644,6376}, {2644,6377}, {2644,6379}, {2645,6360}, {2645,6380},
		{2646,6360}, {2646,6380}, {2647,6356}, {2647,6357}, {2647,6358}, {2647,6359}, {2647,6360}, {2647,6381},
		{2648,6355}, {2648,6382}, {2649,6354}, {2649,6382}, {2650,6353}, {2650,6383}, {2651,6352}, {2651,6384},
		{2652,6352}, {2652,6384}, {2653,6352}, {2653,6384}, {2654,6352}, {2654,6384}, {2655,6352}, {2655,6384},
		{2656,6352}, {2656,6384}, {2657,6352}, {2657,6384}, {2658,6352}, {2658,6384}, {2659,6353}, {2659,6384},
		{2660,6353}, {2660,6384}, {2661,6353}, {2661,6384}, {2662,6353}, {2662,6383}, {2663,6354}, {2663,6382},
		{2664,6355}, {2664,6382}, {2665,6355}, {2665,6380}, {2665,6381}, {2666,6356}, {2666,6357}, {2666,6358},
		{2666,6359}, {2666,6360}, {2666,6378}, {2666,6379}, {2667,6360}, {2667,6378}, {2668,6361}, {2668,6362},
		{2668,6363}, {2668,6364}, {2668,6365}, {2668,6366}, {2668,6377}, {2669,6366}, {2669,6377}, {2670,6367},
		{2670,6368}, {2670,6369}, {2670,6370}, {2670,6371}, {2670,6372}, {2670,6373}, {2670,6374}, {2670,6375},
		{2670,6376},
	};

	private static Set<Long> interior;   // packed (x,y) of walkable interior tiles

	private WhispererArena() {}

	private static long key(int x, int y) { return ((long) x << 20) | (y & 0xFFFFF); }

	private static synchronized Set<Long> interior()
	{
		if (interior != null) return interior;
		Set<Long> walls = new HashSet<>();
		int minx = Integer.MAX_VALUE, maxx = Integer.MIN_VALUE, miny = Integer.MAX_VALUE, maxy = Integer.MIN_VALUE;
		for (int[] w : WALLS)
		{
			walls.add(key(w[0], w[1]));
			minx = Math.min(minx, w[0]); maxx = Math.max(maxx, w[0]);
			miny = Math.min(miny, w[1]); maxy = Math.max(maxy, w[1]);
		}
		Set<Long> inside = new HashSet<>();
		int sx = (minx + maxx) / 2, sy = (miny + maxy) / 2;   // arena centre seed
		ArrayDeque<int[]> q = new ArrayDeque<>();
		if (!walls.contains(key(sx, sy))) { inside.add(key(sx, sy)); q.add(new int[]{sx, sy}); }
		int[][] nb = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!q.isEmpty())
		{
			int[] c = q.poll();
			for (int[] d : nb)
			{
				int nx = c[0] + d[0], ny = c[1] + d[1];
				if (nx < minx - 1 || nx > maxx + 1 || ny < miny - 1 || ny > maxy + 1) continue;
				long k = key(nx, ny);
				if (walls.contains(k) || inside.contains(k)) continue;
				inside.add(k); q.add(new int[]{nx, ny});
			}
		}
		interior = inside;
		return interior;
	}

	/** True if {@code t} is a walkable interior tile of the mapped arena (plane 0). */
	static boolean isInside(WorldPoint t)
	{
		return t != null && t.getPlane() == PLANE && interior().contains(key(t.getX(), t.getY()));
	}

	/**
	 * Keep a dodge destination inside the arena. If {@code from} is inside and {@code target} is outside,
	 * step {@code target} back toward {@code from} until it's inside and return that tile. If {@code from}
	 * is NOT recognised as interior (wrong plane / coordinate space / not in the mapped arena), returns
	 * {@code target} unchanged -- the clamp only acts where the arena map actually applies.
	 */
	static WorldPoint clampInside(WorldPoint target, WorldPoint from)
	{
		if (target == null || from == null) return target;
		if (!isInside(from)) return target;        // space mismatch / outside the map -> don't interfere
		if (isInside(target)) return target;
		int x = target.getX(), y = target.getY();
		int dx = Integer.signum(from.getX() - x), dy = Integer.signum(from.getY() - y);
		for (int i = 0; i < 64 && !(x == from.getX() && y == from.getY()); i++)
		{
			x += dx; y += dy;
			WorldPoint t = new WorldPoint(x, y, target.getPlane());
			if (isInside(t)) return t;
		}
		return from;   // nothing inside along the way -> stay put rather than click outside
	}
}
