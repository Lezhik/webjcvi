package org.webjcvi.rise;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Steepest unique-share rise of newline-delimited rows. v25 DNA uniqueness
 * cliffs at k=12 (share 0.9943) with overhang 7 before residual twins fork at
 * 19. Iteration 24's layout-runway then found log journals with cliffAt=0 —
 * uniqueness never reaches 0.99, so runway is a void. This scan keeps newlines
 * and reports <em>riseAt</em> (k maximizing uniqueShare(k) − uniqueShare(k−1)),
 * which exists even when there is no 0.99 cliff. Inverse of runway-scan (there
 * a missing cliff made the product empty). DNA uniqueAt=20 / tile 16 mark a
 * clock-side rise.
 */
public final class RiseScan {

    public static final int TILE = 16;
    public static final int FLOOR = 8;
    public static final int MAX_K = 256;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;

    private static final Logger log = LoggerFactory.getLogger(RiseScan.class);

    public Scan profile(String text) {
        if (text == null) {
            throw new RiseException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new RiseException("Text exceeds " + MAX_CHARS + " characters");
        }
        List<String> lines = splitLines(text);
        int lineCount = lines.size();
        List<String> rows = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            rows.add(line.toUpperCase(Locale.ROOT));
        }
        int scanned = rows.size();
        double shareAt16 = 0.0;
        int riseAt = 0;
        double gain = 0.0;
        double shareAtRise = 0.0;
        int cliffAt = 0;
        String topPrefix = "";
        int topCount = 0;
        if (scanned > 0) {
            shareAt16 = uniqueShare(rows, TILE);
            Map<String, Integer> groups = new LinkedHashMap<>();
            for (String row : rows) {
                groups.merge(prefix(row, TILE), 1, Integer::sum);
            }
            for (var entry : groups.entrySet()) {
                if (entry.getValue() > topCount) {
                    topCount = entry.getValue();
                    topPrefix = entry.getKey();
                }
            }
            int maxLen = 0;
            for (String row : rows) {
                if (row.length() > maxLen) {
                    maxLen = row.length();
                }
            }
            int cap = Math.min(Math.max(maxLen, FLOOR), MAX_K);
            double previous = 0.0;
            for (int k = FLOOR; k <= cap; k++) {
                double share = uniqueShare(rows, k);
                if (cliffAt == 0 && share >= 0.99) {
                    cliffAt = k;
                }
                double step = share - previous;
                if (step > gain) {
                    gain = step;
                    riseAt = k;
                    shareAtRise = share;
                }
                previous = share;
            }
            if (gain <= 0.0) {
                riseAt = 0;
                shareAtRise = 0.0;
            }
        }
        boolean pastClock = riseAt > TILE;
        Scan scan = new Scan(
                lineCount, scanned, FLOOR, riseAt, gain, shareAtRise, shareAt16, cliffAt,
                pastClock, topPrefix, topCount);
        if (log.isDebugEnabled()) {
            log.debug("rise.profile chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == '\n') {
                String line = text.substring(start, i);
                if (line.endsWith("\r")) {
                    line = line.substring(0, line.length() - 1);
                }
                if (!(i == text.length() && line.isEmpty() && !lines.isEmpty())) {
                    lines.add(line);
                }
                start = i + 1;
            }
        }
        return lines;
    }

    private static double uniqueShare(List<String> rows, int k) {
        int scanned = rows.size();
        if (scanned == 0) {
            return 0.0;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String row : rows) {
            counts.merge(prefix(row, k), 1, Integer::sum);
        }
        return counts.size() * 1.0 / scanned;
    }

    private static String prefix(String row, int k) {
        if (row.length() <= k) {
            return row;
        }
        return row.substring(0, k);
    }

    public record Scan(
            int lineCount,
            int scanned,
            int floorLength,
            int riseAt,
            double gain,
            double shareAtRise,
            double shareAt16,
            int cliffAt,
            boolean pastClock,
            String topPrefix,
            int topCount) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "lines=%d scanned=%d floor=%d riseAt=%d gain=%.4f shareRise=%.4f share16=%.4f cliffAt=%d pastClock=%s topCount=%d",
                    lineCount, scanned, floorLength, riseAt, gain, shareAtRise, shareAt16, cliffAt,
                    pastClock, topCount);
        }
    }
}
