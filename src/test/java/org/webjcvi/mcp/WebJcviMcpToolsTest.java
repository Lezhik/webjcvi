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
                        "unwrap_wraps",
                        "rare_islands",
                        "cut_tokens",
                        "kmer_stamps",
                        "find_palindromes",
                        "extract_spans",
                        "fuzzy_find",
                        "block_contrast",
                        "find_mirrors",
                        "flag_seams",
                        "phase_mirrors",
                        "extract_fields",
                        "find_clones",
                        "group_prefixes",
                        "measure_key_width",
                        "find_forks",
                        "compare_affixes",
                        "profile_lanes",
                        "profile_rows",
                        "profile_cliffs",
                        "profile_runways",
                        "profile_rises",
                        "profile_majorities",
                        "profile_nears",
                        "analyze_logs",
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
        assertThat(markdown).contains("Line near");
        assertThat(markdown).contains("Frame remainder");
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

    @Test
    void unwrapWrapsDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "hidden-wrap");
        String wrapped = "a".repeat(70) + "\ncontinues";
        String result = tools.unwrapWraps(wrapped, 70);
        assertThat(result).contains("stitches=1");
        assertThat(result).contains("continues");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("hidden-wrap");
    }

    @Test
    void rareIslandsDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "GCGCGC-hidden");
        String result = tools.rareIslands("AAAAAAAAAA GCGCGC AAAAAAAAAA", 3);
        assertThat(result).contains("GCGCGC");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("GCGCGC-hidden");
    }

    @Test
    void cutTokensDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "hidden-token");
        String result = tools.cutTokens("AAAAAAAAAA GCGCGC AAAAAAAAAA", 2);
        assertThat(result).contains("AAAAAAAAAA");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("hidden-token");
    }

    @Test
    void kmerStampsDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "hidden-stamp");
        String result = tools.kmerStamps("aaa bbb aaa", 3);
        assertThat(result).contains("AAA");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("hidden-stamp");
    }

    @Test
    void findPalindromesDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "hidden ABBA");
        String result = tools.findPalindromes("xx ABBA TTTT yy", 4);
        assertThat(result).contains("ABBA");
        assertThat(result).doesNotContain("hidden");
        assertThat(result).doesNotContain("TTTT");
        assertThat(tools.readFile("secret.txt")).isEqualTo("hidden ABBA");
    }

    @Test
    void extractSpansDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "(hidden)");
        String result = tools.extractSpans("(hello) () \"nope\"", 1);
        assertThat(result).contains("hello");
        assertThat(result).doesNotContain("hidden");
        assertThat(result).doesNotContain("nope");
        assertThat(tools.readFile("secret.txt")).isEqualTo("(hidden)");
    }

    @Test
    void fuzzyFindDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "hello-hidden");
        String result = tools.fuzzyFind("hello hallo hello", "hello", 1);
        assertThat(result).contains("hallo");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("hello-hidden");
    }

    @Test
    void blockContrastDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "abcdabcd-hidden");
        String result = tools.blockContrast("abcdabcd", 4, 1);
        assertThat(result).contains("stutters=1");
        assertThat(result).contains("ABCD | ABCD");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("abcdabcd-hidden");
    }

    @Test
    void findMirrorsDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "abcddcba-hidden");
        String result = tools.findMirrors("abcddcba", 4);
        assertThat(result).contains("joints=1");
        assertThat(result).contains("ABCD | DCBA");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("abcddcba-hidden");
    }

    @Test
    void flagSeamsDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "ABCD-hidden");
        String left = "x".repeat(66) + "ABCD";
        String right = "DCBA" + "y".repeat(66);
        String result = tools.flagSeams(left + "\n" + right, 70, 4);
        assertThat(result).contains("hits=1");
        assertThat(result).contains("ABCD | DCBA");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("ABCD-hidden");
    }

    @Test
    void phaseMirrorsDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "abcddcba-hidden");
        String text = "x".repeat(19) + "abcddcba";
        String result = tools.phaseMirrors(text, 70, 19, 4);
        assertThat(result).contains("hits=1");
        assertThat(result).contains("ABCD | DCBA");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("abcddcba-hidden");
    }

    @Test
    void extractFieldsDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "GCTAGCTA-hidden");
        char[] buf = new char[70];
        java.util.Arrays.fill(buf, 'x');
        "gctagcta".getChars(0, 8, buf, 4);
        "abcddcba".getChars(0, 8, buf, 19);
        "atgcatgc".getChars(0, 8, buf, 58);
        String result = tools.extractFields(new String(buf), 70);
        assertThat(result).contains("records=1");
        assertThat(result).contains("rc GCTAGCTA");
        assertThat(result).contains("rev ABCDDCBA");
        assertThat(result).contains("id ATGCATGC");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("GCTAGCTA-hidden");
    }

    @Test
    void findClonesDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "CLONEFRAME-hidden");
        String frame = "A".repeat(70);
        String result = tools.findClones(frame + frame, 70);
        assertThat(result).contains("cloneGroups=1");
        assertThat(result).contains("cloneFrames=2");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("CLONEFRAME-hidden");
    }

    @Test
    void groupPrefixesDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "HEADHEAD-hidden");
        String a = "HEADHEAD" + "A".repeat(62);
        String b = "HEADHEAD" + "B".repeat(62);
        String result = tools.groupPrefixes(a + b, 70, 8);
        assertThat(result).contains("familyCount=1");
        assertThat(result).contains("prefix HEADHEAD");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("HEADHEAD-hidden");
    }

    @Test
    void measureKeyWidthDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "HEADHEAD-hidden");
        String a = "HEADHEAD" + "A".repeat(62);
        String b = "HEADHEAD" + "B".repeat(62);
        String result = tools.measureKeyWidth(a + b, 70);
        assertThat(result).contains("uniqueAt=9");
        assertThat(result).contains("k 9");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("HEADHEAD-hidden");
    }

    @Test
    void findForksDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "ABCDEFGHIJKLMNOP-hidden");
        String a = "ABCDEFGHIJKLMNOP" + "A".repeat(54);
        String b = "ABCDEFGHIJKLMNOP" + "B".repeat(54);
        String result = tools.findForks(a + b, 70, 16);
        assertThat(result).contains("twinCount=1");
        assertThat(result).contains("forkAt 17");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("ABCDEFGHIJKLMNOP-hidden");
    }

    @Test
    void compareAffixesDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "AAAAAAAA-hidden");
        String a = "T".repeat(62) + "AAAAAAAA";
        String b = "T".repeat(62) + "CCCCCCCC";
        String result = tools.compareAffixes(a + b, 70);
        assertThat(result).contains("cheaper=tail");
        assertThat(result).contains("tailUniqueAt=8");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("AAAAAAAA-hidden");
    }

    @Test
    void profileLanesDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "AAAAAAAA-hidden");
        String a = "T".repeat(35) + "A".repeat(8) + "T".repeat(27);
        String b = "T".repeat(35) + "C".repeat(8) + "T".repeat(27);
        String result = tools.profileLanes(a + b, 70);
        assertThat(result).contains("peakAt=27");
        assertThat(result).contains("spread=0.5000");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("AAAAAAAA-hidden");
    }

    @Test
    void profileRowsDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "2026-hidden");
        String text = "2026-09-13 21:56 worker-a\n2026-09-13 21:56 worker-b\n";
        String result = tools.profileRows(text);
        assertThat(result).contains("share16=0.5000");
        assertThat(result).contains("twins=1");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("2026-hidden");
    }

    @Test
    void profileCliffsDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "2026-hidden");
        String text = "2026-09-13 21:56 worker-a\n2026-09-13 21:56 worker-b\n";
        String result = tools.profileCliffs(text);
        assertThat(result).contains("forkAt=25");
        assertThat(result).contains("cliffAt=25");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("2026-hidden");
    }

    @Test
    void profileRunwaysDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "2026-hidden");
        String text = "2026-09-13 21:56 worker-a\n2026-09-13 21:56 worker-b\n";
        String result = tools.profileRunways(text);
        assertThat(result).contains("runway=0");
        assertThat(result).contains("stretched=false");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("2026-hidden");
    }

    @Test
    void profileRisesDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "2026-hidden");
        String text = "AAAAAAAA11111111 one\nCCCCCCCC22222222 two\n";
        String result = tools.profileRises(text);
        assertThat(result).contains("riseAt=8");
        assertThat(result).contains("pastClock=false");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("2026-hidden");
    }

    @Test
    void profileMajoritiesDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "2026-hidden");
        String text = "AAAAAAAA11111111 one\nCCCCCCCC22222222 two\n";
        String result = tools.profileMajorities(text);
        assertThat(result).contains("majorityAt=8");
        assertThat(result).contains("pastRise=false");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("2026-hidden");
    }

    @Test
    void profileNearsDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "2026-hidden");
        String text = "AAAAAAAA11111111 one\nCCCCCCCC22222222 two\n";
        String result = tools.profileNears(text);
        assertThat(result).contains("nearAt=8");
        assertThat(result).contains("pastMajority=false");
        assertThat(result).doesNotContain("hidden");
        assertThat(tools.readFile("secret.txt")).isEqualTo("2026-hidden");
    }

    @Test
    void analyzeLogsDoesNotReadProjectFiles() {
        storage.writeText("secret.txt", "hidden-log-needle");
        String json = tools.analyzeLogs("visible ABBA (ok)");
        assertThat(json).doesNotStartWith("Error:");
        assertThat(json).contains("\"apiVersion\":1");
        assertThat(json).contains("\"tape\"");
        assertThat(json).doesNotContain("hidden-log-needle");
        assertThat(tools.readFile("secret.txt")).isEqualTo("hidden-log-needle");
    }
}
