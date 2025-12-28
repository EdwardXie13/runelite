package net.runelite.client.plugins.bloodRuneTrue;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.plusUtils.StepOverlay;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.api.Skill.AGILITY;

@PluginDescriptor(name = "BloodRuneTrue", enabledByDefault = false)
@Slf4j
public class BloodRuneTruePlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private StepOverlay overlay;

    BloodRuneTrueMain main;

    private boolean hasStarted = false;

    private final List<Integer> TRACKED_IDS = Arrays.asList(
            BloodRuneTrueObjectIDs.FAIRY_RING_POH,
            BloodRuneTrueObjectIDs.POOL_OF_REVITALISATION,
            BloodRuneTrueObjectIDs.POOL_OF_REJUVENATION,
            BloodRuneTrueObjectIDs.FANCY_POOL_OF_REJUVENATION,
            BloodRuneTrueObjectIDs.ORNATE_POOL_OF_REJUVENATION,
            BloodRuneTrueObjectIDs.FAIRY_RING_CAVE_ENTRANCE,
            BloodRuneTrueObjectIDs.SECOND_CAVE_ENTRANCE,
            BloodRuneTrueObjectIDs.CAVE_SHORTCUT,
            BloodRuneTrueObjectIDs.CAVE_TO_MYSTERIOUS_RUINS,
            BloodRuneTrueObjectIDs.BLOOD_MYSTERIOUS_RUINS,
            BloodRuneTrueObjectIDs.TRUE_BLOOD_ALTAR,
            BloodRuneTrueObjectIDs.CASTLE_WARS_BANK_CHEST
    );

//    @Subscribe
//    public void onStatChanged(StatChanged statChanged) {
//        if (statChanged.getSkill() != AGILITY) {
//            return;
//        }
//        BloodRuneTrueMain.xpDrop = true;
//        overlay.setCurrentStep("xpDrop = true");
//        log.info("xpDrop = true");
//    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        ItemContainer inv = event.getItemContainer();
        if (inv == null) return;

        int containerId = event.getContainerId();

        if (containerId == InventoryID.INVENTORY.getId())
        {
            BloodRuneTrueMain.currentInventory.clear();
            Collections.addAll(BloodRuneTrueMain.currentInventory, inv.getItems());
            if (inv.contains(ItemID.BLOOD_ESSENCE) && inv.contains(ItemID.BLOOD_ESSENCE_ACTIVE)) {
                System.out.println("already have blood essence");
                BloodRuneTrueMain.needBloodEssence = false;
            } else if (inv.contains(ItemID.COLOSSAL_POUCH_26786)) {
                System.out.println("need to repair pouch");
                BloodRuneTrueMain.needRepairPouch = true;
            } else if (inv.contains(ItemID.COLOSSAL_POUCH)) {
                System.out.println("pouch is ok");
                BloodRuneTrueMain.needRepairPouch = false;
            }
        }
        else if (containerId == InventoryID.BANK.getId())
        {
            BloodRuneTrueMain.currentBank.clear();
            Collections.addAll(BloodRuneTrueMain.currentBank, inv.getItems());
        }
        else if (event.getContainerId() == InventoryID.EQUIPMENT.getId()) {
            BloodRuneTrueMain.currentEquipment.clear();
            Collections.addAll(BloodRuneTrueMain.currentEquipment, inv.getItems());
            if (inv.contains(ItemID.RING_OF_DUELING8)) {
                System.out.println("DUELING EQUIPPED");
                BloodRuneTrueMain.needRingOfDueling = false;
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        // Only listen to system/game messages
        if (event.getType() == ChatMessageType.GAMEMESSAGE)
        {
            String message = event.getMessage();

            if (message.contains("Your ring of dueling crumbles to dust")) {
                System.out.println("Ring of dueling broken!");
                BloodRuneTrueMain.needRingOfDueling = true;
            } else if (message.contains("Your blood essence falls apart, drained of energy")) {
                System.out.println("Blood essence broken");
                BloodRuneTrueMain.needBloodEssence = true;
            } else if (message.contains("You activate the blood essence")) {
                System.out.println("Blood essence activated");
                BloodRuneTrueMain.needBloodEssence = false;
            } else if (message.contains("Your pouch has decayed through use")) {
                System.out.println("Need to NPC contact");
                BloodRuneTrueMain.needRepairPouch = true;
            }
        }
    }

    @Override
    protected void startUp() throws Exception {
        BloodRuneTrueObjectIDs.setAllVarsNull();
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        BloodRuneTrueObjectIDs.setAllVarsNull();
        overlayManager.remove(overlay);
    }

    @Subscribe
    public void onClientTick(ClientTick event) throws AWTException {
        toggleStatus();
        getClickboxes();
        getShapeForTileObject();
        BloodRuneTrueMain.isEquipmentOpen = isEquipmentOpen();
        BloodRuneTrueMain.isLunarBookOpen = isLunarBookOpen();
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged event) {
        if (!(event.getActor() instanceof Player))
            return;

        Player p = (Player) event.getActor();

        if (p == client.getLocalPlayer())
        {
            int anim = p.getAnimation();

            if (anim == 714) {
                System.out.println("isTeleportingCW true");
                BloodRuneTrueMain.isTeleportingCW = true;
            } else if (anim == 4069) {
                System.out.println("isTeleportingPOH true");
                BloodRuneTrueMain.isTeleportingPOH = true;
            }
        }
    }

    private String stripTargetAnchors(String text) {
        Matcher m = Pattern.compile("ff>(.*?)</c").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private void toggleStatus() {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        if (chatboxInput == null) return;

        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if(chatBoxMessage.equals("1") && !BloodRuneTrueMain.isRunning && !hasStarted) {
            main = new BloodRuneTrueMain(client, clientThread, overlay);
            main.reset();

            BloodRuneTrueMain.isRunning = true;
            hasStarted = true;
            overlay.setCurrentStep("status is go");
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("2") && BloodRuneTrueMain.isRunning && hasStarted) {
            main.stop();
            BloodRuneTrueMain.isRunning = false;
            hasStarted = false;
            overlay.setCurrentStep("status is stop");
            System.out.println("status is stop");
        }
    }

    @Subscribe
    private void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN && BloodRuneTrueMain.isRunning && hasStarted)
        {
            BloodRuneTrueMain.isRunning = false;
            hasStarted = false;
            System.out.println("status is stop (login screen)");
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        BloodRuneTrueObjectIDs.assignObjects(client, event);
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        BloodRuneTrueObjectIDs.assignObjects(client, event);
    }

    @Subscribe
    public void onWallObjectSpawned(WallObjectSpawned event) {
        BloodRuneTrueObjectIDs.assignObjects(client, event);
    }

    @Subscribe
    public void onWallObjectDespawned(WallObjectDespawned event) {
        BloodRuneTrueObjectIDs.assignObjects(client, event);
    }

    private void getClickboxes() {
        BloodRuneTrueMain.clickboxCache.clear();

        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        int plane = client.getPlane();

        for (int x = 0; x < 104; x++)
        {
            for (int y = 0; y < 104; y++)
            {
                Tile tile = tiles[plane][x][y];
                if (tile == null) continue;

                // GameObjects
                for (GameObject obj : tile.getGameObjects())
                {
                    if (obj != null && TRACKED_IDS.contains(obj.getId()))
                        tryStoreClickbox(obj);
                }

                // WallObjects
                WallObject w = tile.getWallObject();
                if (w != null && TRACKED_IDS.contains(w.getId()))
                    tryStoreClickbox(w);

                // DecorativeObjects
                DecorativeObject d = tile.getDecorativeObject();
                if (d != null && TRACKED_IDS.contains(d.getId()))
                    tryStoreClickbox(d);

                // GroundObjects
                GroundObject g = tile.getGroundObject();
                if (g != null && TRACKED_IDS.contains(g.getId()))
                    tryStoreClickbox(g);
            }
        }
    }

    private void tryStoreClickbox(TileObject obj) {
        Shape s = obj.getClickbox();
        if (s != null)
        {
            BloodRuneTrueMain.clickboxCache.put(obj, s);
        }
    }

    private boolean isEquipmentOpen() {
        Widget menu = client.getWidget(WidgetInfo.EQUIPMENT_INVENTORY_ITEMS_CONTAINER);
        return (menu != null && !menu.isHidden());
    }

    private boolean isLunarBookOpen() {
        Widget chatModal = client.getWidget(14286848);
        if (chatModal != null) {
            return !chatModal.isHidden();
        }
        return false;
    }

    public void getShapeForTileObject() {
        if (BloodRuneTrueMain.pendingClickboxObject == null)
            return;

        try
        {
            Shape s = BloodRuneTrueMain.pendingClickboxObject.getClickbox();
            if (s != null)
            {
                BloodRuneTrueMain.pendingClickboxShape = s;
            }
        }
        catch (Exception ignored)
        {
            // Client thread guarantees safety, but be defensive
        }
        finally
        {
            BloodRuneTrueMain.pendingClickboxObject = null;
        }
    }
}
