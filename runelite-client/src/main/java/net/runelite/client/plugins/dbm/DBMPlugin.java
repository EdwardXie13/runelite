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
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
        name = "DBM",
        description = "DBM",
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

    private final List<Integer> dontRenderProjectiles = new ArrayList<>(Arrays.asList(1482));

    /**
     * NPC IDs that gate rendering. The overlay only draws when at least one NPC
     * with an ID in this set is currently loaded in the scene.
     *   8061 = Vorkath
     */
    private static final Set<Integer> RENDER_NPCS = new HashSet<>(Arrays.asList(
            8061 // Vorkath
    ));

    @Override
    protected void startUp()
    {
        overlayManager.add(overlay);
        trackedTiles.clear();
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
        if (ProjectilesList.GRAPHICS_OBJECTS.containsKey(obj.getId()))
        {
            log.debug("Graphics object {} at {}", obj.getId(), obj.getLocation());
        }
    }

    @Subscribe
    public void onClientTick(ClientTick tick)
    {
        // Only render while at least one gating NPC is on-screen.
        if (!isRenderNpcOnScene())
        {
            trackedTiles.clear();
            overlay.update(trackedTiles);
            return;
        }

        // Expire old projectiles
        trackedTiles.entrySet().removeIf(
                e -> e.getValue().getLifetimeTicks() < client.getTickCount()
        );

        // Add new projectiles
        for (Projectile p : client.getProjectiles())
        {
            int id = p.getId();

            // don't render
            if (dontRenderProjectiles.contains(id))
                continue;

            WorldPoint targetPoint = p.getTargetPoint();
            if (targetPoint == null)
                continue;

            ProjectileInfo projectile = ProjectilesList.PROJECTILES.get(id);
            if (projectile != null)
            {
                trackedTiles.put(
                    targetPoint,
                    new ProjectileInfo(
                        projectile.getName(),
                        projectile.getRadius(),
                        client.getTickCount() + projectile.getLifetimeTicks()
                    )
                );
            }
        }

        // Push to overlay
        overlay.update(trackedTiles);
    }

    private boolean isRenderNpcOnScene()
    {
        for (NPC npc : client.getNpcs())
        {
            if (npc != null && RENDER_NPCS.contains(npc.getId()))
            {
                return true;
            }
        }
        return false;
    }

}
