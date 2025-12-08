package net.runelite.client.plugins.bloodRuneTrue;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class BreakScheduler {

    private final Queue<Long> scheduledBreaks = new LinkedList<>();
    private final Random random = new Random();

    // Total session time (7 hours)
    private static final long TOTAL_SESSION_MS = 7 * 60 * 60 * 1000L;
    private static final long TARGET_INTERVAL_MS = 3 * 60 * 1000L; // every ~3 minutes

    // Micro break config
    private static final double MICRO_MEAN_MS = 10_000;
    private static final double MICRO_STDDEV_MS = 5_000;
    private static final long MICRO_MIN_MS = 5_000;
    private static final long MICRO_MAX_MS = 25_000;

    // Major break config
    private static final double MAJOR_MEAN_MS = 120_000;
    private static final double MAJOR_STDDEV_MS = 30_000;
    private static final long MAJOR_MIN_MS = 60_000;
    private static final long MAJOR_MAX_MS = 180_000;

    // Major-Major break config
    private static final double MAJOR_MAJOR_MEAN_MS = 300_000;
    private static final double MAJOR_MAJOR_STDDEV_MS = 60_000;
    private static final long MAJOR_MAJOR_MIN_MS = 180_000;
    private static final long MAJOR_MAJOR_MAX_MS = 420_000;

    public BreakScheduler() {
        generateBreakSchedule();
    }

    private void generateBreakSchedule() {
        long elapsed = 0;

        long nextMajorBreakAt = getRandomInterval(20, 40); // mins
        long nextMajorMajorBreakAt = 60 * 60_000L; // 1 hour mark

        while (elapsed < TOTAL_SESSION_MS) {
            if (elapsed >= nextMajorMajorBreakAt) {
                long majorMajorBreak = gaussianClamped(
                        MAJOR_MAJOR_MEAN_MS, MAJOR_MAJOR_STDDEV_MS,
                        MAJOR_MAJOR_MIN_MS, MAJOR_MAJOR_MAX_MS
                );
                scheduledBreaks.add(majorMajorBreak);
                nextMajorMajorBreakAt += 60 * 60_000L; // every ~1hr
            } else if (elapsed >= nextMajorBreakAt) {
                long majorBreak = gaussianClamped(
                        MAJOR_MEAN_MS, MAJOR_STDDEV_MS,
                        MAJOR_MIN_MS, MAJOR_MAX_MS
                );
                scheduledBreaks.add(majorBreak);
                nextMajorBreakAt += getRandomInterval(20, 40);
            } else {
                long microBreak = gaussianClamped(
                        MICRO_MEAN_MS, MICRO_STDDEV_MS,
                        MICRO_MIN_MS, MICRO_MAX_MS
                );
                scheduledBreaks.add(microBreak);
            }

            elapsed += TARGET_INTERVAL_MS;
        }
    }

    private long getRandomInterval(int minMinutes, int maxMinutes) {
        return minMinutes * 60_000L + random.nextInt((maxMinutes - minMinutes + 1) * 60_000);
    }

    private long gaussianClamped(double mean, double stddev, long min, long max) {
        long value = Math.round(mean + random.nextGaussian() * stddev);
        return Math.max(min, Math.min(max, value));
    }

    public int getNextBreakDuration() {
        Long duration = scheduledBreaks.poll();
        return duration != null ? duration.intValue() : 0; // or throw if null is unexpected
    }
}