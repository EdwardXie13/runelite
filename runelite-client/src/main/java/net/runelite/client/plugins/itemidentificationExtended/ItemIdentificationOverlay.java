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
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Color;
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
		textComponent.setColor(Color.WHITE);
		textComponent.setText(iden.name);
		textComponent.render(graphics);
	}

	private ItemIdentification findItemIdentification(final int itemID)
	{
		final int realItemId = itemManager.canonicalize(itemID);
		return ItemIdentification.get(realItemId);
	}
}
