package net.runelite.client.plugins.autoBossCore.state;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Reusable scene-walking helpers for {@link GameObject}s. Centralises the
 * "iterate every tile on the current plane and look at its game objects" loop
 * that boss plugins would otherwise copy-paste (rock scenery, climbable objects,
 * levers, portals, etc.).
 * <p>
 * All methods are null-safe: a null client / scene / tile grid yields an empty
 * result (or a no-op), never an exception. A single {@link GameObject} that spans
 * multiple tiles is registered in several tile cells by the client, so the
 * iteration helpers may visit it more than once; the collecting helpers
 * ({@link #find} / {@link #tiles}) de-duplicate by {@link GameObject#getHash()}.
 * <p>
 * Tiles are returned in INSTANCE coords (via {@link WorldPoint#fromLocal}), matching
 * {@code player.getWorldLocation()}; convert with {@link InstanceCoords} if you need
 * template coords.
 */
public final class SceneScanner
{
	private SceneScanner() {}

	/**
	 * Visit every {@link GameObject} on the current plane. The visitor may be
	 * invoked more than once for a multi-tile object (see class docs). Returns
	 * immediately if the scene isn't loaded.
	 */
	public static void forEach(Client client, Consumer<GameObject> visitor)
	{
		if (client == null || visitor == null) return;
		Scene scene = client.getScene();
		if (scene == null) return;
		Tile[][][] planes = scene.getTiles();
		if (planes == null) return;
		int plane = client.getPlane();
		if (plane < 0 || plane >= planes.length) return;
		Tile[][] tiles = planes[plane];
		if (tiles == null) return;
		for (Tile[] col : tiles)
		{
			if (col == null) continue;
			for (Tile t : col)
			{
				if (t == null) continue;
				GameObject[] gos = t.getGameObjects();
				if (gos == null) continue;
				for (GameObject go : gos)
				{
					if (go != null) visitor.accept(go);
				}
			}
		}
	}

	/**
	 * The first {@link GameObject} on the current plane matching {@code match},
	 * or {@code null} if none. Scanning stops as soon as a match is found.
	 */
	public static GameObject first(Client client, Predicate<GameObject> match)
	{
		if (client == null || match == null) return null;
		GameObject[] hit = new GameObject[1];
		forEach(client, go -> {
			if (hit[0] == null && match.test(go)) hit[0] = go;
		});
		return hit[0];
	}

	/** The first {@link GameObject} on the current plane with id {@code id}, or {@code null}. */
	public static GameObject firstById(Client client, int id)
	{
		return first(client, go -> go.getId() == id);
	}

	/**
	 * All distinct {@link GameObject}s on the current plane matching {@code match}
	 * (de-duplicated by {@link GameObject#getHash()}). Never null.
	 */
	public static List<GameObject> find(Client client, Predicate<GameObject> match)
	{
		List<GameObject> out = new ArrayList<>();
		if (match == null) return out;
		Set<Long> seen = new HashSet<>();
		forEach(client, go -> {
			if (match.test(go) && seen.add(go.getHash())) out.add(go);
		});
		return out;
	}

	/**
	 * All distinct {@link GameObject}s on the current plane whose id is one of
	 * {@code ids} (de-duplicated). Never null.
	 */
	public static List<GameObject> findByIds(Client client, int... ids)
	{
		return find(client, go -> contains(ids, go.getId()));
	}

	/**
	 * INSTANCE-coord tiles of every distinct {@link GameObject} whose id is one of
	 * {@code ids}. Uses {@link GameObject#getLocalLocation()} -> world. Never null.
	 */
	public static List<WorldPoint> tiles(Client client, int... ids)
	{
		List<WorldPoint> out = new ArrayList<>();
		for (GameObject go : findByIds(client, ids))
		{
			WorldPoint wp = WorldPoint.fromLocal(client, go.getLocalLocation());
			if (wp != null) out.add(wp);
		}
		return out;
	}

	private static boolean contains(int[] ids, int id)
	{
		if (ids == null) return false;
		for (int i : ids)
		{
			if (i == id) return true;
		}
		return false;
	}
}
