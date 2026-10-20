package net.runelite.client.plugins.autoBossCore.state;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

/**
 * Small, reusable helpers for reading interface {@link Widget}s by their packed
 * component id. Boss plugins use these to read HUD text such as a boss HP readout
 * ("700/700") without each one re-implementing null-safe widget access + parsing.
 * <p>
 * All methods are null-safe: a missing/hidden widget or unparseable text yields
 * {@code null} (or a sentinel), never an exception.
 */
public final class WidgetReader
{
	private WidgetReader() {}

	/**
	 * Packed component id of the universal boss HP bar HUD shared by the DT2 bosses (the in-fight
	 * "current / max" health overlay). It reads 0 at the kill a few ticks before the NPC despawns.
	 * Parse with {@link #fraction} (index 0 = current HP, index 1 = max), or use {@link #bossHp}.
	 */
	public static final int BOSS_HP_COMPONENT = 19857428;

	/** Current boss HP from {@link #BOSS_HP_COMPONENT}, or -1 when the bar is not showing. */
	public static int bossHp(Client client)
	{
		int[] hp = fraction(client, BOSS_HP_COMPONENT);
		return hp == null ? -1 : hp[0];
	}

	/** Raw text of the widget with this packed component id, or {@code null}. */
	public static String text(Client client, int componentId)
	{
		if (client == null) return null;
		Widget w = client.getWidget(componentId);
		if (w == null || w.isHidden()) return null;
		String t = w.getText();
		return (t == null || t.isEmpty()) ? null : t;
	}

	/**
	 * Parse a "current/max" fraction out of arbitrary widget text (tolerates colour
	 * tags, spaces and commas, e.g. {@code "<col=ff0000>700 / 700</col>"}). Returns
	 * {@code [current, max]}, or {@code null} if two numbers separated by '/' aren't
	 * found.
	 */
	public static int[] parseFraction(String text)
	{
		if (text == null) return null;
		int slash = text.indexOf('/');
		if (slash < 0) return null;
		Integer cur = firstInt(text.substring(0, slash));
		Integer max = firstInt(text.substring(slash + 1));
		if (cur == null || max == null) return null;
		return new int[] { cur, max };
	}

	/**
	 * Read the widget's text and parse it as a "current/max" fraction in one call.
	 * Returns {@code [current, max]} or {@code null}.
	 */
	public static int[] fraction(Client client, int componentId)
	{
		return parseFraction(text(client, componentId));
	}

	/** First run of digits in {@code s} as an int, or {@code null} if none. */
	private static Integer firstInt(String s)
	{
		if (s == null) return null;
		int i = 0, n = s.length();
		while (i < n && !Character.isDigit(s.charAt(i))) i++;
		if (i >= n) return null;
		int start = i;
		while (i < n && Character.isDigit(s.charAt(i))) i++;
		try { return Integer.parseInt(s.substring(start, i)); }
		catch (NumberFormatException e) { return null; }
	}

	/** True when the bank interface is open. */
	public static boolean isBankOpen(Client client)
	{
		Widget w = client.getWidget(net.runelite.api.widgets.WidgetInfo.BANK_CONTAINER);
		return w != null && !w.isHidden();
	}
}
