package org.webjcvi.reflow;

import java.util.ArrayList;
import java.util.List;
import org.webjcvi.tape.ScratchTape;

/**
 * Reconstructs paragraphs from hard-wrapped caller text. The v4 DNA report
 * showed almost every FASTA line is width 70, wrap joints are dominated by
 * same-base dimers ({@code TT}, {@code AA}) rather than complementary pairs
 * (only 26.6%) — the wrap is a display encoding of a linear tape, and a short
 * line is the real break.
 */
public final class WrapReflow {

    public static final int DEFAULT_WIDTH = 70;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_PARAGRAPHS = 80;
    public static final int PREVIEW_CHARS = 240;

    public Result unwrap(String text) {
        return unwrap(text, DEFAULT_WIDTH);
    }

    public Result unwrap(String text, int width) {
        if (text == null) {
            throw new ReflowException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new ReflowException("Text exceeds " + MAX_CHARS + " characters");
        }
        int frame = width <= 0 ? DEFAULT_WIDTH : width;
        if (frame < 8) {
            throw new ReflowException("width must be at least 8");
        }
        List<String> lines = splitLines(text);
        List<String> paragraphs = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int stitches = 0;
        for (String line : lines) {
            if (line.isBlank()) {
                flush(paragraphs, current);
                continue;
            }
            String piece = line.strip();
            if (current.isEmpty()) {
                current.append(piece);
            } else {
                current.append(' ').append(piece);
                stitches++;
            }
            if (line.length() < frame) {
                flush(paragraphs, current);
            }
        }
        flush(paragraphs, current);
        int total = paragraphs.size();
        List<Paragraph> shown = new ArrayList<>();
        int limit = Math.min(MAX_PARAGRAPHS, paragraphs.size());
        for (int i = 0; i < limit; i++) {
            shown.add(new Paragraph(i, paragraphs.get(i).length(), preview(paragraphs.get(i))));
        }
        return new Result(frame, lines.size(), stitches, total, List.copyOf(shown));
    }

    private static void flush(List<String> paragraphs, StringBuilder current) {
        if (current.isEmpty()) {
            return;
        }
        paragraphs.add(current.toString());
        current.setLength(0);
    }

    private static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == '\n') {
                String line = text.substring(start, i);
                if (line.endsWith("\r")) {
                    line = line.substring(0, line.length() - 1);
                }
                if (!(i == text.length() && line.isEmpty() && !lines.isEmpty())) {
                    lines.add(line);
                }
                start = i + 1;
            }
        }
        return lines;
    }

    private static String preview(String body) {
        if (body.length() <= PREVIEW_CHARS) {
            return body;
        }
        return body.substring(0, PREVIEW_CHARS) + "…";
    }

    public record Result(
            int width, int sourceLines, int stitches, int paragraphCount, List<Paragraph> paragraphs) {
    }

    public record Paragraph(int index, int length, String preview) {
    }
}
