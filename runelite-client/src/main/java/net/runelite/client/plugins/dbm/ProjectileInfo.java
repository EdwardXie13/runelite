package net.runelite.client.plugins.dbm;

public class ProjectileInfo
{
    private final String name;
    private final int radius; // 0 = 1x1, 1 = 3x3, 2 = 5x5, etc.
    private final int lifetimeTicks;

    public ProjectileInfo(String name, int radius, int lifetimeTicks)
    {
        this.name = name;
        this.radius = radius;
        this.lifetimeTicks = lifetimeTicks;
    }

    public String getName()
    {
        return name;
    }

    public int getRadius()
    {
        return radius;
    }

    public int getLifetimeTicks()
    {
        return lifetimeTicks;
    }
}


