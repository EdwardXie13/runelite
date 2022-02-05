package net.runelite.client.plugins.smithing;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@PluginDescriptor(
        name = "SmithingAnvil",
        description = "Only smith chose Item",
        tags = {"smithing", "smith", "darts"}
)
public class SmithingAnvilPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private SmithingAnvilConfig config;

    @Provides
    SmithingAnvilConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(SmithingAnvilConfig.class);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        String menuOption = event.getMenuOption();
        String menuTarget = event.getMenuTarget();

        if (client.getWidget(WidgetInfo.SMITHING_INVENTORY_ITEMS_CONTAINER) != null &&
            menuOption.contains("Smith") &&
            !menuTarget.contains(config.selectAnvilOption().getOption())) {
            event.consume();
        }
    }

}
