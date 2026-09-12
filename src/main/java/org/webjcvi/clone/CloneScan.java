package org.webjcvi.clone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Duplicate wrap-frame scanner. The v16 slot census showed the three peak
 * columns are the same AT background (GC% 24.03 / 23.88 / 24.09 vs global
 * 24.14; mode 8-mers TTTTAAAA×13, TTTTTATT×12, TTTTTTAA×10 of 7592). Typed
 * fields are abandoned; uniqueness of the whole width-70 record is the
 * protocol. List frames that occur at least twice. Newlines are dropped so
 * the scan matches FASTA concatenation. Inverse of frame-field extraction
 * (there the mode of a column was the payload).
 */
public final class CloneScan {

    public static final int DEFAULT_WRAP = 70;
    public static final int MAX_WRAP = 256;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_HITS = 40;
    public static final int MIN_COUNT = 2;

    private static final Logger log = LoggerFactory.getLogger(CloneScan.class);

    public Scan scan(String text) {
        return scan(text, DEFAULT_WRAP);
    }

    public Scan scan(String text, int wrapWidth) {
        if (text == null) {
            throw new CloneException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new CloneException("Text exceeds " + MAX_CHARS + " characters");
        }
        int wrap = wrapWidth < 8 ? DEFAULT_WRAP : Math.min(wrapWidth, MAX_WRAP);
        StringBuilder folded = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n' || ch == '\r') {
                continue;
            }
            folded.append(Character.toUpperCase(ch));
        }
        String tape = folded.toString();
        Map<String, Counter> counts = new LinkedHashMap<>();
        int scanned = 0;
        for (int i = 0; i + wrap <= tape.length(); i += wrap) {
            String frame = tape.substring(i, i + wrap);
            scanned++;
            Counter counter = counts.get(frame);
            if (counter == null) {
                counts.put(frame, new Counter(i));
            } else {
                counter.count++;
            }
        }
        int cloneGroups = 0;
        int cloneFrames = 0;
        int topCount = 0;
        String topFrame = "";
        List<Hit> candidates = new ArrayList<>();
        for (var entry : counts.entrySet()) {
            Counter counter = entry.getValue();
            if (counter.count > topCount
                    || (counter.count == topCount && (topFrame.isEmpty() || entry.getKey().compareTo(topFrame) < 0))) {
                topCount = counter.count;
                topFrame = entry.getKey();
            }
            if (counter.count >= MIN_COUNT) {
                cloneGroups++;
                cloneFrames += counter.count;
                candidates.add(new Hit(counter.firstOffset, counter.count, entry.getKey()));
            }
        }
        candidates.sort((a, b) -> {
            int byCount = Integer.compare(b.count(), a.count());
            if (byCount != 0) {
                return byCount;
            }
            return a.frame().compareTo(b.frame());
        });
        List<Hit> hits = candidates.size() <= MAX_HITS
                ? List.copyOf(candidates)
                : List.copyOf(candidates.subList(0, MAX_HITS));
        Scan scan = new Scan(
                wrap,
                scanned,
                counts.size(),
                cloneGroups,
                cloneFrames,
                topCount,
                topFrame,
                hits);
        if (log.isDebugEnabled()) {
            log.debug("clone.scan chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    public record Scan(
            int wrapWidth,
            int scanned,
            int distinct,
            int cloneGroups,
            int cloneFrames,
            int topCount,
            String topFrame,
            List<Hit> hits) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "wrap=%d scanned=%d distinct=%d cloneGroups=%d cloneFrames=%d topCount=%d",
                    wrapWidth, scanned, distinct, cloneGroups, cloneFrames, topCount);
        }
    }

    public record Hit(int firstOffset, int count, String frame) {

        public String preview() {
            if (frame.length() <= 48) {
                return frame;
            }
            return frame.substring(0, 48) + "…";
        }
    }

    private static final class Counter {
        private final int firstOffset;
        private int count = 1;

        private Counter(int firstOffset) {
            this.firstOffset = firstOffset;
        }
    }
}
