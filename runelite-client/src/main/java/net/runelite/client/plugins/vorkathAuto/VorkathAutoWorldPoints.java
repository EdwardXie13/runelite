package net.runelite.client.plugins.vorkathAuto;

import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class VorkathAutoWorldPoints {
    public static WorldPoint INFRONT_OF_POOL;
    public static final WorldPoint LUNAR_ISLE_BANK_INFRONT = new WorldPoint(2099, 3919, 0);
    public static final WorldPoint LUNAR_ISLE_BANK_BOOTH_TILE = new WorldPoint(2099, 3920, 0);
    public static final WorldPoint SIRSAL_BANKER_TILE = new WorldPoint(2098, 3921, 0);
    public static final WorldPoint LUNAR_ISLE_EXILE_TILE = new WorldPoint(2630, 3678, 0);
    public static final WorldPoint TORFINN_RELLEKA_TILE = new WorldPoint(2641, 3697, 0);
    public static final WorldPoint TORFINN_UNGAEL_TILE = new WorldPoint(2277, 4034, 0);
    public static final WorldPoint BEFORE_VORKATH_ICE_CHUNKS_TILE = new WorldPoint(2272, 4052, 0);
    public static final WorldPoint AFTER_VORKATH_ICE_CHUNKS_TILE = new WorldPoint(2272, 4054, 0);

    // 2269 < X < 2275
    public static final WorldPoint INFRONT_OF_VORKATH_CENTER_TILE = new WorldPoint(2272, 4061, 0);
    public static final WorldPoint FIVE_TICK_WOOX_WALK_CENTER_TILE = new WorldPoint(2272, 4058, 0);

    public static final List<WorldPoint> LUNAR_ISLE_TELEPORT_ZONE =
            Collections.unmodifiableList(new ArrayList<>() {{
                for (int x = 2107; x <= 2111; x++)
                    for (int y = 3913; y <= 3917; y++)
                        add(new WorldPoint(x, y, 0));
            }});

    public static final List<WorldPoint> INFRONT_OF_VORKATH_ROW =
            Collections.unmodifiableList(new ArrayList<>() {{
                for (int x = 2269; x <= 2275; x++)
                        add(new WorldPoint(x, 4061, 0));
            }});

    public static final List<WorldPoint> FOUR_TICK_WOOX_WALK_ROW =
            Collections.unmodifiableList(new ArrayList<>() {{
                for (int x = 2269; x <= 2275; x++)
                    add(new WorldPoint(x, 4059, 0));
            }});

    public static final List<WorldPoint> FIVE_TICK_WOOX_WALK_ROW =
            Collections.unmodifiableList(new ArrayList<>() {{
                for (int x = 2269; x <= 2275; x++)
                        add(new WorldPoint(x, 4058, 0));
            }});

    /** FANG standing area — 4 rows × 7 cols. South safe row is 4058, so acid on
     *  any row 4058..4061 matters. */
    public static final List<WorldPoint> VORKATH_STANDING_AREA_FANG =
            Collections.unmodifiableList(new ArrayList<>() {{
                for (int x = 2269; x <= 2275; x++)
                    for (int y = 4061; y >= 4058; y--)   // 4 rows: 4061, 4060, 4059, 4058
                        add(new WorldPoint(x, y, 0));
            }});

    /** LANCE standing area — 3 rows × 7 cols. South safe row is 4059, so row 4058
     *  is south of the walk envelope and any acid there is irrelevant to column
     *  cleanliness. Fewer rows to scan → fewer false positives from acid outside
     *  the walk area. */
    public static final List<WorldPoint> VORKATH_STANDING_AREA_LANCE =
            Collections.unmodifiableList(new ArrayList<>() {{
                for (int x = 2269; x <= 2275; x++)
                    for (int y = 4061; y >= 4059; y--)   // 3 rows: 4061, 4060, 4059
                        add(new WorldPoint(x, y, 0));
            }});

    /** Back-compat alias — defaults to FANG (widest area). New call sites should
     *  pick the weapon-specific variant instead. */
    public static final List<WorldPoint> VORKATH_STANDING_AREA = VORKATH_STANDING_AREA_FANG;

    public static int bestColumnForWooxWalk(
            List<WorldPoint> standingArea,
            List<WorldPoint> acidTiles,
            WorldPoint player
    ) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (WorldPoint p : standingArea) {
            if (p.getX() < minX) minX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getY() > maxY) maxY = p.getY();
        }

        // Bit c is set iff column (minX + c) has any acid on it, INSIDE the strip.
        // Acid tiles outside the x-range or y-range are ignored so they can't
        // falsely poison a column that is actually walkable inside the standing area.
        int dirty = 0;
        for (WorldPoint p : acidTiles) {
            int c = p.getX() - minX;
            if (c < 0 || c >= 7) continue;
            if (p.getY() < minY || p.getY() > maxY) continue;
            dirty |= 1 << c;
        }
        int clean = (~dirty) & 0x7F;   // guaranteed non-zero by the invariant

        // Pick the clean column closest to the CENTER of the standing area,
        // tie-break by closeness to the player. Human-like behavior: acid
        // patterns force sidesteps anyway, so we may as well drift toward
        // the middle each cycle rather than sitting on a side column just
        // because it happened to be clean this iteration.
        //
        // Scoring (both minimized):
        //   distFromCenter * 10  — primary; pulls toward middle of the 7-wide strip
        //   distFromPlayer * 1   — tie-break; if two cols are equally central,
        //                          pick the one requiring less movement. Also
        //                          naturally keeps us in place when we're already
        //                          on the center column and it's clean.
        int px = player.getX();
        int centerX = minX + 3;   // middle of the 7-wide standing strip
        int bestX = -1, bestScore = Integer.MAX_VALUE;
        for (int c = 0; c < 7; c++) {
            if ((clean & (1 << c)) == 0) continue;
            int x = minX + c;
            int distFromCenter = Math.abs(x - centerX);
            int distFromPlayer = Math.abs(x - px);
            int score = distFromCenter * 10 + distFromPlayer;
            if (score < bestScore) { bestScore = score; bestX = x; }
        }
        return bestX;
    }

    /**
     * Fallback for the rare case bestColumnForWooxWalk() returns -1 (every
     * column has acid somewhere in the standing area). Finds the LONGEST
     * contiguous clean run of tiles in any single row (west→east). Minimum
     * qualifying length is 3 so the oscillation has room. Tie-break: prefer
     * the north-most row (closer to Vorkath / attack row).
     *
     * Returns {leftEndpoint, rightEndpoint} — the two tiles at the ends of
     * the run. Oscillate between them; distance = runLength - 1 tiles.
     * Returns null if no row has a run of length >= 3 (fully boxed in — a
     * separate, much rarer failure mode).
     */
    public static WorldPoint[] longestCleanRowForWooxWalk(
            List<WorldPoint> standingArea,
            List<WorldPoint> acidTiles
    ) {
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (WorldPoint p : standingArea) {
            if (p.getX() < minX) minX = p.getX();
            if (p.getX() > maxX) maxX = p.getX();
            if (p.getY() < minY) minY = p.getY();
            if (p.getY() > maxY) maxY = p.getY();
        }

        // Acid membership: pack (x,y) into a long for O(1) lookup.
        java.util.Set<Long> acidSet = new java.util.HashSet<>();
        for (WorldPoint a : acidTiles) {
            acidSet.add(((long) a.getX() << 20) | (a.getY() & 0xFFFFFL));
        }

        int bestLen = 0;
        int bestY = -1, bestStart = -1, bestEnd = -1;

        // Scan north → south (maxY down to minY). Since we only update on
        // strict `>`, a tie leaves the north-most row selected — that's the
        // tie-break rule.
        for (int y = maxY; y >= minY; y--) {
            int runStart = -1;
            // x <= maxX+1 sentinel: an extra iteration past the east edge
            // triggers end-of-run handling for a run that reaches maxX.
            for (int x = minX; x <= maxX + 1; x++) {
                boolean atEdge = (x > maxX);
                boolean isAcid = !atEdge && acidSet.contains(((long) x << 20) | (y & 0xFFFFFL));
                if (atEdge || isAcid) {
                    if (runStart >= 0) {
                        int runEnd = x - 1;
                        int runLen = runEnd - runStart + 1;
                        if (runLen >= 3 && runLen > bestLen) {
                            bestLen = runLen;
                            bestY = y;
                            bestStart = runStart;
                            bestEnd = runEnd;
                        }
                        runStart = -1;
                    }
                } else {
                    if (runStart == -1) runStart = x;
                }
            }
        }

        if (bestLen < 3) return null;   // fully boxed in — caller decides what to do
        return new WorldPoint[] {
            new WorldPoint(bestStart, bestY, 0),
            new WorldPoint(bestEnd,   bestY, 0)
        };
    }
}


