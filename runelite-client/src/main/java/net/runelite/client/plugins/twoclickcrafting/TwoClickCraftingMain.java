package net.runelite.client.plugins.twoclickcrafting;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Item;
import net.runelite.api.ItemID;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.agilityPlusV2.MouseCoordCalculation;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TwoClickCraftingMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    public static boolean isIdle = true;
    public long start;

    public static boolean isRunning = false;

    public static List<Item> inventoryItems = new ArrayList<>();

    private static final Set<Integer> dragonHideBodies = ImmutableSet.of(
            ItemID.GREEN_DHIDE_BODY,
            ItemID.BLUE_DHIDE_BODY,
            ItemID.RED_DHIDE_BODY,
            ItemID.BLACK_DHIDE_BODY
    );

    private static final Set<Integer> dragonLeather = ImmutableSet.of(
            ItemID.GREEN_DRAGON_LEATHER,
            ItemID.BLUE_DRAGON_LEATHER,
            ItemID.RED_DRAGON_LEATHER,
            ItemID.BLACK_DRAGON_LEATHER
    );

    private final Set<Integer> grimyHerbs = ImmutableSet.of(
            ItemID.GRIMY_GUAM_LEAF,
            ItemID.GRIMY_MARRENTILL,
            ItemID.GRIMY_TARROMIN,
            ItemID.GRIMY_HARRALANDER,
            ItemID.GRIMY_RANARR_WEED,
            ItemID.GRIMY_IRIT_LEAF,
            ItemID.GRIMY_AVANTOE,
            ItemID.GRIMY_KWUARM,
            ItemID.GRIMY_CADANTINE,
            ItemID.GRIMY_DWARF_WEED,
            ItemID.GRIMY_TORSTOL,
            ItemID.GRIMY_LANTADYME,
            ItemID.GRIMY_TOADFLAX,
            ItemID.GRIMY_SNAPDRAGON
    );

    private final Set<Integer> cleanHerbs = ImmutableSet.of(
            ItemID.GUAM_LEAF,
            ItemID.MARRENTILL,
            ItemID.TARROMIN,
            ItemID.HARRALANDER,
            ItemID.RANARR_WEED,
            ItemID.IRIT_LEAF,
            ItemID.AVANTOE,
            ItemID.KWUARM,
            ItemID.CADANTINE,
            ItemID.DWARF_WEED,
            ItemID.TORSTOL,
            ItemID.LANTADYME,
            ItemID.TOADFLAX,
            ItemID.SNAPDRAGON
    );

    private final Set<Integer> unfPots = ImmutableSet.of(
        ItemID.GUAM_POTION_UNF,
        ItemID.MARRENTILL_POTION_UNF,
        ItemID.TARROMIN_POTION_UNF,
        ItemID.HARRALANDER_POTION_UNF,
        ItemID.RANARR_POTION_UNF,
        ItemID.IRIT_POTION_UNF,
        ItemID.AVANTOE_POTION_UNF,
        ItemID.KWUARM_POTION_UNF,
        ItemID.CADANTINE_POTION_UNF,
        ItemID.DWARF_WEED_POTION_UNF,
        ItemID.TORSTOL_POTION_UNF,
        ItemID.LANTADYME_POTION_UNF,
        ItemID.TOADFLAX_POTION_UNF,
        ItemID.SNAPDRAGON_POTION_UNF
    );

    private final List<Point> inventoryCoords = new ArrayList(ImmutableList.of(
            new Point(782, 768), // 1 row 1 col
            new Point(782, 804), // 2 row 1 col
            new Point(782, 840), // 3 row 1 col
            new Point(782, 876), // 4 row 1 col
            new Point(782, 912), // 5 row 1 col
            new Point(782, 948), // 6 row 1 col
            new Point(782, 984), // 7 row 1 col

            new Point(824, 984), // 7 row 2 col
            new Point(824, 948), // 6 row 2 col
            new Point(824, 912), // 5 row 2 col
            new Point(824, 876), // 4 row 2 col
            new Point(824, 840), // 3 row 2 col
            new Point(824, 804), // 2 row 2 col
            new Point(824, 768), // 1 row 2 col

            new Point(866, 768), // 1 row 3 col
            new Point(866, 804), // 2 row 3 col
            new Point(866, 840), // 3 row 3 col
            new Point(866, 876), // 4 row 3 col
            new Point(866, 912), // 5 row 3 col
            new Point(866, 948), // 6 row 3 col
            new Point(866, 984), // 7 row 3 col
            new Point(908, 984), // 7 row 4 col

            new Point(908, 948), // 6 row 4 col
            new Point(908, 912), // 5 row 4 col
            new Point(908, 876), // 4 row 4 col
            new Point(908, 840), // 3 row 4 col
            new Point(908, 804), // 2 row 4 col
            new Point(908, 768)  // 1 row 4 col
    ));

    Thread t;

    TwoClickCraftingMain(Client client, ClientThread clientThread) {
        this.client = client;
        this.clientThread = clientThread;

        t = new Thread(this);
        System.out.println("New thread: " + t);
        start = System.currentTimeMillis();
        t.start(); // Starting the thread
    }

    public void run()
    {
        while (isRunning) {
            if (checkIdle() && checkLastReset())
                reset();

            if (isAtWorldPoint(new WorldPoint(3094, 3489, 0))
                || isAtWorldPoint(new WorldPoint(3185, 3444, 0)))
                doPotionMaking();
        }
        System.out.println("Thread has stopped.");
    }

    private void doPotionMaking() {
        if(!isBankOpen() && (
                getInventoryCount() == 0 ||
                checkInventoryCount(unfPots, 14) ||
                (checkInventoryCount(ImmutableSet.of(ItemID.VIAL_OF_WATER), 14) && getInventoryCount() == 14 ) ||
                (checkInventoryCount(cleanHerbs, 2) && !checkInventoryCount(ImmutableSet.of(ItemID.VIAL_OF_WATER), 1))
        )) {
            client.setCameraPitchTarget(512);
            changeCameraYaw(512);
            setCameraZoom(977);
            delay(500);
            scheduledPointDelay(new Point(482, 730), 8);
            delay(1000);
        } else if(isBankOpen() && (getInventoryCount() == 0 || getInventoryCount() == 14 || checkInventoryCount(cleanHerbs, 2))) {
            delay(300);
            herbBankingSequence();
        } else if(!isBankOpen() && checkInventoryCount(ImmutableSet.of(ItemID.VIAL_OF_WATER), 14)) {
            delay(300);
            scheduledPointDelay(new Point(824, 876), 3); // row 4 col 2
            delay(300);
            scheduledPointDelay(new Point(866, 876), 3); // row 4 col 3
            delay(2000);
        } else if(!isBankOpen() && checkInventoryCount(grimyHerbs, 2)) {
            herbCleaningSequence();
        }
    }

    private void doCrafting() {
        if(!isBankOpen() && checkInventoryCount(dragonHideBodies, 8) && isIdle) {
            client.setCameraPitchTarget(512);
            changeCameraYaw(0);
            setCameraZoom(977);
            delay(500);
            scheduledPointDelay(new Point(691, 539), 10);
        } else if(isBankOpen() && checkInventoryCount(dragonHideBodies, 8) && isIdle) {
            delay(300);
            bankingSequence();
        } else if(!isBankOpen() && checkInventoryCount(dragonLeather, 26) && isIdle) {
            delay(300);
            scheduledPointDelay(new Point(782, 768), 3); // row 1 col 1
            delay(300);
            scheduledPointDelay(new Point(782, 804), 3); // row 2 col 1
            delay(300);
        }
    }

    private void herbCleaningSequence() {
        inventoryCoords.forEach(point -> {
            delay(50);
            scheduledPointDelay(point, 3);
        });
        delay(1000);
        isIdle = true;
    }

    private int getInventoryCount() {
        return (int) inventoryItems.stream().filter(item -> item.getId() != -1).count();
    }

    private boolean checkInventoryCount(Set<Integer> itemSet, int count) {
        try {
            return inventoryItems.stream()
                    .filter(item -> itemSet.contains(item.getId()))
                    .count() >= count;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isAtWorldPoint(WorldPoint worldPoint) {
        boolean playerX = client.getLocalPlayer().getWorldLocation().getX() == worldPoint.getX();
        boolean playerY = client.getLocalPlayer().getWorldLocation().getY() == worldPoint.getY();
        return playerX && playerY;
    }

    private void herbBankingSequence() {
        // deposit button
        scheduledPointDelay(new Point(539, 824), 3);
        delay(300);

        // no grimy herbs to clean
        if (isEmptyBankSlot(149) || getBankSlotQuantity(149) < 1) {
            // withdraw X
            scheduledPointDelay(new Point(405, 830), 2);
            delay(300);

//            if (getBankSlotQuantity(150) < 14) {
//                System.out.println("no vials");
//                TwoClickCraftingPlugin.thread.t.interrupt();
//                TwoClickCraftingMain.isRunning = false;
//                delay(300);
//            } else {
                // withdraw lower right - 1
                scheduledPointDelay(new Point(473, 793), 3);
                delay(300);
//            }

//            if (getBankSlotQuantity(151) < 14) {
//                System.out.println("no herb");
//                TwoClickCraftingPlugin.thread.t.interrupt();
//                TwoClickCraftingMain.isRunning = false;
//                delay(1000);
//            } else {
                // withdraw lower right
                scheduledPointDelay(new Point(524, 793), 3);
                delay(1000);
//            }
        } else {
            // withdraw All
            scheduledPointDelay(new Point(432, 830), 2);
            delay(300);

            // withdraw lower right - 2
            scheduledPointDelay(new Point(422, 793), 3);
            delay(300);

            pressKey(KeyEvent.VK_ESCAPE);
            delay(1000);
        }
        isIdle = true;
    }

    private void bankingSequence() {
        // deposit leather (click slot 1)
        // scheduledPointDelay(new Point(782, 768), 3);
        // 524, 793 bottom right bank

        // deposit slot 1
        scheduledPointDelay(new Point(782, 768), 3);
        delay(300);

        // withdraw lower right - 1
        scheduledPointDelay(new Point(473, 793), 3);
        delay(300);

        // withdraw lower right
        scheduledPointDelay(new Point(524, 793), 3);
        delay(1000);
        isIdle = true;
    }

    private boolean isEmptyBankSlot(int slot) {
        Widget bankWidget = client.getWidget(786445);
        return bankWidget != null &&
                bankWidget.getChild(slot).getOpacity() == 120;
    }

    private int getBankSlotQuantity(int slot) {
        Widget bankWidget = client.getWidget(786445);
        return Optional.ofNullable(bankWidget).map(widget -> widget.getChild(slot)).map(Widget::getItemQuantity).orElse(0);
    }

    private void pressKey(int key) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyRelease);
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

    private void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
            e.getStackTrace(); }
    }

    private void scheduledPointDelay(Point point, int sigma) {
        isIdle = false;
        try {
            MouseCoordCalculation.generateCoord(client, point, sigma);
        } catch (Exception e) {
            e.printStackTrace();
            isIdle = true;
        }
    }

    private boolean isBankOpen() {
        return client.getWidget(WidgetInfo.BANK_CONTAINER) != null;
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
        isIdle = true;
        delay(200);
        start = System.currentTimeMillis();
    }

    private boolean checkLastReset() {
        long end = System.currentTimeMillis();
        double sec = (end - start) / 1000F;

        return sec >= 5;
    }
}
