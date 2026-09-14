package org.webjcvi.linger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.dup.DupException;
import org.webjcvi.dup.DupScan;
import org.webjcvi.residue.ResidueScan;
import org.webjcvi.tape.ScratchTape;

/**
 * Long-lag tail of leftover twins after a 0.90-width. v30 DNA leftover at
 * nearAt=10 never copies (lag0=0), modes at lag 1 (220 of 324), but
 * <em>lagLong=33</em> groups persist past DNA's two-base climb. Iteration
 * 29's dup-scan then found log journals whose leftover is mostly copies
 * (copyShare ≈ 0.62) while leftover <em>forks</em> look DNA-tight
 * (minFork = nearAt+1). This scan keeps newlines, reuses {@link DupScan},
 * and bins leftover groups by splitLag. Inverse of dup-scan (there the
 * copy majority was the product) and of residue-scan (there the
 * <em>modal</em> split had to stretch). Distinct: count groups with lag
 * &gt; 2 even when the mode is copies or lag 1.
 */
public final class LingerScan {

    public static final int TILE = DupScan.TILE;
    public static final int FLOOR = DupScan.FLOOR;
    public static final int MAX_K = DupScan.MAX_K;
    public static final int DNA_TIGHT_LAG = ResidueScan.DNA_TIGHT_LAG;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;

    private static final Logger log = LoggerFactory.getLogger(LingerScan.class);

    private final DupScan dups;

    public LingerScan() {
        this(new DupScan());
    }

    public LingerScan(DupScan dups) {
        this.dups = dups;
    }

    public Scan profile(String text) {
        DupScan.Scan dup;
        try {
            dup = dups.profile(text);
        } catch (DupException ex) {
            throw new LingerException(ex.getMessage());
        }
        List<String> rows = new ArrayList<>();
        for (String line : splitLines(text == null ? "" : text)) {
            if (line.isBlank()) {
                continue;
            }
            rows.add(line.toUpperCase(Locale.ROOT));
        }
        int lag0 = 0;
        int lag1 = 0;
        int lag2 = 0;
        int lagLong = 0;
        int maxLag = 0;
        if (dup.nearAt() > 0 && !rows.isEmpty()) {
            Map<String, List<String>> groups = new LinkedHashMap<>();
            for (String row : rows) {
                String key = row.length() <= dup.nearAt() ? row : row.substring(0, dup.nearAt());
                groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
            }
            for (var entry : groups.entrySet()) {
                List<String> members = entry.getValue();
                if (members.size() < 2) {
                    continue;
                }
                int fork = ResidueScan.forkAt(members);
                int lag = 0;
                if (fork > dup.nearAt()) {
                    lag = fork - dup.nearAt();
                }
                if (lag > maxLag) {
                    maxLag = lag;
                }
                if (lag == 0) {
                    lag0++;
                } else if (lag == 1) {
                    lag1++;
                } else if (lag == 2) {
                    lag2++;
                } else {
                    lagLong++;
                }
            }
        }
        int modalLag = 0;
        int best = lag0;
        if (lag1 > best) {
            best = lag1;
            modalLag = 1;
        }
        if (lag2 > best) {
            best = lag2;
            modalLag = 2;
        }
        if (lagLong > best) {
            modalLag = DNA_TIGHT_LAG + 1;
        }
        double longShare = dup.twinGroups() == 0 ? 0.0 : lagLong * 1.0 / dup.twinGroups();
        boolean longTail = lagLong > 0;
        Scan scan = new Scan(
                dup.lineCount(),
                dup.scanned(),
                FLOOR,
                dup.nearAt(),
                dup.twinGroups(),
                lag0,
                lag1,
                lag2,
                lagLong,
                modalLag,
                maxLag,
                longShare,
                longTail,
                dup.copyShare(),
                dup.mostlyCopies());
        if (log.isDebugEnabled()) {
            log.debug("linger.profile chars={} {}", text == null ? 0 : text.length(), scan.summary());
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

    public record Scan(
            int lineCount,
            int scanned,
            int floorLength,
            int nearAt,
            int twinGroups,
            int lag0,
            int lag1,
            int lag2,
            int lagLong,
            int modalLag,
            int maxLag,
            double longShare,
            boolean longTail,
            double copyShare,
            boolean mostlyCopies) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "lines=%d scanned=%d floor=%d nearAt=%d twins=%d lag0=%d lag1=%d lag2=%d lagLong=%d modalLag=%d maxLag=%d longShare=%.4f longTail=%s mostlyCopies=%s",
                    lineCount, scanned, floorLength, nearAt, twinGroups, lag0, lag1, lag2,
                    lagLong, modalLag, maxLag, longShare, longTail, mostlyCopies);
        }
    }
}
