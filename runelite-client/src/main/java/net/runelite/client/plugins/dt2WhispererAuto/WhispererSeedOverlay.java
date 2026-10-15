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
 * World overlay that draws the TRANSLATED good-seed route -- the overworld tiles we computed from the
 * shadow-realm good seeds (47573) via {@link WhispererAutoMain#shadowToOverworld}. Each tile is a cyan
 * square with its walk-order index, so we can visually verify the shadow->overworld mapping lands on
 * the right overworld seeds. Only renders when the route is populated (during/after the seed scout).
 */
@Singleton
public class WhispererSeedOverlay extends Overlay
{
	private final Client client;
	private final WhispererAutoMain main;

	@Inject
	public WhispererSeedOverlay(Client client, WhispererAutoMain main)
	{
		this.client = client;
		this.main = main;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<WorldPoint> path = main.seedRouteTiles();
		if (path == null || path.isEmpty()) return null;
		int idx = main.seedWalkIndex();
		int plane = client.getTopLevelWorldView() == null ? 0 : client.getTopLevelWorldView().getPlane();

		// Resolve each tile to a local point once.
		LocalPoint[] lps = new LocalPoint[path.size()];
		for (int i = 0; i < path.size(); i++)
			lps[i] = path.get(i) == null ? null : LocalPoint.fromWorld(client, path.get(i));

		// Draw the path graph: lines connecting consecutive tiles in walk order.
		graphics.setColor(new Color(0, 255, 255, 110));
		for (int i = 0; i + 1 < lps.length; i++)
		{
			if (lps[i] == null || lps[i + 1] == null) continue;
			Point a = Perspective.localToCanvas(client, lps[i], plane);
			Point b = Perspective.localToCanvas(client, lps[i + 1], plane);
			if (a != null && b != null) graphics.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
		}

		// Draw each tile + its walk-order number, coloured by progress:
		// green = already stepped, yellow = current target, cyan = upcoming.
		for (int i = 0; i < lps.length; i++)
		{
			if (lps[i] == null) continue;
			Color c = i < idx ? Color.GREEN : (i == idx ? Color.YELLOW : Color.CYAN);
			Polygon poly = Perspective.getCanvasTileAreaPoly(client, lps[i], 1);
			if (poly != null) OverlayUtil.renderPolygon(graphics, poly, c);

			String label = String.valueOf(i + 1);
			Point txt = Perspective.getCanvasTextLocation(client, graphics, lps[i], label, 0);
			if (txt != null) OverlayUtil.renderTextLocation(graphics, txt, label, c);
		}
		return null;
	}
}
