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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

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

        //mahogany homes
        playerDialogue();
        isConstructionWindowOpen();
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

    private void playerDialogue() {
        Widget dialogueWindow = client.getWidget(14221318);
        if(dialogueWindow != null &&
                (dialogueWindow.getText().contains("finished with the work you wanted") ||
                dialogueWindow.getText().contains("Could I have an adept contract") ||
                dialogueWindow.getText().contains("my current construction") ||
                dialogueWindow.getText().contains("love a cuppa"))
        ) {
            pressKey(KeyEvent.VK_SPACE);
        }
    }

    private void isConstructionWindowOpen() {
        // 30015492 - 30015499
        Widget constructionWindow = client.getWidget(30015489);
        if(constructionWindow != null && constructionWindow.getChild(1).getText().contains("Furniture Creation Menu")) {
            pressOtherKey('3'); // Teak
        }
    }

    private void removeBox() {
        Widget removeBox = client.getWidget(14352385);
        if (removeBox != null &&
                (removeBox.getChild(0).getText().contains("Really remove it") ||
                removeBox.getChild(0).getText().contains("Repeat last task") ||
                removeBox.getChild(0).getText().contains("Take tea?"))
        ) {
            pressOtherKey('1');
        } else if (removeBox != null && removeBox.getChild(3).getText().contains("Adept Contract")) {
            pressOtherKey('3');
        }
    }

    private void buildBox1() {
        Widget removeBox = client.getWidget(30015492);
        if (removeBox != null && removeBox.getName().contains("Oak door")) {
            pressOtherKey('1');
        }
    }

    private void buildBox2() {
        Widget removeBox = client.getWidget(30015493);
        if(removeBox != null && removeBox.getName().contains("Oak larder")) {
            pressOtherKey('2');
        }
    }

    private void butlerChat() {
        Widget removeBox = client.getWidget(15138822);
        if (removeBox != null &&
                (removeBox.getText().contains("if thou") ||
                removeBox.getText().contains("unfailing service") ||
                removeBox.getText().contains("get another job") ||
                removeBox.getText().contains("Would you like a cup of tea"))
        ) {
            pressKey(KeyEvent.VK_SPACE);
        }
    }

    private void butlerPayment() {
        Widget removeBox = client.getWidget(14352385);
        if (removeBox != null &&
                (removeBox.getChild(1).getText().contains("Okay, here's") ||
                removeBox.getChild(1).getText().contains("text take tea"))
        ) {
            pressOtherKey('1');
        }
    }

    private void pressKey(int key) {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, key, KeyEvent.CHAR_UNDEFINED);
        this.client.getCanvas().dispatchEvent(keyRelease);
    }

    private void pressOtherKey(int key) {
        KeyEvent keyTyped = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, (char) key);
        this.client.getCanvas().dispatchEvent(keyTyped);
    }
}
