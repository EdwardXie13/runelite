package net.runelite.client.plugins.dbm;

import com.google.inject.Provides;

import java.util.*;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
        name = "DBM",
        description = "Boss mechanic alerts and overlays like WeakAuras/DBM",
        tags = {"pvm", "bossing", "raids", "timers"}
)
public class DBMPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private DBMOverlay overlay;

    private final Map<WorldPoint, ProjectileInfo> trackedTiles = new HashMap<>();

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
        trackedTiles.clear();
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event)
    {
        GraphicsObject obj = event.getGraphicsObject();
        if (ProjectilesList.GRAPHICS_OBJECTS.contains(obj.getId()))
        {
            log.debug("Graphics object {} at {}", obj.getId(), obj.getLocation());
        }
    }

    @Subscribe
    public void onClientTick(ClientTick tick)
    {
        for (Projectile p : client.getProjectiles())
        {
            WorldPoint targetPoint = p.getTargetPoint();
            int id = p.getId();

            // 1x1 projectiles
            if (ProjectilesList.PROJECTILES_1x1.contains(id))
            {
                trackedTiles.put(
                        targetPoint,
                        new ProjectileInfo(client.getTickCount() + 3, 0) // size=0 → 1x1
                );
                log.debug("1x1 Projectile {} affects tile {}", id, targetPoint);
            }

            // 3x3 projectiles
            if (ProjectilesList.PROJECTILES_3x3.contains(id))
            {
                trackedTiles.put(
                        targetPoint,
                        new ProjectileInfo(client.getTickCount() + 3, 1) // size=1 → 3x3
                );
                log.debug("3x3 Projectile {} affects tile {}", id, targetPoint);
            }

            // Future: size=2 → 5x5, size=3 → 7x7, etc.
        }

        // Remove expired
        trackedTiles.entrySet().removeIf(e -> e.getValue().expireTick < client.getTickCount());

        // Push to overlay
        overlay.update(trackedTiles);
    }

}