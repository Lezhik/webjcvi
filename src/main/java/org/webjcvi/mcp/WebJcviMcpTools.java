package org.webjcvi.mcp;

import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.webjcvi.report.DnaReport;
import org.webjcvi.report.DnaReportService;
import org.webjcvi.storage.FileStorageService;
import org.webjcvi.storage.StorageException;
import org.webjcvi.tape.ScratchTape;
import org.webjcvi.tape.TapeException;

/**
 * MCP tool surface. File tools go through {@link FileStorageService}. The
 * scratch tape is in-memory caller text and never reads the DNA file or
 * project paths.
 */
@Component
public class WebJcviMcpTools {

    private final FileStorageService storage;
    private final DnaReportService reports;
    private final ScratchTape tape;

    public WebJcviMcpTools(FileStorageService storage, DnaReportService reports) {
        this(storage, reports, new ScratchTape());
    }

    @Autowired
    public WebJcviMcpTools(FileStorageService storage, DnaReportService reports, ScratchTape tape) {
        this.storage = storage;
        this.reports = reports;
        this.tape = tape;
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

    @Tool(name = "tape_load", description = "Replace the in-memory scratch tape with caller-supplied text. Not the DNA file and not a project path. Limit 524288 characters.")
    public String tapeLoad(
            @ToolParam(description = "Arbitrary text to search later") String text) {
        return run(() -> {
            tape.load(text);
            return "loaded " + tape.length() + " characters";
        });
    }

    @Tool(name = "tape_find", description = "Fold-then-exact search on the scratch tape. Returns line, offset, and snippet.")
    public String tapeFind(
            @ToolParam(description = "Motif; matching is case-insensitive after trim") String motif) {
        return run(() -> {
            var hits = tape.find(motif);
            if (hits.isEmpty()) {
                return "(no hits)";
            }
            StringBuilder out = new StringBuilder();
            for (var hit : hits) {
                out.append("line ").append(hit.line())
                        .append(" offset ").append(hit.offset())
                        .append(": ").append(hit.snippet()).append('\n');
            }
            return out.toString();
        });
    }

    @Tool(name = "tape_runs", description = "List consecutive identical-character runs on the scratch tape (homopolymer analog for any text).")
    public String tapeRuns(
            @ToolParam(description = "Minimum run length, at least 2") int minLength) {
        return run(() -> {
            int floor = minLength < 2 ? 2 : minLength;
            var runs = tape.runs(floor, 40);
            if (runs.isEmpty()) {
                return "(no runs)";
            }
            StringBuilder out = new StringBuilder();
            for (var run : runs) {
                out.append(run.symbol()).append(" x").append(run.length())
                        .append(" @").append(run.offset()).append('\n');
            }
            return out.toString();
        });
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
        } catch (StorageException | TapeException e) {
            return "Error: " + e.getMessage();
        }
    }

    @FunctionalInterface
    private interface ToolAction {
        String execute();
    }
}
