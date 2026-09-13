package org.webjcvi.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.row.RowException;
import org.webjcvi.row.RowScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class RowPageController {

    private final RowScan rows;

    public RowPageController(RowScan rows) {
        this.rows = rows;
    }

    @GetMapping("/rows")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/rows", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> profile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new RowException("Text is missing");
                    }
                    var result = rows.profile(text);
                    return view(true, result.summary(), result.lineCount(), result.scanned(),
                            result.uniqueAt(), result.uniqueShareAt16(), result.twinCount(),
                            result.twinLines(), result.topPrefix(), result.topCount());
                });
    }

    private static Rendering emptyView() {
        return view(false, "", 0, 0, 0, 0.0, 0, 0, "", 0);
    }

    private static Rendering view(
            boolean scanned,
            String summary,
            int lineCount,
            int rowCount,
            int uniqueAt,
            double uniqueShareAt16,
            int twinCount,
            int twinLines,
            String topPrefix,
            int topCount) {
        return Rendering.view("rows")
                .modelAttribute("scanned", scanned)
                .modelAttribute("maxChars", RowScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("lineCount", lineCount)
                .modelAttribute("rowCount", rowCount)
                .modelAttribute("uniqueAt", uniqueAt)
                .modelAttribute("uniqueShareAt16", uniqueShareAt16)
                .modelAttribute("twinCount", twinCount)
                .modelAttribute("twinLines", twinLines)
                .modelAttribute("topPrefix", topPrefix)
                .modelAttribute("topCount", topCount)
                .build();
    }
}
