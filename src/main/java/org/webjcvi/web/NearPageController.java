package org.webjcvi.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.near.NearException;
import org.webjcvi.near.NearScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class NearPageController {

    private final NearScan nears;

    public NearPageController(NearScan nears) {
        this.nears = nears;
    }

    @GetMapping("/near")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/near", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> profile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new NearException("Text is missing");
                    }
                    var result = nears.profile(text);
                    return view(true, result.summary(), result.lineCount(), result.scanned(),
                            result.nearAt(), result.shareAtNear(), result.majorityAt(),
                            result.shareAtMajority(), result.lag(), result.pastMajority(),
                            result.topPrefix(), result.topCount());
                });
    }

    private static Rendering emptyView() {
        return view(false, "", 0, 0, 0, 0.0, 0, 0.0, 0, false, "", 0);
    }

    private static Rendering view(
            boolean scanned,
            String summary,
            int lineCount,
            int rowCount,
            int nearAt,
            double shareAtNear,
            int majorityAt,
            double shareAtMajority,
            int lag,
            boolean pastMajority,
            String topPrefix,
            int topCount) {
        return Rendering.view("near")
                .modelAttribute("scanned", scanned)
                .modelAttribute("maxChars", NearScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("lineCount", lineCount)
                .modelAttribute("rowCount", rowCount)
                .modelAttribute("nearAt", nearAt)
                .modelAttribute("shareAtNear", shareAtNear)
                .modelAttribute("majorityAt", majorityAt)
                .modelAttribute("shareAtMajority", shareAtMajority)
                .modelAttribute("lag", lag)
                .modelAttribute("pastMajority", pastMajority)
                .modelAttribute("topPrefix", topPrefix)
                .modelAttribute("topCount", topCount)
                .build();
    }
}
