package net.runelite.client.plugins.autoBossCore.state;

import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.Collection;

/**
 * Static helpers for template <-> instance WorldPoint conversion. Nearly every DT2 boss
 * (and most newer OSRS bosses) spawns the player into an instanced copy of a template
 * region, so hardcoded tiles in constants files must be TEMPLATE coords -- then converted
 * to the live instance at runtime for walking or dangerous-tile math.
 * <p>
 * All methods are null-safe: pass null or a point outside the loaded scene and you get
 * null back. Callers decide what to do.
 */
public final class InstanceCoords
{
	private InstanceCoords() {}

	/**
	 * Convert an INSTANCE WorldPoint (what {@code player.getWorldLocation()} returns inside
	 * an instance) to its TEMPLATE equivalent. Use this to compare player position against
	 * template constants in a rotation-safe way.
	 */
	public static WorldPoint toTemplate(Client client, WorldPoint instanceWp)
	{
		if (client == null || instanceWp == null) return null;
		LocalPoint lp = LocalPoint.fromWorld(client, instanceWp);
		if (lp == null) return null;
		return WorldPoint.fromLocalInstance(client, lp);
	}

	/**
	 * Convert a TEMPLATE WorldPoint to the first matching INSTANCE WorldPoint in the
	 * current scene. Returns null if the template isn't present in this instance.
	 * <p>
	 * {@link WorldPoint#toLocalInstance} can return multiple matches when the same template
	 * chunk appears in several places; this helper takes the first one. If a boss has an
	 * ambiguous instance layout (unlikely but possible), roll your own picker instead.
	 */
	public static WorldPoint fromTemplate(Client client, WorldPoint template)
	{
		if (client == null || template == null) return null;
		Collection<WorldPoint> matches = WorldPoint.toLocalInstance(client, template);
		return matches == null || matches.isEmpty() ? null : matches.iterator().next();
	}

	/**
	 * Convert a TEMPLATE WorldPoint directly to a {@link LocalPoint} in the current scene.
	 * Equivalent to {@code LocalPoint.fromWorld(client, fromTemplate(client, template))}.
	 */
	public static LocalPoint fromTemplateToLocal(Client client, WorldPoint template)
	{
		WorldPoint instanceWp = fromTemplate(client, template);
		return instanceWp == null ? null : LocalPoint.fromWorld(client, instanceWp);
	}

	/**
	 * Quick test: does {@code instanceWp} refer to the same tile as {@code template}?
	 * Compares both sides in template space.
	 */
	public static boolean equalsTemplate(Client client, WorldPoint instanceWp, WorldPoint template)
	{
		if (template == null) return instanceWp == null;
		WorldPoint t = toTemplate(client, instanceWp);
		return t != null && t.equals(template);
	}
}
