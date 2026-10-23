package net.runelite.client.plugins.autoBossCore.loot;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.autoBossCore.combat.ClickRouter;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.Predicate;

/**
 * Shared post-death loot pass for the DT2 bosses, lifted from the Whisperer. Drains the drop pile the
 * {@link GroundItemTracker} has seen since the kill, RICHEST item first, one ground-item "Take" every
 * {@link #ACTION_INTERVAL} ticks (the Take option auto-walks us onto the tile). Takes only what FITS -- a free
 * inventory slot, or a stackable we already hold -- so there is no inventory sacrifice. The owning boss feeds it
 * the kill tick and an optional arena filter and drives it from its own post-death window.
 * <p>
 * The boss plugin still owns the {@link GroundItemTracker} (registers it on the eventBus and clears it); this
 * collector reads the same singleton instance. {@code collect} returns a status string while still collecting and
 * {@code null} once the pass is done -- so a caller can {@code if (collect(...) != null) return;} to hold the tick.
 */
@Slf4j
@Singleton
public class LootCollector
{
	/** Ticks to wait after the kill for the drop pile to finish landing before the first grab. */
	private static final int SPAWN_GRACE_TICKS = 2;
	/** Minimum ticks between pickup clicks. Vorkath fires ONE action per tick (human-spam pace); match it. */
	private static final int ACTION_INTERVAL = 1;
	/** Give up the pass this many ticks after the kill, so a stuck pickup can't hang the caller forever. */
	private static final int TIMEOUT_TICKS = 60;

	private final Client client;
	private final ItemManager itemManager;
	private final GroundItemTracker groundItems;

	private int lastClickTick = -100;
	private WorldPoint lastTile = null;
	private int passDeathTick = Integer.MIN_VALUE;   // which kill the flag below belongs to
	private boolean sawLoot = false;                 // have we actually grabbed anything THIS pass yet

	@Inject
	public LootCollector(Client client, ItemManager itemManager, GroundItemTracker groundItems)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.groundItems = groundItems;
	}

	/**
	 * Run one tick of the loot pass. {@code deathTick} is the kill tick on {@link Client#getTickCount()}'s clock
	 * (the clock {@link GroundItemTracker} stamps spawns with); {@code inArena} keeps only drops whose tile is in
	 * this boss's arena (pass {@code null} to take anything that spawned since the kill); {@code router} is the
	 * boss's click router. Returns a short status string while there is still loot worth (and room) to grab, or
	 * {@code null} once the grace is over and the pile is clear / nothing fits / the timeout fires.
	 */
	public String collect(ClickRouter router, int deathTick, Predicate<WorldPoint> inArena)
	{
		if (router == null || groundItems == null || deathTick < 0) return null;
		if (deathTick != passDeathTick) { passDeathTick = deathTick; sawLoot = false; }   // new kill -> fresh pass
		int now = client.getTickCount();
		if (now - deathTick < SPAWN_GRACE_TICKS) return "loot: waiting for drop pile";   // let the drops land first
		if (now - deathTick > TIMEOUT_TICKS) return null;                                 // safety: never hang forever
		GroundItemTracker.Entry best = bestLoot(deathTick, inArena);
		if (best == null)
		{
			// If we've already grabbed loot this pass, an empty pile means we're DONE. If we haven't grabbed
			// anything yet, the drops just may not have landed (DT2 loot can lag the kill by several ticks), so
			// HOLD and keep checking until the timeout rather than bailing on tick one.
			if (sawLoot) return null;
			log.info("[loot] waiting for drops: now={} deathTick={} tracked={} sinceKill={}",
				now, deathTick, groundItems.all().size(), groundItems.spawnedSince(deathTick).size());
			return "loot: waiting for drops";
		}
		sawLoot = true;
		if (now - lastClickTick >= ACTION_INTERVAL || !best.tile.equals(lastTile))
		{
			log.info("[loot] take {} x{} @ {}", lootName(best.itemId), best.quantity, best.tile);
			router.takeGroundItem(best.itemId, best.tile, lootName(best.itemId));
			lastClickTick = now;
			lastTile = best.tile;
			return "loot: take " + lootName(best.itemId) + " @ " + best.tile;
		}
		return "loot: collecting";
	}

	/** Highest total-GE-value this-kill ground item (optionally arena-filtered) that we have room to take. */
	private GroundItemTracker.Entry bestLoot(int deathTick, Predicate<WorldPoint> inArena)
	{
		GroundItemTracker.Entry best = null;
		long bestGp = Long.MIN_VALUE;
		for (GroundItemTracker.Entry e : groundItems.spawnedSince(deathTick))
		{
			if (inArena != null && e.tile != null && !inArena.test(e.tile)) continue;
			if (!hasRoomFor(e.itemId)) continue;
			long gp = itemPrice(e.itemId) * Math.max(1, e.quantity);
			if (gp > bestGp) { bestGp = gp; best = e; }
		}
		return best;
	}

	/** Room for one more of {@code itemId}: any free inventory slot, or (full) a stackable we already hold. */
	private boolean hasRoomFor(int itemId)
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		if (inv == null) return false;
		int used = 0;
		boolean present = false;
		for (Item it : inv.getItems())
		{
			if (it == null || it.getId() < 0) continue;
			used++;
			if (it.getId() == itemId) present = true;
		}
		if (used < 28) return true;
		if (!present) return false;
		try { ItemComposition c = client.getItemDefinition(itemId); return c != null && c.isStackable(); }
		catch (Throwable t) { return false; }
	}

	private long itemPrice(int itemId)
	{
		try { return itemManager == null ? 0L : itemManager.getItemPrice(itemId); }
		catch (Throwable t) { return 0L; }
	}

	private String lootName(int itemId)
	{
		try { ItemComposition c = client.getItemDefinition(itemId); return c == null ? "" : c.getName(); }
		catch (Throwable t) { return ""; }
	}
}
