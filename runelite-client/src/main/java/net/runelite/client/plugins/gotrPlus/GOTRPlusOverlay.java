package net.runelite.client.plugins.gotrPlus;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class GOTRPlusOverlay extends Overlay {
    private final Client client;

    @Inject
    public GOTRPlusOverlay(Client client) {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        this.client = client;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        renderPortals(graphics);
        return null;
    }

    private void renderPortals(Graphics2D graphics) {
        for (Pair<GameObject, BufferedImage> pair : GOTRPlusObjectIDs.getAltarWithIcon()) {
            GameObject gameObject = pair.getKey();
            BufferedImage icon = pair.getValue();

            Point canvasLoc = Perspective.getCanvasImageLocation(client, gameObject.getLocalLocation(), icon, 150);

            if (canvasLoc != null) {
                graphics.drawImage(icon, canvasLoc.getX(), canvasLoc.getY(), null);
            }
        }
    }
}
