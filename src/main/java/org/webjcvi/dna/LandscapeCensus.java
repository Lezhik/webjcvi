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
 * Unique share of a fixed-width wrap-frame tile at <em>every</em> start
 * column. v21 sampled only the center (uniqueAt=17, k=16 share 0.9999).
 * Iteration 21's five-column lane scan then found a log-journal spread of
 * ~0.012 — too flat to prefer a fingerprint. This census asks whether a
 * hidden peak exists between those five samples, or uniqueness at k=16 is
 * actually a plateau.
 */
public final class LandscapeCensus {

    public static final int TILE = 16;

    private static final Logger log = LoggerFactory.getLogger(LandscapeCensus.class);

    private final int wrapWidth;
    private final int tileLength;
    private final int frames;
    private final int troughAt;
    private final int peakAt;
    private final double troughShare;
    private final double peakShare;
    private final double spread;
    private final List<Column> columns;

    private LandscapeCensus(
            int wrapWidth,
            int tileLength,
            int frames,
            int troughAt,
            int peakAt,
            double troughShare,
            double peakShare,
            double spread,
            List<Column> columns) {
        this.wrapWidth = wrapWidth;
        this.tileLength = tileLength;
        this.frames = frames;
        this.troughAt = troughAt;
        this.peakAt = peakAt;
        this.troughShare = troughShare;
        this.peakShare = peakShare;
        this.spread = spread;
        this.columns = columns;
    }

    public static LandscapeCensus from(DnaSequence sequence, int wrapWidth) {
        Objects.requireNonNull(sequence, "sequence");
        int frame = wrapWidth < TILE ? 70 : wrapWidth;
        int tile = Math.min(TILE, frame);
        if (log.isDebugEnabled()) {
            log.debug("landscape.start length={} wrapWidth={}", sequence.length(), frame);
        }
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < sequence.normalized().length(); i++) {
            char ch = sequence.normalized().charAt(i);
            if (ch == 'A' || ch == 'T' || ch == 'G' || ch == 'C') {
                canonical.append(ch);
            }
        }
        String tape = canonical.toString();
        List<String> frames = new ArrayList<>();
        for (int i = 0; i + frame <= tape.length(); i += frame) {
            frames.add(tape.substring(i, i + frame));
        }
        List<Column> columns = new ArrayList<>();
        int troughAt = 0;
        int peakAt = 0;
        double troughShare = 0.0;
        double peakShare = 0.0;
        if (!frames.isEmpty() && tile > 0) {
            for (int start = 0; start + tile <= frame; start++) {
                int distinct = distinctAt(frames, start, tile);
                double share = distinct * 1.0 / frames.size();
                columns.add(new Column(start, distinct, share));
            }
            if (!columns.isEmpty()) {
                troughAt = columns.get(0).start();
                peakAt = columns.get(0).start();
                troughShare = columns.get(0).uniqueShare();
                peakShare = columns.get(0).uniqueShare();
                for (Column column : columns) {
                    if (column.uniqueShare() < troughShare) {
                        troughShare = column.uniqueShare();
                        troughAt = column.start();
                    }
                    if (column.uniqueShare() > peakShare) {
                        peakShare = column.uniqueShare();
                        peakAt = column.start();
                    }
                }
            }
        }
        double spread = peakShare - troughShare;
        LandscapeCensus census = new LandscapeCensus(
                frame, tile, frames.size(), troughAt, peakAt, troughShare, peakShare, spread,
                List.copyOf(columns));
        if (log.isDebugEnabled()) {
            log.debug("landscape.done {}", census.toTextRow());
        }
        return census;
    }

    private static int distinctAt(List<String> frames, int start, int tile) {
        Map<String, Integer> counts = new HashMap<>();
        for (String frame : frames) {
            counts.merge(frame.substring(start, start + tile), 1, Integer::sum);
        }
        return counts.size();
    }

    public int wrapWidth() {
        return wrapWidth;
    }

    public int tileLength() {
        return tileLength;
    }

    public int frames() {
        return frames;
    }

    public int troughAt() {
        return troughAt;
    }

    public int peakAt() {
        return peakAt;
    }

    public double troughShare() {
        return troughShare;
    }

    public double peakShare() {
        return peakShare;
    }

    public double spread() {
        return spread;
    }

    public List<Column> columns() {
        return columns;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "wrap=%d tile=%d frames=%d troughAt=%d peakAt=%d trough=%.4f peak=%.4f spread=%.4f columns=%d",
                wrapWidth, tileLength, frames, troughAt, peakAt, troughShare, peakShare, spread, columns.size());
    }

    public record Column(int start, int distinct, double uniqueShare) {
    }
}
