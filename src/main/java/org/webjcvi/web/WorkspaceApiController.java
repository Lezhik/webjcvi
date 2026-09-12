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

    public WorkspaceApiController(
            FileStorageService storage,
            DnaReportService reports,
            ScratchTape tape,
            BannerSplitter splitter,
            PairDrift drift) {
        this.storage = storage;
        this.reports = reports;
        this.tape = tape;
        this.splitter = splitter;
        this.drift = drift;
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
}
