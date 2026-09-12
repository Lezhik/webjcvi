package org.webjcvi.prefix;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Prefix-family scanner. The v17 clone census found every width-70 wrap
 * frame unique (7592 distinct of 7592, clone groups 0) while 8-mer slots
 * still collide (TTTTAAAA×13). Exact copies are forbidden; a shared
 * leading 8-mer (the measured slot width) plus a unique remainder is the
 * protocol. Group frames by their prefix and list prefixes that occur at
 * least twice. Newlines are dropped so the scan matches FASTA
 * concatenation. Inverse of clone-scan (there the whole frame had to match).
 */
public final class PrefixGroup {

    public static final int DEFAULT_WRAP = 70;
    public static final int DEFAULT_PREFIX = 8;
    public static final int MAX_WRAP = 256;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_HITS = 40;
    public static final int MIN_COUNT = 2;

    private static final Logger log = LoggerFactory.getLogger(PrefixGroup.class);

    public Scan group(String text) {
        return group(text, DEFAULT_WRAP, DEFAULT_PREFIX);
    }

    public Scan group(String text, int wrapWidth, int prefixLength) {
        if (text == null) {
            throw new PrefixException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new PrefixException("Text exceeds " + MAX_CHARS + " characters");
        }
        int wrap = wrapWidth < 8 ? DEFAULT_WRAP : Math.min(wrapWidth, MAX_WRAP);
        int prefix = prefixLength < 1 || prefixLength > wrap ? DEFAULT_PREFIX : prefixLength;
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
            String lead = tape.substring(i, i + prefix);
            scanned++;
            Counter counter = counts.get(lead);
            if (counter == null) {
                counts.put(lead, new Counter(i));
            } else {
                counter.count++;
            }
        }
        int familyCount = 0;
        int familyFrames = 0;
        int topCount = 0;
        String topPrefix = "";
        List<Hit> candidates = new ArrayList<>();
        for (var entry : counts.entrySet()) {
            Counter counter = entry.getValue();
            if (counter.count > topCount
                    || (counter.count == topCount && (topPrefix.isEmpty() || entry.getKey().compareTo(topPrefix) < 0))) {
                topCount = counter.count;
                topPrefix = entry.getKey();
            }
            if (counter.count >= MIN_COUNT) {
                familyCount++;
                familyFrames += counter.count;
                candidates.add(new Hit(counter.firstOffset, counter.count, entry.getKey()));
            }
        }
        candidates.sort((a, b) -> {
            int byCount = Integer.compare(b.count(), a.count());
            if (byCount != 0) {
                return byCount;
            }
            return a.prefix().compareTo(b.prefix());
        });
        List<Hit> hits = candidates.size() <= MAX_HITS
                ? List.copyOf(candidates)
                : List.copyOf(candidates.subList(0, MAX_HITS));
        Scan scan = new Scan(
                wrap,
                prefix,
                scanned,
                counts.size(),
                familyCount,
                familyFrames,
                topCount,
                topPrefix,
                hits);
        if (log.isDebugEnabled()) {
            log.debug("prefix.group chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    public record Scan(
            int wrapWidth,
            int prefixLength,
            int scanned,
            int distinct,
            int familyCount,
            int familyFrames,
            int topCount,
            String topPrefix,
            List<Hit> hits) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "wrap=%d prefix=%d scanned=%d distinct=%d familyCount=%d familyFrames=%d topCount=%d topPrefix=%s",
                    wrapWidth, prefixLength, scanned, distinct, familyCount, familyFrames, topCount,
                    topPrefix == null || topPrefix.isEmpty() ? "-" : topPrefix);
        }
    }

    public record Hit(int firstOffset, int count, String prefix) {
    }

    private static final class Counter {
        private final int firstOffset;
        private int count = 1;

        private Counter(int firstOffset) {
            this.firstOffset = firstOffset;
        }
    }
}
