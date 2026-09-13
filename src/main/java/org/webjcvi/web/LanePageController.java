package org.webjcvi.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.lane.LaneException;
import org.webjcvi.lane.LaneScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class LanePageController {

    private final LaneScan lanes;

    public LanePageController(LaneScan lanes) {
        this.lanes = lanes;
    }

    @GetMapping("/lanes")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/lanes", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> profile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new LaneException("Text is missing");
                    }
                    int wrap = parseInt(form.getFirst("wrap"), LaneScan.DEFAULT_WRAP);
                    var result = lanes.profile(text, wrap);
                    return view(true, wrap, result.summary(), result.scanned(), result.troughAt(),
                            result.peakAt(), result.troughShare(), result.peakShare(), result.spread(),
                            result.lanes());
                });
    }

    private static Rendering emptyView() {
        return view(false, LaneScan.DEFAULT_WRAP, "", 0, 0, 0, 0.0, 0.0, 0.0, java.util.List.of());
    }

    private static Rendering view(
            boolean scanned,
            int wrapWidth,
            String summary,
            int frameCount,
            int troughAt,
            int peakAt,
            double troughShare,
            double peakShare,
            double spread,
            java.util.List<LaneScan.Lane> hits) {
        return Rendering.view("lanes")
                .modelAttribute("scanned", scanned)
                .modelAttribute("wrapWidth", wrapWidth)
                .modelAttribute("maxChars", LaneScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("frameCount", frameCount)
                .modelAttribute("troughAt", troughAt)
                .modelAttribute("peakAt", peakAt)
                .modelAttribute("troughShare", troughShare)
                .modelAttribute("peakShare", peakShare)
                .modelAttribute("spread", spread)
                .modelAttribute("hits", hits)
                .build();
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new LaneException("wrap must be an integer");
        }
    }
}
