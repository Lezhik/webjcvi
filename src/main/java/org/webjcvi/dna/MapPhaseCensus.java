package org.webjcvi.dna;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Peak wrap-frame phase of identity, reverse, and RC distance-0. v14 reverse
 * peaked at column 19; this asks whether that column is reverse-specific or
 * the same phase wins for every neighbor map (a compositional artifact).
 */
public final class MapPhaseCensus {

    public static final int PAIRS = 4;

    private static final Logger log = LoggerFactory.getLogger(MapPhaseCensus.class);

    private final int wrapWidth;
    private final int windows;
    private final List<MapPeak> maps;

    private MapPhaseCensus(int wrapWidth, int windows, List<MapPeak> maps) {
        this.wrapWidth = wrapWidth;
        this.windows = windows;
        this.maps = maps;
    }

    public static MapPhaseCensus from(DnaSequence sequence, int wrapWidth) {
        Objects.requireNonNull(sequence, "sequence");
        int frame = wrapWidth < PAIRS * 2 ? 70 : wrapWidth;
        if (log.isDebugEnabled()) {
            log.debug("map-phase.start length={} wrapWidth={}", sequence.length(), frame);
        }
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < sequence.normalized().length(); i++) {
            char ch = sequence.normalized().charAt(i);
            if (ch == 'A' || ch == 'T' || ch == 'G' || ch == 'C') {
                canonical.append(ch);
            }
        }
        String tape = canonical.toString();
        int span = PAIRS * 2;
        int[] identity = new int[frame];
        int[] reverse = new int[frame];
        int[] rc = new int[frame];
        int[] counts = new int[frame];
        int windows = 0;
        for (int i = 0; i + span <= tape.length(); i++) {
            int phase = i % frame;
            counts[phase]++;
            windows++;
            identity[phase] += zero(tape, i, false, false) ? 1 : 0;
            reverse[phase] += zero(tape, i, true, false) ? 1 : 0;
            rc[phase] += zero(tape, i, true, true) ? 1 : 0;
        }
        List<MapPeak> maps = List.of(
                peak("identity", identity, counts, frame),
                peak("reverse", reverse, counts, frame),
                peak("rc", rc, counts, frame));
        MapPhaseCensus census = new MapPhaseCensus(frame, windows, maps);
        if (log.isDebugEnabled()) {
            log.debug("map-phase.done {}", census.toTextRow());
        }
        return census;
    }

    private static boolean zero(String tape, int offset, boolean reverseRight, boolean complementRight) {
        for (int i = 0; i < PAIRS; i++) {
            int rightIndex = reverseRight ? offset + PAIRS + PAIRS - 1 - i : offset + PAIRS + i;
            char r = tape.charAt(rightIndex);
            if (complementRight) {
                r = complement(r);
            }
            if (tape.charAt(offset + i) != r) {
                return false;
            }
        }
        return true;
    }

    private static char complement(char ch) {
        return switch (ch) {
            case 'A' -> 'T';
            case 'T' -> 'A';
            case 'G' -> 'C';
            case 'C' -> 'G';
            default -> 0;
        };
    }

    private static MapPeak peak(String mode, int[] zero, int[] counts, int frame) {
        int bestPhase = 0;
        double bestShare = -1.0;
        int bestZero = 0;
        for (int p = 0; p < frame; p++) {
            double share = counts[p] == 0 ? 0.0 : zero[p] * 1.0 / counts[p];
            if (share > bestShare || (share == bestShare && p < bestPhase)) {
                bestShare = share;
                bestPhase = p;
                bestZero = zero[p];
            }
        }
        if (bestShare < 0.0) {
            bestShare = 0.0;
        }
        return new MapPeak(mode, bestPhase, bestShare, bestZero);
    }

    public int wrapWidth() {
        return wrapWidth;
    }

    public int windows() {
        return windows;
    }

    public List<MapPeak> maps() {
        return maps;
    }

    public MapPeak identity() {
        return maps.get(0);
    }

    public MapPeak reverse() {
        return maps.get(1);
    }

    public MapPeak rc() {
        return maps.get(2);
    }

    public String toTextRow() {
        StringBuilder out = new StringBuilder();
        out.append("wrap=").append(wrapWidth).append(" windows=").append(windows);
        for (MapPeak row : maps) {
            out.append(' ').append(row.mode()).append("{phase=")
                    .append(row.peakPhase())
                    .append(" share=")
                    .append(String.format(Locale.ROOT, "%.4f", row.peakShare()))
                    .append('}');
        }
        return out.toString();
    }

    public record MapPeak(String mode, int peakPhase, double peakShare, int peakZero) {
    }
}
