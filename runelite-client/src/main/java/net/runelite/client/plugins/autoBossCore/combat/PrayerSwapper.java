package net.runelite.client.plugins.autoBossCore.combat;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Prayer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Boss-agnostic prayer orchestration. Two modes:
 * <ol>
 *   <li>{@link #ensureOn(Prayer...)} / {@link #ensureOnlyOn(Prayer...)} -- camping/toggling.
 *       Call every tick; the swapper reactivates anything dropped (e.g. no-prayer tiles).</li>
 *   <li>{@link #scheduleSwap(Prayer, int)} -- queue a prayer activation at a specific tick,
 *       for projectile-driven swaps (Leviathan range vs mage).</li>
 * </ol>
 * <p>
 * Activation goes through {@link ClickRouter#activatePrayer(int, String)} so the orb-mode
 * bypass is preserved. Prayer-active state is read from {@link Client#isPrayerActive}.
 */
@Slf4j
@Singleton
public class PrayerSwapper
{
	private final Client client;
	private final ClickRouter router;

	private final Map<Integer, Prayer> scheduledAtTick = new HashMap<>();

	/** Guard: within this cooldown (ticks) we won't re-fire activation on the same prayer. */
	private final Map<Prayer, Integer> lastClickTick = new HashMap<>();
	private static final int SAME_PRAYER_CLICK_COOLDOWN = 2;

	@Inject
	public PrayerSwapper(Client client, ClickRouter router)
	{
		this.client = client;
		this.router = router;
	}

	// -----------------------------------------------------------------
	// Camping / toggling.
	// -----------------------------------------------------------------

	/**
	 * Ensure every listed prayer is on. Reactivates any that have dropped.
	 * <p>
	 * Respects overhead mutual-exclusion: if the caller asks to ensure
	 * {@link Prayer#PROTECT_FROM_MELEE} but a DIFFERENT overhead is currently active
	 * (e.g. a scheduled swap flipped us to {@link Prayer#PROTECT_FROM_MISSILES} this
	 * tick), we leave the other overhead alone instead of ping-ponging between them.
	 */
	public void ensureOn(int tickCounter, Prayer... prayers)
	{
		if (prayers == null) return;
		Set<Prayer> overheads = OVERHEAD_SET();
		for (Prayer p : prayers)
		{
			if (p == null) continue;
			if (client.isPrayerActive(p)) continue;

			// Overhead mutual exclusion: if ANY other overhead is active, don't fight it.
			if (overheads.contains(p))
			{
				boolean someOtherOverheadOn = false;
				for (Prayer o : overheads)
				{
					if (o != p && client.isPrayerActive(o)) { someOtherOverheadOn = true; break; }
				}
				if (someOtherOverheadOn) continue;
			}

			click(p, tickCounter);
		}
	}

	/** Ensure the listed prayers are on and all others of the given set are off. */
	public void ensureOnlyOn(int tickCounter, Set<Prayer> keepOn, Set<Prayer> watchSet)
	{
		for (Prayer p : watchSet)
		{
			boolean want = keepOn.contains(p);
			boolean have = client.isPrayerActive(p);
			if (want && !have) click(p, tickCounter);
			if (!want && have) click(p, tickCounter);  // clicking an active prayer deactivates it
		}
	}

	// -----------------------------------------------------------------
	// Scheduled swaps (projectile-driven overheads).
	// -----------------------------------------------------------------

	public void scheduleSwap(Prayer target, int atTick)
	{
		scheduledAtTick.put(atTick, target);
	}

	public void onGameTick(int tickCounter)
	{
		Prayer due = scheduledAtTick.remove(tickCounter);
		if (due != null && !client.isPrayerActive(due))
		{
			click(due, tickCounter);
		}
		// Expire anything in the past that we missed.
		Iterator<Map.Entry<Integer, Prayer>> it = scheduledAtTick.entrySet().iterator();
		while (it.hasNext())
		{
			if (it.next().getKey() < tickCounter - 1) it.remove();
		}
	}

	public void clearScheduled() { scheduledAtTick.clear(); }

	// -----------------------------------------------------------------
	// Queries.
	// -----------------------------------------------------------------

	public boolean isOn(Prayer p) { return p != null && client.isPrayerActive(p); }

	/** List of currently-active prayers in the given watch set. */
	public List<Prayer> activeIn(Set<Prayer> watchSet)
	{
		List<Prayer> out = new ArrayList<>();
		for (Prayer p : watchSet) if (client.isPrayerActive(p)) out.add(p);
		return out;
	}

	// -----------------------------------------------------------------
	// Helpers.
	// -----------------------------------------------------------------

	private void click(Prayer p, int tickCounter)
	{
		Integer last = lastClickTick.get(p);
		if (last != null && tickCounter - last < SAME_PRAYER_CLICK_COOLDOWN) return;
		int widget = Prayers.widgetId(client, p);
		if (widget < 0)
		{
			log.warn("[pray] no widget registered for {}", p);
			return;
		}
		boolean want = !client.isPrayerActive(p);
		String name = Prayers.name(p);
		log.info("[pray] click tick={} prayer={} widgetId=0x{} name='{}' action={}",
			tickCounter, p, Integer.toHexString(widget), name, want ? "Activate" : "Deactivate");
		if (want) router.activatePrayer(widget, name);
		else      router.deactivatePrayer(widget, name);
		lastClickTick.put(p, tickCounter);
	}

	/** Convenience: an EnumSet of the "overhead" prayers worth watching. */
	public static Set<Prayer> OVERHEAD_SET()
	{
		return Collections.unmodifiableSet(EnumSet.of(
			Prayer.PROTECT_FROM_MAGIC,
			Prayer.PROTECT_FROM_MISSILES,
			Prayer.PROTECT_FROM_MELEE
		));
	}
}
