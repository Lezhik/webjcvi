package org.webjcvi.dna;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uniqueness of whole wrap-frames versus the 8-mer slot collisions that v16
 * treated as typed fields. If 70-mers almost never repeat while TTTTAAAA
 * already collides 13 times, the record — not the slot — is the identifier.
 */
public final class CloneCensus {

    private static final Logger log = LoggerFactory.getLogger(CloneCensus.class);

    private final int wrapWidth;
    private final int frames;
    private final int distinct;
    private final int cloneGroups;
    private final int cloneFrames;
    private final int topCount;
    private final String topFramePreview;

    private CloneCensus(
            int wrapWidth,
            int frames,
            int distinct,
            int cloneGroups,
            int cloneFrames,
            int topCount,
            String topFramePreview) {
        this.wrapWidth = wrapWidth;
        this.frames = frames;
        this.distinct = distinct;
        this.cloneGroups = cloneGroups;
        this.cloneFrames = cloneFrames;
        this.topCount = topCount;
        this.topFramePreview = topFramePreview;
    }

    public static CloneCensus from(DnaSequence sequence, int wrapWidth) {
        Objects.requireNonNull(sequence, "sequence");
        int frame = wrapWidth < 8 ? 70 : wrapWidth;
        if (log.isDebugEnabled()) {
            log.debug("clone.start length={} wrapWidth={}", sequence.length(), frame);
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
            String rec = tape.substring(i, i + frame);
            counts.merge(rec, 1, Integer::sum);
        }
        int cloneGroups = 0;
        int cloneFrames = 0;
        int topCount = 0;
        String topKey = "";
        for (var entry : counts.entrySet()) {
            int count = entry.getValue();
            if (count > topCount
                    || (count == topCount && (topKey.isEmpty() || entry.getKey().compareTo(topKey) < 0))) {
                topCount = count;
                topKey = entry.getKey();
            }
            if (count >= 2) {
                cloneGroups++;
                cloneFrames += count;
            }
        }
        String topPreview = topKey.length() <= 16 ? topKey : topKey.substring(0, 16);
        CloneCensus census = new CloneCensus(
                frame, frames, counts.size(), cloneGroups, cloneFrames, topCount, topPreview);
        if (log.isDebugEnabled()) {
            log.debug("clone.done {}", census.toTextRow());
        }
        return census;
    }

    public int wrapWidth() {
        return wrapWidth;
    }

    public int frames() {
        return frames;
    }

    public int distinct() {
        return distinct;
    }

    public int cloneGroups() {
        return cloneGroups;
    }

    public int cloneFrames() {
        return cloneFrames;
    }

    public int topCount() {
        return topCount;
    }

    public String topFramePreview() {
        return topFramePreview;
    }

    public double uniqueShare() {
        return frames == 0 ? 0.0 : distinct * 1.0 / frames;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "wrap=%d frames=%d distinct=%d cloneGroups=%d cloneFrames=%d topCount=%d unique=%.4f",
                wrapWidth, frames, distinct, cloneGroups, cloneFrames, topCount, uniqueShare());
    }
}
