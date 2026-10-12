package net.runelite.client.plugins.dt2DukeAuto;

import net.runelite.client.plugins.autoBossCore.ui.BaseOverlay;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Thin subclass of {@link BaseOverlay} so Guice can inject it. */
@Singleton
public class DukeAutoOverlay extends BaseOverlay
{
	@Inject
	public DukeAutoOverlay()
	{
		super("DT2 Duke Auto", 5);
	}
}
