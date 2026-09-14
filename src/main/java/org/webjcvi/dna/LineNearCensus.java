package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Near-unique width of FASTA source lines. v27 uniqueness covers a majority
 * at the floor (majorityAt=8, share 0.7191) then 0.8726 at k=9 and cliffs at
 * k=12 (share 0.9943). Iteration 26's majority-scan then found log journals
 * whose 0.5-width sat at k=61 with share only ~0.55, while 0.99 never
 * arrives. This census keeps FASTA newlines and asks for the smallest k
 * whose unique share is ≥ 0.90, not where uniqueness first covers half.
 */
public final class LineNearCensus {

    public static final int FLOOR = 8;
    public static final double THRESHOLD = 0.90;
    public static final int[] SAMPLE_LENGTHS = {8, 9, 10, 11, 12, 16};

    private static final Logger log = LoggerFactory.getLogger(LineNearCensus.class);

    private final int lines;
    private final int nearAt;
    private final double shareAtNear;
    private final int majorityAt;
    private final double shareAtMajority;
    private final List<Row> samples;

    private LineNearCensus(
            int lines,
            int nearAt,
            double shareAtNear,
            int majorityAt,
            double shareAtMajority,
            List<Row> samples) {
        this.lines = lines;
        this.nearAt = nearAt;
        this.shareAtNear = shareAtNear;
        this.majorityAt = majorityAt;
        this.shareAtMajority = shareAtMajority;
        this.samples = samples;
    }

    public static LineNearCensus fromRaw(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (log.isDebugEnabled()) {
            log.debug("linenear.start chars={}", raw.length());
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
        int maxLen = 0;
        for (String row : rows) {
            if (row.length() > maxLen) {
                maxLen = row.length();
            }
        }
        int nearAt = 0;
        double shareAtNear = 0.0;
        int majorityAt = 0;
        double shareAtMajority = 0.0;
        if (!rows.isEmpty()) {
            int cap = Math.max(maxLen, FLOOR);
            for (int k = FLOOR; k <= cap; k++) {
                double share = uniqueShare(rows, k);
                if (majorityAt == 0 && share >= 0.5) {
                    majorityAt = k;
                    shareAtMajority = share;
                }
                if (nearAt == 0 && share >= THRESHOLD) {
                    nearAt = k;
                    shareAtNear = share;
                    break;
                }
            }
        }
        List<Row> samples = new ArrayList<>();
        for (int k : SAMPLE_LENGTHS) {
            samples.add(new Row(k, uniqueShare(rows, k)));
        }
        LineNearCensus census = new LineNearCensus(
                rows.size(), nearAt, shareAtNear, majorityAt, shareAtMajority, List.copyOf(samples));
        if (log.isDebugEnabled()) {
            log.debug("linenear.done {}", census.toTextRow());
        }
        return census;
    }

    private static double uniqueShare(List<String> rows, int k) {
        if (rows.isEmpty()) {
            return 0.0;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String row : rows) {
            String key = row.length() <= k ? row : row.substring(0, k);
            counts.merge(key, 1, Integer::sum);
        }
        return counts.size() * 1.0 / rows.size();
    }

    public int lines() {
        return lines;
    }

    public int nearAt() {
        return nearAt;
    }

    public double shareAtNear() {
        return shareAtNear;
    }

    public int majorityAt() {
        return majorityAt;
    }

    public double shareAtMajority() {
        return shareAtMajority;
    }

    public List<Row> samples() {
        return samples;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "lines=%d nearAt=%d shareNear=%.4f majorityAt=%d shareMaj=%.4f",
                lines, nearAt, shareAtNear, majorityAt, shareAtMajority);
    }

    public record Row(int length, double uniqueShare) {
    }
}
