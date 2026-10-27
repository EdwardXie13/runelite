package net.runelite.client.plugins.autoBossCore.travel;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.autoBossCore.combat.ClickRouter;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;
import net.runelite.client.plugins.autoBossCore.state.SceneScanner;
import net.runelite.client.plugins.autoBossCore.state.WidgetReader;

/**
 * The read/act surface a {@link TravelLeg} gets: live state queries plus the shared action debounce so legs
 * never spam clicks while the toon is walking or casting. One instance per {@link TravelRunner}.
 */
public final class TravelContext
{
	public final Client client;
	public final MirrorState mirror;
	public final ClickRouter router;

	private int lastActionTick = Integer.MIN_VALUE / 2;

	public TravelContext(Client client, MirrorState mirror, ClickRouter router)
	{
		this.client = client; this.mirror = mirror; this.router = router;
	}

	// --- reads ---------------------------------------------------------

	/** True if {@code regionId} is one of the loaded template chunks (handles instances, same as BankRun). */
	public boolean inRegion(int regionId)
	{
		int[] rs = client.getMapRegions();
		if (rs == null) return false;
		for (int r : rs) if (r == regionId) return true;
		return false;
	}

	public WorldPoint playerTile() { return mirror.getPlayerTile(); }

	public boolean atTile(WorldPoint tile)
	{
		WorldPoint me = mirror.getPlayerTile();
		return me != null && tile != null
			&& me.getX() == tile.getX() && me.getY() == tile.getY() && me.getPlane() == tile.getPlane();
	}

	public boolean bankOpen() { return WidgetReader.isBankOpen(client); }

	/** First scene object whose id is in {@code ids} and (if {@code tileOrNull} is set) sits on that tile. */
	public GameObject object(int[] ids, WorldPoint tileOrNull)
	{
		return SceneScanner.first(client, go -> {
			int id = go.getId();
			boolean idOk = false;
			for (int x : ids) if (x == id) { idOk = true; break; }
			if (!idOk) return false;
			return tileOrNull == null || tileOrNull.equals(go.getWorldLocation());
		});
	}

	/** First loaded NPC with the given id, or null if none is in the scene. */
	public NPC npc(int npcId)
	{
		for (NPC n : client.getNpcs()) if (n != null && n.getId() == npcId) return n;
		return null;
	}

	// --- shared action debounce ---------------------------------------

	public boolean actionReady(int tick, int gapTicks) { return tick - lastActionTick >= gapTicks; }
	public void    markAction(int tick)                { lastActionTick = tick; }
	public void    resetDebounce()                     { lastActionTick = Integer.MIN_VALUE / 2; }
}
