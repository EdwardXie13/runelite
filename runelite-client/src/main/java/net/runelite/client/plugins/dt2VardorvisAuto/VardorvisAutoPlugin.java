package net.runelite.client.plugins.dt2VardorvisAuto;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.api.events.GameTick;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.autoBossCore.debug.CaptureLogger;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.event.KeyEvent;

/**
 * RuneLite entry point. Keeps all event wiring thin -- strategy lives in
 * {@link VardorvisAutoMain}.
 */
@PluginDescriptor(
	name = "DT2 Vardorvis Auto",
	description = "Automation + capture harness for Vardorvis.",
	tags = {"dt2", "vardorvis", "boss", "auto"},
	enabledByDefault = false
)
@Slf4j
public class VardorvisAutoPlugin extends Plugin implements KeyListener
{
	@Inject private EventBus eventBus;
	@Inject private OverlayManager overlayManager;
	@Inject private KeyManager keyManager;
	@Inject private ClientThread clientThread;

	@Inject private VardorvisAutoConfig config;
	@Inject private VardorvisAutoOverlay overlay;

	@Inject private MirrorState mirrorState;
	@Inject private CaptureLogger captureLogger;
	@Inject private VardorvisAutoMain main;

	@Provides
	VardorvisAutoConfig provideConfig(ConfigManager cm)
	{
		return cm.getConfig(VardorvisAutoConfig.class);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e) {
		System.out.println("[VARD MENU] "
				+ " action=" + e.getMenuAction()
				+ " id="     + e.getId()
				+ " itemId=" + e.getItemId()
				+ " param0=" + e.getParam0()
				+ " param1=" + e.getParam1()
				+ " option='" + e.getMenuOption() + "'"
				+ " target='" + e.getMenuTarget() + "'");
	}

	@Override
	protected void startUp()
	{
		eventBus.register(mirrorState);
		eventBus.register(captureLogger);
		keyManager.registerKeyListener(this);
		overlayManager.add(overlay);
		main.bindOverlay(overlay);
		applyCaptureConfig();
		log.info("[vardAuto] startUp");
	}

	@Override
	protected void shutDown()
	{
		main.stop();
		captureLogger.setEnabled(false);
		overlayManager.remove(overlay);
		keyManager.unregisterKeyListener(this);
		eventBus.unregister(captureLogger);
		eventBus.unregister(mirrorState);
		log.info("[vardAuto] shutDown");
	}

	// -------------------------------------------------------------
	// Event forwarding.
	// -------------------------------------------------------------

	@Subscribe
	public void onGameTick(GameTick e)
	{
		main.onGameTick();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!"dt2VardorvisAuto".equals(e.getGroup())) return;
		applyCaptureConfig();
	}

	// -------------------------------------------------------------
	// Hotkey.
	// -------------------------------------------------------------

	@Override public void keyTyped(KeyEvent e) {}
	@Override public void keyReleased(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (!config.toggleKey().matches(e)) return;
		if (main.isActive()) main.stop(); else main.start();
		e.consume();
	}

	// -------------------------------------------------------------

	private void applyCaptureConfig()
	{
		captureLogger.setArenaRegion(config.arenaRegion());
		captureLogger.setPrefix(config.capturePrefix());
		captureLogger.setEnabled(config.captureMode());
	}
}
