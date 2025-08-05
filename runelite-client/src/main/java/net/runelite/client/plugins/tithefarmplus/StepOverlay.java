package net.runelite.client.plugins.tithefarmplus;

import lombok.Setter;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;

public class StepOverlay extends Overlay
{
    private final PanelComponent panelComponent = new PanelComponent();
    @Setter
    private String currentStep = "Idle";

    @Inject
    public StepOverlay()
    {
        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Step:")
                .right(currentStep)
                .build());
        return panelComponent.render(graphics);
    }
}