package org.webjcvi.phase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Fixed-phase reverse joints: only windows whose start offset modulo wrap
 * width equals the peak reverse phase. The v14 phase census peaked at column
 * <strong>19</strong> (share 0.0140 vs mean 0.0114) while wrap column 66 was
 * depleted (wrap/mean 0.913) — reverse is a field inside the frame, not a
 * wrap seam. Inverse of seam guard. Newlines are dropped so the scan matches
 * FASTA concatenation. Default wrap 70, phase 19, block 4.
 */
public final class PhaseJoint {

    public static final int DEFAULT_WRAP = 70;
    public static final int DEFAULT_PHASE = 19;
    public static final int DEFAULT_BLOCK = 4;
    public static final int MAX_WRAP = 256;
    public static final int MAX_BLOCK = 32;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_HITS = 40;

    private static final Logger log = LoggerFactory.getLogger(PhaseJoint.class);

    public Scan scan(String text) {
        return scan(text, DEFAULT_WRAP, DEFAULT_PHASE, DEFAULT_BLOCK);
    }

    public Scan scan(String text, int wrapWidth, int phase, int blockWidth) {
        if (text == null) {
            throw new PhaseException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new PhaseException("Text exceeds " + MAX_CHARS + " characters");
        }
        int wrap = wrapWidth < 8 ? DEFAULT_WRAP : Math.min(wrapWidth, MAX_WRAP);
        int block = blockWidth < 2 ? DEFAULT_BLOCK : Math.min(blockWidth, MAX_BLOCK);
        int column = phase < 0 ? DEFAULT_PHASE : phase % wrap;
        StringBuilder folded = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n' || ch == '\r') {
                continue;
            }
            folded.append(Character.toUpperCase(ch));
        }
        String tape = folded.toString();
        int span = block * 2;
        List<Hit> hits = new ArrayList<>();
        int scanned = 0;
        int hitCount = 0;
        for (int i = column; i + span <= tape.length(); i += wrap) {
            scanned++;
            int identity = hamming(tape, i, block, false);
            int reverse = hamming(tape, i, block, true);
            if (reverse == 0 && identity > 0) {
                hitCount++;
                if (hits.size() < MAX_HITS) {
                    hits.add(new Hit(
                            hits.size(),
                            i,
                            identity,
                            tape.substring(i, i + block),
                            tape.substring(i + block, i + span)));
                }
            }
        }
        Scan scan = new Scan(wrap, column, block, scanned, hitCount, List.copyOf(hits));
        if (log.isDebugEnabled()) {
            log.debug("phase.scan chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    private static int hamming(String tape, int left, int width, boolean reverseRight) {
        int dist = 0;
        int right = left + width;
        for (int i = 0; i < width; i++) {
            char r = reverseRight ? tape.charAt(right + width - 1 - i) : tape.charAt(right + i);
            if (tape.charAt(left + i) != r) {
                dist++;
            }
        }
        return dist;
    }

    public record Scan(int wrapWidth, int phase, int blockWidth, int scanned, int hitCount, List<Hit> hits) {

        public String summary() {
            return String.format(Locale.ROOT, "wrap=%d phase=%d block=%d scanned=%d hits=%d",
                    wrapWidth, phase, blockWidth, scanned, hitCount);
        }
    }

    public record Hit(int index, int offset, int identityDistance, String left, String right) {
    }
}
