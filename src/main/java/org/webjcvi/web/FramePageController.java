package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.frame.FrameException;
import org.webjcvi.frame.FrameFields;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class FramePageController {

    private final FrameFields fields;

    public FramePageController(FrameFields fields) {
        this.fields = fields;
    }

    @GetMapping("/fields")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/fields", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> extract(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new FrameException("Text is missing");
                    }
                    int wrap = parseInt(form.getFirst("wrap"), FrameFields.DEFAULT_WRAP, "wrap");
                    var result = fields.extract(text, wrap);
                    return view(true, wrap, result.summary(), result.recordCount(),
                            result.topRc(), result.topReverse(), result.topIdentity(), result.records());
                });
    }

    private static Rendering emptyView() {
        return view(false, FrameFields.DEFAULT_WRAP, "", 0, "", "", "", List.of());
    }

    private static Rendering view(
            boolean scanned,
            int wrapWidth,
            String summary,
            int recordCount,
            String topRc,
            String topReverse,
            String topIdentity,
            List<?> records) {
        return Rendering.view("fields")
                .modelAttribute("scanned", scanned)
                .modelAttribute("wrapWidth", wrapWidth)
                .modelAttribute("maxChars", FrameFields.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("recordCount", recordCount)
                .modelAttribute("topRc", topRc)
                .modelAttribute("topReverse", topReverse)
                .modelAttribute("topIdentity", topIdentity)
                .modelAttribute("records", records)
                .build();
    }

    private static int parseInt(String raw, int fallback, String name) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new FrameException(name + " must be an integer");
        }
    }
}
