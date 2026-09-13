package org.webjcvi.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.rise.RiseException;
import org.webjcvi.rise.RiseScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class RisePageController {

    private final RiseScan rises;

    public RisePageController(RiseScan rises) {
        this.rises = rises;
    }

    @GetMapping("/rise")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/rise", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> profile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new RiseException("Text is missing");
                    }
                    var result = rises.profile(text);
                    return view(true, result.summary(), result.lineCount(), result.scanned(),
                            result.riseAt(), result.gain(), result.shareAtRise(), result.shareAt16(),
                            result.cliffAt(), result.pastClock(), result.topPrefix(), result.topCount());
                });
    }

    private static Rendering emptyView() {
        return view(false, "", 0, 0, 0, 0.0, 0.0, 0.0, 0, false, "", 0);
    }

    private static Rendering view(
            boolean scanned,
            String summary,
            int lineCount,
            int rowCount,
            int riseAt,
            double gain,
            double shareAtRise,
            double shareAt16,
            int cliffAt,
            boolean pastClock,
            String topPrefix,
            int topCount) {
        return Rendering.view("rise")
                .modelAttribute("scanned", scanned)
                .modelAttribute("maxChars", RiseScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("lineCount", lineCount)
                .modelAttribute("rowCount", rowCount)
                .modelAttribute("riseAt", riseAt)
                .modelAttribute("gain", gain)
                .modelAttribute("shareAtRise", shareAtRise)
                .modelAttribute("shareAt16", shareAt16)
                .modelAttribute("cliffAt", cliffAt)
                .modelAttribute("pastClock", pastClock)
                .modelAttribute("topPrefix", topPrefix)
                .modelAttribute("topCount", topCount)
                .build();
    }
}
