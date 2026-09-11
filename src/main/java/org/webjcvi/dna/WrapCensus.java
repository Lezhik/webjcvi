package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Line-width census of the raw FASTA-like tape. The v1 report only stored a
 * line count; this version treats wrap width as the frame size of the tape.
 */
public final class WrapCensus {

    private final int lineCount;
    private final int minWidth;
    private final int maxWidth;
    private final int medianWidth;
    private final int modalWidth;

    private WrapCensus(int lineCount, int minWidth, int maxWidth, int medianWidth, int modalWidth) {
        this.lineCount = lineCount;
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        this.medianWidth = medianWidth;
        this.modalWidth = modalWidth;
    }

    public static WrapCensus fromRaw(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (raw.isEmpty()) {
            return new WrapCensus(0, 0, 0, 0, 0);
        }
        List<Integer> widths = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= raw.length(); i++) {
            if (i == raw.length() || raw.charAt(i) == '\n') {
                String line = raw.substring(start, i);
                if (line.endsWith("\r")) {
                    line = line.substring(0, line.length() - 1);
                }
                if (!(i == raw.length() && line.isEmpty() && !widths.isEmpty())) {
                    widths.add(line.length());
                }
                start = i + 1;
            }
        }
        List<Integer> sorted = new ArrayList<>(widths);
        Collections.sort(sorted);
        int min = sorted.getFirst();
        int max = sorted.getLast();
        int median = sorted.get(sorted.size() / 2);
        int modal = modalValue(widths);
        return new WrapCensus(widths.size(), min, max, median, modal);
    }

    private static int modalValue(List<Integer> widths) {
        int bestWidth = widths.getFirst();
        int bestCount = 0;
        for (int width : widths) {
            int count = 0;
            for (int other : widths) {
                if (other == width) {
                    count++;
                }
            }
            if (count > bestCount || (count == bestCount && width < bestWidth)) {
                bestCount = count;
                bestWidth = width;
            }
        }
        return bestWidth;
    }

    public int lineCount() {
        return lineCount;
    }

    public int minWidth() {
        return minWidth;
    }

    public int maxWidth() {
        return maxWidth;
    }

    public int medianWidth() {
        return medianWidth;
    }

    public int modalWidth() {
        return modalWidth;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT, "lines=%d min=%d median=%d mode=%d max=%d",
                lineCount, minWidth, medianWidth, modalWidth, maxWidth);
    }
}
