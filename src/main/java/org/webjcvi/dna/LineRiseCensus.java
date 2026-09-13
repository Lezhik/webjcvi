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
 * Steepest unique-share rise of FASTA source lines. v25 uniqueness cliffs at
 * k=12 (share 0.9943) with overhang 7 before residual twins fork at 19.
 * Iteration 25's rise-scan then found log journals whose 0.99 cliff is missing
 * (cliffAt=0) while the steepest uniqueness jump sat past the clock (riseAt=23).
 * This census keeps FASTA newlines and asks where unique share rises fastest
 * along k, not whether it ever reaches 0.99.
 */
public final class LineRiseCensus {

    public static final int FLOOR = 8;
    public static final int[] SAMPLE_LENGTHS = {8, 9, 12, 16, 20};

    private static final Logger log = LoggerFactory.getLogger(LineRiseCensus.class);

    private final int lines;
    private final int riseAt;
    private final double gain;
    private final double shareAtRise;
    private final int cliffAt;
    private final List<Row> samples;

    private LineRiseCensus(
            int lines,
            int riseAt,
            double gain,
            double shareAtRise,
            int cliffAt,
            List<Row> samples) {
        this.lines = lines;
        this.riseAt = riseAt;
        this.gain = gain;
        this.shareAtRise = shareAtRise;
        this.cliffAt = cliffAt;
        this.samples = samples;
    }

    public static LineRiseCensus fromRaw(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (log.isDebugEnabled()) {
            log.debug("linerise.start chars={}", raw.length());
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
        int riseAt = 0;
        double gain = 0.0;
        double shareAtRise = 0.0;
        int cliffAt = 0;
        if (!rows.isEmpty()) {
            int cap = Math.max(maxLen, FLOOR);
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
        List<Row> samples = new ArrayList<>();
        for (int k : SAMPLE_LENGTHS) {
            double share = uniqueShare(rows, k);
            double step = k == FLOOR ? share : share - uniqueShare(rows, k - 1);
            samples.add(new Row(k, share, step));
        }
        LineRiseCensus census = new LineRiseCensus(
                rows.size(), riseAt, gain, shareAtRise, cliffAt, List.copyOf(samples));
        if (log.isDebugEnabled()) {
            log.debug("linerise.done {}", census.toTextRow());
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

    public int riseAt() {
        return riseAt;
    }

    public double gain() {
        return gain;
    }

    public double shareAtRise() {
        return shareAtRise;
    }

    public int cliffAt() {
        return cliffAt;
    }

    public List<Row> samples() {
        return samples;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "lines=%d riseAt=%d gain=%.4f shareRise=%.4f cliffAt=%d",
                lines, riseAt, gain, shareAtRise, cliffAt);
    }

    public record Row(int length, double uniqueShare, double gain) {
    }
}
