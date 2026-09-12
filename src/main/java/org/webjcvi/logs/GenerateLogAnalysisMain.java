package org.webjcvi.logs;

import java.nio.file.Path;
import org.webjcvi.storage.FileStorageService;

/**
 * Command-line log analyzer used before iteration pros/cons. Reads
 * {@code reports/logs/test.log} and writes {@code reports/logs/report.json}
 * through {@link FileStorageService}.
 */
public final class GenerateLogAnalysisMain {

    private GenerateLogAnalysisMain() {
    }

    public static void main(String[] args) {
        Path projectRoot = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        FileStorageService storage = new FileStorageService(projectRoot);
        LogAnalysisService analysis = new LogAnalysisService();
        String json = analysis.persistFromTestJournal(storage);
        System.out.println("Wrote " + LogAnalysisService.REPORT_JSON_PATH
                + " (" + json.length() + " chars)");
    }
}
