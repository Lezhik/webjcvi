package org.webjcvi.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileStorageServiceTest {

    @TempDir
    Path projectRoot;

    private FileStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new FileStorageService(projectRoot, 64);
    }

    @Test
    void productionDefaultLimitIs500Mb() {
        assertThat(FileStorageService.DEFAULT_MAX_FILE_SIZE_BYTES)
                .isEqualTo(500L * 1024L * 1024L);
    }

    @Test
    void writesAndReadsTextInsideTheSandbox() {
        storage.writeText("notes/hello.txt", "hello");
        assertThat(storage.readText("notes/hello.txt")).isEqualTo("hello");
        assertThat(storage.exists("notes/hello.txt")).isTrue();
        assertThat(storage.size("notes/hello.txt")).isEqualTo(5);
    }

    @Test
    void listAndListRecursiveReturnRelativeSlashPaths() {
        storage.writeText("a.txt", "a");
        storage.writeText("sub/b.txt", "b");
        assertThat(storage.list(".")).contains("a.txt", "sub");
        assertThat(storage.listRecursive(".")).containsExactly("a.txt", "sub/b.txt");
    }

    @Test
    void rejectsParentDirectoryTraversal() {
        assertThatThrownBy(() -> storage.readText("../secret.txt"))
                .isInstanceOf(PathEscapeException.class);
        assertThatThrownBy(() -> storage.writeText("../out.txt", "nope"))
                .isInstanceOf(PathEscapeException.class);
        assertThatThrownBy(() -> storage.list(".."))
                .isInstanceOf(PathEscapeException.class);
    }

    @Test
    void rejectsNestedTraversal() {
        storage.writeText("sub/ok.txt", "ok");
        assertThatThrownBy(() -> storage.readText("sub/../../secret.txt"))
                .isInstanceOf(PathEscapeException.class);
    }

    @Test
    void rejectsAbsolutePaths() {
        Path outside = projectRoot.getParent().resolve("outside.txt");
        assertThatThrownBy(() -> storage.readText(outside.toAbsolutePath().toString()))
                .isInstanceOf(PathEscapeException.class);
        assertThatThrownBy(() -> storage.writeText(outside.toAbsolutePath().toString(), "x"))
                .isInstanceOf(PathEscapeException.class);
    }

    @Test
    void symlinkEscapeIsRejected() throws IOException {
        Path outside = projectRoot.getParent().resolve("outside-secret.txt");
        Files.writeString(outside, "classified");
        Path link = projectRoot.resolve("escape-link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | IOException ex) {
            assumeTrue(false, "Symbolic links are not available: " + ex.getMessage());
        }
        assertThatThrownBy(() -> storage.readText("escape-link"))
                .isInstanceOf(PathEscapeException.class);
        assertThatThrownBy(() -> storage.readBytes("escape-link"))
                .isInstanceOf(PathEscapeException.class);
    }

    @Test
    void writeExactlyAtLimitSucceedsAndOneByteOverFails() {
        byte[] atLimit = new byte[64];
        storage.writeBytes("at-limit.bin", atLimit);
        assertThat(storage.size("at-limit.bin")).isEqualTo(64);

        byte[] over = new byte[65];
        assertThatThrownBy(() -> storage.writeBytes("over.bin", over))
                .isInstanceOf(FileSizeLimitException.class);
        assertThat(storage.exists("over.bin")).isFalse();
    }

    @Test
    void readRejectsExistingFileAboveLimit() throws IOException {
        Path huge = projectRoot.resolve("huge.bin");
        Files.write(huge, new byte[65]);
        assertThatThrownBy(() -> storage.readBytes("huge.bin"))
                .isInstanceOf(FileSizeLimitException.class);
        assertThatThrownBy(() -> storage.size("huge.bin"))
                .isInstanceOf(FileSizeLimitException.class);
    }

    @Test
    void missingFileThrowsNotFoundOnRead() {
        assertThatThrownBy(() -> storage.readText("nope.txt"))
                .isInstanceOf(StorageNotFoundException.class);
    }

    @Test
    void deleteOfMissingFileIsNoOp() {
        storage.delete("missing.txt");
    }

    @Test
    void deleteContentsClearsNestedFiles() {
        storage.writeText("build/reports/old.md", "old");
        storage.writeText("build/reports/nested/x.md", "x");
        storage.deleteContents("build/reports");
        assertThat(storage.listRecursive("build/reports")).isEmpty();
        assertThat(Files.isDirectory(projectRoot.resolve("build/reports"))).isTrue();
    }

    @Test
    void dnaFileIsReadOnly() throws IOException {
        Files.writeString(projectRoot.resolve("jcvi-dna.txt"), "ATGC");
        assertThat(storage.readText("jcvi-dna.txt")).isEqualTo("ATGC");
        assertThatThrownBy(() -> storage.writeText("jcvi-dna.txt", "GGGG"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("read-only");
        assertThatThrownBy(() -> storage.delete("jcvi-dna.txt"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("read-only");
        assertThat(storage.readText("jcvi-dna.txt")).isEqualTo("ATGC");
    }

    @Test
    void listMissingDirectoryReturnsEmpty() {
        assertThat(storage.list("no-such-dir")).isEmpty();
    }

    @Test
    void utf8RoundTrip() {
        String text = "αβγ\n";
        storage.writeText("utf8.txt", text);
        assertThat(storage.readText("utf8.txt")).isEqualTo(text);
        assertThat(storage.readBytes("utf8.txt")).isEqualTo(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void listNonDirectoryThrows() {
        storage.writeText("file.txt", "x");
        assertThatThrownBy(() -> storage.list("file.txt"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Not a directory");
    }

    @Test
    void emptyRelativePathResolvesToProjectRoot() {
        storage.writeText("root.txt", "r");
        List<String> names = storage.list("");
        assertThat(names).contains("root.txt");
    }
}
