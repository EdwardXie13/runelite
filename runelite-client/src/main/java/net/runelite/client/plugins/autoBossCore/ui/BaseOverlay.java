package net.runelite.client.plugins.autoBossCore.ui;

import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic status panel for a boss auto plugin. Shows:
 * <ul>
 *   <li>Plugin title.</li>
 *   <li>Current step string (set via {@link #setStep(String)}).</li>
 *   <li>Recent step history (last N transitions).</li>
 *   <li>Named stat lines (set via {@link #setStat(String, String)}).</li>
 * </ul>
 * <p>
 * The subclass is still a RuneLite overlay; this class is intended to be either extended
 * or wrapped. Not injected -- the boss plugin owns the instance.
 */
public class BaseOverlay extends OverlayPanel
{
	private final String title;
	private final int historySize;

	private String currentStep = "";
	private final Deque<String> history = new ArrayDeque<>();
	private final Map<String, String> stats = new LinkedHashMap<>();

	public BaseOverlay(String title) { this(title, 5); }

	public BaseOverlay(String title, int historySize)
	{
		this.title = title;
		this.historySize = Math.max(0, historySize);
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	public synchronized void setStep(String s)
	{
		if (s == null || s.equals(currentStep)) return;
		if (!currentStep.isEmpty())
		{
			history.addFirst(currentStep);
			while (history.size() > historySize) history.removeLast();
		}
		currentStep = s;
	}

	public synchronized void setStat(String key, String value)
	{
		if (value == null) stats.remove(key); else stats.put(key, value);
	}

	public synchronized void clearStats() { stats.clear(); }

	@Override
	public Dimension render(Graphics2D g)
	{
		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(
			TitleComponent.builder().text(title).color(Color.CYAN).build()
		);

		panelComponent.getChildren().add(LineComponent.builder()
			.left("step")
			.right(currentStep)
			.rightColor(Color.YELLOW)
			.build());

		for (Map.Entry<String, String> e : stats.entrySet())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left(e.getKey())
				.right(e.getValue())
				.build());
		}

		int shown = 0;
		for (String h : history)
		{
			if (shown++ >= historySize) break;
			panelComponent.getChildren().add(LineComponent.builder()
				.left("<")
				.right(h)
				.rightColor(Color.LIGHT_GRAY)
				.build());
		}

		return super.render(g);
	}
}
