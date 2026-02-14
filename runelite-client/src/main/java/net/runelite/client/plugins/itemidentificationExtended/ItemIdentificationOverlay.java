/*
 * Copyright (c) 2019, Hydrox6 <ikada@protonmail.ch>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.itemidentificationExtended;

import com.google.inject.Inject;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

class ItemIdentificationOverlay extends WidgetItemOverlay
{
	private final ItemManager itemManager;

	@Inject
	ItemIdentificationOverlay(ItemManager itemManager)
	{
		this.itemManager = itemManager;
		showOnInventory();
		showOnBank();
		showOnInterfaces(InterfaceID.KEPT_ON_DEATH, InterfaceID.GUIDE_PRICES, InterfaceID.LOOTING_BAG, InterfaceID.SEED_BOX, InterfaceID.KINGDOM);
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		ItemIdentification iden = findItemIdentification(itemId);
		if (iden == null)
		{
			return;
		}

		graphics.setFont(FontManager.getRunescapeSmallFont());
		renderText(graphics, widgetItem.getCanvasBounds(), iden);
	}

	private void renderText(Graphics2D graphics, Rectangle bounds, ItemIdentification iden)
	{
		final TextComponent textComponent = new TextComponent();
		textComponent.setPosition(new Point(bounds.x - 1, bounds.y + bounds.height - 1));
		textComponent.setColor(Color.WHITE);  // Default color for main text

		String text = iden.name;
		int subscriptYOffset = 4;  // Adjust this value to move the subscript lower

		// Check if the text contains a subscript (inside parentheses)
		Pattern pattern = Pattern.compile("\\((.*?)\\)\\s*(.*)");  // Matches "(subscriptText) mainText"
		Matcher matcher = pattern.matcher(text);

		if (matcher.matches())
		{
			String subscriptText = matcher.group(1).trim();  // Subscript text inside parentheses
			String mainText = matcher.group(2).trim();  // Text after parentheses

			// Apply a static font size of 10 for the subscript
			Font originalFont = graphics.getFont();
			Font subscriptFont = originalFont.deriveFont(15f);  // Set font size to 10
			graphics.setFont(subscriptFont);

			// Set the color for the subscript (e.g., Red)
			Color subscriptColor = Color.RED;
			textComponent.setColor(subscriptColor);

			// Render the subscript slightly lower
			textComponent.setText(subscriptText);
			textComponent.setPosition(new Point(bounds.x, bounds.y + bounds.height - 1 + subscriptYOffset));  // Move down
			textComponent.render(graphics);

			// Restore original font
			graphics.setFont(originalFont);

			// Render main text after the subscript
			textComponent.setColor(Color.WHITE);  // Reset color to white for main text
			textComponent.setText(mainText);
			textComponent.setPosition(new Point(
					bounds.x + graphics.getFontMetrics().stringWidth(subscriptText),
					bounds.y + bounds.height - 1  // Position after subscript
			));
			textComponent.render(graphics);

			return;
		}

		// If no subscript pattern found, render normally
		textComponent.setText(text);
		textComponent.render(graphics);
	}

	private ItemIdentification findItemIdentification(final int itemID)
	{
		final int realItemId = itemManager.canonicalize(itemID);
		return ItemIdentification.get(realItemId);
	}
}
