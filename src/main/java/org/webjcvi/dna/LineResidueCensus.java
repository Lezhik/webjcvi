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
 * Leftover colliding source-line prefixes after unique share ≥ 0.90. v28
 * uniqueness turns near-unique at k=10 (share 0.9532) then 0.9835 at k=11
 * and cliffs at k=12 (share 0.9943). Iteration 27's near-scan then found
 * log journals whose 0.90-width sat at k≈95 with share only ~0.92, and
 * iteration 28's residue-scan found the leftover modal split was 0 —
 * identical copies, not a DNA-tight two-base climb. This census keeps
 * FASTA newlines and asks whether leftover twins at nearAt are exact
 * copies or still fork, plus how leftover share evaporates at k=10..16.
 */
public final class LineResidueCensus {

    public static final int FLOOR = 8;
    public static final double THRESHOLD = 0.90;
    public static final int[] SAMPLE_LENGTHS = {10, 11, 12, 16};

    private static final Logger log = LoggerFactory.getLogger(LineResidueCensus.class);

    private final int lines;
    private final int nearAt;
    private final double shareAtNear;
    private final int twinGroups;
    private final int twinLines;
    private final int copyGroups;
    private final int copyLines;
    private final int forkGroups;
    private final int forkLines;
    private final double copyShare;
    private final int minFork;
    private final List<Row> samples;

    private LineResidueCensus(
            int lines,
            int nearAt,
            double shareAtNear,
            int twinGroups,
            int twinLines,
            int copyGroups,
            int copyLines,
            int forkGroups,
            int forkLines,
            double copyShare,
            int minFork,
            List<Row> samples) {
        this.lines = lines;
        this.nearAt = nearAt;
        this.shareAtNear = shareAtNear;
        this.twinGroups = twinGroups;
        this.twinLines = twinLines;
        this.copyGroups = copyGroups;
        this.copyLines = copyLines;
        this.forkGroups = forkGroups;
        this.forkLines = forkLines;
        this.copyShare = copyShare;
        this.minFork = minFork;
        this.samples = samples;
    }

    public static LineResidueCensus fromRaw(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (log.isDebugEnabled()) {
            log.debug("lineresidue.start chars={}", raw.length());
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
        int twinLines = 0;
        int copyGroups = 0;
        int copyLines = 0;
        int forkGroups = 0;
        int forkLines = 0;
        int minFork = 0;
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
                twinLines += members.size();
                int fork = LineForkCensus.forkAt(members);
                if (fork == 0) {
                    copyGroups++;
                    copyLines += members.size();
                } else {
                    forkGroups++;
                    forkLines += members.size();
                    if (minFork == 0 || fork < minFork) {
                        minFork = fork;
                    }
                }
            }
        }
        double copyShare = twinLines == 0 ? 0.0 : copyLines * 1.0 / twinLines;
        List<Row> samples = new ArrayList<>();
        for (int k : SAMPLE_LENGTHS) {
            double share = uniqueShare(rows, k);
            samples.add(new Row(k, 1.0 - share));
        }
        LineResidueCensus census = new LineResidueCensus(
                rows.size(), nearAt, shareAtNear, twinGroups, twinLines,
                copyGroups, copyLines, forkGroups, forkLines, copyShare, minFork,
                List.copyOf(samples));
        if (log.isDebugEnabled()) {
            log.debug("lineresidue.done {}", census.toTextRow());
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

    public int twinLines() {
        return twinLines;
    }

    public int copyGroups() {
        return copyGroups;
    }

    public int copyLines() {
        return copyLines;
    }

    public int forkGroups() {
        return forkGroups;
    }

    public int forkLines() {
        return forkLines;
    }

    public double copyShare() {
        return copyShare;
    }

    public int minFork() {
        return minFork;
    }

    public List<Row> samples() {
        return samples;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "lines=%d nearAt=%d shareNear=%.4f twins=%d/%d copies=%d/%d forks=%d/%d copyShare=%.4f minFork=%d",
                lines, nearAt, shareAtNear, twinGroups, twinLines, copyGroups, copyLines,
                forkGroups, forkLines, copyShare, minFork);
    }

    public record Row(int length, double leftoverShare) {
    }
}
