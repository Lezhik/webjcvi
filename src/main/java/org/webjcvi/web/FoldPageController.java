package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.fold.FoldException;
import org.webjcvi.fold.PalindromeScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class FoldPageController {

    private final PalindromeScan palindromes;

    public FoldPageController(PalindromeScan palindromes) {
        this.palindromes = palindromes;
    }

    @GetMapping("/fold")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/fold", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> find(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new FoldException("Text is missing");
                    }
                    int min = parseMin(form.getFirst("min"));
                    var result = palindromes.find(text, min);
                    return view(true, min, result.summary(), result.hitCount(),
                            result.longest(), result.hits());
                });
    }

    private static Rendering emptyView() {
        return view(false, PalindromeScan.DEFAULT_MIN, "", 0, 0, List.of());
    }

    private static Rendering view(
            boolean found,
            int minLength,
            String summary,
            int hitCount,
            int longest,
            List<?> hits) {
        return Rendering.view("fold")
                .modelAttribute("found", found)
                .modelAttribute("minLength", minLength)
                .modelAttribute("maxChars", PalindromeScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("hitCount", hitCount)
                .modelAttribute("longest", longest)
                .modelAttribute("hits", hits)
                .build();
    }

    private static int parseMin(String raw) {
        if (raw == null || raw.isBlank()) {
            return PalindromeScan.DEFAULT_MIN;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new FoldException("min must be an integer");
        }
    }
}
