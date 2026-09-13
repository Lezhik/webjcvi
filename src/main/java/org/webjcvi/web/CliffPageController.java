package org.webjcvi.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.cliff.CliffException;
import org.webjcvi.cliff.CliffScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class CliffPageController {

    private final CliffScan cliffs;

    public CliffPageController(CliffScan cliffs) {
        this.cliffs = cliffs;
    }

    @GetMapping("/cliff")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/cliff", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> profile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new CliffException("Text is missing");
                    }
                    var result = cliffs.profile(text);
                    return view(true, result.summary(), result.lineCount(), result.scanned(),
                            result.forkAt(), result.cliffAt(), result.shareAt16(), result.shareAtCliff(),
                            result.topPrefix(), result.topCount());
                });
    }

    private static Rendering emptyView() {
        return view(false, "", 0, 0, 0, 0, 0.0, 0.0, "", 0);
    }

    private static Rendering view(
            boolean scanned,
            String summary,
            int lineCount,
            int rowCount,
            int forkAt,
            int cliffAt,
            double shareAt16,
            double shareAtCliff,
            String topPrefix,
            int topCount) {
        return Rendering.view("cliff")
                .modelAttribute("scanned", scanned)
                .modelAttribute("maxChars", CliffScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("lineCount", lineCount)
                .modelAttribute("rowCount", rowCount)
                .modelAttribute("forkAt", forkAt)
                .modelAttribute("cliffAt", cliffAt)
                .modelAttribute("shareAt16", shareAt16)
                .modelAttribute("shareAtCliff", shareAtCliff)
                .modelAttribute("topPrefix", topPrefix)
                .modelAttribute("topCount", topCount)
                .build();
    }
}
