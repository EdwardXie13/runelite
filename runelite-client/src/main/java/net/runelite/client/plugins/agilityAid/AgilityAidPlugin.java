package net.runelite.client.plugins.agilityAid;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.*;

import static net.runelite.client.plugins.agilityAid.AgilityAidWorldPoints.*;

@PluginDescriptor(name = "Agility Aid", description = "Agility Aid")
@Slf4j
public class AgilityAidPlugin extends Plugin {
    @Inject private Client client;
    @Inject private ClientThread clientThread;

    @Subscribe
    public void onClientTick(ClientTick tick) throws AWTException {
        checkLocation();
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned itemSpawned)
    {
        final TileItem item = itemSpawned.getItem();
        final Tile tile = itemSpawned.getTile();

        if (item.getId() == ItemID.MARK_OF_GRACE)
        {
            WorldPoint MOG_TILE = tile.getWorldLocation();
            if(MOG_TILE.equals(CANFIS_GRACEFULMARK1))
                MOG_CANFIS1 = true;
            else if(MOG_TILE.equals(CANFIS_GRACEFULMARK2))
                MOG_CANFIS2 = true;
            else if(MOG_TILE.equals(CANFIS_GRACEFULMARK3))
                MOG_CANFIS3 = true;
            else if(MOG_TILE.equals(CANFIS_GRACEFULMARK4))
                MOG_CANFIS4 = true;
            else if(MOG_TILE.equals(CANFIS_GRACEFULMARK5))
                MOG_CANFIS5 = true;
        }
    }

    @Subscribe
    public void onItemDespawned(ItemDespawned itemDespawned)
    {
        final TileItem item = itemDespawned.getItem();
        final Tile tile = itemDespawned.getTile();

        if (item.getId() == ItemID.MARK_OF_GRACE)
        {
            WorldPoint MOG_TILE = tile.getWorldLocation();
            if(MOG_TILE.equals(CANFIS_GRACEFULMARK1))
                MOG_CANFIS1 = false;
            else if(MOG_TILE.equals(CANFIS_GRACEFULMARK2))
                MOG_CANFIS2 = false;
            else if(MOG_TILE.equals(CANFIS_GRACEFULMARK3))
                MOG_CANFIS3 = false;
            else if(MOG_TILE.equals(CANFIS_GRACEFULMARK4))
                MOG_CANFIS4 = false;
            else if(MOG_TILE.equals(CANFIS_GRACEFULMARK5))
                MOG_CANFIS5 = false;
        }
    }

    public void checkLocation() {
        if(getRegionID() == 13878) {
            doCanfis();
        } else if(getRegionID() == 10553 || getRegionID() == 10297) {
            doRelleka();
        } else if (getRegionID() == 10547) {
            doArdy();
        }
    }

    public void doCanfis() {
        if (isAtWorldPoint(CANFIS_START)) {
            setZoomPitchYaw(730, 110, 1342);
        } else if(isAtWorldPoint(CANFIS_FIRST_ROOF) && !MOG_CANFIS1) {
            setZoomPitchYaw(-47, 157, 1024);
        } else if(isAtWorldPoint(CANFIS_FIRST_ROOF) && MOG_CANFIS1) {
            setZoomPitchYaw(750, 512, 1024);
        } else if(isAtWorldPoint(CANFIS_GRACEFULMARK1)) {
            setZoomPitchYaw(800, 512, 1024);
        } else if(isAtWorldPoint(CANFIS_SECOND_ROOF) && !MOG_CANFIS2) {
            setZoomPitchYaw(340, 20, 1536);
        } else if(isAtWorldPoint(CANFIS_SECOND_ROOF) && MOG_CANFIS2) {
            setZoomPitchYaw(896, 512, 1536);
        } else if(isAtWorldPoint(CANFIS_GRACEFULMARK2)) {
            setZoomPitchYaw(255, 52, 1631);
        } else if(isAtWorldPoint(CANFIS_THIRD_ROOF) && !MOG_CANFIS3) {
            setZoomPitchYaw(-47, 190, 1757);
        } else if(isAtWorldPoint(CANFIS_THIRD_ROOF) && MOG_CANFIS3) {
            setZoomPitchYaw(695, 472, 1748);
        } else if(isAtWorldPoint(CANFIS_GRACEFULMARK3)) {
            setZoomPitchYaw(223, 60, 1700);
        } else if(isAtWorldPoint(CANFIS_FOURTH_ROOF) && !MOG_CANFIS4) {
            setZoomPitchYaw(-47, 103, 2002);
        } else if(isAtWorldPoint(CANFIS_FOURTH_ROOF) && MOG_CANFIS4) {
            setZoomPitchYaw(758, 512, 1836);
        } else if(isAtWorldPoint(CANFIS_GRACEFULMARK4)) {
            setZoomPitchYaw(896, 20, 1024);
        } else if(isAtWorldPoint(CANFIS_FIFTH_ROOF) && !MOG_CANFIS5) {
            setZoomPitchYaw(430, 25, 205);
        } else if(isAtWorldPoint(CANFIS_FIFTH_ROOF) && MOG_CANFIS5) {
            setZoomPitchYaw(870, 512, 0);
        } else if(isAtWorldPoint(CANFIS_GRACEFULMARK5)) {
            setZoomPitchYaw(770, 434, 260);
        } else if(isAtWorldPoint(CANFIS_SIXTH_ROOF)) {
            setZoomPitchYaw(815, 70, 1536);
        } else if(isAtWorldPoint(CANFIS_SEVENTH_ROOF)) {
            setZoomPitchYaw(896, 50, 0);
        } else if (isAtWorldPoint(CANFIS_FAIL)) {
            detachCameraPoint(CANFIS_BUSH, 512, 1024, 896);
        } else if (isDestinationTile(CANFIS_BUSH) || isAtWorldPoint(CANFIS_BUSH)) {
            resetZoomPitchYaw(896, 512, 1024);
        }
    }

    public void doRelleka() {
        if (isAtWorldPoint(RELLEKA_START)) {
            setZoomPitchYaw(896, 300, 1024);
        } else if(isAtWorldPoint(RELLEKA_FIRST_ROOF)) {
            setZoomPitchYaw(390, 512, 0);
        } else if(isAtWorldPoint(RELLEKA_GRACEFULMARK1)) {
            setZoomPitchYaw(460, 512, 0);
        } else if(isAtWorldPoint(RELLEKA_SECOND_ROOF)) {
            setZoomPitchYaw(300, 512, 0);
        } else if(isAtWorldPoint(RELLEKA_THIRD_ROOF)) {
            setZoomPitchYaw(540, 512, 0);
        } else if((isAtWorldPoint(RELLEKA_GRACEFULMARK3_1) || isAtWorldPoint(RELLEKA_GRACEFULMARK3_2))) {
            setZoomPitchYaw(570, 512, 1024);
        } else if(isAtWorldPoint(RELLEKA_FOURTH_ROOF)) {
            setZoomPitchYaw(540, 512, 0);
        } else if((isAtWorldPoint(RELLEKA_GRACEFULMARK4_1) || isAtWorldPoint(RELLEKA_GRACEFULMARK4_2))) {
            setZoomPitchYaw(570, 512, 1024);
        } else if(isAtWorldPoint(RELLEKA_FIFTH_ROOF)) {
            setZoomPitchYaw(500, 512, 1536);
        } else if(isAtWorldPoint(RELLEKA_SIXTH_ROOF)) {
            setZoomPitchYaw(430, 512, 1024);
        } else if (isAtWorldPoint(RELLEKA_FINISH) || isAtWorldPoint(RELLEKA_FAIL1) || isAtWorldPoint(RELLEKA_FAIL2)) {
            detachCameraPoint(RELLEKA_START, 512, 1024, 896);
        } else if (isDestinationTile(RELLEKA_START)) {
            resetZoomPitchYaw(200, 512, 1024);
        }
    }

    public void doArdy() {
        if (isAtWorldPoint(ARDY_START)) {
            setZoomPitchYaw(896, 125, 0);
        } else if(isAtWorldPoint(ARDY_FIRST_ROOF)) {
            setZoomPitchYaw(-47, 240, 965);
        } else if(isAtWorldPoint(ARDY_FIRST_ROOF_RUN_POINT)) {
            setZoomPitchYaw(896, 512, 1024);
        } else if(isAtWorldPoint(ARDY_SECOND_ROOF)) {
            setZoomPitchYaw(680, 512, 1420);
        } else if(isAtWorldPoint(ARDY_THIRD_ROOF)) {
            setZoomPitchYaw(709, 512, 1536);
        } else if(isAtWorldPoint(ARDY_FOURTH_ROOF)) {
            setZoomPitchYaw(523, 512, 1926);
        } else if(isAtWorldPoint(ARDY_FIFTH_ROOF)) {
            setZoomPitchYaw(375, 512, 0);
        } else if(isAtWorldPoint(ARDY_SIXTH_ROOF)) {
            setZoomPitchYaw(896, 512, 0);
        } else if (isAtWorldPoint(ARDY_FINISH)) {
            setZoomPitchYaw(896, 4, 570);
        } else if(isAtWorldPoint(ARDY_FAIL1) || isAtWorldPoint(ARDY_FAIL2)) {
            detachCameraPoint(ARDY_START, 512, 0, 896);
        } else if (isDestinationTile(ARDY_START)) {
            resetZoomPitchYaw(200, 512, 1024);
        }
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private int[] destination() {
        LocalPoint localDestination = client.getLocalDestinationLocation();
        if(localDestination == null) {
            return new int[] {-1, -1};
        }

        WorldPoint worldDestination = WorldPoint.fromLocal(client, localDestination);
        return new int[] {worldDestination.getX(), worldDestination.getY()};
    }

    private boolean isDestinationTile(WorldPoint wp) {
        int[] dest = destination();
        return dest[0] == wp.getX() && dest[1] == wp.getY();
    }

    private void setZoomPitchYaw(int zoom, int pitch, int yaw) {
        setCameraZoom(zoom);
        setCameraPitch(pitch);
        setCameraYaw(yaw);
    }

    private void detachCameraPoint(WorldPoint wp, int pitch, int yaw, int zoom) {
        setZoomPitchYaw(zoom, pitch, yaw);
        centerCameraOnTile(wp);
    }

    public void centerCameraOnTile(WorldPoint tile)
    {
        client.setCameraMode(1);

        LocalPoint lp = LocalPoint.fromWorld(client, tile);
        if (lp == null)
            return;  // Tile not in loaded scene

        client.setCameraFocalPointX(lp.getX());
        client.setCameraFocalPointZ(lp.getY());
    }


    private boolean isAtWorldPoint(WorldPoint worldPoint) {
        boolean playerX = client.getLocalPlayer().getWorldLocation().getX() == worldPoint.getX();
        boolean playerY = client.getLocalPlayer().getWorldLocation().getY() == worldPoint.getY();
        boolean playerPlane = client.getLocalPlayer().getWorldLocation().getPlane() == worldPoint.getPlane();
        return playerX && playerY && playerPlane;
    }

    private void setCameraZoom(int zoom) {
        clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom));
    }

    private void resetZoomPitchYaw(int zoom, int pitch, int yaw) {
        setZoomPitchYaw(zoom, pitch, yaw);
        client.setCameraMode(0);
    }

    private void setCameraYaw(int yaw) {
        if(client.getCameraYaw() == yaw)
            return;
        client.setCameraYawTarget(yaw);
//        north: 0
//        east:1536
//        south:1024
//        west:512
    }

    private void setCameraPitch(int pitch) {
        if(client.getCameraPitch() == pitch)
            return;
        client.setCameraPitchTarget(pitch);
    }
}
