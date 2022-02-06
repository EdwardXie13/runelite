package net.runelite.client.plugins.neverLogout;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.concurrent.Executors;

import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
        name = "NeverLogout",
        description = "Plugin to never logout",
        tags = {"logout"}
)
public class NeverLogoutPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private NeverLogoutConfig config;

    @Provides
    NeverLogoutConfig getConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NeverLogoutConfig.class);
    }

    private final Random random = new Random();
    private long randomDelay;

    @Override
    protected void startUp() throws Exception
    {
        if (client.getGameState() == GameState.LOGGED_IN) {
            randomDelay = randomDelay();
        }
    }

    @Override
    protected void shutDown() throws Exception
    {

    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (checkIdleLogout() && config.neverLogout())
        {
            randomDelay = randomDelay();
            Executors.newSingleThreadExecutor()
                    .submit(this::pressKey);
        }
    }

    private boolean checkIdleLogout()
    {
        int idleClientTicks = client.getKeyboardIdleTicks();

        if (client.getMouseIdleTicks() < idleClientTicks)
        {
            idleClientTicks = client.getMouseIdleTicks();
        }

        return idleClientTicks >= randomDelay;
    }

    private long randomDelay()
    {
        return (long) clamp(
                Math.round(random.nextGaussian() * 8000)
        );
    }

    private static double clamp(double val)
    {
        return Math.max(1, Math.min(13000, val));
    }

    private void pressKey()
    {
        KeyEvent keyPress = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_BACK_SPACE);
        this.client.getCanvas().dispatchEvent(keyPress);
        KeyEvent keyRelease = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_BACK_SPACE);
        this.client.getCanvas().dispatchEvent(keyRelease);
        KeyEvent keyTyped = new KeyEvent(this.client.getCanvas(), KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_BACK_SPACE);
        this.client.getCanvas().dispatchEvent(keyTyped);
    }
}
