package org.webjcvi.residue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.near.NearException;
import org.webjcvi.near.NearScan;
import org.webjcvi.tape.ScratchTape;

/**
 * Residual colliding prefixes after a 0.90-width. v28 DNA turns near-unique
 * at k=10 (share 0.9532) then cliffs two bases later at k=12 (share 0.9943).
 * Iteration 27's near-scan then found log journals whose 0.90-width sat at
 * k≈95 with share only ~0.92 — about 8% of lines still collide, and 0.99
 * never arrives. This scan keeps newlines, reuses {@link NearScan}, and
 * lists leftover twin groups at <em>nearAt</em> plus where they first
 * split. Inverse of near-scan (there reaching 0.90 was enough) and of
 * clock-cliff (there waiting for 0.99 emptied the product). Distinct from
 * wrap-frame forks at a fixed k=16.
 */
public final class ResidueScan {

    public static final int TILE = NearScan.TILE;
    public static final int FLOOR = NearScan.FLOOR;
    public static final int MAX_K = NearScan.MAX_K;
    public static final int DNA_TIGHT_LAG = 2;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;

    private static final Logger log = LoggerFactory.getLogger(ResidueScan.class);

    private final NearScan nears;

    public ResidueScan() {
        this(new NearScan());
    }

    public ResidueScan(NearScan nears) {
        this.nears = nears;
    }

    public Scan profile(String text) {
        NearScan.Scan near;
        try {
            near = nears.profile(text);
        } catch (NearException ex) {
            throw new ResidueException(ex.getMessage());
        }
        List<String> rows = new ArrayList<>();
        for (String line : splitLines(text == null ? "" : text)) {
            if (line.isBlank()) {
                continue;
            }
            rows.add(line.toUpperCase(Locale.ROOT));
        }
        int twinGroups = 0;
        int twinLines = 0;
        int minSplit = 0;
        int modalSplit = 0;
        int maxSplit = 0;
        String topPrefix = "";
        int topCount = 0;
        if (near.nearAt() > 0 && !rows.isEmpty()) {
            Map<String, List<String>> groups = new LinkedHashMap<>();
            for (String row : rows) {
                String key = row.length() <= near.nearAt() ? row : row.substring(0, near.nearAt());
                groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
            }
            Map<Integer, Integer> splitCounts = new HashMap<>();
            for (var entry : groups.entrySet()) {
                List<String> members = entry.getValue();
                if (members.size() < 2) {
                    continue;
                }
                twinGroups++;
                twinLines += members.size();
                int split = forkAt(members);
                splitCounts.merge(split, 1, Integer::sum);
                if (minSplit == 0 || (split > 0 && split < minSplit)) {
                    minSplit = split;
                }
                if (split > maxSplit) {
                    maxSplit = split;
                }
                if (members.size() > topCount) {
                    topCount = members.size();
                    topPrefix = entry.getKey();
                }
            }
            int best = 0;
            for (var entry : splitCounts.entrySet()) {
                if (entry.getValue() > best || (entry.getValue() == best && entry.getKey() < modalSplit)) {
                    best = entry.getValue();
                    modalSplit = entry.getKey();
                }
            }
        }
        double residueShare = rows.isEmpty() ? 0.0 : twinLines * 1.0 / rows.size();
        int splitLag = 0;
        if (modalSplit > near.nearAt() && near.nearAt() > 0) {
            splitLag = modalSplit - near.nearAt();
        }
        boolean stretched = splitLag > DNA_TIGHT_LAG;
        Scan scan = new Scan(
                near.lineCount(),
                near.scanned(),
                FLOOR,
                near.nearAt(),
                near.shareAtNear(),
                residueShare,
                twinGroups,
                twinLines,
                minSplit,
                modalSplit,
                maxSplit,
                splitLag,
                stretched,
                topPrefix,
                topCount);
        if (log.isDebugEnabled()) {
            log.debug("residue.profile chars={} {}", text == null ? 0 : text.length(), scan.summary());
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

    public static int forkAt(List<String> members) {
        int maxLen = 0;
        for (String member : members) {
            if (member.length() > maxLen) {
                maxLen = member.length();
            }
        }
        for (int col = 0; col < maxLen; col++) {
            Character expected = null;
            for (String member : members) {
                Character ch = col < member.length() ? member.charAt(col) : null;
                if (expected == null) {
                    expected = ch;
                } else if (!java.util.Objects.equals(ch, expected)) {
                    return col + 1;
                }
            }
        }
        return 0;
    }

    public record Scan(
            int lineCount,
            int scanned,
            int floorLength,
            int nearAt,
            double shareAtNear,
            double residueShare,
            int twinGroups,
            int twinLines,
            int minSplit,
            int modalSplit,
            int maxSplit,
            int splitLag,
            boolean stretched,
            String topPrefix,
            int topCount) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "lines=%d scanned=%d floor=%d nearAt=%d shareNear=%.4f residueShare=%.4f twinGroups=%d twinLines=%d minSplit=%d modalSplit=%d maxSplit=%d splitLag=%d stretched=%s topCount=%d",
                    lineCount, scanned, floorLength, nearAt, shareAtNear, residueShare,
                    twinGroups, twinLines, minSplit, modalSplit, maxSplit, splitLag, stretched, topCount);
        }
    }
}
