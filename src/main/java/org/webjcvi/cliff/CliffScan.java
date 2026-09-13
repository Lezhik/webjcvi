package org.webjcvi.cliff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Clock-cliff of newline-delimited rows. v23 line keys saturate at uniqueAt=20
 * with a jump k=8 share 0.7191 → k=16 0.9997. Iteration 22's row scan then
 * reported uniqueAt=0 on Logback journals: a 16-character veto (the clock)
 * without naming the column where lines fork. This scan keeps newlines and
 * reports <em>forkAt</em> (1-based first difference inside the largest
 * colliding 16-prefix) and <em>cliffAt</em> (smallest k≥8 with unique share
 * ≥ 0.99). Inverse of row-scan (there uniqueShareAt16 was the headline) and
 * of wrap-frame fork-scan (there newlines are dropped).
 */
public final class CliffScan {

    public static final int TILE = 16;
    public static final int FLOOR = 8;
    public static final int MAX_K = 256;
    public static final double CLIFF_SHARE = 0.99;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;

    private static final Logger log = LoggerFactory.getLogger(CliffScan.class);

    public Scan profile(String text) {
        if (text == null) {
            throw new CliffException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new CliffException("Text exceeds " + MAX_CHARS + " characters");
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
        int cliffAt = 0;
        double shareAtCliff = 0.0;
        int forkAt = 0;
        String topPrefix = "";
        int topCount = 0;
        if (scanned > 0) {
            shareAt16 = uniqueShare(rows, TILE);
            Map<String, List<String>> groups = new LinkedHashMap<>();
            for (String row : rows) {
                groups.computeIfAbsent(prefix(row, TILE), key -> new ArrayList<>()).add(row);
            }
            List<String> topMembers = List.of();
            for (var entry : groups.entrySet()) {
                int count = entry.getValue().size();
                if (count > topCount) {
                    topCount = count;
                    topPrefix = entry.getKey();
                    topMembers = entry.getValue();
                }
            }
            if (topCount >= 2) {
                forkAt = forkAt(topMembers);
            }
            int maxLen = 0;
            for (String row : rows) {
                if (row.length() > maxLen) {
                    maxLen = row.length();
                }
            }
            int cap = Math.min(Math.max(maxLen, FLOOR), MAX_K);
            for (int k = FLOOR; k <= cap; k++) {
                double share = uniqueShare(rows, k);
                if (share >= CLIFF_SHARE) {
                    cliffAt = k;
                    shareAtCliff = share;
                    break;
                }
            }
        }
        Scan scan = new Scan(
                lineCount, scanned, TILE, forkAt, cliffAt, shareAt16, shareAtCliff, topPrefix, topCount);
        if (log.isDebugEnabled()) {
            log.debug("cliff.profile chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    static int forkAt(List<String> members) {
        if (members.size() < 2) {
            return 0;
        }
        int maxLen = 0;
        for (String member : members) {
            if (member.length() > maxLen) {
                maxLen = member.length();
            }
        }
        for (int col = 0; col < maxLen; col++) {
            Character expected = null;
            for (String member : members) {
                Character ch = col < member.length() ? member.charAt(col) : null;
                if (expected == null) {
                    expected = ch;
                } else if (!Objects.equals(ch, expected)) {
                    return col + 1;
                }
            }
        }
        return 0;
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
            int tileLength,
            int forkAt,
            int cliffAt,
            double shareAt16,
            double shareAtCliff,
            String topPrefix,
            int topCount) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "lines=%d scanned=%d tile=%d forkAt=%d cliffAt=%d share16=%.4f cliff=%.4f topCount=%d",
                    lineCount, scanned, tileLength, forkAt, cliffAt, shareAt16, shareAtCliff, topCount);
        }
    }
}
