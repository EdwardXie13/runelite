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

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.plugins.agilityPlusV2.MouseCoordCalculation;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DenseRunestoneOverlay extends Overlay
{
    private static final Color CLICKBOX_BORDER_COLOR = Color.GREEN;
    private static final Color CLICKBOX_FILL_COLOR = new Color(CLICKBOX_BORDER_COLOR.getRed(), CLICKBOX_BORDER_COLOR.getGreen(), CLICKBOX_BORDER_COLOR.getBlue(), 50);
    private static final Color CLICKBOX_BORDER_HOVER_COLOR = CLICKBOX_BORDER_COLOR.darker();

    private static final Color Pink_Color = new Color(255,128,255, 255);
    private static final Set<Integer> GOTR_OUTFIT = ImmutableSet.of(
        ItemID.HAT_OF_THE_EYE,
        ItemID.ROBE_TOP_OF_THE_EYE,
        ItemID.ROBE_BOTTOMS_OF_THE_EYE,
        ItemID.BOOTS_OF_THE_EYE
//        ItemID.ROBE_TOP_OF_THE_EYE_RED,
//        ItemID.ROBE_BOTTOMS_OF_THE_EYE_RED,
//        ItemID.HAT_OF_THE_EYE_GREEN,
//        ItemID.ROBE_TOP_OF_THE_EYE_GREEN,
//        ItemID.ROBE_BOTTOMS_OF_THE_EYE_GREEN,
//        ItemID.HAT_OF_THE_EYE_BLUE,
//        ItemID.ROBE_TOP_OF_THE_EYE_BLUE,
//        ItemID.ROBE_BOTTOMS_OF_THE_EYE_BLUE
    );

    Robot robot = new Robot();

    boolean northStoneMineable;
    boolean southStoneMineable;
    GameObject northStone;
    GameObject southStone;
    GameObject bloodAltar;
    GameObject darkAltar;
    GameObject edgeBankBooth;
    DecorativeObject chaosRift;
    DecorativeObject cosmicRift;
    DecorativeObject deathRift;
    DecorativeObject natureRift;
    DecorativeObject lawRift;
    DecorativeObject bloodRift;
    GameObject statue;
    GameObject caveEntrance;
    GameObject fountainGlory;
    GameObject wrathAltarEntrance;
    GameObject wrathAltar;

    private static boolean secondRun = false;

    private static final WorldPoint afterNorthRock = new WorldPoint(1761, 3874, 0);
    private static final WorldPoint returnRunArea = new WorldPoint(1761, 3877, 0);
    private static final WorldPoint runZone = new WorldPoint(1722, 3855, 0);
    private static final WorldPoint centerOfMine = new WorldPoint(1761, 3860, 0);

    private static final WorldPoint[] bloodAltarArea = {
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

    private static final WorldPoint[] darkAltarArea = {
        new WorldPoint(1714, 3885, 0),
        new WorldPoint(1715, 3885, 0),
        new WorldPoint(1716, 3885, 0),
        new WorldPoint(1717, 3885, 0),

        new WorldPoint(1718, 3885, 0),
        new WorldPoint(1718, 3884, 0),
        new WorldPoint(1718, 3883, 0),
        new WorldPoint(1718, 3882, 0),

        new WorldPoint(1718, 3881, 0),
        new WorldPoint(1717, 3881, 0),
        new WorldPoint(1716, 3881, 0),
        new WorldPoint(1715, 3881, 0),

        new WorldPoint(1714, 3881, 0),
        new WorldPoint(1714, 3852, 0),
        new WorldPoint(1714, 3883, 0),
        new WorldPoint(1714, 3884, 0)
    };

    private static final WorldPoint[] wildernessWall = {
        new WorldPoint(3101, 3522, 0),
        new WorldPoint(3102, 3522, 0),
        new WorldPoint(3103, 3522, 0),
        new WorldPoint(3104, 3522, 0),
        new WorldPoint(3105, 3522, 0),
        new WorldPoint(3106, 3522, 0),

        new WorldPoint(3101, 3521, 0),
        new WorldPoint(3102, 3521, 0),
        new WorldPoint(3103, 3521, 0),
        new WorldPoint(3104, 3521, 0),
        new WorldPoint(3105, 3521, 0),
        new WorldPoint(3106, 3521, 0)
    };

    private static final int cameraHeadingAltar = 200;
    private static final int cameraRunZone = 830;
    private static final int cameraReturnZone = 1570;
    private static final int cameraReset = 0;
    private static final int cameraSouth = 1024;

    private static final Set<String> RCpouch = new HashSet<>(Arrays.asList("Small pouch", "Medium pouch", "Large pouch", "Giant pouch", "Colossal pouch"));
    private static final Set<String> Skillcapes = new HashSet<>(Arrays.asList("Runecraft cape", "Runecraft cape(t)", "Agility cape", "Agility cape(t)"));
    private static final Set<String> Capes = new HashSet<>(Arrays.asList("Mythical cape"));
    private static final Set<String> AmuletOfGlory = new HashSet<>(Arrays.asList("Amulet of glory(4)", "Amulet of glory(3)", "Amulet of glory(2)", "Amulet of glory(1)"));
    private static final Set<String> RingOfDueling = new HashSet<>(Arrays.asList("Ring of dueling(8)", "Ring of dueling(7)", "Ring of dueling(6)", "Ring of dueling(5)",
                                                                                 "Ring of dueling(4)", "Ring of dueling(3)", "Ring of dueling(2)", "Ring of dueling(1)"));
    private static final Set<String> UnchargedGlory = new HashSet<>(Arrays.asList("Amulet of glory"));

    private static final WorldPoint EdgeTpReturn = new WorldPoint(3087, 3496, 0);
    private static final WorldPoint FrontStatue = new WorldPoint(2457, 2849, 0);
    private static final WorldPoint EnterStatue = new WorldPoint(1936, 9009, 0);
    private static final WorldPoint WrathAltarEntrance = new WorldPoint(2445, 2818, 0);
    private static final WorldPoint WrathAltar = new WorldPoint(2335, 4832, 0);

    private static Set<GameObject> abyssObjects = new HashSet<>();

//    private static final int BankContainerWidgetID = 786445;
//    private static final int BankInventoryWidgetID = 983043;

    private static final Set<Integer> RCAltarRegionIDs = new HashSet<>(Arrays.asList(
            11339, //air altar
            10059, //body altar
            12875, //blood altar
            9035, //chaos altar
            8523, //cosmic altar
            8779, //death altar
            10571, //earth altar
            10315, //fire altar
            9803, //law altar
            11083, //mind altar
            9547, //nature altar
            9291, //wrath altar
            10827 //water altar
    ));

    private final Client client;
    private final RunecraftPlusPlugin plugin;
    private final RunecraftPlusConfig config;

    @Inject
    private DenseRunestoneOverlay(Client client, RunecraftPlusPlugin plugin, RunecraftPlusConfig config) throws AWTException {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setLayer(OverlayLayer.ABOVE_SCENE);
        setPosition(OverlayPosition.DYNAMIC);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        init();
        disableEmoteMenu();
        disableMusicMenu();

        if(config.showZeahClickbox() && isInChiselRegion()) {
            clickBoxRender(graphics);
        } else if(config.showDenseRunestoneClickbox()) { //Normal render
            closerRockRender(graphics, CLICKBOX_FILL_COLOR, CLICKBOX_BORDER_COLOR, CLICKBOX_BORDER_HOVER_COLOR);
        }

        if(config.showAbyssClickBox() != AbyssRifts.NONE) {
            if(isNearWorldTile(EdgeTpReturn, 2)) {
                changeCameraYaw(cameraReset);
                renderObject(graphics, edgeBankBooth, Pink_Color, Pink_Color, Pink_Color);
            }
//            else if (isAtTile(3094, 3491) && isBankOpen()) {
//                if(isUnchargedGloryEquipped() && !inventoryContains(ItemID.AMULET_OF_GLORY4))
//                    //outline glory in bank
//                    renderItem(graphics, BankContainerWidgetID, ItemID.AMULET_OF_GLORY4);
//                else
//                    //outline glory in inventory
//                    System.out.println("outline glory in inv");
//            }

            if(config.showAbyssClickBox() == AbyssRifts.WRATH)
                renderWrath(graphics);
            else
                abyssClickBoxes(graphics);
        }

        return null;
    }

    private void init() {
        northStoneMineable = plugin.isDenseRunestoneNorthMineable();
        southStoneMineable = plugin.isDenseRunestoneSouthMineable();
        northStone = plugin.getDenseRunestoneNorth();
        southStone = plugin.getDenseRunestoneSouth();
        bloodAltar = plugin.getBloodAltar();
        darkAltar = plugin.getDarkAltar();
        edgeBankBooth = plugin.getEdgeBankBooth();
        chaosRift = plugin.getChaosRift();
        cosmicRift = plugin.getCosmicRift();
        deathRift = plugin.getDeathRift();
        natureRift = plugin.getNatureRift();
        lawRift = plugin.getLawRift();
        bloodRift = plugin.getBloodRift();
        statue = plugin.getStatue();
        caveEntrance = plugin.getCaveEntrance();
        fountainGlory = plugin.getFountainGlory();
        wrathAltarEntrance = plugin.getWrathAltarEntrance();
        wrathAltar = plugin.getWrathAltar();

        abyssObjects.add(plugin.getAbyssObject26187());
        abyssObjects.add(plugin.getAbyssObject26187());
        abyssObjects.add(plugin.getAbyssObject26188());
        abyssObjects.add(plugin.getAbyssObject26189());
        abyssObjects.add(plugin.getAbyssObject26190());
        abyssObjects.add(plugin.getAbyssObject26191());
        abyssObjects.add(plugin.getAbyssObject26192());
        abyssObjects.add(plugin.getAbyssObject26208());
        abyssObjects.add(plugin.getAbyssObject26250());
        abyssObjects.add(plugin.getAbyssObject26251());
        abyssObjects.add(plugin.getAbyssObject26252());
        abyssObjects.add(plugin.getAbyssObject26253());
        abyssObjects.add(plugin.getAbyssObject26574());
    }

    private void clickBoxRender(Graphics2D graphics) {
        //after return
        if(isAtTile(1752, 3854, 0)){
            changeCameraYaw(cameraReset);
        }
        //Render closer mine Rock
        if(getInventorySlotID(27) == -1 && isNearWorldTile(centerOfMine, 13)) {
            changeCameraYaw(cameraReset);
            closerRockRender(graphics, Pink_Color, Pink_Color, Pink_Color);
        }
        //at north rock or south rock
        else if(getInventorySlotID(27) == 13445 && isNearWorldTile(centerOfMine, 13)) {
            changeCameraYaw(cameraReset);
            checkLap();
            northRockClimb(graphics);
        }
        //after rock climb then render the altar
        else if(getInventorySlotID(27) == 13445 && isNearWorldTile(afterNorthRock,1)) {
            changeCameraYaw(cameraHeadingAltar);
            renderObject(graphics, darkAltar, Pink_Color, Pink_Color, Pink_Color);
        }
        //if destination is dark altar
        else if(destinationDarkAltar() && !secondRun && inventoryContainsBlocks()) {
            client.setOculusOrbState(1);
        }
        //if destination is return Area
        else if((isInReturnRun() || isNearWorldTile(afterNorthRock,1)) && getInventorySlotID(27) == -1) {
            client.setOculusOrbState(0);
            northRockClimb(graphics);
        }
        //if at altar after imbue but NO fragments / YES fragments
        else if(isInArea(darkAltarArea) && getInventorySlotID(27) == 13446) {
            if(secondRun) {
                changeCameraYaw(cameraRunZone);
                renderTileArea(graphics, LocalPoint.fromWorld(client, runZone), 3);
            } else {
                renderTileArea(graphics, LocalPoint.fromWorld(client, returnRunArea), 7);
            }
        }
        //if at run zone, render blood altar
        else if(getInventorySlotID(27) == 13446 && isNearWorldTile(runZone, 2)) {
            changeCameraYaw(cameraRunZone);
            renderObject(graphics, bloodAltar, Pink_Color, Pink_Color, Pink_Color);
        }
        //if at blood area, render altar or return zone
        else if(isInArea(bloodAltarArea)) {
            if(getInventorySlotID(0) == -1 && getInventorySlotID(27) == -1) {
                changeCameraYaw(cameraReturnZone);
                returnRockClimb(graphics);
                secondRun = false;
            } else {
                // If inv contains GOTR, render it
                // if
//                for (Integer itemId: GOTR_OUTFIT) {
//                    if(client.getItemContainer(InventoryID.INVENTORY).contains(itemId)) {
//                        renderItem(graphics, 9764864, itemId);
//                    }
//                }
                renderObject(graphics, bloodAltar, Pink_Color, Pink_Color, Pink_Color);
            }
        }
    }

//    private void generatePointToClick(GroundObject gameObject) {
//        Shape convexHull = gameObject.getConvexHull();
//        MouseCoordCalculation.generatePointToClick(convexHull);
//    }

    private void closerRockRender(Graphics2D graphics, Color fill, Color border, Color borderHover) {
        if ((northStoneMineable && northStone != null && closerRock().equals("N")) || !southStoneMineable)
        {
            renderObject(graphics, northStone, fill, border, borderHover);
        }
        if ((southStoneMineable && southStone != null && closerRock().equals("S")) || !northStoneMineable)
        {
            renderObject(graphics, southStone, fill, border, borderHover);
        }
    }

    private void abyssClickBoxes(Graphics2D graphics) {
        if (isAtTile(3094, 3491, 0) && !isBankOpen()) {
            renderWildernessWall(graphics);
        } else if(isInAbyss()) {
            renderRifts(graphics);
        } else if(isAtRCAltar()) {
            //soom in
        }
    }

    private void changeCameraYaw(int yaw) {
        client.setCameraYawTarget(yaw);
    }

    private boolean isInArea(WorldPoint[] area) {
        for (WorldPoint worldPoint : area) {
            if (worldPoint.getX() == client.getLocalPlayer().getWorldLocation().getX() &&
                    worldPoint.getY() == client.getLocalPlayer().getWorldLocation().getY()) {
                return true;
            }
        }
        return false;
    }

    private Boolean isNearWorldTile(final WorldPoint target, final int range) {
        return this.client.getLocalPlayer().getWorldLocation().distanceTo2D(target) < range;
    }

    private Boolean isAtTile(final int x, final int y, final int z) {
        Boolean playerX = client.getLocalPlayer().getWorldLocation().getX() == x;
        Boolean playerY = client.getLocalPlayer().getWorldLocation().getY() == y;
        Boolean playerZ = client.getLocalPlayer().getWorldLocation().getPlane() == z;
        return playerX && playerY && playerZ;
    }

    private void northRockClimb(Graphics2D graphics) {
        Tile[][][] sceneTiles = client.getScene().getTiles();

        int startX = sceneTiles[0][0][0].getWorldLocation().getX();
        int startY = sceneTiles[0][0][0].getWorldLocation().getY();

        GroundObject rockClimb = sceneTiles[0][1761-startX][3873-startY].getGroundObject();
//        generatePointToClick(rockClimb);
        renderObject(graphics, rockClimb);
    }

    private void returnRockClimb(Graphics2D graphics) {
        Tile[][][] sceneTiles = client.getScene().getTiles();

        int startX = sceneTiles[0][0][0].getWorldLocation().getX();
        int startY = sceneTiles[0][0][0].getWorldLocation().getY();

        GroundObject rockClimb = sceneTiles[0][1743-startX][3854-startY].getGroundObject();

        renderObject(graphics, rockClimb);
    }

    private int getInventorySlotID(int i) {
        Item temp = client.getItemContainer(InventoryID.INVENTORY).getItem(i);
        if(temp == null)
            return -1;
        else
            return temp.getId();
    }

    private boolean inventoryContainsBlocks() {
        //contains unimbued blocks, imbued blocks, or fragments
        return client.getItemContainer(InventoryID.INVENTORY).contains(13445) ||
                client.getItemContainer(InventoryID.INVENTORY).contains(13446) ||
                client.getItemContainer(InventoryID.INVENTORY).contains(7938);
    }

    private String closerRock() {
        int NorthRockDist = client.getLocalPlayer().getWorldLocation().distanceTo2D(plugin.getDenseRunestoneNorth().getWorldLocation());
        int SouthRockDist = client.getLocalPlayer().getWorldLocation().distanceTo2D(plugin.getDenseRunestoneSouth().getWorldLocation());

        if(NorthRockDist < SouthRockDist)
            return "N";
        else
            return "S";
    }

    private void renderObject(Graphics2D graphics, GameObject gameObject, Color fill, Color border, Color borderHover) {
        Shape clickbox = gameObject.getClickbox();
        Point mousePosition = client.getMouseCanvasPosition();
        OverlayUtil.renderHoverableArea(graphics, clickbox, mousePosition, fill, border, borderHover);
    }

    private void renderObject(Graphics2D graphics, GroundObject groundObject) {
        Shape clickbox = groundObject.getClickbox();
        Point mousePosition = client.getMouseCanvasPosition();
        OverlayUtil.renderHoverableArea( graphics, clickbox, mousePosition, Pink_Color, Pink_Color, Pink_Color);
    }

    private void renderObject(Graphics2D graphics, DecorativeObject decorativeObject, Color fill, Color border, Color borderHover) {
        Shape clickbox = decorativeObject.getClickbox();
        Point mousePosition = client.getMouseCanvasPosition();
        OverlayUtil.renderHoverableArea(graphics, clickbox, mousePosition, fill, border, borderHover);
    }

//    private void renderItem(Graphics2D graphics, int itemID) {
//        Widget widget = client.getWidget(InterfaceID.INVENTORY, 0);
//        List<Widget> container = new ArrayList<>(Arrays.asList(client.getWidget(widgetID).getChildren()));
//        for(Widget item : container) {
//            if(item.getItemId() == itemID) {
////                Polygon poly = new Polygon.Rectangle(item.getBounds());
//                Rectangle rectangle = new Rectangle(item.getBounds());
//                graphics.setColor(Pink_Color);
//                graphics.fill(rectangle);
//            }
//        }
//    }

    private void renderTileArea(Graphics2D graphics, final LocalPoint dest, int size) {
        Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, dest, size);
        if (poly == null)
        {
            return;
        }
        graphics.setColor(Pink_Color);
        graphics.draw(poly);
        graphics.setColor(Pink_Color);
        graphics.fill(poly);
    }

    private void disableEmoteMenu() {
        if(config.disableEmoteMenu() && isInChiselRegion()) {
            Widget emoteMenu = client.getWidget(WidgetInfo.EMOTE_CONTAINER);
            if(!emoteMenu.isHidden()) {
                pressKey(KeyEvent.VK_ESCAPE, 100);
            }
        }
    }

    private void disableMusicMenu() {
        if(config.disableMusicMenu() && isInChiselRegion()) {
            Widget musicMenu = client.getWidget(WidgetInfo.MUSIC_WINDOW);
            if(!musicMenu.isHidden()) {
                pressKey(KeyEvent.VK_ESCAPE, 100);
            }
        }
    }

    private void checkLap() {
        if(getInventorySlotID(0) == 7938 && !secondRun) {
            secondRun = true;
        }
    }

    private int getRegionID() {
        return this.client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    private boolean isInChiselRegion() {
//        6972 - mine
//        6716 - altar
//        6715 - blood
        int regionID = getRegionID();
        return regionID == 6972 || regionID == 6716 || regionID == 6715;
    }

    private boolean isInAbyss() {
        int regionID = getRegionID();
        return regionID == 12107;
    }

    private int[] destination() {
        LocalPoint localDestination = client.getLocalDestinationLocation();
        if(localDestination == null) {
            return new int[] {-1, -1};
        }

        WorldPoint worldDestination = WorldPoint.fromLocal(client, localDestination);
        return new int[] {worldDestination.getX(), worldDestination.getY()};
    }

    private boolean destinationDarkAltar() {
        int[] dest = destination();
        return dest[0] == 1718 && dest[1] == 3882;
    }

    private boolean isInReturnRun() {
        int[] location = {client.getLocalPlayer().getWorldLocation().getX(), client.getLocalPlayer().getWorldLocation().getY()};
        return location[0] >= 1758 &&
               location[0] <= 1764 &&
               location[1] >= 3874 &&
               location[1] <= 3880;
    }

    private boolean swapBankItem(MenuEntry e) {
        String target = stripTargetAnchors(e);
        return RCpouch.contains(target) || Skillcapes.contains(target) || AmuletOfGlory.contains(target) || RingOfDueling.contains(target);
    }

    private boolean swapWorldItem(MenuEntry e) {
        String target;
        try {
            target = stripTargetAnchors(e);
        } catch (Exception exception) {
            return false;
        }
        return RCpouch.contains(target) || Capes.contains(target) || UnchargedGlory.contains(target) || isZammyMage(e);
    }

    private boolean swapEquipItems(MenuEntry e) {
        String target = stripTargetAnchors(e);
        return AmuletOfGlory.contains(target) || RingOfDueling.contains(target);
    }

    private boolean isBankOpen() {
        return client.getWidget(WidgetInfo.BANK_CONTAINER) != null;
    }

    public void swapMenus() {
        MenuEntry entry = client.getMenuEntries()[client.getMenuEntries().length - 1];
        if (isBankOpen()) {
            if(swapBankItem(entry)) {
                bankModeSwap();
            }
        } else if(swapWorldItem(entry)) {
            inventorySwap();
        } else if(swapEquipItems(entry)) {
            // swap dueling ring to duel arena tp
            if(getRegionID() == 9776 || getRegionID() == 12344 || getRegionID() == 13130)
                swapDuelingRingToDuelArenaTp();
            // swap dueling ring to castle wars tp
//            else if(getRegionID() == 10315)
//                swapDuelingRingToCastleWarsTp();
            else
                equipSwap();
        }
    }

    private String stripTargetAnchors(MenuEntry menuEntries) {
        Matcher m = Pattern.compile(">(.*?)<").matcher(menuEntries.getTarget());
        return m.find() ? m.group(1) : "";
    }

    private boolean isZammyMage(MenuEntry menuEntries) {
        return menuEntries.getTarget().contains("Mage of Zamorak");
    }

    private void bankModeSwap() {
        MenuEntry[] menuEntries = client.getMenuEntries();
        for (int i = menuEntries.length - 1; i >= 0; --i) {
            MenuEntry entry = menuEntries[i];
            String target = stripTargetAnchors(entry);

            if(
                (RCpouch.contains(target) && entry.getIdentifier() == 9 && (menuEntries.length == 9 || menuEntries.length == 10)) ||
                (RingOfDueling.contains(target) && entry.getOption().equals("Withdraw-1") && (menuEntries.length == 9 || menuEntries.length == 10) && !isRingOfDuelingEquipped()) ||
                (RingOfDueling.contains(target) && entry.getOption().equals("Wear") && (menuEntries.length == 9 || menuEntries.length == 10) && !isRingOfDuelingEquipped()) ||

                (AmuletOfGlory.contains(target) && entry.getOption().equals("Withdraw-1") && (menuEntries.length == 9 || menuEntries.length == 10) && isUnchargedGloryEquipped()) ||
                (AmuletOfGlory.contains(target) && entry.getOption().equals("Wear") && (menuEntries.length == 9 || menuEntries.length == 10) && isUnchargedGloryEquipped()) ||

                (Skillcapes.contains(target) && entry.getOption().equals("Wear") && (menuEntries.length == 9 || menuEntries.length == 10))
            ) {
                entry.setType(MenuAction.CC_OP);

                menuEntries[i] = menuEntries[menuEntries.length - 1];
                menuEntries[menuEntries.length - 1] = entry;

                client.setMenuEntries(menuEntries);
                break;
            }

//            if((swapBankItem(entry) && entry.getType() == MenuAction.CC_OP_LOW_PRIORITY && entry.getIdentifier() == 9 && menuEntries.length == 10) &&
//                (RCpouch.contains(target) ||
//                Skillcapes.contains(target) ||
//                (AmuletOfGlory.contains(target) && isUnchargedGloryEquipped()) ||
//                (RingOfDueling.contains(target) && !isRingOfDuelingEquipped()))) {
//                    entry.setType(MenuAction.CC_OP);
//
//                    menuEntries[i] = menuEntries[menuEntries.length - 1];
//                    menuEntries[menuEntries.length - 1] = entry;
//
//                    client.setMenuEntries(menuEntries);
//                    break;
//            }
        }
    }

    private void inventorySwap() {
        MenuEntry[] menuEntries = client.getMenuEntries();
        for (int i = menuEntries.length - 1; i >= 0; --i) {
            MenuEntry entry = menuEntries[i];
            String target = stripTargetAnchors(entry);
            MenuAction targetType = entry.getType();
            if (
                (RCpouch.contains(target) && entry.getIdentifier() == 2 && menuEntries.length == 7 && isAtRCAltar()) ||
                (Capes.contains(target) && entry.getIdentifier() == 4 && menuEntries.length == 6) ||
                (UnchargedGlory.contains(target) && entry.getIdentifier() == 6 && menuEntries.length == 6) ||
                (isZammyMage(entry) && targetType == MenuAction.NPC_FOURTH_OPTION && menuEntries.length == 6)
            )
            {
                menuEntries[i] = menuEntries[menuEntries.length - 1];
                menuEntries[menuEntries.length - 1] = entry;

                client.setMenuEntries(menuEntries);
                break;
            }
        }
    }

    private void equipSwap() {
        MenuEntry[] menuEntries = client.getMenuEntries();

        for (int i = menuEntries.length - 1; i >= 0; --i) {
            MenuEntry entry = menuEntries[i];
            String target = stripTargetAnchors(entry);

            if((AmuletOfGlory.contains(target) && entry.getIdentifier() == 2 && menuEntries.length == 7) ||
               (RingOfDueling.contains(target) && entry.getIdentifier() == 4 && menuEntries.length == 6) ) {
                menuEntries[i] = menuEntries[menuEntries.length - 1];
                menuEntries[menuEntries.length - 1] = entry;

                client.setMenuEntries(menuEntries);
                break;
            }
        }
    }

    private void swapDuelingRingToDuelArenaTp() {
        MenuEntry[] menuEntries = client.getMenuEntries();

        for (int i = menuEntries.length - 1; i >= 0; --i) {
            MenuEntry entry = menuEntries[i];
            String target = stripTargetAnchors(entry);

            if(RingOfDueling.contains(target) && entry.getIdentifier() == 2 && menuEntries.length == 6) {
                menuEntries[i] = menuEntries[menuEntries.length - 1];
                menuEntries[menuEntries.length - 1] = entry;

                client.setMenuEntries(menuEntries);
                break;
            }
        }
    }

    private void swapDuelingRingToCastleWarsTp() {
        MenuEntry[] menuEntries = client.getMenuEntries();

        for (int i = menuEntries.length - 1; i >= 0; --i) {
            MenuEntry entry = menuEntries[i];
            String target = stripTargetAnchors(entry);

            if(RingOfDueling.contains(target) && entry.getIdentifier() == 3 && menuEntries.length == 6) {
                menuEntries[i] = menuEntries[menuEntries.length - 1];
                menuEntries[menuEntries.length - 1] = entry;

                client.setMenuEntries(menuEntries);
                break;
            }
        }
    }

    private boolean isUnchargedGloryEquipped() {
        return client.getItemContainer(InventoryID.EQUIPMENT).contains(ItemID.AMULET_OF_GLORY);
    }

    private boolean isRingOfDuelingEquipped() {
        List<Item> equipItems = new ArrayList<>(Arrays.asList(client.getItemContainer(InventoryID.EQUIPMENT).getItems()));
        Set<Integer> rod = new HashSet<>(Arrays.asList(2552, 2554, 2556, 2558, 2560, 2562, 2564, 2566));

        for(Item i : equipItems) {
            if(rod.contains(i.getId()))
                return true;
        }

        return false;
    }

    private boolean inventoryContains(int id) {
        return client.getItemContainer(InventoryID.INVENTORY).contains(id);
    }

    private void renderRifts(Graphics2D graphics) {
        int riftID = config.showAbyssClickBox().getId();
        if(riftID == 1) {
            renderObject(graphics, chaosRift, Color.GREEN, Color.GREEN, Color.GREEN);
        } else if(riftID == 2) {
            renderObject(graphics, cosmicRift, Color.GREEN, Color.GREEN, Color.GREEN);
        } else if(riftID == 3) {
            renderObject(graphics, deathRift, Color.GREEN, Color.GREEN, Color.GREEN);
        } else if(riftID == 4) {
            renderObject(graphics, natureRift, Color.GREEN, Color.GREEN, Color.GREEN);
        } else if(riftID == 5) {
            renderObject(graphics, lawRift, Color.GREEN, Color.GREEN, Color.GREEN);
        } else if(riftID == 7) {
            renderObject(graphics, bloodRift, Color.GREEN, Color.GREEN, Color.GREEN);
        }
    }

    private void renderWrath(Graphics2D graphics) {
        if(isNearWorldTile(FrontStatue, 5)) {
            changeCameraYaw(cameraSouth);
            renderObject(graphics, statue, Pink_Color, Pink_Color, Pink_Color);
        } else if(isNearWorldTile(EnterStatue, 5)) {
            changeCameraYaw(cameraSouth);
            if(inventoryContains(ItemID.AMULET_OF_GLORY))
                renderObject(graphics, fountainGlory, Pink_Color, Pink_Color, Pink_Color);
            else
                renderObject(graphics, caveEntrance, Pink_Color, Pink_Color, Pink_Color);
        } else if(isNearWorldTile(WrathAltarEntrance, 10)) {
            changeCameraYaw(cameraSouth);
            renderObject(graphics, wrathAltarEntrance, Pink_Color, Pink_Color, Pink_Color);
        } else if(isNearWorldTile(WrathAltar, 10)) {
            changeCameraYaw(cameraSouth);
            renderObject(graphics, wrathAltar, Pink_Color, Pink_Color, Pink_Color);
        }
    }

    private void renderWildernessWall(Graphics2D graphics) {
        for(WorldPoint wp : wildernessWall) {
            renderTileArea(graphics, LocalPoint.fromWorld(client, wp), 1);
        }
    }

    private void pressKey(int key, int ms) {
        robot.keyPress(key);
        robot.delay(ms);
        robot.keyRelease(key);
    }

    private boolean isAtRCAltar() {
        int regionID = getRegionID();
        return RCAltarRegionIDs.contains(regionID);
    }
}
