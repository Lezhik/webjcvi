package org.webjcvi.dna;

import java.util.Locale;
import java.util.Objects;

/**
 * Reverse-complement foldback stems versus same-base palindromes. Forward
 * same-base lags were nearly independent (peak ×1.134); this asks whether
 * identity instead folds (A↔T, G↔C around a center).
 */
public final class FoldbackCensus {

    public static final int MIN_STEM = 4;
    public static final int MAX_RADIUS = 200;

    private final int rcEven;
    private final int rcOdd;
    private final int longestRc;
    private final long rcAtPairs;
    private final long rcGcPairs;
    private final int sameEven;
    private final int longestSame;

    private FoldbackCensus(
            int rcEven,
            int rcOdd,
            int longestRc,
            long rcAtPairs,
            long rcGcPairs,
            int sameEven,
            int longestSame) {
        this.rcEven = rcEven;
        this.rcOdd = rcOdd;
        this.longestRc = longestRc;
        this.rcAtPairs = rcAtPairs;
        this.rcGcPairs = rcGcPairs;
        this.sameEven = sameEven;
        this.longestSame = longestSame;
    }

    public static FoldbackCensus from(DnaSequence sequence) {
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
        int rcEven = 0;
        int rcOdd = 0;
        int longestRc = 0;
        long rcAtPairs = 0;
        long rcGcPairs = 0;
        int sameEven = 0;
        int longestSame = 0;
        for (int i = 0; i < n; i++) {
            int evenRc = expandEven(tape, i, true);
            if (evenRc >= MIN_STEM) {
                rcEven++;
                longestRc = Math.max(longestRc, evenRc);
                PairCount pairs = pairCount(tape, i - evenRc / 2 + 1, evenRc);
                rcAtPairs += pairs.at;
                rcGcPairs += pairs.gc;
            }
            int oddRc = expandOdd(tape, i, true);
            if (oddRc >= MIN_STEM) {
                rcOdd++;
                longestRc = Math.max(longestRc, oddRc);
                PairCount pairs = pairCountOdd(tape, i, oddRc);
                rcAtPairs += pairs.at;
                rcGcPairs += pairs.gc;
            }
            int evenSame = expandEven(tape, i, false);
            if (evenSame >= MIN_STEM && !homopolymer(tape, i - evenSame / 2 + 1, evenSame)) {
                sameEven++;
                longestSame = Math.max(longestSame, evenSame);
            }
        }
        return new FoldbackCensus(rcEven, rcOdd, longestRc, rcAtPairs, rcGcPairs, sameEven, longestSame);
    }

    private static int expandEven(String tape, int leftOfCenter, boolean complement) {
        int n = tape.length();
        int L = leftOfCenter;
        int R = leftOfCenter + 1;
        int radius = 0;
        while (L >= 0 && R < n && radius < MAX_RADIUS && match(tape.charAt(L), tape.charAt(R), complement)) {
            radius++;
            L--;
            R++;
        }
        return 2 * radius;
    }

    private static int expandOdd(String tape, int center, boolean complement) {
        int n = tape.length();
        int radius = 0;
        while (center - radius - 1 >= 0 && center + radius + 1 < n && radius + 1 <= MAX_RADIUS
                && match(tape.charAt(center - radius - 1), tape.charAt(center + radius + 1), complement)) {
            radius++;
        }
        return radius == 0 ? 0 : 2 * radius + 1;
    }

    private static boolean match(char a, char b, boolean complement) {
        if (complement) {
            return complementOf(a) == b;
        }
        return a == b;
    }

    private static char complementOf(char ch) {
        return switch (ch) {
            case 'A' -> 'T';
            case 'T' -> 'A';
            case 'G' -> 'C';
            case 'C' -> 'G';
            default -> 0;
        };
    }

    private static boolean homopolymer(String tape, int offset, int len) {
        if (offset < 0 || offset + len > tape.length()) {
            return true;
        }
        char first = tape.charAt(offset);
        for (int i = 1; i < len; i++) {
            if (tape.charAt(offset + i) != first) {
                return false;
            }
        }
        return true;
    }

    private static PairCount pairCount(String tape, int offset, int len) {
        long at = 0;
        long gc = 0;
        int pairs = len / 2;
        for (int i = 0; i < pairs; i++) {
            char left = tape.charAt(offset + i);
            if (left == 'A' || left == 'T') {
                at++;
            } else {
                gc++;
            }
        }
        return new PairCount(at, gc);
    }

    private static PairCount pairCountOdd(String tape, int center, int len) {
        int radius = (len - 1) / 2;
        long at = 0;
        long gc = 0;
        for (int i = 1; i <= radius; i++) {
            char left = tape.charAt(center - i);
            if (left == 'A' || left == 'T') {
                at++;
            } else {
                gc++;
            }
        }
        return new PairCount(at, gc);
    }

    public int rcEven() {
        return rcEven;
    }

    public int rcOdd() {
        return rcOdd;
    }

    public int rcStems() {
        return rcEven + rcOdd;
    }

    public int longestRc() {
        return longestRc;
    }

    public long rcAtPairs() {
        return rcAtPairs;
    }

    public long rcGcPairs() {
        return rcGcPairs;
    }

    public int sameEven() {
        return sameEven;
    }

    public int longestSame() {
        return longestSame;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "rcStems=%d longestRc=%d even=%d odd=%d atPairs=%d gcPairs=%d sameEven=%d longestSame=%d",
                rcStems(), longestRc, rcEven, rcOdd, rcAtPairs, rcGcPairs, sameEven, longestSame);
    }

    private record PairCount(long at, long gc) {
    }
}
