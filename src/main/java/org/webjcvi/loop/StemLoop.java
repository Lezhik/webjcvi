package org.webjcvi.loop;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import org.webjcvi.tape.ScratchTape;

/**
 * Extracts complementary delimiter spans ({@code ()[]{}<>}) and the loop
 * they enclose. The v9 foldback census showed reverse-complement stems beat
 * same-base palindromes (longest 34 vs 24; 96208 vs 40569) and odd RC outran
 * even (51553 vs 44655), so the protocol is pairing around a gap, not a
 * solid letter palindrome. Quotes are skipped: they are same-base folds.
 */
public final class StemLoop {

    public static final int DEFAULT_MIN_LOOP = 1;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_HITS = 40;
    public static final int PREVIEW_CHARS = 80;

    public Scan extract(String text) {
        return extract(text, DEFAULT_MIN_LOOP);
    }

    public Scan extract(String text, int minLoop) {
        if (text == null) {
            throw new LoopException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new LoopException("Text exceeds " + MAX_CHARS + " characters");
        }
        int floor = minLoop < 1 ? DEFAULT_MIN_LOOP : minLoop;
        List<Span> spans = new ArrayList<>();
        Deque<Open> stack = new ArrayDeque<>();
        int leftoverCloses = 0;
        int nested = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isOpen(ch)) {
                stack.push(new Open(ch, i, stack.size()));
            } else if (isClose(ch)) {
                if (stack.isEmpty() || !matches(stack.peek().ch(), ch)) {
                    leftoverCloses++;
                    continue;
                }
                Open open = stack.pop();
                int loopLength = i - open.index() - 1;
                if (open.depth() > 0) {
                    nested++;
                }
                if (loopLength >= floor) {
                    String interior = text.substring(open.index() + 1, i);
                    spans.add(new Span(spans.size(), open.index(), i - open.index() + 1,
                            loopLength, String.valueOf(open.ch()), String.valueOf(ch),
                            preview(interior)));
                }
            }
        }
        int leftoverOpens = stack.size();
        spans.sort(Comparator
                .comparingInt(Span::loopLength).reversed()
                .thenComparingInt(Span::offset));
        if (spans.size() > MAX_HITS) {
            spans = new ArrayList<>(spans.subList(0, MAX_HITS));
        }
        for (int i = 0; i < spans.size(); i++) {
            Span s = spans.get(i);
            spans.set(i, new Span(i, s.offset(), s.length(), s.loopLength(),
                    s.opener(), s.closer(), s.preview()));
        }
        int longestLoop = spans.isEmpty() ? 0 : spans.get(0).loopLength();
        return new Scan(text.length(), spans.size(), nested, leftoverOpens, leftoverCloses,
                longestLoop, List.copyOf(spans));
    }

    private static boolean isOpen(char ch) {
        return ch == '(' || ch == '[' || ch == '{' || ch == '<';
    }

    private static boolean isClose(char ch) {
        return ch == ')' || ch == ']' || ch == '}' || ch == '>';
    }

    private static boolean matches(char open, char close) {
        return (open == '(' && close == ')')
                || (open == '[' && close == ']')
                || (open == '{' && close == '}')
                || (open == '<' && close == '>');
    }

    private static String preview(String body) {
        if (body.length() <= PREVIEW_CHARS) {
            return body;
        }
        return body.substring(0, PREVIEW_CHARS) + "…";
    }

    public record Scan(
            int scanned,
            int spanCount,
            int nested,
            int leftoverOpens,
            int leftoverCloses,
            int longestLoop,
            List<Span> spans) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "scanned=%d spans=%d nested=%d leftoverOpens=%d leftoverCloses=%d longestLoop=%d",
                    scanned, spanCount, nested, leftoverOpens, leftoverCloses, longestLoop);
        }
    }

    public record Span(
            int index,
            int offset,
            int length,
            int loopLength,
            String opener,
            String closer,
            String preview) {
    }

    private record Open(char ch, int index, int depth) {
    }
}
