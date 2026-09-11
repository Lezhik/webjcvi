package org.webjcvi.dna;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed nucleotide sequence plus composition counts. Canonical bases are
 * stored separately from IUPAC ambiguity codes and any leftover invalid
 * characters so callers can decide how to present them.
 */
public final class DnaSequence {

    public static final String CANONICAL_BASES = "ATGC";
    public static final String IUPAC_AMBIGUITY = "NRYSWKMBDHV";

    private final String normalized;
    private final Map<Character, Long> canonicalCounts;
    private final Map<Character, Long> ambiguousCounts;
    private final Map<Character, Long> invalidCounts;
    private final int sourceLineCount;

    public DnaSequence(
            String normalized,
            Map<Character, Long> canonicalCounts,
            Map<Character, Long> ambiguousCounts,
            Map<Character, Long> invalidCounts,
            int sourceLineCount) {
        this.normalized = Objects.requireNonNull(normalized, "normalized");
        this.canonicalCounts = Collections.unmodifiableMap(new LinkedHashMap<>(canonicalCounts));
        this.ambiguousCounts = Collections.unmodifiableMap(new LinkedHashMap<>(ambiguousCounts));
        this.invalidCounts = Collections.unmodifiableMap(new LinkedHashMap<>(invalidCounts));
        this.sourceLineCount = sourceLineCount;
    }

    public String normalized() {
        return normalized;
    }

    public int length() {
        return normalized.length();
    }

    public boolean isEmpty() {
        return normalized.isEmpty();
    }

    public Map<Character, Long> canonicalCounts() {
        return canonicalCounts;
    }

    public Map<Character, Long> ambiguousCounts() {
        return ambiguousCounts;
    }

    public Map<Character, Long> invalidCounts() {
        return invalidCounts;
    }

    public long ambiguousTotal() {
        return ambiguousCounts.values().stream().mapToLong(Long::longValue).sum();
    }

    public long invalidTotal() {
        return invalidCounts.values().stream().mapToLong(Long::longValue).sum();
    }

    public int sourceLineCount() {
        return sourceLineCount;
    }

    public long count(char base) {
        Character key = Character.toUpperCase(base);
        if (canonicalCounts.containsKey(key)) {
            return canonicalCounts.get(key);
        }
        return ambiguousCounts.getOrDefault(key, 0L);
    }

    public double gcPercent() {
        long a = canonicalCounts.getOrDefault('A', 0L);
        long t = canonicalCounts.getOrDefault('T', 0L);
        long g = canonicalCounts.getOrDefault('G', 0L);
        long c = canonicalCounts.getOrDefault('C', 0L);
        long atgc = a + t + g + c;
        if (atgc == 0) {
            return 0.0;
        }
        return (g + c) * 100.0 / atgc;
    }

    public String preview(int maxChars) {
        if (normalized.length() <= maxChars) {
            return normalized;
        }
        return normalized.substring(0, maxChars) + "...";
    }

    @Override
    public String toString() {
        return "DnaSequence{length=" + length()
                + ", gc=" + String.format(Locale.ROOT, "%.2f", gcPercent())
                + "%, ambiguous=" + ambiguousTotal()
                + ", invalid=" + invalidTotal() + "}";
    }
}
