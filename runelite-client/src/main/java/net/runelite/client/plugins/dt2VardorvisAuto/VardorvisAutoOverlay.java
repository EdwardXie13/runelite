package net.runelite.client.plugins.dt2VardorvisAuto;

import net.runelite.client.plugins.autoBossCore.ui.BaseOverlay;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Thin subclass of {@link BaseOverlay} so Guice can inject it. */
@Singleton
public class VardorvisAutoOverlay extends BaseOverlay
{
	@Inject
	public VardorvisAutoOverlay()
	{
		super("DT2 Vardorvis Auto", 5);
	}
}
