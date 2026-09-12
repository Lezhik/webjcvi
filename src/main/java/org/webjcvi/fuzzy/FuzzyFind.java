package org.webjcvi.fuzzy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Sliding Hamming search: find near-matches of a motif in caller text.
 * The v10 hairpin census was flat across loop widths 0–13 (~44k–55k stems),
 * so exact complementary pairing is background. Default max distance 1 is
 * the smallest mismatch; min motif 4 is the hairpin MIN_STEM analog.
 * Inverse of exact tape find.
 */
public final class FuzzyFind {

    public static final int DEFAULT_MAX_DIST = 1;
    public static final int MIN_MOTIF = 4;
    public static final int MAX_MOTIF = 80;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_HITS = 40;

    private static final Logger log = LoggerFactory.getLogger(FuzzyFind.class);

    public Scan search(String text, String motif) {
        return search(text, motif, DEFAULT_MAX_DIST);
    }

    public Scan search(String text, String motif, int maxDist) {
        if (text == null) {
            throw new FuzzyException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new FuzzyException("Text exceeds " + MAX_CHARS + " characters");
        }
        if (motif == null) {
            throw new FuzzyException("Motif is missing");
        }
        String needle = motif.trim().toUpperCase(Locale.ROOT);
        if (needle.length() < MIN_MOTIF) {
            throw new FuzzyException("Motif must be at least " + MIN_MOTIF + " characters");
        }
        if (needle.length() > MAX_MOTIF) {
            throw new FuzzyException("Motif exceeds " + MAX_MOTIF + " characters");
        }
        int cap = maxDist < 0 ? DEFAULT_MAX_DIST : Math.min(maxDist, needle.length());
        String folded = fold(text);
        List<Hit> hits = new ArrayList<>();
        int m = needle.length();
        int n = folded.length();
        int scanned = Math.max(0, n - m + 1);
        for (int i = 0; i < scanned; i++) {
            int dist = hamming(folded, i, needle);
            if (dist <= cap) {
                hits.add(new Hit(hits.size(), i, dist, text.substring(i, i + m)));
            }
        }
        hits.sort(Comparator
                .comparingInt(Hit::distance)
                .thenComparingInt(Hit::offset));
        if (hits.size() > MAX_HITS) {
            hits = new ArrayList<>(hits.subList(0, MAX_HITS));
        }
        for (int i = 0; i < hits.size(); i++) {
            Hit h = hits.get(i);
            hits.set(i, new Hit(i, h.offset(), h.distance(), h.preview()));
        }
        Scan scan = new Scan(scanned, hits.size(), m, cap, List.copyOf(hits));
        if (log.isDebugEnabled()) {
            log.debug("fuzzy.search chars={} motifChars={} {}", text.length(), m, scan.summary());
        }
        return scan;
    }

    private static String fold(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            out.append(Character.toUpperCase(text.charAt(i)));
        }
        return out.toString();
    }

    private static int hamming(String folded, int offset, String needle) {
        int dist = 0;
        for (int i = 0; i < needle.length(); i++) {
            if (folded.charAt(offset + i) != needle.charAt(i)) {
                dist++;
            }
        }
        return dist;
    }

    public record Scan(int scanned, int hitCount, int motifLength, int maxDist, List<Hit> hits) {

        public String summary() {
            return String.format(Locale.ROOT, "scanned=%d hits=%d motif=%d maxDist=%d",
                    scanned, hitCount, motifLength, maxDist);
        }
    }

    public record Hit(int index, int offset, int distance, String preview) {
    }
}
