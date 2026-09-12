package org.webjcvi.contrast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Adjacent-window Hamming contrast. The v11 pairing-mismatch census showed
 * exact reverse-complement pairing is depleted (distance 0 ×0.846) while
 * the mode is distance 3 of 4 and distance 4 is enriched (×1.099) — neighbors
 * systematically differ. Inverse of fuzzy find (there a motif may almost
 * match anywhere; here consecutive blocks should not almost match).
 */
public final class BlockContrast {

    public static final int DEFAULT_WIDTH = 4;
    public static final int DEFAULT_FLAG_MAX = 1;
    public static final int MAX_WIDTH = 32;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_HITS = 40;

    private static final Logger log = LoggerFactory.getLogger(BlockContrast.class);

    public Scan scan(String text) {
        return scan(text, DEFAULT_WIDTH, DEFAULT_FLAG_MAX);
    }

    public Scan scan(String text, int width, int flagMax) {
        if (text == null) {
            throw new ContrastException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new ContrastException("Text exceeds " + MAX_CHARS + " characters");
        }
        int w = width < 2 ? DEFAULT_WIDTH : Math.min(width, MAX_WIDTH);
        int cap = flagMax < 0 ? DEFAULT_FLAG_MAX : Math.min(flagMax, w);
        String folded = text.toUpperCase(Locale.ROOT);
        int scanned = Math.max(0, folded.length() - 2 * w + 1);
        int[] hist = new int[w + 1];
        List<Hit> hits = new ArrayList<>();
        int stutterCount = 0;
        long distSum = 0;
        for (int i = 0; i < scanned; i++) {
            int dist = hamming(folded, i, i + w, w);
            hist[dist]++;
            distSum += dist;
            if (dist <= cap) {
                stutterCount++;
                if (hits.size() < MAX_HITS) {
                    hits.add(new Hit(
                            hits.size(),
                            i,
                            dist,
                            folded.substring(i, i + w),
                            folded.substring(i + w, i + 2 * w)));
                }
            }
        }
        int modal = 0;
        int best = -1;
        for (int d = 0; d <= w; d++) {
            if (hist[d] > best) {
                best = hist[d];
                modal = d;
            }
        }
        double mean = scanned == 0 ? 0.0 : distSum * 1.0 / scanned;
        Scan scan = new Scan(w, scanned, stutterCount, modal, mean, cap, List.copyOf(hits));
        if (log.isDebugEnabled()) {
            log.debug("contrast.scan chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    private static int hamming(String folded, int left, int right, int width) {
        int dist = 0;
        for (int i = 0; i < width; i++) {
            if (folded.charAt(left + i) != folded.charAt(right + i)) {
                dist++;
            }
        }
        return dist;
    }

    public record Scan(
            int width,
            int scanned,
            int stutterCount,
            int modalDistance,
            double meanDistance,
            int flagMax,
            List<Hit> hits) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "width=%d scanned=%d stutters=%d modalDist=%d mean=%.2f flagMax=%d",
                    width, scanned, stutterCount, modalDistance, meanDistance, flagMax);
        }
    }

    public record Hit(int index, int offset, int distance, String left, String right) {
    }
}
