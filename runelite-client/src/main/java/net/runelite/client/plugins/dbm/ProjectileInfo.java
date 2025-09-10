package net.runelite.client.plugins.dbm;

public class ProjectileInfo
{
    public final int expireTick;
    public final int size;

    public ProjectileInfo(int expireTick, int size)
    {
        this.expireTick = expireTick;
        this.size = size;
    }

    public int getExpireTick()
    {
        return expireTick;
    }

    public int getSize()
    {
        return size;
    }
}
