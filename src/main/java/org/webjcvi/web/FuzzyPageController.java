package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.fuzzy.FuzzyException;
import org.webjcvi.fuzzy.FuzzyFind;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class FuzzyPageController {

    private final FuzzyFind fuzzy;

    public FuzzyPageController(FuzzyFind fuzzy) {
        this.fuzzy = fuzzy;
    }

    @GetMapping("/fuzzy")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/fuzzy", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> search(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    String motif = form.getFirst("motif");
                    if (text == null) {
                        throw new FuzzyException("Text is missing");
                    }
                    if (motif == null) {
                        throw new FuzzyException("Motif is missing");
                    }
                    int dist = parseDist(form.getFirst("dist"));
                    var result = fuzzy.search(text, motif, dist);
                    return view(true, dist, motif, result.summary(), result.hitCount(), result.hits());
                });
    }

    private static Rendering emptyView() {
        return view(false, FuzzyFind.DEFAULT_MAX_DIST, "", "", 0, List.of());
    }

    private static Rendering view(
            boolean searched,
            int maxDist,
            String motif,
            String summary,
            int hitCount,
            List<?> hits) {
        return Rendering.view("fuzzy")
                .modelAttribute("searched", searched)
                .modelAttribute("maxDist", maxDist)
                .modelAttribute("motif", motif)
                .modelAttribute("maxChars", FuzzyFind.MAX_CHARS)
                .modelAttribute("minMotif", FuzzyFind.MIN_MOTIF)
                .modelAttribute("summary", summary)
                .modelAttribute("hitCount", hitCount)
                .modelAttribute("hits", hits)
                .build();
    }

    private static int parseDist(String raw) {
        if (raw == null || raw.isBlank()) {
            return FuzzyFind.DEFAULT_MAX_DIST;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new FuzzyException("dist must be an integer");
        }
    }
}
