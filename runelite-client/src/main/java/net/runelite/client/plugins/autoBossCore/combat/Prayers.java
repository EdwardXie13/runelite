package net.runelite.client.plugins.autoBossCore.combat;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import java.util.EnumMap;
import java.util.Map;

/**
 * Prayer -> prayerbook widget id resolver.
 * <p>
 * Known-good widget IDs (post-Deadeye/Mystic Vigour layout, captured live) --
 * cross-check against the runtime resolver's output:
 * <pre>
 *   PROTECT_FROM_MAGIC    35454997  (0x021d0015, PRAYER13)
 *   PROTECT_FROM_MISSILES 35454998  (0x021d0016, PRAYER14)
 *   PROTECT_FROM_MELEE    35454999  (0x021d0017, PRAYER15)
 *   EAGLE_EYE             35455005  (0x021d001d, PRAYER21)
 *   MYSTIC_MIGHT          35455008  (0x021d0020, PRAYER24)
 *   RIGOUR                35455009  (0x021d0021, PRAYER25)
 *   PIETY                 35455011  (0x021d0023, PRAYER27)
 *   AUGURY                35455012  (0x021d0024, PRAYER28)
 * </pre>
 * <p>
 * The prayer book widget (interface 541, {@link InterfaceID#PRAYERBOOK}) exposes 30 slots
 * {@code PRAYER1 .. PRAYER30}. Which prayer sits in which slot depends on the player's
 * prayerbook layout, which has changed historically (Deadeye and Mystic Vigour additions
 * reshuffled things). Rather than hardcode a mapping that goes stale, we iterate the live
 * widgets at runtime, read each widget's name, and match it to a {@link Prayer} enum
 * value by name. The result is cached per-session and auto-rebuilt if a lookup misses.
 */
@Slf4j
public final class Prayers
{
	private Prayers() {}

	private static final Map<Prayer, Integer> WIDGET = new EnumMap<>(Prayer.class);
	private static final Map<Prayer, String>  NAME   = new EnumMap<>(Prayer.class);
	private static boolean resolved = false;

	/** Call this early (plugin startUp) if you want eager resolution. Safe to call multiple times. */
	public static synchronized void resolve(Client client)
	{
		if (client == null) return;
		Map<Prayer, Integer> newWidget = new EnumMap<>(Prayer.class);
		Map<Prayer, String>  newName   = new EnumMap<>(Prayer.class);

		for (int slot = 1; slot <= 30; slot++)
		{
			int widgetId = InterfaceID.Prayerbook.PRAYER1 + (slot - 1);
			Widget w = client.getWidget(widgetId);
			if (w == null) continue;
			String raw = w.getName();
			if (raw == null || raw.isEmpty()) continue;
			String clean = stripTags(raw).trim();
			Prayer p = matchPrayer(clean);
			if (p == null) continue;
			newWidget.put(p, widgetId);
			newName.put(p, clean);
		}

		if (newWidget.isEmpty())
		{
			log.debug("[pray] resolver found no prayer widgets yet (book interface not loaded?)");
			return;  // keep whatever we had
		}

		WIDGET.clear();  WIDGET.putAll(newWidget);
		NAME.clear();    NAME.putAll(newName);
		resolved = true;
		log.info("[pray] resolved {} prayers from widget tree", WIDGET.size());
		for (Map.Entry<Prayer, Integer> e : WIDGET.entrySet())
		{
			log.debug("[pray]   {} -> widget 0x{} ({})", e.getKey(),
				Integer.toHexString(e.getValue()), NAME.get(e.getKey()));
		}
	}

	public static int widgetId(Client client, Prayer p)
	{
		if (!resolved) resolve(client);
		Integer id = WIDGET.get(p);
		if (id != null) return id;
		// Not found yet. Re-resolve once in case the book just opened.
		resolve(client);
		id = WIDGET.get(p);
		return id == null ? -1 : id;
	}

	public static String name(Prayer p)
	{
		String n = NAME.get(p);
		return n == null ? p.name() : n;
	}

	public static boolean isRegistered(Prayer p) { return WIDGET.containsKey(p); }

	// -----------------------------------------------------------------
	// Internals.
	// -----------------------------------------------------------------

	private static String stripTags(String s)
	{
		if (s == null) return "";
		return s.replaceAll("<[^>]*>", "");
	}

	/** Match a cleaned widget name (e.g. "Piety") to a Prayer enum value, case-insensitive. */
	private static Prayer matchPrayer(String name)
	{
		if (name == null || name.isEmpty()) return null;
		String target = name.replace("_", " ").replace("-", " ").trim();
		for (Prayer p : Prayer.values())
		{
			String enumName = p.name().replace("_", " ");
			if (enumName.equalsIgnoreCase(target)) return p;
		}
		// Fuzzy second pass: compare without spaces.
		String squashed = target.replaceAll("\\s+", "").toLowerCase();
		for (Prayer p : Prayer.values())
		{
			if (p.name().replaceAll("_", "").equalsIgnoreCase(squashed)) return p;
		}
		return null;
	}
}
