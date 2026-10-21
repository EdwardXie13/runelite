package net.runelite.client.plugins.autoBossCore.loot;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemQuantityChanged;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Generic, event-driven ground-item tracker for autoBossCore. Mirrors RuneLite's
 * {@link ItemSpawned}/{@link ItemDespawned}/{@link ItemQuantityChanged} events into a live list of
 * {@link Entry}s so a boss strategy can see loot the instant it hits the ground -- no per-tick scene
 * scan. Modeled on the vorkathAuto ground-item tracker, lifted into the shared library so every boss
 * can reuse it. This class only DETECTS loot: per-boss loot HANDLING (which items, value floors, the
 * walk/pick-up, "this kill" vs. leftover gating) lives in each boss script and is wired in later.
 * <p>
 * The owning boss plugin must {@code eventBus.register(...)} this (and unregister on shutdown), the same
 * way it registers {@link net.runelite.client.plugins.autoBossCore.state.MirrorState}. Events arrive on
 * the client thread; the backing list is copy-on-write so a reader iterating a snapshot is always safe.
 */
@Slf4j
@Singleton
public class GroundItemTracker
{
	/** One stack of a single item id on a single tile. {@code quantity} is mutated in place as the stack
	 *  grows/shrinks; {@code tickSpawned} is the client tick it first appeared, so a consumer can separate
	 *  this kill's drops (tick >= the kill tick) from older leftover loot. */
	public static final class Entry
	{
		public final WorldPoint tile;
		public final int itemId;
		public int quantity;
		public final int tickSpawned;

		Entry(WorldPoint tile, int itemId, int quantity, int tickSpawned)
		{
			this.tile = tile;
			this.itemId = itemId;
			this.quantity = quantity;
			this.tickSpawned = tickSpawned;
		}
	}

	private final Client client;
	private final List<Entry> items = new CopyOnWriteArrayList<>();

	@Inject
	public GroundItemTracker(Client client)
	{
		this.client = client;
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		try
		{
			TileItem item = event.getItem();
			Tile tile = event.getTile();
			if (item == null || tile == null) return;
			WorldPoint wp = tile.getWorldLocation();
			if (wp == null) return;
			int id = item.getId();
			int qty = item.getQuantity();
			for (Entry e : items)
			{
				if (e.itemId == id && e.tile.equals(wp)) { e.quantity += qty; return; }
			}
			items.add(new Entry(wp, id, qty, client.getTickCount()));
		}
		catch (Throwable t) { log.debug("[groundItem] spawn", t); }
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		try
		{
			TileItem item = event.getItem();
			Tile tile = event.getTile();
			if (item == null || tile == null) return;
			WorldPoint wp = tile.getWorldLocation();
			if (wp == null) return;
			int id = item.getId();
			int qty = item.getQuantity();
			for (Entry e : items)
			{
				if (e.itemId == id && e.tile.equals(wp))
				{
					e.quantity -= qty;
					if (e.quantity <= 0) items.remove(e);
					return;
				}
			}
		}
		catch (Throwable t) { log.debug("[groundItem] despawn", t); }
	}

	@Subscribe
	public void onItemQuantityChanged(ItemQuantityChanged event)
	{
		try
		{
			TileItem item = event.getItem();
			Tile tile = event.getTile();
			if (item == null || tile == null) return;
			WorldPoint wp = tile.getWorldLocation();
			if (wp == null) return;
			int id = item.getId();
			int delta = event.getNewQuantity() - event.getOldQuantity();
			for (Entry e : items)
			{
				if (e.itemId == id && e.tile.equals(wp))
				{
					e.quantity += delta;
					if (e.quantity <= 0) items.remove(e);
					return;
				}
			}
			if (event.getNewQuantity() > 0)
				items.add(new Entry(wp, id, event.getNewQuantity(), client.getTickCount()));
		}
		catch (Throwable t) { log.debug("[groundItem] qtyChanged", t); }
	}

	// ----- queries for the (later) per-boss loot handling -----

	/** True if no tracked ground items remain. */
	public boolean isEmpty() { return items.isEmpty(); }

	/** Live snapshot of every tracked ground item (a copy; safe to iterate or mutate the returned list). */
	public List<Entry> all() { return new ArrayList<>(items); }

	/** Tracked items that first appeared on or after {@code sinceTick} -- this-kill drops vs. older leftovers. */
	public List<Entry> spawnedSince(int sinceTick)
	{
		List<Entry> out = new ArrayList<>();
		for (Entry e : items) if (e.tickSpawned >= sinceTick) out.add(e);
		return out;
	}

	/** Tracked items whose tile satisfies {@code zone} (e.g. a boss arena loot zone). Null zone = all. */
	public List<Entry> inZone(Predicate<WorldPoint> zone)
	{
		if (zone == null) return all();
		List<Entry> out = new ArrayList<>();
		for (Entry e : items) if (zone.test(e.tile)) out.add(e);
		return out;
	}

	/** Drop all tracked items. A boss calls this when leaving the arena / starting a fresh kill, since the
	 *  WorldPoints are only valid in the current scene (and instanced arenas reuse coordinates). */
	public void clear() { items.clear(); }
}
