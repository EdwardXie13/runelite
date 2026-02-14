package net.runelite.client.plugins.menuentryswapextended;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Keybind;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
enum TreeRingSwap implements HotkeyableMenuSwapsPlugin.hasKeybind {
    TREE("tree", HotkeyableMenuSwapsConfig::getSwapTreeHotkey),
    ZANARIS("zanaris", HotkeyableMenuSwapsConfig::getSwapZanarisHotkey),
    LAST_DESTINATION("last-destination", HotkeyableMenuSwapsConfig::getSwapLastDestinationHotkey),
    CONFIGURE("configure", HotkeyableMenuSwapsConfig::getSwapConfigureHotkey),
    ;

    private final String menuOptionName;
    private final Function<HotkeyableMenuSwapsConfig, Keybind> keybindFunction;

    public Keybind getKeybind(HotkeyableMenuSwapsConfig config) {
        return keybindFunction.apply(config);
    }
}
