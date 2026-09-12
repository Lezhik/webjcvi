package org.webjcvi.dna;

import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Two-class islands on the linear tape: consecutive GC (minority, ~24%) versus
 * consecutive AT (majority). Homopolymers count one base; Chargaff windows mix
 * both classes inside a frame. This census asks how the rare class clusters.
 */
public final class GcIslandCensus {

    private static final Logger log = LoggerFactory.getLogger(GcIslandCensus.class);

    private final int gcIslands;
    private final int atIslands;
    private final int longestGc;
    private final int longestAt;
    private final int gcIslandsAtLeast5;
    private final int gcIslandsAtLeast10;
    private final long gcBasesInIslands;

    private GcIslandCensus(
            int gcIslands,
            int atIslands,
            int longestGc,
            int longestAt,
            int gcIslandsAtLeast5,
            int gcIslandsAtLeast10,
            long gcBasesInIslands) {
        this.gcIslands = gcIslands;
        this.atIslands = atIslands;
        this.longestGc = longestGc;
        this.longestAt = longestAt;
        this.gcIslandsAtLeast5 = gcIslandsAtLeast5;
        this.gcIslandsAtLeast10 = gcIslandsAtLeast10;
        this.gcBasesInIslands = gcBasesInIslands;
    }

    public static GcIslandCensus from(DnaSequence sequence) {
        Objects.requireNonNull(sequence, "sequence");
        if (log.isDebugEnabled()) {
            log.debug("gc-island.start length={}", sequence.length());
        }
        String bases = sequence.normalized();
        int gcIslands = 0;
        int atIslands = 0;
        int longestGc = 0;
        int longestAt = 0;
        int ge5 = 0;
        int ge10 = 0;
        long gcMass = 0;
        int i = 0;
        while (i < bases.length()) {
            char ch = bases.charAt(i);
            boolean gc = ch == 'G' || ch == 'C';
            boolean at = ch == 'A' || ch == 'T';
            if (!gc && !at) {
                i++;
                continue;
            }
            int j = i + 1;
            while (j < bases.length()) {
                char next = bases.charAt(j);
                boolean nextGc = next == 'G' || next == 'C';
                boolean nextAt = next == 'A' || next == 'T';
                if (gc && nextGc || at && nextAt) {
                    j++;
                } else {
                    break;
                }
            }
            int len = j - i;
            if (gc) {
                gcIslands++;
                longestGc = Math.max(longestGc, len);
                gcMass += len;
                if (len >= 5) {
                    ge5++;
                }
                if (len >= 10) {
                    ge10++;
                }
            } else {
                atIslands++;
                longestAt = Math.max(longestAt, len);
            }
            i = j;
        }
        GcIslandCensus census = new GcIslandCensus(gcIslands, atIslands, longestGc, longestAt, ge5, ge10, gcMass);
        if (log.isDebugEnabled()) {
            log.debug("gc-island.done gcIslands={} atIslands={} longestGc={}", gcIslands, atIslands, longestGc);
        }
        return census;
    }

    public int gcIslands() {
        return gcIslands;
    }

    public int atIslands() {
        return atIslands;
    }

    public int longestGc() {
        return longestGc;
    }

    public int longestAt() {
        return longestAt;
    }

    public int gcIslandsAtLeast5() {
        return gcIslandsAtLeast5;
    }

    public int gcIslandsAtLeast10() {
        return gcIslandsAtLeast10;
    }

    public long gcBasesInIslands() {
        return gcBasesInIslands;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "gcIslands=%d longestGc=%d ge5=%d ge10=%d atIslands=%d longestAt=%d",
                gcIslands, longestGc, gcIslandsAtLeast5, gcIslandsAtLeast10, atIslands, longestAt);
    }
}
