package org.webjcvi.dna;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composition of the three wrap-frame slots that v15 assigned to different
 * neighbor maps (RC column 4, reverse 19, identity 58). Asks whether those
 * 8-mer fields actually differ in GC% and mode k-mer, or are the same AT
 * background sampled at three offsets.
 */
public final class SlotCensus {

    public static final int SLOT = 8;
    public static final int RC_COLUMN = 4;
    public static final int REVERSE_COLUMN = 19;
    public static final int IDENTITY_COLUMN = 58;

    private static final Logger log = LoggerFactory.getLogger(SlotCensus.class);

    private final int wrapWidth;
    private final int frames;
    private final SlotRow rc;
    private final SlotRow reverse;
    private final SlotRow identity;

    private SlotCensus(int wrapWidth, int frames, SlotRow rc, SlotRow reverse, SlotRow identity) {
        this.wrapWidth = wrapWidth;
        this.frames = frames;
        this.rc = rc;
        this.reverse = reverse;
        this.identity = identity;
    }

    public static SlotCensus from(DnaSequence sequence, int wrapWidth) {
        Objects.requireNonNull(sequence, "sequence");
        int frame = wrapWidth < IDENTITY_COLUMN + SLOT ? 70 : wrapWidth;
        if (log.isDebugEnabled()) {
            log.debug("slot.start length={} wrapWidth={}", sequence.length(), frame);
        }
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < sequence.normalized().length(); i++) {
            char ch = sequence.normalized().charAt(i);
            if (ch == 'A' || ch == 'T' || ch == 'G' || ch == 'C') {
                canonical.append(ch);
            }
        }
        String tape = canonical.toString();
        Counter rc = new Counter();
        Counter reverse = new Counter();
        Counter identity = new Counter();
        int frames = 0;
        for (int i = 0; i + frame <= tape.length(); i += frame) {
            frames++;
            rc.add(tape.substring(i + RC_COLUMN, i + RC_COLUMN + SLOT));
            reverse.add(tape.substring(i + REVERSE_COLUMN, i + REVERSE_COLUMN + SLOT));
            identity.add(tape.substring(i + IDENTITY_COLUMN, i + IDENTITY_COLUMN + SLOT));
        }
        SlotCensus census = new SlotCensus(frame, frames, rc.toRow("rc"), reverse.toRow("reverse"), identity.toRow("identity"));
        if (log.isDebugEnabled()) {
            log.debug("slot.done {}", census.toTextRow());
        }
        return census;
    }

    public int wrapWidth() {
        return wrapWidth;
    }

    public int frames() {
        return frames;
    }

    public SlotRow rc() {
        return rc;
    }

    public SlotRow reverse() {
        return reverse;
    }

    public SlotRow identity() {
        return identity;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT, "wrap=%d frames=%d %s %s %s",
                wrapWidth, frames, rc.toText(), reverse.toText(), identity.toText());
    }

    public record SlotRow(String name, int windows, double gcPercent, String topKmer, long topCount) {

        String toText() {
            return String.format(Locale.ROOT, "%s{gc=%.2f top=%s×%d}", name, gcPercent, topKmer, topCount);
        }
    }

    private static final class Counter {
        private long gc;
        private long bases;
        private final Map<String, Long> kmers = new LinkedHashMap<>();

        void add(String slot) {
            kmers.merge(slot, 1L, Long::sum);
            for (int i = 0; i < slot.length(); i++) {
                bases++;
                char ch = slot.charAt(i);
                if (ch == 'G' || ch == 'C') {
                    gc++;
                }
            }
        }

        SlotRow toRow(String name) {
            String top = "";
            long topCount = 0L;
            for (var entry : kmers.entrySet()) {
                if (entry.getValue() > topCount
                        || (entry.getValue() == topCount && (top.isEmpty() || entry.getKey().compareTo(top) < 0))) {
                    top = entry.getKey();
                    topCount = entry.getValue();
                }
            }
            double gcPercent = bases == 0 ? 0.0 : gc * 100.0 / bases;
            return new SlotRow(name, kmers.isEmpty() ? 0 : (int) kmers.values().stream().mapToLong(Long::longValue).sum(),
                    gcPercent, top, topCount);
        }
    }
}
