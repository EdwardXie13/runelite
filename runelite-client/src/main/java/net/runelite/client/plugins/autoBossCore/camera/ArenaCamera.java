package net.runelite.client.plugins.autoBossCore.camera;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Generic, boss-agnostic camera controller. Detaches the game camera from the player
 * ("free camera" mode) and parks a fixed view over the arena -- most usefully a straight-down
 * overhead/plan view for watching pathing, hazards and the dodge corridors while a script runs.
 *
 * <p>Generalized from the ad-hoc camera helpers in {@code VorkathAutoMain}. Any boss plugin can
 * inject this singleton and call {@link #overhead(WorldPoint)} (or the lower-level
 * {@link #detach}) with the arena centre, then {@link #reset()} to hand the camera back to the
 * player.
 *
 * <h2>Angle units (JAU14)</h2>
 * Yaw and pitch are in Jagex Angle Units, 14-bit: a full turn is {@value #FULL_CIRCLE}
 * ({@code 2^14}) units, so one unit is {@code 2*PI / 2^14} radians. Yaw is a compass heading
 * (see {@link #YAW_NORTH} etc.); pitch is the tilt, where ~0 looks along the horizon and
 * {@value #PITCH_OVERHEAD} points essentially straight down for a top-down plan view.
 *
 * <h2>Coordinates</h2>
 * {@link #centreOn(WorldPoint)} and the presets take a tile that must exist in the CURRENTLY
 * LOADED scene. For a dynamic instanced arena (e.g. Duke), convert your stored template tile to
 * the live instance tile before passing it in -- a template tile will not resolve to a
 * {@link LocalPoint} and the centring is silently skipped.
 *
 * <p>All public operations marshal onto the client thread, so this is safe to call from a tick
 * handler, a config listener or a key binding alike.
 */
@Slf4j
@Singleton
public class ArenaCamera
{
	/** JAU14 full circle: {@code 2^14} units. */
	public static final int FULL_CIRCLE = 16384;

	// Yaw compass headings (JAU14). Yaw does not change WHAT an overhead view sees, only how the
	// arena is oriented on screen; NORTH keeps world-north pointing up.
	public static final int YAW_NORTH = 0;
	public static final int YAW_EAST  = FULL_CIRCLE / 4;        // 4096
	public static final int YAW_SOUTH = FULL_CIRCLE / 2;        // 8192
	public static final int YAW_WEST  = FULL_CIRCLE * 3 / 4;    // 12288

	// Pitch (JAU14). ~0 looks along the horizon; FULL_CIRCLE/4 (=4096, 90 degrees) is straight
	// down. OVERHEAD is a hair past vertical for a clean top-down plan view -- the value proven
	// out in VorkathAuto.
	public static final int PITCH_HORIZON  = 128;
	public static final int PITCH_OVERHEAD = 4160;

	// CAMERA_DO_ZOOM argument. LOWER = further out / wider field (fits a whole arena); HIGHER =
	// closer in. Tune per arena; this default is a sensible whole-arena starting point.
	public static final int ZOOM_ARENA_WIDE = 256;

	private final Client client;
	private final ClientThread clientThread;

	// Player-camera orientation captured on the first detach so reset() can restore it.
	private Integer savedYaw;
	private Integer savedPitch;

	@Inject
	public ArenaCamera(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	// -----------------------------------------------------------------
	// High-level presets.
	// -----------------------------------------------------------------

	/** Overhead plan view centred on {@code centre}, north-up, zoomed out for the whole arena. */
	public void overhead(WorldPoint centre)
	{
		overhead(centre, ZOOM_ARENA_WIDE, YAW_NORTH);
	}

	/** Overhead plan view on {@code centre} with a caller-chosen zoom, north-up. */
	public void overhead(WorldPoint centre, int zoom)
	{
		overhead(centre, zoom, YAW_NORTH);
	}

	/** Overhead plan view on {@code centre} with explicit zoom and yaw (screen orientation). */
	public void overhead(WorldPoint centre, int zoom, int yaw)
	{
		detach(centre, PITCH_OVERHEAD, yaw, zoom);
	}

	/**
	 * Overhead plan view centred on the bounding-box midpoint of {@code arenaTiles}. The tiles
	 * must be LOADED-SCENE (instance) tiles -- convert from template first for an instanced arena.
	 */
	public void overheadOf(List<WorldPoint> arenaTiles, int zoom)
	{
		WorldPoint centre = boundingCentre(arenaTiles);
		if (centre != null) overhead(centre, zoom, YAW_NORTH);
	}

	// -----------------------------------------------------------------
	// Generic detached view.
	// -----------------------------------------------------------------

	/**
	 * Detach the camera (free-camera mode) and aim a fixed view at {@code centre}. Unlike the
	 * player camera this does not follow the avatar. Angles are JAU14.
	 *
	 * @param centre loaded-scene tile to look at (instance tile for an instanced arena).
	 * @param pitch  tilt in JAU14 ({@link #PITCH_OVERHEAD} for straight down).
	 * @param yaw    heading in JAU14 ({@link #YAW_NORTH} etc.).
	 * @param zoom   CAMERA_DO_ZOOM argument (lower = further out).
	 */
	public void detach(WorldPoint centre, int pitch, int yaw, int zoom)
	{
		clientThread.invoke(() -> {
			rememberPlayerOrientation();
			applyZoom(zoom);
			applyPitch(pitch);
			applyYaw(yaw);
			applyCentre(centre);
		});
	}

	/** Set zoom, pitch and yaw together without changing the focal point or camera mode. */
	public void setZoomPitchYaw(int zoom, int pitch, int yaw)
	{
		clientThread.invoke(() -> {
			applyZoom(zoom);
			applyPitch(pitch);
			applyYaw(yaw);
		});
	}

	/**
	 * Park the free-camera focal point on {@code tile}'s ground position. Switches to free-camera
	 * mode. No-op (debug-logged) if the tile is not in the loaded scene.
	 */
	public void centreOn(WorldPoint tile)
	{
		clientThread.invoke(() -> applyCentre(tile));
	}

	// -----------------------------------------------------------------
	// Restore.
	// -----------------------------------------------------------------

	/** Re-attach the camera to the player and restore the pre-detach orientation. */
	public void reset()
	{
		clientThread.invoke(() -> {
			if (savedPitch != null) applyPitch(savedPitch);
			if (savedYaw != null) applyYaw(savedYaw);
			client.setCameraMode(0); // player-follow camera
			savedPitch = null;
			savedYaw = null;
		});
	}

	/** True while the camera is detached (free-camera mode). */
	public boolean isDetached()
	{
		return client.getCameraMode() == 1;
	}

	// -----------------------------------------------------------------
	// Low-level setters (public for callers that want one axis at a time). Each marshals onto
	// the client thread; the apply* variants below assume they are already on it.
	// -----------------------------------------------------------------

	public void setYaw(int yaw)     { clientThread.invoke(() -> applyYaw(yaw)); }
	public void setPitch(int pitch) { clientThread.invoke(() -> applyPitch(pitch)); }
	public void setZoom(int zoom)   { clientThread.invoke(() -> applyZoom(zoom)); }

	// -----------------------------------------------------------------
	// Internals -- client-thread only.
	// -----------------------------------------------------------------

	private void applyYaw(int yaw)
	{
		if (client.getCameraYaw() == yaw) return;
		client.setCameraYawTarget(yaw);
	}

	private void applyPitch(int pitch)
	{
		if (client.getCameraPitch() == pitch) return;
		client.setCameraPitchTarget(pitch);
	}

	private void applyZoom(int zoom)
	{
		client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom);
	}

	private void applyCentre(WorldPoint tile)
	{
		if (tile == null) return;
		client.setCameraMode(1); // free camera
		LocalPoint lp = LocalPoint.fromWorld(client, tile);
		if (lp == null)
		{
			log.debug("[arena-cam] centre skipped -- tile {} not in loaded scene", tile);
			return;
		}
		// Focal point is in scene-local units: X maps to world-x, Z maps to world-y (ground
		// plane); Y (height) is left at its default so we look at ground level.
		client.setCameraFocalPointX(lp.getX());
		client.setCameraFocalPointZ(lp.getY());
	}

	private void rememberPlayerOrientation()
	{
		if (savedYaw == null) savedYaw = client.getCameraYaw();
		if (savedPitch == null) savedPitch = client.getCameraPitch();
	}

	/** Bounding-box midpoint of a set of tiles (same plane as the first), or null if empty. */
	public static WorldPoint boundingCentre(List<WorldPoint> tiles)
	{
		if (tiles == null || tiles.isEmpty()) return null;
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		for (WorldPoint p : tiles)
		{
			minX = Math.min(minX, p.getX());
			minY = Math.min(minY, p.getY());
			maxX = Math.max(maxX, p.getX());
			maxY = Math.max(maxY, p.getY());
		}
		return new WorldPoint((minX + maxX) / 2, (minY + maxY) / 2, tiles.get(0).getPlane());
	}
}
