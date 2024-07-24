package net.runelite.client.plugins.leftClickThief;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@PluginDescriptor(name = "Left Click Thief")
public class LeftClickThiefPlugin extends Plugin {
    private final Set<Integer> swapNPCS = new HashSet<>(
            Arrays.asList(
                    735, // 56 bandit
                    737, // 41 bandit
                    3550 // menaphite thug
            )
    );

    private final Set<Integer> animationKO = new HashSet<>(
            Arrays.asList(
                    808, // arrg my head
                    -1 // nothing
            ));

//    private final String KNOCK_OUT_FAIL = "Your blow only glances";
//    private final String ALREADY_KNOCKED_OUT = "They're unconscious.";

    @Inject
    private Client client;

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded)
    {
        swapMenus();
    }

//    @Subscribe
//    public void onChatMessage(ChatMessage event) {
//    }

    public void swapMenus() {
        MenuEntry entry = client.getMenuEntries()[client.getMenuEntries().length - 1];
        NPC npc = entry.getNpc();
        if(npc != null && swapNPCS.contains(npc.getId())) {
            String overheadText = npc.getOverheadText();
            if(overheadText != null) {
                if(overheadText.contains("Zzzzzz") ||
                    overheadText.contains("I'll kill you for that")
                ) {
                    swapPickpocket();
                } else if(overheadText.contains("Arghh my head")) {
                    swapKO();
                }
            } else {
                swapKO();
            }
        }
    }

    private void swapKO() {
        MenuEntry[] menuEntries = client.getMenuEntries();
        for (int i = menuEntries.length - 1; i >= 0; --i) {
            MenuEntry entry = menuEntries[i];
            if (entry.getOption().equals("Knock-Out"))
            {
                menuEntries[i] = menuEntries[menuEntries.length - 1];
                menuEntries[menuEntries.length - 1] = entry;

                client.setMenuEntries(menuEntries);
                break;
            }
        }
    }

    private void swapPickpocket() {
        MenuEntry[] menuEntries = client.getMenuEntries();
        for (int i = menuEntries.length - 1; i >= 0; --i) {
            MenuEntry entry = menuEntries[i];
            if (entry.getOption().equals("Pickpocket"))
            {
                menuEntries[i] = menuEntries[menuEntries.length - 1];
                menuEntries[menuEntries.length - 1] = entry;

                client.setMenuEntries(menuEntries);
                break;
            }
        }
    }
}
