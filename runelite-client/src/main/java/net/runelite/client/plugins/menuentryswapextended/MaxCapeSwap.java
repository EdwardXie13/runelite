package net.runelite.client.plugins.menuentryswapextended;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.config.Keybind;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
enum MaxCapeSwap implements HotkeyableMenuSwapsPlugin.hasKeybind {
    CRAFTING_GUILD("crafting guild", HotkeyableMenuSwapsConfig::maxCapeCraftingGuildSwapHotKey),
    WARRIORS_GUILD("warriors' guild", HotkeyableMenuSwapsConfig::maxCapeWarriorsGuildSwapHotKey),
    TELE_POH("tele to poh", HotkeyableMenuSwapsConfig::maxCapeTelePOHSwapHotKey),
    ;

    private final String menuOptionName;
    private final Function<HotkeyableMenuSwapsConfig, Keybind> keybindFunction;

    public Keybind getKeybind(HotkeyableMenuSwapsConfig config) {
        return keybindFunction.apply(config);
    }
}
