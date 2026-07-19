package net.runelite.client.plugins.dbm;

import java.util.Map;

public class ProjectilesList {
    public static final Map<Integer, ProjectileInfo> PROJECTILES = Map.ofEntries(
        // VORKATH
        Map.entry(1481, new ProjectileInfo("Bomb", 1, 0)), // BOMB
        Map.entry(1482, new ProjectileInfo("Missile", 0, 0)), // ACID MISSLE
        Map.entry(1483, new ProjectileInfo("Acid", 0, 24)), // ACID
        Map.entry(1484, new ProjectileInfo("Spawn", 0,0)) // ZOMBIE_SPAWN
    );

    public static final Map<Integer, ProjectileInfo> GRAPHICS_OBJECTS = Map.ofEntries(

    );
}