package net.runelite.client.plugins.autoBossCore.combat;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default scheduling dodger. For each hazard, it projects which tiles are dangerous at
 * which future ticks (horizon-limited), then checks the player's current tile against
 * that set and -- if it's dangerous within the planning horizon -- picks the best legal
 * adjacent tile to walk to.
 * <p>
 * Scoring prefers:
 * <ol>
 *   <li>Not standing on anything dangerous in the horizon.</li>
 *   <li>Fewer dangerous tiles in the four cardinals around the candidate (penalise cul-de-sacs).</li>
 *   <li>Closer to the player's current tile (less wasted movement).</li>
 * </ol>
 */
@Slf4j
@Singleton
public class SchedulingTileDodger implements TileDodger
{
	private final MirrorState mirror;
	private final List<Hazard> hazards = new ArrayList<>();

	@Inject
	public SchedulingTileDodger(MirrorState mirror)
	{
		this.mirror = mirror;
	}

	@Override
	public void addHazard(Hazard h)
	{
		if (h == null) return;
		// Dedupe moving-npc entries by NPC identity.
		if (h.kind == Hazard.Kind.MOVING_NPC || h.kind == Hazard.Kind.NPC_CURRENT_TILE)
		{
			for (Hazard existing : hazards)
			{
				if (existing.kind == h.kind && existing.npc == h.npc) return;
			}
		}
		hazards.add(h);
	}

	@Override
	public void onGameTick(int tickCounter)
	{
		// Static-tile hazards expire once their atTick has passed.
		// NPC-based hazards expire when the NPC is no longer in the mirror's track map.
		Iterator<Hazard> it = hazards.iterator();
		while (it.hasNext())
		{
			Hazard h = it.next();
			switch (h.kind)
			{
				case STATIC_TILES_AT_TICK:
					if (tickCounter > h.atTick) it.remove();
					break;
				case MOVING_NPC:
				case NPC_CURRENT_TILE:
					if (h.npc == null || mirror.trackOf(h.npc) == null) it.remove();
					break;
			}
		}
	}

	@Override
	public void clearAll()
	{
		hazards.clear();
	}

	@Override
	public Optional<WorldPoint> computeDodge(WorldPoint from, Set<WorldPoint> allowed, int horizon)
	{
		if (from == null) return Optional.empty();
		int startTick = mirror.getTickCounter();

		// Build per-tick danger set: tick -> set of tiles that will be bad at that tick.
		Map<Integer, Set<WorldPoint>> dangerByTick = buildDangerMap(startTick, horizon);

		// Is the current tile dangerous within the horizon?
		boolean currentlyAtRisk = false;
		for (int t = startTick; t <= startTick + horizon; t++)
		{
			Set<WorldPoint> bad = dangerByTick.get(t);
			if (bad != null && bad.contains(from)) { currentlyAtRisk = true; break; }
		}
		if (!currentlyAtRisk) return Optional.empty();

		// Pick the best adjacent tile. Includes the player's own as a fallback.
		WorldPoint best = null;
		int bestScore = Integer.MIN_VALUE;
		for (WorldPoint cand : neighbors(from))
		{
			if (allowed != null && !allowed.contains(cand)) continue;
			int score = scoreCandidate(cand, from, startTick, horizon, dangerByTick);
			if (score > bestScore)
			{
				bestScore = score;
				best = cand;
			}
		}
		return Optional.ofNullable(best);
	}

	// -----------------------------------------------------------------
	// Internals.
	// -----------------------------------------------------------------

	protected Map<Integer, Set<WorldPoint>> buildDangerMap(int startTick, int horizon)
	{
		Map<Integer, Set<WorldPoint>> out = new LinkedHashMap<>();
		for (Hazard h : hazards)
		{
			switch (h.kind)
			{
				case STATIC_TILES_AT_TICK:
				{
					if (h.atTick < startTick) break;
					if (h.atTick > startTick + horizon) break;
					Set<WorldPoint> s = out.computeIfAbsent(h.atTick, k -> new HashSet<>());
					s.addAll(h.tiles);
					break;
				}
				case MOVING_NPC:
				{
					MirrorState.NpcTrack track = mirror.trackOf(h.npc);
					if (track == null || track.current == null) break;
					int dx = track.dx(), dy = track.dy();
					int size = npcSize(h.npc);
					for (int step = 0; step <= Math.min(h.lookaheadTicks, horizon); step++)
					{
						int originX = track.current.getX() + dx * step;
						int originY = track.current.getY() + dy * step;
						Set<WorldPoint> dmap = out.computeIfAbsent(startTick + step, k -> new HashSet<>());
						for (int ox = 0; ox < size; ox++)
							for (int oy = 0; oy < size; oy++)
								dmap.add(new WorldPoint(originX + ox, originY + oy, track.current.getPlane()));
					}
					break;
				}
				case NPC_CURRENT_TILE:
				{
					MirrorState.NpcTrack track = mirror.trackOf(h.npc);
					if (track == null || track.current == null) break;
					int size = npcSize(h.npc);
					Set<WorldPoint> dmap = out.computeIfAbsent(startTick, k -> new HashSet<>());
					for (int ox = 0; ox < size; ox++)
						for (int oy = 0; oy < size; oy++)
							dmap.add(new WorldPoint(track.current.getX() + ox, track.current.getY() + oy, track.current.getPlane()));
					break;
				}
			}
		}
		return out;
	}

	protected int scoreCandidate(WorldPoint cand, WorldPoint from,
	                             int startTick, int horizon,
	                             Map<Integer, Set<WorldPoint>> dangerByTick)
	{
		int score = 0;

		// Penalise danger on this tile at any point in the horizon (strong).
		for (int t = startTick; t <= startTick + horizon; t++)
		{
			Set<WorldPoint> bad = dangerByTick.get(t);
			if (bad == null) continue;
			if (bad.contains(cand))
			{
				// Earlier danger is worse than later danger (less time to react again).
				score -= 100 - (t - startTick);
			}
		}

		// Penalise cul-de-sacs: candidates with many bad neighbours score lower.
		int badNeighbours = 0;
		for (WorldPoint n : neighbors(cand))
		{
			for (int t = startTick; t <= startTick + horizon; t++)
			{
				Set<WorldPoint> bad = dangerByTick.get(t);
				if (bad != null && bad.contains(n)) { badNeighbours++; break; }
			}
		}
		score -= badNeighbours * 2;

		// Mild preference for staying close.
		int d = Math.abs(cand.getX() - from.getX()) + Math.abs(cand.getY() - from.getY());
		score -= d;

		return score;
	}

	protected static List<WorldPoint> neighbors(WorldPoint p)
	{
		List<WorldPoint> list = new ArrayList<>(5);
		list.add(p);
		list.add(new WorldPoint(p.getX() + 1, p.getY(),     p.getPlane()));
		list.add(new WorldPoint(p.getX() - 1, p.getY(),     p.getPlane()));
		list.add(new WorldPoint(p.getX(),     p.getY() + 1, p.getPlane()));
		list.add(new WorldPoint(p.getX(),     p.getY() - 1, p.getPlane()));
		return list;
	}

	protected int npcSize(NPC npc)
	{
		if (npc == null) return 1;
		try
		{
			NPCComposition c = npc.getComposition();
			return c == null ? 1 : Math.max(1, c.getSize());
		}
		catch (Throwable t) { return 1; }
	}

	@Override
	public boolean isTileDangerous(WorldPoint tile, int horizon)
	{
		if (tile == null) return false;
		int startTick = mirror.getTickCounter();
		Map<Integer, Set<WorldPoint>> dangerByTick = buildDangerMap(startTick, horizon);
		for (int t = startTick; t <= startTick + horizon; t++)
		{
			Set<WorldPoint> bad = dangerByTick.get(t);
			if (bad != null && bad.contains(tile)) return true;
		}
		return false;
	}

	@Override
	public java.util.Set<WorldPoint> dumpDangerSetAt(int absoluteTick)
	{
		int startTick = mirror.getTickCounter();
		int horizon = Math.max(0, absoluteTick - startTick);
		Map<Integer, Set<WorldPoint>> dangerByTick = buildDangerMap(startTick, horizon);
		Set<WorldPoint> bad = dangerByTick.get(absoluteTick);
		return bad == null ? java.util.Collections.emptySet() : bad;
	}

	@Override
	public boolean isTileDangerousAt(WorldPoint tile, int absoluteTick)
	{
		if (tile == null) return false;
		int startTick = mirror.getTickCounter();
		int horizon = Math.max(0, absoluteTick - startTick);
		Map<Integer, Set<WorldPoint>> dangerByTick = buildDangerMap(startTick, horizon);
		Set<WorldPoint> bad = dangerByTick.get(absoluteTick);
		return bad != null && bad.contains(tile);
	}
}
