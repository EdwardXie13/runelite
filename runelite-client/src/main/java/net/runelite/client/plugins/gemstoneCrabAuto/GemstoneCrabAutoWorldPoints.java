package net.runelite.client.plugins.gemstoneCrabAuto;

import lombok.experimental.UtilityClass;
import net.runelite.api.coords.WorldPoint;

/**
 * Region IDs, arrival tiles, and per-arrival pre-walk targets for the
 * Gemstone Crab bot.
 */
@UtilityClass
public class GemstoneCrabAutoWorldPoints {
    /** Region IDs the crab can spawn in. Any player-region match against this
     *  set means we're at the fight (same region set both pre- and post-cave). */
    public static final int[] GEMSTONE_CRAB_REGIONS = { 4911, 4913, 5424 };

    /** Ticks to wait after landing on an arrival tile before firing the
     *  pre-walk click. Same value for all three arrivals — lets cave-exit
     *  animation / camera settle before we move. Tune here to change all
     *  three at once. */
    public static final int PRE_WALK_DELAY_TICKS = 2;

    /** (arrival tile, pre-walk destination) pair. Delay is uniform across
     *  arrivals — see PRE_WALK_DELAY_TICKS above. */
    public static final class Arrival {
        public final WorldPoint arrival;
        public final WorldPoint preWalk;
        public Arrival(WorldPoint a, WorldPoint p) {
            this.arrival = a;
            this.preWalk = p;
        }
    }

    public static final Arrival[] ARRIVALS = {
        new Arrival(new WorldPoint(1352, 3122, 0), new WorldPoint(1352, 3115, 0)),
        new Arrival(new WorldPoint(1277, 3168, 0), new WorldPoint(1275, 3170, 0)),
        new Arrival(new WorldPoint(1246, 3038, 0), new WorldPoint(1243, 3041, 0)),
    };

    /** Return the Arrival entry matching this template WorldPoint, or null.
     *  Callers should pass toTemplate(player.getWorldLocation()) so the match
     *  works regardless of instance. */
    public static Arrival matchArrival(WorldPoint at) {
        if (at == null) return null;
        for (Arrival ar : ARRIVALS) {
            if (ar.arrival.getX() == at.getX()
                    && ar.arrival.getY() == at.getY()
                    && ar.arrival.getPlane() == at.getPlane()) {
                return ar;
            }
        }
        return null;
    }
}
