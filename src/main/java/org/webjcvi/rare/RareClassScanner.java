package org.webjcvi.rare;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Finds islands of the rare character class in caller-supplied text. The v5
 * DNA report showed wrap joints are not a special seam (same-base 35.9% vs
 * 36.1%); the unused signal is GC at 24.14% — the minority mass. This scanner
 * takes symbols from the rarest upward until they cover that share, then lists
 * consecutive runs of those symbols (IDs, hex, punctuation clusters in logs).
 */
public final class RareClassScanner {

    /** Measured GC share of the canonical tape. */
    public static final double DEFAULT_MASS = 0.2414;
    public static final int DEFAULT_MIN_ISLAND = 3;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_ISLANDS = 80;
    public static final int PREVIEW_CHARS = 80;

    private static final Logger log = LoggerFactory.getLogger(RareClassScanner.class);

    public Scan scan(String text) {
        return scan(text, DEFAULT_MIN_ISLAND);
    }

    /**
     * Rare-class alphabet of {@code text} (bottom {@link #DEFAULT_MASS} of
     * non-whitespace mass). Shared with the rare-break tokenizer.
     */
    public Set<Character> rareSymbols(String text) {
        if (text == null) {
            throw new RareException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new RareException("Text exceeds " + MAX_CHARS + " characters");
        }
        Freq freq = frequencies(text);
        Set<Character> symbols = rareClass(freq.counts(), freq.total());
        if (log.isDebugEnabled()) {
            log.debug("rare.symbols chars={} classSize={} total={}", text.length(), symbols.size(), freq.total());
        }
        return symbols;
    }

    public Scan scan(String text, int minIsland) {
        if (text == null) {
            throw new RareException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new RareException("Text exceeds " + MAX_CHARS + " characters");
        }
        int floor = minIsland < 2 ? DEFAULT_MIN_ISLAND : minIsland;
        Freq freq = frequencies(text);
        Set<Character> rare = rareClass(freq.counts(), freq.total());
        String rareLabel = rareLabel(rare);
        long total = freq.total();
        if (total == 0 || rare.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("rare.scan chars={} empty class=[{}]", text.length(), rareLabel);
            }
            return new Scan(rareLabel, total, 0, 0, List.of());
        }
        List<Island> islands = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char folded = Character.toUpperCase(text.charAt(i));
            if (Character.isWhitespace(text.charAt(i)) || !rare.contains(folded)) {
                i++;
                continue;
            }
            int j = i + 1;
            while (j < text.length()) {
                char next = text.charAt(j);
                if (Character.isWhitespace(next)) {
                    break;
                }
                if (!rare.contains(Character.toUpperCase(next))) {
                    break;
                }
                j++;
            }
            int len = j - i;
            if (len >= floor && islands.size() < MAX_ISLANDS) {
                String body = text.substring(i, j);
                islands.add(new Island(islands.size(), i, len, preview(body)));
            }
            i = j;
        }
        Scan scan = new Scan(rareLabel, total, rare.size(), islands.size(), List.copyOf(islands));
        if (log.isDebugEnabled()) {
            log.debug("rare.scan chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    static Freq frequencies(String text) {
        Map<Character, Long> freq = new LinkedHashMap<>();
        long total = 0;
        for (int i = 0; i < text.length(); i++) {
            char raw = text.charAt(i);
            if (Character.isWhitespace(raw)) {
                continue;
            }
            char ch = Character.toUpperCase(raw);
            freq.merge(ch, 1L, Long::sum);
            total++;
        }
        return new Freq(freq, total);
    }

    static Set<Character> rareClass(Map<Character, Long> freq, long total) {
        Set<Character> rare = new LinkedHashSet<>();
        if (total <= 0 || freq.isEmpty()) {
            return rare;
        }
        List<Map.Entry<Character, Long>> ranked = new ArrayList<>(freq.entrySet());
        ranked.sort(Comparator
                .<Map.Entry<Character, Long>>comparingLong(Map.Entry::getValue)
                .thenComparing(Map.Entry::getKey));
        long need = Math.max(1L, Math.round(total * DEFAULT_MASS));
        long acc = 0;
        double majorityFloor = 1.0 - DEFAULT_MASS;
        for (var entry : ranked) {
            double share = entry.getValue() * 1.0 / total;
            if (share > majorityFloor) {
                continue;
            }
            rare.add(entry.getKey());
            acc += entry.getValue();
            if (acc >= need) {
                break;
            }
        }
        return rare;
    }

    private static String rareLabel(Set<Character> rare) {
        StringBuilder out = new StringBuilder();
        for (char ch : rare) {
            out.append(ch);
        }
        return out.toString();
    }

    private static String preview(String body) {
        if (body.length() <= PREVIEW_CHARS) {
            return body;
        }
        return body.substring(0, PREVIEW_CHARS) + "…";
    }

    public record Scan(String rareClass, long scanned, int rareSymbolCount, int islandCount, List<Island> islands) {

        public String summary() {
            return String.format(Locale.ROOT, "rare=[%s] symbols=%d islands=%d scanned=%d",
                    rareClass, rareSymbolCount, islandCount, scanned);
        }
    }

    public record Island(int index, int offset, int length, String preview) {
    }

    record Freq(Map<Character, Long> counts, long total) {
    }
}
