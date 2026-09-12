package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.phase.PhaseException;
import org.webjcvi.phase.PhaseJoint;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class PhasePageController {

    private final PhaseJoint joints;

    public PhasePageController(PhaseJoint joints) {
        this.joints = joints;
    }

    @GetMapping("/phase")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/phase", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> scan(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new PhaseException("Text is missing");
                    }
                    int wrap = parseInt(form.getFirst("wrap"), PhaseJoint.DEFAULT_WRAP, "wrap");
                    int phase = parseInt(form.getFirst("phase"), PhaseJoint.DEFAULT_PHASE, "phase");
                    int block = parseInt(form.getFirst("block"), PhaseJoint.DEFAULT_BLOCK, "block");
                    var result = joints.scan(text, wrap, phase, block);
                    return view(true, wrap, phase, block, result.summary(), result.hitCount(), result.hits());
                });
    }

    private static Rendering emptyView() {
        return view(false, PhaseJoint.DEFAULT_WRAP, PhaseJoint.DEFAULT_PHASE, PhaseJoint.DEFAULT_BLOCK,
                "", 0, List.of());
    }

    private static Rendering view(
            boolean scanned,
            int wrapWidth,
            int phase,
            int blockWidth,
            String summary,
            int hitCount,
            List<?> hits) {
        return Rendering.view("phase")
                .modelAttribute("scanned", scanned)
                .modelAttribute("wrapWidth", wrapWidth)
                .modelAttribute("phase", phase)
                .modelAttribute("blockWidth", blockWidth)
                .modelAttribute("maxChars", PhaseJoint.MAX_CHARS)
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
            throw new PhaseException(name + " must be an integer");
        }
    }
}
