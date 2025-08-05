package net.runelite.client.plugins.agilityAid;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.agilityPlusV2.AgilityPlusWorldPoints;

import javax.inject.Inject;
import java.awt.*;

import static net.runelite.client.plugins.agilityAid.AgilityAidWorldPoints.*;

@PluginDescriptor(name = "Agility Aid", description = "Agility Aid")
@Slf4j
public class AgilityAidPlugin extends Plugin {
    @Inject private Client client;
    @Inject private ClientThread clientThread;

    @Subscribe
    public void onGameTick(GameTick event) throws AWTException {
        checkLocation();
    }

    public void checkLocation() {
        if(getRegionID() == 10553 || getRegionID() == 10297) {
            doRelleka();
        } else if (getRegionID() == 10547) {
            doArdy();
        }
    }

    public void doRelleka() {
        if (isAtWorldPoint(RELLEKA_START)) {
            setZoomPitchYaw(896, 300, 1024);
        } else if(isAtWorldPoint(AgilityAidWorldPoints.RELLEKA_FIRST_ROOF)) {
            setZoomPitchYaw(390, 512, 0);
        } else if(isAtWorldPoint(AgilityAidWorldPoints.RELLEKA_GRACEFULMARK1)) {
            setZoomPitchYaw(460, 512, 0);
        } else if(isAtWorldPoint(AgilityAidWorldPoints.RELLEKA_SECOND_ROOF)) {
            setZoomPitchYaw(300, 512, 0);
        } else if(isAtWorldPoint(AgilityAidWorldPoints.RELLEKA_THIRD_ROOF)) {
            setZoomPitchYaw(540, 512, 0);
        } else if((isAtWorldPoint(AgilityAidWorldPoints.RELLEKA_GRACEFULMARK3_1) || isAtWorldPoint(AgilityAidWorldPoints.RELLEKA_GRACEFULMARK3_2))) {
            setZoomPitchYaw(570, 512, 1024);
        } else if(isAtWorldPoint(AgilityAidWorldPoints.RELLEKA_FOURTH_ROOF)) {
            setZoomPitchYaw(540, 512, 0);
        } else if((isAtWorldPoint(AgilityAidWorldPoints.RELLEKA_GRACEFULMARK4_1) || isAtWorldPoint(AgilityAidWorldPoints.RELLEKA_GRACEFULMARK4_2))) {
            setZoomPitchYaw(570, 512, 1024);
        } else if(isAtWorldPoint(AgilityAidWorldPoints.RELLEKA_FIFTH_ROOF)) {
            setZoomPitchYaw(500, 512, 1536);
        } else if(isAtWorldPoint(AgilityAidWorldPoints.RELLEKA_SIXTH_ROOF)) {
            setZoomPitchYaw(430, 512, 1024);
        } else if (isAtWorldPoint(RELLEKA_FINISH) || isAtWorldPoint(RELLEKA_FAIL1) || isAtWorldPoint(RELLEKA_FAIL2)) {
            detachCameraPoint(3264, 6847, 512, 1024, 896);
        } else if (isDestinationTile(RELLEKA_START)) {
            resetZoomPitchYaw(200, 512, 1024);
        }
    }

    public void doArdy() {
        if (isAtWorldPoint(ARDY_START)) {
            setZoomPitchYaw(896, 300, 0);
        } else if(isAtWorldPoint(ARDY_FIRST_ROOF)) {
            setZoomPitchYaw(-47, 240, 965);
        } else if(isAtWorldPoint(ARDY_FIRST_ROOF_RUN_POINT)) {
            setZoomPitchYaw(896, 512, 1024);
        } else if(isAtWorldPoint(ARDY_SECOND_ROOF)) {
            setZoomPitchYaw(670, 512, 1420);
        } else if(isAtWorldPoint(ARDY_THIRD_ROOF)) {
            setZoomPitchYaw(709, 512, 1536);
        } else if(isAtWorldPoint(ARDY_FOURTH_ROOF)) {
            setZoomPitchYaw(523, 512, 1926);
        } else if(isAtWorldPoint(ARDY_FIFTH_ROOF)) {
            setZoomPitchYaw(375, 512, 0);
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.ARDY_SIXTH_ROOF)) {
            setZoomPitchYaw(896, 512, 0);
        } else if (isAtWorldPoint(ARDY_FINISH)) {
            setZoomPitchYaw(896, 4, 570);
        } else if(isAtWorldPoint(ARDY_FAIL1) || isAtWorldPoint(ARDY_FAIL2)) {
            detachCameraPoint(7338, 6336, 512, 0, 896);
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

    private void detachCameraPoint(double x, double z, int pitch, int yaw, int zoom) {
        setZoomPitchYaw(zoom, pitch, yaw);
        client.setCameraMode(1);
        client.setCameraFocalPointX(x);
        client.setCameraFocalPointZ(z);
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
