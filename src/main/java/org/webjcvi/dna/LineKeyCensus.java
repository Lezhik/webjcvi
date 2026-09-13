package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uniqueness of <em>source-line</em> prefixes versus k. Iteration 22 found
 * concatenated k=16 uniqueness is a plateau (spread 0.0005) while Logback
 * line prefixes at k=16 are a clock (unique share ~0.001). This census keeps
 * FASTA newlines and asks where line uniqueness saturates — a clock-cliff
 * versus the concatenated-tape landscape.
 */
public final class LineKeyCensus {

    public static final int FLOOR = 8;
    public static final int[] SAMPLE_LENGTHS = {8, 16, 20, 24, 32, 70};

    private static final Logger log = LoggerFactory.getLogger(LineKeyCensus.class);

    private final int lines;
    private final int floorLength;
    private final int uniqueAt;
    private final int maxLength;
    private final List<Row> samples;

    private LineKeyCensus(int lines, int floorLength, int uniqueAt, int maxLength, List<Row> samples) {
        this.lines = lines;
        this.floorLength = floorLength;
        this.uniqueAt = uniqueAt;
        this.maxLength = maxLength;
        this.samples = samples;
    }

    public static LineKeyCensus fromRaw(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (log.isDebugEnabled()) {
            log.debug("linekey.start chars={}", raw.length());
        }
        List<String> rows = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= raw.length(); i++) {
            if (i == raw.length() || raw.charAt(i) == '\n') {
                String line = raw.substring(start, i);
                if (line.endsWith("\r")) {
                    line = line.substring(0, line.length() - 1);
                }
                StringBuilder canonical = new StringBuilder();
                for (int c = 0; c < line.length(); c++) {
                    char ch = Character.toUpperCase(line.charAt(c));
                    if (ch == 'A' || ch == 'T' || ch == 'G' || ch == 'C') {
                        canonical.append(ch);
                    }
                }
                if (!canonical.isEmpty()) {
                    rows.add(canonical.toString());
                }
                start = i + 1;
            }
        }
        int maxLength = 0;
        for (String row : rows) {
            if (row.length() > maxLength) {
                maxLength = row.length();
            }
        }
        int uniqueAt = 0;
        if (!rows.isEmpty()) {
            int cap = Math.max(maxLength, FLOOR);
            for (int k = FLOOR; k <= cap; k++) {
                if (distinctCount(rows, k) == rows.size()) {
                    uniqueAt = k;
                    break;
                }
            }
        }
        List<Row> samples = new ArrayList<>();
        for (int k : SAMPLE_LENGTHS) {
            samples.add(row(rows, k));
        }
        LineKeyCensus census = new LineKeyCensus(rows.size(), FLOOR, uniqueAt, maxLength, List.copyOf(samples));
        if (log.isDebugEnabled()) {
            log.debug("linekey.done {}", census.toTextRow());
        }
        return census;
    }

    private static Row row(List<String> rows, int k) {
        int distinct = distinctCount(rows, k);
        double share = rows.isEmpty() ? 0.0 : distinct * 1.0 / rows.size();
        return new Row(k, distinct, share);
    }

    private static int distinctCount(List<String> rows, int k) {
        Map<String, Integer> counts = new HashMap<>();
        for (String row : rows) {
            counts.merge(prefix(row, k), 1, Integer::sum);
        }
        return counts.size();
    }

    private static String prefix(String row, int k) {
        if (row.length() <= k) {
            return row;
        }
        return row.substring(0, k);
    }

    public int lines() {
        return lines;
    }

    public int floorLength() {
        return floorLength;
    }

    public int uniqueAt() {
        return uniqueAt;
    }

    public int maxLength() {
        return maxLength;
    }

    public List<Row> samples() {
        return samples;
    }

    public double shareAt(int length) {
        for (Row row : samples) {
            if (row.length() == length) {
                return row.uniqueShare();
            }
        }
        return 0.0;
    }

    public String toTextRow() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "lines=%d floor=%d uniqueAt=%d maxLen=%d",
                lines, floorLength, uniqueAt, maxLength));
        for (Row row : samples) {
            sb.append(String.format(Locale.ROOT, " k%d=%.4f", row.length(), row.uniqueShare()));
        }
        return sb.toString();
    }

    public record Row(int length, int distinct, double uniqueShare) {
    }
}
