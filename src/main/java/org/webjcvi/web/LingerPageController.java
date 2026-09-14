package org.webjcvi.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.linger.LingerException;
import org.webjcvi.linger.LingerScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class LingerPageController {

    private final LingerScan lingers;

    public LingerPageController(LingerScan lingers) {
        this.lingers = lingers;
    }

    @GetMapping("/linger")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/linger", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> profile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new LingerException("Text is missing");
                    }
                    var result = lingers.profile(text);
                    return view(true, result.summary(), result.lineCount(), result.scanned(),
                            result.nearAt(), result.twinGroups(), result.lag0(), result.lag1(),
                            result.lag2(), result.lagLong(), result.modalLag(), result.maxLag(),
                            result.longShare(), result.longTail(), result.copyShare(),
                            result.mostlyCopies());
                });
    }

    private static Rendering emptyView() {
        return view(false, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0, false, 0.0, false);
    }

    private static Rendering view(
            boolean scanned,
            String summary,
            int lineCount,
            int rowCount,
            int nearAt,
            int twinGroups,
            int lag0,
            int lag1,
            int lag2,
            int lagLong,
            int modalLag,
            int maxLag,
            double longShare,
            boolean longTail,
            double copyShare,
            boolean mostlyCopies) {
        return Rendering.view("linger")
                .modelAttribute("scanned", scanned)
                .modelAttribute("maxChars", LingerScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("lineCount", lineCount)
                .modelAttribute("rowCount", rowCount)
                .modelAttribute("nearAt", nearAt)
                .modelAttribute("twinGroups", twinGroups)
                .modelAttribute("lag0", lag0)
                .modelAttribute("lag1", lag1)
                .modelAttribute("lag2", lag2)
                .modelAttribute("lagLong", lagLong)
                .modelAttribute("modalLag", modalLag)
                .modelAttribute("maxLag", maxLag)
                .modelAttribute("longShare", longShare)
                .modelAttribute("longTail", longTail)
                .modelAttribute("copyShare", copyShare)
                .modelAttribute("mostlyCopies", mostlyCopies)
                .build();
    }
}
