package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reverse Hamming of adjacent 4-mers at FASTA wrap joints versus the same
 * measurement on the linear interior tape. v12 reverse distance-0 was
 * enriched (×1.130); this asks whether that mirror lives on the wrap seam
 * (last 4 of line N vs reverse of first 4 of line N+1) or along the tape.
 */
public final class WrapReverseCensus {

    public static final int WIDTH = 4;

    private static final Logger log = LoggerFactory.getLogger(WrapReverseCensus.class);

    private final int wrapJoints;
    private final int wrapReverseZero;
    private final double wrapReverseShare;
    private final int interiorWindows;
    private final int interiorReverseZero;
    private final double interiorReverseShare;
    private final double wrapVsInterior;

    private WrapReverseCensus(
            int wrapJoints,
            int wrapReverseZero,
            double wrapReverseShare,
            int interiorWindows,
            int interiorReverseZero,
            double interiorReverseShare,
            double wrapVsInterior) {
        this.wrapJoints = wrapJoints;
        this.wrapReverseZero = wrapReverseZero;
        this.wrapReverseShare = wrapReverseShare;
        this.interiorWindows = interiorWindows;
        this.interiorReverseZero = interiorReverseZero;
        this.interiorReverseShare = interiorReverseShare;
        this.wrapVsInterior = wrapVsInterior;
    }

    public static WrapReverseCensus from(String raw, NeighborCensus interior) {
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(interior, "interior");
        if (log.isDebugEnabled()) {
            log.debug("wrap-reverse.start rawChars={} interiorWindows={}", raw.length(), interior.windows());
        }
        List<String> lines = lines(raw);
        int wrapJoints = 0;
        int wrapReverseZero = 0;
        for (int i = 0; i + 1 < lines.size(); i++) {
            String left = foldCanonical(lines.get(i));
            String right = foldCanonical(lines.get(i + 1));
            if (left.length() < WIDTH || right.length() < WIDTH) {
                continue;
            }
            wrapJoints++;
            if (reverseZero(left.substring(left.length() - WIDTH), right.substring(0, WIDTH))) {
                wrapReverseZero++;
            }
        }
        double wrapShare = wrapJoints == 0 ? 0.0 : wrapReverseZero * 1.0 / wrapJoints;
        double interiorShare = interior.reverse().zeroShare();
        double ratio = interiorShare == 0.0 ? 0.0 : wrapShare / interiorShare;
        WrapReverseCensus census = new WrapReverseCensus(
                wrapJoints,
                wrapReverseZero,
                wrapShare,
                interior.windows(),
                interior.reverse().zeroCount(),
                interiorShare,
                ratio);
        if (log.isDebugEnabled()) {
            log.debug("wrap-reverse.done {}", census.toTextRow());
        }
        return census;
    }

    private static boolean reverseZero(String left, String right) {
        for (int i = 0; i < WIDTH; i++) {
            if (left.charAt(i) != right.charAt(WIDTH - 1 - i)) {
                return false;
            }
        }
        return true;
    }

    private static String foldCanonical(String line) {
        StringBuilder out = new StringBuilder(line.length());
        for (int i = 0; i < line.length(); i++) {
            char ch = Character.toUpperCase(line.charAt(i));
            if (ch == 'A' || ch == 'T' || ch == 'G' || ch == 'C') {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static List<String> lines(String raw) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= raw.length(); i++) {
            if (i == raw.length() || raw.charAt(i) == '\n') {
                String line = raw.substring(start, i);
                if (line.endsWith("\r")) {
                    line = line.substring(0, line.length() - 1);
                }
                if (!(i == raw.length() && line.isEmpty() && !lines.isEmpty())) {
                    lines.add(line);
                }
                start = i + 1;
            }
        }
        return lines;
    }

    public int wrapJoints() {
        return wrapJoints;
    }

    public int wrapReverseZero() {
        return wrapReverseZero;
    }

    public double wrapReverseShare() {
        return wrapReverseShare;
    }

    public int interiorWindows() {
        return interiorWindows;
    }

    public int interiorReverseZero() {
        return interiorReverseZero;
    }

    public double interiorReverseShare() {
        return interiorReverseShare;
    }

    public double wrapVsInterior() {
        return wrapVsInterior;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "wrapJoints=%d wrapRev0=%d wrapShare=%.4f interior=%d interiorRev0=%d interiorShare=%.4f wrap/interior=%.3f",
                wrapJoints, wrapReverseZero, wrapReverseShare,
                interiorWindows, interiorReverseZero, interiorReverseShare, wrapVsInterior);
    }
}
