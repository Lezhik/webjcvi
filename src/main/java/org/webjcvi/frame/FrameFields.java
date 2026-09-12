package org.webjcvi.frame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.tape.ScratchTape;

/**
 * Three-slot wrap-frame records. The v15 map-phase census peaked at three
 * different columns: RC at 4 (share 0.0111), reverse at 19 (0.0140), identity
 * at 58 (0.0121) — the wrap frame is a typed record, not one compositional
 * column. Extract the 8-character slots (adjacent 4+4 windows) at those
 * offsets from each width-70 frame. Newlines are dropped so the scan matches
 * FASTA concatenation. Inverse of phase-joint Hamming (there only reverse at
 * 19 was a hit test).
 */
public final class FrameFields {

    public static final int DEFAULT_WRAP = 70;
    public static final int DEFAULT_SLOT = 8;
    public static final int RC_COLUMN = 4;
    public static final int REVERSE_COLUMN = 19;
    public static final int IDENTITY_COLUMN = 58;
    public static final int MAX_WRAP = 256;
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;
    public static final int MAX_RECORDS = 40;

    private static final Logger log = LoggerFactory.getLogger(FrameFields.class);

    public Scan extract(String text) {
        return extract(text, DEFAULT_WRAP);
    }

    public Scan extract(String text, int wrapWidth) {
        if (text == null) {
            throw new FrameException("Text is missing");
        }
        if (text.length() > MAX_CHARS) {
            throw new FrameException("Text exceeds " + MAX_CHARS + " characters");
        }
        int wrap = wrapWidth < IDENTITY_COLUMN + DEFAULT_SLOT ? DEFAULT_WRAP : Math.min(wrapWidth, MAX_WRAP);
        StringBuilder folded = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n' || ch == '\r') {
                continue;
            }
            folded.append(Character.toUpperCase(ch));
        }
        String tape = folded.toString();
        List<Record> records = new ArrayList<>();
        Map<String, Long> rcCounts = new LinkedHashMap<>();
        Map<String, Long> reverseCounts = new LinkedHashMap<>();
        Map<String, Long> identityCounts = new LinkedHashMap<>();
        int recordCount = 0;
        for (int i = 0; i + wrap <= tape.length(); i += wrap) {
            String rc = tape.substring(i + RC_COLUMN, i + RC_COLUMN + DEFAULT_SLOT);
            String reverse = tape.substring(i + REVERSE_COLUMN, i + REVERSE_COLUMN + DEFAULT_SLOT);
            String identity = tape.substring(i + IDENTITY_COLUMN, i + IDENTITY_COLUMN + DEFAULT_SLOT);
            recordCount++;
            rcCounts.merge(rc, 1L, Long::sum);
            reverseCounts.merge(reverse, 1L, Long::sum);
            identityCounts.merge(identity, 1L, Long::sum);
            if (records.size() < MAX_RECORDS) {
                records.add(new Record(records.size(), i, rc, reverse, identity));
            }
        }
        Scan scan = new Scan(
                wrap,
                recordCount,
                topKey(rcCounts),
                topKey(reverseCounts),
                topKey(identityCounts),
                List.copyOf(records));
        if (log.isDebugEnabled()) {
            log.debug("frame.extract chars={} {}", text.length(), scan.summary());
        }
        return scan;
    }

    private static String topKey(Map<String, Long> counts) {
        String best = "";
        long bestCount = 0L;
        for (var entry : counts.entrySet()) {
            if (entry.getValue() > bestCount
                    || (entry.getValue() == bestCount && (best.isEmpty() || entry.getKey().compareTo(best) < 0))) {
                best = entry.getKey();
                bestCount = entry.getValue();
            }
        }
        return best;
    }

    public record Scan(
            int wrapWidth,
            int recordCount,
            String topRc,
            String topReverse,
            String topIdentity,
            List<Record> records) {

        public String summary() {
            return String.format(Locale.ROOT, "wrap=%d records=%d topRc=%s topReverse=%s topIdentity=%s",
                    wrapWidth, recordCount, blank(topRc), blank(topReverse), blank(topIdentity));
        }

        private static String blank(String value) {
            return value == null || value.isEmpty() ? "-" : value;
        }
    }

    public record Record(int index, int offset, String rc, String reverse, String identity) {
    }
}
