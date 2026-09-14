package org.webjcvi.report;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import org.webjcvi.dna.CodonFrameCensus;
import org.webjcvi.dna.DimerContrast;
import org.webjcvi.dna.DnaParser;
import org.webjcvi.dna.DnaSequence;
import org.webjcvi.dna.FoldbackCensus;
import org.webjcvi.dna.FrameSkewCensus;
import org.webjcvi.dna.GcIslandCensus;
import org.webjcvi.dna.HairpinCensus;
import org.webjcvi.dna.HomopolymerProfile;
import org.webjcvi.dna.MismatchCensus;
import org.webjcvi.dna.NeighborCensus;
import org.webjcvi.dna.SameBaseLagCensus;
import org.webjcvi.dna.SkewIslandCensus;
import org.webjcvi.dna.WrapCensus;
import org.webjcvi.dna.WrapJointCensus;
import org.webjcvi.dna.WrapReverseCensus;
import org.webjcvi.dna.ReversePhaseCensus;
import org.webjcvi.dna.MapPhaseCensus;
import org.webjcvi.dna.SlotCensus;
import org.webjcvi.dna.CloneCensus;
import org.webjcvi.dna.PrefixCensus;
import org.webjcvi.dna.KeyWidthCensus;
import org.webjcvi.dna.SuffixCensus;
import org.webjcvi.dna.InteriorCensus;
import org.webjcvi.dna.LandscapeCensus;
import org.webjcvi.dna.LineKeyCensus;
import org.webjcvi.dna.LineForkCensus;
import org.webjcvi.dna.LineRunwayCensus;
import org.webjcvi.dna.LineRiseCensus;
import org.webjcvi.dna.LineMajorityCensus;
import org.webjcvi.storage.FileStorageService;
import org.webjcvi.storage.StorageNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fixed-API DNA reader and report builder. Reads {@code jcvi-dna.txt} and
 * writes reports exclusively through {@link FileStorageService}. The report
 * <em>content</em> may evolve; this class's method signatures should stay
 * stable so web and MCP surfaces can both depend on it.
 */
public final class DnaReportService {

    public static final String DEFAULT_DNA_PATH = FileStorageService.DNA_FILE_NAME;
    public static final String DEFAULT_REPORT_DIRECTORY = "build/reports/dna";
    public static final String DEFAULT_REPORT_FILE_NAME = "dna-report.md";

    private static final Logger log = LoggerFactory.getLogger(DnaReportService.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;
    private static final int PREVIEW_BASES = 80;

    private final FileStorageService storage;
    private final DnaParser parser;
    private final Clock clock;
    private final String dnaRelativePath;
    private final String reportDirectory;
    private final String reportFileName;

    public DnaReportService(FileStorageService storage) {
        this(storage, new DnaParser(), Clock.systemUTC(),
                DEFAULT_DNA_PATH, DEFAULT_REPORT_DIRECTORY, DEFAULT_REPORT_FILE_NAME);
    }

    public DnaReportService(
            FileStorageService storage,
            DnaParser parser,
            Clock clock,
            String dnaRelativePath,
            String reportDirectory,
            String reportFileName) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.dnaRelativePath = Objects.requireNonNull(dnaRelativePath, "dnaRelativePath");
        this.reportDirectory = Objects.requireNonNull(reportDirectory, "reportDirectory");
        this.reportFileName = Objects.requireNonNull(reportFileName, "reportFileName");
    }

    public String reportRelativePath() {
        return reportDirectory + "/" + reportFileName;
    }

    public String reportDirectory() {
        return reportDirectory;
    }

    /**
     * Regenerates the report from scratch: clears prior reports for this
     * scope, re-reads the DNA file, and writes a fresh markdown report.
     */
    public DnaReport regenerate() {
        if (log.isDebugEnabled()) {
            log.debug("dna-report.regenerate.start path={}", dnaRelativePath);
        }
        storage.deleteContents(reportDirectory);
        String raw;
        try {
            raw = storage.readText(dnaRelativePath);
        } catch (StorageNotFoundException e) {
            raw = "";
        }
        DnaSequence sequence = parser.parse(raw);
        Instant generatedAt = clock.instant();
        List<String> javaSources = listJavaSources();
        WrapCensus wraps = WrapCensus.fromRaw(raw);
        HomopolymerProfile runs = HomopolymerProfile.from(sequence);
        FrameSkewCensus skew = FrameSkewCensus.from(sequence, wraps.modalWidth());
        SkewIslandCensus islands = SkewIslandCensus.from(sequence, wraps.modalWidth());
        WrapJointCensus joints = WrapJointCensus.fromRaw(raw);
        DimerContrast dimers = DimerContrast.from(sequence, joints);
        GcIslandCensus gcIslands = GcIslandCensus.from(sequence);
        CodonFrameCensus codons = CodonFrameCensus.from(sequence);
        SameBaseLagCensus lags = SameBaseLagCensus.from(sequence);
        FoldbackCensus foldback = FoldbackCensus.from(sequence);
        HairpinCensus hairpins = HairpinCensus.from(sequence);
        MismatchCensus mismatches = MismatchCensus.from(sequence);
        NeighborCensus neighbors = NeighborCensus.from(sequence);
        WrapReverseCensus wrapReverse = WrapReverseCensus.from(raw, neighbors);
        ReversePhaseCensus phases = ReversePhaseCensus.from(sequence, wraps.modalWidth());
        MapPhaseCensus mapPhases = MapPhaseCensus.from(sequence, wraps.modalWidth());
        SlotCensus slots = SlotCensus.from(sequence, wraps.modalWidth());
        CloneCensus cloneCensus = CloneCensus.from(sequence, wraps.modalWidth());
        PrefixCensus prefixCensus = PrefixCensus.from(sequence, wraps.modalWidth());
        KeyWidthCensus keyWidth = KeyWidthCensus.from(sequence, wraps.modalWidth());
        SuffixCensus suffixCensus = SuffixCensus.from(sequence, wraps.modalWidth());
        InteriorCensus interior = InteriorCensus.from(sequence, wraps.modalWidth());
        LandscapeCensus landscape = LandscapeCensus.from(sequence, wraps.modalWidth());
        LineKeyCensus lineKeys = LineKeyCensus.fromRaw(raw);
        LineForkCensus lineForks = LineForkCensus.fromRaw(raw);
        LineRunwayCensus lineRunways = LineRunwayCensus.fromRaw(raw);
        LineRiseCensus lineRises = LineRiseCensus.fromRaw(raw);
        LineMajorityCensus lineMajorities = LineMajorityCensus.fromRaw(raw);
        Map<String, String> sections = buildSections(
                sequence, generatedAt, javaSources, wraps, runs, skew, islands, joints, dimers, gcIslands, codons, lags, foldback, hairpins, mismatches, neighbors, wrapReverse, phases, mapPhases, slots, cloneCensus, prefixCensus, keyWidth, suffixCensus, interior, landscape, lineKeys, lineForks, lineRunways, lineRises, lineMajorities);
        String markdown = renderMarkdown(
                sections, sequence, generatedAt, javaSources, wraps, runs, skew, islands, joints, dimers, gcIslands, codons, lags, foldback, hairpins, mismatches, neighbors, wrapReverse, phases, mapPhases, slots, cloneCensus, prefixCensus, keyWidth, suffixCensus, interior, landscape, lineKeys, lineForks, lineRunways, lineRises, lineMajorities);
        storage.writeText(reportRelativePath(), markdown);
        if (log.isDebugEnabled()) {
            log.debug("dna-report.regenerate.done bases={} markdownChars={} path={}",
                    sequence.length(), markdown.length(), reportRelativePath());
        }
        return new DnaReport(generatedAt, sequence, sections, markdown);
    }

    /**
     * Loads the on-disk report if one exists, without regenerating. The
     * structured {@link DnaSequence} is re-parsed from the DNA file so callers
     * still get a typed object even though section content lives in markdown.
     */
    public Optional<DnaReport> loadCurrent() {
        if (!storage.exists(reportRelativePath())) {
            return Optional.empty();
        }
        String markdown = storage.readText(reportRelativePath());
        DnaSequence sequence = readSequence();
        Instant generatedAt = clock.instant();
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("stored-markdown", markdown);
        return Optional.of(new DnaReport(generatedAt, sequence, sections, markdown));
    }

    public DnaSequence readSequence() {
        String raw;
        try {
            raw = storage.readText(dnaRelativePath);
        } catch (StorageNotFoundException e) {
            raw = "";
        }
        return parser.parse(raw);
    }

    private List<String> listJavaSources() {
        if (!storage.exists("src/main/java")) {
            return List.of();
        }
        return storage.listRecursive("src/main/java");
    }

    private Map<String, String> buildSections(
            DnaSequence sequence,
            Instant generatedAt,
            List<String> javaSources,
            WrapCensus wraps,
            HomopolymerProfile runs,
            FrameSkewCensus skew,
            SkewIslandCensus islands,
            WrapJointCensus joints,
            DimerContrast dimers,
            GcIslandCensus gcIslands,
            CodonFrameCensus codons,
            SameBaseLagCensus lags,
            FoldbackCensus foldback,
            HairpinCensus hairpins,
            MismatchCensus mismatches,
            NeighborCensus neighbors,
            WrapReverseCensus wrapReverse,
            ReversePhaseCensus phases,
            MapPhaseCensus mapPhases,
            SlotCensus slots,
            CloneCensus cloneCensus,
            PrefixCensus prefixCensus,
            KeyWidthCensus keyWidth,
            SuffixCensus suffixCensus,
            InteriorCensus interior,
            LandscapeCensus landscape,
            LineKeyCensus lineKeys,
            LineForkCensus lineForks,
            LineRunwayCensus lineRunways,
            LineRiseCensus lineRises,
            LineMajorityCensus lineMajorities) {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("meta", "generatedAt=" + ISO.format(generatedAt));
        sections.put("length", Integer.toString(sequence.length()));
        sections.put("gcPercent", String.format(Locale.ROOT, "%.2f", sequence.gcPercent()));
        sections.put("ambiguousTotal", Long.toString(sequence.ambiguousTotal()));
        sections.put("invalidTotal", Long.toString(sequence.invalidTotal()));
        sections.put("javaSourceCount", Integer.toString(javaSources.size()));
        sections.put("wrapModalWidth", Integer.toString(wraps.modalWidth()));
        sections.put("lineMajorityAt", Integer.toString(lineMajorities.majorityAt()));
        sections.put("lineShareAtMajority", String.format(Locale.ROOT, "%.4f", lineMajorities.shareAtMajority()));
        sections.put("lineRiseAt", Integer.toString(lineRises.riseAt()));
        sections.put("lineCliffAt", Integer.toString(lineRises.cliffAt()));
        sections.put("lineOverhang", Integer.toString(lineRunways.overhang()));
        sections.put("lineUniqueAt", Integer.toString(lineKeys.uniqueAt()));
        sections.put("longestHomopolymer", runs.longestBase() + "x" + runs.longestLength());
        return sections;
    }

    private String renderMarkdown(
            Map<String, String> sections,
            DnaSequence sequence,
            Instant generatedAt,
            List<String> javaSources,
            WrapCensus wraps,
            HomopolymerProfile runs,
            FrameSkewCensus skew,
            SkewIslandCensus islands,
            WrapJointCensus joints,
            DimerContrast dimers,
            GcIslandCensus gcIslands,
            CodonFrameCensus codons,
            SameBaseLagCensus lags,
            FoldbackCensus foldback,
            HairpinCensus hairpins,
            MismatchCensus mismatches,
            NeighborCensus neighbors,
            WrapReverseCensus wrapReverse,
            ReversePhaseCensus phases,
            MapPhaseCensus mapPhases,
            SlotCensus slots,
            CloneCensus cloneCensus,
            PrefixCensus prefixCensus,
            KeyWidthCensus keyWidth,
            SuffixCensus suffixCensus,
            InteriorCensus interior,
            LandscapeCensus landscape,
            LineKeyCensus lineKeys,
            LineForkCensus lineForks,
            LineRunwayCensus lineRunways,
            LineRiseCensus lineRises,
            LineMajorityCensus lineMajorities) {
        StringBuilder md = new StringBuilder();
        md.append("# WebJCVI DNA Report\n\n");
        md.append("Generated at **").append(ISO.format(generatedAt.atOffset(ZoneOffset.UTC))).append("**.\n\n");
        md.append("This report is rebuilt from `").append(dnaRelativePath)
                .append("` and a snapshot of the current codebase. Version 27 of the builder ")
                .append("walks the smallest k whose unique share is at least 0.5 on source lines. ")
                .append("v26 uniqueness jumps at the floor (riseAt=8, gain 0.7191); ")
                .append("log journals then had riseAt=23 with share only ~0.28. ")
                .append("This asks where FASTA unique share covers a majority, not where it accelerates fastest.\n\n");

        md.append("## Sequence composition\n\n");
        md.append("| Metric | Value |\n| --- | --- |\n");
        md.append("| Length (normalized bases) | ").append(sequence.length()).append(" |\n");
        md.append("| Source lines | ").append(sequence.sourceLineCount()).append(" |\n");
        md.append("| GC% (canonical ATGC only) | ")
                .append(String.format(Locale.ROOT, "%.2f", sequence.gcPercent())).append(" |\n");
        md.append("| Ambiguous IUPAC characters | ").append(sequence.ambiguousTotal()).append(" |\n");
        md.append("| Invalid / non-nucleotide characters | ").append(sequence.invalidTotal()).append(" |\n\n");

        md.append("### Canonical bases\n\n");
        md.append("| Base | Count | Share of ATGC |\n| --- | --- | --- |\n");
        long canonicalTotal = sequence.canonicalCounts().values().stream().mapToLong(Long::longValue).sum();
        for (var entry : sequence.canonicalCounts().entrySet()) {
            double share = canonicalTotal == 0 ? 0.0 : entry.getValue() * 100.0 / canonicalTotal;
            md.append("| ").append(entry.getKey()).append(" | ")
                    .append(entry.getValue()).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f%%", share)).append(" |\n");
        }

        md.append("\n### Ambiguous and invalid characters\n\n");
        md.append("Ambiguous IUPAC codes (`N`, `R`, `Y`, `S`, `W`, `K`, `M`, `B`, `D`, `H`, `V`) ");
        md.append("are kept in the normalized sequence and counted separately. ");
        md.append("Any other non-whitespace character is dropped from the normalized sequence ");
        md.append("and recorded as invalid so parsing never crashes.\n\n");
        if (sequence.ambiguousCounts().isEmpty() && sequence.invalidCounts().isEmpty()) {
            md.append("No ambiguous or invalid characters were present.\n\n");
        } else {
            md.append("| Character | Class | Count |\n| --- | --- | --- |\n");
            sequence.ambiguousCounts().forEach((ch, count) ->
                    md.append("| `").append(ch).append("` | ambiguous | ").append(count).append(" |\n"));
            sequence.invalidCounts().forEach((ch, count) ->
                    md.append("| `").append(escapeCell(ch)).append("` | invalid | ").append(count).append(" |\n"));
            md.append('\n');
        }

        md.append("### Preview\n\n");
        md.append("First ").append(PREVIEW_BASES).append(" normalized bases:\n\n```\n");
        md.append(sequence.preview(PREVIEW_BASES)).append("\n```\n\n");

        md.append("## Line majority\n\n");
        md.append("Source lines kept. **majorityAt** is the smallest k≥8 whose unique share is ≥ 0.5. ")
                .append("If majorityAt equals riseAt, the steepest jump already covers a majority ")
                .append("(DNA-tight). If majorityAt sits past riseAt, residual collisions remain after the jump.\n\n");
        md.append("| Metric | Value |\n| --- | --- |\n");
        md.append("| Lines | ").append(lineMajorities.lines()).append(" |\n");
        md.append("| Majority at (k) | ").append(lineMajorities.majorityAt()).append(" |\n");
        md.append("| Share at majority | ").append(String.format(Locale.ROOT, "%.4f", lineMajorities.shareAtMajority())).append(" |\n");
        md.append("| Rise at (k) | ").append(lineMajorities.riseAt()).append(" |\n");
        md.append("| Share at rise | ").append(String.format(Locale.ROOT, "%.4f", lineMajorities.shareAtRise())).append(" |\n");
        for (LineMajorityCensus.Row row : lineMajorities.samples()) {
            md.append("| k=").append(row.length())
                    .append(" share | ").append(String.format(Locale.ROOT, "%.4f", row.uniqueShare())).append(" |\n");
        }
        md.append('\n');
        md.append("`").append(lineMajorities.toTextRow()).append("`\n\n");

        md.append("## Frame remainder\n\n");
        md.append("Line-rise remainder: riseAt **").append(lineRises.riseAt())
                .append("**, gain **")
                .append(String.format(Locale.ROOT, "%.4f", lineRises.gain()))
                .append("**, share at rise **")
                .append(String.format(Locale.ROOT, "%.4f", lineRises.shareAtRise()))
                .append("**, cliffAt **").append(lineRises.cliffAt())
                .append("**. Line-runway remainder: cliffAt **").append(lineRunways.cliffAt())
                .append("**, modalFork **").append(lineRunways.modalFork())
                .append("**, runway **").append(lineRunways.runway())
                .append("**, overhang **").append(lineRunways.overhang())
                .append("**. Line-fork remainder: twin groups **").append(lineForks.twinGroups())
                .append("**, twin lines **").append(lineForks.twinLines())
                .append("**, minFork **").append(lineForks.minFork())
                .append("**, modalFork **").append(lineForks.modalFork())
                .append("**, maxFork **").append(lineForks.maxFork())
                .append("**. Line-key remainder: uniqueAt **").append(lineKeys.uniqueAt())
                .append("**, unique share k=8 **")
                .append(String.format(Locale.ROOT, "%.4f", lineKeys.shareAt(8)))
                .append("**, k=16 **")
                .append(String.format(Locale.ROOT, "%.4f", lineKeys.shareAt(16)))
                .append("**, k=20 **")
                .append(String.format(Locale.ROOT, "%.4f", lineKeys.shareAt(20)))
                .append("**. Landscape remainder: troughAt **").append(landscape.troughAt())
                .append("** peakAt **").append(landscape.peakAt())
                .append("** spread **")
                .append(String.format(Locale.ROOT, "%.4f", landscape.spread()))
                .append("**. Interior-key remainder: uniqueAt **").append(interior.uniqueAt())
                .append("**, k=16 start column **").append(interior.midStartAt16())
                .append("**, unique share k=8 **")
                .append(String.format(Locale.ROOT, "%.4f", interior.shareAt(8)))
                .append("**, k=16 **")
                .append(String.format(Locale.ROOT, "%.4f", interior.shareAt(16)))
                .append("**, k=20 **")
                .append(String.format(Locale.ROOT, "%.4f", interior.shareAt(20)))
                .append("**. Trailing-key remainder: uniqueAt **").append(suffixCensus.uniqueAt())
                .append("**, unique share k=8 **")
                .append(String.format(Locale.ROOT, "%.4f", suffixCensus.shareAt(8)))
                .append("**, k=16 **")
                .append(String.format(Locale.ROOT, "%.4f", suffixCensus.shareAt(16)))
                .append("**, k=20 **")
                .append(String.format(Locale.ROOT, "%.4f", suffixCensus.shareAt(20)))
                .append("**. Leading-key remainder: uniqueAt **").append(keyWidth.uniqueAt())
                .append("**, unique share k=8 **")
                .append(String.format(Locale.ROOT, "%.4f", keyWidth.shareAt(8)))
                .append("**, k=16 **")
                .append(String.format(Locale.ROOT, "%.4f", keyWidth.shareAt(16)))
                .append("**, k=70 **")
                .append(String.format(Locale.ROOT, "%.4f", keyWidth.shareAt(70)))
                .append("**. Prefix remainder: **").append(prefixCensus.distinct())
                .append("** distinct of **").append(prefixCensus.frames())
                .append("** (unique share **")
                .append(String.format(Locale.ROOT, "%.4f", prefixCensus.uniqueShare()))
                .append("**), families **").append(prefixCensus.familyCount())
                .append("**, top `").append(prefixCensus.topPrefix())
                .append("` × **").append(prefixCensus.topCount())
                .append("**. Clone remainder: **").append(cloneCensus.distinct())
                .append("** distinct of **").append(cloneCensus.frames())
                .append("** (unique share **")
                .append(String.format(Locale.ROOT, "%.4f", cloneCensus.uniqueShare()))
                .append("**), clone groups **").append(cloneCensus.cloneGroups())
                .append("**. Slot remainder: RC GC **")
                .append(String.format(Locale.ROOT, "%.2f", slots.rc().gcPercent()))
                .append("%** top `").append(slots.rc().topKmer())
                .append("`; reverse GC **")
                .append(String.format(Locale.ROOT, "%.2f", slots.reverse().gcPercent()))
                .append("%** top `").append(slots.reverse().topKmer())
                .append("`; identity GC **")
                .append(String.format(Locale.ROOT, "%.2f", slots.identity().gcPercent()))
                .append("%** top `").append(slots.identity().topKmer())
                .append("`. Map-phase remainder: identity peaks at **").append(mapPhases.identity().peakPhase())
                .append("** (share **")
                .append(String.format(Locale.ROOT, "%.4f", mapPhases.identity().peakShare()))
                .append("**); reverse at **").append(mapPhases.reverse().peakPhase())
                .append("** (share **")
                .append(String.format(Locale.ROOT, "%.4f", mapPhases.reverse().peakShare()))
                .append("**); RC at **").append(mapPhases.rc().peakPhase())
                .append("** (share **")
                .append(String.format(Locale.ROOT, "%.4f", mapPhases.rc().peakShare()))
                .append("**). Reverse-only phase: peak **").append(phases.peakPhase())
                .append("** share **")
                .append(String.format(Locale.ROOT, "%.4f", phases.peakShare()))
                .append("**; wrap phase **").append(phases.wrapPhase())
                .append("** share **")
                .append(String.format(Locale.ROOT, "%.4f", phases.wrapPhaseShare()))
                .append("** (wrap/mean **")
                .append(String.format(Locale.ROOT, "%.3f", phases.wrapVsMean()))
                .append("**). Wrap vs interior reverse: wrap/interior **")
                .append(String.format(Locale.ROOT, "%.3f", wrapReverse.wrapVsInterior()))
                .append("**. Neighbor Hamming: identity zero× **")
                .append(String.format(Locale.ROOT, "%.3f", neighbors.identity().zeroEnrichment()))
                .append("**; reverse × **")
                .append(String.format(Locale.ROOT, "%.3f", neighbors.reverse().zeroEnrichment()))
                .append("**; RC × **")
                .append(String.format(Locale.ROOT, "%.3f", neighbors.rc().zeroEnrichment()))
                .append("**. Pairing mismatches (RC-only): modal **").append(mismatches.modalDistance())
                .append("**, distance-0 × **")
                .append(String.format(Locale.ROOT, "%.3f", mismatches.zeroEnrichment()))
                .append("**. Hairpins: modal loop **").append(hairpins.modalLoop())
                .append("** (").append(hairpins.modalHairpins())
                .append("); adjacent **").append(hairpins.adjacent())
                .append("**, gapped **").append(hairpins.gapped())
                .append("**. Foldback RC **").append(foldback.rcStems())
                .append("**, longest **").append(foldback.longestRc())
                .append("**. Same-base lags: peak **").append(lags.peakLag())
                .append("** × ")
                .append(String.format(Locale.ROOT, "%.3f", lags.peakEnrichment()))
                .append(". Codon GC spread **")
                .append(String.format(Locale.ROOT, "%.2f", codons.maxGcSpread()))
                .append("**. Wrap modal **").append(wraps.modalWidth())
                .append("**. Homopolymer longest **")
                .append(runs.longestBase()).append(" × ").append(runs.longestLength())
                .append("**. Mean |AT-skew| **")
                .append(String.format(Locale.ROOT, "%.4f", skew.meanAbsAtSkew()))
                .append("**.\n\n");

        md.append("## Extracted tape rules\n\n");
        appendExtractedRules(md, sequence, wraps, runs, skew, islands, joints, dimers, gcIslands, codons, lags, foldback, hairpins, mismatches, neighbors, wrapReverse, phases, mapPhases, slots, cloneCensus, prefixCensus, keyWidth, suffixCensus, interior, landscape, lineKeys, lineForks, lineRunways, lineRises, lineMajorities);

        md.append("## Codebase snapshot\n\n");
        md.append(javaSources.size()).append(" Java source files under `src/main/java`:\n\n");
        if (javaSources.isEmpty()) {
            md.append("_No Java sources found — this is unexpected after preparation._\n\n");
        } else {
            for (String path : javaSources) {
                md.append("- `").append(path).append("`\n");
            }
            md.append('\n');
        }

        md.append("## Growth notes for the next iteration\n\n");
        md.append("- Dominant canonical base: **").append(dominantBase(sequence)).append("**.\n");
        md.append("- Line majority: majorityAt **").append(lineMajorities.majorityAt())
                .append("**, share **")
                .append(String.format(Locale.ROOT, "%.4f", lineMajorities.shareAtMajority()))
                .append("**. Line-rise remainder: riseAt **").append(lineRises.riseAt())
                .append("**, gain **")
                .append(String.format(Locale.ROOT, "%.4f", lineRises.gain()))
                .append("**, cliffAt **").append(lineRises.cliffAt())
                .append("**. If majorityAt equals riseAt, uniqueness is DNA-tight; if it sits past riseAt, keep walking after the steepest jump.\n");
        md.append("- Reverse-only remainder: wrap/mean **")
                .append(String.format(Locale.ROOT, "%.3f", phases.wrapVsMean()))
                .append("**.\n");
        md.append("- Homopolymer longest **").append(runs.longestBase()).append(" × ")
                .append(runs.longestLength()).append("**.\n");
        md.append("- Module split (TZ §6.15) is **not** indicated: single Gradle module, no sub-module reports.\n");

        md.append("\n## Machine-readable sections\n\n");
        md.append("```\n");
        sections.forEach((key, value) -> md.append(key).append('=').append(value).append('\n'));
        md.append("```\n");
        return md.toString();
    }

    private static void appendExtractedRules(
            StringBuilder md,
            DnaSequence sequence,
            WrapCensus wraps,
            HomopolymerProfile runs,
            FrameSkewCensus skew,
            SkewIslandCensus islands,
            WrapJointCensus joints,
            DimerContrast dimers,
            GcIslandCensus gcIslands,
            CodonFrameCensus codons,
            SameBaseLagCensus lags,
            FoldbackCensus foldback,
            HairpinCensus hairpins,
            MismatchCensus mismatches,
            NeighborCensus neighbors,
            WrapReverseCensus wrapReverse,
            ReversePhaseCensus phases,
            MapPhaseCensus mapPhases,
            SlotCensus slots,
            CloneCensus cloneCensus,
            PrefixCensus prefixCensus,
            KeyWidthCensus keyWidth,
            SuffixCensus suffixCensus,
            InteriorCensus interior,
            LandscapeCensus landscape,
            LineKeyCensus lineKeys,
            LineForkCensus lineForks,
            LineRunwayCensus lineRunways,
            LineRiseCensus lineRises,
            LineMajorityCensus lineMajorities) {
        long a = sequence.canonicalCounts().getOrDefault('A', 0L);
        long t = sequence.canonicalCounts().getOrDefault('T', 0L);
        md.append("1. **R1 Line majority.** Lines ").append(lineMajorities.lines())
                .append(" majorityAt ").append(lineMajorities.majorityAt())
                .append(" shareMaj ")
                .append(String.format(Locale.ROOT, "%.4f", lineMajorities.shareAtMajority()))
                .append(" riseAt ").append(lineMajorities.riseAt())
                .append(" shareRise ")
                .append(String.format(Locale.ROOT, "%.4f", lineMajorities.shareAtRise()))
                .append(".\n");
        md.append("2. **R2 Line-rise/runway remainder.** riseAt ").append(lineRises.riseAt())
                .append(" gain ")
                .append(String.format(Locale.ROOT, "%.4f", lineRises.gain()))
                .append(" cliffAt ").append(lineRises.cliffAt())
                .append("; overhang ").append(lineRunways.overhang())
                .append(" modalFork ").append(lineRunways.modalFork())
                .append(" runway ").append(lineRunways.runway())
                .append("; twinGroups ").append(lineForks.twinGroups())
                .append(" minFork ").append(lineForks.minFork())
                .append(" maxFork ").append(lineForks.maxFork())
                .append("; uniqueAt ").append(lineKeys.uniqueAt())
                .append(" share k=8 ")
                .append(String.format(Locale.ROOT, "%.4f", lineKeys.shareAt(8)))
                .append(" k=16 ")
                .append(String.format(Locale.ROOT, "%.4f", lineKeys.shareAt(16)))
                .append(" k=20 ")
                .append(String.format(Locale.ROOT, "%.4f", lineKeys.shareAt(20)))
                .append("; spread ")
                .append(String.format(Locale.ROOT, "%.4f", landscape.spread()))
                .append(" troughAt ").append(landscape.troughAt())
                .append(" peakAt ").append(landscape.peakAt())
                .append("; mid uniqueAt ").append(interior.uniqueAt())
                .append(" midStart16 ").append(interior.midStartAt16())
                .append(" share k=8 ")
                .append(String.format(Locale.ROOT, "%.4f", interior.shareAt(8)))
                .append(" k=16 ")
                .append(String.format(Locale.ROOT, "%.4f", interior.shareAt(16)))
                .append("; tail uniqueAt ").append(suffixCensus.uniqueAt())
                .append(" share k=8 ")
                .append(String.format(Locale.ROOT, "%.4f", suffixCensus.shareAt(8)))
                .append(" k=16 ")
                .append(String.format(Locale.ROOT, "%.4f", suffixCensus.shareAt(16)))
                .append("; lead uniqueAt ").append(keyWidth.uniqueAt())
                .append(" wrap ").append(keyWidth.wrapWidth())
                .append("; unique share k=8 ")
                .append(String.format(Locale.ROOT, "%.4f", keyWidth.shareAt(8)))
                .append(" k=16 ")
                .append(String.format(Locale.ROOT, "%.4f", keyWidth.shareAt(16)))
                .append(" k=24 ")
                .append(String.format(Locale.ROOT, "%.4f", keyWidth.shareAt(24)))
                .append(" k=32 ")
                .append(String.format(Locale.ROOT, "%.4f", keyWidth.shareAt(32)))
                .append(" k=70 ")
                .append(String.format(Locale.ROOT, "%.4f", keyWidth.shareAt(70)))
                .append(". Prefix remainder: frames ").append(prefixCensus.frames())
                .append(" distinct prefixes ").append(prefixCensus.distinct())
                .append(" unique share ")
                .append(String.format(Locale.ROOT, "%.4f", prefixCensus.uniqueShare()))
                .append("; families ").append(prefixCensus.familyCount())
                .append(" family frames ").append(prefixCensus.familyFrames())
                .append(" top ").append(prefixCensus.topPrefix())
                .append("×").append(prefixCensus.topCount())
                .append(". Clone remainder: frames ").append(cloneCensus.frames())
                .append(" distinct ").append(cloneCensus.distinct())
                .append(" unique share ")
                .append(String.format(Locale.ROOT, "%.4f", cloneCensus.uniqueShare()))
                .append("; clone groups ").append(cloneCensus.cloneGroups())
                .append(" clone frames ").append(cloneCensus.cloneFrames())
                .append(" top count ").append(cloneCensus.topCount())
                .append(". RC col ").append(SlotCensus.RC_COLUMN)
                .append(" GC ")
                .append(String.format(Locale.ROOT, "%.2f", slots.rc().gcPercent()))
                .append("% top ").append(slots.rc().topKmer())
                .append("×").append(slots.rc().topCount())
                .append("; reverse col ").append(SlotCensus.REVERSE_COLUMN)
                .append(" GC ")
                .append(String.format(Locale.ROOT, "%.2f", slots.reverse().gcPercent()))
                .append("% top ").append(slots.reverse().topKmer())
                .append("×").append(slots.reverse().topCount())
                .append("; identity col ").append(SlotCensus.IDENTITY_COLUMN)
                .append(" GC ")
                .append(String.format(Locale.ROOT, "%.2f", slots.identity().gcPercent()))
                .append("% top ").append(slots.identity().topKmer())
                .append("×").append(slots.identity().topCount())
                .append(". Identity peaks at ").append(mapPhases.identity().peakPhase())
                .append(" share ")
                .append(String.format(Locale.ROOT, "%.4f", mapPhases.identity().peakShare()))
                .append("; reverse at ").append(mapPhases.reverse().peakPhase())
                .append(" share ")
                .append(String.format(Locale.ROOT, "%.4f", mapPhases.reverse().peakShare()))
                .append("; RC at ").append(mapPhases.rc().peakPhase())
                .append(" share ")
                .append(String.format(Locale.ROOT, "%.4f", mapPhases.rc().peakShare()))
                .append(". Reverse-only peak phase ").append(phases.peakPhase())
                .append(" share ")
                .append(String.format(Locale.ROOT, "%.4f", phases.peakShare()))
                .append("; wrap/mean ")
                .append(String.format(Locale.ROOT, "%.3f", phases.wrapVsMean()))
                .append(". Wrap/interior ")
                .append(String.format(Locale.ROOT, "%.3f", wrapReverse.wrapVsInterior()))
                .append(". Identity zero×")
                .append(String.format(Locale.ROOT, "%.3f", neighbors.identity().zeroEnrichment()))
                .append("; reverse ×")
                .append(String.format(Locale.ROOT, "%.3f", neighbors.reverse().zeroEnrichment()))
                .append("; RC ×")
                .append(String.format(Locale.ROOT, "%.3f", neighbors.rc().zeroEnrichment()))
                .append(". Pairing-mismatch modal ")
                .append(mismatches.modalDistance())
                .append("; distance-0 ×")
                .append(String.format(Locale.ROOT, "%.3f", mismatches.zeroEnrichment()))
                .append(". Global A=").append(a).append(", T=").append(t).append(".\n");
        md.append("3. **R3 Remainder foldback.** RC stems ").append(foldback.rcStems())
                .append(", longest ").append(foldback.longestRc()).append(".\n");
        md.append("4. **R4 Remainder.** Wrap modal ").append(wraps.modalWidth())
                .append("; GC spread ")
                .append(String.format(Locale.ROOT, "%.2f", codons.maxGcSpread()))
                .append("; same-base dimers ")
                .append(String.format(Locale.ROOT, "%.1f%%", dimers.sameBaseGlobalShare()))
                .append("/")
                .append(String.format(Locale.ROOT, "%.1f%%", dimers.sameBaseJointShare()))
                .append("; complementary joints ")
                .append(String.format(Locale.ROOT, "%.1f%%", joints.complementaryShare()))
                .append("; skew fail/pass ")
                .append(islands.failIslands()).append("/").append(islands.passIslands())
                .append("; GC island max ").append(gcIslands.longestGc())
                .append("; homopolymer ")
                .append(runs.longestBase()).append("×").append(runs.longestLength())
                .append("; mean |AT-skew| ")
                .append(String.format(Locale.ROOT, "%.4f", skew.meanAbsAtSkew()))
                .append("; peak lag ").append(lags.peakLag())
                .append(" × ")
                .append(String.format(Locale.ROOT, "%.3f", lags.peakEnrichment()))
                .append(".\n\n");
    }

    private static String dominantBase(DnaSequence sequence) {
        StringJoiner ties = new StringJoiner("/");
        long best = -1;
        for (var entry : sequence.canonicalCounts().entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                ties = new StringJoiner("/");
                ties.add(String.valueOf(entry.getKey()));
            } else if (entry.getValue() == best) {
                ties.add(String.valueOf(entry.getKey()));
            }
        }
        return best <= 0 ? "none" : ties.toString();
    }

    private static String escapeCell(char ch) {
        if (ch == '|') {
            return "\\|";
        }
        if (Character.isISOControl(ch)) {
            return String.format(Locale.ROOT, "U+%04X", (int) ch);
        }
        return String.valueOf(ch);
    }
}
