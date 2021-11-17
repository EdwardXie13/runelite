package net.runelite.client.plugins.runecraftPlus;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

public class StatusOverlayPanel extends OverlayPanel {
    RunecraftActivity CurrentStatus = RunecraftActivity.IDLE;

    private final Client client;
    private final RunecraftPlusPlugin plugin;
    private final RunecraftPlusConfig config;

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
        if (!this.config.showStatus()) {
            return null;
        }

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(String.valueOf(CurrentStatus))
                .color(Color.GREEN)
                .build());
        return super.render(graphics);
    }
}
