package org.webjcvi.dup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.residue.ResidueException;
import org.webjcvi.residue.ResidueScan;
import org.webjcvi.tape.ScratchTape;

/**
 * Copy versus fork split of leftover twins after a 0.90-width. v29 DNA
 * leftover at nearAt=10 is all forks (copyGroups=0, copyShare=0.0000,
 * minFork=11) then evaporates by k=12. Iteration 28's residue-scan then
 * found log journals whose leftover modal split was 0 — identical copies,
 * so stretched was false and not an actionable signal. This scan keeps
 * newlines, reuses {@link ResidueScan}, and reports how many leftover
 * twin groups are exact copies versus twins that still fork. Inverse of
 * residue-scan (there splitLag/stretched was the product) and distinct
 * from wrap-frame clones at a fixed 70-mer.
 */
public final class DupScan {

    public static final int TILE = ResidueScan.TILE;
    public static final int FLOOR = ResidueScan.FLOOR;
    public static final int MAX_K = ResidueScan.MAX_K;
    public static final double COPY_MAJORITY = 0.5;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;

    private static final Logger log = LoggerFactory.getLogger(DupScan.class);

    private final ResidueScan residues;

    public DupScan() {
        this(new ResidueScan());
    }

    public DupScan(ResidueScan residues) {
        this.residues = residues;
    }

    public Scan profile(String text) {
        ResidueScan.Scan residue;
        try {
            residue = residues.profile(text);
        } catch (ResidueException ex) {
            throw new DupException(ex.getMessage());
        }
        List<String> rows = new ArrayList<>();
        for (String line : splitLines(text == null ? "" : text)) {
            if (line.isBlank()) {
                continue;
            }
            rows.add(line.toUpperCase(Locale.ROOT));
        }
        int copyGroups = 0;
        int copyLines = 0;
        int forkGroups = 0;
        int forkLines = 0;
        int minFork = 0;
        String topPrefix = "";
        int topCount = 0;
        if (residue.nearAt() > 0 && !rows.isEmpty()) {
            Map<String, List<String>> groups = new LinkedHashMap<>();
            for (String row : rows) {
                String key = row.length() <= residue.nearAt() ? row : row.substring(0, residue.nearAt());
                groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
            }
            for (var entry : groups.entrySet()) {
                List<String> members = entry.getValue();
                if (members.size() < 2) {
                    continue;
                }
                int split = ResidueScan.forkAt(members);
                if (split == 0) {
                    copyGroups++;
                    copyLines += members.size();
                    if (members.size() > topCount) {
                        topCount = members.size();
                        topPrefix = entry.getKey();
                    }
                } else {
                    forkGroups++;
                    forkLines += members.size();
                    if (minFork == 0 || split < minFork) {
                        minFork = split;
                    }
                }
            }
        }
        double copyShare = residue.twinLines() == 0 ? 0.0 : copyLines * 1.0 / residue.twinLines();
        boolean mostlyCopies = copyShare > COPY_MAJORITY;
        Scan scan = new Scan(
                residue.lineCount(),
                residue.scanned(),
                FLOOR,
                residue.nearAt(),
                residue.shareAtNear(),
                residue.residueShare(),
                residue.twinGroups(),
                residue.twinLines(),
                copyGroups,
                copyLines,
                forkGroups,
                forkLines,
                copyShare,
                mostlyCopies,
                minFork,
                topPrefix,
                topCount);
        if (log.isDebugEnabled()) {
            log.debug("dup.profile chars={} {}", text == null ? 0 : text.length(), scan.summary());
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
            double shareAtNear,
            double residueShare,
            int twinGroups,
            int twinLines,
            int copyGroups,
            int copyLines,
            int forkGroups,
            int forkLines,
            double copyShare,
            boolean mostlyCopies,
            int minFork,
            String topPrefix,
            int topCount) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "lines=%d scanned=%d floor=%d nearAt=%d copyGroups=%d copyLines=%d forkGroups=%d forkLines=%d copyShare=%.4f mostlyCopies=%s minFork=%d topCount=%d",
                    lineCount, scanned, floorLength, nearAt, copyGroups, copyLines,
                    forkGroups, forkLines, copyShare, mostlyCopies, minFork, topCount);
        }
    }
}
