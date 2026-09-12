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

    @PostMapping(value = "/logs/analyze", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<LogAnalysisReport> analyzeLogs(
            @RequestParam(name = "text", defaultValue = "") String text) {
        return Mono.fromCallable(() -> logAnalysis.analyze(text))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
