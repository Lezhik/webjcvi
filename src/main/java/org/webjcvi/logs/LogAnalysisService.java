package org.webjcvi.logs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webjcvi.drift.PairDrift;
import org.webjcvi.fold.PalindromeScan;
import org.webjcvi.fuzzy.FuzzyFind;
import org.webjcvi.loop.StemLoop;
import org.webjcvi.rare.RareClassScanner;
import org.webjcvi.reflow.WrapReflow;
import org.webjcvi.segment.BannerSplitter;
import org.webjcvi.stamp.KmerStamp;
import org.webjcvi.storage.FileStorageService;
import org.webjcvi.tape.ScratchTape;
import org.webjcvi.token.RareBreakTokenizer;

/**
 * Fixed-API facade over the text-processing algorithms for log analysis.
 * Input is a single string; output is {@link LogAnalysisReport} / JSON.
 * Does not read the DNA file and does not touch the filesystem; callers
 * persist {@code reports/logs/report.json} through {@link FileStorageService}.
 */
public final class LogAnalysisService {

    public static final int API_VERSION = 1;
    public static final String TEST_LOG_PATH = "reports/logs/test.log";
    public static final String REPORT_JSON_PATH = "reports/logs/report.json";
    public static final int MAX_CHARS = ScratchTape.MAX_CHARS;

    private static final Logger log = LoggerFactory.getLogger(LogAnalysisService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BannerSplitter splitter;
    private final PairDrift drift;
    private final WrapReflow reflow;
    private final RareClassScanner rare;
    private final RareBreakTokenizer tokenizer;
    private final KmerStamp stamp;
    private final PalindromeScan palindromes;
    private final StemLoop loops;
    private final FuzzyFind fuzzy;

    public LogAnalysisService() {
        this(new BannerSplitter(), new PairDrift(), new WrapReflow(), new RareClassScanner(),
                new RareBreakTokenizer(), new KmerStamp(), new PalindromeScan(), new StemLoop(),
                new FuzzyFind());
    }

    public LogAnalysisService(
            BannerSplitter splitter,
            PairDrift drift,
            WrapReflow reflow,
            RareClassScanner rare,
            RareBreakTokenizer tokenizer,
            KmerStamp stamp,
            PalindromeScan palindromes,
            StemLoop loops,
            FuzzyFind fuzzy) {
        this.splitter = splitter;
        this.drift = drift;
        this.reflow = reflow;
        this.rare = rare;
        this.tokenizer = tokenizer;
        this.stamp = stamp;
        this.palindromes = palindromes;
        this.loops = loops;
        this.fuzzy = fuzzy;
    }

    /**
     * Analyze caller-supplied log text. {@code null} is treated as empty.
     * Input longer than {@link #MAX_CHARS} is truncated and flagged.
     */
    public LogAnalysisReport analyze(String text) {
        int inputLength = text == null ? 0 : text.length();
        boolean truncated = inputLength > MAX_CHARS;
        String payload = text == null ? "" : text;
        if (truncated) {
            payload = payload.substring(0, MAX_CHARS);
        }
        if (log.isDebugEnabled()) {
            log.debug("log-analysis.start chars={} truncated={}", inputLength, truncated);
        }

        ScratchTape tape = new ScratchTape();
        tape.load(payload);
        var runs = tape.runs(2, ScratchTape.DEFAULT_MAX_HITS);
        int longestRun = 0;
        for (var run : runs) {
            if (run.length() > longestRun) {
                longestRun = run.length();
            }
        }
        var tapeSection = new LogAnalysisReport.TapeSection(tape.length(), runs.size(), longestRun);

        var banners = splitter.split(payload);
        var bannerSection = new LogAnalysisReport.BannerSection(banners.size(), BannerSplitter.DEFAULT_MIN_RUN);

        var driftScan = drift.scan(payload);
        var driftSection = new LogAnalysisReport.DriftSection(
                driftScan.windowCount(),
                driftScan.hotspotCount(),
                driftScan.opens(),
                driftScan.closes(),
                driftScan.globalSkew());

        var reflowResult = reflow.unwrap(payload);
        var reflowSection = new LogAnalysisReport.ReflowSection(
                reflowResult.sourceLines(),
                reflowResult.stitches(),
                reflowResult.paragraphCount());

        var rareScan = rare.scan(payload);
        var rareSection = new LogAnalysisReport.RareSection(
                rareScan.rareClass(),
                rareScan.rareSymbolCount(),
                rareScan.islandCount(),
                rareScan.scanned());

        var cut = tokenizer.cut(payload);
        var tokenSection = new LogAnalysisReport.TokenSection(
                cut.rareClass(), cut.rareSymbolCount(), cut.tokenCount());

        var census = stamp.rank(payload);
        var stampSection = new LogAnalysisReport.StampSection(
                census.k(), census.scanned(), census.distinct(), census.topKmer(), census.topCount());

        var foldScan = palindromes.find(payload);
        var palindromeSection = new LogAnalysisReport.PalindromeSection(
                foldScan.scanned(), foldScan.hitCount(), foldScan.longest());

        var loopScan = loops.extract(payload);
        var spanSection = new LogAnalysisReport.SpanSection(
                loopScan.scanned(),
                loopScan.spanCount(),
                loopScan.nested(),
                loopScan.leftoverOpens(),
                loopScan.leftoverCloses(),
                loopScan.longestLoop());

        String motif = pickFuzzyMotif(census, cut);
        LogAnalysisReport.FuzzySection fuzzySection;
        if (motif.isEmpty()) {
            fuzzySection = new LogAnalysisReport.FuzzySection("", 0, 0, 0, FuzzyFind.DEFAULT_MAX_DIST, true);
        } else {
            var fuzzyScan = fuzzy.search(payload, motif);
            fuzzySection = new LogAnalysisReport.FuzzySection(
                    motif,
                    fuzzyScan.scanned(),
                    fuzzyScan.hitCount(),
                    fuzzyScan.motifLength(),
                    fuzzyScan.maxDist(),
                    false);
        }

        LogAnalysisReport report = new LogAnalysisReport(
                API_VERSION,
                inputLength,
                truncated,
                tapeSection,
                bannerSection,
                driftSection,
                reflowSection,
                rareSection,
                tokenSection,
                stampSection,
                palindromeSection,
                spanSection,
                fuzzySection);
        if (log.isDebugEnabled()) {
            log.debug("log-analysis.done truncated={} tapeRuns={} banners={} hotspots={} tokens={} stamps={} palindromes={} spans={} fuzzyHits={}",
                    truncated,
                    tapeSection.runCount(),
                    bannerSection.sectionCount(),
                    driftSection.hotspotCount(),
                    tokenSection.tokenCount(),
                    stampSection.distinct(),
                    palindromeSection.hitCount(),
                    spanSection.spanCount(),
                    fuzzySection.hitCount());
        }
        return report;
    }

    public String analyzeToJson(String text) {
        return toJson(analyze(text));
    }

    public String toJson(LogAnalysisReport report) {
        try {
            return MAPPER.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize log-analysis report", e);
        }
    }

    /**
     * Read {@link #TEST_LOG_PATH} (empty if missing) and write JSON to
     * {@link #REPORT_JSON_PATH} through the storage sandbox.
     */
    public String persistFromTestJournal(FileStorageService storage) {
        String journal = "";
        if (storage.exists(TEST_LOG_PATH)) {
            journal = storage.readText(TEST_LOG_PATH);
        }
        String json = analyzeToJson(journal);
        storage.writeText(REPORT_JSON_PATH, json);
        if (log.isDebugEnabled()) {
            log.debug("log-analysis.persisted journalChars={} path={}", journal.length(), REPORT_JSON_PATH);
        }
        return json;
    }

    private static String pickFuzzyMotif(KmerStamp.Census census, RareBreakTokenizer.Cut cut) {
        if (census.topKmer() != null && census.topKmer().length() >= FuzzyFind.MIN_MOTIF) {
            return census.topKmer();
        }
        for (var token : cut.tokens()) {
            if (token.preview() != null && token.preview().length() >= FuzzyFind.MIN_MOTIF) {
                String candidate = token.preview();
                if (candidate.length() > FuzzyFind.MAX_MOTIF) {
                    return candidate.substring(0, FuzzyFind.MAX_MOTIF);
                }
                return candidate;
            }
        }
        return "";
    }
}
