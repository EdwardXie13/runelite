package net.runelite.client.plugins.prayerHelp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** Tiny status panel: shows whether the Prayer Help cadence is armed (ON) or not (OFF). */
class PrayerHelpOverlay extends OverlayPanel
{
	private final PrayerHelpPlugin plugin;

	@Inject
	private PrayerHelpOverlay(PrayerHelpPlugin plugin)
	{
		super(plugin);
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_LOW);
		this.plugin = plugin;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		boolean on = plugin.isFlicking();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Prayer Help")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Status:")
			.right(on ? "ON" : "OFF")
			.rightColor(on ? Color.GREEN : Color.RED)
			.build());
		return super.render(graphics);
	}
}
