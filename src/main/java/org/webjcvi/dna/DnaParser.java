package org.webjcvi.dna;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses FASTA-like DNA without a {@code >} header. Whitespace and line
 * wrapping are ignored, letters are normalized to uppercase, and unexpected
 * characters are counted rather than treated as fatal.
 */
public final class DnaParser {

    private static final Logger log = LoggerFactory.getLogger(DnaParser.class);

    public DnaSequence parse(String rawContent) {
        String source = rawContent == null ? "" : rawContent;
        if (log.isDebugEnabled()) {
            log.debug("dna-parse.start chars={}", source.length());
        }
        StringBuilder normalized = new StringBuilder(source.length());
        Map<Character, Long> canonical = zeroed(DnaSequence.CANONICAL_BASES);
        Map<Character, Long> ambiguous = new LinkedHashMap<>();
        Map<Character, Long> invalid = new LinkedHashMap<>();
        int lineCount = countLines(source);

        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            char upper = Character.toUpperCase(ch);
            if (DnaSequence.CANONICAL_BASES.indexOf(upper) >= 0) {
                canonical.merge(upper, 1L, Long::sum);
                normalized.append(upper);
            } else if (DnaSequence.IUPAC_AMBIGUITY.indexOf(upper) >= 0) {
                ambiguous.merge(upper, 1L, Long::sum);
                normalized.append(upper);
            } else {
                invalid.merge(ch, 1L, Long::sum);
            }
        }

        DnaSequence parsed = new DnaSequence(normalized.toString(), canonical, ambiguous, invalid, lineCount);
        if (log.isDebugEnabled()) {
            log.debug("dna-parse.done length={} invalid={} ambiguous={}",
                    parsed.length(), parsed.invalidTotal(), parsed.ambiguousTotal());
        }
        return parsed;
    }

    private static Map<Character, Long> zeroed(String alphabet) {
        Map<Character, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i < alphabet.length(); i++) {
            counts.put(alphabet.charAt(i), 0L);
        }
        return counts;
    }

    private static int countLines(String source) {
        if (source.isEmpty()) {
            return 0;
        }
        int lines = 1;
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                lines++;
            }
        }
        if (source.endsWith("\n") && lines > 0) {
            lines--;
        }
        return lines;
    }
}
