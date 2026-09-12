package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Dinucleotides at FASTA wrap joints (last base of line N + first of line N+1).
 * v3 measured pairing <em>inside</em> each frame; this census asks what sits
 * on the glue between frames.
 */
public final class WrapJointCensus {

    private final int jointCount;
    private final int complementaryJoints;
    private final String topJoint;
    private final long topJointCount;
    private final Map<String, Long> topJoints;
    private final Map<String, Long> allJoints;

    private WrapJointCensus(
            int jointCount,
            int complementaryJoints,
            String topJoint,
            long topJointCount,
            Map<String, Long> topJoints,
            Map<String, Long> allJoints) {
        this.jointCount = jointCount;
        this.complementaryJoints = complementaryJoints;
        this.topJoint = topJoint;
        this.topJointCount = topJointCount;
        this.topJoints = Collections.unmodifiableMap(new LinkedHashMap<>(topJoints));
        this.allJoints = Collections.unmodifiableMap(new LinkedHashMap<>(allJoints));
    }

    public static WrapJointCensus fromRaw(String raw) {
        Objects.requireNonNull(raw, "raw");
        List<String> lines = lines(raw);
        Map<String, Long> counts = new LinkedHashMap<>();
        int joints = 0;
        int complementary = 0;
        for (int i = 0; i + 1 < lines.size(); i++) {
            String left = lines.get(i);
            String right = lines.get(i + 1);
            if (left.isEmpty() || right.isEmpty()) {
                continue;
            }
            char a = Character.toUpperCase(left.charAt(left.length() - 1));
            char b = Character.toUpperCase(right.charAt(0));
            if ("ATGC".indexOf(a) < 0 || "ATGC".indexOf(b) < 0) {
                continue;
            }
            String dimer = "" + a + b;
            counts.merge(dimer, 1L, Long::sum);
            joints++;
            if (paired(a, b)) {
                complementary++;
            }
        }
        List<Map.Entry<String, Long>> ranked = new ArrayList<>(counts.entrySet());
        ranked.sort((left, right) -> {
            int byCount = Long.compare(right.getValue(), left.getValue());
            return byCount != 0 ? byCount : left.getKey().compareTo(right.getKey());
        });
        Map<String, Long> top = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(8, ranked.size()); i++) {
            var entry = ranked.get(i);
            top.put(entry.getKey(), entry.getValue());
        }
        String best = ranked.isEmpty() ? "" : ranked.getFirst().getKey();
        long bestCount = ranked.isEmpty() ? 0L : ranked.getFirst().getValue();
        return new WrapJointCensus(joints, complementary, best, bestCount, top, counts);
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

    private static boolean paired(char left, char right) {
        return (left == 'A' && right == 'T')
                || (left == 'T' && right == 'A')
                || (left == 'G' && right == 'C')
                || (left == 'C' && right == 'G');
    }

    public int jointCount() {
        return jointCount;
    }

    public int complementaryJoints() {
        return complementaryJoints;
    }

    public double complementaryShare() {
        return jointCount == 0 ? 0.0 : complementaryJoints * 100.0 / jointCount;
    }

    public String topJoint() {
        return topJoint;
    }

    public long topJointCount() {
        return topJointCount;
    }

    public Map<String, Long> topJoints() {
        return topJoints;
    }

    public Map<String, Long> allJoints() {
        return allJoints;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT, "joints=%d complementary=%.1f%% top=%s x%d",
                jointCount, complementaryShare(), topJoint.isEmpty() ? "-" : topJoint, topJointCount);
    }
}
