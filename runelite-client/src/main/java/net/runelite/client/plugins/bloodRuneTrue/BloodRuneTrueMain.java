package net.runelite.client.plugins.bloodRuneTrue;

import com.google.common.collect.ImmutableList;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.plusUtils.Clicker;
import net.runelite.client.plugins.plusUtils.StepOverlay;

import java.awt.Point;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BloodRuneTrueMain implements Runnable {
    private final Client client;
    private final ClientThread clientThread;
    private final StepOverlay overlay;

    public static boolean needRechargeStamina = false;
    public static boolean isIdle = true;
    public static boolean xpDrop = false;
    public static boolean isEquipmentOpen = false;
    public static int essenceRemaining = 0;
    private static int maxEssenceAvailible = 40;
    private boolean awaitingMovement = false;
    public static boolean isTeleportingCW = false;
    public static boolean isTeleportingPOH = false;
    public static boolean isNpcContact = false;
    public static boolean isLunarBookOpen = false;
    public static boolean needRepairPouch = true;
    public static boolean needRingOfDueling = false;
    public static boolean needBloodEssence = false;
    public static TileObject pendingClickboxObject = null;
    public static Shape pendingClickboxShape = null;
    public static final List<Item> currentInventory = new ArrayList<>();
    public static final List<Item> currentBank = new ArrayList<>();
    public static final List<Item> currentEquipment = new ArrayList<>();
    public static final List<Integer> ringOfDuelingIds = new ArrayList<>(Arrays.asList(
        ItemID.RING_OF_DUELING1,
        ItemID.RING_OF_DUELING2,
        ItemID.RING_OF_DUELING3,
        ItemID.RING_OF_DUELING4,
        ItemID.RING_OF_DUELING5,
        ItemID.RING_OF_DUELING6,
        ItemID.RING_OF_DUELING7,
        ItemID.RING_OF_DUELING8
    ));

    public final Point invSlot1 = new Point(772, 763);
    public final Point invSlot2 = new Point(816, 763);
    public final Point invSlot3 = new Point(859, 763);

    private static final List<Point> inventoryCoords = new ArrayList(ImmutableList.of(
            new Point(782, 768), // 1 row 1 col
            new Point(824, 768), // 1 row 2 col
            new Point(866, 768), // 1 row 3 col
            new Point(908, 768), // 1 row 4 col
            new Point(782, 804), // 2 row 1 col
            new Point(824, 804), // 2 row 2 col
            new Point(866, 804), // 2 row 3 col
            new Point(908, 804), // 2 row 4 col
            new Point(782, 840), // 3 row 1 col
            new Point(824, 840), // 3 row 2 col
            new Point(866, 840), // 3 row 3 col
            new Point(908, 840), // 3 row 4 col
            new Point(782, 876), // 4 row 1 col
            new Point(824, 876), // 4 row 2 col
            new Point(866, 876), // 4 row 3 col
            new Point(908, 876), // 4 row 4 col
            new Point(782, 912), // 5 row 1 col
            new Point(824, 912), // 5 row 2 col
            new Point(866, 912), // 5 row 3 col
            new Point(908, 912), // 5 row 4 col
            new Point(782, 948), // 6 row 1 col
            new Point(824, 948), // 6 row 2 col
            new Point(866, 948), // 6 row 3 col
            new Point(908, 948), // 6 row 4 col
            new Point(782, 984), // 7 row 1 col
            new Point(824, 984), // 7 row 2 col
            new Point(866, 984), // 7 row 3 col
            new Point(908, 984)  // 7 row 4 col
    ));

//    public final Point invSlot5 = new Point(775, 799);
    public final Point invSlot28 = new Point(900, 977);
    public final Point equipmentRingSlot = new Point(895, 920); // equipment ring slot
    public final Point depositAllSlot = new Point(540, 823); // bank deposit all
    public final Point bankPureEssence = new Point(523, 788); // bank dueling ring(8)
    public final Point bankDuelingRing8 = new Point(523, 750); // bank dueling ring(8)
    public final Point bankBloodEssence = new Point(523, 715); // bank blood essence

    private Runnable pendingAction = null;
    private WorldPoint lastLocation = null;
    private long lastMovementTime = 0;
    public long start;
    private final int STAMINA_THRESHOLD = 20;
    public static boolean isRunning = false;

    Clicker clicker;
    BreakScheduler scheduler;

    public static final Map<TileObject, Shape> clickboxCache = new HashMap<>();

    Thread t;

    BloodRuneTrueMain(Client client, ClientThread clientThread, StepOverlay overlay) {
        this.client = client;
        this.clientThread = clientThread;
        this.overlay = overlay;
        clicker = new Clicker(client);
        scheduler = new BreakScheduler();

        t = new Thread(this);
        System.out.println("New thread: " + t);
        t.start(); // Starting the thread
    }

    public void stop() {
        isRunning = false;
        reset();
        t.interrupt(); // in case it's sleeping or waiting
    }

    public void reset() {
        isIdle = true;
    }

    // execution of thread starts from run() method
    public void run()
    {
        while (isRunning) {
//            if (checkIdle(100)) {
//                isIdle = true;
//            }

            // Update idle based on movement
            updateIdleStatus();

            // Retry pending actions if necessary
            checkActionSuccess();

            if (!isMoving()) {
                if (isInsidePOH()) {
                    System.out.println("in POH");
                    isTeleportingPOH = false;

                    clicker.randomDelayStDev(150, 250, 25);

                    if (needRechargeStamina) {
                        tryAction(this::rechargeStamina);
                    }

                    else if (isAtWorldPoint(BloodRuneTrueWorldPoints.INFRONT_OF_POOL)) {
                        setZoomPitchYaw(640, 512, 0);
                        tryAction(this::clickFairyRingPOH);
                    }

                    else {
                        setZoomPitchYaw(408, 512, 0);
                        tryAction(this::clickFairyRingPOH);
                    }
                }
                else if(isAtWorldPoint(BloodRuneTrueWorldPoints.DLS_FAIRY_RING) && getRegionID() == 13721 && isIdle) {
                    tryAction(this::clickFairyRingCaveEntrance);
                }
                // fairy ring => cave 1
                else if(isAtWorldPoint(BloodRuneTrueWorldPoints.FAIRY_RING_CAVE_EXIT) && getRegionID() == 13977 && isIdle) {
                        tryAction(this::clickCaveEntrance1);
                }

                // cave 1 => shortcut cave
                else if(isAtWorldPoint(BloodRuneTrueWorldPoints.SECOND_CAVE_ENTRANCE_EXIT) && getRegionID() == 13977 && isIdle) {
                        tryAction(this::clickShortcutCave);
                }

                // standing in front of shortcut cave (IFFY)
//                else if(isAtWorldPoint(BloodRuneTrueWorldPoints.CAVE_SHORTCUT_STAND_TILE) && getRegionID() == 13977 && isIdle) {
//                        tryAction(this::clickShortcutCaveInfront);
//                }

                // shortcut cave => cave to mysterious ruins
                else if(isAtWorldPoint(BloodRuneTrueWorldPoints.CAVE_SHORTCUT_EXIT) && getRegionID() == 14232 && isIdle) {
                        tryAction(this::clickCaveToMysteriousRuins);
                }

                // standing in front of cave to mysterious ruins (IFFY)
//                else if(isAtWorldPoint(BloodRuneTrueWorldPoints.CAVE_TO_MYSTERIOUS_RUINS_STAND_TILE) && getRegionID() == 14232 && isIdle) {
//                    tryAction(this::clickCaveToMysteriousRuinsInfront);
//                }

                // cave to mysterious ruins => blood mysterious ruins
                else if(isAtWorldPoint(BloodRuneTrueWorldPoints.CAVE_TO_MYSTERIOUS_RUINS_EXIT) && getRegionID() == 14232 && isIdle) {
                    tryAction(this::clickMysteriousRuins);
                }

                // TRUE BLOOD ALTAR
                else if(isAtWorldPoint(BloodRuneTrueWorldPoints.TRUE_MYSTERIOUS_RUINS_SPAWN_IN_TILE) && getRegionID() == 12875 && isIdle) {
                    clickToBindInitialRunes();
                }

                // NEXT TO BLOOD ALTAR
                else if (isAtWorldPoint(BloodRuneTrueWorldPoints.INFRONT_OF_BLOOD_ALTAR) && getRegionID() == 12875 && (essenceRemaining > 0 || hasItem(currentInventory, ItemID.PURE_ESSENCE))) {
                    clickToBindPouchRunes();
                }

                // BLOOD ALTAR DONE TP OUT
                else if (isAtWorldPoint(BloodRuneTrueWorldPoints.INFRONT_OF_BLOOD_ALTAR) && getRegionID() == 12875 && essenceRemaining == 0 && !isTeleportingCW && isIdle) {
                    tryAction(this::clickDuelRing);
                }
//
//                // CASTLE WARS
                else if(getRegionID() == 9776) {
                    System.out.println("at castle wars");
                    isTeleportingCW = false;

                    if (isStaminaBelow(STAMINA_THRESHOLD)) {
                        System.out.println("need recharge");
                        needRechargeStamina = true;
                    }

                    if (isWorldPointInArea(myWorldPoint(), BloodRuneTrueWorldPoints.CASTLE_WARS_TP_ZONE) && isIdle) {
                        clicker.pressKey(KeyEvent.VK_ESCAPE);
                        clicker.randomDelayStDev(150, 250, 25);
                        tryAction(this::clickBank);
                    } else if (isAtWorldPoint(BloodRuneTrueWorldPoints.CASTLE_WARS_BANK_CHEST_INFRONT)) {
                        // if bank is open
                        clicker.randomDelayStDev(150, 250, 25);
                        if (isBankOpen()) {
                            isDuelingRingEquipped();
                            isBloodEssenceActive();

                            if (needRepairPouch) {
                                System.out.println("post fill pouch repair needed");
                                clicker.pressKey(KeyEvent.VK_ESCAPE);
                                clicker.randomDelayStDev(150, 250, 25);
                            }

                            else if (!isReadyForAltar()) {
                                if (hasItem(currentInventory, ItemID.BLOOD_RUNE)) {
                                    clickDepositBloodRunes();
                                }

                                if (getItemCount(currentInventory, ItemID.RING_OF_DUELING8) > 1 && !needRingOfDueling) {
                                    System.out.println("FIND WHICH SLOT RING IS IN");
                                    getSlotOfItem(currentInventory, ItemID.RING_OF_DUELING8)
                                        .forEach(i -> {
                                            clicker.clickPoint(inventoryCoords.get(i));
                                            clicker.randomDelayStDev(150, 250, 25);
                                        });
                                }

                                // and has blood ess in bank
                                if (needBloodEssence && hasItem(currentBank, ItemID.BLOOD_ESSENCE)) {
                                    // withdraw blood essence
//                                    while (!hasItem(currentInventory, ItemID.BLOOD_ESSENCE)) {
                                        withdrawBloodEssence();
//                                    }
                                }

                                if (needRingOfDueling) {
                                    System.out.println("need dueling ring");
                                    // withdraw dueling ring
                                    if (!hasItem(currentInventory, ItemID.RING_OF_DUELING8)) {
                                        withdrawDuelingRing();
                                    }
                                    if (hasItem(currentInventory, ItemID.RING_OF_DUELING8)) {
                                        equipDuelingRing();
                                    }
                                }

                                if (essenceRemaining != maxEssenceAvailible || !hasEnoughPureEssence()) {
//                                if (essenceRemaining != 40) {
                                    withdrawPureEssence();
                                }
//                                else if (getItemCount(currentInventory, ItemID.PURE_ESSENCE) < 25) {
//                                    withdrawPureEssence();
//                                }
                            }
                            else {
                                while(isBankOpen()) {
                                    System.out.println("trying to close bank");
                                    clicker.pressKey(KeyEvent.VK_ESCAPE);
                                    clicker.randomDelayStDev(150, 250, 25);
                                }
                            }
                        }
                        else if (!isBankOpen()) {
                            System.out.println("bank not open");
                            if (hasItem(currentInventory, ItemID.BLOOD_ESSENCE)) {
                                activateBloodEssence();
                            }

                            if (needRepairPouch) {
                                if (isNPCContactWindowOpen()) {
                                    overlay.setCurrentStep("npc contact window open");
                                    clicker.randomDelayStDev(150, 250, 25);
                                    clicker.clickPoint(new Point(346, 375));
                                    clicker.randomDelayStDev(150, 250, 25);
                                } else if (isChattingDarkMage()) {
                                    overlay.setCurrentStep("chatting with dark mage");
                                    darkMageChat();
                                }
                                else if (isNpcContact) {
                                    overlay.setCurrentStep("delay 300ms");
                                    clicker.delay(300);
                                }
                                else if (isLunarBookOpen) {
                                    overlay.setCurrentStep("lunar book open");
                                    clicker.clickPoint(new Point(769, 858));
                                    clicker.randomDelayStDev(150, 250, 25);
                                    clicker.pressKey(KeyEvent.VK_ESCAPE);
                                    clicker.randomDelayStDev(150, 250, 25);
                                } else {
                                    overlay.setCurrentStep("need to open mage book");
                                    clicker.pressKey(KeyEvent.VK_F6);
                                }
                            }

                            else if (!isReadyForAltar() && isIdle && !hasItem(currentInventory, ItemID.BLOOD_RUNE)) {
                                System.out.println("click bank infront");
//                                clicker.randomDelayStDev(250,350,25);
                                setZoomPitchYaw(896, 512, 0);
                                clickBank();
                                clicker.randomDelayStDev(500,650,25);
                            }

                            if (isReadyForAltar() && !isTeleportingPOH) {
                                tryAction(this::clickTeletab);
                            }

                            if (isLunarBookOpen && !needRepairPouch) {
                                clicker.pressKey(KeyEvent.VK_ESCAPE);
                            }
                        }
                    }
                }
//
                // POH TRANSITION SCREEN
                else if(getRegionID() == 11826) {
                    System.out.println("AT HOUSE");
                }
            }

        }
        System.out.println("Thread has stopped.");
    }

    public int getRegionID() {
        return client.getLocalPlayer().getWorldLocation().getRegionID();
    }

    public boolean isInsidePOH()
    {
        return client.isInInstancedRegion();
    }

    private void rechargeStamina() {
        overlay.setCurrentStep("click restorationPoolPOH");
        System.out.println("click restorationPoolPOH");
        setZoomPitchYaw(595, 512, 0);
        clickPointObject(BloodRuneTrueObjectIDs.restorationPoolPOH, false);
        clicker.delay(500);
    }

    private void clickFairyRingPOH() {
        overlay.setCurrentStep("click fairyRingPOH");
        System.out.println("click fairyRingPOH");
        clicker.randomDelayStDev(150, 250, 25);
        clickPointObject(BloodRuneTrueObjectIDs.fairyRingPOH, false);
        clicker.delay(2500);
    }

    private void clickFairyRingCaveEntrance() {
        overlay.setCurrentStep("click fairyRingCaveEntrance");
        System.out.println("click fairyRingCaveEntrance");
        setZoomPitchYaw(380, 85, 0);
        clickPointObject(BloodRuneTrueObjectIDs.fairyRingCaveEntrance, true);
        clicker.delay(1500);
    }

    private void clickCaveEntrance1() {
        overlay.setCurrentStep("click caveEntrance1");
        System.out.println("click caveEntrance1");
        setZoomPitchYaw(70, 240, 945);
        clicker.randomDelayStDev(200, 300, 25);
        clickPointObject(BloodRuneTrueObjectIDs.caveEntrance1, true);
        clicker.delay(1500);
    }

    private void clickShortcutCave() {
        overlay.setCurrentStep("click shortcutCave");
        System.out.println("click shortcutCave");
        setZoomPitchYaw(325, 165, 1110);
        clicker.randomDelayStDev(200, 300, 25);
        clickPointObject(BloodRuneTrueObjectIDs.caveShortcut, true);
        clicker.delay(1500);
    }

    private void clickShortcutCaveInfront() {
        overlay.setCurrentStep("click shortcutCaveInfront");
        System.out.println("click shortcutCaveInfront");
        setZoomPitchYaw(410, 0, 0);
        clickPointObject(BloodRuneTrueObjectIDs.caveShortcut, true);
        clicker.delay(1500);
    }

    private void clickCaveToMysteriousRuins() {
        overlay.setCurrentStep("click caveToMysteriousRuins");
        System.out.println("click caveToMysteriousRuins");
        setZoomPitchYaw(63, 55, 755);
//        clicker.randomDelayStDev(150, 250, 25);
        clickPointObject(BloodRuneTrueObjectIDs.caveToMysteriousRuins, true);
        clicker.delay(1500);
    }

    private void clickCaveToMysteriousRuinsInfront() {
        overlay.setCurrentStep("click caveToMysteriousRuinsInfront");
        System.out.println("click caveToMysteriousRuinsInfront");
        setZoomPitchYaw(520, 15, 1536);
        clicker.randomDelayStDev(150, 250, 25);
        clickPointObject(BloodRuneTrueObjectIDs.caveToMysteriousRuins, true);
        clicker.delay(1500);
    }

    private void clickMysteriousRuins() {
        overlay.setCurrentStep("click MysteriousRuins");
        System.out.println("click MysteriousRuins");
        setZoomPitchYaw(530, 110, 1720);
        clicker.randomDelayStDev(150, 250, 25);
        clickPointObject(BloodRuneTrueObjectIDs.bloodMysteriousRuins, false);
        // pre turn camera
        clicker.delay(100);
        setZoomPitchYaw(0, 155, 1536);
        clicker.delay(1500);
    }

    private void clickToBindInitialRunes() {
        overlay.setCurrentStep("bind runes1");
        System.out.println("bind runes1");
//        setZoomPitchYaw(0, 155, 1536);
        clicker.randomDelayStDev(150, 250, 25);
        clickPointObject(BloodRuneTrueObjectIDs.trueBloodAltar, false);
        // pre turn camera
        clicker.delay(100);
        setZoomPitchYaw(615, 200, 1588);
    }

    private void clickToBindPouchRunes() {
        overlay.setCurrentStep("bind runes2");
        System.out.println("bind runes2");
//        setZoomPitchYaw(615, 200, 1588);
        clicker.randomDelayStDev(250,350,25);
        while(essenceRemaining > 0) {
            clicker.clickPoint(invSlot1);
            clicker.randomDelayStDev(200,300,20);
            clickPointObject(BloodRuneTrueObjectIDs.trueBloodAltar, false);
            clicker.randomDelayStDev(200,300,20);
        }
//        clicker.delay(500);
    }

    private void clickDuelRing() {
        overlay.setCurrentStep("click duelRing");
        System.out.println("click duelRing");
        clicker.pressKey(KeyEvent.VK_F4);
        clicker.randomDelayStDev(150, 250, 25);
        if (isEquipmentOpen) {
            clicker.clickPoint(equipmentRingSlot);
            // pre turn camera
            clicker.delay(50);
            setZoomPitchYaw(350, 280, 1185);
        }
        clicker.delay(500);
    }

    private void clickBank() {
        overlay.setCurrentStep("click bank");
        System.out.println("click bank");
//        setZoomPitchYaw(520, 15, 1536);

        clickPointObject(BloodRuneTrueObjectIDs.castleWarsBankChest, false);
        clicker.delay(500);
    }

    private void clickDepositBloodRunes() {
        overlay.setCurrentStep("click deposit blood runes");
        System.out.println("click deposit blood runes");
        clicker.randomDelayStDev(250,350,25);
        if (hasItem(currentInventory, ItemID.BLOOD_RUNE)) {
            if (hasItem(currentInventory, ItemID.BLOOD_ESSENCE) || hasItem(currentInventory, ItemID.BLOOD_ESSENCE_ACTIVE)) {
                clicker.clickPoint(invSlot3);
            } else {
                clicker.clickPoint(invSlot2);
            }
        }
        clicker.randomDelayStDev(250,350,25);
    }

    private void withdrawBloodEssence() {
        overlay.setCurrentStep("withdraw blood essence");
        System.out.println("withdraw blood essence");
        clicker.clickPoint(bankBloodEssence);
        clicker.randomDelayStDev(350,450,25);
    }

    private void withdrawDuelingRing() {
        overlay.setCurrentStep("withdraw dueling ring");
        System.out.println("withdraw dueling ring");
        clicker.clickPoint(bankDuelingRing8);
        clicker.randomDelayStDev(350,450,25);
    }

    private void equipDuelingRing() {
        overlay.setCurrentStep("equip dueling ring");
        System.out.println("equip dueling ring");
        if (hasItem(currentInventory, ItemID.RING_OF_DUELING8)) {
            if (hasItem(currentInventory, ItemID.BLOOD_ESSENCE) || hasItem(currentInventory, ItemID.BLOOD_ESSENCE_ACTIVE)) {
                clicker.clickPoint(invSlot3);
            } else {
                clicker.clickPoint(invSlot2);
            }
        }
        clicker.randomDelayStDev(350,450,25);
    }

    private void withdrawPureEssence() {
        overlay.setCurrentStep("withdraw pure essence");
        System.out.println("withdraw pure essence");
        while(essenceRemaining != maxEssenceAvailible) {
            if (needRepairPouch) { return; }
            clicker.clickPoint(bankPureEssence);
            clicker.randomDelayStDev(350,450,25);
            clicker.clickPoint(invSlot1);
            clicker.randomDelayStDev(350,450,25);
        }

        clicker.clickPoint(bankPureEssence);
        clicker.randomDelayStDev(350,450,25);
    }

    private void activateBloodEssence() {
        overlay.setCurrentStep("activate blood essence");
        System.out.println("activate blood essence");
        clicker.clickPoint(invSlot2);
        clicker.randomDelayStDev(250,350,25);
    }

    private void clickTeletab() {
        overlay.setCurrentStep("click teletab");
        System.out.println("click teletab");
        clicker.clickPoint(invSlot28);
        clicker.randomDelayStDev(250,350,25);
        clicker.delay(500);
    }

    private void isDuelingRingEquipped() {
        needRingOfDueling = true;
        for (int id : ringOfDuelingIds)
        {
            if (hasItem(currentEquipment, id)) {
                needRingOfDueling = false;
                return;
            }
        }
    }

    private void isBloodEssenceActive()
    {
        boolean bankHas = hasItem(currentBank, ItemID.BLOOD_ESSENCE);
        boolean invHasActive = hasItem(currentInventory, ItemID.BLOOD_ESSENCE_ACTIVE);
        boolean invHasRegular = hasItem(currentInventory, ItemID.BLOOD_ESSENCE);

        // If inventory already has essence (regular or active) → don't need more
        if (invHasRegular || (invHasActive && !bankHas))
        {
            needBloodEssence = false;
        }
        else
        {
            // Otherwise, need it only if bank has it XOR inventory has active
            needBloodEssence = bankHas ^ invHasActive;
        }
    }

    private boolean hasEnoughPureEssence() {
        return
                (hasItem(currentInventory, ItemID.BLOOD_ESSENCE_ACTIVE) || hasItem(currentInventory, ItemID.BLOOD_ESSENCE)) && getItemCount(currentInventory, ItemID.PURE_ESSENCE) == 24 && hasItem(currentInventory, ItemID.RUNE_POUCH) ||
                        (!hasItem(currentInventory, ItemID.BLOOD_ESSENCE_ACTIVE) || !hasItem(currentInventory, ItemID.BLOOD_ESSENCE)) && getItemCount(currentInventory, ItemID.PURE_ESSENCE) == 25 && hasItem(currentInventory, ItemID.RUNE_POUCH) ||
                        (hasItem(currentInventory, ItemID.BLOOD_ESSENCE_ACTIVE) || hasItem(currentInventory, ItemID.BLOOD_ESSENCE)) && getItemCount(currentInventory, ItemID.PURE_ESSENCE) == 25 && (hasItem(currentEquipment, ItemID.RUNECRAFT_CAPE) || hasItem(currentEquipment, ItemID.RUNECRAFT_CAPET)) ||
                        (!hasItem(currentInventory, ItemID.BLOOD_ESSENCE_ACTIVE) || !hasItem(currentInventory, ItemID.BLOOD_ESSENCE)) && getItemCount(currentInventory, ItemID.PURE_ESSENCE) == 26 && (hasItem(currentEquipment, ItemID.RUNECRAFT_CAPE) || hasItem(currentEquipment, ItemID.RUNECRAFT_CAPET))
                ;
    }

    private boolean isReadyForAltar() {
//        boolean hasBloodEssence = hasItem(currentInventory, ItemID.BLOOD_ESSENCE_ACTIVE) || hasItem(currentInventory, ItemID.BLOOD_ESSENCE);
        boolean isGiantPouchFull = (essenceRemaining == maxEssenceAvailible);
        boolean hasPureEssence = hasEnoughPureEssence();
        boolean hasHouseTeletab = hasItem(currentInventory, ItemID.TELEPORT_TO_HOUSE);

//        return hasBloodEssence && isGiantPouchFull && hasPureEssence && hasHouseTeletab;
        return isGiantPouchFull && hasPureEssence && hasHouseTeletab;
    }

    private void setZoomPitchYaw(int zoom, int pitch, int yaw) {
        setCameraZoom(zoom);
        setCameraPitch(pitch);
        setCameraYaw(yaw);
        clicker.randomDelayStDev(150,300,25);
    }

    private WorldPoint myWorldPoint() {
        return new WorldPoint(
            client.getLocalPlayer().getWorldLocation().getX(),
            client.getLocalPlayer().getWorldLocation().getY(),
            client.getLocalPlayer().getWorldLocation().getPlane()
        );
    }

    private boolean isAtWorldPoint(WorldPoint worldPoint) {
        if (worldPoint == null) return false;
        boolean playerX = client.getLocalPlayer().getWorldLocation().getX() == worldPoint.getX();
        boolean playerY = client.getLocalPlayer().getWorldLocation().getY() == worldPoint.getY();
        boolean playerPlane = client.getLocalPlayer().getWorldLocation().getPlane() == worldPoint.getPlane();
        return playerX && playerY && playerPlane;
    }

    public static boolean isWorldPointInArea(WorldPoint wp, List<WorldPoint> area)
    {
        if (area == null || area.isEmpty())
            return false;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        int plane = area.get(0).getPlane();

        // compute bounds
        for (WorldPoint p : area)
        {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
        }

        return wp.getPlane() == plane
                && wp.getX() >= minX && wp.getX() <= maxX
                && wp.getY() >= minY && wp.getY() <= maxY;
    }

    private void setCameraYaw(int yaw) {
        if(client.getCameraYaw() == yaw)
            return;
        client.setCameraYawTarget(yaw);
    }

    private void setCameraPitch(int pitch) {
        if(client.getCameraPitch() == pitch)
            return;
        client.setCameraPitchTarget(pitch);
    }

    private void setCameraZoom(int zoom)
    {
        clientThread.invokeLater(() ->
                client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom)
        );
    }

    private void clickPointObject(TileObject o, boolean spam) {
        if (o == null) {
            return;
        }

        Shape s = clickboxCache.get(o);
        if (s == null) {
            pendingClickboxObject = o;
            System.out.println("shape pulled from plugin");

            while (pendingClickboxShape == null) {
                clicker.delay(100);
            }

            s = pendingClickboxShape;
        }

        Point p = clicker.getCentroidFromShape(client, s);
        if (p == null || p.x <= 0 || p.y <= 0) {
            return;
        }

        if (spam) {
            while (!isMoving()) {
                clicker.spamClickPoint(p, 2, 3, 2.4, 0.23);
            }
        } else {
            clicker.clickPoint(p);
        }

        clicker.randomDelayStDev(150, 250, 25);

        pendingClickboxShape = null;
    }

//    public boolean isMoving()
//    {
//        Player p = client.getLocalPlayer();
//        if (p == null)
//            return false;
//
//        // --- 2. Walking movement (destination exists) ---
//        if (client.getLocalDestinationLocation() != null)
//            return true;
//
//        // --- 3. Animation-based movement (agility obstacles, teleports, etc.) ---
//        int anim = p.getAnimation();
//
//        // Animation -1 = idle
//        // 3265 fairy ring tp
//        // 3266 fairy ring tp
//        // 2796 going through cave
//        // 791 special case → treat as NOT moving
//        // 808 = idle look-around animation
//        if (anim == 791)
//            return false;
//
//        else if (anim != -1 && anim != 808)
//            return true;
//
//        return false;
//    }

    public boolean isMoving()
    {
        Player p = client.getLocalPlayer();
        if (p == null)
            return false;

        WorldPoint current = p.getWorldLocation();

        // First time
        if (lastLocation == null) {
            lastLocation = current;
            return false;
        }

        // Detect tile movement
        if (!current.equals(lastLocation)) {
            lastLocation = current;
            lastMovementTime = System.currentTimeMillis();
            return true; // walking or transitioning between tiles
        }

        // Smooth movement without tile change (model movement)
        if (System.currentTimeMillis() - lastMovementTime < 500)
            return true;

        // Teleports or cave-entering animation (~2796)
        int anim = p.getAnimation();
        if (anim == 2796 || anim == 3265 || anim == 3266 || anim == 714 || anim == 4069 || anim == 4071 || anim == 7305 || anim == 4412 || anim == 4413) {
            if (anim == 714)
                isTeleportingCW = true;
            else if (anim == 4069 || anim == 4071)
                isTeleportingPOH = true;
            else if (anim == 7305)
                needRechargeStamina = false;
            else if (anim == 4412 || anim == 4413)
                isNpcContact = true;
            return true;
        }

        return false;
    }

    public void updateIdleStatus()
    {
        if (isMoving())
        {
            // Movement started → reset all event signals
            overlay.setCurrentStep("moving to next");
            isIdle = false;
            xpDrop = false;
//            invUpdate = false;
        }
        else
        {
            // Character standing still
            overlay.setCurrentStep("char idle");
            isIdle = true;
        }
    }

    private void tryAction(Runnable action)
    {
        if (!isIdle)
            return;

        pendingAction = action;
        isIdle = false;
        awaitingMovement = true;   // Expect movement or animation
        action.run();

        clicker.delay(200);
    }

    private void checkActionSuccess()
    {
        if (pendingAction == null)
            return;

        // 1. SUCCESS: movement or animation detected
        if (isMoving())
        {
            System.out.println("moving detected");
            pendingAction = null;
            awaitingMovement = false;
            return;
        }

        // 2. FAILURE: no movement AFTER we expected it
        if (awaitingMovement && isIdle && !isMoving() && essenceRemaining!=0)
        {
            System.out.println("retry");
            overlay.setCurrentStep("retry");

            awaitingMovement = false;
            Runnable retry = pendingAction;
            pendingAction = null;

            tryAction(retry);
        }
    }

    private boolean isStaminaBelow(int threshold) {
        // Widget IDs may vary per client; confirm these are correct
        Widget staminaWidget = client.getWidget(10485788);
        if (staminaWidget == null) {
            return false; // widget not loaded
        }

        try {
            int stamina = Integer.parseInt(staminaWidget.getText());
            return stamina < threshold;
        } catch (NumberFormatException e) {
            return false; // in case the text is not a number
        }
    }

    public boolean hasItem(List<Item> items, int itemId)
    {
        List<Item> snapshot = new ArrayList<>(items);

        for (Item i : snapshot)
        {
            if (i != null && i.getId() == itemId)
                return true;
        }
        return false;
    }

    public int getItemCount(List<Item> items, int itemId)
    {
        int count = 0;
        List<Item> snapshot = new ArrayList<>(items);

        for (Item i : snapshot)
        {
            if (i != null && i.getId() == itemId)
                count += i.getQuantity();
        }

        return count;
    }

    public List<Integer> getSlotOfItem(List<Item> items, int itemId) {
        return IntStream.range(0, items.size())
                .filter(i -> items.get(i).getId() == itemId)
                .boxed()
                .collect(Collectors.toList());
    }

    private boolean isBankOpen() {
        return client.getWidget(WidgetInfo.BANK_EQUIPMENT_BUTTON) != null;
//        Widget bank = client.getWidget(WidgetInfo.BANK_EQUIPMENT_BUTTON);
//        return bank != null && !bank.isHidden();
    }

    private boolean isNPCContactWindowOpen() {
        Widget chatModal = client.getWidget(4915204);
        return chatModal != null;
    }

    private boolean isChattingDarkMage() {
        Widget chatModal = client.getWidget(15138820);
        Widget chatOptions = client.getWidget(14352385);
        Widget chatName = client.getWidget(14221316);
        return chatModal != null || chatOptions != null || chatName != null;
    }

    private void darkMageChat() {
        Widget chatboxLeft = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
        Widget chatboxRight = client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT);
        Widget chatOptions = client.getWidget(WidgetInfo.DIALOG_OPTION_OPTIONS);

        if (chatboxLeft != null) {
            String s = chatboxLeft.getText();
            if (s.contains("What do you want? Can't you see I'm busy?")) {
                clicker.pressKey(KeyEvent.VK_SPACE);
            } else if (s.contains("A simple transfiguration spell should resolve things")) {
                needRepairPouch = false;
                isNpcContact = false;
            }
        } else if (chatboxRight != null) {
            String s = chatboxRight.getText();
            if (s.contains("Can you repair my pouches?")) {
                clicker.pressKey(KeyEvent.VK_SPACE);
            }
        } else if (chatOptions != null) {
            List<Widget> chatOptionsList = new ArrayList<>(Arrays.asList(chatOptions.getChildren()));
            if (!chatOptionsList.isEmpty()) {
                String s = chatOptionsList.get(2).getText();
                if (s.contains("Can you repair my pouches?")) {
                    clicker.pressKey(KeyEvent.VK_2);
                }
            }
        }
    }
}
