package org.webjcvi.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.webjcvi.drift.PairDrift;
import org.webjcvi.rare.RareClassScanner;
import org.webjcvi.reflow.WrapReflow;
import org.webjcvi.token.RareBreakTokenizer;
import org.webjcvi.stamp.KmerStamp;
import org.webjcvi.fold.PalindromeScan;
import org.webjcvi.loop.StemLoop;
import org.webjcvi.fuzzy.FuzzyFind;
import org.webjcvi.contrast.BlockContrast;
import org.webjcvi.mirror.MirrorJoint;
import org.webjcvi.seam.SeamGuard;
import org.webjcvi.phase.PhaseJoint;
import org.webjcvi.frame.FrameFields;
import org.webjcvi.clone.CloneScan;
import org.webjcvi.prefix.PrefixGroup;
import org.webjcvi.key.KeyWidth;
import org.webjcvi.fork.ForkScan;
import org.webjcvi.affix.AffixScan;
import org.webjcvi.lane.LaneScan;
import org.webjcvi.row.RowScan;
import org.webjcvi.cliff.CliffScan;
import org.webjcvi.runway.RunwayScan;
import org.webjcvi.rise.RiseScan;
import org.webjcvi.majority.MajorityScan;
import org.webjcvi.near.NearScan;
import org.webjcvi.logs.LogAnalysisReport;
import org.webjcvi.logs.LogAnalysisService;
import org.webjcvi.report.DnaReport;
import org.webjcvi.report.DnaReportService;
import org.webjcvi.segment.BannerSplitter;
import org.webjcvi.storage.FileStorageService;
import org.webjcvi.tape.ScratchTape;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * HTTP API for sandboxed file access and report regeneration. Mirrors the MCP
 * tools so both surfaces share {@link FileStorageService} and
 * {@link DnaReportService}.
 */
@RestController
@RequestMapping("/api")
public class WorkspaceApiController {

    private final FileStorageService storage;
    private final DnaReportService reports;
    private final ScratchTape tape;
    private final BannerSplitter splitter;
    private final PairDrift drift;
    private final WrapReflow wrapReflow;
    private final RareClassScanner rareScanner;
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
    private final CliffScan cliffs;
    private final RunwayScan runways;
    private final RiseScan rises;
    private final MajorityScan majorities;
    private final NearScan nears;
    private final LogAnalysisService logAnalysis;

    public WorkspaceApiController(
            FileStorageService storage,
            DnaReportService reports,
            ScratchTape tape,
            BannerSplitter splitter,
            PairDrift drift,
            WrapReflow wrapReflow,
            RareClassScanner rareScanner,
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
            CliffScan cliffs,
            RunwayScan runways,
            RiseScan rises,
            MajorityScan majorities,
            NearScan nears,
            LogAnalysisService logAnalysis) {
        this.storage = storage;
        this.reports = reports;
        this.tape = tape;
        this.splitter = splitter;
        this.drift = drift;
        this.wrapReflow = wrapReflow;
        this.rareScanner = rareScanner;
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
        this.cliffs = cliffs;
        this.runways = runways;
        this.rises = rises;
        this.majorities = majorities;
        this.nears = nears;
        this.logAnalysis = logAnalysis;
    }

    @GetMapping("/files")
    public Mono<List<String>> list(
            @RequestParam(name = "path", defaultValue = ".") String path,
            @RequestParam(name = "recursive", defaultValue = "false") boolean recursive) {
        return Mono.fromCallable(() -> storage.list(path, recursive))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(value = "/files/content", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> read(@RequestParam("path") String path) {
        return Mono.fromCallable(() -> storage.readText(path))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping(value = "/report", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<String> currentReport() {
        return Mono.fromCallable(() -> reports.loadCurrent()
                        .map(DnaReport::markdown)
                        .orElseThrow(() -> new org.webjcvi.storage.StorageNotFoundException("No report has been generated yet")))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/report/regenerate")
    public Mono<Map<String, Object>> regenerate() {
        return Mono.fromCallable(reports::regenerate)
                .subscribeOn(Schedulers.boundedElastic())
                .map(report -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("path", reports.reportRelativePath());
                    body.put("length", report.sequence().length());
                    body.put("gcPercent", report.sequence().gcPercent());
                    body.put("sections", report.sectionNames());
                    return body;
                });
    }

    @PostMapping("/tape")
    public Mono<Map<String, Object>> loadTape(@RequestParam("text") String text) {
        return Mono.fromCallable(() -> {
                    tape.load(text);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("length", tape.length());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/tape")
    public Mono<Map<String, Object>> tapeStatus() {
        return Mono.fromCallable(() -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("length", tape.length());
                    body.put("preview", tape.preview(80));
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/tape/find")
    public Mono<List<Map<String, Object>>> find(@RequestParam("q") String query) {
        return Mono.fromCallable(() -> tape.find(query).stream()
                        .map(hit -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("offset", hit.offset());
                            row.put("line", hit.line());
                            row.put("snippet", hit.snippet());
                            return row;
                        })
                        .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/tape/runs")
    public Mono<List<Map<String, Object>>> runs(
            @RequestParam(name = "min", defaultValue = "5") int minLength) {
        return Mono.fromCallable(() -> tape.runs(minLength, 40).stream()
                        .map(run -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("symbol", String.valueOf(run.symbol()));
                            row.put("offset", run.offset());
                            row.put("length", run.length());
                            return row;
                        })
                        .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/split")
    public Mono<List<Map<String, Object>>> split(
            @RequestParam("text") String text,
            @RequestParam(name = "min", defaultValue = "10") int minRun) {
        return Mono.fromCallable(() -> splitter.split(text, minRun).stream()
                        .map(section -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            row.put("index", section.index());
                            row.put("offset", section.offset());
                            row.put("length", section.length());
                            row.put("preview", section.preview());
                            row.put("banner", section.banner());
                            return row;
                        })
                        .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/drift")
    public Mono<Map<String, Object>> drift(
            @RequestParam("text") String text,
            @RequestParam(name = "window", defaultValue = "70") int window) {
        return Mono.fromCallable(() -> {
                    var scan = drift.scan(text, window, PairDrift.DEFAULT_THRESHOLD);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("window", scan.window());
                    body.put("windowCount", scan.windowCount());
                    body.put("hotspotCount", scan.hotspotCount());
                    body.put("opens", scan.opens());
                    body.put("closes", scan.closes());
                    body.put("globalSkew", scan.globalSkew());
                    body.put("hotspots", scan.hotspots().stream()
                            .map(hit -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("index", hit.index());
                                row.put("offset", hit.offset());
                                row.put("length", hit.length());
                                row.put("opens", hit.opens());
                                row.put("closes", hit.closes());
                                row.put("skew", hit.skew());
                                row.put("preview", hit.preview());
                                return row;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/reflow")
    public Mono<Map<String, Object>> reflow(
            @RequestParam("text") String text,
            @RequestParam(name = "width", defaultValue = "70") int width) {
        return Mono.fromCallable(() -> {
                    var result = wrapReflow.unwrap(text, width);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("width", result.width());
                    body.put("sourceLines", result.sourceLines());
                    body.put("stitches", result.stitches());
                    body.put("paragraphCount", result.paragraphCount());
                    body.put("paragraphs", result.paragraphs().stream()
                            .map(para -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("index", para.index());
                                row.put("length", para.length());
                                row.put("preview", para.preview());
                                return row;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/rare")
    public Mono<Map<String, Object>> rareIslands(
            @RequestParam("text") String text,
            @RequestParam(name = "min", defaultValue = "3") int minIsland) {
        return Mono.fromCallable(() -> {
                    var scan = rareScanner.scan(text, minIsland);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("rareClass", scan.rareClass());
                    body.put("scanned", scan.scanned());
                    body.put("rareSymbolCount", scan.rareSymbolCount());
                    body.put("islandCount", scan.islandCount());
                    body.put("islands", scan.islands().stream()
                            .map(island -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("index", island.index());
                                row.put("offset", island.offset());
                                row.put("length", island.length());
                                row.put("preview", island.preview());
                                return row;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/tokens")
    public Mono<Map<String, Object>> cutTokens(
            @RequestParam("text") String text,
            @RequestParam(name = "min", defaultValue = "2") int minToken) {
        return Mono.fromCallable(() -> {
                    var cut = tokenizer.cut(text, minToken);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("rareClass", cut.rareClass());
                    body.put("rareSymbolCount", cut.rareSymbolCount());
                    body.put("tokenCount", cut.tokenCount());
                    body.put("tokens", cut.tokens().stream()
                            .map(token -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("index", token.index());
                                row.put("offset", token.offset());
                                row.put("length", token.length());
                                row.put("preview", token.preview());
                                return row;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/stamps")
    public Mono<Map<String, Object>> kmerStamps(
            @RequestParam("text") String text,
            @RequestParam(name = "k", defaultValue = "3") int k) {
        return Mono.fromCallable(() -> {
                    var census = stamp.rank(text, k);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("k", census.k());
                    body.put("scanned", census.scanned());
                    body.put("distinct", census.distinct());
                    body.put("topKmer", census.topKmer());
                    body.put("topCount", census.topCount());
                    body.put("stamps", census.stamps().stream()
                            .map(row -> {
                                Map<String, Object> item = new LinkedHashMap<>();
                                item.put("index", row.index());
                                item.put("kmer", row.kmer());
                                item.put("count", row.count());
                                return item;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/fold")
    public Mono<Map<String, Object>> findPalindromes(
            @RequestParam("text") String text,
            @RequestParam(name = "min", defaultValue = "4") int minLength) {
        return Mono.fromCallable(() -> {
                    var scan = palindromes.find(text, minLength);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("scanned", scan.scanned());
                    body.put("hitCount", scan.hitCount());
                    body.put("longest", scan.longest());
                    body.put("hits", scan.hits().stream()
                            .map(hit -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("index", hit.index());
                                row.put("offset", hit.offset());
                                row.put("length", hit.length());
                                row.put("preview", hit.preview());
                                return row;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/loop")
    public Mono<Map<String, Object>> extractSpans(
            @RequestParam("text") String text,
            @RequestParam(name = "min", defaultValue = "1") int minLoop) {
        return Mono.fromCallable(() -> {
                    var scan = loops.extract(text, minLoop);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("scanned", scan.scanned());
                    body.put("spanCount", scan.spanCount());
                    body.put("nested", scan.nested());
                    body.put("leftoverOpens", scan.leftoverOpens());
                    body.put("leftoverCloses", scan.leftoverCloses());
                    body.put("longestLoop", scan.longestLoop());
                    body.put("spans", scan.spans().stream()
                            .map(span -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("index", span.index());
                                row.put("offset", span.offset());
                                row.put("length", span.length());
                                row.put("loopLength", span.loopLength());
                                row.put("opener", span.opener());
                                row.put("closer", span.closer());
                                row.put("preview", span.preview());
                                return row;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/fuzzy")
    public Mono<Map<String, Object>> fuzzyFind(
            @RequestParam("text") String text,
            @RequestParam("motif") String motif,
            @RequestParam(name = "dist", defaultValue = "1") int maxDist) {
        return Mono.fromCallable(() -> {
                    var scan = fuzzy.search(text, motif, maxDist);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("scanned", scan.scanned());
                    body.put("hitCount", scan.hitCount());
                    body.put("motifLength", scan.motifLength());
                    body.put("maxDist", scan.maxDist());
                    body.put("hits", scan.hits().stream()
                            .map(hit -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("index", hit.index());
                                row.put("offset", hit.offset());
                                row.put("distance", hit.distance());
                                row.put("preview", hit.preview());
                                return row;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/contrast")
    public Mono<Map<String, Object>> blockContrast(
            @RequestParam("text") String text,
            @RequestParam(name = "width", defaultValue = "4") int width,
            @RequestParam(name = "flag", defaultValue = "1") int flagMax) {
        return Mono.fromCallable(() -> {
                    var scan = contrast.scan(text, width, flagMax);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("width", scan.width());
                    body.put("scanned", scan.scanned());
                    body.put("stutterCount", scan.stutterCount());
                    body.put("modalDistance", scan.modalDistance());
                    body.put("meanDistance", scan.meanDistance());
                    body.put("flagMax", scan.flagMax());
                    body.put("hits", scan.hits().stream()
                            .map(hit -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("index", hit.index());
                                row.put("offset", hit.offset());
                                row.put("distance", hit.distance());
                                row.put("left", hit.left());
                                row.put("right", hit.right());
                                return row;
                            })
                            .toList());
                            return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/mirror")
    public Mono<Map<String, Object>> findMirrors(
            @RequestParam("text") String text,
            @RequestParam(name = "width", defaultValue = "4") int width) {
        return Mono.fromCallable(() -> {
                    var scan = mirrors.scan(text, width);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("width", scan.width());
                    body.put("scanned", scan.scanned());
                    body.put("jointCount", scan.jointCount());
                    body.put("hits", scan.hits().stream()
                            .map(hit -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("index", hit.index());
                                row.put("offset", hit.offset());
                                row.put("identityDistance", hit.identityDistance());
                                row.put("left", hit.left());
                                row.put("right", hit.right());
                                return row;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/seam")
    public Mono<Map<String, Object>> flagSeams(
            @RequestParam("text") String text,
            @RequestParam(name = "wrap", defaultValue = "70") int wrapWidth,
            @RequestParam(name = "block", defaultValue = "4") int blockWidth) {
        return Mono.fromCallable(() -> {
                    var scan = seams.scan(text, wrapWidth, blockWidth);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("wrapWidth", scan.wrapWidth());
                    body.put("blockWidth", scan.blockWidth());
                    body.put("scanned", scan.scanned());
                    body.put("hitCount", scan.hitCount());
                    body.put("hits", scan.hits().stream()
                            .map(hit -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("index", hit.index());
                                row.put("line", hit.line());
                                row.put("identityDistance", hit.identityDistance());
                                row.put("left", hit.left());
                                row.put("right", hit.right());
                                return row;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/phase")
    public Mono<Map<String, Object>> phaseMirrors(
            @RequestParam("text") String text,
            @RequestParam(name = "wrap", defaultValue = "70") int wrapWidth,
            @RequestParam(name = "phase", defaultValue = "19") int phaseColumn,
            @RequestParam(name = "block", defaultValue = "4") int blockWidth) {
        return Mono.fromCallable(() -> {
                    var scan = phase.scan(text, wrapWidth, phaseColumn, blockWidth);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("wrapWidth", scan.wrapWidth());
                    body.put("phase", scan.phase());
                    body.put("blockWidth", scan.blockWidth());
                    body.put("scanned", scan.scanned());
                    body.put("hitCount", scan.hitCount());
                    body.put("hits", scan.hits().stream()
                            .map(hit -> {
                                Map<String, Object> row = new LinkedHashMap<>();
                                row.put("index", hit.index());
                                row.put("offset", hit.offset());
                                row.put("identityDistance", hit.identityDistance());
                                row.put("left", hit.left());
                                row.put("right", hit.right());
                                return row;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/fields")
    public Mono<Map<String, Object>> extractFields(
            @RequestParam("text") String text,
            @RequestParam(name = "wrap", defaultValue = "70") int wrapWidth) {
        return Mono.fromCallable(() -> {
                    var scan = fields.extract(text, wrapWidth);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("wrapWidth", scan.wrapWidth());
                    body.put("recordCount", scan.recordCount());
                    body.put("topRc", scan.topRc());
                    body.put("topReverse", scan.topReverse());
                    body.put("topIdentity", scan.topIdentity());
                    body.put("records", scan.records().stream()
                            .map(row -> {
                                Map<String, Object> rec = new LinkedHashMap<>();
                                rec.put("index", row.index());
                                rec.put("offset", row.offset());
                                rec.put("rc", row.rc());
                                rec.put("reverse", row.reverse());
                                rec.put("identity", row.identity());
                                return rec;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/clones")
    public Mono<Map<String, Object>> findClones(
            @RequestParam("text") String text,
            @RequestParam(name = "wrap", defaultValue = "70") int wrapWidth) {
        return Mono.fromCallable(() -> {
                    var scan = clones.scan(text, wrapWidth);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("wrapWidth", scan.wrapWidth());
                    body.put("scanned", scan.scanned());
                    body.put("distinct", scan.distinct());
                    body.put("cloneGroups", scan.cloneGroups());
                    body.put("cloneFrames", scan.cloneFrames());
                    body.put("topCount", scan.topCount());
                    body.put("hits", scan.hits().stream()
                            .map(hit -> {
                                Map<String, Object> rec = new LinkedHashMap<>();
                                rec.put("firstOffset", hit.firstOffset());
                                rec.put("count", hit.count());
                                rec.put("preview", hit.preview());
                                return rec;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/prefix")
    public Mono<Map<String, Object>> groupPrefixes(
            @RequestParam("text") String text,
            @RequestParam(name = "wrap", defaultValue = "70") int wrapWidth,
            @RequestParam(name = "prefix", defaultValue = "8") int prefixLength) {
        return Mono.fromCallable(() -> {
                    var scan = prefixes.group(text, wrapWidth, prefixLength);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("wrapWidth", scan.wrapWidth());
                    body.put("prefixLength", scan.prefixLength());
                    body.put("scanned", scan.scanned());
                    body.put("distinct", scan.distinct());
                    body.put("familyCount", scan.familyCount());
                    body.put("familyFrames", scan.familyFrames());
                    body.put("topCount", scan.topCount());
                    body.put("topPrefix", scan.topPrefix());
                    body.put("hits", scan.hits().stream()
                            .map(hit -> {
                                Map<String, Object> rec = new LinkedHashMap<>();
                                rec.put("firstOffset", hit.firstOffset());
                                rec.put("count", hit.count());
                                rec.put("prefix", hit.prefix());
                                return rec;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/keys")
    public Mono<Map<String, Object>> measureKeys(
            @RequestParam("text") String text,
            @RequestParam(name = "wrap", defaultValue = "70") int wrapWidth) {
        return Mono.fromCallable(() -> {
                    var scan = keys.measure(text, wrapWidth);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("wrapWidth", scan.wrapWidth());
                    body.put("floorLength", scan.floorLength());
                    body.put("scanned", scan.scanned());
                    body.put("uniqueAt", scan.uniqueAt());
                    body.put("uniqueShareAtFloor", scan.uniqueShareAtFloor());
                    body.put("uniqueShareAtWrap", scan.uniqueShareAtWrap());
                    body.put("samples", scan.samples().stream()
                            .map(hit -> {
                                Map<String, Object> rec = new LinkedHashMap<>();
                                rec.put("length", hit.length());
                                rec.put("distinct", hit.distinct());
                                rec.put("familyCount", hit.familyCount());
                                rec.put("uniqueShare", hit.uniqueShare());
                                return rec;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/forks")
    public Mono<Map<String, Object>> findForks(
            @RequestParam("text") String text,
            @RequestParam(name = "wrap", defaultValue = "70") int wrapWidth,
            @RequestParam(name = "prefix", defaultValue = "16") int prefixLength) {
        return Mono.fromCallable(() -> {
                    var scan = forks.scan(text, wrapWidth, prefixLength);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("wrapWidth", scan.wrapWidth());
                    body.put("prefixLength", scan.prefixLength());
                    body.put("scanned", scan.scanned());
                    body.put("distinct", scan.distinct());
                    body.put("twinCount", scan.twinCount());
                    body.put("twinFrames", scan.twinFrames());
                    body.put("uniqueShare", scan.uniqueShare());
                    body.put("topFork", scan.topFork());
                    body.put("hits", scan.hits().stream()
                            .map(hit -> {
                                Map<String, Object> rec = new LinkedHashMap<>();
                                rec.put("firstOffset", hit.firstOffset());
                                rec.put("count", hit.count());
                                rec.put("forkAt", hit.forkAt());
                                rec.put("prefix", hit.prefix());
                                return rec;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/affix")
    public Mono<Map<String, Object>> compareAffixes(
            @RequestParam("text") String text,
            @RequestParam(name = "wrap", defaultValue = "70") int wrapWidth) {
        return Mono.fromCallable(() -> {
                    var scan = affixes.measure(text, wrapWidth);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("wrapWidth", scan.wrapWidth());
                    body.put("scanned", scan.scanned());
                    body.put("leadUniqueAt", scan.leadUniqueAt());
                    body.put("tailUniqueAt", scan.tailUniqueAt());
                    body.put("cheaperEnd", scan.cheaperEnd());
                    body.put("leadShareAtFloor", scan.leadShareAtFloor());
                    body.put("tailShareAtFloor", scan.tailShareAtFloor());
                    body.put("leadShareAtNear", scan.leadShareAtNear());
                    body.put("tailShareAtNear", scan.tailShareAtNear());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/lanes")
    public Mono<Map<String, Object>> profileLanes(
            @RequestParam("text") String text,
            @RequestParam(name = "wrap", defaultValue = "70") int wrapWidth) {
        return Mono.fromCallable(() -> {
                    var scan = lanes.profile(text, wrapWidth);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("wrapWidth", scan.wrapWidth());
                    body.put("tileLength", scan.tileLength());
                    body.put("scanned", scan.scanned());
                    body.put("troughAt", scan.troughAt());
                    body.put("peakAt", scan.peakAt());
                    body.put("troughShare", scan.troughShare());
                    body.put("peakShare", scan.peakShare());
                    body.put("spread", scan.spread());
                    body.put("lanes", scan.lanes().stream()
                            .map(hit -> {
                                Map<String, Object> rec = new LinkedHashMap<>();
                                rec.put("start", hit.start());
                                rec.put("distinct", hit.distinct());
                                rec.put("uniqueShare", hit.uniqueShare());
                                return rec;
                            })
                            .toList());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/rows")
    public Mono<Map<String, Object>> profileRows(
            @RequestParam("text") String text) {
        return Mono.fromCallable(() -> {
                    var scan = rows.profile(text);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("lineCount", scan.lineCount());
                    body.put("scanned", scan.scanned());
                    body.put("tileLength", scan.tileLength());
                    body.put("uniqueAt", scan.uniqueAt());
                    body.put("uniqueShareAt16", scan.uniqueShareAt16());
                    body.put("twinCount", scan.twinCount());
                    body.put("twinLines", scan.twinLines());
                    body.put("topPrefix", scan.topPrefix());
                    body.put("topCount", scan.topCount());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/cliff")
    public Mono<Map<String, Object>> profileCliffs(
            @RequestParam("text") String text) {
        return Mono.fromCallable(() -> {
                    var scan = cliffs.profile(text);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("lineCount", scan.lineCount());
                    body.put("scanned", scan.scanned());
                    body.put("tileLength", scan.tileLength());
                    body.put("forkAt", scan.forkAt());
                    body.put("cliffAt", scan.cliffAt());
                    body.put("shareAt16", scan.shareAt16());
                    body.put("shareAtCliff", scan.shareAtCliff());
                    body.put("topPrefix", scan.topPrefix());
                    body.put("topCount", scan.topCount());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/runway")
    public Mono<Map<String, Object>> profileRunways(
            @RequestParam("text") String text) {
        return Mono.fromCallable(() -> {
                    var scan = runways.profile(text);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("lineCount", scan.lineCount());
                    body.put("scanned", scan.scanned());
                    body.put("tileLength", scan.tileLength());
                    body.put("forkAt", scan.forkAt());
                    body.put("cliffAt", scan.cliffAt());
                    body.put("runway", scan.runway());
                    body.put("stretched", scan.stretched());
                    body.put("shareAt16", scan.shareAt16());
                    body.put("shareAtCliff", scan.shareAtCliff());
                    body.put("topPrefix", scan.topPrefix());
                    body.put("topCount", scan.topCount());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/rise")
    public Mono<Map<String, Object>> profileRises(
            @RequestParam("text") String text) {
        return Mono.fromCallable(() -> {
                    var scan = rises.profile(text);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("lineCount", scan.lineCount());
                    body.put("scanned", scan.scanned());
                    body.put("floorLength", scan.floorLength());
                    body.put("riseAt", scan.riseAt());
                    body.put("gain", scan.gain());
                    body.put("shareAtRise", scan.shareAtRise());
                    body.put("shareAt16", scan.shareAt16());
                    body.put("cliffAt", scan.cliffAt());
                    body.put("pastClock", scan.pastClock());
                    body.put("topPrefix", scan.topPrefix());
                    body.put("topCount", scan.topCount());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/majority")
    public Mono<Map<String, Object>> profileMajorities(
            @RequestParam("text") String text) {
        return Mono.fromCallable(() -> {
                    var scan = majorities.profile(text);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("lineCount", scan.lineCount());
                    body.put("scanned", scan.scanned());
                    body.put("floorLength", scan.floorLength());
                    body.put("threshold", scan.threshold());
                    body.put("majorityAt", scan.majorityAt());
                    body.put("shareAtMajority", scan.shareAtMajority());
                    body.put("riseAt", scan.riseAt());
                    body.put("shareAtRise", scan.shareAtRise());
                    body.put("lag", scan.lag());
                    body.put("pastRise", scan.pastRise());
                    body.put("topPrefix", scan.topPrefix());
                    body.put("topCount", scan.topCount());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/near")
    public Mono<Map<String, Object>> profileNears(
            @RequestParam("text") String text) {
        return Mono.fromCallable(() -> {
                    var scan = nears.profile(text);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("lineCount", scan.lineCount());
                    body.put("scanned", scan.scanned());
                    body.put("floorLength", scan.floorLength());
                    body.put("threshold", scan.threshold());
                    body.put("nearAt", scan.nearAt());
                    body.put("shareAtNear", scan.shareAtNear());
                    body.put("majorityAt", scan.majorityAt());
                    body.put("shareAtMajority", scan.shareAtMajority());
                    body.put("lag", scan.lag());
                    body.put("pastMajority", scan.pastMajority());
                    body.put("topPrefix", scan.topPrefix());
                    body.put("topCount", scan.topCount());
                    return body;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping(value = "/logs/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<LogAnalysisReport> analyzeLogs(
            @RequestParam(name = "text", defaultValue = "") String text) {
        return Mono.fromCallable(() -> logAnalysis.analyze(text))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
