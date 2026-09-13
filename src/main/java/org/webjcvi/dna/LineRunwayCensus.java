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
 * Tightness of source-line uniqueness versus residual twins. v24 line forks
 * split at uniqueAt (min/modal/max 19/19/20 of two groups). Iteration 24's
 * layout-runway then found log journals whose 0.99 cliff vanished
 * (cliffAt=0) while forkAt sat just past the clock. This census keeps FASTA
 * newlines and reports whether uniqueness <em>cliffs before</em> residual
 * twins fork (overhang) or a layout body sits between them (runway).
 */
public final class LineRunwayCensus {

    public static final int TILE = 16;
    public static final int FLOOR = 8;
    public static final double CLIFF_SHARE = 0.99;

    private static final Logger log = LoggerFactory.getLogger(LineRunwayCensus.class);

    private final int lines;
    private final int tileLength;
    private final int cliffAt;
    private final double shareAtCliff;
    private final int modalFork;
    private final int runway;
    private final int overhang;

    private LineRunwayCensus(
            int lines,
            int tileLength,
            int cliffAt,
            double shareAtCliff,
            int modalFork,
            int runway,
            int overhang) {
        this.lines = lines;
        this.tileLength = tileLength;
        this.cliffAt = cliffAt;
        this.shareAtCliff = shareAtCliff;
        this.modalFork = modalFork;
        this.runway = runway;
        this.overhang = overhang;
    }

    public static LineRunwayCensus fromRaw(String raw) {
        Objects.requireNonNull(raw, "raw");
        LineForkCensus forks = LineForkCensus.fromRaw(raw);
        if (log.isDebugEnabled()) {
            log.debug("linerunway.start chars={}", raw.length());
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
        int cliffAt = 0;
        double shareAtCliff = 0.0;
        if (!rows.isEmpty()) {
            int cap = Math.max(maxLen, FLOOR);
            for (int k = FLOOR; k <= cap; k++) {
                double share = uniqueShare(rows, k);
                if (share >= CLIFF_SHARE) {
                    cliffAt = k;
                    shareAtCliff = share;
                    break;
                }
            }
        }
        int modalFork = forks.modalFork();
        int runway = 0;
        int overhang = 0;
        if (cliffAt > 0 && modalFork > 0 && cliffAt > modalFork) {
            runway = cliffAt - modalFork;
        } else if (cliffAt > 0 && modalFork > cliffAt) {
            overhang = modalFork - cliffAt;
        }
        LineRunwayCensus census = new LineRunwayCensus(
                rows.size(), TILE, cliffAt, shareAtCliff, modalFork, runway, overhang);
        if (log.isDebugEnabled()) {
            log.debug("linerunway.done {}", census.toTextRow());
        }
        return census;
    }

    private static double uniqueShare(List<String> rows, int k) {
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

    public int tileLength() {
        return tileLength;
    }

    public int cliffAt() {
        return cliffAt;
    }

    public double shareAtCliff() {
        return shareAtCliff;
    }

    public int modalFork() {
        return modalFork;
    }

    public int runway() {
        return runway;
    }

    public int overhang() {
        return overhang;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "lines=%d tile=%d cliffAt=%d shareCliff=%.4f modalFork=%d runway=%d overhang=%d",
                lines, tileLength, cliffAt, shareAtCliff, modalFork, runway, overhang);
    }
}
