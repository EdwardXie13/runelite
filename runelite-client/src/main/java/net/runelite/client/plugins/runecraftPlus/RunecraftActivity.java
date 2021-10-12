package net.runelite.client.plugins.runecraftPlus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
enum RunecraftActivity {
    IDLE("IDLE"),
    RUNNING("RUNNING"),
    MINING("MINING"),
    CHISEL("CHISEL"),
    IMBUE("IMBUE"),
    ROCK("ROCK");

    private final String actionString;
}
