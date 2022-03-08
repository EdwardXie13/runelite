package net.runelite.client.plugins.wintertodt;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;

class WintertodtIdleOverlay extends OverlayPanel
{
    private final WintertodtPlugin plugin;
    private final WintertodtConfig wintertodtConfig;
    private final Client client;

    @Inject
    private WintertodtIdleOverlay(WintertodtPlugin plugin, WintertodtConfig wintertodtConfig, Client client)
    {
        super(plugin);
        this.plugin = plugin;
        this.wintertodtConfig = wintertodtConfig;
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return client.getCanvas().getSize();
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isInWintertodt() || plugin.getPreviousTimerValue() > wintertodtConfig.roundNotification())
        {
            return null;
        }

        if (plugin.getCurrentActivity() == WintertodtActivity.IDLE)
        {
            final Color color = graphics.getColor();
            graphics.setColor(new Color(0, 255, 255, 50));
            graphics.fill(new Rectangle(client.getCanvas().getSize()));
            graphics.setColor(color);
        }

        return super.render(graphics);
    }
}