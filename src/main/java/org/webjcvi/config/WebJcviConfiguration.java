package org.webjcvi.config;

import java.nio.file.Path;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.webjcvi.dna.DnaParser;
import org.webjcvi.report.DnaReportService;
import org.webjcvi.segment.BannerSplitter;
import org.webjcvi.storage.FileStorageService;
import org.webjcvi.tape.ScratchTape;
import org.webjcvi.drift.PairDrift;
import org.webjcvi.rare.RareClassScanner;
import org.webjcvi.reflow.WrapReflow;
import org.webjcvi.token.RareBreakTokenizer;
import org.webjcvi.stamp.KmerStamp;
import org.webjcvi.fold.PalindromeScan;
import org.webjcvi.loop.StemLoop;
import org.webjcvi.fuzzy.FuzzyFind;
import org.webjcvi.logs.LogAnalysisService;
import org.webjcvi.contrast.BlockContrast;
import org.webjcvi.mirror.MirrorJoint;
import org.webjcvi.seam.SeamGuard;
import org.webjcvi.phase.PhaseJoint;
import org.webjcvi.frame.FrameFields;
import org.webjcvi.clone.CloneScan;

@Configuration
public class WebJcviConfiguration {

    @Bean
    FileStorageService fileStorageService(WebJcviProperties properties) {
        Path root = resolveProjectRoot(properties.getStorage().getProjectRoot());
        return new FileStorageService(root, properties.getStorage().getMaxFileSizeBytes());
    }

    @Bean
    DnaParser dnaParser() {
        return new DnaParser();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ScratchTape scratchTape() {
        return new ScratchTape();
    }

    @Bean
    BannerSplitter bannerSplitter() {
        return new BannerSplitter();
    }

    @Bean
    PairDrift pairDrift() {
        return new PairDrift();
    }

    @Bean
    WrapReflow wrapReflow() {
        return new WrapReflow();
    }

    @Bean
    RareClassScanner rareClassScanner() {
        return new RareClassScanner();
    }

    @Bean
    RareBreakTokenizer rareBreakTokenizer(RareClassScanner rareClassScanner) {
        return new RareBreakTokenizer(rareClassScanner);
    }

    @Bean
    KmerStamp kmerStamp() {
        return new KmerStamp();
    }

    @Bean
    PalindromeScan palindromeScan() {
        return new PalindromeScan();
    }

    @Bean
    StemLoop stemLoop() {
        return new StemLoop();
    }

    @Bean
    FuzzyFind fuzzyFind() {
        return new FuzzyFind();
    }

    @Bean
    BlockContrast blockContrast() {
        return new BlockContrast();
    }

    @Bean
    MirrorJoint mirrorJoint() {
        return new MirrorJoint();
    }

    @Bean
    SeamGuard seamGuard() {
        return new SeamGuard();
    }

    @Bean
    PhaseJoint phaseJoint() {
        return new PhaseJoint();
    }

    @Bean
    FrameFields frameFields() {
        return new FrameFields();
    }

    @Bean
    CloneScan cloneScan() {
        return new CloneScan();
    }

    @Bean
    LogAnalysisService logAnalysisService() {
        return new LogAnalysisService();
    }

    @Bean
    DnaReportService dnaReportService(
            FileStorageService storage,
            DnaParser parser,
            Clock clock,
            WebJcviProperties properties) {
        return new DnaReportService(
                storage,
                parser,
                clock,
                properties.getDna().getRelativePath(),
                properties.getReport().getDirectory(),
                properties.getReport().getFileName());
    }

    private static Path resolveProjectRoot(String configured) {
        if (configured == null || configured.isBlank()) {
            return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }
}
