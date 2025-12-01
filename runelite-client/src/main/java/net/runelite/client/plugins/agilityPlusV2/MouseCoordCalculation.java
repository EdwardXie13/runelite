package net.runelite.client.plugins.agilityPlusV2;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fazecast.jSerialComm.SerialPort;
//import com.monst.polylabel.PolyLabel;
import lombok.experimental.UtilityClass;
import net.runelite.api.Client;
import net.runelite.api.TileObject;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@UtilityClass
public class MouseCoordCalculation {
    Point generatedPoint = null;

    public void generatePointToClick(Shape shape) {
        generatePointToClick(convertShapeToPolygon(shape));
    }

//    public void generatePointToClick(Polygon polygon) {
////        Polygon polygon = convertShapeToPolygon(shape);
//        String jsonFormat = convertPolygonToGeoJSON(polygon);
//        Integer[][][] coordinates = parseCoordinates(jsonFormat);
//        PolyLabel result1 = PolyLabel.polyLabel(coordinates);
//
//        Point centerPoint = new Point((int)result1.getX(), (int)result1.getY());
//        int sigma = (int) Math.sqrt(result1.getDistance());
//        generateCoordNoRand(centerPoint, sigma);
////        mouseMove();
//    }

    public void generateCoord(Client client, Point point, TileObject gameObject, int sigma) {
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

    public void generateCoord(Client client, Point point, int sigma) {
        //generate 3 more random points
        List<Point> points = new ArrayList<>();

        while(points.size() < 3) {
            Point newPoint = randomCoord(point, sigma);
            if(isInGame(newPoint))
                points.add(newPoint);
        }

        generatedPoint = randomClusterPicker(points);
        mouseMove();
    }

    public void generateCoordNoRand(Point point, int sigma) {
        generatedPoint = randomCoord(point, sigma);
        mouseMove();
    }

    private boolean isCoordInClickBox(Shape clickbox, Point point) {
        return clickbox.contains(point.x, point.y);
    }

    private boolean isInGame(Point point) {
        //Rectangle gameBox = new Rectangle(0, 26, 963, 1039);

        return 0 <= point.x && point.x <= 963
                && 26 <= point.y && point.y <= 1039;
    }

    public Point randomClusterPicker(List<Point> points) {
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

    public Point randomCoord(Point point, int sigma) {
        int minXCoord = point.x-sigma*sigma;
        int maxXCoord = point.x+sigma*sigma;
        int minYCoord = point.y-sigma*sigma;
        int maxYCoord = point.y+sigma*sigma;
        // sigma tweaks the spread
        int newXCoord = (int) randomPos(point.x, sigma, minXCoord, maxXCoord);
        int newYCoord = (int) randomPos(point.y, sigma, minYCoord, maxYCoord);

        return new Point(newXCoord, newYCoord);
    }

//    public void moveWindow() {
//        try {
//            Robot robot = new Robot();
//            windMouse(robot, generatedPoint.x, generatedPoint.y);
//            Point displacement = moveWinTo();
//
//            Runtime.getRuntime().exec("cmd /c start cmd.exe /c \"python C:\\Users\\Main\\Documents\\GitHub\\botTest\\src\\main\\java\\moveWin.py " +
//                    displacement.x + " " + displacement.y);
//            robot.delay(300);
//            mouseClick();
//            robot.delay(200);
//            Runtime.getRuntime().exec("cmd /c start cmd.exe /c \"python C:\\Users\\Main\\Documents\\GitHub\\botTest\\src\\main\\java\\moveWin.py " +
//                    (-displacement.x) + " " + (-displacement.y));
//            robot.delay(200);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void mouseMove() {
        try {
            Robot robot = new Robot();
            // tab into window
//            switchToWindow("RuneLite - " + client.getLocalPlayer().getName());
//            robot.delay(200);
            windMouse(robot, generatedPoint.x, generatedPoint.y);
            robot.delay(150);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
//            mouseClick();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void switchToWindow(String windowName) {
        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window instanceof java.awt.Frame) {
                java.awt.Frame frame = (java.awt.Frame) window;
                if (frame.getTitle().contains(windowName)) {
                    frame.toFront();
                    frame.requestFocus();
                    break;
                }
            }
        }
    }

//    public Point moveWinTo() {
//        Point currentMousePos = MouseInfo.getPointerInfo().getLocation();
//        int currentX = currentMousePos.x;
//        int currentY = currentMousePos.y;
//
//        int displacementX = currentX - generatedPoint.x;
//        int displacementY = currentY - generatedPoint.y;
//
//        return new Point(displacementX, displacementY);
//    }

    public void mouseClick() throws IOException {
        SerialPort sp = SerialPort.getCommPort("COM3"); // device name TODO: must be changed
        sp.setComPortParameters(9600, 8, 1, 0); // default connection settings for Arduino
        sp.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0); // block until bytes can be written

        if(!sp.openPort()) {
            System.out.println("\nCOM port NOT available\n"); return;
        }

        sp.getOutputStream().write((byte) 1);
        sp.closePort();
    }

    private long randomPos(int mu, int sigma, int min, int max) {
        Random random = new Random();
        double t = 2 * Math.PI * random.nextDouble();
        double g = mu + (sigma * Math.sqrt(-2 * Math.log(random.nextDouble()))) * Math.cos(t);

        if(g < min)
            g = min;
        if(g > max)
            g = max;

        return Math.round(g);
    }

    private Polygon convertShapeToPolygon(Shape shape) {
        Polygon polygon = new Polygon();

        PathIterator pathIterator = shape.getPathIterator(null);
        float[] coords = new float[6];

        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);

            switch (type) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    polygon.addPoint((int) coords[0], (int) coords[1]);
                    break;

                default:
                    // Ignore other segment types for simplicity
                    break;
            }

            // Move to the next segment
            pathIterator.next();
        }

        return polygon;
    }

    private String convertPolygonToGeoJSON(Polygon polygon) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode geoJsonPolygon = objectMapper.createObjectNode();

            ArrayNode coordinatesArray = objectMapper.createArrayNode();
            ArrayNode firstRingArray = objectMapper.createArrayNode();

            for (int i = 0; i < polygon.npoints; i++) {
                ArrayNode point = objectMapper.createArrayNode();
                point.add(polygon.xpoints[i]);
                point.add(polygon.ypoints[i]);
                firstRingArray.add(point);
            }

            ArrayNode closingPoint = objectMapper.createArrayNode();
            closingPoint.add(polygon.xpoints[0]);
            closingPoint.add(polygon.ypoints[0]);
            firstRingArray.add(closingPoint);

            coordinatesArray.add(firstRingArray);
            geoJsonPolygon.set("coordinates", coordinatesArray);

            return geoJsonPolygon.get("coordinates").toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Integer[][][] parseCoordinates(String coordinatesString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // Use TypeReference to handle the nested structure
            List<List<List<Integer>>> coordinates = objectMapper.readValue(coordinatesString, new TypeReference<List<List<List<Integer>>>>() {});

            // Convert to Integer[][][]
            Integer[][][] integerCoordinates = new Integer[coordinates.size()][][];
            for (int i = 0; i < coordinates.size(); i++) {
                List<List<Integer>> subArray = coordinates.get(i);
                integerCoordinates[i] = new Integer[subArray.size()][];
                for (int j = 0; j < subArray.size(); j++) {
                    List<Integer> coordinate = subArray.get(j);
                    integerCoordinates[i][j] = coordinate.toArray(new Integer[0]);
                }
            }

            return integerCoordinates;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Internal mouse movement algorithm from SMART. Do not use this without credit to either
     * Benjamin J. Land or BenLand100. This was originally synchronized to prevent multiple
     * motions and bannage but functions poorly with DB3.
     *
     * BEST USED IN FIXED MODE
     *
     * @param xs         The x start
     * @param ys         The y start
     * @param xe         The x destination
     * @param ye         The y destination
     * @param gravity    Strength pulling the position towards the destination
     * @param wind       Strength pulling the position in random directions
     * @param minWait    Minimum relative time per step
     * @param maxWait    Maximum relative time per step
     * @param maxStep    Maximum size of a step, prevents out of control motion
     * @param targetArea Radius of area around the destination that should
     *                   trigger slowing, prevents spiraling
     */
    private void windMouseImpl(Robot robot, double xs, double ys, double xe, double ye, double gravity, double wind, double minWait, double maxWait, double maxStep, double targetArea) {
        final double sqrt3 = Math.sqrt(3);
        final double sqrt5 = Math.sqrt(5);

        double dist, veloX = 0, veloY = 0, windX = 0, windY = 0;
        while ((dist = Math.hypot(xs - xe, ys - ye)) >= 1) {
            wind = Math.min(wind, dist);
            if (dist >= targetArea) {
                windX = windX / sqrt3 + (2D * Math.random() - 1D) * wind / sqrt5;
                windY = windY / sqrt3 + (2D * Math.random() - 1D) * wind / sqrt5;
            } else {
                windX /= sqrt3;
                windY /= sqrt3;
                if (maxStep < 3) {
                    maxStep = Math.random() * 3D + 3D;
                } else {
                    maxStep /= sqrt5;
                }
            }
            veloX += windX + gravity * (xe - xs) / dist;
            veloY += windY + gravity * (ye - ys) / dist;
            double veloMag = Math.hypot(veloX, veloY);
            if (veloMag > maxStep) {
                double randomDist = maxStep / 2D + Math.random() * maxStep / 2D;
                veloX = (veloX / veloMag) * randomDist;
                veloY = (veloY / veloMag) * randomDist;
            }
            int lastX = ((int) (Math.round(xs)));
            int lastY = ((int) (Math.round(ys)));
            xs += veloX;
            ys += veloY;
            if ((lastX != Math.round(xs)) || (lastY != Math.round(ys))) {
                robot.mouseMove((int) Math.round(xs), (int) Math.round(ys));
            }
            double step = Math.hypot(xs - lastX, ys - lastY);
            robot.delay((int) Math.round((maxWait - minWait) * (step / maxStep) + minWait));
        }
        new Point((int) xs, (int) ys);
    }

    /**
     * Moves the mouse from the current position to the specified position.
     * Approximates human movement in a way where smoothness and accuracy are
     * relative to speed, as it should be.
     *
     * @param x The x destination
     * @param y The y destination
     */
    private void windMouse(Robot robot, int x, int y) {
        Point c = MouseInfo.getPointerInfo().getLocation();
        double speed = (Math.random() * 15D + 15D) / 9D;
        windMouseImpl(robot, c.x, c.y, x, y, 9D, 3D, 5D / speed, 10D / speed, 10D * speed, 8D * speed);
    }
}
