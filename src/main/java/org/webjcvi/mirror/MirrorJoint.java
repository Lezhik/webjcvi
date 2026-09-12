package org.webjcvi.mirror;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Adjacent reverse joints: the next block is the reverse of the current one,
 * but not a copy. The v12 neighbor census showed reverse distance-0 enriched
 * (×1.130) while identity (×0.891) and RC (×0.846) are depleted — the
 * protocol is a mirror seam, not a stutter and not complementary pairing.
 * Inverse of block contrast (there identity-low was the hit).
 */
public final class MirrorJoint {

    public static final int DEFAULT_WIDTH = 4;
    public static final int MAX_WIDTH = 32;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_HITS = 40;

    private static final Logger log = LoggerFactory.getLogger(MirrorJoint.class);

    public Scan scan(String text) {
        return scan(text, DEFAULT_WIDTH);
    }

    public Scan scan(String text, int width) {
        if (text == null) {
            throw new MirrorException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new MirrorException("Text exceeds " + MAX_CHARS + " characters");
        }
        int w = width < 2 ? DEFAULT_WIDTH : Math.min(width, MAX_WIDTH);
        String folded = text.toUpperCase(Locale.ROOT);
        int scanned = Math.max(0, folded.length() - 2 * w + 1);
        List<Hit> hits = new ArrayList<>();
        int jointCount = 0;
        for (int i = 0; i < scanned; i++) {
            int identity = hamming(folded, i, i + w, w, false);
            int reverse = hamming(folded, i, i + w, w, true);
            if (reverse == 0 && identity > 0) {
                jointCount++;
                if (hits.size() < MAX_HITS) {
                    hits.add(new Hit(
                            hits.size(),
                            i,
                            identity,
                            folded.substring(i, i + w),
                            folded.substring(i + w, i + 2 * w)));
                }
            }
        }
        Scan scan = new Scan(w, scanned, jointCount, List.copyOf(hits));
        if (log.isDebugEnabled()) {
            log.debug("mirror.scan chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    private static int hamming(String folded, int left, int right, int width, boolean reverseRight) {
        int dist = 0;
        for (int i = 0; i < width; i++) {
            char l = folded.charAt(left + i);
            int rightIndex = reverseRight ? right + width - 1 - i : right + i;
            if (l != folded.charAt(rightIndex)) {
                dist++;
            }
        }
        return dist;
    }

    public record Scan(int width, int scanned, int jointCount, List<Hit> hits) {

        public String summary() {
            return String.format(Locale.ROOT, "width=%d scanned=%d joints=%d", width, scanned, jointCount);
        }
    }

    public record Hit(int index, int offset, int identityDistance, String left, String right) {
    }
}
