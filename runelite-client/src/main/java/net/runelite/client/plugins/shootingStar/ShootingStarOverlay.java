package net.runelite.client.plugins.shootingStar;

import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;

public class ShootingStarOverlay extends Overlay {
    private static final Color BORDER_COLOR = Color.GREEN;
    private static final Color FILL_COLOR = new Color(BORDER_COLOR.getRed(), BORDER_COLOR.getGreen(), BORDER_COLOR.getBlue(), 50);
    private static final Color BORDER_HOVER_COLOR = BORDER_COLOR.darker();

    private final Client client;
    private final ShootingStarPlugin plugin;

    GameObject crashedStar;

    @Inject
    private ShootingStarOverlay(Client client, ShootingStarPlugin plugin) {
        this.client = client;
        this.plugin = plugin;

        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.DYNAMIC);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        crashedStar = plugin.getCrashedStar();

        if(crashedStar != null) {
            renderObject(graphics, crashedStar, BORDER_COLOR, FILL_COLOR, BORDER_HOVER_COLOR);
            OverlayUtil.renderTextLocation(graphics, crashedStar.getCanvasLocation(), idStar(crashedStar), Color.YELLOW);
        }

        return null;
    }

    private void renderObject(Graphics2D graphics, GameObject gameObject, Color fill, Color border, Color borderHover) {
        Shape clickbox = gameObject.getClickbox();
        Point mousePosition = client.getMouseCanvasPosition();
        OverlayUtil.renderHoverableArea(graphics, clickbox, mousePosition, fill, border, borderHover);
    }

    private String idStar(GameObject o) {
        return "level " + ((41240 - o.getId()) - 10) + "0 needed";
    }
}
