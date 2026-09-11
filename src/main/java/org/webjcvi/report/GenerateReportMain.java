package org.webjcvi.report;

import java.nio.file.Path;
import org.webjcvi.storage.FileStorageService;

/**
 * Command-line DNA decoder used during preparation and as step 1 of every
 * iteration. Instantiates {@link FileStorageService} and
 * {@link DnaReportService} and regenerates {@code build/reports/dna/dna-report.md}.
 */
public final class GenerateReportMain {

    private GenerateReportMain() {
    }

    public static void main(String[] args) {
        Path projectRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        FileStorageService storage = new FileStorageService(projectRoot);
        DnaReportService reports = new DnaReportService(storage);
        DnaReport report = reports.regenerate();
        System.out.println("Wrote " + reports.reportRelativePath()
                + " (" + report.sequence().length() + " bases, "
                + report.sequence().gcPercent() + "% GC)");
    }
}
