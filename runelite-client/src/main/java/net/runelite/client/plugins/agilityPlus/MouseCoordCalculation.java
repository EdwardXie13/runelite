package net.runelite.client.plugins.agilityPlus;

import com.github.joonasvali.naturalmouse.api.MouseMotionFactory;
import com.github.joonasvali.naturalmouse.util.FactoryTemplates;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;

import java.awt.Point;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MouseCoordCalculation {
    static Point generatedPoint = null;
    public static void generateCoord(Point point, GameObject gameObject, int sigma) {
        Shape clickbox = gameObject.getClickbox();

        //generate 3 more random points
        List<Point> points = new ArrayList<>();

        while(points.size() < 3) {
            Point newPoint = randomCoord(point, sigma);
            if(isCoordInClickBox(clickbox, newPoint) && isInGame(newPoint))
                points.add(randomCoord(newPoint, sigma));
        }

        generatedPoint = randomClusterPicker(points);
        mouseMove();
    }

    public static void generateCoord(Point point, GroundObject groundObject, int sigma) {
        Shape clickbox = groundObject.getClickbox();

        //generate 3 more random points
        List<Point> points = new ArrayList<>();

        while(points.size() < 3) {
            Point newPoint = randomCoord(point, sigma);
            if(isCoordInClickBox(clickbox, newPoint) && isInGame(newPoint))
                points.add(randomCoord(newPoint, sigma));
        }

        generatedPoint = randomClusterPicker(points);
        mouseMove();
    }

    public static void generateCoord(Point point, DecorativeObject decorativeObject, int sigma) {
        Shape clickbox = decorativeObject.getClickbox();

        //generate 3 more random points
        List<Point> points = new ArrayList<>();

        while(points.size() < 3) {
            Point newPoint = randomCoord(point, sigma);
            if(isCoordInClickBox(clickbox, newPoint) && isInGame(newPoint))
                points.add(randomCoord(newPoint, sigma));
        }

        generatedPoint = randomClusterPicker(points);
        mouseMove();
    }

    public static void generateCoord(Point point, int sigma) {
        //generate 3 more random points
        List<Point> points = new ArrayList<>();

        while(points.size() < 3) {
            Point newPoint = randomCoord(point, sigma);
            points.add(randomCoord(newPoint, sigma));
        }

        generatedPoint = randomClusterPicker(points);
        mouseMove();
    }

    public static boolean isCoordInClickBox(Shape clickbox, Point point) {
        System.out.println("is in box: " + clickbox.contains(point.x, point.y));
        return clickbox.contains(point.x, point.y);
    }

    public static boolean isInGame(Point point) {
        //Rectangle gameBox = new Rectangle(0, 26, 963, 1039);

        return 0 <= point.x && point.x <= 963
                && 26 <= point.y && point.y <= 1039;
    }

    public static Point randomClusterPicker(List<Point> points) {
        try {
            Random random = new Random();
            int num = random.nextInt(10000);

            if (num < 2000) {
                return points.get(0);
            } else if (num < 5000) {
                return points.get(1);
            } else {
                return points.get(2);
            }
        } catch (Exception e) {
            return new Point();
        }
    }

    public static Point randomCoord(Point point, int sigma) {
        int a = (int) Math.floor(sigma/2);
        int minXCoord = point.x-a*a;
        int maxXCoord = point.x+a*a;
        int minYCoord = point.y-a*a;
        int maxYCoord = point.y+a*a;
        // sigma tweaks the spread
        int newXCoord = (int) randomPos(point.x, sigma, minXCoord, maxXCoord);
        int newYCoord = (int) randomPos(point.y, sigma, minYCoord, maxYCoord);

        return new Point(newXCoord, newYCoord);
    }

    public static void mouseMove() {
        MouseMotionFactory fastGamerMotion = FactoryTemplates.createFastGamerMotionFactory();
        try {
            Robot robot = new Robot();
            fastGamerMotion.move(generatedPoint.x, generatedPoint.y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (Exception ignored) { }
    }

    public static long randomPos(int mu, int sigma, int min, int max) {
        Random random = new Random();
        double t = 2 * Math.PI * random.nextDouble();
        double g = mu + (sigma * Math.sqrt(-2 * Math.log(random.nextDouble()))) * Math.cos(t);

        if(g < min)
            g = min;
        if(g > max)
            g = max;

        return Math.round(g);
    }
}
