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
 * Majority unique-share width of FASTA source lines. v26 uniqueness jumps
 * hardest at the floor (riseAt=8, gain 0.7191, share 0.7191) then cliffs at
 * k=12. Iteration 25's rise-scan then found log journals whose steepest jump
 * sat at k=23 with share only ~0.28 — not yet a majority identifier. This
 * census keeps FASTA newlines and asks for the smallest k whose unique share
 * is ≥ 0.5, not where uniqueness accelerates fastest.
 */
public final class LineMajorityCensus {

    public static final int FLOOR = 8;
    public static final double THRESHOLD = 0.5;
    public static final int[] SAMPLE_LENGTHS = {8, 9, 12, 16, 20};

    private static final Logger log = LoggerFactory.getLogger(LineMajorityCensus.class);

    private final int lines;
    private final int majorityAt;
    private final double shareAtMajority;
    private final int riseAt;
    private final double shareAtRise;
    private final List<Row> samples;

    private LineMajorityCensus(
            int lines,
            int majorityAt,
            double shareAtMajority,
            int riseAt,
            double shareAtRise,
            List<Row> samples) {
        this.lines = lines;
        this.majorityAt = majorityAt;
        this.shareAtMajority = shareAtMajority;
        this.riseAt = riseAt;
        this.shareAtRise = shareAtRise;
        this.samples = samples;
    }

    public static LineMajorityCensus fromRaw(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (log.isDebugEnabled()) {
            log.debug("linemajority.start chars={}", raw.length());
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
        int majorityAt = 0;
        double shareAtMajority = 0.0;
        int riseAt = 0;
        double gain = 0.0;
        double shareAtRise = 0.0;
        if (!rows.isEmpty()) {
            int cap = Math.max(maxLen, FLOOR);
            double previous = 0.0;
            for (int k = FLOOR; k <= cap; k++) {
                double share = uniqueShare(rows, k);
                if (majorityAt == 0 && share >= THRESHOLD) {
                    majorityAt = k;
                    shareAtMajority = share;
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
        List<Row> samples = new ArrayList<>();
        for (int k : SAMPLE_LENGTHS) {
            double share = uniqueShare(rows, k);
            samples.add(new Row(k, share));
        }
        LineMajorityCensus census = new LineMajorityCensus(
                rows.size(), majorityAt, shareAtMajority, riseAt, shareAtRise, List.copyOf(samples));
        if (log.isDebugEnabled()) {
            log.debug("linemajority.done {}", census.toTextRow());
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

    public int majorityAt() {
        return majorityAt;
    }

    public double shareAtMajority() {
        return shareAtMajority;
    }

    public int riseAt() {
        return riseAt;
    }

    public double shareAtRise() {
        return shareAtRise;
    }

    public List<Row> samples() {
        return samples;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "lines=%d majorityAt=%d shareMaj=%.4f riseAt=%d shareRise=%.4f",
                lines, majorityAt, shareAtMajority, riseAt, shareAtRise);
    }

    public record Row(int length, double uniqueShare) {
    }
}
