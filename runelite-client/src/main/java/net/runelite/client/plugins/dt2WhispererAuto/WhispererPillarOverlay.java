package net.runelite.client.plugins.dt2WhispererAuto;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;

/**
 * World overlay that labels the 2x2 SAFESPOT north of the pillars we'll hide behind with their
 * hide-order rank (1 = 20%, 2 = 40%, 3 = 100%), shown only during the screech phase
 * ({@link WhispererAutoMain#inScreechPhase()}). Only the latched hide chain is drawn -- the rank-3
 * pillar, the rank-2 nearest it, and the rank-1 nearest that rank-2
 * ({@link WhispererAutoMain#hideChainPillars()}); all other pillars are intentionally not rendered.
 *
 * <p>Like the seed overlay, this renders in BOTH realms: the safespot tiles come from
 * {@link WhispererAutoMain#hideChainSafespots()}, which gives shadow tiles while in the shadow realm
 * and shadow->overworld-translated tiles while in the overworld -- so the plan is visible from the
 * overworld too (before we cross), not just once we're standing among the pillars.
 */
@Singleton
public class WhispererPillarOverlay extends Overlay
{
	private final Client client;
	private final WhispererAutoMain main;

	@Inject
	public WhispererPillarOverlay(Client client, WhispererAutoMain main)
	{
		this.client = client;
		this.main = main;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!main.inScreechPhase()) return null;

		// Latched safespot anchors (SW tile of each 2x2), resolved for whichever realm we're in, in fixed
		// hide order 1 -> 2 -> 3. A pillar's HP/rank shifting during the attack does NOT change this; a
		// tile that fails to resolve this realm is just skipped, so the others still show.
		List<WorldPoint> spots = main.hideChainSafespots();
		if (spots == null || spots.isEmpty()) return null;

		final int half = Perspective.LOCAL_TILE_SIZE / 2;
		for (int i = 0; i < spots.size(); i++)
		{
			WorldPoint w = spots.get(i);
			if (w == null) continue;
			LocalPoint sw = LocalPoint.fromWorld(client, w);
			if (sw == null) continue;   // not in the loaded scene this realm -> skip (keep the others)

			// 2x2 area centre = SW tile centre + half a tile on each axis (the shared corner of the 4 tiles).
			LocalPoint centre = new LocalPoint(sw.getX() + half, sw.getY() + half);
			String text = String.valueOf(i + 1);   // fixed hide order: 1, 2, 3
			Color color = i == 0 ? Color.RED : (i == 1 ? Color.YELLOW : Color.GREEN);

			Polygon poly = Perspective.getCanvasTileAreaPoly(client, centre, 2);
			if (poly != null) OverlayUtil.renderPolygon(graphics, poly, color);

			Point p = Perspective.getCanvasTextLocation(client, graphics, centre, text, 0);
			if (p != null) OverlayUtil.renderTextLocation(graphics, p, text, color);

			// Highlight the exact sub-tile we'll STAND on within this 2x2 -- the one closest to the next
			// hide spot (shortest step onward), so the chosen corner is visible.
			WorldPoint stand = main.hideSpotStandTile(i);
			if (stand != null)
			{
				LocalPoint slp = LocalPoint.fromWorld(client, stand);
				if (slp != null)
				{
					Polygon sp = Perspective.getCanvasTileAreaPoly(client, slp, 1);
					if (sp != null) OverlayUtil.renderPolygon(graphics, sp, Color.WHITE);
				}
			}
		}
		return null;
	}
}
