package org.webjcvi.segment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Splits caller-supplied text on unusually long identical-character runs.
 * The v2 DNA report showed 8114 runs of length ≥ 5 but only 10 runs of
 * length ≥ 10 — those rare long banners are treated as section delimiters,
 * the way log files use {@code ==========} or {@code ----------}.
 */
public final class BannerSplitter {

    /**
     * Length where DNA homopolymers become rare (10–19: 9 events; 20+: 1).
     */
    public static final int DEFAULT_MIN_RUN = 10;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_SECTIONS = 80;
    public static final int PREVIEW_CHARS = 160;

    private static final Logger log = LoggerFactory.getLogger(BannerSplitter.class);

    public List<Section> split(String text) {
        return split(text, DEFAULT_MIN_RUN);
    }

    public List<Section> split(String text, int minRun) {
        if (text == null) {
            throw new SegmentException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new SegmentException("Text exceeds " + MAX_CHARS + " characters");
        }
        if (minRun < 2) {
            throw new SegmentException("minRun must be at least 2");
        }
        String folded = text.toUpperCase(Locale.ROOT);
        List<int[]> banners = new ArrayList<>();
        int i = 0;
        while (i < folded.length()) {
            char ch = folded.charAt(i);
            int j = i + 1;
            while (j < folded.length() && folded.charAt(j) == ch) {
                j++;
            }
            int len = j - i;
            if (len >= minRun) {
                banners.add(new int[] {i, len});
            }
            i = j;
        }
        List<Section> sections = new ArrayList<>();
        int cursor = 0;
        String previousBanner = "";
        for (int[] banner : banners) {
            int start = banner[0];
            int len = banner[1];
            addSection(sections, text, cursor, start, previousBanner);
            previousBanner = text.substring(start, start + len);
            cursor = start + len;
            if (sections.size() >= MAX_SECTIONS) {
                if (log.isDebugEnabled()) {
                    log.debug("banner.split minRun={} banners={} sections={} truncated=true chars={}",
                            minRun, banners.size(), sections.size(), text.length());
                }
                return List.copyOf(sections);
            }
        }
        addSection(sections, text, cursor, text.length(), previousBanner);
        if (sections.isEmpty()) {
            sections.add(new Section(0, 0, text.length(), preview(text), ""));
        }
        if (log.isDebugEnabled()) {
            log.debug("banner.split minRun={} banners={} sections={} chars={}",
                    minRun, banners.size(), sections.size(), text.length());
        }
        return List.copyOf(sections);
    }

    private static void addSection(
            List<Section> sections, String text, int from, int to, String banner) {
        if (from >= to || sections.size() >= MAX_SECTIONS) {
            return;
        }
        String body = text.substring(from, to);
        sections.add(new Section(sections.size(), from, body.length(), preview(body), banner));
    }

    private static String preview(String body) {
        String trimmed = body.trim();
        if (trimmed.length() <= PREVIEW_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, PREVIEW_CHARS) + "…";
    }

    public record Section(int index, int offset, int length, String preview, String banner) {
    }
}
