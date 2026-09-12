package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.key.KeyException;
import org.webjcvi.key.KeyWidth;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class KeyPageController {

    private final KeyWidth keys;

    public KeyPageController(KeyWidth keys) {
        this.keys = keys;
    }

    @GetMapping("/keys")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/keys", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> measure(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new KeyException("Text is missing");
                    }
                    int wrap = parseInt(form.getFirst("wrap"), KeyWidth.DEFAULT_WRAP);
                    var result = keys.measure(text, wrap);
                    return view(true, wrap, result.summary(), result.scanned(), result.uniqueAt(),
                            result.uniqueShareAtFloor(), result.uniqueShareAtWrap(), result.samples());
                });
    }

    private static Rendering emptyView() {
        return view(false, KeyWidth.DEFAULT_WRAP, "", 0, 0, 0.0, 0.0, List.of());
    }

    private static Rendering view(
            boolean scanned,
            int wrapWidth,
            String summary,
            int frameCount,
            int uniqueAt,
            double uniqueShareAtFloor,
            double uniqueShareAtWrap,
            List<?> samples) {
        return Rendering.view("keys")
                .modelAttribute("scanned", scanned)
                .modelAttribute("wrapWidth", wrapWidth)
                .modelAttribute("maxChars", KeyWidth.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("frameCount", frameCount)
                .modelAttribute("uniqueAt", uniqueAt)
                .modelAttribute("uniqueShareAtFloor", uniqueShareAtFloor)
                .modelAttribute("uniqueShareAtWrap", uniqueShareAtWrap)
                .modelAttribute("samples", samples)
                .build();
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new KeyException("wrap must be an integer");
        }
    }
}
