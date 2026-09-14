package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Split-lag histogram of leftover colliding source-line prefixes after
 * unique share ≥ 0.90. v29 leftover at nearAt=10 is all forks (copyShare
 * 0, minFork 11) — every leftover group splits one base later. Iteration
 * 28's residue-scan then found log leftover modal split 0 (copies), and
 * iteration 29's dup-scan found copyShare ≈ 0.62 on journals. This census
 * keeps FASTA newlines and bins leftover groups by splitLag: 0 (exact
 * copies), 1, 2, or longer than DNA's two-base climb. Inverse of a
 * copy/fork snapshot (there the kind was binary); here the variable is
 * how far leftover twins persist past nearAt.
 */
public final class LineLagCensus {

    public static final int FLOOR = 8;
    public static final double THRESHOLD = 0.90;
    public static final int DNA_TIGHT_LAG = 2;

    private static final Logger log = LoggerFactory.getLogger(LineLagCensus.class);

    private final int lines;
    private final int nearAt;
    private final double shareAtNear;
    private final int twinGroups;
    private final int lag0;
    private final int lag1;
    private final int lag2;
    private final int lagLong;
    private final int modalLag;

    private LineLagCensus(
            int lines,
            int nearAt,
            double shareAtNear,
            int twinGroups,
            int lag0,
            int lag1,
            int lag2,
            int lagLong,
            int modalLag) {
        this.lines = lines;
        this.nearAt = nearAt;
        this.shareAtNear = shareAtNear;
        this.twinGroups = twinGroups;
        this.lag0 = lag0;
        this.lag1 = lag1;
        this.lag2 = lag2;
        this.lagLong = lagLong;
        this.modalLag = modalLag;
    }

    public static LineLagCensus fromRaw(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (log.isDebugEnabled()) {
            log.debug("linelag.start chars={}", raw.length());
        }
        List<String> rows = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= raw.length(); i++) {
            if (i == raw.length() || raw.charAt(i) == '\n') {
                String line = raw.substring(start, i);
                if (line.endsWith("\r")) {
                    line = line.substring(0, line.length() - 1);
                }
                StringBuilder canonical = new StringBuilder();
                for (int c = 0; c < line.length(); c++) {
                    char ch = Character.toUpperCase(line.charAt(c));
                    if (ch == 'A' || ch == 'T' || ch == 'G' || ch == 'C') {
                        canonical.append(ch);
                    }
                }
                if (!canonical.isEmpty()) {
                    rows.add(canonical.toString());
                }
                start = i + 1;
            }
        }
        int maxLen = 0;
        for (String row : rows) {
            if (row.length() > maxLen) {
                maxLen = row.length();
            }
        }
        int nearAt = 0;
        double shareAtNear = 0.0;
        if (!rows.isEmpty()) {
            int cap = Math.max(maxLen, FLOOR);
            for (int k = FLOOR; k <= cap; k++) {
                double share = uniqueShare(rows, k);
                if (share >= THRESHOLD) {
                    nearAt = k;
                    shareAtNear = share;
                    break;
                }
            }
        }
        int twinGroups = 0;
        int lag0 = 0;
        int lag1 = 0;
        int lag2 = 0;
        int lagLong = 0;
        if (nearAt > 0 && !rows.isEmpty()) {
            Map<String, List<String>> groups = new LinkedHashMap<>();
            for (String row : rows) {
                String key = row.length() <= nearAt ? row : row.substring(0, nearAt);
                groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
            }
            for (var entry : groups.entrySet()) {
                List<String> members = entry.getValue();
                if (members.size() < 2) {
                    continue;
                }
                twinGroups++;
                int fork = LineForkCensus.forkAt(members);
                int lag = 0;
                if (fork > nearAt) {
                    lag = fork - nearAt;
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
        LineLagCensus census = new LineLagCensus(
                rows.size(), nearAt, shareAtNear, twinGroups, lag0, lag1, lag2, lagLong, modalLag);
        if (log.isDebugEnabled()) {
            log.debug("linelag.done {}", census.toTextRow());
        }
        return census;
    }

    private static double uniqueShare(List<String> rows, int k) {
        if (rows.isEmpty()) {
            return 0.0;
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String row : rows) {
            String key = row.length() <= k ? row : row.substring(0, k);
            counts.merge(key, 1, Integer::sum);
        }
        return counts.size() * 1.0 / rows.size();
    }

    public int lines() {
        return lines;
    }

    public int nearAt() {
        return nearAt;
    }

    public double shareAtNear() {
        return shareAtNear;
    }

    public int twinGroups() {
        return twinGroups;
    }

    public int lag0() {
        return lag0;
    }

    public int lag1() {
        return lag1;
    }

    public int lag2() {
        return lag2;
    }

    public int lagLong() {
        return lagLong;
    }

    public int modalLag() {
        return modalLag;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "lines=%d nearAt=%d shareNear=%.4f twins=%d lag0=%d lag1=%d lag2=%d lagLong=%d modalLag=%d",
                lines, nearAt, shareAtNear, twinGroups, lag0, lag1, lag2, lagLong, modalLag);
    }
}
