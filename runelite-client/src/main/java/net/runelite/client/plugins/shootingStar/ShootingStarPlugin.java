package net.runelite.client.plugins.shootingStar;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectID;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@PluginDescriptor(
        name = "ShootingStar",
        description = "Show overlays for Shooting Stars",
        tags = {"crashed star", "shooting star", "mining"}
)
public class ShootingStarPlugin extends Plugin {



    @Inject
    private Client client;

    @Getter(AccessLevel.PACKAGE)
    private GameObject crashedStar;

    private final Set<Integer> crashedStarSet = new HashSet<>(Arrays.asList(
            ObjectID.CRASHED_STAR_41229, //10
            ObjectID.CRASHED_STAR_41228, //20
            ObjectID.CRASHED_STAR_41227, //30
            ObjectID.CRASHED_STAR_41226, //40
            ObjectID.CRASHED_STAR_41225, //50
            ObjectID.CRASHED_STAR_41224, //60
            ObjectID.CRASHED_STAR_41223  //70
    ));

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ShootingStarOverlay shootingStarOverlay;

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(shootingStarOverlay);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(shootingStarOverlay);
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject obj = event.getGameObject();
        int id = obj.getId();

        if(crashedStarSet.contains(id))
            crashedStar = obj;
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        int id = event.getGameObject().getId();

        if(crashedStarSet.contains(id))
            crashedStar = null;
    }
}
