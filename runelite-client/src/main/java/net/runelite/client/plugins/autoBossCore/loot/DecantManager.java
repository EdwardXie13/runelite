package net.runelite.client.plugins.autoBossCore.loot;

import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.autoBossCore.combat.ClickRouter;
import net.runelite.client.plugins.autoBossCore.consume.SupplyProfile;

/**
 * Shared between-fights decant / inventory-tidy pass for the DT2 bosses. One action per tick (no spam),
 * driven entirely by the boss's {@link SupplyProfile}: it reads the primary food id (the make-room filler)
 * and every potion type's 1..4-dose id list from the profile, so it stays boss-agnostic.
 *
 * <p>Each {@link #tick} does at most ONE of, in priority order:
 * <ol>
 *   <li>advance an in-progress MAKE-ROOM juggle (see below);</li>
 *   <li>drop an empty vial (229) left behind by a combine;</li>
 *   <li>consolidate partial potion doses into FULL (4-dose) vials;</li>
 *   <li>start a MAKE-ROOM juggle -- inventory full + we hold a partial potion + the SAME potion is on the
 *       floor: drop a food filler, grab the floor potion, consolidate, drop the empty vial, re-grab the filler.</li>
 * </ol>
 * Returns {@code true} while it still has work (caller holds the tick), {@code false} once idle.
 *
 * <p>Only safe to drive from a between-fights / cleanup window -- the make-room juggle walks to the floor
 * potion and back, so it must not run mid-fight.
 */
public final class DecantManager
{
	private static final int EMPTY_VIAL_ITEM_ID = 229;
	private static final int COOLDOWN_TICKS = 1;        // at most one action per tick
	private static final int ROOM_TIMEOUT_TICKS = 40;   // abort a stuck make-room juggle

	/** Item ids the bot must NEVER drop (protected items: gear, quest items, etc.). This guards EVERY drop,
	 *  including the make-room filler -- if the only candidate is blacklisted the juggle is skipped. Populate
	 *  with raw ids or ItemID.* constants (add the net.runelite.api gameval ItemID import when you use them). */
	private static final int[] DROP_BLACKLIST = {
		// add protected item ids here
	};

	private final Client client;
	private final SupplyProfile profile;

	private int lastActionTick = -100;

	private enum Room { NONE, GRAB, CONSOLIDATE, DROP_VIAL, REGRAB }
	private Room room = Room.NONE;
	private int roomStartTick = -100;
	private int fillerId = -1;
	private WorldPoint fillerTile = null;
	private int[] roomDoses = null;
	private int roomFloorId = -1;
	private WorldPoint roomFloorTile = null;

	public DecantManager(Client client, SupplyProfile profile)
	{
		this.client = client;
		this.profile = profile;
	}

	/** Clear any in-progress state. Call from the boss's start()/stop() between trips. */
	public void reset()
	{
		room = Room.NONE;
		roomStartTick = -100;
		fillerId = -1; fillerTile = null; roomDoses = null; roomFloorId = -1; roomFloorTile = null;
		lastActionTick = -100;
	}

	/** True while a make-room juggle is mid-sequence (caller should keep holding the tick). */
	public boolean busy() { return room != Room.NONE; }

	/** Run one decant/tidy action. Returns true while busy (hold the tick), false once idle. */
	public boolean tick(ClickRouter router, GroundItemTracker ground, int tick)
	{
		return tick(router, ground, tick, true);
	}

	/** As {@link #tick(ClickRouter, GroundItemTracker, int)}, but {@code allowMakeRoom=false} suppresses the
	 *  make-room juggle (which WALKS to a floor potion) -- use it when the caller is moving and must not be
	 *  pulled off its route. Drop-empty-vial and potion consolidation (no walking) still run. */
	public boolean tick(ClickRouter router, GroundItemTracker ground, int tick, boolean allowMakeRoom)
	{
		if (router == null) return busy();
		if (tick - lastActionTick < COOLDOWN_TICKS) return busy();   // pace: one action per tick

		if (room != Room.NONE) return advanceRoom(router, ground, tick);
		if (dropEmptyVial(tick)) return true;
		if (consolidateAll(tick)) return true;
		if (allowMakeRoom && tryStartMakeRoom(router, ground, tick)) return true;
		return false;
	}

	// ---- inventory helpers ----------------------------------------------

	private Item[] inv()
	{
		ItemContainer c = client.getItemContainer(InventoryID.INVENTORY);
		return c == null ? null : c.getItems();
	}

	private int freeSlots()
	{
		Item[] it = inv();
		if (it == null) return 0;
		int used = 0;
		for (Item i : it) if (i != null && i.getId() >= 0) used++;
		return 28 - used;
	}

	private int countItem(int id)
	{
		Item[] it = inv();
		if (it == null) return 0;
		int n = 0;
		for (Item i : it) if (i != null && i.getId() == id) n++;
		return n;
	}

	private String nameOf(int id)
	{
		try { ItemComposition c = client.getItemDefinition(id); return c == null ? "" : c.getName(); }
		catch (Throwable t) { return ""; }
	}

	private WorldPoint playerTile()
	{
		Player p = client.getLocalPlayer();
		return p == null ? null : p.getWorldLocation();
	}

	private int doseOf(int itemId, int[] doseIds)
	{
		for (int k = 0; k < doseIds.length; k++) if (doseIds[k] == itemId) return k + 1;
		return 0;
	}

	/** Count of non-full (1..3 dose) vials of this potion type currently held. */
	private int partialsOfType(int[] doseIds)
	{
		Item[] it = inv();
		if (it == null) return 0;
		int n = 0;
		for (Item i : it)
		{
			if (i == null) continue;
			int d = doseOf(i.getId(), doseIds);
			if (d >= 1 && d <= 3) n++;
		}
		return n;
	}

	// ---- raw actions (each sets lastActionTick) -------------------------

	private boolean isBlacklisted(int itemId)
	{
		for (int b : DROP_BLACKLIST) if (b == itemId) return true;
		return false;
	}

	private void dropItemId(int itemId, int tick)
	{
		if (isBlacklisted(itemId)) return;   // protected -- never drop
		Item[] it = inv();
		if (it == null) return;
		for (int i = 0; i < it.length; i++)
		{
			if (it[i] == null || it[i].getId() != itemId) continue;
			String name = ClickRouter.ITEM_COLOR_PREFIX + nameOf(itemId) + ClickRouter.ITEM_COLOR_SUFFIX;
			client.menuAction(i, ClickRouter.INVENTORY_WIDGET, MenuAction.CC_OP, 7, itemId, "Drop", name);
			lastActionTick = tick;
			return;
		}
	}

	private boolean dropEmptyVial(int tick)
	{
		if (countItem(EMPTY_VIAL_ITEM_ID) <= 0) return false;
		dropItemId(EMPTY_VIAL_ITEM_ID, tick);
		return true;
	}

	/** Consolidate every potion type the profile carries; issues at most ONE combine. True if it acted. */
	private boolean consolidateAll(int tick)
	{
		for (SupplyProfile.DrinkKind k : SupplyProfile.DrinkKind.values())
		{
			SupplyProfile.PotionEntry pe = profile.potion(k);
			if (pe == null) continue;
			if (consolidate(pe.doseIds, tick)) return true;
		}
		return false;
	}

	/**
	 * Pack doses of one potion type into FULL (4-dose) vials. While TWO OR MORE non-full (1..3) vials exist,
	 * pour the smallest-dose one into the largest-dose one. Each pour strictly cuts the non-full count, so it
	 * ends at <=1 partial vial. One combine per call; full vials are never touched.
	 */
	private boolean consolidate(int[] doseIds, int tick)
	{
		Item[] items = inv();
		if (items == null) return false;
		int srcSlot = -1, srcId = -1, srcDose = Integer.MAX_VALUE;
		int tgtSlot = -1, tgtId = -1, tgtDose = -1;
		int partials = 0;
		for (int i = 0; i < items.length; i++)
		{
			if (items[i] == null) continue;
			int d = doseOf(items[i].getId(), doseIds);
			if (d <= 0 || d >= 4) continue;
			partials++;
			if (d < srcDose) { srcDose = d; srcSlot = i; srcId = items[i].getId(); }
			if (d > tgtDose) { tgtDose = d; tgtSlot = i; tgtId = items[i].getId(); }
		}
		if (partials < 2) return false;
		if (tgtSlot == srcSlot)
			for (int i = 0; i < items.length; i++)
			{
				if (i == srcSlot || items[i] == null) continue;
				int d = doseOf(items[i].getId(), doseIds);
				if (d > 0 && d < 4) { tgtSlot = i; tgtId = items[i].getId(); break; }
			}
		useOnUse(srcSlot, srcId, tgtSlot, tgtId, tick);
		return true;
	}

	private void useOnUse(int srcSlot, int srcId, int tgtSlot, int tgtId, int tick)
	{
		String src = ClickRouter.ITEM_COLOR_PREFIX + nameOf(srcId) + ClickRouter.ITEM_COLOR_SUFFIX;
		String tgt = ClickRouter.ITEM_COLOR_PREFIX + nameOf(tgtId) + ClickRouter.ITEM_COLOR_SUFFIX;
		client.menuAction(srcSlot, ClickRouter.INVENTORY_WIDGET, MenuAction.WIDGET_TARGET, 0, srcId, "Use", src);
		client.menuAction(tgtSlot, ClickRouter.INVENTORY_WIDGET, MenuAction.WIDGET_TARGET_ON_WIDGET,
			0, tgtId, "Use", src + "<col=ffffff> -> " + tgt);
		lastActionTick = tick;
	}

	// ---- make-room juggle -----------------------------------------------

	private GroundItemTracker.Entry findGround(GroundItemTracker ground, int itemId)
	{
		if (ground == null) return null;
		for (GroundItemTracker.Entry e : ground.all())
			if (e != null && e.itemId == itemId && e.tile != null) return e;
		return null;
	}

	/** Full inventory + a floor potion matching a partial we hold + a food filler to drop -> start the juggle. */
	private boolean tryStartMakeRoom(ClickRouter router, GroundItemTracker ground, int tick)
	{
		if (ground == null) return false;
		if (freeSlots() > 0) return false;                       // only when full
		SupplyProfile.FoodEntry food = profile.primaryFood();
		if (food == null || countItem(food.itemId) <= 0 || isBlacklisted(food.itemId)) return false;   // need a droppable filler

		for (GroundItemTracker.Entry e : ground.all())
		{
			if (e == null || e.tile == null) continue;
			if (e.itemId == EMPTY_VIAL_ITEM_ID) continue;
			for (SupplyProfile.DrinkKind k : SupplyProfile.DrinkKind.values())
			{
				SupplyProfile.PotionEntry pe = profile.potion(k);
				if (pe == null) continue;
				if (doseOf(e.itemId, pe.doseIds) <= 0) continue;   // floor item is not this potion type
				if (partialsOfType(pe.doseIds) < 1) continue;      // we hold no partial to combine with
				roomDoses = pe.doseIds;
				roomFloorId = e.itemId;
				roomFloorTile = e.tile;
				fillerId = food.itemId;
				fillerTile = playerTile();
				roomStartTick = tick;
				room = Room.GRAB;
				dropItemId(fillerId, tick);   // step 1: drop the filler to free a slot
				return true;
			}
		}
		return false;
	}

	private boolean advanceRoom(ClickRouter router, GroundItemTracker ground, int tick)
	{
		if (tick - roomStartTick > ROOM_TIMEOUT_TICKS) { room = Room.NONE; return false; }   // safety: never hang
		switch (room)
		{
			case GRAB:
				if (freeSlots() <= 0) return true;                     // filler drop not processed yet -- wait
				router.takeGroundItem(roomFloorId, roomFloorTile, nameOf(roomFloorId));
				lastActionTick = tick;
				room = Room.CONSOLIDATE;
				return true;
			case CONSOLIDATE:
				if (partialsOfType(roomDoses) < 2) return true;        // still walking to / picking up the floor potion
				if (consolidate(roomDoses, tick)) return true;         // combine (may take >1 tick for 3+ partials)
				room = Room.DROP_VIAL;
				return true;
			case DROP_VIAL:
				if (dropEmptyVial(tick)) return true;
				room = Room.REGRAB;
				return true;
			case REGRAB:
				if (countItem(fillerId) > 0) { room = Room.NONE; return false; }   // got the filler back -> done
				GroundItemTracker.Entry fe = findGround(ground, fillerId);
				if (fe == null) { room = Room.NONE; return false; }               // filler gone -- give up
				if (freeSlots() > 0)
				{
					router.takeGroundItem(fillerId, fe.tile, nameOf(fillerId));
					lastActionTick = tick;
				}
				return true;
			default:
				room = Room.NONE;
				return false;
		}
	}
}
