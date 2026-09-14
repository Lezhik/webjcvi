package org.webjcvi.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.majority.MajorityException;
import org.webjcvi.majority.MajorityScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class MajorityPageController {

    private final MajorityScan majorities;

    public MajorityPageController(MajorityScan majorities) {
        this.majorities = majorities;
    }

    @GetMapping("/majority")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/majority", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> profile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new MajorityException("Text is missing");
                    }
                    var result = majorities.profile(text);
                    return view(true, result.summary(), result.lineCount(), result.scanned(),
                            result.majorityAt(), result.shareAtMajority(), result.riseAt(),
                            result.shareAtRise(), result.lag(), result.pastRise(),
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
            int majorityAt,
            double shareAtMajority,
            int riseAt,
            double shareAtRise,
            int lag,
            boolean pastRise,
            String topPrefix,
            int topCount) {
        return Rendering.view("majority")
                .modelAttribute("scanned", scanned)
                .modelAttribute("maxChars", MajorityScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("lineCount", lineCount)
                .modelAttribute("rowCount", rowCount)
                .modelAttribute("majorityAt", majorityAt)
                .modelAttribute("shareAtMajority", shareAtMajority)
                .modelAttribute("riseAt", riseAt)
                .modelAttribute("shareAtRise", shareAtRise)
                .modelAttribute("lag", lag)
                .modelAttribute("pastRise", pastRise)
                .modelAttribute("topPrefix", topPrefix)
                .modelAttribute("topCount", topCount)
                .build();
    }
}
