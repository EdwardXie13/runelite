package net.runelite.client.plugins.itemchargesimproved.item.triggers;

public class OnAnimationChanged extends TriggerBase {
    public final int[] animationId;

    public OnAnimationChanged(final int ...animationId) {
        this.animationId = animationId;
    }
}
