package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.stamp.KmerStamp;
import org.webjcvi.stamp.StampException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class StampPageController {

    private final KmerStamp stamp;

    public StampPageController(KmerStamp stamp) {
        this.stamp = stamp;
    }

    @GetMapping("/stamps")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/stamps", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> rank(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new StampException("Text is missing");
                    }
                    int k = parseK(form.getFirst("k"));
                    var census = stamp.rank(text, k);
                    return view(true, census.k(), census.summary(), census.topKmer(),
                            census.topCount(), census.stamps());
                });
    }

    private static Rendering emptyView() {
        return view(false, KmerStamp.DEFAULT_K, "", "", 0L, List.of());
    }

    private static Rendering view(
            boolean ranked,
            int k,
            String summary,
            String topKmer,
            long topCount,
            List<?> stamps) {
        return Rendering.view("stamps")
                .modelAttribute("ranked", ranked)
                .modelAttribute("k", k)
                .modelAttribute("maxChars", KmerStamp.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("topKmer", topKmer)
                .modelAttribute("topCount", topCount)
                .modelAttribute("stamps", stamps)
                .build();
    }

    private static int parseK(String raw) {
        if (raw == null || raw.isBlank()) {
            return KmerStamp.DEFAULT_K;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new StampException("k must be an integer");
        }
    }
}
