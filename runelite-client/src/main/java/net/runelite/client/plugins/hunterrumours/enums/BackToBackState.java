package net.runelite.client.plugins.hunterrumours.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum BackToBackState {
    UNKNOWN("Unknown"),
    ENABLED("Enabled"),
    DISABLED("Disabled");

    @Getter
    private final String NiceName;
}
