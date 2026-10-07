package net.runelite.client.plugins.dt2LeviathanAuto;

import net.runelite.client.plugins.autoBossCore.ui.BaseOverlay;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Thin subclass of {@link BaseOverlay} so Guice can inject it. */
@Singleton
public class LeviathanAutoOverlay extends BaseOverlay
{
	@Inject
	public LeviathanAutoOverlay()
	{
		super("DT2 Leviathan Auto", 5);
	}
}
