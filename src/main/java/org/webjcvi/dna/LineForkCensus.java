package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * First-difference columns of colliding source-line 16-prefixes. v23 line
 * keys jump from k=8 share 0.7191 to k=16 0.9997 (uniqueAt 20). Iteration
 * 23's clock-cliff then found log-journal forkAt=18 / cliffAt=113 — the
 * identifier lives past the clock, not at saturating uniqueAt. This census
 * keeps FASTA newlines and asks where residual k=16 line-twins fork.
 */
public final class LineForkCensus {

    public static final int TILE = 16;

    private static final Logger log = LoggerFactory.getLogger(LineForkCensus.class);

    private final int lines;
    private final int tileLength;
    private final int twinGroups;
    private final int twinLines;
    private final int minFork;
    private final int modalFork;
    private final int maxFork;
    private final List<Hit> hits;

    private LineForkCensus(
            int lines,
            int tileLength,
            int twinGroups,
            int twinLines,
            int minFork,
            int modalFork,
            int maxFork,
            List<Hit> hits) {
        this.lines = lines;
        this.tileLength = tileLength;
        this.twinGroups = twinGroups;
        this.twinLines = twinLines;
        this.minFork = minFork;
        this.modalFork = modalFork;
        this.maxFork = maxFork;
        this.hits = hits;
    }

    public static LineForkCensus fromRaw(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (log.isDebugEnabled()) {
            log.debug("linefork.start chars={}", raw.length());
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
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String row : rows) {
            String key = row.length() <= TILE ? row : row.substring(0, TILE);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }
        List<Hit> hits = new ArrayList<>();
        int twinLines = 0;
        int minFork = 0;
        int maxFork = 0;
        Map<Integer, Integer> forkCounts = new HashMap<>();
        for (var entry : groups.entrySet()) {
            List<String> members = entry.getValue();
            if (members.size() < 2) {
                continue;
            }
            int fork = forkAt(members);
            hits.add(new Hit(entry.getKey(), members.size(), fork));
            twinLines += members.size();
            forkCounts.merge(fork, 1, Integer::sum);
            if (minFork == 0 || (fork > 0 && fork < minFork)) {
                minFork = fork;
            }
            if (fork > maxFork) {
                maxFork = fork;
            }
        }
        int modalFork = 0;
        int best = 0;
        for (var entry : forkCounts.entrySet()) {
            if (entry.getValue() > best || (entry.getValue() == best && entry.getKey() < modalFork)) {
                best = entry.getValue();
                modalFork = entry.getKey();
            }
        }
        LineForkCensus census = new LineForkCensus(
                rows.size(), TILE, hits.size(), twinLines, minFork, modalFork, maxFork, List.copyOf(hits));
        if (log.isDebugEnabled()) {
            log.debug("linefork.done {}", census.toTextRow());
        }
        return census;
    }

    static int forkAt(List<String> members) {
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

    public int lines() {
        return lines;
    }

    public int tileLength() {
        return tileLength;
    }

    public int twinGroups() {
        return twinGroups;
    }

    public int twinLines() {
        return twinLines;
    }

    public int minFork() {
        return minFork;
    }

    public int modalFork() {
        return modalFork;
    }

    public int maxFork() {
        return maxFork;
    }

    public List<Hit> hits() {
        return hits;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "lines=%d tile=%d twinGroups=%d twinLines=%d minFork=%d modalFork=%d maxFork=%d",
                lines, tileLength, twinGroups, twinLines, minFork, modalFork, maxFork);
    }

    public record Hit(String prefix, int count, int forkAt) {
    }
}
