package net.runelite.client.plugins.coxhelper;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPriority;


// Corner rendering taken from Corner Tile Indicators plugin
public class VisualMetronomeTileOverlay extends Overlay
{

    private final Client client;
    private final CoxConfig config;
    private final CoxPlugin plugin;

    @Inject
    public VisualMetronomeTileOverlay(Client client, CoxConfig config, CoxPlugin plugin)
    {
        super(plugin);
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        final Color color = plugin.currentColor;
        final Color fillColor = new Color(plugin.currentColor.getRed(), plugin.currentColor.getGreen(), plugin.currentColor.getBlue(), config.changeFillColorOpacity());
        final float borderWidth = (float) config.currentTileBorderWidth();

        // 1) Floor metronome tile — the fixed E/D/C/A tile Olm's cycle currently points at.
        //    Gated on config so the user can hide it and keep only the player-tile metronome.
        if (config.showFloorMetronomeTile())
        {
            drawFloorTile(graphics, plugin.getOlm().getMetronomeTile(), color, fillColor, borderWidth, plugin.getOlm().getCycleTick());
        }

        // 2) Player-tile metronome — same color, drawn under the player so you can read the beat
        //    without moving your eyes off your character.
        final WorldPoint playerPos = client.getLocalPlayer() != null ? client.getLocalPlayer().getWorldLocation() : null;
        if (playerPos != null)
        {
            final LocalPoint playerDest = LocalPoint.fromWorld(client, playerPos);
            if (playerDest != null)
            {
                renderTile(graphics, playerDest, color, fillColor, borderWidth);
            }
        }

        return null;
    }

    private void drawFloorTile(final Graphics2D graphics, final WorldPoint wp, final Color color, final Color fillColor, final float borderWidth, final int tick)
    {
        if (wp == null) return;
        final LocalPoint dest = LocalPoint.fromWorld(client, wp);
        if (dest == null) return;
        renderTile(graphics, dest, color, fillColor, borderWidth);
        if (tick >= 0)
        {
            final String label = String.valueOf(tick);
            final Point canvasPoint = Perspective.getCanvasTextLocation(client, graphics, dest, label, 0);
            if (canvasPoint != null)
            {
                graphics.setFont(new Font("Arial", Font.BOLD, 14));
                // shadow for legibility over the coloured tile
                OverlayUtil.renderTextLocation(graphics,
                    new Point(canvasPoint.getX() + 1, canvasPoint.getY() + 1), label, Color.BLACK);
                OverlayUtil.renderTextLocation(graphics, canvasPoint, label, Color.WHITE);
            }
        }
    }

    private void renderTile(final Graphics2D graphics, final LocalPoint dest, final Color color, final Color fillColor, final double borderWidth)
    {
        if (dest == null)
        {
            return;
        }

        final Polygon poly = Perspective.getCanvasTilePoly(client, dest);

        if (poly == null)
        {
            return;
        }

        OverlayUtil.renderPolygon(graphics, poly, color, fillColor, new BasicStroke((float) borderWidth));
    }
}
