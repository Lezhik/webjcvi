package org.webjcvi.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.affix.AffixException;
import org.webjcvi.affix.AffixScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class AffixPageController {

    private final AffixScan affixes;

    public AffixPageController(AffixScan affixes) {
        this.affixes = affixes;
    }

    @GetMapping("/affix")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/affix", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> measure(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new AffixException("Text is missing");
                    }
                    int wrap = parseInt(form.getFirst("wrap"), AffixScan.DEFAULT_WRAP);
                    var result = affixes.measure(text, wrap);
                    return view(true, wrap, result.summary(), result.scanned(), result.leadUniqueAt(),
                            result.tailUniqueAt(), result.cheaperEnd(), result.leadShareAtFloor(),
                            result.tailShareAtFloor(), result.leadShareAtNear(), result.tailShareAtNear());
                });
    }

    private static Rendering emptyView() {
        return view(false, AffixScan.DEFAULT_WRAP, "", 0, 0, 0, "", 0.0, 0.0, 0.0, 0.0);
    }

    private static Rendering view(
            boolean scanned,
            int wrapWidth,
            String summary,
            int frameCount,
            int leadUniqueAt,
            int tailUniqueAt,
            String cheaperEnd,
            double leadShareAtFloor,
            double tailShareAtFloor,
            double leadShareAtNear,
            double tailShareAtNear) {
        return Rendering.view("affix")
                .modelAttribute("scanned", scanned)
                .modelAttribute("wrapWidth", wrapWidth)
                .modelAttribute("maxChars", AffixScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("frameCount", frameCount)
                .modelAttribute("leadUniqueAt", leadUniqueAt)
                .modelAttribute("tailUniqueAt", tailUniqueAt)
                .modelAttribute("cheaperEnd", cheaperEnd)
                .modelAttribute("leadShareAtFloor", leadShareAtFloor)
                .modelAttribute("tailShareAtFloor", tailShareAtFloor)
                .modelAttribute("leadShareAtNear", leadShareAtNear)
                .modelAttribute("tailShareAtNear", tailShareAtNear)
                .build();
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new AffixException("wrap must be an integer");
        }
    }
}
