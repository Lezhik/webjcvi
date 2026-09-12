package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.contrast.BlockContrast;
import org.webjcvi.contrast.ContrastException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class ContrastPageController {

    private final BlockContrast contrast;

    public ContrastPageController(BlockContrast contrast) {
        this.contrast = contrast;
    }

    @GetMapping("/contrast")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/contrast", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> scan(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new ContrastException("Text is missing");
                    }
                    int width = parseInt(form.getFirst("width"), BlockContrast.DEFAULT_WIDTH, "width");
                    int flagMax = parseInt(form.getFirst("flag"), BlockContrast.DEFAULT_FLAG_MAX, "flag");
                    var result = contrast.scan(text, width, flagMax);
                    return view(true, width, flagMax, result.summary(), result.stutterCount(), result.hits());
                });
    }

    private static Rendering emptyView() {
        return view(false, BlockContrast.DEFAULT_WIDTH, BlockContrast.DEFAULT_FLAG_MAX, "", 0, List.of());
    }

    private static Rendering view(
            boolean scanned,
            int width,
            int flagMax,
            String summary,
            int stutterCount,
            List<?> hits) {
        return Rendering.view("contrast")
                .modelAttribute("scanned", scanned)
                .modelAttribute("width", width)
                .modelAttribute("flagMax", flagMax)
                .modelAttribute("maxChars", BlockContrast.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("stutterCount", stutterCount)
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
            throw new ContrastException(name + " must be an integer");
        }
    }
}
