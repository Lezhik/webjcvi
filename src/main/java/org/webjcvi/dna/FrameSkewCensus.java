package org.webjcvi.dna;

import java.util.Locale;
import java.util.Objects;

/**
 * Chargaff skew in wrap-sized windows. Global A≈T can hide local imbalance;
 * this census asks whether pairing holds inside each frame of modal width.
 */
public final class FrameSkewCensus {

    public static final double LOCAL_THRESHOLD = 0.05;

    private final int window;
    private final int windowCount;
    private final double meanAbsAtSkew;
    private final double meanAbsGcSkew;
    private final double maxAbsAtSkew;
    private final double maxAbsGcSkew;
    private final int windowsPastThreshold;

    private FrameSkewCensus(
            int window,
            int windowCount,
            double meanAbsAtSkew,
            double meanAbsGcSkew,
            double maxAbsAtSkew,
            double maxAbsGcSkew,
            int windowsPastThreshold) {
        this.window = window;
        this.windowCount = windowCount;
        this.meanAbsAtSkew = meanAbsAtSkew;
        this.meanAbsGcSkew = meanAbsGcSkew;
        this.maxAbsAtSkew = maxAbsAtSkew;
        this.maxAbsGcSkew = maxAbsGcSkew;
        this.windowsPastThreshold = windowsPastThreshold;
    }

    public static FrameSkewCensus from(DnaSequence sequence, int window) {
        Objects.requireNonNull(sequence, "sequence");
        int width = window <= 0 ? 70 : window;
        String bases = sequence.normalized();
        if (bases.isEmpty()) {
            return new FrameSkewCensus(width, 0, 0, 0, 0, 0, 0);
        }
        double atAbsSum = 0;
        double gcAbsSum = 0;
        double maxAt = 0;
        double maxGc = 0;
        int past = 0;
        int count = 0;
        for (int origin = 0; origin < bases.length(); origin += width) {
            int end = Math.min(bases.length(), origin + width);
            long a = 0;
            long t = 0;
            long g = 0;
            long c = 0;
            for (int i = origin; i < end; i++) {
                switch (bases.charAt(i)) {
                    case 'A' -> a++;
                    case 'T' -> t++;
                    case 'G' -> g++;
                    case 'C' -> c++;
                    default -> {
                    }
                }
            }
            double atSkew = skew(a, t);
            double gcSkew = skew(g, c);
            atAbsSum += Math.abs(atSkew);
            gcAbsSum += Math.abs(gcSkew);
            maxAt = Math.max(maxAt, Math.abs(atSkew));
            maxGc = Math.max(maxGc, Math.abs(gcSkew));
            if (Math.abs(atSkew) > LOCAL_THRESHOLD) {
                past++;
            }
            count++;
        }
        double meanAt = count == 0 ? 0.0 : atAbsSum / count;
        double meanGc = count == 0 ? 0.0 : gcAbsSum / count;
        return new FrameSkewCensus(width, count, meanAt, meanGc, maxAt, maxGc, past);
    }

    private static double skew(long left, long right) {
        long sum = left + right;
        if (sum == 0) {
            return 0.0;
        }
        return (left - right) / (double) sum;
    }

    public int window() {
        return window;
    }

    public int windowCount() {
        return windowCount;
    }

    public double meanAbsAtSkew() {
        return meanAbsAtSkew;
    }

    public double meanAbsGcSkew() {
        return meanAbsGcSkew;
    }

    public double maxAbsAtSkew() {
        return maxAbsAtSkew;
    }

    public double maxAbsGcSkew() {
        return maxAbsGcSkew;
    }

    public int windowsPastThreshold() {
        return windowsPastThreshold;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "window=%d count=%d mean|AT|=%.4f max|AT|=%.4f past=%.0f%% mean|GC|=%.4f",
                window, windowCount, meanAbsAtSkew, maxAbsAtSkew,
                windowCount == 0 ? 0.0 : 100.0 * windowsPastThreshold / windowCount,
                meanAbsGcSkew);
    }
}
