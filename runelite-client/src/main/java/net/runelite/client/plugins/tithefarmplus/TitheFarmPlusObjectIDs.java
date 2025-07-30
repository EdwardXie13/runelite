package net.runelite.client.plugins.tithefarmplus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectSpawned;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static net.runelite.client.plugins.tithefarmplus.TitheFarmPlusMain.hasInit;
import static net.runelite.client.plugins.tithefarmplus.TitheFarmPlusMain.patchStates;

@Slf4j
public class TitheFarmPlusObjectIDs {
    enum PatchState {
        EMPTY,
        UNWATERED,
        WATERED,
        GROWN,
        DEAD,
        NMP
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

        // loads from south west to north west, left to right
        if (id == 27383 && !TitheFarmPlusMain.isRunning) {
            if (!hasInit) {
                TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.add(event.getTile());
                if (TitheFarmPlusWorldPoints.SWTileOfAllTitheFarmPatches.size() == 41) {
                    TitheFarmPlusWorldPoints.initTitheFarm();
                }
            }
        } else {
            PatchState patchState = getPatchStateFromGameObjectId(id);
            if (patchState == PatchState.NMP)
                return;
            patchStates.set(getPatchNumberFromTile(event.getTile()), patchState);
        }
    }

    private static Integer getPatchNumberFromTile(Tile tile) {
        return TitheFarmPlusWorldPoints.patchNumByTile.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(tile))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);
    }

    public static PatchState getPatchStateFromGameObjectId(int gameObjectId) {
        if(gameObjectId == 27383)
            return PatchState.EMPTY;
        else if(unwatered.contains(gameObjectId))
            return PatchState.UNWATERED;
        else if(watered.contains(gameObjectId))
            return PatchState.WATERED;
        else if(grown.contains(gameObjectId))
            return PatchState.GROWN;
        else if(dead.contains(gameObjectId))
            return PatchState.DEAD;
        else
            return PatchState.NMP;
    }
}
