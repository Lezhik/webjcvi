package org.webjcvi.mcp;

import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.webjcvi.report.DnaReport;
import org.webjcvi.report.DnaReportService;
import org.webjcvi.segment.BannerSplitter;
import org.webjcvi.segment.SegmentException;
import org.webjcvi.storage.FileStorageService;
import org.webjcvi.storage.StorageException;
import org.webjcvi.tape.ScratchTape;
import org.webjcvi.tape.TapeException;
import org.webjcvi.drift.DriftException;
import org.webjcvi.drift.PairDrift;
import org.webjcvi.rare.RareClassScanner;
import org.webjcvi.rare.RareException;
import org.webjcvi.reflow.ReflowException;
import org.webjcvi.reflow.WrapReflow;
import org.webjcvi.token.RareBreakTokenizer;
import org.webjcvi.token.TokenException;
import org.webjcvi.stamp.KmerStamp;
import org.webjcvi.stamp.StampException;
import org.webjcvi.fold.FoldException;
import org.webjcvi.fold.PalindromeScan;

/**
 * MCP tool surface. File tools go through {@link FileStorageService}. The
 * scratch tape is in-memory caller text and never reads the DNA file or
 * project paths.
 */
@Component
public class WebJcviMcpTools {

    private final FileStorageService storage;
    private final DnaReportService reports;
    private final ScratchTape tape;
    private final BannerSplitter splitter;
    private final PairDrift drift;
    private final WrapReflow reflow;
    private final RareClassScanner rare;
    private final RareBreakTokenizer tokenizer;
    private final KmerStamp stamp;
    private final PalindromeScan palindromes;

    public WebJcviMcpTools(FileStorageService storage, DnaReportService reports) {
        this(storage, reports, new ScratchTape(), new BannerSplitter(), new PairDrift(),
                new WrapReflow(), new RareClassScanner(), new RareBreakTokenizer(), new KmerStamp(),
                new PalindromeScan());
    }

    @Autowired
    public WebJcviMcpTools(
            FileStorageService storage,
            DnaReportService reports,
            ScratchTape tape,
            BannerSplitter splitter,
            PairDrift drift,
            WrapReflow reflow,
            RareClassScanner rare,
            RareBreakTokenizer tokenizer,
            KmerStamp stamp,
            PalindromeScan palindromes) {
        this.storage = storage;
        this.reports = reports;
        this.tape = tape;
        this.splitter = splitter;
        this.drift = drift;
        this.reflow = reflow;
        this.rare = rare;
        this.tokenizer = tokenizer;
        this.stamp = stamp;
        this.palindromes = palindromes;
    }

    @Tool(name = "regenerate_dna_report", description = "Clear prior reports and rebuild the DNA report from jcvi-dna.txt and the current codebase.")
    public String regenerateDnaReport() {
        return run(() -> {
            DnaReport report = reports.regenerate();
            return "Wrote " + reports.reportRelativePath()
                    + " (" + report.sequence().length() + " bases, GC "
                    + String.format("%.2f", report.sequence().gcPercent()) + "%)";
        });
    }

    @Tool(name = "read_dna_report", description = "Read the current DNA report markdown from build/reports/dna/.")
    public String readDnaReport() {
        return run(() -> reports.loadCurrent()
                .map(DnaReport::markdown)
                .orElse("No report has been generated yet. Call regenerate_dna_report first."));
    }

    @Tool(name = "tape_load", description = "Replace the in-memory scratch tape with caller-supplied text. Not the DNA file and not a project path. Limit 524288 characters.")
    public String tapeLoad(
            @ToolParam(description = "Arbitrary text to search later") String text) {
        return run(() -> {
            tape.load(text);
            return "loaded " + tape.length() + " characters";
        });
    }

    @Tool(name = "tape_find", description = "Fold-then-exact search on the scratch tape. Returns line, offset, and snippet.")
    public String tapeFind(
            @ToolParam(description = "Motif; matching is case-insensitive after trim") String motif) {
        return run(() -> {
            var hits = tape.find(motif);
            if (hits.isEmpty()) {
                return "(no hits)";
            }
            StringBuilder out = new StringBuilder();
            for (var hit : hits) {
                out.append("line ").append(hit.line())
                        .append(" offset ").append(hit.offset())
                        .append(": ").append(hit.snippet()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "tape_runs", description = "List consecutive identical-character runs on the scratch tape (homopolymer analog for any text).")
    public String tapeRuns(
            @ToolParam(description = "Minimum run length, at least 2") int minLength) {
        return run(() -> {
            int floor = minLength < 2 ? 2 : minLength;
            var runs = tape.runs(floor, 40);
            if (runs.isEmpty()) {
                return "(no runs)";
            }
            StringBuilder out = new StringBuilder();
            for (var run : runs) {
                out.append(run.symbol()).append(" x").append(run.length())
                        .append(" @").append(run.offset()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "split_banners", description = "Split caller-supplied text on unusually long identical-character runs (log banners such as ==========). Default min run 10. Not the DNA file and not a project path.")
    public String splitBanners(
            @ToolParam(description = "Arbitrary text to section") String text,
            @ToolParam(description = "Minimum banner length; use 10 unless you need a different floor") int minRun) {
        return run(() -> {
            int floor = minRun < 2 ? BannerSplitter.DEFAULT_MIN_RUN : minRun;
            var sections = splitter.split(text, floor);
            if (sections.isEmpty()) {
                return "(no sections)";
            }
            StringBuilder out = new StringBuilder();
            for (var section : sections) {
                out.append("#").append(section.index())
                        .append(" offset ").append(section.offset())
                        .append(" length ").append(section.length());
                if (!section.banner().isEmpty()) {
                    out.append(" after ").append(section.banner());
                }
                out.append('\n').append(section.preview()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "pair_drift", description = "Scan caller-supplied text in wrap-sized windows (default 70) and list local complementary-bracket hotspots. Not the DNA file and not a project path.")
    public String pairDrift(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Window width; use 70 unless you need a different frame") int window) {
        return run(() -> {
            int width = window < 2 ? PairDrift.DEFAULT_WINDOW : window;
            var scan = drift.scan(text, width, PairDrift.DEFAULT_THRESHOLD);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.hotspots().isEmpty()) {
                out.append("(no hotspots)");
                return out.toString();
            }
            for (var hit : scan.hotspots()) {
                out.append("#").append(hit.index())
                        .append(" offset ").append(hit.offset())
                        .append(" opens ").append(hit.opens())
                        .append(" closes ").append(hit.closes())
                        .append(" skew ").append(hit.skewText())
                        .append('\n')
                        .append(hit.preview()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "unwrap_wraps", description = "Unwrap hard-wrapped caller text: join lines of at least the wrap width (default 70); a shorter line ends the paragraph. Not the DNA file and not a project path.")
    public String unwrapWraps(
            @ToolParam(description = "Hard-wrapped text") String text,
            @ToolParam(description = "Wrap width; use 70 unless the paste used a different column") int width) {
        return run(() -> {
            int frame = width < 8 ? WrapReflow.DEFAULT_WIDTH : width;
            var result = reflow.unwrap(text, frame);
            StringBuilder out = new StringBuilder();
            out.append("paragraphs=").append(result.paragraphCount())
                    .append(" stitches=").append(result.stitches())
                    .append(" lines=").append(result.sourceLines())
                    .append('\n');
            if (result.paragraphs().isEmpty()) {
                out.append("(no paragraphs)");
                return out.toString();
            }
            for (var para : result.paragraphs()) {
                out.append("#").append(para.index())
                        .append(" length ").append(para.length())
                        .append('\n')
                        .append(para.preview()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "rare_islands", description = "Find islands of the rare character class in caller-supplied text. The rare class is the bottom ~24.14% of symbol mass (GC analog). Not the DNA file and not a project path.")
    public String rareIslands(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Minimum island length; use 3 unless you need a different floor") int minIsland) {
        return run(() -> {
            int floor = minIsland < 2 ? RareClassScanner.DEFAULT_MIN_ISLAND : minIsland;
            var scan = rare.scan(text, floor);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.islands().isEmpty()) {
                out.append("(no islands)");
                return out.toString();
            }
            for (var island : scan.islands()) {
                out.append("#").append(island.index())
                        .append(" offset ").append(island.offset())
                        .append(" length ").append(island.length())
                        .append('\n')
                        .append(island.preview()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "cut_tokens", description = "Split caller-supplied text on the rare character class (bottom ~24.14% of symbol mass) and list the majority tokens between those breaks. Not the DNA file and not a project path.")
    public String cutTokens(
            @ToolParam(description = "Arbitrary text to tokenize") String text,
            @ToolParam(description = "Minimum token length; use 2 unless you need a different floor") int minToken) {
        return run(() -> {
            int floor = minToken < 2 ? RareBreakTokenizer.DEFAULT_MIN_TOKEN : minToken;
            var cut = tokenizer.cut(text, floor);
            StringBuilder out = new StringBuilder(cut.summary()).append('\n');
            if (cut.tokens().isEmpty()) {
                out.append("(no tokens)");
                return out.toString();
            }
            for (var token : cut.tokens()) {
                out.append("#").append(token.index())
                        .append(" offset ").append(token.offset())
                        .append(" length ").append(token.length())
                        .append('\n')
                        .append(token.preview()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "kmer_stamps", description = "Rank overlapping k-mers (default k=3) in caller-supplied text after folding case and dropping whitespace. Lists the most common short stamps. Not the DNA file and not a project path.")
    public String kmerStamps(
            @ToolParam(description = "Arbitrary text to rank") String text,
            @ToolParam(description = "k-mer width; use 3 unless you need a different stamp size") int k) {
        return run(() -> {
            int width = k < 2 ? KmerStamp.DEFAULT_K : k;
            var census = stamp.rank(text, width);
            StringBuilder out = new StringBuilder(census.summary()).append('\n');
            if (census.stamps().isEmpty()) {
                out.append("(no stamps)");
                return out.toString();
            }
            for (var row : census.stamps()) {
                out.append("#").append(row.index())
                        .append(" ").append(row.kmer())
                        .append(" x").append(row.count())
                        .append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "find_palindromes", description = "Find even palindromes (length at least 4, not homopolymers) in caller-supplied text after folding case and dropping whitespace. Not the DNA file and not a project path.")
    public String findPalindromes(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Minimum palindrome length; use 4 unless you need a different floor") int minLength) {
        return run(() -> {
            int floor = minLength < 4 ? PalindromeScan.DEFAULT_MIN : minLength;
            var scan = palindromes.find(text, floor);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.hits().isEmpty()) {
                out.append("(no palindromes)");
                return out.toString();
            }
            for (var hit : scan.hits()) {
                out.append("#").append(hit.index())
                        .append(" offset ").append(hit.offset())
                        .append(" length ").append(hit.length())
                        .append('\n')
                        .append(hit.preview()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "list_files", description = "List files inside the project directory. Paths must be relative to the project root.")
    public String listFiles(
            @ToolParam(description = "Relative directory to list, or '.' for the project root") String path,
            @ToolParam(description = "If true, list regular files recursively") boolean recursive) {
        return run(() -> {
            String directory = (path == null || path.isBlank()) ? "." : path;
            List<String> names = storage.list(directory, recursive);
            if (names.isEmpty()) {
                return "(empty)";
            }
            return String.join("\n", names);
        });
    }

    @Tool(name = "read_file", description = "Read a UTF-8 text file inside the project directory. Rejects path traversal, absolute paths, symlink escapes, and files over 500 MB.")
    public String readFile(
            @ToolParam(description = "Path relative to the project root") String path) {
        return run(() -> storage.readText(path));
    }

    private static String run(ToolAction action) {
        try {
            return action.execute();
        } catch (StorageException | TapeException | SegmentException | DriftException | ReflowException
                 | RareException | TokenException | StampException | FoldException e) {
            return "Error: " + e.getMessage();
        }
    }

    @FunctionalInterface
    private interface ToolAction {
        String execute();
    }
}
