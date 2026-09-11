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
import org.webjcvi.report.DnaReport;
import org.webjcvi.report.DnaReportService;
import org.webjcvi.storage.FileStorageService;
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

    public WorkspaceApiController(FileStorageService storage, DnaReportService reports) {
        this.storage = storage;
        this.reports = reports;
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
}
