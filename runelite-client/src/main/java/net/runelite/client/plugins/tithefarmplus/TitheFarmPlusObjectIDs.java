package net.runelite.client.plugins.tithefarmplus;

import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static net.runelite.client.plugins.tithefarmplus.TitheFarmPlusMain.hasInit;
import static net.runelite.client.plugins.tithefarmplus.TitheFarmPlusMain.patchStates;

public class TitheFarmPlusObjectIDs {
    enum PatchState {
        EMPTY,
        UNWATERED,
        WATERED,
        GROWN,
        DEAD
    }

    public static final List<Integer> unwatered = new ArrayList<>(
            Arrays.asList(
                    27384, // GOLOVANOVA_SEEDLING
                    27387, // GOLOVANOVA_PLANT
                    27390, // GOLOVANOVA_PLANT_27390

                    27395, // BOLOGANO_SEEDLING
                    27398, // BOLOGANO_PLANT
                    27401, // BOLOGANO_PLANT_27401

                    27406, // LOGAVANO_SEEDLING
                    27409, // LOGAVANO_PLANT
                    27412 // LOGAVANO_PLANT_27412
            )
    );

    public static final List<Integer> watered = new ArrayList<>(
            Arrays.asList(
                    27385, // GOLOVANOVA_SEEDLING_27385
                    27388, // GOLOVANOVA_PLANT_27388
                    27391, // GOLOVANOVA_PLANT_27391

                    27396, // BOLOGANO_SEEDLING_27396
                    27399, // BOLOGANO_PLANT_27399
                    27402, // BOLOGANO_PLANT_27402

                    27407, // LOGAVANO_SEEDLING_27407
                    27410, // LOGAVANO_PLANT_27410
                    27413 // LOGAVANO_PLANT_27413
            )
    );

    public static final List<Integer> grown = new ArrayList<>(
            Arrays.asList(
                    27393, // GOLOVANOVA_PLANT_27393
                    27404, // BOLOGANO_PLANT_27404
                    27415 // LOGAVANO_PLANT_27415
            )
    );

    public static final List<Integer> dead = new ArrayList<>(
            Arrays.asList(
                    // GOLOVANOVA
                    27386,
                    27389,
                    27392,
                    27394,

                    // BOLOGANO
                    27397,
                    27400,
                    27403,
                    27405,

                    // LOGAVANO
                    27408,
                    27411,
                    27414,
                    27416
            )
    );

    public static void assignObjects(GameObjectSpawned event) {
        GameObject obj = event.getGameObject();

        int id = obj.getId();
        WorldPoint worldPoint = event.getTile().getWorldLocation();

        // loads from south west to north west, left to right
        if(id == 27383 && !TitheFarmPlusMain.isRunning) {
            if(!hasInit) {
                TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.add(convertPatchCenter(worldPoint));
                if(TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.size() == 41) {
                    TitheFarmPlusWorldPoints.initTitheFarm();
                }
            }
        } else if(id == 27383) {
            patchStates.set(getPatchNumberFromWorldPoint(convertPatchCenter(worldPoint)), PatchState.EMPTY);
        } else if(unwatered.contains(id)) {
            patchStates.set(getPatchNumberFromWorldPoint(convertPatchCenter(worldPoint)), PatchState.UNWATERED);
        } else if(watered.contains(id)) {
            patchStates.set(getPatchNumberFromWorldPoint(convertPatchCenter(worldPoint)), PatchState.WATERED);
        } else if(grown.contains(id)) {
            patchStates.set(getPatchNumberFromWorldPoint(convertPatchCenter(worldPoint)), PatchState.GROWN);
        } else if(dead.contains(id)) {
            patchStates.set(getPatchNumberFromWorldPoint(convertPatchCenter(worldPoint)), PatchState.DEAD);
        }
    }

    private static boolean checkIfUselessPatch(WorldPoint worldPoint) {
        int number = TitheFarmPlusWorldPoints.centerOfALlTitheFarmPatches.indexOf(convertPatchCenter(worldPoint));
        System.out.println("the patch # to check if useless: " + number);
        return number < 0 || number > 20 || number == 16;
    }

    private static WorldPoint convertPatchCenter(WorldPoint worldPoint) {
        return new WorldPoint(worldPoint.getX()+1, worldPoint.getY()+1, worldPoint.getPlane());
    }

    private static Integer getPatchNumberFromWorldPoint(WorldPoint worldPoint) {
        return TitheFarmPlusWorldPoints.patchNumByWorldPoint.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(worldPoint))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);
    }
}
