package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Hamming distance of a left 4-mer to the reverse-complement of the adjacent
 * right 4-mer (fixed 8-base windows). Hairpin counts were flat across loop
 * widths; this asks whether exact complementary pairing (distance 0) is
 * actually enriched versus binomial chance, or whether distance 1 is the mode.
 */
public final class MismatchCensus {

    public static final int PAIRS = 4;

    private final int windows;
    private final List<DistanceRow> rows;
    private final int modalDistance;
    private final double pairChance;
    private final double zeroShare;
    private final double zeroExpected;
    private final double zeroEnrichment;

    private MismatchCensus(
            int windows,
            List<DistanceRow> rows,
            int modalDistance,
            double pairChance,
            double zeroShare,
            double zeroExpected,
            double zeroEnrichment) {
        this.windows = windows;
        this.rows = rows;
        this.modalDistance = modalDistance;
        this.pairChance = pairChance;
        this.zeroShare = zeroShare;
        this.zeroExpected = zeroExpected;
        this.zeroEnrichment = zeroEnrichment;
    }

    public static MismatchCensus from(DnaSequence sequence) {
        Objects.requireNonNull(sequence, "sequence");
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < sequence.normalized().length(); i++) {
            char ch = sequence.normalized().charAt(i);
            if (ch == 'A' || ch == 'T' || ch == 'G' || ch == 'C') {
                canonical.append(ch);
            }
        }
        String tape = canonical.toString();
        int n = tape.length();
        int[] counts = new int[PAIRS + 1];
        int windows = 0;
        int span = PAIRS * 2;
        for (int i = 0; i + span <= n; i++) {
            int dist = 0;
            for (int p = 0; p < PAIRS; p++) {
                char left = tape.charAt(i + p);
                char right = tape.charAt(i + span - 1 - p);
                if (complement(left) != right) {
                    dist++;
                }
            }
            counts[dist]++;
            windows++;
        }
        long a = sequence.canonicalCounts().getOrDefault('A', 0L);
        long t = sequence.canonicalCounts().getOrDefault('T', 0L);
        long g = sequence.canonicalCounts().getOrDefault('G', 0L);
        long c = sequence.canonicalCounts().getOrDefault('C', 0L);
        double total = a + t + g + c;
        double pairChance = 0.0;
        if (total > 0) {
            pairChance = 2.0 * (a / total) * (t / total) + 2.0 * (g / total) * (c / total);
        }
        double zeroExpected = Math.pow(pairChance, PAIRS);
        double zeroShare = windows == 0 ? 0.0 : counts[0] * 1.0 / windows;
        double zeroEnrichment = zeroExpected == 0.0 ? 0.0 : zeroShare / zeroExpected;
        int modal = 0;
        int best = -1;
        for (int d = 0; d <= PAIRS; d++) {
            if (counts[d] > best) {
                best = counts[d];
                modal = d;
            }
        }
        List<DistanceRow> rows = new ArrayList<>();
        for (int d = 0; d <= PAIRS; d++) {
            double expected = windows == 0 ? 0.0 : binomial(PAIRS, d, pairChance) * windows;
            double share = windows == 0 ? 0.0 : counts[d] * 100.0 / windows;
            double enrichment = expected == 0.0 ? 0.0 : counts[d] / expected;
            rows.add(new DistanceRow(d, counts[d], share, expected, enrichment));
        }
        return new MismatchCensus(windows, List.copyOf(rows), modal, pairChance,
                zeroShare, zeroExpected, zeroEnrichment);
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

    private static double binomial(int n, int k, double pMatch) {
        double pMismatch = 1.0 - pMatch;
        return combinations(n, k) * Math.pow(pMatch, n - k) * Math.pow(pMismatch, k);
    }

    private static double combinations(int n, int k) {
        if (k < 0 || k > n) {
            return 0.0;
        }
        double c = 1.0;
        for (int i = 0; i < k; i++) {
            c *= (n - i);
            c /= (i + 1);
        }
        return c;
    }

    public int windows() {
        return windows;
    }

    public List<DistanceRow> rows() {
        return rows;
    }

    public int modalDistance() {
        return modalDistance;
    }

    public double pairChance() {
        return pairChance;
    }

    public double zeroShare() {
        return zeroShare;
    }

    public double zeroExpected() {
        return zeroExpected;
    }

    public double zeroEnrichment() {
        return zeroEnrichment;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "windows=%d modalDist=%d zeroShare=%.4f zeroExpected=%.4f zeroEnrichment=%.3f pairChance=%.4f",
                windows, modalDistance, zeroShare, zeroExpected, zeroEnrichment, pairChance);
    }

    public record DistanceRow(int distance, int count, double sharePercent, double expected, double enrichment) {
    }
}
