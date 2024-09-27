package net.runelite.client.plugins.hunterrumours;

import net.runelite.client.plugins.hunterrumours.enums.Rumour;
import net.runelite.client.plugins.hunterrumours.enums.RumourLocation;
import net.runelite.client.plugins.hunterrumours.enums.Trap;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.util.ColorUtil;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RumourInfoBox extends InfoBox {
    private final HunterRumoursPlugin plugin;

    public RumourInfoBox(Rumour rumour, @Nonnull HunterRumoursPlugin plugin, @Nonnull ItemManager itemManager) {
        super(itemManager.getImage(rumour.getTargetCreature().getItemId()), plugin);
        this.plugin = plugin;

        final var locations = RumourLocation.getGroupedLocationsForRumour(rumour);
        final StringBuilder sb = new StringBuilder();

        locations.forEach(entry -> {
            RumourLocation rumourLocation = entry.getValue().get(0);

            sb.append("</br>  • ").append(entry.getKey()).append(" (");
            if (!rumourLocation.getFairyRingCode().equals("")) {
                sb.append(rumourLocation.getFairyRingCode()).append(", ");
            }
            sb.append(entry.getValue().size()).append(" spawns)");
        });

        final Trap trap = rumour.getTargetCreature().getTrap();
        final int pityThreshold = trap.calculatePityRateForItems(plugin.getHunterKitItems());
        String hasFinishedRumourText = plugin.getHunterRumourState() ? "Yes" : "No";

        this.setTooltip(
                ColorUtil.wrapWithColorTag("Rumour: ", Color.YELLOW) + rumour.getFullName() + "</br>" +
                        ColorUtil.wrapWithColorTag("Finished: ", Color.YELLOW) + hasFinishedRumourText + "</br>" +
                        ColorUtil.wrapWithColorTag("Item: ", Color.YELLOW) + itemManager.getItemComposition(rumour.getRumourItemID()).getName() + "</br>" +
                        ColorUtil.wrapWithColorTag("Caught: ", Color.YELLOW) + plugin.getCaughtRumourCreatures() + " / " + pityThreshold + "</br>" +
                        ColorUtil.wrapWithColorTag("Locations:", Color.YELLOW) + sb
        );
    }

    @Override
    public String getText() {
        if (plugin.getHunterRumourState()) {
            return "Done";
        }

        final Rumour currentRumour = plugin.getCurrentRumour();
        if (currentRumour != Rumour.NONE && showNumUntilPity()) {
            final int pityThreshold = currentRumour.getTrap().calculatePityRateForItems(plugin.getHunterKitItems());
            return String.valueOf(pityThreshold - plugin.getCaughtRumourCreatures());
        }

        return "";
    }

    @Override
    public Color getTextColor() {
        HunterRumoursConfig config = plugin.getConfig();
        if (plugin.getHunterRumourState()) {
            return config.completedRumourInfoBoxTextColor();
        } else if (!showNumUntilPity()) {
            return config.defaultInfoBoxTextColor();
        } else {
            final Rumour currentRumour = plugin.getCurrentRumour();
            if (currentRumour == Rumour.NONE) {
                return config.defaultInfoBoxTextColor();
            }
            final int caughtCreatures = plugin.getCaughtRumourCreatures();
            final int pityThreshold = currentRumour.getTrap().calculatePityRateForItems(plugin.getHunterKitItems());
            final float percentage = (float) caughtCreatures / pityThreshold * 100f;
            if (percentage >= 75) {
                return config.unluckyRateInfoBoxTextColor();
            } else if (percentage >= 50) {
                return config.normalRateInfoBoxTextColor();
            } else {
                return config.luckyRateInfoBoxTextColor();
            }
        }
    }

    private boolean showNumUntilPity() {
        if (this.plugin == null || this.plugin.getConfig() == null) {
            return false;
        }

        return this.plugin.getConfig().showCatchesRemainingUntilPity();
    }
}