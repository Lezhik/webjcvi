package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.fork.ForkException;
import org.webjcvi.fork.ForkScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class ForkPageController {

    private final ForkScan forks;

    public ForkPageController(ForkScan forks) {
        this.forks = forks;
    }

    @GetMapping("/forks")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/forks", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> scan(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new ForkException("Text is missing");
                    }
                    int wrap = parseInt(form.getFirst("wrap"), ForkScan.DEFAULT_WRAP, "wrap");
                    int prefix = parseInt(form.getFirst("prefix"), ForkScan.DEFAULT_PREFIX, "prefix");
                    var result = forks.scan(text, wrap, prefix);
                    return view(true, wrap, prefix, result.summary(), result.scanned(), result.distinct(),
                            result.twinCount(), result.twinFrames(), result.uniqueShare(), result.topFork(),
                            result.hits());
                });
    }

    private static Rendering emptyView() {
        return view(false, ForkScan.DEFAULT_WRAP, ForkScan.DEFAULT_PREFIX, "", 0, 0, 0, 0, 0.0, 0, List.of());
    }

    private static Rendering view(
            boolean scanned,
            int wrapWidth,
            int prefixLength,
            String summary,
            int frameCount,
            int distinct,
            int twinCount,
            int twinFrames,
            double uniqueShare,
            int topFork,
            List<?> hits) {
        return Rendering.view("forks")
                .modelAttribute("scanned", scanned)
                .modelAttribute("wrapWidth", wrapWidth)
                .modelAttribute("prefixLength", prefixLength)
                .modelAttribute("maxChars", ForkScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("frameCount", frameCount)
                .modelAttribute("distinct", distinct)
                .modelAttribute("twinCount", twinCount)
                .modelAttribute("twinFrames", twinFrames)
                .modelAttribute("uniqueShare", uniqueShare)
                .modelAttribute("topFork", topFork)
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
            throw new ForkException(name + " must be an integer");
        }
    }
}
