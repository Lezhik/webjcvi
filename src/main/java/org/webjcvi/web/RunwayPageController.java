package org.webjcvi.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.runway.RunwayException;
import org.webjcvi.runway.RunwayScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class RunwayPageController {

    private final RunwayScan runways;

    public RunwayPageController(RunwayScan runways) {
        this.runways = runways;
    }

    @GetMapping("/runway")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/runway", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> profile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new RunwayException("Text is missing");
                    }
                    var result = runways.profile(text);
                    return view(true, result.summary(), result.lineCount(), result.scanned(),
                            result.forkAt(), result.cliffAt(), result.runway(), result.stretched(),
                            result.shareAt16(), result.shareAtCliff(), result.topPrefix(), result.topCount());
                });
    }

    private static Rendering emptyView() {
        return view(false, "", 0, 0, 0, 0, 0, false, 0.0, 0.0, "", 0);
    }

    private static Rendering view(
            boolean scanned,
            String summary,
            int lineCount,
            int rowCount,
            int forkAt,
            int cliffAt,
            int runway,
            boolean stretched,
            double shareAt16,
            double shareAtCliff,
            String topPrefix,
            int topCount) {
        return Rendering.view("runway")
                .modelAttribute("scanned", scanned)
                .modelAttribute("maxChars", RunwayScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("lineCount", lineCount)
                .modelAttribute("rowCount", rowCount)
                .modelAttribute("forkAt", forkAt)
                .modelAttribute("cliffAt", cliffAt)
                .modelAttribute("runway", runway)
                .modelAttribute("stretched", stretched)
                .modelAttribute("shareAt16", shareAt16)
                .modelAttribute("shareAtCliff", shareAtCliff)
                .modelAttribute("topPrefix", topPrefix)
                .modelAttribute("topCount", topCount)
                .build();
    }
}
