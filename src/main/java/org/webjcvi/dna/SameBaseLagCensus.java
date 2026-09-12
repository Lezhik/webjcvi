package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Same-base match rate at selected lags versus the independence baseline
 * sum p_b^2. Codon frames were phase-flat; this asks which distance actually
 * repeats identity — lag 1 (homopolymer), 3 (codon), or 70 (wrap).
 */
public final class SameBaseLagCensus {

    public static final int[] LAGS = {1, 2, 3, 10, 70};

    private static final Logger log = LoggerFactory.getLogger(SameBaseLagCensus.class);

    private final double independent;
    private final List<Lag> lags;
    private final int peakLag;
    private final double peakEnrichment;

    private SameBaseLagCensus(double independent, List<Lag> lags, int peakLag, double peakEnrichment) {
        this.independent = independent;
        this.lags = lags;
        this.peakLag = peakLag;
        this.peakEnrichment = peakEnrichment;
    }

    public static SameBaseLagCensus from(DnaSequence sequence) {
        Objects.requireNonNull(sequence, "sequence");
        if (log.isDebugEnabled()) {
            log.debug("same-base-lag.start length={}", sequence.length());
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
        double independent = 0.0;
        if (n > 0) {
            long[] counts = new long[4];
            for (int i = 0; i < n; i++) {
                counts[index(tape.charAt(i))]++;
            }
            for (long c : counts) {
                double p = c * 1.0 / n;
                independent += p * p;
            }
        }
        List<Lag> lags = new ArrayList<>();
        int peakLag = LAGS[0];
        double peakEnrichment = 0.0;
        for (int lag : LAGS) {
            long matches = 0;
            long pairs = 0;
            if (n > lag) {
                pairs = n - lag;
                for (int i = 0; i < n - lag; i++) {
                    if (tape.charAt(i) == tape.charAt(i + lag)) {
                        matches++;
                    }
                }
            }
            double rate = pairs == 0 ? 0.0 : matches * 100.0 / pairs;
            double enrichment = independent <= 0.0 || pairs == 0
                    ? 0.0
                    : (matches * 1.0 / pairs) / independent;
            lags.add(new Lag(lag, matches, pairs, rate, enrichment));
            if (enrichment > peakEnrichment || (enrichment == peakEnrichment && lag < peakLag)) {
                peakEnrichment = enrichment;
                peakLag = lag;
            }
        }
        SameBaseLagCensus census = new SameBaseLagCensus(independent, List.copyOf(lags), peakLag, peakEnrichment);
        if (log.isDebugEnabled()) {
            log.debug("same-base-lag.done peakLag={} peakEnrichment={}", peakLag, peakEnrichment);
        }
        return census;
    }

    private static int index(char ch) {
        return switch (ch) {
            case 'A' -> 0;
            case 'T' -> 1;
            case 'G' -> 2;
            default -> 3;
        };
    }

    public double independent() {
        return independent;
    }

    public List<Lag> lags() {
        return lags;
    }

    public int peakLag() {
        return peakLag;
    }

    public double peakEnrichment() {
        return peakEnrichment;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT, "peakLag=%d enrichment=%.3f independent=%.4f",
                peakLag, peakEnrichment, independent);
    }

    public record Lag(int lag, long matches, long pairs, double matchPercent, double enrichment) {
    }
}
