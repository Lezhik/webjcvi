package org.webjcvi.affix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Lead versus tail identifier width of wrap-frames. v20 found trailing
 * uniqueAt=17 with k=8/16 unique shares 0.7160/0.9997 — essentially the
 * same curve as the leading uniqueAt=20 / 0.7190 / 0.9997. Uniqueness is
 * end-symmetric entropy, not a front-loaded header, so the cheaper
 * discriminating end must be measured, not assumed. Inverse of key-width
 * (there only the leading uniqueAt was the headline) and of fork-scan
 * (there only leading 16-mer twins were listed). Newlines are dropped so
 * the scan matches FASTA concatenation.
 */
public final class AffixScan {

    public static final int DEFAULT_WRAP = 70;
    public static final int FLOOR = 8;
    public static final int NEAR = 16;
    public static final int MAX_WRAP = 256;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;

    private static final Logger log = LoggerFactory.getLogger(AffixScan.class);

    public Scan measure(String text) {
        return measure(text, DEFAULT_WRAP);
    }

    public Scan measure(String text, int wrapWidth) {
        if (text == null) {
            throw new AffixException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new AffixException("Text exceeds " + MAX_CHARS + " characters");
        }
        int wrap = wrapWidth < FLOOR ? DEFAULT_WRAP : Math.min(wrapWidth, MAX_WRAP);
        StringBuilder folded = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n' || ch == '\r') {
                continue;
            }
            folded.append(Character.toUpperCase(ch));
        }
        String tape = folded.toString();
        List<String> frames = new ArrayList<>();
        for (int i = 0; i + wrap <= tape.length(); i += wrap) {
            frames.add(tape.substring(i, i + wrap));
        }
        int scanned = frames.size();
        int leadUniqueAt = 0;
        int tailUniqueAt = 0;
        double leadShareAtFloor = 0.0;
        double tailShareAtFloor = 0.0;
        double leadShareAtNear = 0.0;
        double tailShareAtNear = 0.0;
        if (scanned > 0) {
            leadShareAtFloor = uniqueShare(frames, FLOOR, true);
            tailShareAtFloor = uniqueShare(frames, FLOOR, false);
            int near = Math.min(NEAR, wrap);
            leadShareAtNear = uniqueShare(frames, near, true);
            tailShareAtNear = uniqueShare(frames, near, false);
            leadUniqueAt = uniqueAt(frames, wrap, true);
            tailUniqueAt = uniqueAt(frames, wrap, false);
        }
        String cheaperEnd = cheaperEnd(leadUniqueAt, tailUniqueAt);
        Scan scan = new Scan(
                wrap,
                FLOOR,
                NEAR,
                scanned,
                leadUniqueAt,
                tailUniqueAt,
                cheaperEnd,
                leadShareAtFloor,
                tailShareAtFloor,
                leadShareAtNear,
                tailShareAtNear);
        if (log.isDebugEnabled()) {
            log.debug("affix.measure chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    static String cheaperEnd(int leadUniqueAt, int tailUniqueAt) {
        if (leadUniqueAt == 0 && tailUniqueAt == 0) {
            return "none";
        }
        if (leadUniqueAt == 0) {
            return "tail";
        }
        if (tailUniqueAt == 0) {
            return "lead";
        }
        if (leadUniqueAt < tailUniqueAt) {
            return "lead";
        }
        if (tailUniqueAt < leadUniqueAt) {
            return "tail";
        }
        return "tie";
    }

    private static int uniqueAt(List<String> frames, int wrap, boolean lead) {
        for (int k = FLOOR; k <= wrap; k++) {
            if (distinctCount(frames, k, lead) == frames.size()) {
                return k;
            }
        }
        return 0;
    }

    private static double uniqueShare(List<String> frames, int k, boolean lead) {
        int scanned = frames.size();
        if (scanned == 0) {
            return 0.0;
        }
        return distinctCount(frames, k, lead) * 1.0 / scanned;
    }

    private static int distinctCount(List<String> frames, int k, boolean lead) {
        Map<String, Integer> counts = new HashMap<>();
        for (String frame : frames) {
            String tile = lead ? frame.substring(0, k) : frame.substring(frame.length() - k);
            counts.merge(tile, 1, Integer::sum);
        }
        return counts.size();
    }

    public record Scan(
            int wrapWidth,
            int floorLength,
            int nearLength,
            int scanned,
            int leadUniqueAt,
            int tailUniqueAt,
            String cheaperEnd,
            double leadShareAtFloor,
            double tailShareAtFloor,
            double leadShareAtNear,
            double tailShareAtNear) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "wrap=%d scanned=%d leadUniqueAt=%d tailUniqueAt=%d cheaper=%s leadShare16=%.4f tailShare16=%.4f",
                    wrapWidth, scanned, leadUniqueAt, tailUniqueAt, cheaperEnd, leadShareAtNear, tailShareAtNear);
        }
    }
}
