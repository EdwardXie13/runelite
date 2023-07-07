package net.runelite.client.plugins.itemchargesimproved;

import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.plugins.itemchargesimproved.triggers.TriggerItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

public class ChargedItemsOverlay extends WidgetItemOverlay {
    private final ChargesImprovedConfig config;
    private final ChargedItemInfoBox[] infoboxes_charged_items;

    public ChargedItemsOverlay(final ChargesImprovedConfig config, final ChargedItemInfoBox[] infoboxes_charged_items) {
        this.config = config;
        this.infoboxes_charged_items = infoboxes_charged_items;
        showOnInventory();
        showOnEquipment();
        showOnBank();
    }

    @Override
    public void renderItemOverlay(final Graphics2D graphics, final int item_id, final WidgetItem item_widget) {
        if (!config.showItemOverlays()) return;

        for (final ChargedItemInfoBox infobox : infoboxes_charged_items) {
            if (infobox.triggers_items == null || config.getHiddenItemOverlays().contains(infobox.infobox_id)) continue;

            TriggerItem trigger_item = null;
            for (final TriggerItem trigger : infobox.triggers_items) {
                if (trigger.item_id == item_id) {
                    trigger_item = trigger;
                    break;
                }
            }
            if (trigger_item == null) continue;

            // Charges from infobox.
            String charges = ChargesImprovedPlugin.getChargesMinified(infobox.getCharges());

            graphics.setFont(FontManager.getRunescapeSmallFont());

            // Charges from name (override the infobox).
            if (trigger_item.fixed_charges != null) {
                charges = ChargesImprovedPlugin.getChargesMinified(trigger_item.fixed_charges);
            }

            final Rectangle bounds = item_widget.getCanvasBounds();
            final TextComponent charges_component = new TextComponent();

            charges_component.setPosition(new Point(bounds.x, (int) bounds.getMaxY()));
            charges_component.setColor(charges.equals("?") ? config.getColorUnknown() : charges.equals("0") && !infobox.zero_charges_is_positive ? config.getColorEmpty() : config.getColorDefault());
            charges_component.setText(charges);
            charges_component.render(graphics);

            return;
        }
    }
}
