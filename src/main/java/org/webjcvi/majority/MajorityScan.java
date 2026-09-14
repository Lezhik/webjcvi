package org.webjcvi.majority;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.rise.RiseException;
import org.webjcvi.rise.RiseScan;
import org.webjcvi.tape.ScratchTape;

/**
 * Majority prefix width of newline-delimited rows. v26 DNA uniqueness jumps
 * at the floor (riseAt=8, gain 0.7191, share 0.7191) — already a majority —
 * then cliffs at k=12. Iteration 25's rise-scan then found log journals whose
 * steepest jump sat past the clock (riseAt=23) while unique share there was
 * only ~0.28, not an identifier. This scan keeps newlines, reuses
 * {@link RiseScan}, and reports <em>majorityAt</em> (smallest k whose unique
 * share is ≥ 0.5). Inverse of rise-scan (there the steepest jump was the
 * product even when share stayed below half).
 */
public final class MajorityScan {

    public static final int TILE = RiseScan.TILE;
    public static final int FLOOR = RiseScan.FLOOR;
    public static final int MAX_K = RiseScan.MAX_K;
    public static final double THRESHOLD = 0.5;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;

    private static final Logger log = LoggerFactory.getLogger(MajorityScan.class);

    private final RiseScan rises;

    public MajorityScan() {
        this(new RiseScan());
    }

    public MajorityScan(RiseScan rises) {
        this.rises = rises;
    }

    public Scan profile(String text) {
        RiseScan.Scan rise;
        try {
            rise = rises.profile(text);
        } catch (RiseException ex) {
            throw new MajorityException(ex.getMessage());
        }
        List<String> rows = new ArrayList<>();
        for (String line : splitLines(text == null ? "" : text)) {
            if (line.isBlank()) {
                continue;
            }
            rows.add(line.toUpperCase(Locale.ROOT));
        }
        int majorityAt = 0;
        double shareAtMajority = 0.0;
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
                    majorityAt = k;
                    shareAtMajority = share;
                    break;
                }
            }
        }
        int lag = 0;
        if (majorityAt > 0 && rise.riseAt() > 0 && majorityAt > rise.riseAt()) {
            lag = majorityAt - rise.riseAt();
        }
        boolean pastRise = majorityAt > rise.riseAt() && majorityAt > 0;
        Scan scan = new Scan(
                rise.lineCount(),
                rise.scanned(),
                FLOOR,
                THRESHOLD,
                majorityAt,
                shareAtMajority,
                rise.riseAt(),
                rise.shareAtRise(),
                lag,
                pastRise,
                rise.topPrefix(),
                rise.topCount());
        if (log.isDebugEnabled()) {
            log.debug("majority.profile chars={} {}", text == null ? 0 : text.length(), scan.summary());
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
            int majorityAt,
            double shareAtMajority,
            int riseAt,
            double shareAtRise,
            int lag,
            boolean pastRise,
            String topPrefix,
            int topCount) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "lines=%d scanned=%d floor=%d majorityAt=%d shareMaj=%.4f riseAt=%d shareRise=%.4f lag=%d pastRise=%s topCount=%d",
                    lineCount, scanned, floorLength, majorityAt, shareAtMajority, riseAt, shareAtRise,
                    lag, pastRise, topCount);
        }
    }
}
