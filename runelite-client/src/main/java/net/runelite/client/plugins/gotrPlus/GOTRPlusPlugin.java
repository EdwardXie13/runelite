package net.runelite.client.plugins.gotrPlus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(name = "GOTR Plus", enabledByDefault = false)
public class GOTRPlusPlugin extends Plugin {
    @Inject
    private ItemManager itemManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private GOTRPlusOverlay gotrPlusOverlay;

    @Override
    protected void startUp() throws Exception
    {
        GOTRPlusObjectIDs.initIcons(itemManager);
        overlayManager.add(gotrPlusOverlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(gotrPlusOverlay);
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GOTRPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        GOTRPlusObjectIDs.assignObjects(event);
    }
}
