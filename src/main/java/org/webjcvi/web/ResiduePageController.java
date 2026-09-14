package org.webjcvi.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.webjcvi.residue.ResidueException;
import org.webjcvi.residue.ResidueScan;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
public class ResiduePageController {

    private final ResidueScan residues;

    public ResiduePageController(ResidueScan residues) {
        this.residues = residues;
    }

    @GetMapping("/residue")
    public Mono<Rendering> page() {
        return Mono.just(emptyView());
    }

    @PostMapping(path = "/residue", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Rendering> profile(ServerWebExchange exchange) {
        return exchange.getFormData()
                .publishOn(Schedulers.boundedElastic())
                .map(form -> {
                    String text = form.getFirst("text");
                    if (text == null) {
                        throw new ResidueException("Text is missing");
                    }
                    var result = residues.profile(text);
                    return view(true, result.summary(), result.lineCount(), result.scanned(),
                            result.nearAt(), result.shareAtNear(), result.residueShare(),
                            result.twinGroups(), result.twinLines(), result.minSplit(),
                            result.modalSplit(), result.maxSplit(), result.splitLag(),
                            result.stretched(), result.topPrefix(), result.topCount());
                });
    }

    private static Rendering emptyView() {
        return view(false, "", 0, 0, 0, 0.0, 0.0, 0, 0, 0, 0, 0, 0, false, "", 0);
    }

    private static Rendering view(
            boolean scanned,
            String summary,
            int lineCount,
            int rowCount,
            int nearAt,
            double shareAtNear,
            double residueShare,
            int twinGroups,
            int twinLines,
            int minSplit,
            int modalSplit,
            int maxSplit,
            int splitLag,
            boolean stretched,
            String topPrefix,
            int topCount) {
        return Rendering.view("residue")
                .modelAttribute("scanned", scanned)
                .modelAttribute("maxChars", ResidueScan.MAX_CHARS)
                .modelAttribute("summary", summary)
                .modelAttribute("lineCount", lineCount)
                .modelAttribute("rowCount", rowCount)
                .modelAttribute("nearAt", nearAt)
                .modelAttribute("shareAtNear", shareAtNear)
                .modelAttribute("residueShare", residueShare)
                .modelAttribute("twinGroups", twinGroups)
                .modelAttribute("twinLines", twinLines)
                .modelAttribute("minSplit", minSplit)
                .modelAttribute("modalSplit", modalSplit)
                .modelAttribute("maxSplit", maxSplit)
                .modelAttribute("splitLag", splitLag)
                .modelAttribute("stretched", stretched)
                .modelAttribute("topPrefix", topPrefix)
                .modelAttribute("topCount", topCount)
                .build();
    }
}
