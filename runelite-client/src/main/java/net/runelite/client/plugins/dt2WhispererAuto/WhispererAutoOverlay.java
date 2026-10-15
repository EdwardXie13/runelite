package net.runelite.client.plugins.dt2WhispererAuto;

import net.runelite.client.plugins.autoBossCore.ui.BaseOverlay;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Thin subclass of {@link BaseOverlay} so Guice can inject it. */
@Singleton
public class WhispererAutoOverlay extends BaseOverlay
{
	@Inject
	public WhispererAutoOverlay()
	{
		super("DT2 Whisperer Auto", 6);
	}
}
