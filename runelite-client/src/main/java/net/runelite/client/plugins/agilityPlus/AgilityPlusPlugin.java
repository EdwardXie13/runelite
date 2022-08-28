package net.runelite.client.plugins.agilityPlus;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GroundObject;
import net.runelite.api.ItemID;
import net.runelite.api.Perspective;
import net.runelite.api.ScriptID;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(
        name = "Agility Plus",
        description = "Show overlays for agility",
        tags = {"agility"}
)
@Slf4j
public class AgilityPlusPlugin extends Plugin {
    public static boolean scheduledMove = false;
    public static boolean isIdle = true;
    public long start;

    ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(1);
    
    public final int MARK_OF_GRACE = ItemID.MARK_OF_GRACE;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    private STATUS toggleStatus = STATUS.STOP;

    @Subscribe
    public void onGameTick(GameTick event) {
        toggleStatus();

        if(toggleStatus == STATUS.START) {
            // if idle for ~10s
            // make 'last reset' time to stop repeating resets
            if (checkIdle() && checkLastReset())
                reset();

            if(isNotHealthly()) { return; }

            if(getRegionID() == 9781)
                doGnomeAgility();
            else if(getRegionID() == 13878)
                doCanfisAgility();
            else if(getRegionID() == 10806)
                doSeersAgility();
        }
    }

    private void doGnomeAgility() {
        setCameraZoom(400);

        if(isNearWorldTile(new WorldPoint(2474, 3436, 0), 4)) {
            changeCameraYaw(0);
            try {
                scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeLogBalance, 10, 1);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_LOG) && isIdle) {
            try {
                scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstacleNet1_M, 15, 1);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB1) && isIdle) {
            changeCameraYaw(315);
            try {
                scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeTreeBranch1, 10, 1);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        else if ((isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB2) || isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_CLIMB2_MISCLICK_ZONE))  && isIdle) {
            changeCameraYaw(0);
            try {
                scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeBalancingRope, 10, 1);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_TIGHTROPE) && isIdle) {
            changeCameraYaw(225);
            try{
                scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeTreeBranch2, 10, 1);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_DROP) && isIdle) {
            try {
                scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstacleNet2_L, 15, 1);
            } catch (Throwable t) {
                log.debug(":( " + t.getStackTrace());
            }
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_NET1) && isIdle) {
            changeCameraYaw(1536);
            try {
                scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstaclePipeLeft, 10, 1);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_AFTER_NET2) && isIdle) {
            changeCameraYaw(1536);
            try {
                scheduledGameObjectDelay(AgilityPlusObjectIDs.gnomeObstaclePipeLeft, 10, 1);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        else if (isAtWorldPoint(AgilityPlusWorldPoints.GNOME_LEFT_PIPE) && isIdle && finishedLap()) {
            // rotate camera west to see log balance
            changeCameraYaw(512);
            try {
                scheduledGroundObjectDelay(AgilityPlusObjectIDs.gnomeLogBalance, 10, 2);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
    }

    private void doCanfisAgility() {
        if(checkLevelUp())
            pressKey(KeyEvent.VK_SPACE);
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_START) && isIdle) {
            setCameraZoom(729);
            changeCameraYaw(1438);
            try {
                service.schedule(() -> scheduledGameObjectPointDelay(new Point(47, 969), AgilityPlusObjectIDs.canfisTallTree, 8, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FAIL1) && isIdle && client.getOculusOrbState() == 0) {
            panCameraToCanfisTree();
            setCameraZoom(896);
            try {
                service.schedule(() -> getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(3506, 3488, 0))), 3, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isNearWorldTile(new WorldPoint(3506, 3488, 0), 3) && isIdle) {
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            setCameraZoom(896);
//            847, 377
            try {
                service.schedule(() -> scheduledGameObjectPointDelay(new Point(847, 377), AgilityPlusObjectIDs.canfisTallTree, 8, 1), 2, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FAIL2) && isIdle) {
            setCameraZoom(404);
            changeCameraYaw(1750);
            // custom center 559, 49 (base of the tree)
            try {
                service.schedule(() -> scheduledGameObjectPointDelay(new Point(31, 51), AgilityPlusObjectIDs.canfisTallTree, 8, 2), 2, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        // 1 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIRST_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1) && isIdle) {
            setCameraZoom(758);
            changeCameraYaw(0);
            try {
                service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIRST_ROOF) || isNearWorldTile(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK1, 2)) && isIdle) {
            setCameraZoom(336);
            changeCameraYaw(0);
            try {
                service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFirstRoofGap, 10, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        // 2 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SECOND_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2) && isIdle) {
            setCameraZoom(896);
            try {
                service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SECOND_ROOF) || isNearWorldTile(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK2, 2)) && isIdle) {
            setCameraZoom(532);
            try {
                service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSecondRoofGap, 10, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        // 3 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_THIRD_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3) && isIdle) {
            setCameraZoom(751);
            try {
                service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3), 1500, TimeUnit.MILLISECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_THIRD_ROOF) || isNearWorldTile(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK3, 2)) && isIdle) {
            setCameraZoom(473);
            try {
                service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisThirdRoofGap, 10, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        // 4 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FOURTH_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4) && isIdle) {
            setCameraZoom(512);
            try {
                service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FOURTH_ROOF) || isNearWorldTile(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK4, 2)) && isIdle) {
            setCameraZoom(404);
            try {
                service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFourthRoofGap, 8, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        // 5 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIFTH_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5) && isIdle) {
            setCameraZoom(876);
            changeCameraYaw(512);
            try {
                service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_FIFTH_ROOF) || isNearWorldTile(AgilityPlusWorldPoints.CANFIS_GRACEFULMARK5, 2)) && isIdle) {
            setCameraZoom(719);
            changeCameraYaw(512);
            try {
                service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisFifthRoofGap, 10, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        // 6 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SIXTH_ROOF) && isIdle && client.getOculusOrbState() == 0) {
            panCameraToCanfisSixthRoofGap();
            setCameraZoom(896);
            try {
                service.schedule(() -> getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(3502, 3476, 3))), 3, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isNearWorldTile(new WorldPoint(3502, 3476, 3), 3) && isIdle) {
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            setCameraZoom(896);
            try {
                service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSixthRoofGap, 10, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        // 7 roof
        else if(isAtWorldPoint(AgilityPlusWorldPoints.CANFIS_SEVENTH_ROOF) && isIdle) {
            setCameraZoom(453);
            changeCameraYaw(0);
            try {
                service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.canfisSeventhRoofGap, 8, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
    }

    private void doSeersAgility() {
        //yaw 29 from finish
        //zoom all way out
        if(checkLevelUp())
            pressKey(KeyEvent.VK_SPACE);
        else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FAIL1) && isIdle && client.getOculusOrbState() == 0) {
            setCameraZoom(896);
            panCameraToSeersStartFromFail1();
            try {
                service.schedule(() -> getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START)), 3, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FAIL2) && isIdle && client.getOculusOrbState() == 0) {
            panCameraToSeersStartFromFail2();
            setCameraZoom(896);
            try {
                service.schedule(() -> getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START)), 3, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIRST_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1)) {
            pressKey(KeyEvent.VK_UP, 3000);
            changeCameraYaw(0);
            setCameraZoom(896);
            try {
                service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIRST_ROOF) && isIdle ) {
            pressKey(KeyEvent.VK_UP, 3000);
            changeCameraYaw(0);
            setCameraZoom(346);
            try {
                service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFirstRoofGap, 10, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK1, 2) && isIdle) {
            setCameraZoom(434);
            try {
                service.schedule(() -> scheduledGameObjectDelay(AgilityPlusObjectIDs.seersFirstRoofGap, 10, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1)) {
            setCameraZoom(483);
            changeCameraYaw(0);
            try {
                service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1, 2) && isIdle) {
            if (doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1)) {
                try {
                    service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_1), 500, TimeUnit.MILLISECONDS);
                } catch (Throwable t) {
                    log.debug(":( " + t.getStackTrace());
                }
                isIdle = false;
            } else {
                setCameraZoom(630);
                try {
                    service.schedule(() -> scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10, 1), 1, TimeUnit.SECONDS);
                } catch (Throwable t) {
                    log.debug(":( " + t.getStackTrace());
                }
            }
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2)) {
            setCameraZoom(661);
            try {
                service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.SEERS_SECOND_ROOF) || isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK2_2, 2)) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(581);
            try {
                service.schedule(() -> scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
        // right before 2nd roof Tightrope (case of misclick)
        else if(isNearWorldTile(new WorldPoint(2710, 3490, 2), 3) && isIdle) {
            setCameraZoom(896);
            try {
                service.schedule(() -> scheduledGroundObjectDelay(AgilityPlusObjectIDs.seersTightrope, 10, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_THIRD_ROOF) && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(896);
            try {
                service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if((isAtWorldPoint(AgilityPlusWorldPoints.SEERS_THIRD_ROOF) || isAtWorldPoint(AgilityPlusWorldPoints.SEERS_GRACEFULMARK3))  && isIdle) {
            setCameraZoom(542);
            changeCameraYaw(0);
            try {
                service.schedule(() -> scheduledGameObjectPointDelay(new Point(494, 905), AgilityPlusObjectIDs.seersThirdRoofGap, 10, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_FOURTH_ROOF, 3) && isIdle && client.getOculusOrbState() == 0) {
            setCameraZoom(600);
            panCameraToSeersFourthRoofGap();
            try {
                service.schedule(() -> getWorldPointCoords(LocalPoint.fromWorld(client, new WorldPoint(2702, 3470, 3))), 3, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_FOURTH_ROOF_RUN_POINT, 2) && isIdle) {
            setCameraZoom(896);
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            try {
                service.schedule(() -> scheduledGameObjectPointDelay(new Point(450, 775), AgilityPlusObjectIDs.seersFourthRoofGap, 12, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIFTH_ROOF) && isIdle && doesWorldPointHaveGracefulMark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5)) {
            setCameraZoom(670);
            try {
                service.schedule(() -> checkGracefulmark(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_GRACEFULMARK5, 2) && isIdle) {
            changeCameraYaw(0);
            setCameraZoom(562);
            try {
                service.schedule(() -> scheduledGameObjectPointDelay(new Point(895, 575), AgilityPlusObjectIDs.seersFifthRoofGap, 10, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FIFTH_ROOF) && isIdle) {
            setCameraZoom(896);
            changeCameraYaw(512);
            try {
                service.schedule(() -> scheduledGameObjectPointDelay(new Point(350, 742), AgilityPlusObjectIDs.seersFifthRoofGap, 12, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isAtWorldPoint(AgilityPlusWorldPoints.SEERS_FINISH) && isIdle && client.getOculusOrbState() == 0) {
            changeCameraYaw(0);
            panCameraToSeersStartFromFinish();
            try {
                service.schedule(() -> getWorldPointCoords(LocalPoint.fromWorld(client, AgilityPlusWorldPoints.SEERS_START)), 4, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        } else if(isNearWorldTile(AgilityPlusWorldPoints.SEERS_START, 4) && isIdle) {
            client.setOculusOrbState(0);
            client.setOculusOrbNormalSpeed(12);
            pressKey(KeyEvent.VK_DOWN, 3000);
            setCameraZoom(768);
            changeCameraYaw(1928);

            try {
                service.schedule(() -> scheduledDecorativeObjectDelay(AgilityPlusObjectIDs.seersStartWall, 10, 1), 1, TimeUnit.SECONDS);
            } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
        }
    }

    private Point generatePointsFromPoint(Point point, int sigma) {
        List<Point> points = new ArrayList<>();

        while(points.size() < 3) {
            Point newPoint = MouseCoordCalculation.randomCoord(point, sigma);
            points.add(MouseCoordCalculation.randomCoord(newPoint, sigma));
        }

        return MouseCoordCalculation.randomClusterPicker(points);
    }

    private void getObstacleCenter(GameObject gameObject, int sigma) {
        Shape groundObjectConvexHull = gameObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, gameObject, sigma);
    }

    private void getObstacleCenter(GroundObject groundObject, int sigma) {
        Shape groundObjectConvexHull = groundObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, groundObject, sigma);
    }

    private void getObstacleCenter(DecorativeObject decorativeObject, int sigma) {
        Shape groundObjectConvexHull = decorativeObject.getConvexHull();
        Rectangle groundObjectRectangle = groundObjectConvexHull.getBounds();

        Point obstacleCenter = getCenterOfRectangle(groundObjectRectangle);

        MouseCoordCalculation.generateCoord(obstacleCenter, decorativeObject, sigma);
    }

    private Point getCenterOfRectangle(Rectangle rectangle) {
        // +26 to the Y coordinate because calculations are taken from canvas, not window
        return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY() + 26);
    }

    private void toggleStatus() {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if(chatBoxMessage.equals("1") && toggleStatus == STATUS.STOP) {
            toggleStatus = STATUS.START;
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && toggleStatus == STATUS.START) {
            toggleStatus = STATUS.STOP;
            service.shutdownNow();
            scheduledMove = false;
            isIdle = true;
            System.out.println("status is stop");
        }
    }

    private String stripTargetAnchors(String text) {
        Matcher m = Pattern.compile(">(.*?)<").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private boolean isAtWorldPoint(WorldPoint worldPoint) {
        boolean playerX = client.getLocalPlayer().getWorldLocation().getX() == worldPoint.getX();
        boolean playerY = client.getLocalPlayer().getWorldLocation().getY() == worldPoint.getY();
        return playerX && playerY;
    }

    private void changeCameraYaw(int yaw) {
        if(client.getCameraYaw() == yaw)
            return;
        client.setCameraYawTarget(yaw);
//        north: 0
//        east:1536
//        south:1024
//        west:512
    }

    private void setCameraZoom(int zoom) {
        clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom));
    }

    private int getRegionID() {
        return this.client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private boolean finishedLap() {
        // lowest line in chatbox window 10616888
        Widget chatboxMessage = client.getWidget(10616888);
        if(chatboxMessage != null) {
            Widget[] chatboxMessageChildren = chatboxMessage.getChildren();
            if(chatboxMessageChildren != null) {
                String text = chatboxMessageChildren[0].getText();
                if (text != null) {
                    return text.contains("Agility lap count is") || text.contains("Congratulations, you've just advanced your Agility level");
                }
            }
        }
        return false;
    }

    private void panCameraToCanfisTree() {
        setCameraZoom(300);
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_S, 500);
        try {
            service.schedule(() -> pressKey(KeyEvent.VK_D, 1500), 1, TimeUnit.SECONDS);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    private void panCameraToSeersStartFromFail1() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_S, 500);

        try {
            service.schedule(() -> pressKey(KeyEvent.VK_D, 800), 700, TimeUnit.MILLISECONDS);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    private void panCameraToSeersStartFromFail2() {
        try {
            client.setOculusOrbNormalSpeed(40);
            client.setOculusOrbState(1);
            pressKey(KeyEvent.VK_D, 1200);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    private void panCameraToSeersStartFromFinish() {
        client.setOculusOrbNormalSpeed(40);
        client.setOculusOrbState(1);
        pressKey(KeyEvent.VK_D, 1600);
        try {
            service.schedule(() -> pressKey(KeyEvent.VK_W, 1200), 2000, TimeUnit.MILLISECONDS);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    private void panCameraToCanfisSixthRoofGap() {
        try {
            setCameraZoom(300);
            client.setOculusOrbNormalSpeed(40);
            client.setOculusOrbState(1);
            service.schedule(() -> pressKey(KeyEvent.VK_D, 800), 1, TimeUnit.SECONDS);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    private void panCameraToSeersFourthRoofGap() {
        try {
            client.setOculusOrbNormalSpeed(40);
            client.setOculusOrbState(1);
            service.schedule(() -> pressKey(KeyEvent.VK_A, 600), 1, TimeUnit.SECONDS);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    private boolean checkLevelUp() {
        Widget levelUpMessage = client.getWidget(10617391);
        return !levelUpMessage.isHidden();
    }

    private void pressKey(int key) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyRelease);
    }

    private void pressKey(int key, int ms) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyPress);

        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key);
        try {
            service.schedule(() -> this.client.getCanvas().dispatchEvent(keyRelease), ms, TimeUnit.MILLISECONDS);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    private void scheduledGroundObjectDelay(GroundObject groundObject, int sigma, int seconds) {
        isIdle = false;
        try {
            service.schedule(() -> getObstacleCenter(groundObject, sigma), seconds, TimeUnit.SECONDS);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    private void scheduledGameObjectDelay(GameObject gameObject, int sigma, int seconds) {
        isIdle = false;
        try {
            service.schedule(() -> getObstacleCenter(gameObject, sigma), seconds, TimeUnit.SECONDS);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    private void scheduledDecorativeObjectDelay(DecorativeObject decorativeObject, int sigma, int seconds) {
        isIdle = false;
        try {
            service.schedule(() -> getObstacleCenter(decorativeObject, sigma), seconds, TimeUnit.SECONDS);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    private void scheduledGameObjectPointDelay(Point point, GameObject gameObject, int sigma, int seconds) {
        isIdle = false;
        Point generatedPoint = generatePointsFromPoint(point, sigma);
        try {
            service.schedule(() -> MouseCoordCalculation.generateCoord(generatedPoint, gameObject, sigma), seconds, TimeUnit.SECONDS);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    //possible use would be a ground item that has no clickbox (doubt that will happen)
    private void scheduledGroundObjectPointDelay(Point point, GroundObject groundObject, int sigma, int seconds) {
        isIdle = false;
        try {
            service.schedule(() -> MouseCoordCalculation.generateCoord(point, groundObject, sigma), seconds, TimeUnit.SECONDS);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    private void scheduledPointDelay(Point point, int sigma, int seconds) {
        isIdle = false;
        try {
            service.schedule(() -> MouseCoordCalculation.generateCoord(point, sigma), seconds, TimeUnit.SECONDS);
        } catch (Throwable t) { log.debug(":( " + t.getStackTrace()); }
    }

    private void getWorldPointCoords(final LocalPoint dest) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, dest, 1);
        if (poly == null)
        {
            return;
        }

        Rectangle boundingBox = poly.getBounds();

        Point obstacleCenter = getCenterOfRectangle(boundingBox);

        scheduledPointDelay(obstacleCenter, 10, 1);
    }

    private boolean isNearWorldTile(final WorldPoint target, final int range) {
        return this.client.getLocalPlayer().getWorldLocation().distanceTo2D(target) < range;
    }

    private void checkGracefulmark(WorldPoint worldpoint) {
        if(doesWorldPointHaveGracefulMark(worldpoint))
            getWorldPointCoords(LocalPoint.fromWorld(client, worldpoint));
    }

    private boolean doesWorldPointHaveGracefulMark(WorldPoint worldpoint) {
        Tile[][][] sceneTiles = client.getScene().getTiles();

        int startX = sceneTiles[0][0][0].getWorldLocation().getX();
        int startY = sceneTiles[0][0][0].getWorldLocation().getY();

        int playerPlane = client.getLocalPlayer().getWorldLocation().getPlane();
        List<TileItem> itemsOnTile = sceneTiles[playerPlane][worldpoint.getX()-startX][worldpoint.getY()-startY].getGroundItems();
        List<Integer> tileItemIds = new ArrayList<>();
        if(itemsOnTile != null)
            itemsOnTile.forEach(tileItem -> tileItemIds.add(tileItem.getId()));

        return tileItemIds.contains(MARK_OF_GRACE);
    }

    private boolean checkIdle()
    {
        int idleClientTicks = client.getKeyboardIdleTicks();

        if (client.getMouseIdleTicks() < idleClientTicks)
        {
            idleClientTicks = client.getMouseIdleTicks();
        }

        return idleClientTicks >= 1600;
    }

    private void reset() {
        System.out.println("idle for too long, reset");
        try {
            service.shutdownNow();
        } catch (Throwable t) { log.debug(":( " + Arrays.toString(t.getStackTrace())); }
        scheduledMove = false;
        isIdle = true;
        client.setOculusOrbState(0);
        start = System.currentTimeMillis();
    }

    private boolean checkLastReset() {
        long end = System.currentTimeMillis();
        double sec = (end - start) / 1000F;

        return sec >= 10;
    }

    private boolean isNotHealthly() {
        Widget hpOrbWidget = client.getWidget(10485769);
        if(hpOrbWidget != null) {
            String hpOrbText = hpOrbWidget.getText();
            if(hpOrbText != null) {
                if(!hpOrbText.isEmpty()) {
                    return Integer.parseInt(hpOrbText) < 10;
                }
            }
        }
        return false;
    }

//    private void eatFood() {
//        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
//        if(inventoryWidget != null) {
//            ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
//            if(inventory.contains(ItemID.SALMON) || inventory.contains(ItemID.CAKE) || inventory.contains(ItemID._23_CAKE) || inventory.contains(ItemID.SLICE_OF_CAKE)) {
//                List<Item> inventoryList = new ArrayList<>(Arrays.asList(inventory.getItems()));
//                inventoryList.stream().findFirst().get(
//            }
//        } else {
//            pressKey(KeyEvent.VK_ESCAPE);
//        }
//    }

    enum STATUS{
        START,
        STOP
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged ev)
    {
        if (ev.getGameState() == GameState.LOGIN_SCREEN)
        {
            toggleStatus = STATUS.STOP;
            service.shutdown();
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onGroundObjectDespawned(GroundObjectDespawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }

//    @Subscribe
//    public void onWallObjectSpawned(WallObjectSpawned event)
//    {
//        onTileObject(event.getTile(), null, event.getWallObject());
//    }
//
//    @Subscribe
//    public void onWallObjectDespawned(WallObjectDespawned event)
//    {
//        onTileObject(event.getTile(), event.getWallObject(), null);
//    }
//
    @Subscribe
    public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }

    @Subscribe
    public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
    {
        AgilityPlusObjectIDs.assignObjects(event);
    }
}
