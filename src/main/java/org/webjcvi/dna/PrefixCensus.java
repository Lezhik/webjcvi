package org.webjcvi.dna;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared leading tiles of wrap-frames. v17 showed every 70-mer is unique;
 * this asks whether a leading 8-mer (the slot width that still collided as
 * TTTTAAAA×13) repeats as a header while the remainder stays unique.
 */
public final class PrefixCensus {

    public static final int PREFIX = 8;

    private static final Logger log = LoggerFactory.getLogger(PrefixCensus.class);

    private final int wrapWidth;
    private final int prefixLength;
    private final int frames;
    private final int distinct;
    private final int familyCount;
    private final int familyFrames;
    private final int topCount;
    private final String topPrefix;

    private PrefixCensus(
            int wrapWidth,
            int prefixLength,
            int frames,
            int distinct,
            int familyCount,
            int familyFrames,
            int topCount,
            String topPrefix) {
        this.wrapWidth = wrapWidth;
        this.prefixLength = prefixLength;
        this.frames = frames;
        this.distinct = distinct;
        this.familyCount = familyCount;
        this.familyFrames = familyFrames;
        this.topCount = topCount;
        this.topPrefix = topPrefix;
    }

    public static PrefixCensus from(DnaSequence sequence, int wrapWidth) {
        Objects.requireNonNull(sequence, "sequence");
        int frame = wrapWidth < PREFIX ? 70 : wrapWidth;
        if (log.isDebugEnabled()) {
            log.debug("prefix.start length={} wrapWidth={} prefix={}", sequence.length(), frame, PREFIX);
        }
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < sequence.normalized().length(); i++) {
            char ch = sequence.normalized().charAt(i);
            if (ch == 'A' || ch == 'T' || ch == 'G' || ch == 'C') {
                canonical.append(ch);
            }
        }
        String tape = canonical.toString();
        Map<String, Integer> counts = new LinkedHashMap<>();
        int frames = 0;
        for (int i = 0; i + frame <= tape.length(); i += frame) {
            frames++;
            String lead = tape.substring(i, i + PREFIX);
            counts.merge(lead, 1, Integer::sum);
        }
        int familyCount = 0;
        int familyFrames = 0;
        int topCount = 0;
        String topPrefix = "";
        for (var entry : counts.entrySet()) {
            int count = entry.getValue();
            if (count > topCount
                    || (count == topCount && (topPrefix.isEmpty() || entry.getKey().compareTo(topPrefix) < 0))) {
                topCount = count;
                topPrefix = entry.getKey();
            }
            if (count >= 2) {
                familyCount++;
                familyFrames += count;
            }
        }
        PrefixCensus census = new PrefixCensus(
                frame, PREFIX, frames, counts.size(), familyCount, familyFrames, topCount, topPrefix);
        if (log.isDebugEnabled()) {
            log.debug("prefix.done {}", census.toTextRow());
        }
        return census;
    }

    public int wrapWidth() {
        return wrapWidth;
    }

    public int prefixLength() {
        return prefixLength;
    }

    public int frames() {
        return frames;
    }

    public int distinct() {
        return distinct;
    }

    public int familyCount() {
        return familyCount;
    }

    public int familyFrames() {
        return familyFrames;
    }

    public int topCount() {
        return topCount;
    }

    public String topPrefix() {
        return topPrefix;
    }

    public double uniqueShare() {
        return frames == 0 ? 0.0 : distinct * 1.0 / frames;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "wrap=%d prefix=%d frames=%d distinct=%d families=%d familyFrames=%d topCount=%d top=%s unique=%.4f",
                wrapWidth, prefixLength, frames, distinct, familyCount, familyFrames, topCount,
                topPrefix.isEmpty() ? "-" : topPrefix, uniqueShare());
    }
}
