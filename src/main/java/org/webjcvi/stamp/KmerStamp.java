package org.webjcvi.stamp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.webjcvi.tape.ScratchTape;

/**
 * Counts overlapping k-mers on a linear, whitespace-stripped, case-folded
 * tape. The v7 codon census was phase-invariant: {@code TTT} led all three
 * frames and GC spread was 1.15%, so the signal is the mode k-mer, not an ORF.
 */
public final class KmerStamp {

    public static final int DEFAULT_K = 3;
    public static final int MAX_K = 8;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_STAMPS = 20;
    public static final int MIN_COUNT = 2;

    public Census rank(String text) {
        return rank(text, DEFAULT_K);
    }

    public Census rank(String text, int k) {
        if (text == null) {
            throw new StampException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new StampException("Text exceeds " + MAX_CHARS + " characters");
        }
        int width = k < 2 ? DEFAULT_K : Math.min(k, MAX_K);
        StringBuilder tape = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char raw = text.charAt(i);
            if (Character.isWhitespace(raw)) {
                continue;
            }
            tape.append(Character.toUpperCase(raw));
        }
        Map<String, Long> freq = new LinkedHashMap<>();
        if (tape.length() >= width) {
            for (int i = 0; i + width <= tape.length(); i++) {
                String mer = tape.substring(i, i + width);
                freq.merge(mer, 1L, Long::sum);
            }
        }
        List<Stamp> stamps = new ArrayList<>();
        List<Map.Entry<String, Long>> ranked = new ArrayList<>(freq.entrySet());
        ranked.sort(Comparator
                .<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));
        for (var entry : ranked) {
            if (entry.getValue() < MIN_COUNT) {
                continue;
            }
            if (stamps.size() >= MAX_STAMPS) {
                break;
            }
            stamps.add(new Stamp(stamps.size(), entry.getKey(), entry.getValue()));
        }
        String top = stamps.isEmpty() ? "" : stamps.get(0).kmer();
        long topCount = stamps.isEmpty() ? 0L : stamps.get(0).count();
        return new Census(width, tape.length(), freq.size(), top, topCount, List.copyOf(stamps));
    }

    public record Census(int k, int scanned, int distinct, String topKmer, long topCount, List<Stamp> stamps) {

        public String summary() {
            return String.format(Locale.ROOT, "k=%d scanned=%d distinct=%d top=%s x%d stamps=%d",
                    k, scanned, distinct, topKmer.isEmpty() ? "-" : topKmer, topCount, stamps.size());
        }
    }

    public record Stamp(int index, String kmer, long count) {
    }
}
