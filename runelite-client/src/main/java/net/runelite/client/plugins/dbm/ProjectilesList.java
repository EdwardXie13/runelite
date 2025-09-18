package net.runelite.client.plugins.dbm;

import java.util.List;
import java.util.Arrays;
import java.util.Map;

import static java.util.Map.entry;

public class ProjectilesList {
    public static final Map<Integer, ProjectileInfo> PROJECTILES = Map.ofEntries(
        // VORKATH
        Map.entry(1481, new ProjectileInfo("Bomb", 1, 1)), // BOMB
        Map.entry(1482, new ProjectileInfo("Missile", 1, 0)), // BOMB
        Map.entry(1483, new ProjectileInfo("Acid", 0, 24)), // ACID
        Map.entry(1484, new ProjectileInfo("Spawn", 0,0)) // ZOMBIE_SPAWN
    );

    public static final Map<Integer, ProjectileInfo> GRAPHICS_OBJECTS = Map.ofEntries(

    );
}