package org.webjcvi.mcp;

import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.webjcvi.report.DnaReport;
import org.webjcvi.report.DnaReportService;
import org.webjcvi.storage.FileStorageService;
import org.webjcvi.storage.StorageException;

/**
 * MCP tool surface. Delegates entirely to {@link FileStorageService} and
 * {@link DnaReportService} so sandboxing and size limits cannot be bypassed.
 */
@Component
public class WebJcviMcpTools {

    private final FileStorageService storage;
    private final DnaReportService reports;

    public WebJcviMcpTools(FileStorageService storage, DnaReportService reports) {
        this.storage = storage;
        this.reports = reports;
    }

    @Tool(name = "regenerate_dna_report", description = "Clear prior reports and rebuild the DNA report from jcvi-dna.txt and the current codebase.")
    public String regenerateDnaReport() {
        return run(() -> {
            DnaReport report = reports.regenerate();
            return "Wrote " + reports.reportRelativePath()
                    + " (" + report.sequence().length() + " bases, GC "
                    + String.format("%.2f", report.sequence().gcPercent()) + "%)";
        });
    }

    @Tool(name = "read_dna_report", description = "Read the current DNA report markdown from build/reports/dna/.")
    public String readDnaReport() {
        return run(() -> reports.loadCurrent()
                .map(DnaReport::markdown)
                .orElse("No report has been generated yet. Call regenerate_dna_report first."));
    }

    @Tool(name = "list_files", description = "List files inside the project directory. Paths must be relative to the project root.")
    public String listFiles(
            @ToolParam(description = "Relative directory to list, or '.' for the project root") String path,
            @ToolParam(description = "If true, list regular files recursively") boolean recursive) {
        return run(() -> {
            String directory = (path == null || path.isBlank()) ? "." : path;
            List<String> names = storage.list(directory, recursive);
            if (names.isEmpty()) {
                return "(empty)";
            }
            return String.join("\n", names);
        });
    }

    @Tool(name = "read_file", description = "Read a UTF-8 text file inside the project directory. Rejects path traversal, absolute paths, symlink escapes, and files over 500 MB.")
    public String readFile(
            @ToolParam(description = "Path relative to the project root") String path) {
        return run(() -> storage.readText(path));
    }

    private static String run(ToolAction action) {
        try {
            return action.execute();
        } catch (StorageException e) {
            return "Error: " + e.getMessage();
        }
    }

    @FunctionalInterface
    private interface ToolAction {
        String execute();
    }
}
