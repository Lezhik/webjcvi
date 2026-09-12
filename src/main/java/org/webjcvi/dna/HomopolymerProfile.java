package org.webjcvi.dna;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Homopolymer run census on the normalized tape. The v1 report only counted
 * bases in a bag; this version measures consecutive repeats, which the 5'
 * preview showed as poly-A / poly-T.
 */
public final class HomopolymerProfile {

    public static final int SHORT = 5;
    public static final int MEDIUM = 10;
    public static final int LONG = 20;

    private static final Logger log = LoggerFactory.getLogger(HomopolymerProfile.class);

    private final Map<Character, Long> maxRun;
    private final long runsAtLeast5;
    private final long runs5to9;
    private final long runs10to19;
    private final long runs20plus;
    private final char longestBase;
    private final long longestLength;

    private HomopolymerProfile(
            Map<Character, Long> maxRun,
            long runsAtLeast5,
            long runs5to9,
            long runs10to19,
            long runs20plus,
            char longestBase,
            long longestLength) {
        this.maxRun = Map.copyOf(maxRun);
        this.runsAtLeast5 = runsAtLeast5;
        this.runs5to9 = runs5to9;
        this.runs10to19 = runs10to19;
        this.runs20plus = runs20plus;
        this.longestBase = longestBase;
        this.longestLength = longestLength;
    }

    public static HomopolymerProfile from(DnaSequence sequence) {
        Objects.requireNonNull(sequence, "sequence");
        if (log.isDebugEnabled()) {
            log.debug("homopolymer.start length={}", sequence.length());
        }
        Map<Character, Long> max = new LinkedHashMap<>();
        for (int i = 0; i < DnaSequence.CANONICAL_BASES.length(); i++) {
            max.put(DnaSequence.CANONICAL_BASES.charAt(i), 0L);
        }
        String bases = sequence.normalized();
        long ge5 = 0;
        long b5 = 0;
        long b10 = 0;
        long b20 = 0;
        char longestBase = '-';
        long longest = 0;
        int i = 0;
        while (i < bases.length()) {
            char ch = bases.charAt(i);
            int j = i + 1;
            while (j < bases.length() && bases.charAt(j) == ch) {
                j++;
            }
            int len = j - i;
            if (DnaSequence.CANONICAL_BASES.indexOf(ch) >= 0) {
                max.merge(ch, (long) len, Math::max);
                if (len > longest) {
                    longest = len;
                    longestBase = ch;
                }
                if (len >= SHORT) {
                    ge5++;
                    if (len >= LONG) {
                        b20++;
                    } else if (len >= MEDIUM) {
                        b10++;
                    } else {
                        b5++;
                    }
                }
            }
            i = j;
        }
        HomopolymerProfile profile = new HomopolymerProfile(max, ge5, b5, b10, b20, longestBase, longest);
        if (log.isDebugEnabled()) {
            log.debug("homopolymer.done ge5={} longest={}{}", ge5, longest, longestBase);
        }
        return profile;
    }

    public Map<Character, Long> maxRun() {
        return maxRun;
    }

    public long runsAtLeast5() {
        return runsAtLeast5;
    }

    public long runs5to9() {
        return runs5to9;
    }

    public long runs10to19() {
        return runs10to19;
    }

    public long runs20plus() {
        return runs20plus;
    }

    public char longestBase() {
        return longestBase;
    }

    public long longestLength() {
        return longestLength;
    }

    public String toTextTable() {
        StringBuilder text = new StringBuilder();
        text.append(String.format(Locale.ROOT, "longest=%s x %d  runs>=5=%d%n",
                longestBase, longestLength, runsAtLeast5));
        text.append("base\tmaxRun\n");
        maxRun.forEach((base, len) -> text.append(base).append('\t').append(len).append('\n'));
        text.append("5-9\t10-19\t20+\n");
        text.append(runs5to9).append('\t').append(runs10to19).append('\t').append(runs20plus).append('\n');
        return text.toString();
    }
}
