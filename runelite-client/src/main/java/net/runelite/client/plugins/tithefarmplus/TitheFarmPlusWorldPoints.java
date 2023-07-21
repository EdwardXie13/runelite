package net.runelite.client.plugins.tithefarmplus;

import com.google.common.collect.ImmutableList;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.runelite.client.plugins.tithefarmplus.TitheFarmPlusMain.hasInit;
import static net.runelite.client.plugins.tithefarmplus.TitheFarmPlusMain.numOfPatches;

public class TitheFarmPlusWorldPoints {
    public static List<WorldPoint> centerOfALlTitheFarmPatches = new ArrayList<>();
    public static Map<Integer, WorldPoint> patchNumByWorldPoint = new HashMap<>();

    public static List<List<WorldPoint>> patchWalkTilesByWorldPoint = new ArrayList<>();
    public static WorldPoint waterBarrelWorldPoint;

    public static void initTitheFarm() {
        initTitheFarmPatchOrder();
        initTitheFarmPatchTiles();
        initWaterBarrelTile();
        hasInit = true;
    }

    public static void initTitheFarmPatchOrder() {
        // First col
        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(0, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(0));
        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(1, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(1));
        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(2, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(2));
        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(3, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(3));
        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(4, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(4));
        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(5, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(5));
        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(6, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(6));
        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(7, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(7));

        // Second col
        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(8, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(15));
        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(9, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(14));
        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(10, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(13));
        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(11, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(12));

        TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(12, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(11));

        int patchesAmount = numOfPatches;
        if(patchesAmount == 16) {
            TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(13, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(10));
            TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(14, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(9));
            TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(15, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(8));
        } else if(patchesAmount == 20) {
            TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(13, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(20));
            TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(14, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(10));
            TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(15, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(19));
            TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(16, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(9));
            TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(17, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(18));
            TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(18, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(17));
            TitheFarmPlusWorldPoints.patchNumByWorldPoint.put(19, TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(8));
        }
    }

    public static void initTitheFarmPatchTiles() {
        // 0-6
        for (int i = 0; i < 7; i++) {
            TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                    returnRightTilesAscending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(i))
            );
        }

        // 7
        TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnRightTilesDescending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(7)));

        int patchesAmount = numOfPatches;
        if(patchesAmount == 16) {
            // 15 to 8 because it ordered from south to north, 8 - 15 (counting from 0)
            for (int i = 15; i > 7; i--) {
                TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                        returnLeftTilesDescending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(i))
                );
            }

            // patch #8 need Left
            TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                    returnLeftAscending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(8)));
        } else if(patchesAmount == 20) {
            // 15 to 13 because it ordered from south to north, 13 - 15 (counting from 0)
            for (int i = 15; i > 12; i--) {
                TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                        returnLeftTilesDescending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(i))
                );
            }

            // patch #12 must click in 2 special tiles
            TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                returnSouthTiles(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(12)));

            // =====
            // patch #11 need right
            TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                    returnRightTilesDescending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(11)));

            // patch #19 need Left
            TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                    returnLeftTilesDescending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(20)));

            // patch #10 need right
            TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                    returnRightTilesDescending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(10)));

            // patch #18 need Left
            TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                    returnLeftTilesDescending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(19)));

            // patch #9 need right
            TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                    returnRightTilesDescending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(9)));

            // patch #17 need Left
            TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                    returnLeftTilesDescending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(18)));

            // patch #16 need Left
            TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                    returnLeftTilesDescending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(17)));

            // patch #8 need Left
            TitheFarmPlusWorldPoints.patchWalkTilesByWorldPoint.add(
                    returnLeftAscending(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(8)));
        }
    }

    private static void initWaterBarrelTile() {
        WorldPoint patch3Center = TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.get(3);
        waterBarrelWorldPoint = new WorldPoint(
                patch3Center.getX() - 1,
                patch3Center.getY() + 2,
                patch3Center.getPlane()
        );
    }

    private static List<WorldPoint> returnRightTilesAscending(WorldPoint worldPoint) {
        return ImmutableList.of(
                new WorldPoint(worldPoint.getX() + 2, worldPoint.getY() + 1, worldPoint.getPlane()),
                new WorldPoint(worldPoint.getX() + 2, worldPoint.getY(), worldPoint.getPlane())
        );
    }

    private static List<WorldPoint> returnRightTilesDescending(WorldPoint worldPoint) {
        return ImmutableList.of(
                new WorldPoint(worldPoint.getX() + 2, worldPoint.getY() - 1, worldPoint.getPlane()),
                new WorldPoint(worldPoint.getX() + 2, worldPoint.getY(), worldPoint.getPlane())
        );
    }

    private static List<WorldPoint> returnLeftTilesDescending(WorldPoint worldPoint) {
        return ImmutableList.of(
                new WorldPoint(worldPoint.getX() - 2, worldPoint.getY() - 1, worldPoint.getPlane()),
                new WorldPoint(worldPoint.getX() - 2, worldPoint.getY(), worldPoint.getPlane())
        );
    }

    private static List<WorldPoint> returnLeftAscending(WorldPoint worldPoint) {
        return ImmutableList.of(
                new WorldPoint(worldPoint.getX() - 2, worldPoint.getY() + 1, worldPoint.getPlane()),
                new WorldPoint(worldPoint.getX() - 2, worldPoint.getY(), worldPoint.getPlane())
        );
    }

    private static List<WorldPoint> returnSouthTiles(WorldPoint worldPoint) {
        return ImmutableList.of(
                new WorldPoint(worldPoint.getX(), worldPoint.getY() - 2, worldPoint.getPlane()),
                new WorldPoint(worldPoint.getX() + 1, worldPoint.getY() - 2, worldPoint.getPlane())
        );
    }
}
