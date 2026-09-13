package org.webjcvi.runway;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.cliff.CliffException;
import org.webjcvi.cliff.CliffScan;
import org.webjcvi.tape.ScratchTape;

/**
 * Layout runway of newline-delimited rows. v24 DNA residual twins fork at
 * uniqueAt (minFork 19 / modalFork 19 / maxFork 20 of two groups). Iteration
 * 23's clock-cliff then found log-journal forkAt just past the 16-character
 * clock while cliffAt sat near 113 — the identifier is not the first split.
 * This scan keeps newlines, reuses {@link CliffScan}, and reports the
 * <em>runway</em> {@code cliffAt - forkAt} (0 if either is missing or the
 * cliff is not past the fork). Inverse of clock-cliff (there forkAt itself
 * was the start column). DNA uniqueAt=20 is the tightness threshold.
 */
public final class RunwayScan {

    public static final int TILE = CliffScan.TILE;
    public static final int DNA_UNIQUE_AT = 20;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;

    private static final Logger log = LoggerFactory.getLogger(RunwayScan.class);

    private final CliffScan cliffs;

    public RunwayScan() {
        this(new CliffScan());
    }

    public RunwayScan(CliffScan cliffs) {
        this.cliffs = cliffs;
    }

    public Scan profile(String text) {
        CliffScan.Scan cliff;
        try {
            cliff = cliffs.profile(text);
        } catch (CliffException ex) {
            throw new RunwayException(ex.getMessage());
        }
        int runway = 0;
        if (cliff.forkAt() > 0 && cliff.cliffAt() > cliff.forkAt()) {
            runway = cliff.cliffAt() - cliff.forkAt();
        }
        boolean stretched = runway > DNA_UNIQUE_AT;
        Scan scan = new Scan(
                cliff.lineCount(),
                cliff.scanned(),
                TILE,
                cliff.forkAt(),
                cliff.cliffAt(),
                runway,
                stretched,
                cliff.shareAt16(),
                cliff.shareAtCliff(),
                cliff.topPrefix(),
                cliff.topCount());
        if (log.isDebugEnabled()) {
            log.debug("runway.profile chars={} {}", text == null ? 0 : text.length(), scan.summary());
        }
        return scan;
    }

    public record Scan(
            int lineCount,
            int scanned,
            int tileLength,
            int forkAt,
            int cliffAt,
            int runway,
            boolean stretched,
            double shareAt16,
            double shareAtCliff,
            String topPrefix,
            int topCount) {

        public String summary() {
            return String.format(Locale.ROOT,
                    "lines=%d scanned=%d tile=%d forkAt=%d cliffAt=%d runway=%d stretched=%s share16=%.4f cliff=%.4f topCount=%d",
                    lineCount, scanned, tileLength, forkAt, cliffAt, runway, stretched,
                    shareAt16, shareAtCliff, topCount);
        }
    }
}
