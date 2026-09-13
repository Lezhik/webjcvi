package org.webjcvi.lane;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Uniqueness geography of wrap-frames at a near-unique tile width.
 * v21 found centered uniqueAt=17 with k=16 share 0.9999 — the same
 * saturation as the tail (17 / 0.9997) and essentially the lead
 * (20 / 0.9997). Uniqueness is not an edge property, so an agent must
 * profile several start columns and pick the peak (fingerprint) versus
 * the trough (collision lane). Inverse of affix-scan (there only lead
 * versus tail uniqueAt) and of fork-scan (there only leading 16-mer
 * twins). Newlines are dropped so the scan matches FASTA concatenation.
 */
public final class LaneScan {

    public static final int DEFAULT_WRAP = 70;
    public static final int TILE = 16;
    public static final int MAX_WRAP = 256;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;

    private static final Logger log = LoggerFactory.getLogger(LaneScan.class);

    public Scan profile(String text) {
        return profile(text, DEFAULT_WRAP);
    }

    public Scan profile(String text, int wrapWidth) {
        if (text == null) {
            throw new LaneException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new LaneException("Text exceeds " + MAX_CHARS + " characters");
        }
        int wrap = wrapWidth < TILE ? DEFAULT_WRAP : Math.min(wrapWidth, MAX_WRAP);
        int tile = Math.min(TILE, wrap);
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
        List<Integer> starts = starts(wrap, tile);
        List<Lane> lanes = new ArrayList<>();
        int troughAt = 0;
        int peakAt = 0;
        double troughShare = 0.0;
        double peakShare = 0.0;
        if (scanned > 0 && !starts.isEmpty()) {
            for (int start : starts) {
                double share = uniqueShare(frames, start, tile);
                int distinct = distinctCount(frames, start, tile);
                lanes.add(new Lane(start, distinct, share));
            }
            troughAt = lanes.get(0).start();
            peakAt = lanes.get(0).start();
            troughShare = lanes.get(0).uniqueShare();
            peakShare = lanes.get(0).uniqueShare();
            for (Lane lane : lanes) {
                if (lane.uniqueShare() < troughShare) {
                    troughShare = lane.uniqueShare();
                    troughAt = lane.start();
                }
                if (lane.uniqueShare() > peakShare) {
                    peakShare = lane.uniqueShare();
                    peakAt = lane.start();
                }
            }
        }
        double spread = peakShare - troughShare;
        Scan scan = new Scan(wrap, tile, scanned, troughAt, peakAt, troughShare, peakShare, spread, List.copyOf(lanes));
        if (log.isDebugEnabled()) {
            log.debug("lane.profile chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    static List<Integer> starts(int wrap, int tile) {
        LinkedHashSet<Integer> set = new LinkedHashSet<>();
        set.add(0);
        if (8 + tile <= wrap) {
            set.add(8);
        }
        if (19 + tile <= wrap) {
            set.add(19);
        }
        set.add((wrap - tile) / 2);
        set.add(wrap - tile);
        List<Integer> out = new ArrayList<>();
        for (int start : set) {
            if (start >= 0 && start + tile <= wrap) {
                out.add(start);
            }
        }
        return out;
    }

    private static double uniqueShare(List<String> frames, int start, int tile) {
        int scanned = frames.size();
        if (scanned == 0) {
            return 0.0;
        }
        return distinctCount(frames, start, tile) * 1.0 / scanned;
    }

    private static int distinctCount(List<String> frames, int start, int tile) {
        Map<String, Integer> counts = new HashMap<>();
        for (String frame : frames) {
            counts.merge(frame.substring(start, start + tile), 1, Integer::sum);
        }
        return counts.size();
    }

    public record Lane(int start, int distinct, double uniqueShare) {
    }

    public record Scan(
            int wrapWidth,
            int tileLength,
            int scanned,
            int troughAt,
            int peakAt,
            double troughShare,
            double peakShare,
            double spread,
            List<Lane> lanes) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "wrap=%d scanned=%d tile=%d troughAt=%d peakAt=%d trough=%.4f peak=%.4f spread=%.4f",
                    wrapWidth, scanned, tileLength, troughAt, peakAt, troughShare, peakShare, spread);
        }
    }
}
