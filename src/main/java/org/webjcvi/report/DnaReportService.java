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
import org.webjcvi.dna.DnaParser;
import org.webjcvi.dna.DnaSequence;
import org.webjcvi.dna.FrameSkewCensus;
import org.webjcvi.dna.HomopolymerProfile;
import org.webjcvi.dna.WrapCensus;
import org.webjcvi.storage.FileStorageService;
import org.webjcvi.storage.StorageNotFoundException;

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
        Map<String, String> sections = buildSections(sequence, generatedAt, javaSources, wraps, runs, skew);
        String markdown = renderMarkdown(sections, sequence, generatedAt, javaSources, wraps, runs, skew);
        storage.writeText(reportRelativePath(), markdown);
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
            FrameSkewCensus skew) {
        Map<String, String> sections = new LinkedHashMap<>();
        sections.put("meta", "generatedAt=" + ISO.format(generatedAt));
        sections.put("length", Integer.toString(sequence.length()));
        sections.put("gcPercent", String.format(Locale.ROOT, "%.2f", sequence.gcPercent()));
        sections.put("ambiguousTotal", Long.toString(sequence.ambiguousTotal()));
        sections.put("invalidTotal", Long.toString(sequence.invalidTotal()));
        sections.put("javaSourceCount", Integer.toString(javaSources.size()));
        sections.put("wrapModalWidth", Integer.toString(wraps.modalWidth()));
        sections.put("skewWindow", Integer.toString(skew.window()));
        sections.put("skewWindowCount", Integer.toString(skew.windowCount()));
        sections.put("meanAbsAtSkew", String.format(Locale.ROOT, "%.4f", skew.meanAbsAtSkew()));
        sections.put("maxAbsAtSkew", String.format(Locale.ROOT, "%.4f", skew.maxAbsAtSkew()));
        sections.put("windowsPastAtThreshold", Integer.toString(skew.windowsPastThreshold()));
        sections.put("longestHomopolymer", runs.longestBase() + "x" + runs.longestLength());
        sections.put("homopolymerRunsAtLeast5", Long.toString(runs.runsAtLeast5()));
        return sections;
    }

    private String renderMarkdown(
            Map<String, String> sections,
            DnaSequence sequence,
            Instant generatedAt,
            List<String> javaSources,
            WrapCensus wraps,
            HomopolymerProfile runs,
            FrameSkewCensus skew) {
        StringBuilder md = new StringBuilder();
        md.append("# WebJCVI DNA Report\n\n");
        md.append("Generated at **").append(ISO.format(generatedAt.atOffset(ZoneOffset.UTC))).append("**.\n\n");
        md.append("This report is rebuilt from `").append(dnaRelativePath)
                .append("` and a snapshot of the current codebase. Version 3 of the builder ")
                .append("walks wrap-sized windows and measures local Chargaff skew — ")
                .append("global A≈T can hide frame-level imbalance.\n\n");

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

        md.append("## Frame-window Chargaff skew\n\n");
        md.append("Each window is **").append(skew.window())
                .append("** bases (the modal wrap width). AT-skew is `(A−T)/(A+T)`; ")
                .append("GC-skew is `(G−C)/(G+C)`. A window is flagged when `|AT-skew|` exceeds ")
                .append(String.format(Locale.ROOT, "%.2f", FrameSkewCensus.LOCAL_THRESHOLD))
                .append(" — larger than the global residual (~0.009).\n\n");
        md.append("| Metric | Value |\n| --- | --- |\n");
        md.append("| Windows | ").append(skew.windowCount()).append(" |\n");
        md.append("| Mean |AT-skew| | ")
                .append(String.format(Locale.ROOT, "%.4f", skew.meanAbsAtSkew())).append(" |\n");
        md.append("| Max |AT-skew| | ")
                .append(String.format(Locale.ROOT, "%.4f", skew.maxAbsAtSkew())).append(" |\n");
        md.append("| Windows with |AT-skew| > threshold | ")
                .append(skew.windowsPastThreshold()).append(" |\n");
        md.append("| Mean |GC-skew| | ")
                .append(String.format(Locale.ROOT, "%.4f", skew.meanAbsGcSkew())).append(" |\n");
        md.append("| Max |GC-skew| | ")
                .append(String.format(Locale.ROOT, "%.4f", skew.maxAbsGcSkew())).append(" |\n\n");
        md.append("`").append(skew.toTextRow()).append("`\n\n");

        md.append("## Wrap width (window source)\n\n");
        md.append("| Metric | Value |\n| --- | --- |\n");
        md.append("| Lines | ").append(wraps.lineCount()).append(" |\n");
        md.append("| Min / median / modal / max | ")
                .append(wraps.minWidth()).append(" / ")
                .append(wraps.medianWidth()).append(" / ")
                .append(wraps.modalWidth()).append(" / ")
                .append(wraps.maxWidth()).append(" |\n\n");

        md.append("## Homopolymer remainder\n\n");
        md.append("Longest **").append(runs.longestBase()).append(" × ").append(runs.longestLength())
                .append("**; ≥5: ").append(runs.runsAtLeast5())
                .append("; 5–9 / 10–19 / 20+: ")
                .append(runs.runs5to9()).append(" / ")
                .append(runs.runs10to19()).append(" / ")
                .append(runs.runs20plus()).append(".\n\n");

        md.append("## Extracted tape rules\n\n");
        appendExtractedRules(md, sequence, wraps, runs, skew);

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
        md.append("- Wrap modal width **").append(wraps.modalWidth())
                .append("** used as the skew window. The next builder should look at *what sits at frame boundaries*, not only the inside of each window.\n");
        md.append("- Local Chargaff: mean |AT-skew| **")
                .append(String.format(Locale.ROOT, "%.4f", skew.meanAbsAtSkew()))
                .append("**, **").append(skew.windowsPastThreshold())
                .append("** windows past ").append(FrameSkewCensus.LOCAL_THRESHOLD)
                .append(". If pairing is a local property, the next hypothesis should use the failing windows, not the global bag.\n");
        md.append("- Homopolymer remainder: longest **").append(runs.longestBase()).append(" × ")
                .append(runs.longestLength()).append("**; 10+ runs remain the rare class (")
                .append(runs.runs10to19()).append(" + ").append(runs.runs20plus()).append(").\n");
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
            FrameSkewCensus skew) {
        long a = sequence.canonicalCounts().getOrDefault('A', 0L);
        long t = sequence.canonicalCounts().getOrDefault('T', 0L);
        md.append("1. **R1 Rare long banners.** Homopolymer 10–19 = ")
                .append(runs.runs10to19())
                .append(", 20+ = ").append(runs.runs20plus())
                .append(" versus 5–9 = ").append(runs.runs5to9())
                .append(". Length ≥ 10 is the separator class; shorter repeats are payload noise.\n");
        md.append("2. **R2 AT-class delimiters.** Max G-run = ")
                .append(runs.maxRun().getOrDefault('G', 0L))
                .append(", max C-run = ")
                .append(runs.maxRun().getOrDefault('C', 0L))
                .append("; longest run overall is ")
                .append(runs.longestBase()).append("×").append(runs.longestLength())
                .append(". The rare 10+ class, when present, comes from the abundant alphabet.\n");
        md.append("3. **R3 Frame-local pairing.** Global A=").append(a)
                .append(", T=").append(t)
                .append(" (|A−T|=").append(Math.abs(a - t))
                .append("). Inside ").append(skew.window()).append("-base windows, mean |AT-skew| = ")
                .append(String.format(Locale.ROOT, "%.4f", skew.meanAbsAtSkew()))
                .append(" and ").append(skew.windowsPastThreshold())
                .append(" windows exceed ").append(FrameSkewCensus.LOCAL_THRESHOLD).append(".\n");
        md.append("4. **R4 Modal wrap is the window.** ").append(wraps.lineCount())
                .append(" lines, modal width ").append(wraps.modalWidth())
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
