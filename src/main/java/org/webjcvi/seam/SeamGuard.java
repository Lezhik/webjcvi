package org.webjcvi.seam;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Wrap-seam anti-mirror: flag newline joints of full-width lines whose last
 * block is the reverse of the next line's first block, but not a copy. The
 * v13 wrap-vs-interior census showed wrap reverse-0 share 0.0104 vs interior
 * 0.0114 (wrap/interior 0.913) — palindromic glue is the depleted class at
 * display wraps, not the payload. Inverse of linear mirror-scan (there reverse
 * joints anywhere were hits). Default wrap 70 and block 4 are the measured
 * FASTA frame and pair size.
 */
public final class SeamGuard {

    public static final int DEFAULT_WRAP = 70;
    public static final int DEFAULT_BLOCK = 4;
    public static final int MAX_WRAP = 256;
    public static final int MAX_BLOCK = 32;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_HITS = 40;

    private static final Logger log = LoggerFactory.getLogger(SeamGuard.class);

    public Scan scan(String text) {
        return scan(text, DEFAULT_WRAP, DEFAULT_BLOCK);
    }

    public Scan scan(String text, int wrapWidth, int blockWidth) {
        if (text == null) {
            throw new SeamException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new SeamException("Text exceeds " + MAX_CHARS + " characters");
        }
        int wrap = wrapWidth < 8 ? DEFAULT_WRAP : Math.min(wrapWidth, MAX_WRAP);
        int block = blockWidth < 2 ? DEFAULT_BLOCK : Math.min(blockWidth, MAX_BLOCK);
        if (wrap < block) {
            wrap = block;
        }
        List<String> lines = splitLines(text);
        List<Hit> hits = new ArrayList<>();
        int scanned = 0;
        int hitCount = 0;
        for (int i = 0; i + 1 < lines.size(); i++) {
            String leftLine = lines.get(i);
            String rightLine = lines.get(i + 1);
            if (leftLine.length() < wrap || rightLine.length() < wrap) {
                continue;
            }
            scanned++;
            String left = leftLine.substring(leftLine.length() - block).toUpperCase(Locale.ROOT);
            String right = rightLine.substring(0, block).toUpperCase(Locale.ROOT);
            int identity = hamming(left, right, false);
            int reverse = hamming(left, right, true);
            if (reverse == 0 && identity > 0) {
                hitCount++;
                if (hits.size() < MAX_HITS) {
                    hits.add(new Hit(hits.size(), i, identity, left, right));
                }
            }
        }
        Scan scan = new Scan(wrap, block, scanned, hitCount, List.copyOf(hits));
        if (log.isDebugEnabled()) {
            log.debug("seam.scan chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    private static int hamming(String left, String right, boolean reverseRight) {
        int dist = 0;
        int n = left.length();
        for (int i = 0; i < n; i++) {
            char r = reverseRight ? right.charAt(n - 1 - i) : right.charAt(i);
            if (left.charAt(i) != r) {
                dist++;
            }
        }
        return dist;
    }

    private static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == '\n') {
                String line = text.substring(start, i);
                if (line.endsWith("\r")) {
                    line = line.substring(0, line.length() - 1);
                }
                if (!(i == text.length() && line.isEmpty() && !lines.isEmpty())) {
                    lines.add(line);
                }
                start = i + 1;
            }
        }
        return lines;
    }

    public record Scan(int wrapWidth, int blockWidth, int scanned, int hitCount, List<Hit> hits) {

        public String summary() {
            return String.format(Locale.ROOT, "wrap=%d block=%d scanned=%d hits=%d",
                    wrapWidth, blockWidth, scanned, hitCount);
        }
    }

    public record Hit(int index, int line, int identityDistance, String left, String right) {
    }
}
