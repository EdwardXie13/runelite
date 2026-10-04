package net.runelite.client.plugins.autoBossCore.combat;

import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.List;

/**
 * A thing to not stand on. Covers the three common mechanic shapes seen across DT2:
 * <ul>
 *   <li>{@link Kind#STATIC_TILES_AT_TICK} -- fixed tiles that will damage at a specific tick
 *       (spike tiles, blood pools after a telegraph).</li>
 *   <li>{@link Kind#MOVING_NPC} -- an NPC whose extrapolated path at its current velocity
 *       is dangerous (Vardorvis axes).</li>
 *   <li>{@link Kind#NPC_CURRENT_TILE} -- an NPC whose present tile is the hazard, re-read
 *       each tick (chasing adds like Vardorvis blood tornadoes).</li>
 * </ul>
 * Factory methods document the intent; direct field use is allowed.
 */
public final class Hazard
{
	public enum Kind { STATIC_TILES_AT_TICK, MOVING_NPC, NPC_CURRENT_TILE }

	public final Kind kind;
	public final List<WorldPoint> tiles;     // STATIC_TILES_AT_TICK
	public final int atTick;                 // STATIC_TILES_AT_TICK: hits at this tick
	public final NPC npc;                    // MOVING_NPC / NPC_CURRENT_TILE
	public final int lookaheadTicks;         // MOVING_NPC: how far forward to project
	public final String label;

	private Hazard(Kind k, List<WorldPoint> tiles, int atTick, NPC npc, int lookahead, String label)
	{
		this.kind           = k;
		this.tiles          = tiles == null ? Collections.emptyList() : Collections.unmodifiableList(tiles);
		this.atTick         = atTick;
		this.npc            = npc;
		this.lookaheadTicks = lookahead;
		this.label          = label == null ? "" : label;
	}

	public static Hazard staticTile(WorldPoint t, int atTick, String label)
	{
		return new Hazard(Kind.STATIC_TILES_AT_TICK, Collections.singletonList(t), atTick, null, 0, label);
	}

	public static Hazard staticTiles(List<WorldPoint> tiles, int atTick, String label)
	{
		return new Hazard(Kind.STATIC_TILES_AT_TICK, tiles, atTick, null, 0, label);
	}

	public static Hazard movingNpc(NPC npc, int lookaheadTicks, String label)
	{
		return new Hazard(Kind.MOVING_NPC, null, 0, npc, lookaheadTicks, label);
	}

	public static Hazard npcCurrent(NPC npc, String label)
	{
		return new Hazard(Kind.NPC_CURRENT_TILE, null, 0, npc, 0, label);
	}
}
