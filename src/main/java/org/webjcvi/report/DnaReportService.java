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
import org.webjcvi.dna.SameBaseLagCensus;
import org.webjcvi.dna.SkewIslandCensus;
import org.webjcvi.dna.WrapCensus;
import org.webjcvi.dna.WrapJointCensus;
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
        Map<String, String> sections = buildSections(
                sequence, generatedAt, javaSources, wraps, runs, skew, islands, joints, dimers, gcIslands, codons, lags, foldback, hairpins, mismatches);
        String markdown = renderMarkdown(
                sections, sequence, generatedAt, javaSources, wraps, runs, skew, islands, joints, dimers, gcIslands, codons, lags, foldback, hairpins, mismatches);
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
            MismatchCensus mismatches) {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("meta", "generatedAt=" + ISO.format(generatedAt));
        sections.put("length", Integer.toString(sequence.length()));
        sections.put("gcPercent", String.format(Locale.ROOT, "%.2f", sequence.gcPercent()));
        sections.put("ambiguousTotal", Long.toString(sequence.ambiguousTotal()));
        sections.put("invalidTotal", Long.toString(sequence.invalidTotal()));
        sections.put("javaSourceCount", Integer.toString(javaSources.size()));
        sections.put("wrapModalWidth", Integer.toString(wraps.modalWidth()));
        sections.put("modalDistance", Integer.toString(mismatches.modalDistance()));
        sections.put("zeroShare", String.format(Locale.ROOT, "%.4f", mismatches.zeroShare()));
        sections.put("zeroEnrichment", String.format(Locale.ROOT, "%.3f", mismatches.zeroEnrichment()));
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
            MismatchCensus mismatches) {
        StringBuilder md = new StringBuilder();
        md.append("# WebJCVI DNA Report\n\n");
        md.append("Generated at **").append(ISO.format(generatedAt.atOffset(ZoneOffset.UTC))).append("**.\n\n");
        md.append("This report is rebuilt from `").append(dnaRelativePath)
                .append("` and a snapshot of the current codebase. Version 11 of the builder ")
                .append("measures Hamming distance of each 4-mer to the reverse-complement of ")
                .append("the adjacent 4-mer. Hairpin counts were flat across loop widths; this ")
                .append("asks whether exact pairing (distance 0) is enriched versus chance.\n\n");

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

        md.append("## Pairing mismatches\n\n");
        md.append("Overlapping 8-base windows: left 4-mer versus reverse-complement of the ")
                .append("right 4-mer. Distance 0 is an exact even stem of length 8. Expected ")
                .append("counts use binomial complementary-pair chance (2pA pT + 2pG pC).\n\n");
        md.append("| Hamming | Count | Share % | Expected | Enrichment |\n| --- | --- | --- | --- | --- |\n");
        for (var row : mismatches.rows()) {
            md.append("| ").append(row.distance()).append(" | ")
                    .append(row.count()).append(" | ")
                    .append(String.format(Locale.ROOT, "%.2f", row.sharePercent())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.1f", row.expected())).append(" | ")
                    .append(String.format(Locale.ROOT, "%.3f", row.enrichment())).append(" |\n");
        }
        md.append("\n| Metric | Value |\n| --- | --- |\n");
        md.append("| Windows | ").append(mismatches.windows()).append(" |\n");
        md.append("| Complementary pair chance | ")
                .append(String.format(Locale.ROOT, "%.4f", mismatches.pairChance())).append(" |\n");
        md.append("| Modal Hamming distance | ").append(mismatches.modalDistance()).append(" |\n");
        md.append("| Distance-0 share | ")
                .append(String.format(Locale.ROOT, "%.4f", mismatches.zeroShare())).append(" |\n");
        md.append("| Distance-0 expected | ")
                .append(String.format(Locale.ROOT, "%.4f", mismatches.zeroExpected())).append(" |\n");
        md.append("| Distance-0 enrichment | ")
                .append(String.format(Locale.ROOT, "%.3f", mismatches.zeroEnrichment())).append(" |\n\n");
        md.append("`").append(mismatches.toTextRow()).append("`\n\n");

        md.append("## Frame remainder\n\n");
        md.append("Hairpins: modal loop **").append(hairpins.modalLoop())
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
        appendExtractedRules(md, sequence, wraps, runs, skew, islands, joints, dimers, gcIslands, codons, lags, foldback, hairpins, mismatches);

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
        md.append("- Pairing mismatches: modal Hamming **").append(mismatches.modalDistance())
                .append("**; distance-0 share **")
                .append(String.format(Locale.ROOT, "%.4f", mismatches.zeroShare()))
                .append("** vs expected **")
                .append(String.format(Locale.ROOT, "%.4f", mismatches.zeroExpected()))
                .append("** (enrichment **")
                .append(String.format(Locale.ROOT, "%.3f", mismatches.zeroEnrichment()))
                .append("**). If distance 0 is not enriched, exact pairing is chance and ")
                .append("fuzzy search should be abandoned; if distance 1 is the mode, keep ")
                .append("one-substitution search; if the histogram is binomial-flat, look at ")
                .append("insertions (indel alignment) instead of substitutions.\n");
        md.append("- Hairpin remainder: modal loop **").append(hairpins.modalLoop())
                .append("**, adjacent **").append(hairpins.adjacent()).append("**.\n");
        md.append("- Homopolymer longest **").append(runs.longestBase()).append(" × ")
                .append(runs.longestLength()).append("**.\n");
        md.append("- Module split (TZ §6.12) is **not** indicated: single Gradle module, no sub-module reports.\n");

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
            MismatchCensus mismatches) {
        long a = sequence.canonicalCounts().getOrDefault('A', 0L);
        long t = sequence.canonicalCounts().getOrDefault('T', 0L);
        md.append("1. **R1 Exact enclosure is flat.** Hairpin modal loop ")
                .append(hairpins.modalLoop()).append(" (").append(hairpins.modalHairpins())
                .append(") vs adjacent ").append(hairpins.adjacent()).append(".\n");
        md.append("2. **R2 Pairing mismatch profile.** Modal Hamming ")
                .append(mismatches.modalDistance())
                .append("; distance-0 share ")
                .append(String.format(Locale.ROOT, "%.4f", mismatches.zeroShare()))
                .append(" vs expected ")
                .append(String.format(Locale.ROOT, "%.4f", mismatches.zeroExpected()))
                .append(" (×")
                .append(String.format(Locale.ROOT, "%.3f", mismatches.zeroEnrichment()))
                .append("). Global A=").append(a).append(", T=").append(t).append(".\n");
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
