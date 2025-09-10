package net.runelite.client.plugins.dbm;

import java.awt.*;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class DBMOverlay extends Overlay
{
    private final Client client;
    private Map<WorldPoint, ProjectileInfo> trackedTiles;

    @Inject
    private DBMOverlay(Client client)
    {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    /**
     * Update tracked tiles with their AoE size + expiration tick
     */
    public void update(Map<WorldPoint, ProjectileInfo> trackedTiles)
    {
        this.trackedTiles = trackedTiles;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (trackedTiles == null) return null;

        for (Map.Entry<WorldPoint, ProjectileInfo> entry : trackedTiles.entrySet())
        {
            WorldPoint wp = entry.getKey();
            ProjectileInfo info = entry.getValue();

            for (int dx = -info.getSize(); dx <= info.getSize(); dx++)
            {
                for (int dy = -info.getSize(); dy <= info.getSize(); dy++)
                {
                    WorldPoint drawPoint = wp.dx(dx).dy(dy);
                    LocalPoint lp = LocalPoint.fromWorld(client, drawPoint);
                    if (lp == null) continue;

                    Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                    if (poly != null)
                    {
                        graphics.setColor(new Color(255, 0, 0, 50));
                        graphics.fill(poly);
                        graphics.setColor(Color.RED);
                        graphics.draw(poly);

                        if (dx == 0 && dy == 0)
                        {
                            graphics.drawString(
                                    "Projectile!",
                                    (int) poly.getBounds().getCenterX(),
                                    (int) poly.getBounds().getCenterY()
                            );
                        }
                    }
                }
            }
        }

        return null;
    }
}
