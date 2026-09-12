package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.reflow.ReflowException;
import org.webjcvi.reflow.WrapReflow;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class ReflowPageController {

    private final WrapReflow reflow;

    public ReflowPageController(WrapReflow reflow) {
        this.reflow = reflow;
    }

    @GetMapping("/reflow")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/reflow", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> unwrap(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new ReflowException("Text is missing");
                    }
                    int width = parseWidth(form.getFirst("width"));
                    var result = reflow.unwrap(text, width);
                    return view(true, result.width(), result.stitches(), result.paragraphCount(),
                            result.sourceLines(), result.paragraphs());
                });
    }

    private static Rendering emptyView() {
        return view(false, WrapReflow.DEFAULT_WIDTH, 0, 0, 0, List.of());
    }

    private static Rendering view(
            boolean unwrapped,
            int width,
            int stitches,
            int paragraphCount,
            int sourceLines,
            List<?> paragraphs) {
        return Rendering.view("reflow")
                .modelAttribute("unwrapped", unwrapped)
                .modelAttribute("width", width)
                .modelAttribute("maxChars", WrapReflow.MAX_CHARS)
                .modelAttribute("stitches", stitches)
                .modelAttribute("paragraphCount", paragraphCount)
                .modelAttribute("sourceLines", sourceLines)
                .modelAttribute("paragraphs", paragraphs)
                .build();
    }

    private static int parseWidth(String raw) {
        if (raw == null || raw.isBlank()) {
            return WrapReflow.DEFAULT_WIDTH;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new ReflowException("width must be an integer");
        }
    }
}
