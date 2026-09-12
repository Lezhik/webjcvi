package org.webjcvi.token;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.rare.RareClassScanner;
import org.webjcvi.rare.RareException;
import org.webjcvi.tape.ScratchTape;

/**
 * Splits caller text on the rare character class. The v6 census showed mixed
 * GC islands do not cluster (longest 9, none ≥ 10) while AT islands run to 54
 * and mean ~4: the minority class is a break, the majority class is the token.
 * Whitespace also breaks, the way non-canonical bases skip island accounting.
 */
public final class RareBreakTokenizer {

    /** Mean AT-island length on the canonical tape is ~4; drop length-1 crumbs. */
    public static final int DEFAULT_MIN_TOKEN = 2;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_TOKENS = 80;
    public static final int PREVIEW_CHARS = 80;

    private static final Logger log = LoggerFactory.getLogger(RareBreakTokenizer.class);

    private final RareClassScanner rareScanner;

    public RareBreakTokenizer() {
        this(new RareClassScanner());
    }

    public RareBreakTokenizer(RareClassScanner rareScanner) {
        this.rareScanner = rareScanner;
    }

    public Cut cut(String text) {
        return cut(text, DEFAULT_MIN_TOKEN);
    }

    public Cut cut(String text, int minToken) {
        if (text == null) {
            throw new TokenException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new TokenException("Text exceeds " + MAX_CHARS + " characters");
        }
        int floor = minToken < 2 ? DEFAULT_MIN_TOKEN : minToken;
        Set<Character> rare;
        try {
            rare = rareScanner.rareSymbols(text);
        } catch (RareException e) {
            throw new TokenException(e.getMessage());
        }
        String rareLabel = rareLabel(rare);
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char raw = text.charAt(i);
            char folded = Character.toUpperCase(raw);
            if (Character.isWhitespace(raw) || rare.contains(folded)) {
                i++;
                continue;
            }
            int j = i + 1;
            while (j < text.length()) {
                char next = text.charAt(j);
                if (Character.isWhitespace(next)) {
                    break;
                }
                if (rare.contains(Character.toUpperCase(next))) {
                    break;
                }
                j++;
            }
            int len = j - i;
            if (len >= floor && tokens.size() < MAX_TOKENS) {
                String body = text.substring(i, j);
                tokens.add(new Token(tokens.size(), i, len, preview(body)));
            }
            i = j;
        }
        Cut cut = new Cut(rareLabel, rare.size(), tokens.size(), List.copyOf(tokens));
        if (log.isDebugEnabled()) {
            log.debug("token.cut chars={} {}", text.length(), cut.summary());
        }
        return cut;
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

    public record Cut(String rareClass, int rareSymbolCount, int tokenCount, List<Token> tokens) {

        public String summary() {
            return String.format(Locale.ROOT, "rare=[%s] symbols=%d tokens=%d",
                    rareClass, rareSymbolCount, tokenCount);
        }
    }

    public record Token(int index, int offset, int length, String preview) {
    }
}
