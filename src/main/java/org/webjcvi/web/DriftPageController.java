package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.drift.DriftException;
import org.webjcvi.drift.PairDrift;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class DriftPageController {

    private final PairDrift drift;

    public DriftPageController(PairDrift drift) {
        this.drift = drift;
    }

    @GetMapping("/drift")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/drift", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> scan(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new DriftException("Text is missing");
                    }
                    int window = parseWindow(form.getFirst("window"));
                    var scan = drift.scan(text, window, PairDrift.DEFAULT_THRESHOLD);
                    return Rendering.view("drift")
                            .modelAttribute("scanned", true)
                            .modelAttribute("window", scan.window())
                            .modelAttribute("maxChars", PairDrift.MAX_CHARS)
                            .modelAttribute("summary", scan.summary())
                            .modelAttribute("hotspotCount", scan.hotspotCount())
                            .modelAttribute("opens", scan.opens())
                            .modelAttribute("closes", scan.closes())
                            .modelAttribute("globalSkew", String.format("%.4f", scan.globalSkew()))
                            .modelAttribute("hotspots", scan.hotspots())
                            .build();
                });
    }

    private static Rendering emptyView() {
        return Rendering.view("drift")
                .modelAttribute("scanned", false)
                .modelAttribute("window", PairDrift.DEFAULT_WINDOW)
                .modelAttribute("maxChars", PairDrift.MAX_CHARS)
                .modelAttribute("summary", "")
                .modelAttribute("hotspotCount", 0)
                .modelAttribute("opens", 0)
                .modelAttribute("closes", 0)
                .modelAttribute("globalSkew", "0.0000")
                .modelAttribute("hotspots", List.of())
                .build();
    }

    private static int parseWindow(String raw) {
        if (raw == null || raw.isBlank()) {
            return PairDrift.DEFAULT_WINDOW;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new DriftException("window must be an integer");
        }
    }
}
