package net.runelite.client.plugins.dt2DukeAuto;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;

/**
 * Draws the plugin's currently-believed BFS walk path (the prediction used by the transit
 * hazard check) on the scene, so it can be verified against where the player actually walks.
 * CYAN = path tiles + the line connecting them; RED (filled) = the first tile the check thinks
 * would be hit by a hazard. Gated by {@link DukeAutoConfig#showPathOverlay()}.
 */
@Singleton
public class DukePathOverlay extends Overlay
{
	private final Client client;
	private final DukeAutoMain main;
	private final DukeAutoConfig config;

	@Inject
	public DukePathOverlay(Client client, DukeAutoMain main, DukeAutoConfig config)
	{
		this.client = client;
		this.main = main;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		if (!config.showPathOverlay()) return null;

		List<WorldPoint> path = main.debugPath();
		if (path == null || path.isEmpty()) return null;
		int plane = client.getTopLevelWorldView().getPlane();

		// connect tile centres
		g.setColor(new Color(0, 200, 255, 200));
		g.setStroke(new BasicStroke(2f));
		Point prev = null;
		for (WorldPoint wp : path)
		{
			LocalPoint lp = LocalPoint.fromWorld(client, wp);
			if (lp == null) { prev = null; continue; }
			Point c = Perspective.localToCanvas(client, lp, plane);
			if (c == null) { prev = null; continue; }
			if (prev != null) g.drawLine(prev.getX(), prev.getY(), c.getX(), c.getY());
			prev = c;

			Polygon poly = Perspective.getCanvasTilePoly(client, lp);
			if (poly != null) g.draw(poly);
		}

		// highlight the predicted hit tile
		WorldPoint hit = main.debugHitTile();
		if (hit != null)
		{
			LocalPoint lp = LocalPoint.fromWorld(client, hit);
			if (lp != null)
			{
				Polygon poly = Perspective.getCanvasTilePoly(client, lp);
				if (poly != null)
				{
					g.setColor(new Color(220, 60, 50, 110));
					g.fill(poly);
					g.setColor(new Color(220, 60, 50));
					g.setStroke(new BasicStroke(2f));
					g.draw(poly);
				}
			}
		}
		return null;
	}
}
