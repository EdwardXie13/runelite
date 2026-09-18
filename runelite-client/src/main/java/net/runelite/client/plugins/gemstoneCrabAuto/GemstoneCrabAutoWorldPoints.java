package net.runelite.client.plugins.gemstoneCrabAuto;

import lombok.experimental.UtilityClass;
import net.runelite.api.coords.WorldPoint;

/**
 * Region IDs + anchor tiles for the Gemstone Crab bot.
 */
@UtilityClass
public class GemstoneCrabAutoWorldPoints {
    /** Region IDs the crab can spawn in. Any player-region match against this
     *  set means we're at the fight. */
    public static final int[] GEMSTONE_CRAB_REGIONS = { 4911, 4913, 5424 };
}
