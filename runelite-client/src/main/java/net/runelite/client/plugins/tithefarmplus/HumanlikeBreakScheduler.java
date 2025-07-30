package net.runelite.client.plugins.tithefarmplus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HumanlikeBreakScheduler {

    private static final long MIN_MAJOR_BREAK_INTERVAL_MS = 15 * 60_000; // 15 min
    private static final long MAX_MAJOR_BREAK_INTERVAL_MS = 40 * 60_000; // 40 min

    private static final long MIN_MAJOR_BREAK_LENGTH_MS = 20 * 1000; // 20 sec
    private static final long MAX_MAJOR_BREAK_LENGTH_MS = 120 * 1000; // 2 min

    private static final long MIN_MICRO_BREAK_LENGTH_MS = 3 * 1000; // 3 sec
    private static final long MAX_MICRO_BREAK_LENGTH_MS = 10 * 1000; // 10 sec

    private final long startTimeMillis;
    private final Random sessionRandom;
    private final List<BreakInterval> scheduledMajorBreaks = new ArrayList<>();

    public HumanlikeBreakScheduler(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
        this.sessionRandom = new Random(startTimeMillis);
    }

    public boolean shouldTakeBreak() {
        long now = System.currentTimeMillis();
        long elapsed = now - startTimeMillis;

        if (elapsed < 0) return false;

        // Schedule major breaks if empty or elapsed exceeds last break
        if (scheduledMajorBreaks.isEmpty() || elapsed > scheduledMajorBreaks.get(scheduledMajorBreaks.size() - 1).end) {
            scheduleMajorBreaksUpTo(elapsed + MAX_MAJOR_BREAK_INTERVAL_MS * 2);
        }

        // Check if current time is inside any major break
        for (BreakInterval brk : scheduledMajorBreaks) {
            if (elapsed >= brk.start && elapsed < brk.end) {
                return true;
            }
        }

        // Micro break chance (~1%)
        if (sessionRandom.nextDouble() < 0.01) {
            int microBreakLen = (int) (MIN_MICRO_BREAK_LENGTH_MS +
                    sessionRandom.nextDouble() * (MAX_MICRO_BREAK_LENGTH_MS - MIN_MICRO_BREAK_LENGTH_MS));
            try {
                Thread.sleep(microBreakLen);
            } catch (InterruptedException ignored) {}
            return true;
        }

        return false;
    }

    // Schedules major breaks progressively up to target time
    private void scheduleMajorBreaksUpTo(long targetElapsed) {
        long lastEnd = scheduledMajorBreaks.isEmpty() ? 0 : scheduledMajorBreaks.get(scheduledMajorBreaks.size() - 1).end;
        long current = lastEnd;

        while (current < targetElapsed) {
            long interval = MIN_MAJOR_BREAK_INTERVAL_MS +
                    (long) (sessionRandom.nextDouble() * (MAX_MAJOR_BREAK_INTERVAL_MS - MIN_MAJOR_BREAK_INTERVAL_MS));
            long breakStart = current + interval;

            // Jitter break start ±30 seconds for natural randomness
            breakStart += (long) ((sessionRandom.nextDouble() - 0.5) * 60_000);

            long breakLen = MIN_MAJOR_BREAK_LENGTH_MS +
                    (long) (sessionRandom.nextDouble() * (MAX_MAJOR_BREAK_LENGTH_MS - MIN_MAJOR_BREAK_LENGTH_MS));

            long breakEnd = breakStart + breakLen;

            if (breakStart > targetElapsed) break;

            scheduledMajorBreaks.add(new BreakInterval(breakStart, breakEnd));
            current = breakEnd;
        }
    }

    private static class BreakInterval {
        final long start;
        final long end;

        BreakInterval(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }
}