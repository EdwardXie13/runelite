package net.runelite.client.plugins.tithefarmplus;

import com.google.common.collect.ImmutableList;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.runelite.client.plugins.tithefarmplus.TitheFarmPlusMain.hasInit;

public class TitheFarmPlusWorldPoints {

    public static List<Tile> SWTileOfAllTitheFarmPatches = new ArrayList<>();
    public static Map<Integer, Tile> patchNumByTile = new HashMap<>();

    public static List<List<WorldPoint>> patchWalkTilesByWorldPoint = new ArrayList<>();
    public static WorldPoint waterBarrelWorldPoint;

    public static void initTitheFarm() {
        initTitheFarmPatchOrder();
        initTitheFarmPatchTiles();
        initWaterBarrelTile();
        hasInit = true;
    }

    public static void initTitheFarmPatchOrder() {
        // First
        TitheFarmPlusWorldPoints.patchNumByTile.put(0, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(0));
        TitheFarmPlusWorldPoints.patchNumByTile.put(1, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(1));
        TitheFarmPlusWorldPoints.patchNumByTile.put(2, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(2));
        TitheFarmPlusWorldPoints.patchNumByTile.put(3, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(3));
        TitheFarmPlusWorldPoints.patchNumByTile.put(4, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(4));
        TitheFarmPlusWorldPoints.patchNumByTile.put(5, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(5));
        TitheFarmPlusWorldPoints.patchNumByTile.put(6, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(6));
        TitheFarmPlusWorldPoints.patchNumByTile.put(7, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(7));

        // Second col
        TitheFarmPlusWorldPoints.patchNumByTile.put(8, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(15));
        TitheFarmPlusWorldPoints.patchNumByTile.put(9, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(14));
        TitheFarmPlusWorldPoints.patchNumByTile.put(10, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(13));
        TitheFarmPlusWorldPoints.patchNumByTile.put(11, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(22));
        TitheFarmPlusWorldPoints.patchNumByTile.put(12, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(21));
        TitheFarmPlusWorldPoints.patchNumByTile.put(13, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(12));

        // Patch before center
        TitheFarmPlusWorldPoints.patchNumByTile.put(14, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(11));

        //Alternate 2nd and 3rd col
        TitheFarmPlusWorldPoints.patchNumByTile.put(15, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(20));
        TitheFarmPlusWorldPoints.patchNumByTile.put(16, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(19));
        TitheFarmPlusWorldPoints.patchNumByTile.put(17, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(10));
        TitheFarmPlusWorldPoints.patchNumByTile.put(18, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(9));
        TitheFarmPlusWorldPoints.patchNumByTile.put(19, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(18));
        TitheFarmPlusWorldPoints.patchNumByTile.put(20, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(17));
        TitheFarmPlusWorldPoints.patchNumByTile.put(21, TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(8));
    }

    public static void initTitheFarmPatchTiles() {
        // 0-6
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(returnRightTilesAscending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(0)));
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(returnRightTilesAscending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(1)));
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(returnRightTilesAscending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(2)));
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(returnTopRightTile(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(3)));
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(returnRightTilesAscending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(4)));
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(returnRightTilesAscending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(5)));
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(returnRightTilesAscending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(6)));

        // 7
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnRightTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(7)));

        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnRightTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(15)));

        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnRightTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(14)));

        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnRightTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(13)));

        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnLeftTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(22)));

        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnLeftTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(21)));

        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnBottomRightTile(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(12)));

        // =====
        // patch #11 need right
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnRightTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(11)));

        // patch #19 need Left
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnLeftTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(20)));

        // patch #18 need Left
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnLeftTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(19)));

        // patch #10 need right
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnRightTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(10)));

        // patch #9 need right
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnRightTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(9)));

        // patch #17 need Left
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnLeftTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(18)));

        // patch #16 need Left
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnLeftTilesDescending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(17)));

        // patch #8 need Left
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnLeftAscending(TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(8)));
    }

    // patch [3]
    private static void initWaterBarrelTile() {
        Tile tile = TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.get(3);
        WorldPoint worldPoint = tile.getWorldLocation();
        waterBarrelWorldPoint = new WorldPoint(
                worldPoint.getX(),
                worldPoint.getY() + 3,
                worldPoint.getPlane()
        );
    }

    private static List<WorldPoint> returnRightTilesAscending(Tile tile) {
        WorldPoint worldPoint = tile.getWorldLocation();
        return ImmutableList.of(
                new WorldPoint(worldPoint.getX() + 3, worldPoint.getY() + 2, worldPoint.getPlane()),
                new WorldPoint(worldPoint.getX() + 3, worldPoint.getY() + 1, worldPoint.getPlane())
        );
    }

    private static List<WorldPoint> returnTopRightTile(Tile tile) {
        WorldPoint worldPoint = tile.getWorldLocation();
        return ImmutableList.of(
                new WorldPoint(worldPoint.getX() + 3, worldPoint.getY() + 2, worldPoint.getPlane())
        );
    }

    private static List<WorldPoint> returnRightTilesDescending(Tile tile) {
        WorldPoint worldPoint = tile.getWorldLocation();
        return ImmutableList.of(
                new WorldPoint(worldPoint.getX() + 3, worldPoint.getY(), worldPoint.getPlane()),
                new WorldPoint(worldPoint.getX() + 3, worldPoint.getY() + 1, worldPoint.getPlane())
        );
    }

    private static List<WorldPoint> returnBottomRightTile(Tile tile) {
        WorldPoint worldPoint = tile.getWorldLocation();
        return ImmutableList.of(new WorldPoint(worldPoint.getX() + 3, worldPoint.getY(), worldPoint.getPlane()));
    }

    private static List<WorldPoint> returnLeftTilesDescending(Tile tile) {
        WorldPoint worldPoint = tile.getWorldLocation();
        return ImmutableList.of(
                new WorldPoint(worldPoint.getX() - 1, worldPoint.getY(), worldPoint.getPlane()),
                new WorldPoint(worldPoint.getX() - 1, worldPoint.getY() + 1, worldPoint.getPlane())
        );
    }

    private static List<WorldPoint> returnLeftAscending(Tile tile) {
        WorldPoint worldPoint = tile.getWorldLocation();
        return ImmutableList.of(
                new WorldPoint(worldPoint.getX() - 1, worldPoint.getY() + 2, worldPoint.getPlane()),
                new WorldPoint(worldPoint.getX() - 1, worldPoint.getY() + 1, worldPoint.getPlane())
        );
    }

    private static List<WorldPoint> returnSouthTiles(Tile tile) {
        WorldPoint worldPoint = tile.getWorldLocation();
        return ImmutableList.of(
                new WorldPoint(worldPoint.getX() + 2, worldPoint.getY() - 1, worldPoint.getPlane()),
                new WorldPoint(worldPoint.getX() + 1, worldPoint.getY() - 1, worldPoint.getPlane())
        );
    }
}
