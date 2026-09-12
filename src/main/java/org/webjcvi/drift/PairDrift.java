package org.webjcvi.drift;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.webjcvi.tape.ScratchTape;

/**
 * Walks caller-supplied text in wrap-sized windows and flags local imbalance
 * of complementary bracket pairs. The v3 DNA report showed global A≈T
 * (residual ~0.009) while 80% of 70-base frames had |AT-skew| &gt; 0.05 —
 * pairing is an accounting identity; the payload is the drifted windows.
 */
public final class PairDrift {

    public static final int DEFAULT_WINDOW = 70;
    public static final double DEFAULT_THRESHOLD = 0.05;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_HOTSPOTS = 80;
    public static final int PREVIEW_CHARS = 80;

    private static final String OPENS = "([{<";
    private static final String CLOSES = ")]}>";

    public Scan scan(String text) {
        return scan(text, DEFAULT_WINDOW, DEFAULT_THRESHOLD);
    }

    public Scan scan(String text, int window, double threshold) {
        if (text == null) {
            throw new DriftException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new DriftException("Text exceeds " + MAX_CHARS + " characters");
        }
        int width = window <= 0 ? DEFAULT_WINDOW : window;
        if (width < 2) {
            throw new DriftException("window must be at least 2");
        }
        if (threshold < 0 || threshold >= 1) {
            throw new DriftException("threshold must be in [0, 1)");
        }
        if (text.isEmpty()) {
            return new Scan(width, 0, 0, 0, 0, 0.0, List.of());
        }
        int opensTotal = 0;
        int closesTotal = 0;
        List<Hotspot> hotspots = new ArrayList<>();
        int windows = 0;
        for (int origin = 0; origin < text.length(); origin += width) {
            int end = Math.min(text.length(), origin + width);
            int opens = 0;
            int closes = 0;
            for (int i = origin; i < end; i++) {
                char ch = text.charAt(i);
                if (OPENS.indexOf(ch) >= 0) {
                    opens++;
                } else if (CLOSES.indexOf(ch) >= 0) {
                    closes++;
                }
            }
            opensTotal += opens;
            closesTotal += closes;
            double skew = skew(opens, closes);
            if (opens + closes > 0 && Math.abs(skew) > threshold && hotspots.size() < MAX_HOTSPOTS) {
                String body = text.substring(origin, end);
                hotspots.add(new Hotspot(
                        hotspots.size(), origin, end - origin, opens, closes, skew, preview(body)));
            }
            windows++;
        }
        return new Scan(
                width,
                windows,
                hotspots.size(),
                opensTotal,
                closesTotal,
                skew(opensTotal, closesTotal),
                List.copyOf(hotspots));
    }

    static double skew(int left, int right) {
        int sum = left + right;
        if (sum == 0) {
            return 0.0;
        }
        return (left - right) / (double) sum;
    }

    private static String preview(String body) {
        String trimmed = body.replace('\n', ' ').replace('\r', ' ').trim();
        if (trimmed.length() <= PREVIEW_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, PREVIEW_CHARS) + "…";
    }

    public record Scan(
            int window,
            int windowCount,
            int hotspotCount,
            int opens,
            int closes,
            double globalSkew,
            List<Hotspot> hotspots) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "windows=%d hotspots=%d opens=%d closes=%d globalSkew=%.4f",
                    windowCount, hotspotCount, opens, closes, globalSkew);
        }
    }

    public record Hotspot(
            int index, int offset, int length, int opens, int closes, double skew, String preview) {

        public String skewText() {
            return String.format(Locale.ROOT, "%.3f", skew);
        }
    }
}
