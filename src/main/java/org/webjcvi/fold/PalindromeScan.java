package org.webjcvi.fold;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.webjcvi.tape.ScratchTape;

/**
 * Finds even palindromes (length ≥ 4, not homopolymers) on a whitespace-stripped,
 * case-folded tape. The v8 lag census peaked at lag 1 only ×1.134 (nearly
 * memoryless) while lag 2 was depleted (0.944) — XYX 3-palindromes are
 * discouraged. Forward identity is not a protocol; Chargaff A≈T is a fold.
 */
public final class PalindromeScan {

    public static final int DEFAULT_MIN = 4;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_HITS = 40;
    public static final int MAX_RADIUS = 40;
    public static final int PREVIEW_CHARS = 80;

    public Scan find(String text) {
        return find(text, DEFAULT_MIN);
    }

    public Scan find(String text, int minLength) {
        if (text == null) {
            throw new FoldException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new FoldException("Text exceeds " + MAX_CHARS + " characters");
        }
        int floor = minLength < 4 ? DEFAULT_MIN : minLength;
        StringBuilder tape = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char raw = text.charAt(i);
            if (Character.isWhitespace(raw)) {
                continue;
            }
            tape.append(Character.toUpperCase(raw));
        }
        String folded = tape.toString();
        List<Hit> hits = new ArrayList<>();
        int n = folded.length();
        for (int i = 0; i < n; i++) {
            expandEven(folded, i, floor, hits);
        }
        hits.sort(Comparator
                .comparingInt(Hit::length).reversed()
                .thenComparingInt(Hit::offset));
        if (hits.size() > MAX_HITS) {
            hits = new ArrayList<>(hits.subList(0, MAX_HITS));
            for (int i = 0; i < hits.size(); i++) {
                Hit h = hits.get(i);
                hits.set(i, new Hit(i, h.offset(), h.length(), h.preview()));
            }
        } else {
            for (int i = 0; i < hits.size(); i++) {
                Hit h = hits.get(i);
                hits.set(i, new Hit(i, h.offset(), h.length(), h.preview()));
            }
        }
        int longest = hits.isEmpty() ? 0 : hits.get(0).length();
        return new Scan(folded.length(), hits.size(), longest, List.copyOf(hits));
    }

    private static void expandEven(String folded, int leftOfCenter, int floor, List<Hit> hits) {
        int n = folded.length();
        int L = leftOfCenter;
        int R = leftOfCenter + 1;
        int radius = 0;
        while (L >= 0 && R < n && radius < MAX_RADIUS && folded.charAt(L) == folded.charAt(R)) {
            radius++;
            int len = 2 * radius;
            if (len >= floor && !homopolymer(folded, L, len)) {
                String body = folded.substring(L, L + len);
                hits.add(new Hit(hits.size(), L, len, preview(body)));
            }
            L--;
            R++;
        }
    }

    private static boolean homopolymer(String folded, int offset, int len) {
        char first = folded.charAt(offset);
        for (int i = 1; i < len; i++) {
            if (folded.charAt(offset + i) != first) {
                return false;
            }
        }
        return true;
    }

    private static String preview(String body) {
        if (body.length() <= PREVIEW_CHARS) {
            return body;
        }
        return body.substring(0, PREVIEW_CHARS) + "…";
    }

    public record Scan(int scanned, int hitCount, int longest, List<Hit> hits) {

        public String summary() {
            return String.format(Locale.ROOT, "scanned=%d palindromes=%d longest=%d",
                    scanned, hitCount, longest);
        }
    }

    public record Hit(int index, int offset, int length, String preview) {
    }
}
