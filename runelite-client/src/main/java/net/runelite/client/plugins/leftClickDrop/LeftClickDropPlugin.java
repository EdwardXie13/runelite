package net.runelite.client.plugins.leftClickDrop;

import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@PluginDescriptor(name = "Left Click Drop")
public class LeftClickDropPlugin  extends Plugin {
    private static final Set<String> powerSet = new HashSet<>(Arrays.asList("Iron ore", "Leaping salmon", "Leaping trout", "Leaping sturgeon"));

    @Inject
    private Client client;

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded menuEntryAdded)
    {
        swapMenus();
    }

    public void swapMenus() {
        MenuEntry entry = client.getMenuEntries()[client.getMenuEntries().length - 1];
        if (powerDrop(entry))
            powerMineSwap();
    }

    public boolean powerDrop(MenuEntry e) {
        String target;
        try {
            target = stripTargetAnchors(e);
        } catch (Exception exception) {
            return false;
        }
        return powerSet.contains(target);
    }

    private void powerMineSwap() {
        MenuEntry[] menuEntries = client.getMenuEntries();
        for (int i = menuEntries.length - 1; i >= 0; --i) {
            MenuEntry entry = menuEntries[i];
            String target = stripTargetAnchors(entry);

            if(powerSet.contains(target) && entry.getIdentifier() == 7 && menuEntries.length == 4) {
                menuEntries[i] = menuEntries[menuEntries.length - 1];
                menuEntries[menuEntries.length - 1] = entry;

                client.setMenuEntries(menuEntries);
                break;
            }
        }
    }

    private String stripTargetAnchors(MenuEntry menuEntries) {
        Matcher m = Pattern.compile(">(.*?)<").matcher(menuEntries.getTarget());
        return m.find() ? m.group(1) : "";
    }
}
