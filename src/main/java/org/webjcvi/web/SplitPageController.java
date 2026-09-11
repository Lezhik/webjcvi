package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.segment.BannerSplitter;
import org.webjcvi.segment.SegmentException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class SplitPageController {

    private final BannerSplitter splitter;

    public SplitPageController(BannerSplitter splitter) {
        this.splitter = splitter;
    }

    @GetMapping("/split")
    public Mono<Rendering> page() {
        return Mono.just(emptyView(BannerSplitter.DEFAULT_MIN_RUN, false, List.of()));
    }

    @PostMapping(path = "/split", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> split(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new SegmentException("Text is missing");
                    }
                    int minRun = parseMinRun(form.getFirst("minRun"));
                    var sections = splitter.split(text, minRun);
                    return Rendering.view("split")
                            .modelAttribute("split", true)
                            .modelAttribute("minRun", minRun)
                            .modelAttribute("maxChars", BannerSplitter.MAX_CHARS)
                            .modelAttribute("sectionCount", sections.size())
                            .modelAttribute("sections", sections)
                            .build();
                });
    }

    private static Rendering emptyView(int minRun, boolean split, List<?> sections) {
        return Rendering.view("split")
                .modelAttribute("split", split)
                .modelAttribute("minRun", minRun)
                .modelAttribute("maxChars", BannerSplitter.MAX_CHARS)
                .modelAttribute("sectionCount", 0)
                .modelAttribute("sections", sections)
                .build();
    }

    private static int parseMinRun(String raw) {
        if (raw == null || raw.isBlank()) {
            return BannerSplitter.DEFAULT_MIN_RUN;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new SegmentException("minRun must be an integer");
        }
    }
}
