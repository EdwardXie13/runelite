package net.runelite.client.plugins.leftClickConstruction;

import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.Robot;
import java.awt.event.KeyEvent;

@PluginDescriptor(name = "Left Click Construction")
public class LeftClickConstructionPlugin extends Plugin {

    @Inject
    private Client client;

    @Subscribe
    public void onGameTick(GameTick event) {
        removeBox();
        buildBox1();
        buildBox2();
        butlerChat();
        butlerPayment();

    }

//    @Subscribe
//    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded)
//    {
//        swapBuild();
//    }

//    private void swapBuild() {
//        MenuEntry[] menuEntries = client.getMenuEntries();
//
//        for (int i = menuEntries.length - 1; i >= 0; --i) {
//            MenuEntry entry = menuEntries[i];
//
//            if (
//                    (entry.getOption().equals("Build") && entry.getTarget().contains("Door space") && menuEntries.length == 4)
////                            ||
////                entry.getOption().equals("Remove") && entry.getIdentifier() == 13345
//            )
//            {
//                menuEntries[i] = menuEntries[menuEntries.length - 1];
//                menuEntries[menuEntries.length - 1] = entry;
//
//                client.setMenuEntries(menuEntries);
//                break;
//            }
//        }
//    }

    private void removeBox() {
        Widget removeBox = client.getWidget(14352385);
        if (removeBox != null &&
            (removeBox.getChild(0).getText().contains("Really remove it") ||
            removeBox.getChild(0).getText().contains("Repeat last task"))
        ) {
            pressOtherKey(KeyEvent.VK_1);
        }
    }

    private void buildBox1() {
        Widget removeBox = client.getWidget(30015492);
        if (removeBox != null && removeBox.getName().contains("Oak door")) {
            pressOtherKey(KeyEvent.VK_1);
        }
    }

    private void buildBox2() {
        Widget removeBox = client.getWidget(30015493);
        if(removeBox != null && removeBox.getName().contains("Oak larder")) {
            pressOtherKey(KeyEvent.VK_2);
        }
    }

    private void butlerChat() {
        Widget removeBox = client.getWidget(15138822);
        if (
                removeBox != null && removeBox.getText().contains("if thou")
                    ||
                removeBox != null && removeBox.getText().contains("unfailing service")

        ) {
            pressKey(KeyEvent.VK_SPACE);
        }
    }

    private void butlerPayment() {
        Widget removeBox = client.getWidget(14352385);
        if (removeBox != null && removeBox.getChild(1).getText().contains("Okay, here's")) {
            pressOtherKey(KeyEvent.VK_1);
        }
    }

    private void pressKey(int key) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key);
        this.client.getCanvas().dispatchEvent(keyRelease);
    }

    private void pressOtherKey(int key) {
        try {
            Robot robot = new Robot();
            robot.keyPress(key);
        } catch (Exception ignored){
        }
    }
}
