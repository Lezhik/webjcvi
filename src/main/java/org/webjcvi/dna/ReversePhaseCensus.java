package org.webjcvi.dna;

import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reverse-0 share of adjacent 4-mers by position modulo wrap width. v13 showed
 * wrap joints slightly depleted vs the linear interior (0.913); this asks
 * which column of the wrap frame actually holds the reverse enrichment — the
 * wrap column (width-4) or some other phase.
 */
public final class ReversePhaseCensus {

    public static final int PAIRS = 4;

    private static final Logger log = LoggerFactory.getLogger(ReversePhaseCensus.class);

    private final int wrapWidth;
    private final int wrapPhase;
    private final int windows;
    private final int reverseZero;
    private final int peakPhase;
    private final double peakShare;
    private final double wrapPhaseShare;
    private final double meanShare;
    private final double wrapVsMean;

    private ReversePhaseCensus(
            int wrapWidth,
            int wrapPhase,
            int windows,
            int reverseZero,
            int peakPhase,
            double peakShare,
            double wrapPhaseShare,
            double meanShare,
            double wrapVsMean) {
        this.wrapWidth = wrapWidth;
        this.wrapPhase = wrapPhase;
        this.windows = windows;
        this.reverseZero = reverseZero;
        this.peakPhase = peakPhase;
        this.peakShare = peakShare;
        this.wrapPhaseShare = wrapPhaseShare;
        this.meanShare = meanShare;
        this.wrapVsMean = wrapVsMean;
    }

    public static ReversePhaseCensus from(DnaSequence sequence, int wrapWidth) {
        Objects.requireNonNull(sequence, "sequence");
        int frame = wrapWidth < PAIRS * 2 ? 70 : wrapWidth;
        if (log.isDebugEnabled()) {
            log.debug("reverse-phase.start length={} wrapWidth={}", sequence.length(), frame);
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
        int wrapPhase = Math.floorMod(frame - PAIRS, frame);
        int[] zero = new int[frame];
        int[] counts = new int[frame];
        int windows = 0;
        int reverseZero = 0;
        for (int i = 0; i + span <= tape.length(); i++) {
            int phase = i % frame;
            counts[phase]++;
            windows++;
            if (reverseZero(tape, i)) {
                zero[phase]++;
                reverseZero++;
            }
        }
        int peakPhase = 0;
        double peakShare = 0.0;
        for (int p = 0; p < frame; p++) {
            double share = counts[p] == 0 ? 0.0 : zero[p] * 1.0 / counts[p];
            if (share > peakShare || (share == peakShare && p < peakPhase)) {
                peakShare = share;
                peakPhase = p;
            }
        }
        if (windows > 0 && peakShare == 0.0) {
            peakPhase = 0;
        }
        double wrapShare = counts[wrapPhase] == 0 ? 0.0 : zero[wrapPhase] * 1.0 / counts[wrapPhase];
        double meanShare = windows == 0 ? 0.0 : reverseZero * 1.0 / windows;
        double wrapVsMean = meanShare == 0.0 ? 0.0 : wrapShare / meanShare;
        ReversePhaseCensus census = new ReversePhaseCensus(
                frame, wrapPhase, windows, reverseZero, peakPhase, peakShare,
                wrapShare, meanShare, wrapVsMean);
        if (log.isDebugEnabled()) {
            log.debug("reverse-phase.done {}", census.toTextRow());
        }
        return census;
    }

    private static boolean reverseZero(String tape, int offset) {
        for (int i = 0; i < PAIRS; i++) {
            if (tape.charAt(offset + i) != tape.charAt(offset + spanRight(i))) {
                return false;
            }
        }
        return true;
    }

    private static int spanRight(int i) {
        return PAIRS + PAIRS - 1 - i;
    }

    public int wrapWidth() {
        return wrapWidth;
    }

    public int wrapPhase() {
        return wrapPhase;
    }

    public int windows() {
        return windows;
    }

    public int reverseZero() {
        return reverseZero;
    }

    public int peakPhase() {
        return peakPhase;
    }

    public double peakShare() {
        return peakShare;
    }

    public double wrapPhaseShare() {
        return wrapPhaseShare;
    }

    public double meanShare() {
        return meanShare;
    }

    public double wrapVsMean() {
        return wrapVsMean;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "wrap=%d wrapPhase=%d windows=%d rev0=%d peakPhase=%d peakShare=%.4f wrapShare=%.4f mean=%.4f wrap/mean=%.3f",
                wrapWidth, wrapPhase, windows, reverseZero, peakPhase, peakShare,
                wrapPhaseShare, meanShare, wrapVsMean);
    }
}
