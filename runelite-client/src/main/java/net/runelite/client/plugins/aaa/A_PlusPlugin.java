package net.runelite.client.plugins.aaa;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.agilityPlus.AgilityPlusPlugin;

import javax.inject.Inject;
import java.awt.Point;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@PluginDescriptor(
        name = "A Plus"
)
public class A_PlusPlugin extends Plugin {
    @Inject
    private Client client;

    private STATUS toggleStatus = STATUS.STOP;

    private boolean isHealthly() {
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

    private Point getFood(List<Item> inventory) {
        List<Item> foodSet = inventory.stream().filter(
                (item) -> item.getId() == ItemID.CAKE ||
                        item.getId() == ItemID._23_CAKE ||
                        item.getId() == ItemID.SLICE_OF_CAKE
        ).collect(Collectors.toList());

        int indexOfFood = Integer.MAX_VALUE;

        for(Item food : foodSet) {
            int idx = inventory.indexOf(food);
            if(idx < indexOfFood)
                indexOfFood = idx;
        }

        Point rowByCol = new Point((indexOfFood /4), (indexOfFood % 4));

        Point foodPoint = new Point((798 + (rowByCol.y * 42)), (764 + (rowByCol.x * 36)));
        System.out.println("location of food: " + foodPoint.x + ", " + foodPoint.y);

        return foodPoint;
    }

    private List<Item> inventoryHasFood() {
        Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
        if(inventoryWidget != null) {
            ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
            if(inventory.contains(ItemID.SALMON) || inventory.contains(ItemID.CAKE) || inventory.contains(ItemID._23_CAKE) || inventory.contains(ItemID.SLICE_OF_CAKE))
                return new ArrayList<>(Arrays.asList(inventory.getItems()));
        }

        return null;
    }

    private void toggleStatus() {
        Widget chatboxInput = client.getWidget(WidgetInfo.CHATBOX_INPUT);
        String chatBoxMessage = stripTargetAnchors(chatboxInput.getText());
        if(chatBoxMessage == null) return;

        if(chatBoxMessage.equals("go") && toggleStatus == STATUS.STOP) {
            toggleStatus = STATUS.START;
            System.out.println("status is go");
        } else if (chatBoxMessage.equals("stop") && toggleStatus == STATUS.START) {
            toggleStatus = STATUS.STOP;
            System.out.println("status is stop");
        }
    }

    private String stripTargetAnchors(String text) {
        Matcher m = Pattern.compile(">(.*?)<").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private boolean checkIdle(int idleTicks)
    {
        int idleClientTicks = client.getKeyboardIdleTicks();

        if (client.getMouseIdleTicks() < idleClientTicks)
        {
            idleClientTicks = client.getMouseIdleTicks();
        }

        return idleClientTicks >= idleTicks;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
//        System.out.println("start " + Instant.now());
//        if(checkIdle(500))
//            System.out.println("end " +Instant.now());
//        toggleStatus();
//
//        if(toggleStatus == STATUS.START) {
//            List<Item> inventory = inventoryHasFood();
//            if(inventory != null) {
//                Point foodPoint = getFood(inventory);
//                toggleStatus = STATUS.PAUSE;
//            }
//        } else if(toggleStatus == STATUS.PAUSE) {}
    }

    enum STATUS{
        START,
        STOP,
        PAUSE
    }
}
