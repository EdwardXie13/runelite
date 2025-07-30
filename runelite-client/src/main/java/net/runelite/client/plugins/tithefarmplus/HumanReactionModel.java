package net.runelite.client.plugins.tithefarmplus;

import java.util.Random;

public class HumanReactionModel {

    private static final Random random = new Random();

    /**
     * Get a simulated human reaction delay in ms.
     *
     * @param min       Minimum base reaction delay (ms)
     * @param max       Maximum base reaction delay (ms)
     * @param startTime Session start time in epoch millis
     * @param drift     Whether to apply human attention drift & phases
     * @return reaction delay in ms
     */
    public static int getHumanReaction(int min, int max, long startTime, boolean drift) {
        if (min >= max) throw new IllegalArgumentException("min must be less than max");

        long now = System.currentTimeMillis();
        double elapsedMinutes = (now - startTime) / 60000.0;

        // Base mean and stdDev
        double baseMean = (min + max) / 2.0;
        double baseStdDev = (max - min) / 6.0;

        double mean = baseMean;
        double stdDev = baseStdDev;

        if (drift) {
            // Phase durations in minutes (can adjust)
            double phase1 = 20;  // fresh
            double phase2 = 40;  // tired
            double phase3 = 20;  // rebound
            double phase4 = 60;  // burnt out
            double totalPhases = phase1 + phase2 + phase3 + phase4;

            // Normalize elapsed into phases looped infinitely
            double cycleMinutes = elapsedMinutes % totalPhases;

            // Phase-specific base mean and stdDev modifiers
            if (cycleMinutes < phase1) {
                // Fresh: fast, low variability
                mean += -15;                       // 15ms faster than base
                stdDev += 5;
                mean += sineFluctuation(cycleMinutes, phase1, 5);
            } else if (cycleMinutes < phase1 + phase2) {
                // Tired: slower, higher variability
                mean += 30;                       // 30ms slower than base
                stdDev += 15;
                mean += sineFluctuation(cycleMinutes - phase1, phase2, 10);
            } else if (cycleMinutes < phase1 + phase2 + phase3) {
                // Rebound: improve focus temporarily
                mean += 5;
                stdDev += 8;
                mean += sineFluctuation(cycleMinutes - phase1 - phase2, phase3, 7);
            } else {
                // Burnt out: slowest, highest variability
                mean += 40;
                stdDev += 20;
                mean += sineFluctuation(cycleMinutes - phase1 - phase2 - phase3, phase4, 15);
            }

            // Add a slow linear fatigue drift over total time (capped)
            double fatigueDrift = Math.min(elapsedMinutes * 0.3, 30);
            mean += fatigueDrift;
        }

        // Gaussian random sampling (Box-Muller)
        double u = 1.0 - random.nextDouble();
        double v = random.nextDouble();
        double z = Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);

        double delay = mean + z * stdDev;

        // Clamp delay within reasonable range (with some overshoot)
        int minClamp = min - 50;
        int maxClamp = max + 100;
        delay = Math.max(minClamp, Math.min(maxClamp, delay));

        // Occasionally simulate distraction with big delay (1%)
        if (drift && random.nextDouble() < 0.01) {
            delay += random.nextInt(600) + 400; // +400 to +1000 ms delay
        }

        return (int) Math.round(delay);
    }

    // Sine fluctuation helper for smooth oscillation in each phase
    private static double sineFluctuation(double elapsedInPhase, double phaseLength, double amplitude) {
        double phasePosition = elapsedInPhase / phaseLength;
        return Math.sin(2 * Math.PI * phasePosition) * amplitude;
    }
}
