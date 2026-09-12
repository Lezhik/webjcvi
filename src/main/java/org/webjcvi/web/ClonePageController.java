package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.clone.CloneException;
import org.webjcvi.clone.CloneScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class ClonePageController {

    private final CloneScan clones;

    public ClonePageController(CloneScan clones) {
        this.clones = clones;
    }

    @GetMapping("/clones")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/clones", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> scan(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new CloneException("Text is missing");
                    }
                    int wrap = parseInt(form.getFirst("wrap"), CloneScan.DEFAULT_WRAP, "wrap");
                    var result = clones.scan(text, wrap);
                    return view(true, wrap, result.summary(), result.scanned(), result.distinct(),
                            result.cloneGroups(), result.cloneFrames(), result.topCount(), result.hits());
                });
    }

    private static Rendering emptyView() {
        return view(false, CloneScan.DEFAULT_WRAP, "", 0, 0, 0, 0, 0, List.of());
    }

    private static Rendering view(
            boolean scanned,
            int wrapWidth,
            String summary,
            int frameCount,
            int distinct,
            int cloneGroups,
            int cloneFrames,
            int topCount,
            List<?> hits) {
        return Rendering.view("clones")
                .modelAttribute("scanned", scanned)
                .modelAttribute("wrapWidth", wrapWidth)
                .modelAttribute("maxChars", CloneScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("frameCount", frameCount)
                .modelAttribute("distinct", distinct)
                .modelAttribute("cloneGroups", cloneGroups)
                .modelAttribute("cloneFrames", cloneFrames)
                .modelAttribute("topCount", topCount)
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
            throw new CloneException(name + " must be an integer");
        }
    }
}
