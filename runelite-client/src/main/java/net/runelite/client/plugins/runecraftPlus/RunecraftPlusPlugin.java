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
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
        name = "Runecraft Plus",
        description = "Show runestone overlays for Zeah RC",
        tags = {"blood", "soul", "rc", "runecrafting", "runestone", "zeah"}
)
public class RunecraftPlusPlugin extends Plugin
{
    private static final int DENSE_RUNESTONE_SOUTH_ID = NullObjectID.NULL_10796;
    private static final int DENSE_RUNESTONE_NORTH_ID = NullObjectID.NULL_8981;
    private static final int BLOOD_ALTAR_ID = ObjectID.BLOOD_ALTAR;
    private static final int DARK_ALTAR_ID = ObjectID.DARK_ALTAR;
    private static final int EDGE_BANK_BOOTH = ObjectID.BANK_BOOTH_10355;
    private static final int CHAOS_RIFT = ObjectID.CHAOS_RIFT;
    private static final int COSMIC_RIFT = ObjectID.COSMIC_RIFT;
    private static final int DEATH_RIFT = ObjectID.DEATH_RIFT;
    private static final int NATURE_RIFT = ObjectID.NATURE_RIFT;
    private static final int LAW_RIFT = ObjectID.LAW_RIFT;
    private static final int STATUE = ObjectID.MYTHIC_STATUE;
    private static final int CAVE_ENTRANCE = ObjectID.CAVE_31807;
    private static final int FountainGlory = ObjectID.FOUNTAIN_OF_UHLD;
    private static final int WRATH_ALTAR_ENTRANCE = ObjectID.MYSTERIOUS_RUINS_34824;
    private static final int WRATH_ALTAR = ObjectID.ALTAR_34772;

    public static final int ABYSS_OBJECT_26187 = 26187;
    public static final int ABYSS_OBJECT_26188 = 26188;
    public static final int ABYSS_OBJECT_26189 = 26189;
    public static final int ABYSS_OBJECT_26190 = 26190;
    public static final int ABYSS_OBJECT_26191 = 26191;
    public static final int ABYSS_OBJECT_26192 = 26192;
    public static final int ABYSS_OBJECT_26208 = 26208;
    public static final int ABYSS_OBJECT_26250 = 26250;
    public static final int ABYSS_OBJECT_26251 = 26251;
    public static final int ABYSS_OBJECT_26252 = 26252;
    public static final int ABYSS_OBJECT_26253 = 26253;
    public static final int ABYSS_OBJECT_26574 = 26574;

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
    private GameObject edgeBankBooth;

    @Getter(AccessLevel.PACKAGE)
    private DecorativeObject chaosRift;

    @Getter(AccessLevel.PACKAGE)
    private DecorativeObject cosmicRift;

    @Getter(AccessLevel.PACKAGE)
    private DecorativeObject deathRift;

    @Getter(AccessLevel.PACKAGE)
    private DecorativeObject natureRift;

    @Getter(AccessLevel.PACKAGE)
    private DecorativeObject lawRift;

    @Getter(AccessLevel.PACKAGE)
    private GameObject statue;

    @Getter(AccessLevel.PACKAGE)
    private GameObject caveEntrance;

    @Getter(AccessLevel.PACKAGE)
    private GameObject fountainGlory;

    @Getter(AccessLevel.PACKAGE)
    private GameObject wrathAltarEntrance;

    @Getter(AccessLevel.PACKAGE)
    private GameObject wrathAltar;

    @Getter(AccessLevel.PACKAGE)
    private boolean denseRunestoneSouthMineable;

    @Getter(AccessLevel.PACKAGE)
    private boolean denseRunestoneNorthMineable;

    @Getter(AccessLevel.PACKAGE)
    private GameObject abyssObject26187;
    @Getter(AccessLevel.PACKAGE)
    private GameObject abyssObject26188;
    @Getter(AccessLevel.PACKAGE)
    private GameObject abyssObject26189;
    @Getter(AccessLevel.PACKAGE)
    private GameObject abyssObject26190;
    @Getter(AccessLevel.PACKAGE)
    private GameObject abyssObject26191;
    @Getter(AccessLevel.PACKAGE)
    private GameObject abyssObject26192;
    @Getter(AccessLevel.PACKAGE)
    private GameObject abyssObject26208;
    @Getter(AccessLevel.PACKAGE)
    private GameObject abyssObject26250;
    @Getter(AccessLevel.PACKAGE)
    private GameObject abyssObject26251;
    @Getter(AccessLevel.PACKAGE)
    private GameObject abyssObject26252;
    @Getter(AccessLevel.PACKAGE)
    private GameObject abyssObject26253;
    @Getter(AccessLevel.PACKAGE)
    private GameObject abyssObject26574;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private DenseRunestoneOverlay denseRunestoneOverlay;

    @Inject
    private RunecraftPlusConfig config;

    @Provides
    RunecraftPlusConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(RunecraftPlusConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(denseRunestoneOverlay);
        if (client.getGameState() == GameState.LOGGED_IN) {
            updateDenseRunestoneState();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(denseRunestoneOverlay);
        setAllVarsNull();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState gameState = event.getGameState();
        switch (gameState)
        {
            case LOADING:
                setAllVarsNull();
                break;
            case CONNECTION_LOST:
            case HOPPING:
            case LOGIN_SCREEN:
            case LOGGED_IN:
                break;
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
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
            case EDGE_BANK_BOOTH:
                if(obj.getWorldLocation().equals(new WorldPoint(3095, 3491, 0)))
                    edgeBankBooth = obj;
                break;
            case STATUE:
                statue = obj;
                break;
            case CAVE_ENTRANCE:
                caveEntrance = obj;
                break;
            case FountainGlory:
                fountainGlory = obj;
                break;
            case WRATH_ALTAR_ENTRANCE:
                wrathAltarEntrance = obj;
                break;
            case WRATH_ALTAR:
                wrathAltar = obj;
                break;
            case ABYSS_OBJECT_26187:
                abyssObject26187 = obj;
                break;
            case ABYSS_OBJECT_26188:
                abyssObject26188 = obj;
                break;
            case ABYSS_OBJECT_26189:
                abyssObject26189 = obj;
                break;
            case ABYSS_OBJECT_26190:
                abyssObject26190 = obj;
                break;
            case ABYSS_OBJECT_26191:
                abyssObject26191 = obj;
                break;
            case ABYSS_OBJECT_26192:
                abyssObject26192 = obj;
                break;
            case ABYSS_OBJECT_26208:
                abyssObject26208 = obj;
                break;
            case ABYSS_OBJECT_26250:
                abyssObject26250 = obj;
                break;
            case ABYSS_OBJECT_26251:
                abyssObject26251 = obj;
                break;
            case ABYSS_OBJECT_26252:
                abyssObject26252 = obj;
                break;
            case ABYSS_OBJECT_26253:
                abyssObject26253 = obj;
                break;
            case ABYSS_OBJECT_26574:
                abyssObject26574 = obj;
                break;
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
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
            case EDGE_BANK_BOOTH:
                edgeBankBooth = null;
                break;
            case STATUE:
                statue = null;
                break;
            case CAVE_ENTRANCE:
                caveEntrance = null;
                break;
            case FountainGlory:
                fountainGlory = null;
                break;
            case WRATH_ALTAR_ENTRANCE:
                wrathAltarEntrance = null;
                break;
            case WRATH_ALTAR:
                wrathAltar = null;
                break;
            case ABYSS_OBJECT_26187:
                abyssObject26187 = null;
                break;
            case ABYSS_OBJECT_26188:
                abyssObject26188 = null;
                break;
            case ABYSS_OBJECT_26189:
                abyssObject26189 = null;
                break;
            case ABYSS_OBJECT_26190:
                abyssObject26190 = null;
                break;
            case ABYSS_OBJECT_26191:
                abyssObject26191 = null;
                break;
            case ABYSS_OBJECT_26192:
                abyssObject26192 = null;
                break;
            case ABYSS_OBJECT_26208:
                abyssObject26208 = null;
                break;
            case ABYSS_OBJECT_26250:
                abyssObject26250 = null;
                break;
            case ABYSS_OBJECT_26251:
                abyssObject26251 = null;
                break;
            case ABYSS_OBJECT_26252:
                abyssObject26252 = null;
                break;
            case ABYSS_OBJECT_26253:
                abyssObject26253 = null;
                break;
            case ABYSS_OBJECT_26574:
                abyssObject26574 = null;
                break;
        }
    }

    @Subscribe
    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event) {
        DecorativeObject obj = event.getDecorativeObject();
        int id = obj.getId();
        switch (id)
        {
            case CHAOS_RIFT:
                chaosRift = obj;
                break;
            case COSMIC_RIFT:
                cosmicRift = obj;
                break;
            case DEATH_RIFT:
                deathRift = obj;
                break;
            case NATURE_RIFT:
                natureRift = obj;
                break;
            case LAW_RIFT:
                lawRift = obj;
                break;
        }
    }

    @Subscribe
    public void onDecorativeObjectDespawned(DecorativeObjectDespawned event) {
        switch (event.getDecorativeObject().getId())
        {
            case CHAOS_RIFT:
                chaosRift = null;
                break;
            case COSMIC_RIFT:
                cosmicRift = null;
                break;
            case DEATH_RIFT:
                deathRift = null;
                break;
            case NATURE_RIFT:
                natureRift = null;
                break;
            case LAW_RIFT:
                lawRift = null;
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

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded)
    {
        if(config.showAbyssClickBox() != AbyssRifts.NONE) {
            denseRunestoneOverlay.swapMenus();
        }
    }

    public void setAllVarsNull() {
        denseRunestoneNorth = null;
        denseRunestoneSouth = null;
        bloodAltar = null;
        darkAltar = null;
        edgeBankBooth = null;
        chaosRift = null;
        cosmicRift = null;
        deathRift = null;
        natureRift = null;
        lawRift = null;
        statue = null;
        caveEntrance = null;
        fountainGlory = null;
        wrathAltarEntrance = null;
        wrathAltar = null;
        abyssObject26187 = null;
        abyssObject26188 = null;
        abyssObject26189 = null;
        abyssObject26190 = null;
        abyssObject26191 = null;
        abyssObject26192 = null;
        abyssObject26208 = null;
        abyssObject26250 = null;
        abyssObject26251 = null;
        abyssObject26252 = null;
        abyssObject26253 = null;
        abyssObject26574 = null;
    }
}
