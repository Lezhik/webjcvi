package org.webjcvi.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.webjcvi.dna.DnaParser;
import org.webjcvi.storage.FileStorageService;

class DnaReportServiceTest {

    @TempDir
    Path projectRoot;

    private FileStorageService storage;
    private DnaReportService reports;

    @BeforeEach
    void setUp() throws Exception {
        storage = new FileStorageService(projectRoot);
        reports = new DnaReportService(
                storage,
                new DnaParser(),
                Clock.fixed(Instant.parse("2026-09-11T17:00:00Z"), ZoneOffset.UTC),
                "jcvi-dna.txt",
                "build/reports/dna",
                "dna-report.md");
        Files.createDirectories(projectRoot.resolve("src/main/java/org/webjcvi"));
        Files.writeString(projectRoot.resolve("src/main/java/org/webjcvi/Example.java"), "class Example {}");
        Files.writeString(projectRoot.resolve("jcvi-dna.txt"), "ATGC\natgN\n");
    }

    @Test
    void regenerateWritesMarkdownUnderBuildReports() {
        DnaReport report = reports.regenerate();
        assertThat(storage.exists("build/reports/dna/dna-report.md")).isTrue();
        assertThat(report.sequence().normalized()).isEqualTo("ATGCATGN");
        assertThat(report.markdown()).contains("WebJCVI DNA Report");
        assertThat(report.markdown()).contains("src/main/java/org/webjcvi/Example.java");
        assertThat(report.markdown()).contains("Line runway");
        assertThat(report.markdown()).contains("Frame remainder");
        assertThat(report.sections()).containsKeys(
                "length", "gcPercent", "javaSourceCount",
                "wrapModalWidth", "lineCliffAt", "lineModalFork", "lineRunway",
                "lineOverhang", "lineUniqueAt", "lineShareAt16");
        assertThat(report.sequence().ambiguousTotal()).isEqualTo(1);
    }

    @Test
    void regenerateClearsPriorReportsInTheSameDirectory() {
        storage.writeText("build/reports/dna/stale.md", "stale");
        storage.writeText("build/reports/dna/nested/old.md", "old");
        reports.regenerate();
        assertThat(storage.exists("build/reports/dna/stale.md")).isFalse();
        assertThat(storage.exists("build/reports/dna/nested/old.md")).isFalse();
        assertThat(storage.exists("build/reports/dna/dna-report.md")).isTrue();
        assertThat(storage.listRecursive("build/reports/dna")).containsExactly("build/reports/dna/dna-report.md");
    }

    @Test
    void loadCurrentReadsTheStoredMarkdown() {
        reports.regenerate();
        DnaReport loaded = reports.loadCurrent().orElseThrow();
        assertThat(loaded.markdown()).contains("Canonical bases");
        assertThat(loaded.sequence().length()).isEqualTo(8);
    }

    @Test
    void emptyDnaFileStillProducesAReport() throws Exception {
        Files.writeString(projectRoot.resolve("jcvi-dna.txt"), "");
        DnaReport report = reports.regenerate();
        assertThat(report.sequence().isEmpty()).isTrue();
        assertThat(report.markdown()).contains("Length (normalized bases) | 0");
    }

    @Test
    void missingDnaFileIsTreatedAsEmpty() throws Exception {
        Files.delete(projectRoot.resolve("jcvi-dna.txt"));
        DnaReport report = reports.regenerate();
        assertThat(report.sequence().isEmpty()).isTrue();
    }
}
