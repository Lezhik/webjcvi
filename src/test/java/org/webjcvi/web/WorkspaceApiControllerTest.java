package org.webjcvi.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.webjcvi.storage.FileStorageService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class WorkspaceApiControllerTest {

    @TempDir
    static Path projectRoot;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("webjcvi.storage.project-root", () -> projectRoot.toAbsolutePath().toString());
        registry.add("webjcvi.storage.max-file-size-bytes", () -> 8192);
        registry.add("spring.ai.mcp.server.enabled", () -> false);
        registry.add("gg.jte.usePrecompiledTemplates", () -> false);
        registry.add("gg.jte.development-mode", () -> true);
    }

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    FileStorageService storage;

    @Autowired
    org.webjcvi.tape.ScratchTape tape;

    @Test
    void listAndReadStayInsideTheSandbox() {
        storage.writeText("ok.txt", "hello-api");
        webTestClient.get()
                .uri("/api/files?path=.")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<java.util.List<String>>() {})
                .value(names -> assertThat(names).contains("ok.txt"));

        webTestClient.get()
                .uri(uri -> uri.path("/api/files/content").queryParam("path", "ok.txt").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("hello-api");
    }

    @Test
    void httpReadRejectsTraversal() {
        webTestClient.get()
                .uri(uri -> uri.path("/api/files/content").queryParam("path", "../secret.txt").build())
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.error").isEqualTo("path_escape");
    }

    @Test
    void httpReadRejectsAbsolutePath() {
        String absolute = projectRoot.getParent().resolve("secret.txt").toAbsolutePath().toString();
        webTestClient.get()
                .uri(uri -> uri.path("/api/files/content").queryParam("path", absolute).build())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void httpReadRejectsOversizeFile() throws Exception {
        java.nio.file.Files.write(projectRoot.resolve("huge.bin"), new byte[8193]);
        webTestClient.get()
                .uri(uri -> uri.path("/api/files/content").queryParam("path", "huge.bin").build())
                .exchange()
                .expectStatus().isEqualTo(413);
    }

    @Test
    void regenerateProducesAReport() throws Exception {
        java.nio.file.Files.writeString(projectRoot.resolve("jcvi-dna.txt"), "ATGC");
        webTestClient.post()
                .uri("/api/report/regenerate")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length").isEqualTo(4)
                .jsonPath("$.path").isEqualTo("build/reports/dna/dna-report.md");

        webTestClient.get()
                .uri("/api/report")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("WebJCVI DNA Report"));
    }

    @Test
    void homePageRenders() {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("WebJCVI");
                    assertThat(html).contains("/tape");
                    assertThat(html).contains("/split");
                    assertThat(html).contains("/drift");
                    assertThat(html).contains("/reflow");
                    assertThat(html).contains("/rare");
                    assertThat(html).contains("/tokens");
                    assertThat(html).contains("/stamps");
                    assertThat(html).contains("/fold");
                    assertThat(html).contains("/loop");
                    assertThat(html).contains("/fuzzy");
                    assertThat(html).contains("/contrast");
                });
    }

    @Test
    void scratchTapeApiRoundTrip() {
        tape.clear();
        webTestClient.post()
                .uri(uri -> uri.path("/api/tape").queryParam("text", "Hello\nHELLO").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length").isEqualTo(11);

        webTestClient.get()
                .uri(uri -> uri.path("/api/tape/find").queryParam("q", "hello").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].line").isEqualTo(1)
                .jsonPath("$[1].line").isEqualTo(2);

        webTestClient.get()
                .uri("/api/tape/runs?min=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].symbol").isEqualTo("L")
                .jsonPath("$[0].length").isEqualTo(2);
    }

    @Test
    void tapePageRenders() {
        webTestClient.get()
                .uri("/tape")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Scratch tape"));
    }

    @Test
    void bannerSplitApiCutsOnLongRuns() {
        webTestClient.post()
                .uri(uri -> uri.path("/api/split")
                        .queryParam("text", "alpha\n==========\nbeta")
                        .queryParam("min", "10")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].preview").isEqualTo("alpha")
                .jsonPath("$[1].banner").isEqualTo("==========")
                .jsonPath("$[1].preview").isEqualTo("beta");
    }

    @Test
    void splitPageRenders() {
        webTestClient.get()
                .uri("/split")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Banner split"));
    }

    @Test
    void splitPageFormCutsOnLongRuns() {
        webTestClient.post()
                .uri("/split")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("text=alpha%0A==========%0Abeta&minRun=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("2 section");
                    assertThat(html).contains("alpha");
                    assertThat(html).contains("beta");
                    assertThat(html).contains("==========");
                });
    }

    @Test
    void pairDriftApiFlagsLocalImbalance() {
        webTestClient.post()
                .uri(uri -> uri.path("/api/drift")
                        .queryParam("text", "((((((((((          ))))))))))          ")
                        .queryParam("window", "20")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.hotspotCount").isEqualTo(2)
                .jsonPath("$.hotspots[0].opens").isEqualTo(10)
                .jsonPath("$.hotspots[1].closes").isEqualTo(10);
    }

    @Test
    void driftPageRenders() {
        webTestClient.get()
                .uri("/drift")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Pair drift"));
    }

    @Test
    void driftPageFormFlagsLocalImbalance() {
        webTestClient.post()
                .uri("/drift")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("text=%28%28%28%28%28%28%28%28%28%28++++++++++%29%29%29%29%29%29%29%29%29%29++++++++++&window=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("hotspot");
                    assertThat(html).contains("opens 10");
                    assertThat(html).contains("closes 10");
                });
    }

    @Test
    void reflowApiStitchesFullWidthLines() {
        String wrapped = "a".repeat(70) + "\ncontinues";
        webTestClient.post()
                .uri(uri -> uri.path("/api/reflow")
                        .queryParam("text", wrapped)
                        .queryParam("width", "70")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.stitches").isEqualTo(1)
                .jsonPath("$.paragraphCount").isEqualTo(1)
                .jsonPath("$.paragraphs[0].length").isEqualTo(80);
    }

    @Test
    void reflowPageRenders() {
        webTestClient.get()
                .uri("/reflow")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Unwrap hard wrap"));
    }

    @Test
    void reflowPageFormStitchesFullWidthLines() {
        String body = "text=" + "a".repeat(70) + "%0Acontinues&width=70";
        webTestClient.post()
                .uri("/reflow")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("1 paragraph");
                    assertThat(html).contains("continues");
                });
    }

    @Test
    void rareApiFindsGcLikeIslands() {
        webTestClient.post()
                .uri(uri -> uri.path("/api/rare")
                        .queryParam("text", "AAAAAAAAAA GCGCGC AAAAAAAAAA")
                        .queryParam("min", "3")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.islandCount").isEqualTo(1)
                .jsonPath("$.islands[0].preview").isEqualTo("GCGCGC");
    }

    @Test
    void rarePageRenders() {
        webTestClient.get()
                .uri("/rare")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Rare-class islands"));
    }

    @Test
    void rarePageFormFindsGcLikeIslands() {
        webTestClient.post()
                .uri("/rare")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("text=AAAAAAAAAA+GCGCGC+AAAAAAAAAA&min=3")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("1 island");
                    assertThat(html).contains("GCGCGC");
                });
    }

    @Test
    void tokensApiSplitsOnGcLikeBreaks() {
        webTestClient.post()
                .uri(uri -> uri.path("/api/tokens")
                        .queryParam("text", "AAAAAAAAAA GCGCGC AAAAAAAAAA")
                        .queryParam("min", "2")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.tokenCount").isEqualTo(2)
                .jsonPath("$.tokens[0].preview").isEqualTo("AAAAAAAAAA")
                .jsonPath("$.tokens[1].preview").isEqualTo("AAAAAAAAAA");
    }

    @Test
    void tokensPageRenders() {
        webTestClient.get()
                .uri("/tokens")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Rare-break tokens"));
    }

    @Test
    void tokensPageFormSplitsOnGcLikeBreaks() {
        webTestClient.post()
                .uri("/tokens")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("text=AAAAAAAAAA+GCGCGC+AAAAAAAAAA&min=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("2 token");
                    assertThat(html).contains("AAAAAAAAAA");
                });
    }

    @Test
    void stampsApiRanksOverlappingAaa() {
        webTestClient.post()
                .uri(uri -> uri.path("/api/stamps")
                        .queryParam("text", "aaa bbb aaa")
                        .queryParam("k", "3")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.topKmer").isEqualTo("AAA")
                .jsonPath("$.stamps[0].kmer").isEqualTo("AAA");
    }

    @Test
    void stampsPageRenders() {
        webTestClient.get()
                .uri("/stamps")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("K-mer stamps"));
    }

    @Test
    void stampsPageFormRanksOverlappingAaa() {
        webTestClient.post()
                .uri("/stamps")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("text=aaa+bbb+aaa&k=3")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("AAA");
                    assertThat(html).contains("Top stamp");
                });
    }

    @Test
    void foldApiFindsAbbaAndSkipsHomopolymer() {
        webTestClient.post()
                .uri(uri -> uri.path("/api/fold")
                        .queryParam("text", "xx ABBA TTTT yy")
                        .queryParam("min", "4")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.hitCount").isEqualTo(1)
                .jsonPath("$.hits[0].preview").isEqualTo("ABBA");
    }

    @Test
    void foldPageRenders() {
        webTestClient.get()
                .uri("/fold")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Fold palindromes"));
    }

    @Test
    void foldPageFormFindsAbba() {
        webTestClient.post()
                .uri("/fold")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("text=xx+ABBA+TTTT+yy&min=4")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("ABBA");
                    assertThat(html).doesNotContain("TTTT");
                    assertThat(html).contains("palindrome");
                });
    }

    @Test
    void loopApiExtractsComplementarySpan() {
        webTestClient.post()
                .uri(uri -> uri.path("/api/loop")
                        .queryParam("text", "(hello) () \"nope\"")
                        .queryParam("min", "1")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.spanCount").isEqualTo(1)
                .jsonPath("$.spans[0].preview").isEqualTo("hello");
    }

    @Test
    void loopPageRenders() {
        webTestClient.get()
                .uri("/loop")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Stem loops"));
    }

    @Test
    void loopPageFormExtractsComplementarySpan() {
        webTestClient.post()
                .uri("/loop")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("text=%28hello%29+%28%29+%22nope%22&min=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("hello");
                    assertThat(html).doesNotContain("nope");
                    assertThat(html).contains("span");
                });
    }

    @Test
    void fuzzyApiFindsHalloAtDistanceOne() {
        webTestClient.post()
                .uri(uri -> uri.path("/api/fuzzy")
                        .queryParam("text", "hello hallo hello")
                        .queryParam("motif", "hello")
                        .queryParam("dist", "1")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.hitCount").isEqualTo(3)
                .jsonPath("$.hits[0].distance").isEqualTo(0)
                .jsonPath("$.hits[2].preview").isEqualTo("hallo");
    }

    @Test
    void fuzzyPageRenders() {
        webTestClient.get()
                .uri("/fuzzy")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Fuzzy find"));
    }

    @Test
    void fuzzyPageFormFindsHallo() {
        webTestClient.post()
                .uri("/fuzzy")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("text=hello+hallo+hello&motif=hello&dist=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("hallo");
                    assertThat(html).contains("distance");
                });
    }

    @Test
    void contrastApiFlagsRepeatedBlocks() {
        webTestClient.post()
                .uri(uri -> uri.path("/api/contrast")
                        .queryParam("text", "abcdabcd")
                        .queryParam("width", "4")
                        .queryParam("flag", "1")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.stutterCount").isEqualTo(1)
                .jsonPath("$.hits[0].left").isEqualTo("ABCD")
                .jsonPath("$.hits[0].right").isEqualTo("ABCD");
    }

    @Test
    void contrastPageRenders() {
        webTestClient.get()
                .uri("/contrast")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Block contrast"));
    }

    @Test
    void contrastPageFormFlagsRepeatedBlocks() {
        webTestClient.post()
                .uri("/contrast")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("text=abcdabcd&width=4&flag=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("ABCD");
                    assertThat(html).contains("stutter");
                });
    }

    @Test
    void logsAnalyzeApiReturnsFixedJsonContract() {
        webTestClient.post()
                .uri(uri -> uri.path("/api/logs/analyze")
                        .queryParam("text", "ERROR hello ABBA (loop)")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.apiVersion").isEqualTo(1)
                .jsonPath("$.truncated").isEqualTo(false)
                .jsonPath("$.tape.length").isNumber()
                .jsonPath("$.banners.sectionCount").isNumber()
                .jsonPath("$.drift.windowCount").isNumber()
                .jsonPath("$.reflow.paragraphCount").isNumber()
                .jsonPath("$.rare.islandCount").isNumber()
                .jsonPath("$.tokens.tokenCount").isNumber()
                .jsonPath("$.stamps.k").isNumber()
                .jsonPath("$.palindromes.hitCount").isNumber()
                .jsonPath("$.spans.spanCount").isNumber()
                .jsonPath("$.fuzzy.skipped").isBoolean()
                .jsonPath("$.contrast.stutterCount").isNumber();
    }
}
