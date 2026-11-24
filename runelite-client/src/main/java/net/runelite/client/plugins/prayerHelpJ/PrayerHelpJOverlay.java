package net.runelite.client.plugins.prayerHelpJ;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** Tiny status panel: whether the Jad helper is armed (ON/OFF) and how many targeted Jads are in the scene. */
class PrayerHelpJOverlay extends OverlayPanel
{
	private final PrayerHelpJPlugin plugin;

	@Inject
	private PrayerHelpJOverlay(PrayerHelpJPlugin plugin)
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
			.text("Prayer Help J")
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Status:")
			.right(on ? "ON" : "OFF")
			.rightColor(on ? Color.GREEN : Color.RED)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Jads:")
			.right(Integer.toString(plugin.jadCount()))
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Set:")
			.right(plugin.setStatus())
			.build());
		return super.render(graphics);
	}
}
