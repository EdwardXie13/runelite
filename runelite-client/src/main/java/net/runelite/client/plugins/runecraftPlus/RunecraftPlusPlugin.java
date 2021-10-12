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
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static net.runelite.api.AnimationID.IDLE;

@PluginDescriptor(
        name = "RunecraftPlus",
        description = "Show runestone overlays for Zeah RC",
        tags = {"blood", "soul", "rc", "runecrafting", "runestone", "zeah"}
)
public class RunecraftPlusPlugin extends Plugin
{
    private static final int DENSE_RUNESTONE_SOUTH_ID = NullObjectID.NULL_10796;
    private static final int DENSE_RUNESTONE_NORTH_ID = NullObjectID.NULL_8981;
    private static final int BLOOD_ALTAR_ID = ObjectID.BLOOD_ALTAR;
    private static final int DARK_ALTAR_ID = ObjectID.DARK_ALTAR;

    private Instant lastAnimating;
    private int lastAnimation = IDLE;
    private Instant lastInteracting;
    private Actor lastInteract;
    private Instant lastMoving;
    private WorldPoint lastPosition;
    private boolean notifyPosition = false;
    private boolean notifyIdleLogout = true;
    private boolean notify6HourLogout = true;
    private Instant sixHourWarningTime;
    private boolean ready;
    private static final Duration SIX_HOUR_LOGOUT_WARNING_AFTER_DURATION = Duration.ofMinutes(340);

    @Inject
    private Client client;

    @Getter(AccessLevel.PACKAGE)
    private GameObject denseRunestoneSouth;

    @Getter(AccessLevel.PACKAGE)
    private GameObject denseRunestoneNorth;

    @Getter(AccessLevel.PACKAGE)
    private GameObject bloodAltar;

    @Getter(AccessLevel.PACKAGE)
    private GameObject darkAltar;

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
    private StatusOverlayPanel statusOverlayPanel;

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
        overlayManager.add(statusOverlayPanel);
        if (client.getGameState() == GameState.LOGGED_IN) {
            updateDenseRunestoneState();
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(denseRunestoneOverlay);
        overlayManager.remove(statusOverlayPanel);
        denseRunestoneNorth = null;
        denseRunestoneSouth = null;
        bloodAltar = null;
        darkAltar = null;
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        final Actor source = event.getSource();

        if (source != client.getLocalPlayer())
        {
            return;
        }

        final Actor target = event.getTarget();

        // Reset last interact
        if (target != null)
        {
            lastInteract = null;
        }
        else
        {
            lastInteracting = Instant.now();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        final Player local = client.getLocalPlayer();
        final Duration waitDuration = Duration.ofMillis(500);

        if (client.getGameState() != GameState.LOGGED_IN
                || local == null
                // If user has clicked in the last second then they're not idle so don't send idle notification
                || System.currentTimeMillis() - client.getMouseLastPressedMillis() < 1000
                || client.getKeyboardIdleTicks() < 10)
        {
            resetTimers();
            return;
        }

        if (check6hrLogout())
        {
            System.out.println("You are about to log out from being online for 6 hours!");
        }

        if (checkAnimationIdle(waitDuration, local))
        {
            System.out.println("You are now idle!");
            statusOverlayPanel.CurrentStatus = RunecraftActivity.IDLE;
        }

        if (checkMovementIdle(waitDuration, local))
        {
            System.out.println("You have stopped moving!");
            statusOverlayPanel.CurrentStatus = RunecraftActivity.IDLE;
        }

        if (checkInteractionIdle(waitDuration, local))
        {
            System.out.println("You are now idle!");
            statusOverlayPanel.CurrentStatus = RunecraftActivity.IDLE;
        }
    }

    private boolean checkMovementIdle(Duration waitDuration, Player local)
    {
        if (lastPosition == null)
        {
            lastPosition = local.getWorldLocation();
            return false;
        }

        WorldPoint position = local.getWorldLocation();

        if (lastPosition.equals(position))
        {
            if (notifyPosition
                    && local.getAnimation() == IDLE
                    && Instant.now().compareTo(lastMoving.plus(waitDuration)) >= 0)
            {
                notifyPosition = false;
                // Return true only if we weren't just breaking out of an animation
                return lastAnimation == IDLE;
            }
        }
        else
        {
            notifyPosition = true;
            lastPosition = position;
            lastMoving = Instant.now();
            statusOverlayPanel.CurrentStatus = RunecraftActivity.RUNNING;
        }
        return false;
    }

    private void resetTimers()
    {
        final Player local = client.getLocalPlayer();

        // Reset animation idle timer
        lastAnimating = null;
        if (client.getGameState() == GameState.LOGIN_SCREEN || local == null || local.getAnimation() != lastAnimation)
        {
            lastAnimation = IDLE;
        }

        // Reset interaction idle timer
        lastInteracting = null;
        if (client.getGameState() == GameState.LOGIN_SCREEN || local == null || local.getInteracting() != lastInteract)
        {
            lastInteract = null;
        }
    }

    private boolean check6hrLogout()
    {
        if (sixHourWarningTime == null)
        {
            return false;
        }

        if (Instant.now().compareTo(sixHourWarningTime) >= 0)
        {
            if (notify6HourLogout)
            {
                notify6HourLogout = false;
                return true;
            }
        }
        else
        {
            notify6HourLogout = true;
        }

        return false;
    }

    private void changeAnimationStatus(int animation) {
        if(animation == 7201 || animation == 624)
            statusOverlayPanel.CurrentStatus = RunecraftActivity.MINING;
        else if(animation == 645 || animation == 791)
            statusOverlayPanel.CurrentStatus = RunecraftActivity.IMBUE;
        else if(animation == 7202)
            statusOverlayPanel.CurrentStatus = RunecraftActivity.CHISEL;
        else if(animation == 1148 || animation == 839)
            statusOverlayPanel.CurrentStatus = RunecraftActivity.ROCK;
        else if(animation == -1)
            statusOverlayPanel.CurrentStatus = RunecraftActivity.IDLE;
    }

    private boolean checkAnimationIdle(Duration waitDuration, Player local)
    {
        final int animation = local.getAnimation();

        if (animation == IDLE)
        {
            if (lastAnimating != null && Instant.now().compareTo(lastAnimating.plus(waitDuration)) >= 0)
            {
                lastAnimation = IDLE;
                lastAnimating = null;

                // prevent interaction notifications from firing too
                lastInteract = null;
                lastInteracting = null;

                return true;
            }
        }
        else
        {
            lastAnimating = Instant.now();
            changeAnimationStatus(animation);
        }

        return false;
    }

    private boolean checkInteractionIdle(Duration waitDuration, Player local)
    {
        if (lastInteract == null)
        {
            return false;
        }

        final Actor interact = local.getInteracting();

        if (interact == null)
        {
            if (lastInteracting != null
                    && Instant.now().compareTo(lastInteracting.plus(waitDuration)) >= 0)
            {
                lastInteract = null;
                lastInteracting = null;

                // prevent animation notifications from firing too
                lastAnimation = IDLE;
                lastAnimating = null;

                return true;
            }
        }
        else
        {
            lastInteracting = Instant.now();
        }

        return false;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        lastInteracting = null;
        GameState gameState = event.getGameState();
        switch (gameState)
        {
            case LOADING:
                denseRunestoneNorth = null;
                denseRunestoneSouth = null;
                bloodAltar = null;
                darkAltar = null;
                break;
            case CONNECTION_LOST:
                ready = true;
            case HOPPING:
            case LOGIN_SCREEN:
                resetTimers();
                break;
            case LOGGED_IN:
                if (ready)
                {
                    sixHourWarningTime = Instant.now().plus(SIX_HOUR_LOGOUT_WARNING_AFTER_DURATION);
                    ready = false;
                    resetTimers();
                }

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
            case BLOOD_ALTAR_ID:
                bloodAltar = obj;
                break;
            case DARK_ALTAR_ID:
                darkAltar = obj;
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
            case BLOOD_ALTAR_ID:
                bloodAltar = null;
                break;
            case DARK_ALTAR_ID:
                darkAltar = null;
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
