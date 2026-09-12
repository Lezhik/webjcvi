package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reverse-complement hairpins with an explicit loop (gap) between the two
 * stem halves. Adjacent foldback (loop 0 / 1) mixed stem length with the
 * center; this census asks which loop widths actually carry stems of length
 * ≥ 4, so enclosure-with-payload can be kept or abandoned.
 */
public final class HairpinCensus {

    public static final int MIN_STEM = 4;
    public static final int MAX_RADIUS = 200;
    public static final int[] LOOPS = {0, 1, 2, 3, 5, 8, 13};

    private static final Logger log = LoggerFactory.getLogger(HairpinCensus.class);

    private final List<LoopRow> rows;
    private final int modalLoop;
    private final int modalHairpins;
    private final int adjacent;
    private final int gapped;
    private final int longestAdjacent;
    private final int longestGapped;

    private HairpinCensus(
            List<LoopRow> rows,
            int modalLoop,
            int modalHairpins,
            int adjacent,
            int gapped,
            int longestAdjacent,
            int longestGapped) {
        this.rows = rows;
        this.modalLoop = modalLoop;
        this.modalHairpins = modalHairpins;
        this.adjacent = adjacent;
        this.gapped = gapped;
        this.longestAdjacent = longestAdjacent;
        this.longestGapped = longestGapped;
    }

    public static HairpinCensus from(DnaSequence sequence) {
        Objects.requireNonNull(sequence, "sequence");
        if (log.isDebugEnabled()) {
            log.debug("hairpin.start length={}", sequence.length());
        }
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < sequence.normalized().length(); i++) {
            char ch = sequence.normalized().charAt(i);
            if (ch == 'A' || ch == 'T' || ch == 'G' || ch == 'C') {
                canonical.append(ch);
            }
        }
        String tape = canonical.toString();
        List<LoopRow> rows = new ArrayList<>();
        int modalLoop = LOOPS[0];
        int modalHairpins = -1;
        int adjacent = 0;
        int gapped = 0;
        int longestAdjacent = 0;
        int longestGapped = 0;
        for (int loop : LOOPS) {
            LoopRow row = countLoop(tape, loop);
            rows.add(row);
            if (loop == 0) {
                adjacent = row.hairpins();
                longestAdjacent = row.longestStem();
            } else {
                gapped += row.hairpins();
                longestGapped = Math.max(longestGapped, row.longestStem());
            }
            if (row.hairpins() > modalHairpins
                    || (row.hairpins() == modalHairpins && loop < modalLoop)) {
                modalHairpins = row.hairpins();
                modalLoop = loop;
            }
        }
        HairpinCensus census = new HairpinCensus(List.copyOf(rows), modalLoop, Math.max(modalHairpins, 0),
                adjacent, gapped, longestAdjacent, longestGapped);
        if (log.isDebugEnabled()) {
            log.debug("hairpin.done modalLoop={} adjacent={} gapped={}", modalLoop, adjacent, gapped);
        }
        return census;
    }

    private static LoopRow countLoop(String tape, int loop) {
        int n = tape.length();
        int hairpins = 0;
        int longest = 0;
        long at = 0;
        long gc = 0;
        if (loop == 0) {
            for (int i = 0; i < n - 1; i++) {
                Stem stem = expand(tape, i, i + 1);
                if (stem.length >= MIN_STEM) {
                    hairpins++;
                    longest = Math.max(longest, stem.length);
                    at += stem.at;
                    gc += stem.gc;
                }
            }
        } else {
            for (int start = 0; start + loop <= n; start++) {
                if (start == 0 || start + loop >= n) {
                    continue;
                }
                Stem stem = expand(tape, start - 1, start + loop);
                if (stem.length >= MIN_STEM) {
                    hairpins++;
                    longest = Math.max(longest, stem.length);
                    at += stem.at;
                    gc += stem.gc;
                }
            }
        }
        return new LoopRow(loop, hairpins, longest, at, gc);
    }

    private static Stem expand(String tape, int left, int right) {
        int n = tape.length();
        int L = left;
        int R = right;
        int radius = 0;
        long at = 0;
        long gc = 0;
        while (L >= 0 && R < n && radius < MAX_RADIUS && complement(tape.charAt(L)) == tape.charAt(R)) {
            char leftBase = tape.charAt(L);
            if (leftBase == 'A' || leftBase == 'T') {
                at++;
            } else {
                gc++;
            }
            radius++;
            L--;
            R++;
        }
        return new Stem(2 * radius, at, gc);
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

    public List<LoopRow> rows() {
        return rows;
    }

    public int modalLoop() {
        return modalLoop;
    }

    public int modalHairpins() {
        return modalHairpins;
    }

    public int adjacent() {
        return adjacent;
    }

    public int gapped() {
        return gapped;
    }

    public int longestAdjacent() {
        return longestAdjacent;
    }

    public int longestGapped() {
        return longestGapped;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "modalLoop=%d modalHairpins=%d adjacent=%d gapped=%d longestAdj=%d longestGap=%d",
                modalLoop, modalHairpins, adjacent, gapped, longestAdjacent, longestGapped);
    }

    public record LoopRow(int loop, int hairpins, int longestStem, long atPairs, long gcPairs) {
    }

    private record Stem(int length, long at, long gc) {
    }
}
