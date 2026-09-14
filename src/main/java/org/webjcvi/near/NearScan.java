package org.webjcvi.near;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.majority.MajorityException;
import org.webjcvi.majority.MajorityScan;
import org.webjcvi.tape.ScratchTape;

/**
 * Near-unique prefix width of newline-delimited rows. v27 DNA uniqueness
 * covers a majority at the floor (majorityAt=8, share 0.7191) then jumps to
 * 0.8726 at k=9 and cliffs at k=12 (share 0.9943). Iteration 26's majority
 * scan then found log journals whose 0.5-width sat at k=61 with share only
 * ~0.55 — just over half, not a near-unique key — while the 0.99 cliff never
 * arrives. This scan keeps newlines, reuses {@link MajorityScan}, and reports
 * <em>nearAt</em> (smallest k whose unique share is ≥ 0.90). Inverse of
 * majority-scan (there half the lines was enough) and of clock-cliff (there
 * 0.99 emptied the product).
 */
public final class NearScan {

    public static final int TILE = MajorityScan.TILE;
    public static final int FLOOR = MajorityScan.FLOOR;
    public static final int MAX_K = MajorityScan.MAX_K;
    public static final double THRESHOLD = 0.90;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;

    private static final Logger log = LoggerFactory.getLogger(NearScan.class);

    private final MajorityScan majorities;

    public NearScan() {
        this(new MajorityScan());
    }

    public NearScan(MajorityScan majorities) {
        this.majorities = majorities;
    }

    public Scan profile(String text) {
        MajorityScan.Scan majority;
        try {
            majority = majorities.profile(text);
        } catch (MajorityException ex) {
            throw new NearException(ex.getMessage());
        }
        List<String> rows = new ArrayList<>();
        for (String line : splitLines(text == null ? "" : text)) {
            if (line.isBlank()) {
                continue;
            }
            rows.add(line.toUpperCase(Locale.ROOT));
        }
        int nearAt = 0;
        double shareAtNear = 0.0;
        if (!rows.isEmpty()) {
            int maxLen = 0;
            for (String row : rows) {
                if (row.length() > maxLen) {
                    maxLen = row.length();
                }
            }
            int cap = Math.min(Math.max(maxLen, FLOOR), MAX_K);
            for (int k = FLOOR; k <= cap; k++) {
                double share = uniqueShare(rows, k);
                if (share >= THRESHOLD) {
                    nearAt = k;
                    shareAtNear = share;
                    break;
                }
            }
        }
        int lag = 0;
        if (nearAt > 0 && majority.majorityAt() > 0 && nearAt > majority.majorityAt()) {
            lag = nearAt - majority.majorityAt();
        }
        boolean pastMajority = nearAt > majority.majorityAt() && nearAt > 0;
        Scan scan = new Scan(
                majority.lineCount(),
                majority.scanned(),
                FLOOR,
                THRESHOLD,
                nearAt,
                shareAtNear,
                majority.majorityAt(),
                majority.shareAtMajority(),
                lag,
                pastMajority,
                majority.topPrefix(),
                majority.topCount());
        if (log.isDebugEnabled()) {
            log.debug("near.profile chars={} {}", text == null ? 0 : text.length(), scan.summary());
        }
        return scan;
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

    private static double uniqueShare(List<String> rows, int k) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String row : rows) {
            String key = row.length() <= k ? row : row.substring(0, k);
            counts.merge(key, 1, Integer::sum);
        }
        return counts.size() * 1.0 / rows.size();
    }

    public record Scan(
            int lineCount,
            int scanned,
            int floorLength,
            double threshold,
            int nearAt,
            double shareAtNear,
            int majorityAt,
            double shareAtMajority,
            int lag,
            boolean pastMajority,
            String topPrefix,
            int topCount) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "lines=%d scanned=%d floor=%d nearAt=%d shareNear=%.4f majorityAt=%d shareMaj=%.4f lag=%d pastMajority=%s topCount=%d",
                    lineCount, scanned, floorLength, nearAt, shareAtNear, majorityAt, shareAtMajority,
                    lag, pastMajority, topCount);
        }
    }
}
