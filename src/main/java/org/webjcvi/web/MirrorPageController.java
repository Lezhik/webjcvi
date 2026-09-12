package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.mirror.MirrorException;
import org.webjcvi.mirror.MirrorJoint;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class MirrorPageController {

    private final MirrorJoint mirrors;

    public MirrorPageController(MirrorJoint mirrors) {
        this.mirrors = mirrors;
    }

    @GetMapping("/mirror")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/mirror", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> scan(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new MirrorException("Text is missing");
                    }
                    int width = parseInt(form.getFirst("width"), MirrorJoint.DEFAULT_WIDTH, "width");
                    var result = mirrors.scan(text, width);
                    return view(true, width, result.summary(), result.jointCount(), result.hits());
                });
    }

    private static Rendering emptyView() {
        return view(false, MirrorJoint.DEFAULT_WIDTH, "", 0, List.of());
    }

    private static Rendering view(
            boolean scanned,
            int width,
            String summary,
            int jointCount,
            List<?> hits) {
        return Rendering.view("mirror")
                .modelAttribute("scanned", scanned)
                .modelAttribute("width", width)
                .modelAttribute("maxChars", MirrorJoint.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("jointCount", jointCount)
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
            throw new MirrorException(name + " must be an integer");
        }
    }
}
