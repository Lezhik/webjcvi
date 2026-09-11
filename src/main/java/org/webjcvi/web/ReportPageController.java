package org.webjcvi.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.webjcvi.report.DnaReport;
import org.webjcvi.report.DnaReportService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class ReportPageController {

    private final DnaReportService reports;

    public ReportPageController(DnaReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/")
    public Mono<Rendering> home() {
        return Mono.fromCallable(reports::loadCurrent)
                .subscribeOn(Schedulers.boundedElastic())
                .map(current -> {
                    DnaReport report = current.orElse(null);
                    return Rendering.view("index")
                            .modelAttribute("hasReport", report != null)
                            .modelAttribute("length", report == null ? 0 : report.sequence().length())
                            .modelAttribute("gcPercent", report == null ? "n/a" : String.format("%.2f", report.sequence().gcPercent()))
                            .modelAttribute("ambiguous", report == null ? 0 : report.sequence().ambiguousTotal())
                            .modelAttribute("invalid", report == null ? 0 : report.sequence().invalidTotal())
                            .build();
                });
    }

    @GetMapping("/report")
    public Mono<Rendering> report() {
        return Mono.fromCallable(reports::loadCurrent)
                .subscribeOn(Schedulers.boundedElastic())
                .map(current -> Rendering.view("report")
                        .modelAttribute("hasReport", current.isPresent())
                        .modelAttribute("markdown", current.map(DnaReport::markdown).orElse(""))
                        .build());
    }

    @PostMapping(path = "/report/regenerate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> regenerate() {
        return Mono.fromCallable(reports::regenerate)
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(Rendering.redirectTo("/report").build());
    }
}
