package net.runelite.client.plugins.tithefarmplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.List;
import java.util.Random;

@Slf4j
public class TitheClicker {
    private final Client client;
    private static final Random random = new Random();
    private static Robot robot;

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
            robot = null;
        }
    }

    public TitheClicker(Client client) {
        this.client = client;
    }

    public void spamClickPoint(Point p) {
        int count = getHumanSpamClickCount();
        for (int i = 0; i < count; i++) {
            clickPoint(p);
            robot.delay(110 + random.nextInt(51)); // 80–180ms between clicks
        }
    }

    public void clickWorldPoint(WorldPoint wp) {
        Point toClick = worldPointToPoint(wp);
        clickPoint(toClick);
        robot.delay(110 + random.nextInt(31));
    }

    public void spamClickWorldPoint(WorldPoint wp) {
        int count = getHumanSpamClickCount();
        for (int i = 0; i < count; i++) {
            Point toClick = worldPointToPoint(wp);
            clickPoint(toClick);
            robot.delay(110 + random.nextInt(31)); // 80–180ms between clicks
        }
    }

    private Point worldPointToPoint(WorldPoint worldPoint) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, LocalPoint.fromWorld(client, worldPoint), 1);
        if (poly == null)
        {
            return new Point(0,0);
        }

        Rectangle boundingBox = poly.getBounds();

        return getCenterOfRectangle(boundingBox);
    }

    public int getHumanSpamClickCount() {
        double mean = 2.0;
        double stdDev = 0.6;

        // Get a Gaussian value, round it, and clamp between 2 and 7
        int clicks = (int) Math.round(mean + random.nextGaussian() * stdDev);
        return Math.max(1, Math.min(4, clicks));
    }

    private Point getCenterOfRectangle(Rectangle rectangle) {
        // +26 to the Y coordinate because calculations are taken from canvas, not window
        return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY() + 26);
    }

    Point clickTile(net.runelite.api.Tile tile) {
        WorldPoint tileWP = tile.getWorldLocation();
        LocalPoint destination = LocalPoint.fromWorld(client, new WorldPoint(tileWP.getX() + 1, tileWP.getY() + 1, tileWP.getPlane()));
        Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, destination, 1);
        if (poly == null)
        {
            return new Point(0,0);
        }

        Rectangle boundingBox = poly.getBounds();

        return getCenterOfRectangle(boundingBox);
    }

    public void clickPoint(Point p) {
        robot.mouseMove(p.x, p.y);

        // Minimal fixed hover delay to ensure game registers mouseover (no randomness)
        robot.delay(100);

        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    public void clickPointHumanized(Point p, int minHover, int maxHover, int minPost, int maxPost) {
        log.debug("clickPointHumanized: {}", p);
        robot.mouseMove(p.x, p.y);

        robot.delay(minHover + random.nextInt(maxHover - minHover + 1));

        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        robot.delay(minPost + random.nextInt(maxPost - minPost + 1));
    }
}
