package net.runelite.client.plugins.bankPinInputer;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

@PluginDescriptor(
        name = "Bank Pin Inputer",
        description = "input bank pin"
)
public class BankPinInputerPlugin extends Plugin {
    @Inject
    private Client client;

    private Map<String, String> envVariables = new HashMap<>();

    private boolean lockout = false;

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!lockout) {
            inputBankPin();
        }
    }

    @Override
    protected void startUp() throws Exception {
        envVariables = parseJsonToMap("env.json");
    }

    @Override
    protected void shutDown() throws Exception {
        envVariables.clear();
    }

    public static Map<String, String> parseJsonToMap(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(filePath), Map.class);
    }

    private void inputBankPin() {
        if (isBankPinOpen() & !lockout) { // lockout false
            lockout = true; // lockout true
            String localPlayerName = client.getLocalPlayer().getName();
            String pin = envVariables.get(localPlayerName);
            processFourDigitString(pin);
        }
    }

    public void processFourDigitString(String str) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Robot robot = new Robot();
                for (char ch : str.toCharArray()) {
                    robot.keyPress(ch);
                    robot.delay(100);
                }
                robot.delay(1500);
            } catch (Exception ignored) {
            } finally {
                lockout = false; // lockout false
            }
        });
    }

    private boolean isBankPinOpen() {
        return client.getWidget(WidgetInfo.BANK_PIN_CONTAINER) != null;
    }
}
