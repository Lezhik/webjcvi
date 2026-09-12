package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.loop.LoopException;
import org.webjcvi.loop.StemLoop;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class LoopPageController {

    private final StemLoop loops;

    public LoopPageController(StemLoop loops) {
        this.loops = loops;
    }

    @GetMapping("/loop")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/loop", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> extract(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new LoopException("Text is missing");
                    }
                    int min = parseMin(form.getFirst("min"));
                    var result = loops.extract(text, min);
                    return view(true, min, result.summary(), result.spanCount(),
                            result.nested(), result.longestLoop(), result.spans());
                });
    }

    private static Rendering emptyView() {
        return view(false, StemLoop.DEFAULT_MIN_LOOP, "", 0, 0, 0, List.of());
    }

    private static Rendering view(
            boolean extracted,
            int minLoop,
            String summary,
            int spanCount,
            int nested,
            int longestLoop,
            List<?> spans) {
        return Rendering.view("loop")
                .modelAttribute("extracted", extracted)
                .modelAttribute("minLoop", minLoop)
                .modelAttribute("maxChars", StemLoop.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("spanCount", spanCount)
                .modelAttribute("nested", nested)
                .modelAttribute("longestLoop", longestLoop)
                .modelAttribute("spans", spans)
                .build();
    }

    private static int parseMin(String raw) {
        if (raw == null || raw.isBlank()) {
            return StemLoop.DEFAULT_MIN_LOOP;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new LoopException("min must be an integer");
        }
    }
}
