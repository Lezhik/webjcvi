package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.seam.SeamException;
import org.webjcvi.seam.SeamGuard;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class SeamPageController {

    private final SeamGuard seams;

    public SeamPageController(SeamGuard seams) {
        this.seams = seams;
    }

    @GetMapping("/seam")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/seam", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> scan(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new SeamException("Text is missing");
                    }
                    int wrap = parseInt(form.getFirst("wrap"), SeamGuard.DEFAULT_WRAP, "wrap");
                    int block = parseInt(form.getFirst("block"), SeamGuard.DEFAULT_BLOCK, "block");
                    var result = seams.scan(text, wrap, block);
                    return view(true, wrap, block, result.summary(), result.hitCount(), result.hits());
                });
    }

    private static Rendering emptyView() {
        return view(false, SeamGuard.DEFAULT_WRAP, SeamGuard.DEFAULT_BLOCK, "", 0, List.of());
    }

    private static Rendering view(
            boolean scanned,
            int wrapWidth,
            int blockWidth,
            String summary,
            int hitCount,
            List<?> hits) {
        return Rendering.view("seam")
                .modelAttribute("scanned", scanned)
                .modelAttribute("wrapWidth", wrapWidth)
                .modelAttribute("blockWidth", blockWidth)
                .modelAttribute("maxChars", SeamGuard.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("hitCount", hitCount)
                .modelAttribute("hits", hits)
                .build();
    }

    private static int parseInt(String raw, int fallback, String name) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new SeamException(name + " must be an integer");
        }
    }
}
