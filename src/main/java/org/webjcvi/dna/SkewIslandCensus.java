package org.webjcvi.dna;

import java.util.Objects;

/**
 * Geography of local Chargaff failure: consecutive wrap-sized windows whose
 * |AT-skew| is above or below the v3 threshold, compressed into islands.
 * Aggregate mean/max (v3) cannot tell whether drifted frames cluster.
 */
public final class SkewIslandCensus {

    private final int window;
    private final int windowCount;
    private final int failWindows;
    private final int failIslands;
    private final int passIslands;
    private final int longestFail;
    private final int longestPass;

    private SkewIslandCensus(
            int window,
            int windowCount,
            int failWindows,
            int failIslands,
            int passIslands,
            int longestFail,
            int longestPass) {
        this.window = window;
        this.windowCount = windowCount;
        this.failWindows = failWindows;
        this.failIslands = failIslands;
        this.passIslands = passIslands;
        this.longestFail = longestFail;
        this.longestPass = longestPass;
    }

    public static SkewIslandCensus from(DnaSequence sequence, int window) {
        Objects.requireNonNull(sequence, "sequence");
        int width = window <= 0 ? 70 : window;
        String bases = sequence.normalized();
        if (bases.isEmpty()) {
            return new SkewIslandCensus(width, 0, 0, 0, 0, 0, 0);
        }
        int windows = 0;
        int failWindows = 0;
        int failIslands = 0;
        int passIslands = 0;
        int longestFail = 0;
        int longestPass = 0;
        int runLen = 0;
        Boolean runFail = null;
        for (int origin = 0; origin < bases.length(); origin += width) {
            int end = Math.min(bases.length(), origin + width);
            long a = 0;
            long t = 0;
            for (int i = origin; i < end; i++) {
                char ch = bases.charAt(i);
                if (ch == 'A') {
                    a++;
                } else if (ch == 'T') {
                    t++;
                }
            }
            boolean fail = Math.abs(skew(a, t)) > FrameSkewCensus.LOCAL_THRESHOLD;
            if (runFail == null) {
                runFail = fail;
                runLen = 1;
            } else if (runFail == fail) {
                runLen++;
            } else {
                if (runFail) {
                    failIslands++;
                    longestFail = Math.max(longestFail, runLen);
                } else {
                    passIslands++;
                    longestPass = Math.max(longestPass, runLen);
                }
                runFail = fail;
                runLen = 1;
            }
            if (fail) {
                failWindows++;
            }
            windows++;
        }
        if (runFail != null) {
            if (runFail) {
                failIslands++;
                longestFail = Math.max(longestFail, runLen);
            } else {
                passIslands++;
                longestPass = Math.max(longestPass, runLen);
            }
        }
        return new SkewIslandCensus(
                width, windows, failWindows, failIslands, passIslands, longestFail, longestPass);
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

    public int failWindows() {
        return failWindows;
    }

    public int failIslands() {
        return failIslands;
    }

    public int passIslands() {
        return passIslands;
    }

    public int longestFail() {
        return longestFail;
    }

    public int longestPass() {
        return longestPass;
    }
}
