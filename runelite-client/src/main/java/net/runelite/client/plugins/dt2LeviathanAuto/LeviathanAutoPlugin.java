package net.runelite.client.plugins.dt2LeviathanAuto;

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
import net.runelite.api.events.ChatMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.autoBossCore.debug.CaptureLogger;
import net.runelite.client.plugins.autoBossCore.state.MirrorState;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.awt.event.KeyEvent;

/**
 * RuneLite entry point for Leviathan. Keeps event wiring thin -- strategy lives in
 * {@link LeviathanAutoMain}. Mirrors the Vardorvis plugin layout.
 */
@PluginDescriptor(
	name = "DT2 Leviathan Auto",
	description = "Automation + capture harness for Leviathan.",
	tags = {"dt2", "leviathan", "boss", "auto"},
	enabledByDefault = false
)
@Slf4j
public class LeviathanAutoPlugin extends Plugin implements KeyListener
{
	@Inject private EventBus eventBus;
	@Inject private OverlayManager overlayManager;
	@Inject private KeyManager keyManager;
	@Inject private ClientThread clientThread;

	@Inject private LeviathanAutoConfig config;
	@Inject private LeviathanAutoOverlay overlay;

	@Inject private MirrorState mirrorState;
	@Inject private CaptureLogger captureLogger;
	@Inject private LeviathanAutoMain main;

	@Provides
	LeviathanAutoConfig provideConfig(ConfigManager cm)
	{
		return cm.getConfig(LeviathanAutoConfig.class);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked e) {
		System.out.println("[LEV MENU] "
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
		log.info("[levAuto] startUp");
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
		log.info("[levAuto] shutDown");
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
	public void onChatMessage(ChatMessage e)
	{
		main.onChatMessage(e.getMessage());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!"dt2LeviathanAuto".equals(e.getGroup())) return;
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
