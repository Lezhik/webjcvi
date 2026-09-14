package org.webjcvi.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.dup.DupException;
import org.webjcvi.dup.DupScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class DupPageController {

    private final DupScan dups;

    public DupPageController(DupScan dups) {
        this.dups = dups;
    }

    @GetMapping("/dups")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/dups", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> profile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new DupException("Text is missing");
                    }
                    var result = dups.profile(text);
                    return view(true, result.summary(), result.lineCount(), result.scanned(),
                            result.nearAt(), result.shareAtNear(), result.residueShare(),
                            result.twinGroups(), result.twinLines(), result.copyGroups(),
                            result.copyLines(), result.forkGroups(), result.forkLines(),
                            result.copyShare(), result.mostlyCopies(), result.minFork(),
                            result.topPrefix(), result.topCount());
                });
    }

    private static Rendering emptyView() {
        return view(false, "", 0, 0, 0, 0.0, 0.0, 0, 0, 0, 0, 0, 0, 0.0, false, 0, "", 0);
    }

    private static Rendering view(
            boolean scanned,
            String summary,
            int lineCount,
            int rowCount,
            int nearAt,
            double shareAtNear,
            double residueShare,
            int twinGroups,
            int twinLines,
            int copyGroups,
            int copyLines,
            int forkGroups,
            int forkLines,
            double copyShare,
            boolean mostlyCopies,
            int minFork,
            String topPrefix,
            int topCount) {
        return Rendering.view("dups")
                .modelAttribute("scanned", scanned)
                .modelAttribute("maxChars", DupScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("lineCount", lineCount)
                .modelAttribute("rowCount", rowCount)
                .modelAttribute("nearAt", nearAt)
                .modelAttribute("shareAtNear", shareAtNear)
                .modelAttribute("residueShare", residueShare)
                .modelAttribute("twinGroups", twinGroups)
                .modelAttribute("twinLines", twinLines)
                .modelAttribute("copyGroups", copyGroups)
                .modelAttribute("copyLines", copyLines)
                .modelAttribute("forkGroups", forkGroups)
                .modelAttribute("forkLines", forkLines)
                .modelAttribute("copyShare", copyShare)
                .modelAttribute("mostlyCopies", mostlyCopies)
                .modelAttribute("minFork", minFork)
                .modelAttribute("topPrefix", topPrefix)
                .modelAttribute("topCount", topCount)
                .build();
    }
}
