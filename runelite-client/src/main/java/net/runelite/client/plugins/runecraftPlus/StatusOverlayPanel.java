package net.runelite.client.plugins.runecraftPlus;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayPanel;
import static net.runelite.api.AnimationID.IDLE;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public class StatusOverlayPanel extends OverlayPanel {
    private String CurrentStatus = "";

    private final Client client;
    private final RunecraftPlusPlugin plugin;
    private final RunecraftPlusConfig config;

    private String currentActivity = "IDLE";

    @Inject
    private StatusOverlayPanel(Client client, RunecraftPlusPlugin plugin, RunecraftPlusConfig config)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.BOTTOM_RIGHT);
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "RC overlay"));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!this.config.showClickbox())
        {
            return null;
        }

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(currentActivity)
                .color(Color.GREEN)
                .build());

        return super.render(graphics);
    }
}
