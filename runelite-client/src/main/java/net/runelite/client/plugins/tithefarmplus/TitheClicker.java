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

import static net.runelite.client.plugins.tithefarmplus.TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint;

@Slf4j
public class TitheClicker {
    private final Client client;
    private final long sessionStartTimeMillis;
    private long lastBreakTimeMillis;
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

    public TitheClicker(Client client, long sessionStartTimeMillis) {
        this.client = client;
        this.sessionStartTimeMillis = sessionStartTimeMillis;
        this.lastBreakTimeMillis = sessionStartTimeMillis;
    }

    public void recordBreakEnded() {
        lastBreakTimeMillis = System.currentTimeMillis();
    }

//    public void performClick(Tile correctTile, List<Tile> possibleMisclickTiles) {
//        double skillLevel = getSkillLevel();
//        boolean allowMisclick = !possibleMisclickTiles.isEmpty();
//
//        if (!allowMisclick) {
//            double reduction = 0.02 + random.nextDouble() * 0.02;
//            skillLevel = Math.max(0, skillLevel - reduction);
//            System.out.printf("No misclick tiles: skillLevel reduced to %.4f%n", skillLevel);
//        }
//
//        double chance = random.nextDouble();
//
//        if (!allowMisclick || chance < skillLevel) {
//            clickTile(correctTile);
//            return;
//        }
//
//        Tile wrongTile = randomFrom(possibleMisclickTiles);
//
//        // Misclick simulation: spam wrong tile, then recover
//        System.out.println("Misclicking tile: " + wrongTile);
//
//        if (wrongTile.screenPoint != null) {
//            spamClickPoint(wrongTile.screenPoint); // wrong click burst
//        } else {
//            clickTile(wrongTile);
//        }
//
//        robot.delay(getReactionDelayMillis(100, 200)); // hesitation delay
//
//        if (correctTile.screenPoint != null) {
//            spamClickPoint(correctTile.screenPoint); // correction burst
//        } else {
//            clickTile(correctTile);
//        }
//    }

    private Tile randomFrom(List<Tile> tiles) {
        return tiles.get(random.nextInt(tiles.size()));
    }

    public void spamClickPoint(Point p) {
        int count = getHumanSpamClickCount();
        for (int i = 0; i < count; i++) {
            clickPoint(p);
            robot.delay(110 + random.nextInt(51)); // 80–180ms between clicks
        }
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
        double mean = 4.0;
        double stdDev = 1.0;

        // Get a Gaussian value, round it, and clamp between 2 and 7
        int clicks = (int) Math.round(mean + random.nextGaussian() * stdDev);
        return Math.max(2, Math.min(5, clicks));
    }

    public int getReactionDelayMillis(int baseMin, int baseMax) {
        long now = System.currentTimeMillis();
        int delay = baseMin + random.nextInt(baseMax - baseMin + 1);

        long sinceBreak = now - lastBreakTimeMillis;
        double minutesSinceBreak = sinceBreak / 60_000.0;

        if (minutesSinceBreak < 1.5) {
            delay -= 50 + random.nextInt(30); // boosted responsiveness after break
        } else if (minutesSinceBreak > 15) {
            delay += 30 + random.nextInt(50); // fatigue delay
        }

        delay = Math.max(150, Math.min(delay, 600)); // clamp
        return delay;
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

    private double getSkillLevel() {
        long elapsedMillis = System.currentTimeMillis() - sessionStartTimeMillis;
        double hoursElapsed = elapsedMillis / 3_600_000.0;
        hoursElapsed = Math.max(0, Math.min(6, hoursElapsed));
        return 0.98 - (0.005 * hoursElapsed); // from 0.98 down to 0.95
    }
}
