package org.webjcvi.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.webjcvi.report.DnaReportService;
import org.webjcvi.storage.FileStorageService;

class WebJcviMcpToolsTest {

    @TempDir
    Path projectRoot;

    private WebJcviMcpTools tools;
    private FileStorageService storage;

    @BeforeEach
    void setUp() throws Exception {
        storage = new FileStorageService(projectRoot, 64);
        DnaReportService reports = new DnaReportService(storage);
        tools = new WebJcviMcpTools(storage, reports);
        Files.writeString(projectRoot.resolve("jcvi-dna.txt"), "ATGC");
        storage.writeText("visible.txt", "safe");
    }

    @Test
    void toolCallbackProviderExposesTheFourSharedTools() {
        var provider = MethodToolCallbackProvider.builder().toolObjects(tools).build();
        assertThat(provider.getToolCallbacks())
                .extracting(callback -> callback.getToolDefinition().name())
                .containsExactlyInAnyOrder(
                        "regenerate_dna_report",
                        "read_dna_report",
                        "tape_load",
                        "tape_find",
                        "tape_runs",
                        "split_banners",
                        "pair_drift",
                        "list_files",
                        "read_file");
    }

    @Test
    void readFileServesSandboxedContent() {
        assertThat(tools.readFile("visible.txt")).isEqualTo("safe");
    }

    @Test
    void readFileRejectsTraversal() {
        String result = tools.readFile("../secret.txt");
        assertThat(result).startsWith("Error:");
        assertThat(result).containsIgnoringCase("escape");
    }

    @Test
    void readFileRejectsAbsolutePath() {
        String absolute = projectRoot.getParent().resolve("secret.txt").toAbsolutePath().toString();
        String result = tools.readFile(absolute);
        assertThat(result).startsWith("Error:");
        assertThat(result).doesNotContain("classified");
    }

    @Test
    void readFileRejectsOversizeFile() throws Exception {
        Files.write(projectRoot.resolve("huge.bin"), new byte[65]);
        String result = tools.readFile("huge.bin");
        assertThat(result).startsWith("Error:");
        assertThat(result).contains("byte limit");
    }

    @Test
    void listFilesRejectsTraversal() {
        String result = tools.listFiles("..", false);
        assertThat(result).startsWith("Error:");
    }

    @Test
    void regenerateAndReadReportRoundTrip() throws Exception {
        FileStorageService roomy = new FileStorageService(projectRoot);
        WebJcviMcpTools roomyTools = new WebJcviMcpTools(roomy, new DnaReportService(roomy));
        String regenerated = roomyTools.regenerateDnaReport();
        assertThat(regenerated).doesNotStartWith("Error:");
        assertThat(regenerated).contains("build/reports/dna/dna-report.md");
        String markdown = roomyTools.readDnaReport();
        assertThat(markdown).contains("WebJCVI DNA Report");
        assertThat(markdown).contains("ATGC");
        assertThat(markdown).contains("Skew-island geography");
        assertThat(markdown).contains("Wrap-joint dinucleotides");
    }

    @Test
    void scratchTapeFindDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "needle-in-file");
        assertThat(tools.tapeLoad("Needle in the haystack")).startsWith("loaded");
        assertThat(tools.tapeFind("needle")).contains("offset 0");
        assertThat(tools.tapeRuns(2)).contains("E x2");
        assertThat(tools.readFile("secret.txt")).isEqualTo("needle-in-file");
    }

    @Test
    void splitBannersDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "==========hidden");
        String result = tools.splitBanners("head\n==========\ntail", 10);
        assertThat(result).contains("head");
        assertThat(result).contains("tail");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("==========hidden");
    }

    @Test
    void pairDriftDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "((((((((((hidden))))))))))");
        String result = tools.pairDrift("((((((((((          ))))))))))          ", 20);
        assertThat(result).contains("hotspots=2");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("((((((((((hidden))))))))))");
    }
}
