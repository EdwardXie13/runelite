/*
 * Copyright (c) 2018, John Pettenger
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.runecraftPlus;

import java.awt.*;
import java.awt.event.KeyEvent;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.OverlayPosition;

@Slf4j
public class DenseRunestoneOverlay extends Overlay
{
    private static final Color CLICKBOX_BORDER_COLOR = Color.GREEN;
    private static final Color CLICKBOX_FILL_COLOR = new Color(
            CLICKBOX_BORDER_COLOR.getRed(), CLICKBOX_BORDER_COLOR.getGreen(),
            CLICKBOX_BORDER_COLOR.getBlue(), 50);
    private static final Color CLICKBOX_BORDER_HOVER_COLOR = CLICKBOX_BORDER_COLOR.darker();

//    private static final WorldPoint beforeRockClimb = new WorldPoint(1761, 3872, 0);
    private static final WorldPoint darkAltarArea = new WorldPoint(1718, 3882, 0);
    private static final WorldPoint middleArea = new WorldPoint(1737, 3875, 0);
    private static final WorldPoint runZone = new WorldPoint(1721, 3855, 0);
    private static final WorldPoint bloodZone = new WorldPoint(1717, 3835,0);
    private static final WorldPoint returnZone = new WorldPoint(1738, 3852, 0);
    private static final WorldPoint centerOfMine = new WorldPoint(1761, 3860, 0);

    private static final WorldPoint bloodAltarArea[] = {
            new WorldPoint(1715, 3832, 0),
            new WorldPoint(1716, 3832, 0),
            new WorldPoint(1717, 3832, 0),
            new WorldPoint(1718, 3832, 0),
            new WorldPoint(1719, 3832, 0),

            new WorldPoint(1719, 3831, 0),
            new WorldPoint(1719, 3830, 0),
            new WorldPoint(1719, 3829, 0),
            new WorldPoint(1719, 3828, 0),
            new WorldPoint(1719, 3827, 0),

            new WorldPoint(1718, 3827, 0),
            new WorldPoint(1717, 3827, 0),
            new WorldPoint(1716, 3827, 0),
            new WorldPoint(1715, 3827, 0)
    };

    private static final Color Pink_Color = new Color(255,128,255, 255);
//    private static final WorldPoint NorthRock = new WorldPoint(1762, 3856, 0);
//    private static final WorldPoint SouthRock = new WorldPoint(1762, 3848, 0);

    private static final int cameraRunZone = 800;
    private static final int cameraReturnZone = 1930;
    private static final int cameraReset = 0;

    private final Client client;
    private final RunecraftPlusPlugin plugin;
    private final RunecraftPlusConfig config;

    Robot robot;

    @Inject
    private DenseRunestoneOverlay(Client client, RunecraftPlusPlugin plugin, RunecraftPlusConfig config) throws AWTException {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.DYNAMIC);

        this.robot = new Robot();
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        boolean northStoneMineable = plugin.isDenseRunestoneNorthMineable();
        boolean southStoneMineable = plugin.isDenseRunestoneSouthMineable();
        GameObject northStone = plugin.getDenseRunestoneNorth();
        GameObject southStone = plugin.getDenseRunestoneSouth();
        GameObject bloodAltar = plugin.getBloodAltar();
        GameObject darkAltar = plugin.getDarkAltar();

        if(config.disableEmoteMenu()) {
            Widget emoteMenu = client.getWidget(WidgetInfo.EMOTE_CONTAINER);
            if(!emoteMenu.isHidden()) {
                robot.keyPress(KeyEvent.VK_ESCAPE);
            }
        }

        if (config.showClickbox()) {
            //after return
            if(isAtTile(1752, 3854)){
                if(config.rotateCamera() && client.getMapAngle() != cameraReset) {
                    client.setCameraYawTarget(cameraReset);
                }
            }
            //Render closer mine Rock
            if(getInventorySlotID(27) == -1 && isNearWorldTile(centerOfMine, 13)) {
                if ((northStoneMineable && northStone != null && closerRock() == "N") || !southStoneMineable)
                {
                    renderBox(graphics, northStone);
                }
                if ((southStoneMineable && southStone != null && closerRock() == "S") || !northStoneMineable)
                {
                    renderBox(graphics, southStone);
                }
            }
            //at north rock or south rock
            else if(getInventorySlotID(27) == 13445 && isNearWorldTile(centerOfMine, 13)) {
                northRockClimb(graphics);
            }
            //at outer south rock
//            else if(getInventorySlotID(27) == 13445 && isAtTile(1761, 3848)) {
//                renderTile(graphics, LocalPoint.fromWorld(client, beforeRockClimb));
//            }
            //before rock climb coming back
//            else if(getInventorySlotID(27) == -1 && isAtTile(1761, 3872)) {
//                northRockClimb(graphics);
//            }
            //after rock climb or at the altar, then render the middle spot
            else if(getInventorySlotID(27) == 13445 && isAtTile(1761, 3874) || (getInventorySlotID(27) == -1 && isAtTile(1718, 3882))) {
                renderTileArea(graphics, LocalPoint.fromWorld(client, middleArea));
            }
            //if in middle area with dense essence blocks. then render altar
            else if(getInventorySlotID(27) == 13445 && isNearWorldTile(middleArea, 2) && darkAltar != null) {
                renderBox(graphics, darkAltar);
            }
            //if in middle area with dark essence blocks OR nothing then render rock
            else if((getInventorySlotID(27) == 13446 || getInventorySlotID(27) == -1) && isNearWorldTile(middleArea, 2)) {
                northRockClimb(graphics);
            }
            //if at altar after imbue but NO fragments / YES fragments
            else if(getInventorySlotID(27) == 13446 && isNearWorldTile(darkAltarArea, 3)) {
                if(getInventorySlotID(0) == 13446 || getInventorySlotID(2) == -1) { // denseBlocks
                    if(config.rotateCamera() && client.getMapAngle() != cameraReset) {
                        client.setCameraYawTarget(cameraReset);
                    }

                    renderTileArea(graphics, LocalPoint.fromWorld(client, middleArea));
                } else { //getInventorySlotID(0) == 7938 <- fragments
                    //turn camera to see run zone
                    if(config.rotateCamera() && client.getMapAngle() != cameraRunZone) {
                        client.setCameraYawTarget(cameraRunZone);
                    }

                    renderTileArea(graphics, LocalPoint.fromWorld(client, runZone));
                }
            }
            //if at run zone, render blood altar
            else if(getInventorySlotID(27) == 13446 && isNearWorldTile(runZone, 2)) {
                if(config.rotateCamera() && client.getMapAngle() != cameraRunZone) {
                    client.setCameraYawTarget(cameraRunZone);
                }

                renderBox(graphics, bloodAltar);
            }
            //if at blood area, render altar or return zone
            else if(isInBloodArea()) {
                if(getInventorySlotID(0) == 7938 && getInventorySlotID(27) == -1)
                    renderBox(graphics, bloodAltar);
                else {
                    if(config.rotateCamera() && client.getMapAngle() != cameraReturnZone) {
                        client.setCameraYawTarget(cameraReturnZone);
                    }

                    renderTileArea(graphics, LocalPoint.fromWorld(client, returnZone));
                }
            }
            //if at return zone render rockClimb
            else if(getInventorySlotID(27) == -1 && isNearWorldTile(returnZone, 3)) {
                returnRockClimb(graphics);
            }
        } else { //Normal render
            if ((northStoneMineable && northStone != null && closerRock() == "N") || !southStoneMineable)
            {
                renderStone(graphics, northStone);
            }
            if ((southStoneMineable && southStone != null && closerRock() == "S") || !northStoneMineable)
            {
                renderStone(graphics, southStone);
            }
        }

        return null;
    }

    public Boolean isInBloodArea() {
        for(int i = 0; i < bloodAltarArea.length; i++) {
            if(bloodAltarArea[i].getX() == client.getLocalPlayer().getWorldLocation().getX() &&
                    bloodAltarArea[i].getY() == client.getLocalPlayer().getWorldLocation().getY() ) {
                return true;
            }
        }
        return false;
    }

    public Boolean isNearWorldTile(final WorldPoint target, final int range) {
        return this.client.getLocalPlayer().getWorldLocation().distanceTo2D(target) < range;
    }

    public Boolean isAtTile(final int x, final int y) {
        Boolean playerX = client.getLocalPlayer().getWorldLocation().getX() == x;
        Boolean playerY = client.getLocalPlayer().getWorldLocation().getY() == y;
        return playerX && playerY;
    }

    public void northRockClimb(Graphics2D graphics) {
        Tile[][][] sceneTiles = client.getScene().getTiles();

        int startX = sceneTiles[0][0][0].getWorldLocation().getX();
        int startY = sceneTiles[0][0][0].getWorldLocation().getY();

        GroundObject rockClimb = sceneTiles[0][1761-startX][3873-startY].getGroundObject();

        renderRock(graphics, rockClimb);
    }

    public void returnRockClimb(Graphics2D graphics) {
        Tile[][][] sceneTiles = client.getScene().getTiles();

        int startX = sceneTiles[0][0][0].getWorldLocation().getX();
        int startY = sceneTiles[0][0][0].getWorldLocation().getY();

        GroundObject rockClimb = sceneTiles[0][1743-startX][3854-startY].getGroundObject();

        renderRock(graphics, rockClimb);
    }

    public int getInventorySlotID(int i) {
        Item temp = client.getItemContainer(InventoryID.INVENTORY).getItem(i);
        if(temp == null)
            return -1;
        else
            return temp.getId();
    }

    public String closerRock() {
        int NorthRockDist = client.getLocalPlayer().getWorldLocation().distanceTo2D(plugin.getDenseRunestoneNorth().getWorldLocation());
        int SouthRockDist = client.getLocalPlayer().getWorldLocation().distanceTo2D(plugin.getDenseRunestoneSouth().getWorldLocation());

        if(NorthRockDist < SouthRockDist)
            return "N";
        else
            return "S";
    }

    private void renderStone(Graphics2D graphics, GameObject gameObject)
    {
        if (config.showDenseRunestoneClickbox())
        {
            Shape clickbox = gameObject.getClickbox();
            Point mousePosition = client.getMouseCanvasPosition();
            OverlayUtil.renderHoverableArea(
                    graphics, clickbox, mousePosition,
                    CLICKBOX_FILL_COLOR, CLICKBOX_BORDER_COLOR, CLICKBOX_BORDER_HOVER_COLOR);
        }
    }
    private void renderBox(Graphics2D graphics, GameObject gameObject)
    {
        if (config.showClickbox()) {
            Shape clickbox = gameObject.getClickbox();
            Point mousePosition = client.getMouseCanvasPosition();
            OverlayUtil.renderHoverableArea(
                    graphics, clickbox, mousePosition,
                    Pink_Color,
                    Pink_Color,
                    Pink_Color
            );
        }
    }
    private void renderRock(Graphics2D graphics, GroundObject gameObject)
    {
        if (config.showClickbox()) {
            Shape clickbox = gameObject.getClickbox();
            Point mousePosition = client.getMouseCanvasPosition();
            OverlayUtil.renderHoverableArea(
                    graphics, clickbox, mousePosition,
                    Pink_Color,
                    Pink_Color,
                    Pink_Color
            );
        }
    }
    public void renderTile(Graphics2D graphics, final LocalPoint dest)
    {
        Polygon poly = Perspective.getCanvasTilePoly(this.client, dest);
        if (poly == null)
        {
            return;
        }
        graphics.setColor(Pink_Color);
        graphics.draw(poly);
        graphics.setColor(Pink_Color);
        graphics.fill(poly);
    }
    public void renderTileArea(Graphics2D graphics, final LocalPoint dest)
    {
        Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, dest, 3);
        if (poly == null)
        {
            return;
        }
        graphics.setColor(Pink_Color);
        graphics.draw(poly);
        graphics.setColor(Pink_Color);
        graphics.fill(poly);
    }
}