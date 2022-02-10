package net.runelite.client.plugins.smithing;

import com.google.inject.Provides;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
        name = "Smithing Anvil",
        description = "Only smith chose Item",
        tags = {"smithing", "smith", "darts"}
)
public class SmithingAnvilPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private ChatMessageManager chatMessageManager;

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
            sendChatMessage("Event consumed, only allowed to smith " + config.selectAnvilOption().getOption());
        }
    }

    private void sendChatMessage(String chatMessage)
    {
        final String message = new ChatMessageBuilder()
                .append(new Color(60,70,200), chatMessage)
                .build();

        chatMessageManager.queue(
                QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(message)
                        .build());
    }

}
