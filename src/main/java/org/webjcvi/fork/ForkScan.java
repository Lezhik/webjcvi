package org.webjcvi.fork;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Residual twins at the near-unique prefix width. v19 found uniqueness
 * saturates sharply: unique share 0.7190 at k=8, 0.9997 at k=16, uniqueAt=20.
 * The last collisions at k=16 are the payload — near-duplicate wrap-frames
 * that a 16-character fingerprint still fails to distinguish, plus the
 * column where they first fork. Inverse of key-width (there uniqueAt was
 * the headline) and distinct from prefix-families (there k=8 produced 1290
 * headers). Newlines are dropped so the scan matches FASTA concatenation.
 */
public final class ForkScan {

    public static final int DEFAULT_WRAP = 70;
    public static final int DEFAULT_PREFIX = 16;
    public static final int MAX_WRAP = 256;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_HITS = 40;
    public static final int MIN_COUNT = 2;

    private static final Logger log = LoggerFactory.getLogger(ForkScan.class);

    public Scan scan(String text) {
        return scan(text, DEFAULT_WRAP, DEFAULT_PREFIX);
    }

    public Scan scan(String text, int wrapWidth, int prefixLength) {
        if (text == null) {
            throw new ForkException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new ForkException("Text exceeds " + MAX_CHARS + " characters");
        }
        int wrap = wrapWidth < DEFAULT_PREFIX ? DEFAULT_WRAP : Math.min(wrapWidth, MAX_WRAP);
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
        Map<String, Bucket> groups = new LinkedHashMap<>();
        int scanned = 0;
        for (int i = 0; i + wrap <= tape.length(); i += wrap) {
            String frame = tape.substring(i, i + wrap);
            String lead = frame.substring(0, prefix);
            scanned++;
            Bucket bucket = groups.get(lead);
            if (bucket == null) {
                groups.put(lead, new Bucket(i, frame));
            } else {
                bucket.members.add(frame);
            }
        }
        int twinCount = 0;
        int twinFrames = 0;
        int topFork = 0;
        int topCount = 0;
        List<Hit> candidates = new ArrayList<>();
        for (var entry : groups.entrySet()) {
            Bucket bucket = entry.getValue();
            int count = bucket.members.size();
            if (count < MIN_COUNT) {
                continue;
            }
            twinCount++;
            twinFrames += count;
            int forkAt = forkAt(bucket.members);
            if (count > topCount
                    || (count == topCount && (topFork == 0 || forkAt < topFork))) {
                topCount = count;
                topFork = forkAt;
            }
            candidates.add(new Hit(bucket.firstOffset, count, forkAt, entry.getKey()));
        }
        candidates.sort((a, b) -> {
            int byCount = Integer.compare(b.count(), a.count());
            if (byCount != 0) {
                return byCount;
            }
            int byFork = Integer.compare(a.forkAt(), b.forkAt());
            if (byFork != 0) {
                return byFork;
            }
            return a.prefix().compareTo(b.prefix());
        });
        List<Hit> hits = candidates.size() <= MAX_HITS
                ? List.copyOf(candidates)
                : List.copyOf(candidates.subList(0, MAX_HITS));
        double uniqueShare = scanned == 0 ? 0.0 : groups.size() * 1.0 / scanned;
        Scan scan = new Scan(wrap, prefix, scanned, groups.size(), twinCount, twinFrames, uniqueShare, topFork, hits);
        if (log.isDebugEnabled()) {
            log.debug("fork.scan chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    static int forkAt(List<String> members) {
        if (members.size() < MIN_COUNT) {
            return 0;
        }
        int wrap = members.get(0).length();
        for (int col = 0; col < wrap; col++) {
            char expected = members.get(0).charAt(col);
            for (int i = 1; i < members.size(); i++) {
                if (members.get(i).charAt(col) != expected) {
                    return col + 1;
                }
            }
        }
        return 0;
    }

    public record Scan(
            int wrapWidth,
            int prefixLength,
            int scanned,
            int distinct,
            int twinCount,
            int twinFrames,
            double uniqueShare,
            int topFork,
            List<Hit> hits) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "wrap=%d prefix=%d scanned=%d distinct=%d twinCount=%d twinFrames=%d uniqueShare=%.4f topFork=%d",
                    wrapWidth, prefixLength, scanned, distinct, twinCount, twinFrames, uniqueShare, topFork);
        }
    }

    public record Hit(int firstOffset, int count, int forkAt, String prefix) {
    }

    private static final class Bucket {
        private final int firstOffset;
        private final List<String> members = new ArrayList<>();

        private Bucket(int firstOffset, String frame) {
            this.firstOffset = firstOffset;
            this.members.add(frame);
        }
    }
}
