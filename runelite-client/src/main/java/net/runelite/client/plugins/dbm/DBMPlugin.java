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
        // Expire old projectiles
        trackedTiles.entrySet().removeIf(
                e -> e.getValue().getLifetimeTicks() < client.getTickCount()
        );

        // Add new projectiles
        for (Projectile p : client.getProjectiles())
        {
            int id = p.getId();
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

//                log.debug("{} ({}x{}) affects tile {}", projectile.getName(), projectile.getRadius(), projectile.getRadius(), targetPoint);
            }
        }

        // Push to overlay
        overlay.update(trackedTiles);

        // Pray correct prayer
//        Player local = client.getLocalPlayer();
//        if (local != null) {
//            HeadIcon overhead = local.getOverheadIcon();
//            if (overhead != null) {
//                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Your overhead icon: " + overhead, null);
//            } else {
//                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "No overhead icon active.", null);
//            }
//        }
    }

}