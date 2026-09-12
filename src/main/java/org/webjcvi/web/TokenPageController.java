package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.token.RareBreakTokenizer;
import org.webjcvi.token.TokenException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class TokenPageController {

    private final RareBreakTokenizer tokenizer;

    public TokenPageController(RareBreakTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    @GetMapping("/tokens")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/tokens", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> cut(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new TokenException("Text is missing");
                    }
                    int minToken = parseMin(form.getFirst("min"));
                    var result = tokenizer.cut(text, minToken);
                    return view(true, minToken, result.rareClass(), result.summary(),
                            result.tokenCount(), result.tokens());
                });
    }

    private static Rendering emptyView() {
        return view(false, RareBreakTokenizer.DEFAULT_MIN_TOKEN, "", "", 0, List.of());
    }

    private static Rendering view(
            boolean cut,
            int minToken,
            String rareClass,
            String summary,
            int tokenCount,
            List<?> tokens) {
        return Rendering.view("tokens")
                .modelAttribute("cut", cut)
                .modelAttribute("minToken", minToken)
                .modelAttribute("maxChars", RareBreakTokenizer.MAX_CHARS)
                .modelAttribute("rareClass", rareClass)
                .modelAttribute("summary", summary)
                .modelAttribute("tokenCount", tokenCount)
                .modelAttribute("tokens", tokens)
                .build();
    }

    private static int parseMin(String raw) {
        if (raw == null || raw.isBlank()) {
            return RareBreakTokenizer.DEFAULT_MIN_TOKEN;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new TokenException("min must be an integer");
        }
    }
}
