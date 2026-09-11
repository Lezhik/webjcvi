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
