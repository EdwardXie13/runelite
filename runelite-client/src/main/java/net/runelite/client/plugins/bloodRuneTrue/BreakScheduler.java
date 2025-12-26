package net.runelite.client.plugins.bloodRuneTrue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class BreakScheduler {

    private static class Break {
        final long offsetMs;    // when break should occur (session time)
        final long durationMs;  // break length

        Break(long offsetMs, long durationMs) {
            this.offsetMs = offsetMs;
            this.durationMs = durationMs;
        }
    }

    /* ===================== */
    /* ===== CONFIG ======= */
    /* ===================== */

    private static final int TOTAL_HOURS = 7;
    private static final long HOUR_MS = 60 * 60 * 1000L;

    // Micro break duration (10–25s)
    private static final long MICRO_MIN_MS = 10_000;
    private static final long MICRO_MAX_MS = 25_000;

    // Micro timing (20–40 min, centered at 30)
    private static final double MICRO_TIME_MEAN_MS = 30 * 60_000.0;
    private static final double MICRO_TIME_STDDEV_MS = 5 * 60_000.0;
    private static final long MICRO_TIME_MIN_MS = 20 * 60_000L;
    private static final long MICRO_TIME_MAX_MS = 40 * 60_000L;

    // Major duration (2–3 min, centered at 2.5)
    private static final double MAJOR_MEAN_MS = 2.5 * 60_000.0;
    private static final double MAJOR_STDDEV_MS = 15_000.0;
    private static final long MAJOR_MIN_MS = 2 * 60_000L;
    private static final long MAJOR_MAX_MS = 3 * 60_000L;

    /* ===================== */
    /* ===== STATE ======== */
    /* ===================== */

    private final Queue<Break> scheduledBreaks = new LinkedList<>();
    private final Random random = new Random();
    private final long sessionStartMs;

    /* ===================== */
    /* ===== SETUP ======== */
    /* ===================== */

    public BreakScheduler() {
        this.sessionStartMs = System.currentTimeMillis();
        generateSchedule();
    }

    /* ===================== */
    /* ===== PUBLIC API ==== */
    /* ===================== */

    /**
     * Call every tick.
     * @return break duration in ms, or 0 if no break now
     */
    public long isBreak(long nowMs) {
        Break next = scheduledBreaks.peek();
        if (next == null) {
            return 0;
        }

        long elapsedMs = nowMs - sessionStartMs;

        // Not time yet
        if (elapsedMs < next.offsetMs) {
            return 0;
        }

        // Break is due (or overdue)
        scheduledBreaks.poll();
        return next.durationMs;
    }

    /* ===================== */
    /* ===== INTERNAL ===== */
    /* ===================== */

    private void generateSchedule() {
        for (int hour = 0; hour < TOTAL_HOURS; hour++) {
            long hourStart = hour * HOUR_MS;

            // Micro break
            long microOffset = hourStart + gaussianClamped(
                    MICRO_TIME_MEAN_MS,
                    MICRO_TIME_STDDEV_MS,
                    MICRO_TIME_MIN_MS,
                    MICRO_TIME_MAX_MS
            );
            long microDuration = randomBetween(MICRO_MIN_MS, MICRO_MAX_MS);
            scheduledBreaks.add(new Break(microOffset, microDuration));

            // Major break (hour mark)
            long majorOffset = hourStart + HOUR_MS;
            long majorDuration = gaussianClamped(
                    MAJOR_MEAN_MS,
                    MAJOR_STDDEV_MS,
                    MAJOR_MIN_MS,
                    MAJOR_MAX_MS
            );
            scheduledBreaks.add(new Break(majorOffset, majorDuration));
        }
    }

    private long gaussianClamped(double mean, double stddev, long min, long max) {
        long value = Math.round(mean + random.nextGaussian() * stddev);
        return Math.max(min, Math.min(max, value));
    }

    private long randomBetween(long min, long max) {
        return min + (long) (random.nextDouble() * (max - min));
    }
}