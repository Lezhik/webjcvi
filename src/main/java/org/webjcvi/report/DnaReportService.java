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
        Map<String, String> sections = buildSections(
                sequence, generatedAt, javaSources, wraps, runs, skew, islands, joints, dimers, gcIslands, codons, lags, foldback, hairpins, mismatches, neighbors, wrapReverse);
        String markdown = renderMarkdown(
                sections, sequence, generatedAt, javaSources, wraps, runs, skew, islands, joints, dimers, gcIslands, codons, lags, foldback, hairpins, mismatches, neighbors, wrapReverse);
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
            WrapReverseCensus wrapReverse) {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("meta", "generatedAt=" + ISO.format(generatedAt));
        sections.put("length", Integer.toString(sequence.length()));
        sections.put("gcPercent", String.format(Locale.ROOT, "%.2f", sequence.gcPercent()));
        sections.put("ambiguousTotal", Long.toString(sequence.ambiguousTotal()));
        sections.put("invalidTotal", Long.toString(sequence.invalidTotal()));
        sections.put("javaSourceCount", Integer.toString(javaSources.size()));
        sections.put("wrapModalWidth", Integer.toString(wraps.modalWidth()));
        sections.put("wrapJoints", Integer.toString(wrapReverse.wrapJoints()));
        sections.put("wrapReverseShare", String.format(Locale.ROOT, "%.4f", wrapReverse.wrapReverseShare()));
        sections.put("interiorReverseShare", String.format(Locale.ROOT, "%.4f", wrapReverse.interiorReverseShare()));
        sections.put("wrapVsInterior", String.format(Locale.ROOT, "%.3f", wrapReverse.wrapVsInterior()));
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
            WrapReverseCensus wrapReverse) {
        StringBuilder md = new StringBuilder();
        md.append("# WebJCVI DNA Report\n\n");
        md.append("Generated at **").append(ISO.format(generatedAt.atOffset(ZoneOffset.UTC))).append("**.\n\n");
        md.append("This report is rebuilt from `").append(dnaRelativePath)
                .append("` and a snapshot of the current codebase. Version 13 of the builder ")
                .append("compares reverse Hamming of adjacent 4-mers at FASTA wrap joints ")
                .append("(last 4 of line N vs reverse of first 4 of line N+1) against the same ")
                .append("measurement on the linear interior tape. v12 reverse distance-0 was ")
                .append("enriched (×1.130); this asks whether that mirror lives on the wrap seam ")
                .append("or along the tape.\n\n");

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

        md.append("## Wrap reverse\n\n");
        md.append("Wrap joints sample the last 4 canonical bases of line N against the reverse ")
                .append("of the first 4 of line N+1. Interior windows are overlapping adjacent ")
                .append("4-mers on the linear tape (the v12 reverse map). If wrap/interior ≫ 1, ")
                .append("reverse enrichment is a wrap-seam protocol; if ≈ 1, it is linear.\n\n");
        md.append("| Metric | Value |\n| --- | --- |\n");
        md.append("| Wrap joints | ").append(wrapReverse.wrapJoints()).append(" |\n");
        md.append("| Wrap reverse-0 | ").append(wrapReverse.wrapReverseZero()).append(" |\n");
        md.append("| Wrap reverse-0 share | ")
                .append(String.format(Locale.ROOT, "%.4f", wrapReverse.wrapReverseShare())).append(" |\n");
        md.append("| Interior windows | ").append(wrapReverse.interiorWindows()).append(" |\n");
        md.append("| Interior reverse-0 | ").append(wrapReverse.interiorReverseZero()).append(" |\n");
        md.append("| Interior reverse-0 share | ")
                .append(String.format(Locale.ROOT, "%.4f", wrapReverse.interiorReverseShare())).append(" |\n");
        md.append("| Wrap / interior | ")
                .append(String.format(Locale.ROOT, "%.3f", wrapReverse.wrapVsInterior())).append(" |\n\n");
        md.append("`").append(wrapReverse.toTextRow()).append("`\n\n");

        md.append("## Frame remainder\n\n");
        md.append("Neighbor Hamming (linear tape): identity modal **")
                .append(neighbors.identity().modalDistance())
                .append("** zero× **")
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
        appendExtractedRules(md, sequence, wraps, runs, skew, islands, joints, dimers, gcIslands, codons, lags, foldback, hairpins, mismatches, neighbors, wrapReverse);

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
        md.append("- Wrap reverse: wrap reverse-0 share **")
                .append(String.format(Locale.ROOT, "%.4f", wrapReverse.wrapReverseShare()))
                .append("** vs interior **")
                .append(String.format(Locale.ROOT, "%.4f", wrapReverse.interiorReverseShare()))
                .append("** (wrap/interior **")
                .append(String.format(Locale.ROOT, "%.3f", wrapReverse.wrapVsInterior()))
                .append("**). If wrap/interior ≫ 1, reverse enrichment is a wrap-seam protocol; ")
                .append("if ≈ 1, the mirror lives on the linear tape and wrap is just a sample of it.\n");
        md.append("- Neighbor remainder: identity zero× **")
                .append(String.format(Locale.ROOT, "%.3f", neighbors.identity().zeroEnrichment()))
                .append("**; reverse × **")
                .append(String.format(Locale.ROOT, "%.3f", neighbors.reverse().zeroEnrichment()))
                .append("**; RC × **")
                .append(String.format(Locale.ROOT, "%.3f", neighbors.rc().zeroEnrichment()))
                .append("**.\n");
        md.append("- Homopolymer longest **").append(runs.longestBase()).append(" × ")
                .append(runs.longestLength()).append("**.\n");
        md.append("- Module split (TZ §6.14) is **not** indicated: single Gradle module, no sub-module reports.\n");

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
            WrapReverseCensus wrapReverse) {
        long a = sequence.canonicalCounts().getOrDefault('A', 0L);
        long t = sequence.canonicalCounts().getOrDefault('T', 0L);
        md.append("1. **R1 Wrap vs interior reverse.** Wrap reverse-0 share ")
                .append(String.format(Locale.ROOT, "%.4f", wrapReverse.wrapReverseShare()))
                .append(" vs interior ")
                .append(String.format(Locale.ROOT, "%.4f", wrapReverse.interiorReverseShare()))
                .append(" (wrap/interior ")
                .append(String.format(Locale.ROOT, "%.3f", wrapReverse.wrapVsInterior()))
                .append(").\n");
        md.append("2. **R2 Neighbor remainder.** Identity zero×")
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
