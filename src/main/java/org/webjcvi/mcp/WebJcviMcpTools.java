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
import org.webjcvi.loop.LoopException;
import org.webjcvi.loop.StemLoop;
import org.webjcvi.fuzzy.FuzzyException;
import org.webjcvi.fuzzy.FuzzyFind;
import org.webjcvi.contrast.BlockContrast;
import org.webjcvi.contrast.ContrastException;
import org.webjcvi.mirror.MirrorException;
import org.webjcvi.mirror.MirrorJoint;
import org.webjcvi.seam.SeamException;
import org.webjcvi.seam.SeamGuard;
import org.webjcvi.phase.PhaseException;
import org.webjcvi.phase.PhaseJoint;
import org.webjcvi.frame.FrameException;
import org.webjcvi.frame.FrameFields;
import org.webjcvi.clone.CloneException;
import org.webjcvi.clone.CloneScan;
import org.webjcvi.prefix.PrefixException;
import org.webjcvi.prefix.PrefixGroup;
import org.webjcvi.key.KeyException;
import org.webjcvi.key.KeyWidth;
import org.webjcvi.fork.ForkException;
import org.webjcvi.fork.ForkScan;
import org.webjcvi.affix.AffixException;
import org.webjcvi.affix.AffixScan;
import org.webjcvi.lane.LaneException;
import org.webjcvi.lane.LaneScan;
import org.webjcvi.row.RowException;
import org.webjcvi.row.RowScan;
import org.webjcvi.logs.LogAnalysisService;

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
    private final StemLoop loops;
    private final FuzzyFind fuzzy;
    private final BlockContrast contrast;
    private final MirrorJoint mirrors;
    private final SeamGuard seams;
    private final PhaseJoint phase;
    private final FrameFields fields;
    private final CloneScan clones;
    private final PrefixGroup prefixes;
    private final KeyWidth keys;
    private final ForkScan forks;
    private final AffixScan affixes;
    private final LaneScan lanes;
    private final RowScan rows;
    private final LogAnalysisService logAnalysis;

    public WebJcviMcpTools(FileStorageService storage, DnaReportService reports) {
        this(storage, reports, new ScratchTape(), new BannerSplitter(), new PairDrift(),
                new WrapReflow(), new RareClassScanner(), new RareBreakTokenizer(), new KmerStamp(),
                new PalindromeScan(), new StemLoop(), new FuzzyFind(), new BlockContrast(),
                new MirrorJoint(), new SeamGuard(), new PhaseJoint(), new FrameFields(),
                new CloneScan(), new PrefixGroup(), new KeyWidth(), new ForkScan(), new AffixScan(), new LaneScan(), new RowScan(), new LogAnalysisService());
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
            PalindromeScan palindromes,
            StemLoop loops,
            FuzzyFind fuzzy,
            BlockContrast contrast,
            MirrorJoint mirrors,
            SeamGuard seams,
            PhaseJoint phase,
            FrameFields fields,
            CloneScan clones,
            PrefixGroup prefixes,
            KeyWidth keys,
            ForkScan forks,
            AffixScan affixes,
            LaneScan lanes,
            RowScan rows,
            LogAnalysisService logAnalysis) {
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
        this.loops = loops;
        this.fuzzy = fuzzy;
        this.contrast = contrast;
        this.mirrors = mirrors;
        this.seams = seams;
        this.phase = phase;
        this.fields = fields;
        this.clones = clones;
        this.prefixes = prefixes;
        this.keys = keys;
        this.forks = forks;
        this.affixes = affixes;
        this.lanes = lanes;
        this.rows = rows;
        this.logAnalysis = logAnalysis;
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

    @Tool(name = "extract_spans", description = "Extract complementary delimiter spans (()[]{}<>) and the loop they enclose from caller-supplied text. Skips quotes (same-base analog). Default min loop 1. Not the DNA file and not a project path.")
    public String extractSpans(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Minimum loop length; use 1 unless you need a different floor") int minLoop) {
        return run(() -> {
            int floor = minLoop < 1 ? StemLoop.DEFAULT_MIN_LOOP : minLoop;
            var scan = loops.extract(text, floor);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.spans().isEmpty()) {
                out.append("(no spans)");
                return out.toString();
            }
            for (var span : scan.spans()) {
                out.append("#").append(span.index())
                        .append(" ").append(span.opener()).append("…").append(span.closer())
                        .append(" offset ").append(span.offset())
                        .append(" loop ").append(span.loopLength())
                        .append('\n')
                        .append(span.preview()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "fuzzy_find", description = "Find near-matches of a motif in caller-supplied text using Hamming distance (default max 1). Min motif length 4. Not the DNA file and not a project path.")
    public String fuzzyFind(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Motif to search; at least 4 characters") String motif,
            @ToolParam(description = "Maximum Hamming distance; use 1 unless you need a different floor") int maxDist) {
        return run(() -> {
            int cap = maxDist < 0 ? FuzzyFind.DEFAULT_MAX_DIST : maxDist;
            var scan = fuzzy.search(text, motif, cap);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.hits().isEmpty()) {
                out.append("(no hits)");
                return out.toString();
            }
            for (var hit : scan.hits()) {
                out.append("#").append(hit.index())
                        .append(" offset ").append(hit.offset())
                        .append(" distance ").append(hit.distance())
                        .append('\n')
                        .append(hit.preview()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "block_contrast", description = "Find adjacent-window Hamming stutters in caller-supplied text. Default width 4, flag distance ≤ 1. Neighbors should differ (DNA distance 0 is depleted). Not the DNA file and not a project path.")
    public String blockContrast(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Window width; use 4 unless you need a different floor") int width,
            @ToolParam(description = "Flag Hamming distances at or below this; use 1 unless you need a different floor") int flagMax) {
        return run(() -> {
            int w = width < 2 ? BlockContrast.DEFAULT_WIDTH : width;
            int cap = flagMax < 0 ? BlockContrast.DEFAULT_FLAG_MAX : flagMax;
            var scan = contrast.scan(text, w, cap);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.hits().isEmpty()) {
                out.append("(no stutters)");
                return out.toString();
            }
            for (var hit : scan.hits()) {
                out.append("#").append(hit.index())
                        .append(" offset ").append(hit.offset())
                        .append(" distance ").append(hit.distance())
                        .append('\n')
                        .append(hit.left()).append(" | ").append(hit.right()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "find_mirrors", description = "Find adjacent reverse joints in caller-supplied text: the next block is the reverse of the current one, but not a copy. Default width 4. Not the DNA file and not a project path.")
    public String findMirrors(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Window width; use 4 unless you need a different floor") int width) {
        return run(() -> {
            int w = width < 2 ? MirrorJoint.DEFAULT_WIDTH : width;
            var scan = mirrors.scan(text, w);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.hits().isEmpty()) {
                out.append("(no mirrors)");
                return out.toString();
            }
            for (var hit : scan.hits()) {
                out.append("#").append(hit.index())
                        .append(" offset ").append(hit.offset())
                        .append(" identity ").append(hit.identityDistance())
                        .append('\n')
                        .append(hit.left()).append(" | ").append(hit.right()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "flag_seams", description = "Flag palindromic wrap seams in caller-supplied text: last 4 chars of a full-width line are the reverse of the first 4 of the next, but not a copy. Default wrap 70, block 4. Not the DNA file and not a project path.")
    public String flagSeams(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Minimum line width that counts as a wrap; use 70 unless you need a different floor") int wrapWidth,
            @ToolParam(description = "Block width at the seam; use 4 unless you need a different floor") int blockWidth) {
        return run(() -> {
            int wrap = wrapWidth < 8 ? SeamGuard.DEFAULT_WRAP : wrapWidth;
            int block = blockWidth < 2 ? SeamGuard.DEFAULT_BLOCK : blockWidth;
            var scan = seams.scan(text, wrap, block);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.hits().isEmpty()) {
                out.append("(no seams)");
                return out.toString();
            }
            for (var hit : scan.hits()) {
                out.append("#").append(hit.index())
                        .append(" line ").append(hit.line())
                        .append(" identity ").append(hit.identityDistance())
                        .append('\n')
                        .append(hit.left()).append(" | ").append(hit.right()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "phase_mirrors", description = "Find reverse joints at a fixed wrap-frame column in caller-supplied text. Default wrap 70, phase 19, block 4. Newlines are dropped. Not the DNA file and not a project path.")
    public String phaseMirrors(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Wrap frame width; use 70 unless you need a different floor") int wrapWidth,
            @ToolParam(description = "Column inside the frame; use 19 unless you need a different floor") int phase,
            @ToolParam(description = "Block width; use 4 unless you need a different floor") int blockWidth) {
        return run(() -> {
            int wrap = wrapWidth < 8 ? PhaseJoint.DEFAULT_WRAP : wrapWidth;
            int column = phase < 0 ? PhaseJoint.DEFAULT_PHASE : phase;
            int block = blockWidth < 2 ? PhaseJoint.DEFAULT_BLOCK : blockWidth;
            var scan = this.phase.scan(text, wrap, column, block);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.hits().isEmpty()) {
                out.append("(no phase joints)");
                return out.toString();
            }
            for (var hit : scan.hits()) {
                out.append("#").append(hit.index())
                        .append(" offset ").append(hit.offset())
                        .append(" identity ").append(hit.identityDistance())
                        .append('\n')
                        .append(hit.left()).append(" | ").append(hit.right()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "extract_fields", description = "Extract the three 8-character slots (columns 4, 19, 58) from 70-wide wrap frames of caller-supplied text. Newlines are dropped. Not the DNA file and not a project path.")
    public String extractFields(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Wrap frame width; use 70 unless you need a different floor") int wrapWidth) {
        return run(() -> {
            int wrap = wrapWidth < 66 ? FrameFields.DEFAULT_WRAP : wrapWidth;
            var scan = fields.extract(text, wrap);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.records().isEmpty()) {
                out.append("(no records)");
                return out.toString();
            }
            for (var row : scan.records()) {
                out.append("#").append(row.index())
                        .append(" offset ").append(row.offset())
                        .append('\n')
                        .append("rc ").append(row.rc())
                        .append(" rev ").append(row.reverse())
                        .append(" id ").append(row.identity())
                        .append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "find_clones", description = "Find duplicate wrap-frame records of caller-supplied text. Default wrap 70. Newlines are dropped. Not the DNA file and not a project path.")
    public String findClones(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Wrap frame width; use 70 unless you need a different floor") int wrapWidth) {
        return run(() -> {
            int wrap = wrapWidth < 8 ? CloneScan.DEFAULT_WRAP : wrapWidth;
            var scan = clones.scan(text, wrap);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.hits().isEmpty()) {
                out.append("(no clones)");
                return out.toString();
            }
            for (var hit : scan.hits()) {
                out.append("count ").append(hit.count())
                        .append(" firstOffset ").append(hit.firstOffset())
                        .append('\n')
                        .append(hit.preview())
                        .append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "group_prefixes", description = "Group wrap-frame records of caller-supplied text by a shared leading prefix. Default wrap 70, prefix 8. Newlines are dropped. Not the DNA file and not a project path.")
    public String groupPrefixes(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Wrap frame width; use 70 unless you need a different floor") int wrapWidth,
            @ToolParam(description = "Prefix length; use 8 unless you need a different floor") int prefixLength) {
        return run(() -> {
            int wrap = wrapWidth < 8 ? PrefixGroup.DEFAULT_WRAP : wrapWidth;
            int prefix = prefixLength < 1 ? PrefixGroup.DEFAULT_PREFIX : prefixLength;
            var scan = prefixes.group(text, wrap, prefix);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.hits().isEmpty()) {
                out.append("(no prefix families)");
                return out.toString();
            }
            for (var hit : scan.hits()) {
                out.append("count ").append(hit.count())
                        .append(" firstOffset ").append(hit.firstOffset())
                        .append(" prefix ").append(hit.prefix())
                        .append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "measure_key_width", description = "Find the smallest wrap-frame prefix length that uniquely identifies every record of caller-supplied text. Default wrap 70, floor 8. Newlines are dropped. Not the DNA file and not a project path.")
    public String measureKeyWidth(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Wrap frame width; use 70 unless you need a different floor") int wrapWidth) {
        return run(() -> {
            int wrap = wrapWidth < 8 ? KeyWidth.DEFAULT_WRAP : wrapWidth;
            var scan = keys.measure(text, wrap);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.samples().isEmpty()) {
                out.append("(no wrap frames)");
                return out.toString();
            }
            for (var hit : scan.samples()) {
                out.append("k ").append(hit.length())
                        .append(" distinct ").append(hit.distinct())
                        .append(" families ").append(hit.familyCount())
                        .append(" uniqueShare ").append(hit.uniqueShare())
                        .append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "find_forks", description = "Find wrap-frame records of caller-supplied text that still collide at a near-unique prefix (default 16) and report the column where they first fork. Default wrap 70. Newlines are dropped. Not the DNA file and not a project path.")
    public String findForks(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Wrap frame width; use 70 unless you need a different floor") int wrapWidth,
            @ToolParam(description = "Prefix length; use 16 unless you need a different floor") int prefixLength) {
        return run(() -> {
            int wrap = wrapWidth < 16 ? ForkScan.DEFAULT_WRAP : wrapWidth;
            int prefix = prefixLength < 1 ? ForkScan.DEFAULT_PREFIX : prefixLength;
            var scan = forks.scan(text, wrap, prefix);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.hits().isEmpty()) {
                out.append("(no residual twins)");
                return out.toString();
            }
            for (var hit : scan.hits()) {
                out.append("count ").append(hit.count())
                        .append(" forkAt ").append(hit.forkAt())
                        .append(" firstOffset ").append(hit.firstOffset())
                        .append(" prefix ").append(hit.prefix())
                        .append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "compare_affixes", description = "Compare leading versus trailing wrap-frame identifier width of caller-supplied text and report which end saturates uniqueness first. Default wrap 70. Newlines are dropped. Not the DNA file and not a project path.")
    public String compareAffixes(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Wrap frame width; use 70 unless you need a different floor") int wrapWidth) {
        return run(() -> {
            int wrap = wrapWidth < AffixScan.FLOOR ? AffixScan.DEFAULT_WRAP : wrapWidth;
            var scan = affixes.measure(text, wrap);
            return scan.summary();
        });
    }

    @Tool(name = "profile_lanes", description = "Profile wrap-frame uniqueness of caller-supplied text at a 16-character tile across several start columns (0, 8, 19, center, tail). Reports the collision trough and fingerprint peak. Default wrap 70. Newlines are dropped. Not the DNA file and not a project path.")
    public String profileLanes(
            @ToolParam(description = "Arbitrary text to scan") String text,
            @ToolParam(description = "Wrap frame width; use 70 unless you need a different floor") int wrapWidth) {
        return run(() -> {
            int wrap = wrapWidth < LaneScan.TILE ? LaneScan.DEFAULT_WRAP : wrapWidth;
            var scan = lanes.profile(text, wrap);
            StringBuilder out = new StringBuilder(scan.summary()).append('\n');
            if (scan.lanes().isEmpty()) {
                out.append("(no wrap frames)");
                return out.toString();
            }
            for (var hit : scan.lanes()) {
                out.append("start ").append(hit.start())
                        .append(" distinct ").append(hit.distinct())
                        .append(" uniqueShare ").append(hit.uniqueShare())
                        .append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "profile_rows", description = "Profile uniqueness of newline-delimited caller-supplied lines. Each non-blank line is a record; newlines are kept. Reports unique share of a 16-character line prefix and saturating uniqueAt. Not the DNA file and not a project path.")
    public String profileRows(
            @ToolParam(description = "Arbitrary text to scan") String text) {
        return run(() -> rows.profile(text).summary());
    }

    @Tool(name = "analyze_logs", description = "Analyze caller-supplied log text with the shared log-analysis facade. Returns a JSON report. Not the DNA file. Truncates at 524288 characters.")
    public String analyzeLogs(
            @ToolParam(description = "Log text to analyze") String text) {
        return run(() -> logAnalysis.analyzeToJson(text));
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
                 | RareException | TokenException | StampException | FoldException | LoopException
                 | FuzzyException | ContrastException | MirrorException | SeamException | PhaseException | FrameException | CloneException | PrefixException | KeyException | ForkException | AffixException | LaneException | RowException e) {
            return "Error: " + e.getMessage();
        }
    }

    @FunctionalInterface
    private interface ToolAction {
        String execute();
    }
}
