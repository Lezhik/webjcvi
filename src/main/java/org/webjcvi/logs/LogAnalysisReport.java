package org.webjcvi.logs;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Fixed JSON shape of the log-analysis facade. Field names are part of the
 * public contract and are locked by unit tests.
 */
@JsonPropertyOrder({
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
        "fuzzy"
})
public record LogAnalysisReport(
        int apiVersion,
        int inputLength,
        boolean truncated,
        TapeSection tape,
        BannerSection banners,
        DriftSection drift,
        ReflowSection reflow,
        RareSection rare,
        TokenSection tokens,
        StampSection stamps,
        PalindromeSection palindromes,
        SpanSection spans,
        FuzzySection fuzzy) {

    @JsonPropertyOrder({"length", "runCount", "longestRun"})
    public record TapeSection(int length, int runCount, int longestRun) {
    }

    @JsonPropertyOrder({"sectionCount", "minRun"})
    public record BannerSection(int sectionCount, int minRun) {
    }

    @JsonPropertyOrder({"windowCount", "hotspotCount", "opens", "closes", "globalSkew"})
    public record DriftSection(int windowCount, int hotspotCount, int opens, int closes, double globalSkew) {
    }

    @JsonPropertyOrder({"sourceLines", "stitches", "paragraphCount"})
    public record ReflowSection(int sourceLines, int stitches, int paragraphCount) {
    }

    @JsonPropertyOrder({"rareClass", "rareSymbolCount", "islandCount", "scanned"})
    public record RareSection(String rareClass, int rareSymbolCount, int islandCount, long scanned) {
    }

    @JsonPropertyOrder({"rareClass", "rareSymbolCount", "tokenCount"})
    public record TokenSection(String rareClass, int rareSymbolCount, int tokenCount) {
    }

    @JsonPropertyOrder({"k", "scanned", "distinct", "topKmer", "topCount"})
    public record StampSection(int k, int scanned, int distinct, String topKmer, long topCount) {
    }

    @JsonPropertyOrder({"scanned", "hitCount", "longest"})
    public record PalindromeSection(int scanned, int hitCount, int longest) {
    }

    @JsonPropertyOrder({"scanned", "spanCount", "nested", "leftoverOpens", "leftoverCloses", "longestLoop"})
    public record SpanSection(
            int scanned,
            int spanCount,
            int nested,
            int leftoverOpens,
            int leftoverCloses,
            int longestLoop) {
    }

    @JsonPropertyOrder({"motif", "scanned", "hitCount", "motifLength", "maxDist", "skipped"})
    public record FuzzySection(
            String motif,
            int scanned,
            int hitCount,
            int motifLength,
            int maxDist,
            boolean skipped) {
    }
}
