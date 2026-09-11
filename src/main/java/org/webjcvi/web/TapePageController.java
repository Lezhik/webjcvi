package org.webjcvi.web;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.tape.ScratchTape;
import org.webjcvi.tape.TapeException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class TapePageController {

    private final ScratchTape tape;

    public TapePageController(ScratchTape tape) {
        this.tape = tape;
    }

    @GetMapping("/tape")
    public Mono<Rendering> page(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "minRun", defaultValue = "5") int minRun) {
        return Mono.fromCallable(() -> {
                    boolean searched = !query.isBlank();
                    List<ScratchTape.Hit> hits = searched ? tape.find(query) : List.of();
                    int runFloor = Math.max(2, minRun);
                    List<ScratchTape.Run> runs = tape.isEmpty() ? List.of() : tape.runs(runFloor, 30);
                    return Rendering.view("tape")
                            .modelAttribute("length", tape.length())
                            .modelAttribute("preview", tape.preview(240))
                            .modelAttribute("query", query)
                            .modelAttribute("searched", searched)
                            .modelAttribute("hits", hits)
                            .modelAttribute("runs", runs)
                            .modelAttribute("minRun", runFloor)
                            .modelAttribute("maxChars", ScratchTape.MAX_CHARS)
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping(path = "/tape/load", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> load(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new TapeException("Tape text is missing");
                    }
                    tape.load(text);
                    return Rendering.redirectTo("/tape").build();
                });
    }
}
