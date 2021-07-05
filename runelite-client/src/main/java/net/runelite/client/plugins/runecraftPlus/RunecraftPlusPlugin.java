/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.runecraftPlus;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.NullObjectID;
import net.runelite.api.Varbits;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import net.runelite.api.Client;

@PluginDescriptor(
        name = "RunecraftPlus",
        description = "Show runestone overlays for Zeah RC",
        tags = {"blood", "soul", "rc", "runecrafting", "runestone", "zeah"}
)
public class RunecraftPlusPlugin extends Plugin
{
    private static final int DENSE_RUNESTONE_SOUTH_ID = NullObjectID.NULL_10796;
    private static final int DENSE_RUNESTONE_NORTH_ID = NullObjectID.NULL_8981;

    @Inject
    private Client client;

    @Getter(AccessLevel.PACKAGE)
    private GameObject denseRunestoneSouth;

    @Getter(AccessLevel.PACKAGE)
    private GameObject denseRunestoneNorth;

    @Getter(AccessLevel.PACKAGE)
    private boolean denseRunestoneSouthMineable;

    @Getter(AccessLevel.PACKAGE)
    private boolean denseRunestoneNorthMineable;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private DenseRunestoneOverlay denseRunestoneOverlay;

    @Inject
    private RunecraftPlusConfig config;

    @Inject
    private Notifier notifier;

    @Provides
    RunecraftPlusConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(RunecraftPlusConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(denseRunestoneOverlay);
        updateDenseRunestoneState();
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(denseRunestoneOverlay);
        denseRunestoneNorth = null;
        denseRunestoneSouth = null;
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }

//        if (config.degradingNotification())
//        {
//            if (event.getMessage().contains(POUCH_DECAYED_MESSAGE))
//            {
//                notifier.notify(POUCH_DECAYED_NOTIFICATION_MESSAGE);
//            }
//        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        GameState gameState = event.getGameState();
        switch (gameState)
        {
            case LOADING:
                denseRunestoneNorth = null;
                denseRunestoneSouth = null;
                break;
            case CONNECTION_LOST:
            case HOPPING:
            case LOGIN_SCREEN:
                break;
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject obj = event.getGameObject();
        int id = obj.getId();

        switch (id)
        {
            case DENSE_RUNESTONE_SOUTH_ID:
                denseRunestoneSouth = obj;
                break;
            case DENSE_RUNESTONE_NORTH_ID:
                denseRunestoneNorth = obj;
                break;
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        switch (event.getGameObject().getId())
        {
            case DENSE_RUNESTONE_SOUTH_ID:
                denseRunestoneSouth = null;
                break;
            case DENSE_RUNESTONE_NORTH_ID:
                denseRunestoneNorth = null;
                break;
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        updateDenseRunestoneState();
    }

    private void updateDenseRunestoneState()
    {
        denseRunestoneSouthMineable = client.getVar(Varbits.DENSE_RUNESTONE_SOUTH_DEPLETED) == 0;
        denseRunestoneNorthMineable = client.getVar(Varbits.DENSE_RUNESTONE_NORTH_DEPLETED) == 0;
    }
}
