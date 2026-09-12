package org.webjcvi.dna;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overlapping dinucleotides of the whole tape versus dinucleotides sampled
 * only at wrap joints. v4 listed joint dimers in isolation; this contrast
 * asks whether the glue is a special protocol or just the global dimer
 * background (AT-rich TT/AA).
 */
public final class DimerContrast {

    private static final Logger log = LoggerFactory.getLogger(DimerContrast.class);

    public record Row(String dimer, long globalCount, double globalShare, long jointCount, double jointShare, double enrichment) {
    }

    private final long globalDimers;
    private final int jointCount;
    private final double sameBaseGlobalShare;
    private final double sameBaseJointShare;
    private final String mostEnriched;
    private final double mostEnrichedValue;
    private final String mostDepleted;
    private final double mostDepletedValue;
    private final List<Row> topEnriched;

    private DimerContrast(
            long globalDimers,
            int jointCount,
            double sameBaseGlobalShare,
            double sameBaseJointShare,
            String mostEnriched,
            double mostEnrichedValue,
            String mostDepleted,
            double mostDepletedValue,
            List<Row> topEnriched) {
        this.globalDimers = globalDimers;
        this.jointCount = jointCount;
        this.sameBaseGlobalShare = sameBaseGlobalShare;
        this.sameBaseJointShare = sameBaseJointShare;
        this.mostEnriched = mostEnriched;
        this.mostEnrichedValue = mostEnrichedValue;
        this.mostDepleted = mostDepleted;
        this.mostDepletedValue = mostDepletedValue;
        this.topEnriched = List.copyOf(topEnriched);
    }

    public static DimerContrast from(DnaSequence sequence, WrapJointCensus joints) {
        Objects.requireNonNull(sequence, "sequence");
        Objects.requireNonNull(joints, "joints");
        if (log.isDebugEnabled()) {
            log.debug("dimer-contrast.start length={} joints={}", sequence.length(), joints.jointCount());
        }
        Map<String, Long> global = overlapping(sequence.normalized());
        long globalTotal = global.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Long> jointMap = joints.allJoints();
        int jointTotal = joints.jointCount();
        List<Row> rows = new ArrayList<>();
        long sameGlobal = 0;
        long sameJoint = 0;
        for (int i = 0; i < DnaSequence.CANONICAL_BASES.length(); i++) {
            for (int j = 0; j < DnaSequence.CANONICAL_BASES.length(); j++) {
                String dimer = "" + DnaSequence.CANONICAL_BASES.charAt(i) + DnaSequence.CANONICAL_BASES.charAt(j);
                long g = global.getOrDefault(dimer, 0L);
                long k = jointMap.getOrDefault(dimer, 0L);
                double gShare = globalTotal == 0 ? 0.0 : g / (double) globalTotal;
                double kShare = jointTotal == 0 ? 0.0 : k / (double) jointTotal;
                double enrich = gShare == 0.0 ? 0.0 : kShare / gShare;
                rows.add(new Row(dimer, g, gShare, k, kShare, enrich));
                if (dimer.charAt(0) == dimer.charAt(1)) {
                    sameGlobal += g;
                    sameJoint += k;
                }
            }
        }
        rows.sort((left, right) -> {
            int byEnrich = Double.compare(right.enrichment(), left.enrichment());
            return byEnrich != 0 ? byEnrich : left.dimer().compareTo(right.dimer());
        });
        String enriched = rows.isEmpty() ? "" : rows.getFirst().dimer();
        double enrichedValue = rows.isEmpty() ? 0.0 : rows.getFirst().enrichment();
        String depleted = "";
        double depletedValue = 0.0;
        if (!rows.isEmpty()) {
            Row last = rows.getLast();
            depleted = last.dimer();
            depletedValue = last.enrichment();
        }
        List<Row> top = rows.subList(0, Math.min(8, rows.size()));
        double sameG = globalTotal == 0 ? 0.0 : 100.0 * sameGlobal / globalTotal;
        double sameJ = jointTotal == 0 ? 0.0 : 100.0 * sameJoint / jointTotal;
        DimerContrast contrast = new DimerContrast(
                globalTotal, jointTotal, sameG, sameJ, enriched, enrichedValue, depleted, depletedValue, top);
        if (log.isDebugEnabled()) {
            log.debug("dimer-contrast.done global={} joints={} mostEnriched={}", globalTotal, jointTotal, enriched);
        }
        return contrast;
    }

    private static Map<String, Long> overlapping(String bases) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (int i = 0; i + 1 < bases.length(); i++) {
            char a = bases.charAt(i);
            char b = bases.charAt(i + 1);
            if (DnaSequence.CANONICAL_BASES.indexOf(a) < 0 || DnaSequence.CANONICAL_BASES.indexOf(b) < 0) {
                continue;
            }
            counts.merge("" + a + b, 1L, Long::sum);
        }
        return counts;
    }

    public long globalDimers() {
        return globalDimers;
    }

    public int jointCount() {
        return jointCount;
    }

    public double sameBaseGlobalShare() {
        return sameBaseGlobalShare;
    }

    public double sameBaseJointShare() {
        return sameBaseJointShare;
    }

    public String mostEnriched() {
        return mostEnriched;
    }

    public double mostEnrichedValue() {
        return mostEnrichedValue;
    }

    public String mostDepleted() {
        return mostDepleted;
    }

    public double mostDepletedValue() {
        return mostDepletedValue;
    }

    public List<Row> topEnriched() {
        return topEnriched;
    }

    public String toTextRow() {
        return String.format(Locale.ROOT,
                "global=%d joints=%d sameBase global=%.1f%% joint=%.1f%% enriched=%s x%.2f depleted=%s x%.2f",
                globalDimers, jointCount, sameBaseGlobalShare, sameBaseJointShare,
                mostEnriched.isEmpty() ? "-" : mostEnriched, mostEnrichedValue,
                mostDepleted.isEmpty() ? "-" : mostDepleted, mostDepletedValue);
    }
}
