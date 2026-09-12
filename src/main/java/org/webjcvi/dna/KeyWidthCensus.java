package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uniqueness of wrap-frame prefixes as a function of length. v18 showed
 * leading 8-mers collide (unique share 0.7190) while whole 70-mers never
 * do. This asks where uniqueness saturates — a sharp key width versus a
 * slow climb from AT entropy.
 */
public final class KeyWidthCensus {

    public static final int FLOOR = 8;
    public static final int[] SAMPLE_LENGTHS = {8, 16, 24, 32, 48, 70};

    private static final Logger log = LoggerFactory.getLogger(KeyWidthCensus.class);

    private final int wrapWidth;
    private final int floorLength;
    private final int frames;
    private final int uniqueAt;
    private final List<Row> samples;

    private KeyWidthCensus(int wrapWidth, int floorLength, int frames, int uniqueAt, List<Row> samples) {
        this.wrapWidth = wrapWidth;
        this.floorLength = floorLength;
        this.frames = frames;
        this.uniqueAt = uniqueAt;
        this.samples = samples;
    }

    public static KeyWidthCensus from(DnaSequence sequence, int wrapWidth) {
        Objects.requireNonNull(sequence, "sequence");
        int frame = wrapWidth < FLOOR ? 70 : wrapWidth;
        if (log.isDebugEnabled()) {
            log.debug("keywidth.start length={} wrapWidth={}", sequence.length(), frame);
        }
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < sequence.normalized().length(); i++) {
            char ch = sequence.normalized().charAt(i);
            if (ch == 'A' || ch == 'T' || ch == 'G' || ch == 'C') {
                canonical.append(ch);
            }
        }
        String tape = canonical.toString();
        List<String> frames = new ArrayList<>();
        for (int i = 0; i + frame <= tape.length(); i += frame) {
            frames.add(tape.substring(i, i + frame));
        }
        int uniqueAt = 0;
        if (!frames.isEmpty()) {
            for (int k = FLOOR; k <= frame; k++) {
                if (distinctCount(frames, k) == frames.size()) {
                    uniqueAt = k;
                    break;
                }
            }
        }
        List<Row> samples = new ArrayList<>();
        for (int k : SAMPLE_LENGTHS) {
            int length = Math.min(k, frame);
            samples.add(row(frames, length));
        }
        KeyWidthCensus census = new KeyWidthCensus(frame, FLOOR, frames.size(), uniqueAt, List.copyOf(samples));
        if (log.isDebugEnabled()) {
            log.debug("keywidth.done {}", census.toTextRow());
        }
        return census;
    }

    private static Row row(List<String> frames, int k) {
        int distinct = distinctCount(frames, k);
        double share = frames.isEmpty() ? 0.0 : distinct * 1.0 / frames.size();
        return new Row(k, distinct, share);
    }

    private static int distinctCount(List<String> frames, int k) {
        Map<String, Integer> counts = new HashMap<>();
        for (String frame : frames) {
            counts.merge(frame.substring(0, k), 1, Integer::sum);
        }
        return counts.size();
    }

    public int wrapWidth() {
        return wrapWidth;
    }

    public int floorLength() {
        return floorLength;
    }

    public int frames() {
        return frames;
    }

    public int uniqueAt() {
        return uniqueAt;
    }

    public List<Row> samples() {
        return samples;
    }

    public double shareAt(int length) {
        for (Row row : samples) {
            if (row.length() == length) {
                return row.uniqueShare();
            }
        }
        return 0.0;
    }

    public String toTextRow() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "wrap=%d floor=%d frames=%d uniqueAt=%d",
                wrapWidth, floorLength, frames, uniqueAt));
        for (Row row : samples) {
            sb.append(String.format(Locale.ROOT, " k%d=%.4f", row.length(), row.uniqueShare()));
        }
        return sb.toString();
    }

    public record Row(int length, int distinct, double uniqueShare) {
    }
}
