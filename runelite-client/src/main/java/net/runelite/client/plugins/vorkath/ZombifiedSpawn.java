package net.runelite.client.plugins.vorkath;

import lombok.Getter;
import net.runelite.api.NPC;

public class ZombifiedSpawn {
    @Getter
    private NPC npc;

    ZombifiedSpawn(NPC npc)
    {
        this.npc = npc;
    }
}