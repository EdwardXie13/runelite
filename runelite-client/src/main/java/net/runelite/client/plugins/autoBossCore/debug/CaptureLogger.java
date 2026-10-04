package net.runelite.client.plugins.autoBossCore.debug;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Deque;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Development-only event dumper for identifying boss mechanics IDs.
 * <p>
 * Projectile detection uses the Vorkath-style polling pattern on each GameTick, since
 * the dedicated ProjectileSpawned event is gone from newer RuneLite. A projectile is
 * logged exactly once per instance (identity-tracked), with a {@code NEW} marker the
 * first time its ID is seen in the whole run.
 */
@Slf4j
@Singleton
public class CaptureLogger
{
	private final Client client;

	private volatile boolean enabled = false;
	private volatile int arenaRegion = -1;
	private String prefix = "[cap]";

	private int tickCounter = 0;

	private final Set<Integer> seenProjectileIds  = new HashSet<>();
	private final Set<Integer> seenGraphicsIds    = new HashSet<>();
	private final Set<Integer> seenAnimIds        = new HashSet<>();
	private final Set<Integer> seenNpcIds         = new HashSet<>();
	private final Set<Integer> seenGameObjectIds  = new HashSet<>();
	private final java.util.Set<Long> seenVarbitChanges = new HashSet<>();

	// Identity-tracked projectile instances already logged. Pruned each tick to the live set.
	private final Set<Projectile> loggedInstances =
		Collections.newSetFromMap(new IdentityHashMap<>());

	@Inject
	public CaptureLogger(Client client)
	{
		this.client = client;
	}

	public void setEnabled(boolean on)
	{
		if (on && !enabled) log.info("{} capture ENABLED (region={})", prefix, arenaRegion);
		if (!on && enabled) log.info("{} capture DISABLED", prefix);
		this.enabled = on;
	}

	public boolean isEnabled() { return enabled; }

	public void setArenaRegion(int region) { this.arenaRegion = region; }

	public void setPrefix(String p) { this.prefix = p == null ? "[cap]" : p; }

	public void resetDedupe()
	{
		seenProjectileIds.clear();
		seenGraphicsIds.clear();
		seenAnimIds.clear();
		seenNpcIds.clear();
		seenGameObjectIds.clear();
		loggedInstances.clear();
		seenVarbitChanges.clear();
	}

	private boolean inArena()
	{
		if (arenaRegion < 0) return true;
		Player p = client.getLocalPlayer();
		if (p == null) return false;
		WorldPoint w = p.getWorldLocation();
		return w != null && w.getRegionID() == arenaRegion;
	}

	private boolean active() { return enabled && inArena(); }

	@Subscribe
	public void onGameTick(GameTick e)
	{
		tickCounter++;

		// Projectile polling. Always rebuild the "live" identity set so we can prune
		// loggedInstances afterwards and avoid unbounded growth.
		Deque<Projectile> live = client.getProjectiles();
		Set<Projectile> currentLive = Collections.newSetFromMap(new IdentityHashMap<>());
		if (live != null)
		{
			for (Projectile p : live) if (p != null) currentLive.add(p);
		}

		if (active())
		{
			int clientCycle = client.getGameCycle();
			for (Projectile p : currentLive)
			{
				if (!loggedInstances.add(p)) continue;  // already logged this instance

				boolean freshId = seenProjectileIds.add(p.getId());
				String marker = freshId ? "NEW " : "    ";

				WorldPoint src = p.getSourcePoint();
				WorldPoint tgt = p.getTargetPoint();
				Actor srcActor = p.getSourceActor();
				String srcName = srcActor == null || srcActor.getName() == null ? "?" : srcActor.getName();

				int ticksToStart = Math.max(0, (p.getStartCycle() - clientCycle) / 30);
				int ticksToEnd   = Math.max(0, (p.getEndCycle()   - clientCycle) / 30);

				log.info(
					"{} t={} {}proj id={} src='{}' from=({},{}) to=({},{}) tStart=+{} tEnd=+{} slope={} startH={} endH={}",
					prefix, tickCounter, marker, p.getId(), srcName,
					src == null ? -1 : src.getX(), src == null ? -1 : src.getY(),
					tgt == null ? -1 : tgt.getX(), tgt == null ? -1 : tgt.getY(),
					ticksToStart, ticksToEnd,
					p.getSlope(), p.getStartHeight(), p.getEndHeight()
				);
			}
		}

		loggedInstances.retainAll(currentLive);
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated e)
	{
		if (!active()) return;
		GraphicsObject g = e.getGraphicsObject();
		if (g == null) return;

		boolean fresh = seenGraphicsIds.add(g.getId());
		String marker = fresh ? "NEW " : "    ";

		LocalPoint lp = g.getLocation();
		WorldPoint wp = lp == null ? null : WorldPoint.fromLocalInstance(client, lp);

		log.info(
			"{} t={} {}gfx id={} tile=({},{}) startCycle={} finished={}",
			prefix, tickCounter, marker, g.getId(),
			wp == null ? -1 : wp.getX(), wp == null ? -1 : wp.getY(),
			g.getStartCycle(), g.finished()
		);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged e)
	{
		if (!active()) return;
		Actor a = e.getActor();
		if (a == null) return;

		int anim = a.getAnimation();
		if (anim == -1) return;

		boolean fresh = seenAnimIds.add(anim);
		String marker = fresh ? "NEW " : "    ";

		String kind = (a == client.getLocalPlayer()) ? "self"
			: a instanceof NPC ? "npc" : "actor";
		String name = a.getName() == null ? "?" : a.getName();

		log.info("{} t={} {}anim id={} {} name='{}'", prefix, tickCounter, marker, anim, kind, name);
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		if (!active()) return;
		NPC n = e.getNpc();
		if (n == null) return;
		boolean fresh = seenNpcIds.add(n.getId());
		String marker = fresh ? "NEW " : "    ";
		WorldPoint w = n.getWorldLocation();
		log.info(
			"{} t={} {}npcSpawn id={} name='{}' tile=({},{})",
			prefix, tickCounter, marker, n.getId(),
			n.getName() == null ? "?" : n.getName(),
			w == null ? -1 : w.getX(), w == null ? -1 : w.getY()
		);
	}

	@Subscribe
	public void onNpcChanged(NpcChanged e)
	{
		if (!active()) return;
		NPC n = e.getNpc();
		NPCComposition old = e.getOld();
		if (n == null) return;
		int newId = n.getId();
		int oldId = old == null ? -1 : old.getId();
		if (newId == oldId) return;

		boolean fresh = seenNpcIds.add(newId);
		String marker = fresh ? "NEW " : "    ";
		log.info(
			"{} t={} {}npcChange {} -> {} name='{}'",
			prefix, tickCounter, marker, oldId, newId,
			n.getName() == null ? "?" : n.getName()
		);
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e)
	{
		if (!active()) return;
		Actor a = e.getActor();
		int dmg = e.getHitsplat().getAmount();
		int type = e.getHitsplat().getHitsplatType();
		String target = a == client.getLocalPlayer() ? "self"
			: a == null || a.getName() == null ? "?" : a.getName();
		log.info("{} t={} hit target='{}' dmg={} type={}", prefix, tickCounter, target, dmg, type);
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		if (!active()) return;
		GameObject g = e.getGameObject();
		if (g == null) return;
		int id = g.getId();
		if (!seenGameObjectIds.add(id)) return;
		WorldPoint w = g.getWorldLocation();
		log.info(
			"{} t={} NEW gobj id={} tile=({},{})",
			prefix, tickCounter, id,
			w == null ? -1 : w.getX(), w == null ? -1 : w.getY()
		);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged e)
	{
		if (!active()) return;
		int id = e.getVarbitId();
		if (id < 0) return;  // -1 means it's a varp update, not a varbit
		long key = ((long) id << 20) | (e.getValue() & 0xFFFFF);
		if (!seenVarbitChanges.add(key)) return;  // only log unique (id, value) pairs
		log.info("{} t={} NEW varbit id={} value={}", prefix, tickCounter, id, e.getValue());
	}
}
