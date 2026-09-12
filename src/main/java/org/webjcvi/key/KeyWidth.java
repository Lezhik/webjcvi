package org.webjcvi.key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Shortest unique prefix of wrap-frames. v18 found leading 8-mers collide
 * (unique share 0.7190, 1290 families, top TTAATAAA×10) while every 70-mer
 * is unique. Shared prefixes are a key that is too short, not a header
 * catalog. Scan prefix lengths from the measured floor 8 up to wrap and
 * report the smallest k at which every frame is unique. Inverse of
 * prefix-family grouping (there collisions were the payload).
 */
public final class KeyWidth {

    public static final int DEFAULT_WRAP = 70;
    public static final int FLOOR = 8;
    public static final int STEP = 8;
    public static final int MAX_WRAP = 256;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_HITS = 40;

    private static final Logger log = LoggerFactory.getLogger(KeyWidth.class);

    public Scan measure(String text) {
        return measure(text, DEFAULT_WRAP);
    }

    public Scan measure(String text, int wrapWidth) {
        if (text == null) {
            throw new KeyException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new KeyException("Text exceeds " + MAX_CHARS + " characters");
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
        double shareAtFloor = 0.0;
        double shareAtWrap = 0.0;
        int uniqueAt = 0;
        if (scanned > 0) {
            shareAtFloor = uniqueShare(frames, FLOOR);
            shareAtWrap = uniqueShare(frames, wrap);
            for (int k = FLOOR; k <= wrap; k++) {
                if (distinctCount(frames, k) == scanned) {
                    uniqueAt = k;
                    break;
                }
            }
        }
        List<Hit> samples = sampleCurve(frames, wrap, uniqueAt);
        Scan scan = new Scan(
                wrap,
                FLOOR,
                scanned,
                uniqueAt,
                shareAtFloor,
                shareAtWrap,
                samples);
        if (log.isDebugEnabled()) {
            log.debug("key.width chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    private static List<Hit> sampleCurve(List<String> frames, int wrap, int uniqueAt) {
        List<Hit> samples = new ArrayList<>();
        if (frames.isEmpty()) {
            return List.of();
        }
        int last = 0;
        for (int k = FLOOR; k < wrap; k += STEP) {
            samples.add(hit(frames, k));
            last = k;
        }
        if (last != wrap) {
            samples.add(hit(frames, wrap));
        }
        if (uniqueAt >= FLOOR && uniqueAt <= wrap) {
            boolean present = false;
            for (Hit hit : samples) {
                if (hit.length() == uniqueAt) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                Hit extra = hit(frames, uniqueAt);
                int insert = 0;
                while (insert < samples.size() && samples.get(insert).length() < uniqueAt) {
                    insert++;
                }
                samples.add(insert, extra);
            }
        }
        if (samples.size() > MAX_HITS) {
            return List.copyOf(samples.subList(0, MAX_HITS));
        }
        return List.copyOf(samples);
    }

    private static Hit hit(List<String> frames, int k) {
        Map<String, Integer> counts = new HashMap<>();
        for (String frame : frames) {
            counts.merge(frame.substring(0, k), 1, Integer::sum);
        }
        int familyCount = 0;
        for (int count : counts.values()) {
            if (count >= 2) {
                familyCount++;
            }
        }
        double share = frames.isEmpty() ? 0.0 : counts.size() * 1.0 / frames.size();
        return new Hit(k, counts.size(), familyCount, share);
    }

    private static int distinctCount(List<String> frames, int k) {
        Map<String, Integer> counts = new HashMap<>();
        for (String frame : frames) {
            counts.merge(frame.substring(0, k), 1, Integer::sum);
        }
        return counts.size();
    }

    private static double uniqueShare(List<String> frames, int k) {
        int scanned = frames.size();
        if (scanned == 0) {
            return 0.0;
        }
        return distinctCount(frames, k) * 1.0 / scanned;
    }

    public record Scan(
            int wrapWidth,
            int floorLength,
            int scanned,
            int uniqueAt,
            double uniqueShareAtFloor,
            double uniqueShareAtWrap,
            List<Hit> samples) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "wrap=%d floor=%d scanned=%d uniqueAt=%d shareAtFloor=%.4f shareAtWrap=%.4f",
                    wrapWidth, floorLength, scanned, uniqueAt, uniqueShareAtFloor, uniqueShareAtWrap);
        }
    }

    public record Hit(int length, int distinct, int familyCount, double uniqueShare) {
    }
}
