package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adjacent 4-mer Hamming under three neighbor maps: identity, reverse, and
 * reverse-complement. The v11 RC-only histogram was depleted at distance 0
 * and peaked at 3; this asks whether neighbors avoid <em>any</em> similarity
 * or specifically avoid complementary pairing.
 */
public final class NeighborCensus {

    public static final int PAIRS = 4;

    private static final Logger log = LoggerFactory.getLogger(NeighborCensus.class);

    private final int windows;
    private final List<ModeRow> modes;

    private NeighborCensus(int windows, List<ModeRow> modes) {
        this.windows = windows;
        this.modes = modes;
    }

    public static NeighborCensus from(DnaSequence sequence) {
        Objects.requireNonNull(sequence, "sequence");
        if (log.isDebugEnabled()) {
            log.debug("neighbor.start length={}", sequence.length());
        }
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < sequence.normalized().length(); i++) {
            char ch = sequence.normalized().charAt(i);
            if (ch == 'A' || ch == 'T' || ch == 'G' || ch == 'C') {
                canonical.append(ch);
            }
        }
        String tape = canonical.toString();
        int n = tape.length();
        int span = PAIRS * 2;
        int[] identity = new int[PAIRS + 1];
        int[] reverse = new int[PAIRS + 1];
        int[] rc = new int[PAIRS + 1];
        int windows = 0;
        for (int i = 0; i + span <= n; i++) {
            identity[hamming(tape, i, i + PAIRS, false, false)]++;
            reverse[hamming(tape, i, i + PAIRS, true, false)]++;
            rc[hamming(tape, i, i + PAIRS, true, true)]++;
            windows++;
        }
        long a = sequence.canonicalCounts().getOrDefault('A', 0L);
        long t = sequence.canonicalCounts().getOrDefault('T', 0L);
        long g = sequence.canonicalCounts().getOrDefault('G', 0L);
        long c = sequence.canonicalCounts().getOrDefault('C', 0L);
        double total = a + t + g + c;
        double pSame = 0.0;
        double pPair = 0.0;
        if (total > 0) {
            pSame = (a * a + t * t + g * g + c * c) / (total * total);
            pPair = 2.0 * (a / total) * (t / total) + 2.0 * (g / total) * (c / total);
        }
        List<ModeRow> modes = List.of(
                summarize("identity", identity, windows, pSame),
                summarize("reverse", reverse, windows, pSame),
                summarize("rc", rc, windows, pPair));
        NeighborCensus census = new NeighborCensus(windows, modes);
        if (log.isDebugEnabled()) {
            log.debug("neighbor.done windows={} {}", windows, census.toTextRow());
        }
        return census;
    }

    private static int hamming(String tape, int left, int right, boolean reverseRight, boolean complementRight) {
        int dist = 0;
        for (int p = 0; p < PAIRS; p++) {
            char l = tape.charAt(left + p);
            int rightIndex = reverseRight ? right + PAIRS - 1 - p : right + p;
            char r = tape.charAt(rightIndex);
            if (complementRight) {
                r = complement(r);
            }
            if (l != r) {
                dist++;
            }
        }
        return dist;
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

    private static ModeRow summarize(String mode, int[] counts, int windows, double pMatch) {
        int modal = 0;
        int best = -1;
        long distSum = 0;
        for (int d = 0; d < counts.length; d++) {
            if (counts[d] > best) {
                best = counts[d];
                modal = d;
            }
            distSum += (long) d * counts[d];
        }
        double mean = windows == 0 ? 0.0 : distSum * 1.0 / windows;
        double zeroShare = windows == 0 ? 0.0 : counts[0] * 1.0 / windows;
        double zeroExpected = Math.pow(pMatch, PAIRS);
        double zeroEnrichment = zeroExpected == 0.0 ? 0.0 : zeroShare / zeroExpected;
        return new ModeRow(mode, modal, mean, zeroShare, zeroExpected, zeroEnrichment, counts[0]);
    }

    public int windows() {
        return windows;
    }

    public List<ModeRow> modes() {
        return modes;
    }

    public ModeRow identity() {
        return modes.get(0);
    }

    public ModeRow reverse() {
        return modes.get(1);
    }

    public ModeRow rc() {
        return modes.get(2);
    }

    public String toTextRow() {
        StringBuilder out = new StringBuilder();
        out.append("windows=").append(windows);
        for (ModeRow row : modes) {
            out.append(' ').append(row.mode()).append("{modal=")
                    .append(row.modalDistance())
                    .append(" zero×")
                    .append(String.format(Locale.ROOT, "%.3f", row.zeroEnrichment()))
                    .append('}');
        }
        return out.toString();
    }

    public record ModeRow(
            String mode,
            int modalDistance,
            double meanDistance,
            double zeroShare,
            double zeroExpected,
            double zeroEnrichment,
            int zeroCount) {
    }
}
