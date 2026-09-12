package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Non-overlapping triplet census in the three reading phases. Island geography
 * and wrap dimers never asked whether the tape is 3-periodic (codon-like).
 */
public final class CodonFrameCensus {

    private static final String START = "ATG";
    private static final java.util.Set<String> STOPS = java.util.Set.of("TAA", "TAG", "TGA");

    private final List<Frame> frames;
    private final int bestAtgPhase;
    private final int bestStopPhase;
    private final int maxAtg;
    private final int maxStops;
    private final double maxGcSpread;

    private CodonFrameCensus(
            List<Frame> frames,
            int bestAtgPhase,
            int bestStopPhase,
            int maxAtg,
            int maxStops,
            double maxGcSpread) {
        this.frames = frames;
        this.bestAtgPhase = bestAtgPhase;
        this.bestStopPhase = bestStopPhase;
        this.maxAtg = maxAtg;
        this.maxStops = maxStops;
        this.maxGcSpread = maxGcSpread;
    }

    public static CodonFrameCensus from(DnaSequence sequence) {
        Objects.requireNonNull(sequence, "sequence");
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < sequence.normalized().length(); i++) {
            char ch = sequence.normalized().charAt(i);
            if (ch == 'A' || ch == 'T' || ch == 'G' || ch == 'C') {
                canonical.append(ch);
            }
        }
        String tape = canonical.toString();
        List<Frame> frames = new ArrayList<>(3);
        int bestAtgPhase = 0;
        int bestStopPhase = 0;
        int maxAtg = 0;
        int maxStops = 0;
        double maxSpread = 0.0;
        for (int phase = 0; phase < 3; phase++) {
            Frame frame = countPhase(tape, phase);
            frames.add(frame);
            if (frame.atg() > maxAtg || (frame.atg() == maxAtg && phase < bestAtgPhase)) {
                maxAtg = frame.atg();
                bestAtgPhase = phase;
            }
            if (frame.stops() > maxStops || (frame.stops() == maxStops && phase < bestStopPhase)) {
                maxStops = frame.stops();
                bestStopPhase = phase;
            }
            maxSpread = Math.max(maxSpread, frame.gcSpread());
        }
        return new CodonFrameCensus(List.copyOf(frames), bestAtgPhase, bestStopPhase, maxAtg, maxStops, maxSpread);
    }

    private static Frame countPhase(String tape, int phase) {
        int[] gc = new int[3];
        int[] n = new int[3];
        Map<String, Long> codons = new LinkedHashMap<>();
        int atg = 0;
        int stops = 0;
        int count = 0;
        for (int i = phase; i + 2 < tape.length(); i += 3) {
            char a = tape.charAt(i);
            char b = tape.charAt(i + 1);
            char c = tape.charAt(i + 2);
            String codon = "" + a + b + c;
            codons.merge(codon, 1L, Long::sum);
            tally(a, 0, gc, n);
            tally(b, 1, gc, n);
            tally(c, 2, gc, n);
            if (START.equals(codon)) {
                atg++;
            }
            if (STOPS.contains(codon)) {
                stops++;
            }
            count++;
        }
        String top = "-";
        long topCount = 0;
        if (!codons.isEmpty()) {
            var ranked = new ArrayList<>(codons.entrySet());
            ranked.sort(Comparator
                    .<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                    .thenComparing(Map.Entry::getKey));
            top = ranked.get(0).getKey();
            topCount = ranked.get(0).getValue();
        }
        return new Frame(
                phase,
                count,
                percent(gc[0], n[0]),
                percent(gc[1], n[1]),
                percent(gc[2], n[2]),
                atg,
                stops,
                top,
                topCount);
    }

    private static void tally(char ch, int pos, int[] gc, int[] n) {
        n[pos]++;
        if (ch == 'G' || ch == 'C') {
            gc[pos]++;
        }
    }

    private static double percent(int gc, int n) {
        if (n == 0) {
            return 0.0;
        }
        return gc * 100.0 / n;
    }

    public List<Frame> frames() {
        return frames;
    }

    public int bestAtgPhase() {
        return bestAtgPhase;
    }

    public int bestStopPhase() {
        return bestStopPhase;
    }

    public int maxAtg() {
        return maxAtg;
    }

    public int maxStops() {
        return maxStops;
    }

    public double maxGcSpread() {
        return maxGcSpread;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "bestAtgPhase=%d atg=%d bestStopPhase=%d stops=%d gcSpread=%.2f",
                bestAtgPhase, maxAtg, bestStopPhase, maxStops, maxGcSpread);
    }

    public record Frame(
            int phase,
            int codonCount,
            double gcPos1,
            double gcPos2,
            double gcPos3,
            int atg,
            int stops,
            String topCodon,
            long topCount) {

        public double gcSpread() {
            return Math.abs(gcPos3 - gcPos1);
        }
    }
}
