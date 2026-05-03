package net.runelite.client.plugins.bloodRuneTrue;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BloodRuneTrueWorldPoints {
    public static final WorldPoint DLS_FAIRY_RING = new WorldPoint(3447, 9824, 0);
    public static final WorldPoint FAIRY_RING_CAVE_ENTRANCE = new WorldPoint(3447, 9821, 0);
    public static final WorldPoint FAIRY_RING_CAVE_EXIT = new WorldPoint(3460, 9813, 0);
    public static final WorldPoint SECOND_CAVE_ENTRANCE_TILE = new WorldPoint(3467, 9820, 0);
    public static final WorldPoint SECOND_CAVE_ENTRANCE_EXIT = new WorldPoint(3481, 9824, 0);
    public static final WorldPoint CAVE_SHORTCUT_STAND_TILE = new WorldPoint(3500, 9804, 0);
    public static final WorldPoint CAVE_SHORTCUT_TILE = new WorldPoint(3500, 9803, 0);
    public static final WorldPoint CAVE_SHORTCUT_EXIT = new WorldPoint(3536, 9768, 0);
    public static final WorldPoint CAVE_TO_MYSTERIOUS_RUINS_STAND_TILE = new WorldPoint(3539, 9772, 0);
    public static final WorldPoint CAVE_TO_MYSTERIOUS_RUINS_TILE = new WorldPoint(3540, 9772, 0);
    public static final WorldPoint CAVE_TO_MYSTERIOUS_RUINS_EXIT = new WorldPoint(3543, 9772, 0);
    public static final WorldPoint TRUE_MYSTERIOUS_RUINS_SPAWN_IN_TILE = new WorldPoint(3239, 4832, 0);
    public static final WorldPoint INFRONT_OF_BLOOD_ALTAR = new WorldPoint(3234, 4832, 0);
    public static final WorldPoint CASTLE_WARS_BANK_CHEST = new WorldPoint(2444, 3083, 0);
    public static final WorldPoint CASTLE_WARS_BANK_CHEST_INFRONT = new WorldPoint(2443, 3083, 0);

    public static final List<WorldPoint> CASTLE_WARS_TP_ZONE =
            Collections.unmodifiableList(new ArrayList<>() {{
                for (int x = 2438; x <= 2442; x++)
                    for (int y = 3088; y <= 3094; y++)
                        add(new WorldPoint(x, y, 0));
            }});

    public static WorldPoint INFRONT_OF_POOL;
}


