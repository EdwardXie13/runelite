package net.runelite.client.plugins.autoBossCore.loot;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Generic ground-item planner. Walks the scene tile grid, finds {@link TileItem}s inside
 * an optional loot-zone (world-point filter), applies a value floor + a boss-specific
 * predicate, and returns a prioritised pickup list.
 * <p>
 * Not a plugin, not injected into the strategy's inventory logic -- the plan is just the
 * ordered list of {@link Target}s. The strategy decides when to execute and uses
 * {@link net.runelite.client.plugins.autoBossCore.combat.ClickRouter#takeGroundItem} to fire.
 */
@Slf4j
@Singleton
public class LootPlanner
{
	private final Client client;
	private final ItemManager itemManager;
	private final MirrorState mirror;

	@Inject
	public LootPlanner(Client client, ItemManager itemManager, MirrorState mirror)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.mirror = mirror;
	}

	public static final class Target
	{
		public final int itemId;
		public final int quantity;
		public final WorldPoint tile;
		public final int gpPerItem;
		public final String name;

		public Target(int itemId, int quantity, WorldPoint tile, int gpPerItem, String name)
		{
			this.itemId = itemId; this.quantity = quantity; this.tile = tile;
			this.gpPerItem = gpPerItem; this.name = name;
		}

		public int totalGp() { return gpPerItem * quantity; }
	}

	public static final class Request
	{
		/** Minimum GE value per single item to consider picking it up. */
		public final int minGpPerItem;
		/** Optional id-level allow-filter. Pass {@code x -> true} to accept all. */
		public final IntPredicate idAllowed;
		/** Optional zone filter -- only tiles satisfying this are considered. */
		public final java.util.function.Predicate<WorldPoint> inZone;
		/** Max items in the plan. */
		public final int maxTargets;

		public Request(int minGpPerItem, IntPredicate idAllowed,
		               java.util.function.Predicate<WorldPoint> inZone, int maxTargets)
		{
			this.minGpPerItem = minGpPerItem;
			this.idAllowed = idAllowed == null ? x -> true : idAllowed;
			this.inZone = inZone == null ? p -> true : inZone;
			this.maxTargets = maxTargets <= 0 ? Integer.MAX_VALUE : maxTargets;
		}
	}

	public List<Target> plan(Request req)
	{
		List<Target> out = new ArrayList<>();
		Scene scene = client.getTopLevelWorldView().getScene();
		if (scene == null) return out;
		int plane = client.getTopLevelWorldView().getPlane();
		Tile[][][] tiles = scene.getExtendedTiles();
		if (tiles == null || plane < 0 || plane >= tiles.length) return out;
		Tile[][] planeTiles = tiles[plane];
		if (planeTiles == null) return out;

		for (Tile[] col : planeTiles)
		{
			if (col == null) continue;
			for (Tile t : col)
			{
				if (t == null) continue;
				List<TileItem> items = t.getGroundItems();
				if (items == null || items.isEmpty()) continue;
				WorldPoint tile = t.getWorldLocation();
				if (!req.inZone.test(tile)) continue;
				for (TileItem it : items)
				{
					if (!req.idAllowed.test(it.getId())) continue;
					int price = priceOf(it.getId());
					if (price < req.minGpPerItem) continue;
					String name = nameOf(it.getId());
					out.add(new Target(it.getId(), it.getQuantity(), tile, price, name));
				}
			}
		}

		// Sort by total value descending so the engine takes richest items first.
		out.sort(Comparator.<Target>comparingInt(Target::totalGp).reversed());
		if (out.size() > req.maxTargets) return out.subList(0, req.maxTargets);
		return out;
	}

	/** Convenience: total GP of a plan. */
	public static int totalGp(List<Target> plan)
	{
		int total = 0;
		for (Target t : plan) total += t.totalGp();
		return total;
	}

	// -----------------------------------------------------------------

	protected int priceOf(int itemId)
	{
		try { return (int) itemManager.getItemPrice(itemId); }
		catch (Throwable t) { return 0; }
	}

	protected String nameOf(int itemId)
	{
		try
		{
			net.runelite.api.ItemComposition c = client.getItemDefinition(itemId);
			return c == null ? "" : c.getName();
		}
		catch (Throwable t) { return ""; }
	}
}
