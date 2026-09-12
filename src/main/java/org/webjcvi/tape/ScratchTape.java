package org.webjcvi.tape;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-process working tape for caller-supplied text. Not the DNA file and not
 * a project path: the DNA's closed alphabet, linear layout, and homopolymer
 * runs are the protocol, the payload is whatever the caller pastes.
 */
public final class ScratchTape {

    /**
     * Bound near the measured DNA length (531,490) and the AT-rich "keep small"
     * note from the composition report.
     */
    public static final int MAX_CHARS = 524_288;
    public static final int DEFAULT_MAX_HITS = 40;
    public static final int SNIPPET_CHARS = 120;

    private static final Logger log = LoggerFactory.getLogger(ScratchTape.class);

    private String original = "";
    private String folded = "";

    public synchronized void load(String text) {
        if (text == null) {
            throw new TapeException("Tape text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new TapeException("Tape exceeds " + MAX_CHARS + " characters");
        }
        this.original = text;
        this.folded = foldPreserve(text);
        if (log.isDebugEnabled()) {
            log.debug("tape.load chars={}", text.length());
        }
    }

    public synchronized void clear() {
        original = "";
        folded = "";
        if (log.isDebugEnabled()) {
            log.debug("tape.clear");
        }
    }

    public synchronized int length() {
        return original.length();
    }

    public synchronized boolean isEmpty() {
        return original.isEmpty();
    }

    public synchronized String preview(int maxChars) {
        if (maxChars < 0) {
            throw new TapeException("Preview length must be non-negative");
        }
        if (original.length() <= maxChars) {
            return original;
        }
        return original.substring(0, maxChars) + "…";
    }

    public synchronized List<Hit> find(String motif) {
        return find(motif, DEFAULT_MAX_HITS);
    }

    public synchronized List<Hit> find(String motif, int maxHits) {
        String needle = foldQuery(motif);
        if (needle.isEmpty()) {
            throw new TapeException("Motif is empty");
        }
        if (maxHits <= 0) {
            throw new TapeException("maxHits must be positive");
        }
        List<Hit> hits = new ArrayList<>();
        int from = 0;
        while (from < folded.length() && hits.size() < maxHits) {
            int at = folded.indexOf(needle, from);
            if (at < 0) {
                break;
            }
            hits.add(new Hit(at, lineNumber(original, at), snippet(original, at, needle.length())));
            from = at + Math.max(1, needle.length());
        }
        if (log.isDebugEnabled()) {
            log.debug("tape.find motifChars={} maxHits={} hits={}", needle.length(), maxHits, hits.size());
        }
        return List.copyOf(hits);
    }

    public synchronized List<Run> runs(int minLength, int limit) {
        if (minLength < 2) {
            throw new TapeException("minLength must be at least 2");
        }
        if (limit <= 0) {
            throw new TapeException("limit must be positive");
        }
        List<Run> found = new ArrayList<>();
        int i = 0;
        while (i < folded.length()) {
            char ch = folded.charAt(i);
            int j = i + 1;
            while (j < folded.length() && folded.charAt(j) == ch) {
                j++;
            }
            int len = j - i;
            if (len >= minLength) {
                found.add(new Run(ch, i, len));
                if (found.size() >= limit) {
                    if (log.isDebugEnabled()) {
                        log.debug("tape.runs minLength={} limit={} runCount={} truncated=true",
                                minLength, limit, found.size());
                    }
                    return List.copyOf(found);
                }
            }
            i = j;
        }
        if (log.isDebugEnabled()) {
            log.debug("tape.runs minLength={} limit={} runCount={}", minLength, limit, found.size());
        }
        return List.copyOf(found);
    }

    static String foldQuery(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private static String foldPreserve(String raw) {
        return raw.toUpperCase(Locale.ROOT);
    }

    private static int lineNumber(String text, int offset) {
        int line = 1;
        int bound = Math.min(offset, text.length());
        for (int i = 0; i < bound; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static String snippet(String text, int offset, int matchLength) {
        int end = Math.min(text.length(), offset + Math.max(matchLength, SNIPPET_CHARS));
        String slice = text.substring(offset, end).replace('\n', ' ').replace('\r', ' ').trim();
        if (slice.length() > SNIPPET_CHARS) {
            return slice.substring(0, SNIPPET_CHARS) + "…";
        }
        return slice;
    }

    public record Hit(int offset, int line, String snippet) {
    }

    public record Run(char symbol, int offset, int length) {
    }
}
