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
 * Long-lag tail of leftover colliding source-line prefixes after unique
 * share ≥ 0.90. v30 leftover histogram at nearAt=10 is lag0=0, lag1=220,
 * lag2=71, lagLong=33, modalLag=1 — a count of bins, not how thick the
 * tail is or how far the longest leftover persists. Iteration 30's
 * linger-scan then found journals with mostlyCopies and still lagLong&gt;0
 * (longShare ≈ 0.20, maxLag=53) while tz.md leftover is a long-tail
 * majority (longShare ≈ 0.68). This census keeps FASTA newlines and
 * reports <em>longShare</em> = lagLong / twinGroups and <em>maxLag</em>.
 * Inverse of a 0/1/2/&gt;2 histogram (there the modal bin was the product).
 */
public final class LineLongCensus {

    public static final int FLOOR = 8;
    public static final double THRESHOLD = 0.90;
    public static final int DNA_TIGHT_LAG = 2;

    private static final Logger log = LoggerFactory.getLogger(LineLongCensus.class);

    private final int lines;
    private final int nearAt;
    private final double shareAtNear;
    private final int twinGroups;
    private final int lagLong;
    private final int maxLag;
    private final double longShare;
    private final boolean longTail;

    private LineLongCensus(
            int lines,
            int nearAt,
            double shareAtNear,
            int twinGroups,
            int lagLong,
            int maxLag,
            double longShare,
            boolean longTail) {
        this.lines = lines;
        this.nearAt = nearAt;
        this.shareAtNear = shareAtNear;
        this.twinGroups = twinGroups;
        this.lagLong = lagLong;
        this.maxLag = maxLag;
        this.longShare = longShare;
        this.longTail = longTail;
    }

    public static LineLongCensus fromRaw(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (log.isDebugEnabled()) {
            log.debug("linelong.start chars={}", raw.length());
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
        int lagLong = 0;
        int maxLag = 0;
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
                if (lag > maxLag) {
                    maxLag = lag;
                }
                if (lag > DNA_TIGHT_LAG) {
                    lagLong++;
                }
            }
        }
        double longShare = twinGroups == 0 ? 0.0 : lagLong * 1.0 / twinGroups;
        boolean longTail = lagLong > 0;
        LineLongCensus census = new LineLongCensus(
                rows.size(), nearAt, shareAtNear, twinGroups, lagLong, maxLag, longShare, longTail);
        if (log.isDebugEnabled()) {
            log.debug("linelong.done {}", census.toTextRow());
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

    public int lagLong() {
        return lagLong;
    }

    public int maxLag() {
        return maxLag;
    }

    public double longShare() {
        return longShare;
    }

    public boolean longTail() {
        return longTail;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "lines=%d nearAt=%d shareNear=%.4f twins=%d lagLong=%d maxLag=%d longShare=%.4f longTail=%s",
                lines, nearAt, shareAtNear, twinGroups, lagLong, maxLag, longShare, longTail);
    }
}
