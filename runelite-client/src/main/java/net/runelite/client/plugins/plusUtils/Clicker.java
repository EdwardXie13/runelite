package net.runelite.client.plugins.plusUtils;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.Random;

@Slf4j
public class Clicker {
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

    public Clicker(Client client) {
        this.client = client;
    }

    public void delay(int delay) {
        robot.delay(delay);
    }

    public void randomDelay(int minMs, int maxMs) {
        robot.delay(minMs + (int)(Math.random() * (maxMs - minMs + 1)));
    }

    public void randomDelayStDev(int minMs, int maxMs, double stdDev)
    {
        // Calculate the mean as the midpoint
        double mean = (minMs + maxMs) / 2.0;

        // Generate a Gaussian value
        double delay = mean + random.nextGaussian() * stdDev;

        // Clamp to min/max
        delay = Math.max(minMs, Math.min(maxMs, delay));

        // Delay
        robot.delay((int) delay);
    }

    public void spamClickPoint(Point p, int min, int max, double mean, double stdDev) {
        int count = getHumanSpamClickCount(min, max, mean, stdDev);
        for (int i = 0; i < count; i++) {
            System.out.println("count++");
            clickPoint(p);
            randomDelay(110, 160);
        }
        delay(500);
    }

    public void spamPoint(Point p) {
        clickPoint(p);
        randomDelay(110, 160);
    }

    public void clickWorldPoint(WorldPoint wp) {
        Point toClick = worldPointToPoint(wp);
        clickPoint(toClick);
        randomDelay(110, 140);
    }

    public void spamClickWorldPoint(WorldPoint wp, int min, int max, double mean, double stdDev) {
        int count = getHumanSpamClickCount(min, max, mean, stdDev);
        for (int i = 0; i < count; i++) {
            Point toClick = worldPointToPoint(wp);
            clickPoint(toClick);
            randomDelay(110, 140);
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

    private Point localPointToPoint(LocalPoint lp) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, lp, 1);
        if (poly == null)
        {
            return new Point(0,0);
        }

        Rectangle boundingBox = poly.getBounds();

        return getCenterOfRectangle(boundingBox);
    }

    public int getHumanSpamClickCount(int min, int max, double mean, double stdDev)
    {
        // Gaussian around your mean
        double value = mean + random.nextGaussian() * stdDev;

        int clicks = (int) Math.round(value);

        // Clamp to range
        return Math.max(min, Math.min(max, clicks));
    }


    private Point getCenterOfRectangle(Rectangle rectangle) {
        // +26 to the Y coordinate because calculations are taken from canvas, not window
        return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY() + 26);
    }

    public Point clickTile(net.runelite.api.Tile tile) {
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
        robot.mouseMove(p.x, p.y);

        robot.delay(minHover + random.nextInt(maxHover - minHover + 1));

        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        robot.delay(minPost + random.nextInt(maxPost - minPost + 1));
    }

    public Point getCentroidFromShape(Client client, Shape polygon)
    {
        if (polygon == null)
            return null;

        // Build screen rectangle
        Rectangle screenRect = new Rectangle(
                0,
                0,
                client.getCanvasWidth(),
                client.getCanvasHeight()
        );

        // Clip polygon to screen
        Area clipped = new Area(polygon);
        clipped.intersect(new Area(screenRect));

        if (clipped.isEmpty())
            return null; // nothing visible

        return getInteriorPoint(clipped);
    }

    public Point getInteriorPoint(Shape polygon)
    {
        Rectangle bounds = polygon.getBounds();

        int margin = 10;
        margin = Math.min(margin, Math.min(bounds.width / 2, bounds.height / 2));

        int step = 3;

        int bestX = 0, bestY = 0;
        double maxDist = -1;

        for (int x = bounds.x + margin; x < bounds.x + bounds.width - margin; x += step)
        {
            for (int y = bounds.y + margin; y < bounds.y + bounds.height - margin; y += step)
            {
                if (!polygon.contains(x, y))
                    continue;

                // distance to bounding box edges
                double dist = Math.min(
                        Math.min(x - bounds.x, bounds.x + bounds.width - x),
                        Math.min(y - bounds.y, bounds.y + bounds.height - y)
                );

                if (dist > maxDist)
                {
                    maxDist = dist;
                    bestX = x;
                    bestY = y;
                }
            }
        }

        if (maxDist < 0)
            return null;

        return new Point(bestX, bestY);
    }

    public void pressKey(int key) {
        robot.keyPress(key);
        robot.delay(100);
        robot.keyRelease(key);
    }
}
