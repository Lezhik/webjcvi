package org.webjcvi.logs;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.webjcvi.storage.FileStorageService;
import org.webjcvi.tape.ScratchTape;

class LogAnalysisServiceTest {

    private static final List<String> REQUIRED_KEYS = List.of(
            "apiVersion",
            "inputLength",
            "truncated",
            "tape",
            "banners",
            "drift",
            "reflow",
            "rare",
            "tokens",
            "stamps",
            "palindromes",
            "spans",
            "fuzzy",
            "contrast",
            "mirrors",
            "seams",
            "phase",
            "fields",
            "clones");

    private static final List<String> TAPE_KEYS = List.of("length", "runCount", "longestRun");
    private static final List<String> BANNER_KEYS = List.of("sectionCount", "minRun");
    private static final List<String> DRIFT_KEYS = List.of("windowCount", "hotspotCount", "opens", "closes", "globalSkew");
    private static final List<String> REFLOW_KEYS = List.of("sourceLines", "stitches", "paragraphCount");
    private static final List<String> RARE_KEYS = List.of("rareClass", "rareSymbolCount", "islandCount", "scanned");
    private static final List<String> TOKEN_KEYS = List.of("rareClass", "rareSymbolCount", "tokenCount");
    private static final List<String> STAMP_KEYS = List.of("k", "scanned", "distinct", "topKmer", "topCount");
    private static final List<String> PALINDROME_KEYS = List.of("scanned", "hitCount", "longest");
    private static final List<String> SPAN_KEYS = List.of(
            "scanned", "spanCount", "nested", "leftoverOpens", "leftoverCloses", "longestLoop");
    private static final List<String> FUZZY_KEYS = List.of(
            "motif", "scanned", "hitCount", "motifLength", "maxDist", "skipped");
    private static final List<String> CONTRAST_KEYS = List.of(
            "scanned", "stutterCount", "modalDistance", "meanDistance", "flagMax");
    private static final List<String> MIRROR_KEYS = List.of("scanned", "jointCount", "width");
    private static final List<String> SEAM_KEYS = List.of("scanned", "hitCount", "wrapWidth", "blockWidth");
    private static final List<String> PHASE_KEYS = List.of("scanned", "hitCount", "wrapWidth", "phase", "blockWidth");
    private static final List<String> FIELD_KEYS = List.of(
            "scanned", "recordCount", "wrapWidth", "topRc", "topReverse", "topIdentity");
    private static final List<String> CLONE_KEYS = List.of(
            "scanned", "distinct", "cloneGroups", "cloneFrames", "topCount", "wrapWidth");

    private final LogAnalysisService service = new LogAnalysisService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void analyzeAndAnalyzeToJsonSignaturesAreStable() throws Exception {
        Method analyze = LogAnalysisService.class.getMethod("analyze", String.class);
        assertThat(analyze.getReturnType()).isEqualTo(LogAnalysisReport.class);
        Method json = LogAnalysisService.class.getMethod("analyzeToJson", String.class);
        assertThat(json.getReturnType()).isEqualTo(String.class);
        Method persist = LogAnalysisService.class.getMethod("persistFromTestJournal", FileStorageService.class);
        assertThat(persist.getReturnType()).isEqualTo(String.class);
        assertThat(LogAnalysisService.API_VERSION).isEqualTo(1);
        assertThat(LogAnalysisService.TEST_LOG_PATH).isEqualTo("reports/logs/test.log");
        assertThat(LogAnalysisService.REPORT_JSON_PATH).isEqualTo("reports/logs/report.json");
        assertThat(LogAnalysisService.MAX_CHARS).isEqualTo(ScratchTape.MAX_CHARS);
    }

    @Test
    void jsonContractContainsRequiredKeysAndTypes() throws Exception {
        String sample = "ERROR hello hallo\n==========\nAAAAAAAAAA (payload) ABBA\n";
        JsonNode root = mapper.readTree(service.analyzeToJson(sample));
        assertThat(root.fieldNames()).toIterable().containsExactlyElementsOf(REQUIRED_KEYS);
        assertThat(root.get("apiVersion").isInt()).isTrue();
        assertThat(root.get("apiVersion").intValue()).isEqualTo(1);
        assertThat(root.get("inputLength").intValue()).isEqualTo(sample.length());
        assertThat(root.get("truncated").isBoolean()).isTrue();
        assertThat(root.get("truncated").booleanValue()).isFalse();
        assertThat(root.get("tape").fieldNames()).toIterable().containsExactlyElementsOf(TAPE_KEYS);
        assertThat(root.get("banners").fieldNames()).toIterable().containsExactlyElementsOf(BANNER_KEYS);
        assertThat(root.get("drift").fieldNames()).toIterable().containsExactlyElementsOf(DRIFT_KEYS);
        assertThat(root.get("reflow").fieldNames()).toIterable().containsExactlyElementsOf(REFLOW_KEYS);
        assertThat(root.get("rare").fieldNames()).toIterable().containsExactlyElementsOf(RARE_KEYS);
        assertThat(root.get("tokens").fieldNames()).toIterable().containsExactlyElementsOf(TOKEN_KEYS);
        assertThat(root.get("stamps").fieldNames()).toIterable().containsExactlyElementsOf(STAMP_KEYS);
        assertThat(root.get("palindromes").fieldNames()).toIterable().containsExactlyElementsOf(PALINDROME_KEYS);
        assertThat(root.get("spans").fieldNames()).toIterable().containsExactlyElementsOf(SPAN_KEYS);
        assertThat(root.get("fuzzy").fieldNames()).toIterable().containsExactlyElementsOf(FUZZY_KEYS);
        assertThat(root.get("contrast").fieldNames()).toIterable().containsExactlyElementsOf(CONTRAST_KEYS);
        assertThat(root.get("mirrors").fieldNames()).toIterable().containsExactlyElementsOf(MIRROR_KEYS);
        assertThat(root.get("seams").fieldNames()).toIterable().containsExactlyElementsOf(SEAM_KEYS);
        assertThat(root.get("phase").fieldNames()).toIterable().containsExactlyElementsOf(PHASE_KEYS);
        assertThat(root.get("fields").fieldNames()).toIterable().containsExactlyElementsOf(FIELD_KEYS);
        assertThat(root.get("clones").fieldNames()).toIterable().containsExactlyElementsOf(CLONE_KEYS);
        assertThat(root.get("tape").get("length").intValue()).isEqualTo(sample.length());
        assertThat(root.get("banners").get("sectionCount").intValue()).isGreaterThanOrEqualTo(1);
        assertThat(root.get("spans").get("spanCount").intValue()).isGreaterThanOrEqualTo(1);
        assertThat(root.get("palindromes").get("hitCount").intValue()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void nullAndEmptyProduceTheSameEmptyShape() throws Exception {
        JsonNode empty = mapper.readTree(service.analyzeToJson(""));
        JsonNode missing = mapper.readTree(service.analyzeToJson(null));
        assertThat(empty).isEqualTo(missing);
        assertThat(empty.get("inputLength").intValue()).isZero();
        assertThat(empty.get("truncated").booleanValue()).isFalse();
        assertThat(empty.get("tape").get("length").intValue()).isZero();
        assertThat(empty.get("fuzzy").get("skipped").booleanValue()).isTrue();
        assertThat(empty.fieldNames()).toIterable().containsExactlyElementsOf(REQUIRED_KEYS);
    }

    @Test
    void oversizeInputIsTruncatedAndFlagged() {
        String huge = "x".repeat(LogAnalysisService.MAX_CHARS + 25);
        LogAnalysisReport report = service.analyze(huge);
        assertThat(report.truncated()).isTrue();
        assertThat(report.inputLength()).isEqualTo(LogAnalysisService.MAX_CHARS + 25);
        assertThat(report.tape().length()).isEqualTo(LogAnalysisService.MAX_CHARS);
    }

    @Test
    void persistFromTestJournalWritesJsonThroughStorage(@TempDir Path projectRoot) throws Exception {
        FileStorageService storage = new FileStorageService(projectRoot);
        storage.writeText(LogAnalysisService.TEST_LOG_PATH, "hello hallo hello ABBA (loop)");
        String json = service.persistFromTestJournal(storage);
        assertThat(storage.readText(LogAnalysisService.REPORT_JSON_PATH)).isEqualTo(json);
        JsonNode root = mapper.readTree(json);
        assertThat(root.get("apiVersion").intValue()).isEqualTo(1);
        assertThat(root.get("inputLength").intValue()).isGreaterThan(0);
        assertThat(root.fieldNames()).toIterable().containsExactlyElementsOf(REQUIRED_KEYS);
    }

    @Test
    void persistFromMissingJournalWritesEmptyReport(@TempDir Path projectRoot) throws Exception {
        FileStorageService storage = new FileStorageService(projectRoot);
        String json = service.persistFromTestJournal(storage);
        JsonNode root = mapper.readTree(json);
        assertThat(root.get("inputLength").intValue()).isZero();
        assertThat(storage.readText(LogAnalysisService.REPORT_JSON_PATH)).isEqualTo(json);
    }

    @Test
    void analyzeDoesNotReadDnaOrProjectFiles(@TempDir Path projectRoot) throws Exception {
        FileStorageService storage = new FileStorageService(projectRoot);
        storage.writeText("secret.txt", "hidden-needle-should-not-appear");
        java.nio.file.Files.writeString(projectRoot.resolve("jcvi-dna.txt"), "ATGCATGCATGCATGC");
        LogAnalysisReport report = service.analyze("visible ABBA (ok)");
        String json = service.toJson(report);
        assertThat(json).doesNotContain("hidden-needle-should-not-appear");
        assertThat(json).doesNotContain("ATGCATGCATGCATGC");
        assertThat(storage.readText("secret.txt")).isEqualTo("hidden-needle-should-not-appear");
    }
}
