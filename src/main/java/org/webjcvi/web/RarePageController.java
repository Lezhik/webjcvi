package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.rare.RareClassScanner;
import org.webjcvi.rare.RareException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class RarePageController {

    private final RareClassScanner scanner;

    public RarePageController(RareClassScanner scanner) {
        this.scanner = scanner;
    }

    @GetMapping("/rare")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/rare", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> scan(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new RareException("Text is missing");
                    }
                    int minIsland = parseMin(form.getFirst("min"));
                    var result = scanner.scan(text, minIsland);
                    return view(true, minIsland, result.rareClass(), result.summary(),
                            result.islandCount(), result.islands());
                });
    }

    private static Rendering emptyView() {
        return view(false, RareClassScanner.DEFAULT_MIN_ISLAND, "", "", 0, List.of());
    }

    private static Rendering view(
            boolean scanned,
            int minIsland,
            String rareClass,
            String summary,
            int islandCount,
            List<?> islands) {
        return Rendering.view("rare")
                .modelAttribute("scanned", scanned)
                .modelAttribute("minIsland", minIsland)
                .modelAttribute("maxChars", RareClassScanner.MAX_CHARS)
                .modelAttribute("rareClass", rareClass)
                .modelAttribute("summary", summary)
                .modelAttribute("islandCount", islandCount)
                .modelAttribute("islands", islands)
                .build();
    }

    private static int parseMin(String raw) {
        if (raw == null || raw.isBlank()) {
            return RareClassScanner.DEFAULT_MIN_ISLAND;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new RareException("min must be an integer");
        }
    }
}
