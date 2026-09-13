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
        "fuzzy",
        "contrast",
        "mirrors",
        "seams",
        "phase",
        "fields",
        "clones",
        "prefixes",
        "keys",
        "forks",
        "affixes",
        "lanes"
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
        FuzzySection fuzzy,
        ContrastSection contrast,
        MirrorSection mirrors,
        SeamSection seams,
        PhaseSection phase,
        FieldSection fields,
        CloneSection clones,
        PrefixSection prefixes,
        KeySection keys,
        ForkSection forks,
        AffixSection affixes,
        LaneSection lanes) {

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

    @JsonPropertyOrder({"scanned", "stutterCount", "modalDistance", "meanDistance", "flagMax"})
    public record ContrastSection(
            int scanned,
            int stutterCount,
            int modalDistance,
            double meanDistance,
            int flagMax) {
    }

    @JsonPropertyOrder({"scanned", "jointCount", "width"})
    public record MirrorSection(int scanned, int jointCount, int width) {
    }

    @JsonPropertyOrder({"scanned", "hitCount", "wrapWidth", "blockWidth"})
    public record SeamSection(int scanned, int hitCount, int wrapWidth, int blockWidth) {
    }

    @JsonPropertyOrder({"scanned", "hitCount", "wrapWidth", "phase", "blockWidth"})
    public record PhaseSection(int scanned, int hitCount, int wrapWidth, int phase, int blockWidth) {
    }

    @JsonPropertyOrder({"scanned", "recordCount", "wrapWidth", "topRc", "topReverse", "topIdentity"})
    public record FieldSection(
            int scanned,
            int recordCount,
            int wrapWidth,
            String topRc,
            String topReverse,
            String topIdentity) {
    }

    @JsonPropertyOrder({"scanned", "distinct", "cloneGroups", "cloneFrames", "topCount", "wrapWidth"})
    public record CloneSection(
            int scanned,
            int distinct,
            int cloneGroups,
            int cloneFrames,
            int topCount,
            int wrapWidth) {
    }

    @JsonPropertyOrder({"scanned", "distinct", "familyCount", "familyFrames", "topCount", "topPrefix", "wrapWidth", "prefixLength"})
    public record PrefixSection(
            int scanned,
            int distinct,
            int familyCount,
            int familyFrames,
            int topCount,
            String topPrefix,
            int wrapWidth,
            int prefixLength) {
    }

    @JsonPropertyOrder({"scanned", "wrapWidth", "floorLength", "uniqueAt", "uniqueShareAtFloor", "uniqueShareAtWrap"})
    public record KeySection(
            int scanned,
            int wrapWidth,
            int floorLength,
            int uniqueAt,
            double uniqueShareAtFloor,
            double uniqueShareAtWrap) {
    }

    @JsonPropertyOrder({"scanned", "wrapWidth", "prefixLength", "twinCount", "twinFrames", "uniqueShare", "topFork"})
    public record ForkSection(
            int scanned,
            int wrapWidth,
            int prefixLength,
            int twinCount,
            int twinFrames,
            double uniqueShare,
            int topFork) {
    }

    @JsonPropertyOrder({"scanned", "wrapWidth", "leadUniqueAt", "tailUniqueAt", "cheaperEnd", "leadShareAtNear", "tailShareAtNear"})
    public record AffixSection(
            int scanned,
            int wrapWidth,
            int leadUniqueAt,
            int tailUniqueAt,
            String cheaperEnd,
            double leadShareAtNear,
            double tailShareAtNear) {
    }

    @JsonPropertyOrder({"scanned", "wrapWidth", "tileLength", "troughAt", "peakAt", "troughShare", "peakShare", "spread"})
    public record LaneSection(
            int scanned,
            int wrapWidth,
            int tileLength,
            int troughAt,
            int peakAt,
            double troughShare,
            double peakShare,
            double spread) {
    }
}
