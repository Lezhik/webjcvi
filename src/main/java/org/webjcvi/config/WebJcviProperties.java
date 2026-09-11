package org.webjcvi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "webjcvi")
public class WebJcviProperties {

    private final Storage storage = new Storage();
    private final Dna dna = new Dna();
    private final Report report = new Report();

    public Storage getStorage() {
        return storage;
    }

    public Dna getDna() {
        return dna;
    }

    public Report getReport() {
        return report;
    }

    public static class Storage {
        /**
         * Absolute project root. Empty means {@code user.dir} (the directory the
         * process was started from, which Gradle/Boot set to the project root).
         */
        private String projectRoot = "";
        private long maxFileSizeBytes = 524_288_000L;

        public String getProjectRoot() {
            return projectRoot;
        }

        public void setProjectRoot(String projectRoot) {
            this.projectRoot = projectRoot;
        }

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }
    }

    public static class Dna {
        private String relativePath = "jcvi-dna.txt";

        public String getRelativePath() {
            return relativePath;
        }

        public void setRelativePath(String relativePath) {
            this.relativePath = relativePath;
        }
    }

    public static class Report {
        private String directory = "build/reports/dna";
        private String fileName = "dna-report.md";

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }
}
