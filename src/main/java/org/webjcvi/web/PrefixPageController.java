package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.prefix.PrefixException;
import org.webjcvi.prefix.PrefixGroup;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class PrefixPageController {

    private final PrefixGroup prefixes;

    public PrefixPageController(PrefixGroup prefixes) {
        this.prefixes = prefixes;
    }

    @GetMapping("/prefix")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/prefix", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> group(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new PrefixException("Text is missing");
                    }
                    int wrap = parseInt(form.getFirst("wrap"), PrefixGroup.DEFAULT_WRAP, "wrap");
                    int prefix = parseInt(form.getFirst("prefix"), PrefixGroup.DEFAULT_PREFIX, "prefix");
                    var result = prefixes.group(text, wrap, prefix);
                    return view(true, wrap, prefix, result.summary(), result.scanned(), result.distinct(),
                            result.familyCount(), result.familyFrames(), result.topCount(), result.topPrefix(),
                            result.hits());
                });
    }

    private static Rendering emptyView() {
        return view(false, PrefixGroup.DEFAULT_WRAP, PrefixGroup.DEFAULT_PREFIX, "", 0, 0, 0, 0, 0, "", List.of());
    }

    private static Rendering view(
            boolean scanned,
            int wrapWidth,
            int prefixLength,
            String summary,
            int frameCount,
            int distinct,
            int familyCount,
            int familyFrames,
            int topCount,
            String topPrefix,
            List<?> hits) {
        return Rendering.view("prefix")
                .modelAttribute("scanned", scanned)
                .modelAttribute("wrapWidth", wrapWidth)
                .modelAttribute("prefixLength", prefixLength)
                .modelAttribute("maxChars", PrefixGroup.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("frameCount", frameCount)
                .modelAttribute("distinct", distinct)
                .modelAttribute("familyCount", familyCount)
                .modelAttribute("familyFrames", familyFrames)
                .modelAttribute("topCount", topCount)
                .modelAttribute("topPrefix", topPrefix)
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
            throw new PrefixException(name + " must be an integer");
        }
    }
}
