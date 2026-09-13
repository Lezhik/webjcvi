package org.webjcvi.row;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Uniqueness of newline-delimited rows. v22's k=16 landscape is a plateau
 * (spread 0.0005); iteration 21's five wrap-lanes then failed on logs
 * (spread ~0.012) because uniqueness was measured after dropping newlines
 * and re-slicing at 70. DNA source lines <em>are</em> the records (7593
 * lines, modal 70). Concatenating them is identity on FASTA and destroys
 * Logback journals. Inverse of lane-scan (there geography of concatenated
 * wrap-frames) and distinct from fork-scan / key-width (there newlines
 * are dropped). Blank lines are counted but not keyed.
 */
public final class RowScan {

    public static final int TILE = 16;
    public static final int FLOOR = 8;
    public static final int MAX_K = 256;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;

    private static final Logger log = LoggerFactory.getLogger(RowScan.class);

    public Scan profile(String text) {
        if (text == null) {
            throw new RowException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new RowException("Text exceeds " + MAX_CHARS + " characters");
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
        int maxLen = 0;
        for (String row : rows) {
            if (row.length() > maxLen) {
                maxLen = row.length();
            }
        }
        int uniqueAt = 0;
        double uniqueShareAt16 = 0.0;
        int twinCount = 0;
        int twinLines = 0;
        String topPrefix = "";
        int topCount = 0;
        if (scanned > 0) {
            uniqueShareAt16 = uniqueShare(rows, TILE);
            Map<String, Integer> atTile = counts(rows, TILE);
            for (var entry : atTile.entrySet()) {
                int count = entry.getValue();
                if (count >= 2) {
                    twinCount++;
                    twinLines += count;
                }
                if (count > topCount) {
                    topCount = count;
                    topPrefix = entry.getKey();
                }
            }
            int cap = Math.min(Math.max(maxLen, FLOOR), MAX_K);
            for (int k = FLOOR; k <= cap; k++) {
                if (distinctCount(rows, k) == scanned) {
                    uniqueAt = k;
                    break;
                }
            }
        }
        Scan scan = new Scan(
                lineCount, scanned, TILE, uniqueAt, uniqueShareAt16, twinCount, twinLines, topPrefix, topCount);
        if (log.isDebugEnabled()) {
            log.debug("row.profile chars={} {}", text.length(), scan.summary());
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
        return distinctCount(rows, k) * 1.0 / scanned;
    }

    private static int distinctCount(List<String> rows, int k) {
        return counts(rows, k).size();
    }

    private static Map<String, Integer> counts(List<String> rows, int k) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String row : rows) {
            String key = prefix(row, k);
            counts.merge(key, 1, Integer::sum);
        }
        return counts;
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
            int uniqueAt,
            double uniqueShareAt16,
            int twinCount,
            int twinLines,
            String topPrefix,
            int topCount) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "lines=%d scanned=%d tile=%d uniqueAt=%d share16=%.4f twins=%d topCount=%d",
                    lineCount, scanned, tileLength, uniqueAt, uniqueShareAt16, twinCount, topCount);
        }
    }
}
