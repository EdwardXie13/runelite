package net.runelite.client.plugins.autoBossCore.state;

import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Deque;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shadow of the live client state that boss strategies read from.
 * <p>
 * Tracks across ticks:
 * <ul>
 *   <li>Monotonic tick counter.</li>
 *   <li>Player animation + the tick it started on.</li>
 *   <li>Projectile queue (polled from {@link Client#getProjectiles()} since the
 *       {@code ProjectileSpawned} event is gone).</li>
 *   <li>Per-NPC position history (current + previous tile) so strategies / dodgers
 *       can compute velocity for moving-NPC hazards like Vardorvis axes.</li>
 *   <li>Inventory snapshot updated on {@link ItemContainerChanged}.</li>
 *   <li>Last-seen value of every varbit the strategy subscribes to.</li>
 * </ul>
 */
@Singleton
public class MirrorState
{
	private final Client client;

	@Getter private int tickCounter = 0;

	@Getter private int playerAnim = -1;
	@Getter private int playerAnimStartTick = -1;

	// Projectiles.
	private final List<ProjectileRecord> projectiles = new ArrayList<>();
	private final Set<Projectile> seenInstances =
		Collections.newSetFromMap(new IdentityHashMap<>());

	// Per-NPC position history (key = NPC identity).
	private final Map<NPC, NpcTrack> npcTracks = new IdentityHashMap<>();

	// Inventory snapshot.
	private final Map<Integer, Integer> invCounts = new HashMap<>();

	// Last seen value for every varbit anyone cared about.
	private final Map<Integer, Integer> varbitSnapshot = new HashMap<>();

	@Inject
	public MirrorState(Client client)
	{
		this.client = client;
	}

	// -------------------------------------------------------------
	// Live getters.
	// -------------------------------------------------------------

	public int getHp()          { return client.getBoostedSkillLevel(Skill.HITPOINTS); }
	public int getMaxHp()       { return client.getRealSkillLevel(Skill.HITPOINTS); }
	public int getPrayer()      { return client.getBoostedSkillLevel(Skill.PRAYER); }
	public int getMaxPrayer()   { return client.getRealSkillLevel(Skill.PRAYER); }
	public int getRunEnergy()   { return client.getEnergy() / 100; }

	public Player getLocalPlayer() { return client.getLocalPlayer(); }

	public WorldPoint getPlayerTile()
	{
		Player p = client.getLocalPlayer();
		return p == null ? null : p.getWorldLocation();
	}

	public int getRegion()
	{
		WorldPoint w = getPlayerTile();
		return w == null ? -1 : w.getRegionID();
	}

	public int getPlane()
	{
		WorldPoint w = getPlayerTile();
		return w == null ? -1 : w.getPlane();
	}

	// -------------------------------------------------------------
	// Inventory.
	// -------------------------------------------------------------

	public int countItem(int itemId)
	{
		Integer c = invCounts.get(itemId);
		return c == null ? 0 : c;
	}

	public int countItems(int... itemIds)
	{
		int total = 0;
		for (int id : itemIds) total += countItem(id);
		return total;
	}

	public int countFreeSlots()
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		if (inv == null) return 0;
		Item[] items = inv.getItems();
		int used = 0;
		for (Item it : items) if (it != null && it.getId() != -1) used++;
		return items.length - used;
	}

	// -------------------------------------------------------------
	// Projectiles.
	// -------------------------------------------------------------

	public List<ProjectileRecord> getProjectileRecords()
	{
		return Collections.unmodifiableList(projectiles);
	}

	public boolean hasProjectile(int id)
	{
		for (ProjectileRecord r : projectiles) if (r.id == id) return true;
		return false;
	}

	// -------------------------------------------------------------
	// NPC tracking.
	// -------------------------------------------------------------

	/** Position history for a tracked NPC. {@code last} is tile from the previous tick. */
	public static final class NpcTrack
	{
		public final NPC npc;
		public WorldPoint current;
		public WorldPoint last;
		public int lastUpdatedTick;

		NpcTrack(NPC npc, WorldPoint current, int tick)
		{
			this.npc = npc;
			this.current = current;
			this.last = current;
			this.lastUpdatedTick = tick;
		}

		/** Velocity in tiles-per-tick. (0,0) if stationary or just appeared. */
		public int dx() { return current == null || last == null ? 0 : current.getX() - last.getX(); }
		public int dy() { return current == null || last == null ? 0 : current.getY() - last.getY(); }
	}

	public NpcTrack trackOf(NPC npc)
	{
		return npc == null ? null : npcTracks.get(npc);
	}

	// -------------------------------------------------------------
	// Varbits.
	// -------------------------------------------------------------

	/**
	 * Read a varbit's last-seen value. Returns {@code def} if we have not received a
	 * {@code VarbitChanged} for it yet since this plugin started.
	 */
	public int getVarbit(int varbitId, int def)
	{
		Integer v = varbitSnapshot.get(varbitId);
		return v == null ? def : v;
	}

	// -------------------------------------------------------------
	// Event hooks.
	// -------------------------------------------------------------

	@Subscribe
	public void onGameTick(GameTick e)
	{
		tickCounter++;

		// --- Projectile polling. ---
		Deque<Projectile> live = client.getProjectiles();
		Set<Projectile> currentLive = Collections.newSetFromMap(new IdentityHashMap<>());
		if (live != null)
		{
			int clientCycle = client.getGameCycle();
			for (Projectile p : live)
			{
				if (p == null) continue;
				currentLive.add(p);
				if (!seenInstances.add(p)) continue;

				int ticksToStart = Math.max(0, (p.getStartCycle() - clientCycle) / 30);
				int ticksToEnd   = Math.max(0, (p.getEndCycle()   - clientCycle) / 30);
				projectiles.add(new ProjectileRecord(
					p.getId(),
					tickCounter + ticksToStart,
					tickCounter + ticksToEnd,
					p
				));
			}
		}
		seenInstances.retainAll(currentLive);
		Iterator<ProjectileRecord> itP = projectiles.iterator();
		while (itP.hasNext())
		{
			ProjectileRecord r = itP.next();
			if (tickCounter > r.endTick + 1) itP.remove();
		}

		// --- NPC position history. Shift current -> last for every tracked NPC. ---
		List<NPC> npcs = client.getNpcs();
		if (npcs != null)
		{
			for (NPC n : npcs)
			{
				if (n == null) continue;
				WorldPoint tile = n.getWorldLocation();
				NpcTrack t = npcTracks.get(n);
				if (t == null)
				{
					npcTracks.put(n, new NpcTrack(n, tile, tickCounter));
				}
				else
				{
					t.last = t.current;
					t.current = tile;
					t.lastUpdatedTick = tickCounter;
				}
			}
			// Prune tracks for NPCs that are no longer live.
			Set<NPC> liveSet = Collections.newSetFromMap(new IdentityHashMap<>());
			liveSet.addAll(npcs);
			npcTracks.keySet().retainAll(liveSet);
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged e)
	{
		Actor a = e.getActor();
		if (a == null) return;
		if (a == client.getLocalPlayer())
		{
			playerAnim = a.getAnimation();
			playerAnimStartTick = tickCounter;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged e)
	{
		if (e.getContainerId() != InventoryID.INVENTORY.getId()) return;
		invCounts.clear();
		Item[] items = e.getItemContainer().getItems();
		for (Item it : items)
		{
			if (it == null || it.getId() == -1) continue;
			invCounts.merge(it.getId(), it.getQuantity(), Integer::sum);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		varbitSnapshot.put(e.getVarbitId(), e.getValue());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		if (e.getNpc() != null) npcTracks.remove(e.getNpc());
	}

	// -------------------------------------------------------------
	// Records.
	// -------------------------------------------------------------

	public static final class ProjectileRecord
	{
		public final int id;
		public final int startTick;
		public final int endTick;
		public final Projectile projectile;

		ProjectileRecord(int id, int startTick, int endTick, Projectile projectile)
		{
			this.id = id;
			this.startTick = startTick;
			this.endTick = endTick;
			this.projectile = projectile;
		}
	}
}
