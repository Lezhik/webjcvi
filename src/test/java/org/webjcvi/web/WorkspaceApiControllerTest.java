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
}
