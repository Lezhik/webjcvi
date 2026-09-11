package org.webjcvi.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Sole filesystem gateway for the application. Every other component must go
 * through this class; it enforces the project-root sandbox and the 500 MB
 * per-file size limit on both reads and writes.
 */
public final class FileStorageService {

    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 500L * 1024L * 1024L;

    /** DNA input must remain read-only from the application's perspective (TZ §4.1). */
    public static final String DNA_FILE_NAME = "jcvi-dna.txt";

    private final Path projectRoot;
    private final long maxFileSizeBytes;

    public FileStorageService(Path projectRoot) {
        this(projectRoot, DEFAULT_MAX_FILE_SIZE_BYTES);
    }

    public FileStorageService(Path projectRoot, long maxFileSizeBytes) {
        Objects.requireNonNull(projectRoot, "projectRoot");
        if (maxFileSizeBytes <= 0) {
            throw new IllegalArgumentException("maxFileSizeBytes must be positive");
        }
        try {
            this.projectRoot = projectRoot.toAbsolutePath().normalize();
            if (!Files.exists(this.projectRoot, LinkOption.NOFOLLOW_LINKS)) {
                throw new StorageException("Project root does not exist: " + this.projectRoot);
            }
            if (!Files.isDirectory(this.projectRoot, LinkOption.NOFOLLOW_LINKS)) {
                throw new StorageException("Project root is not a directory: " + this.projectRoot);
            }
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException("Unable to initialize project root: " + projectRoot, e);
        }
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public Path projectRoot() {
        return projectRoot;
    }

    public long maxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public boolean exists(String relativePath) {
        Path resolved = resolveExisting(relativePath, false);
        return Files.exists(resolved, LinkOption.NOFOLLOW_LINKS);
    }

    public long size(String relativePath) {
        Path resolved = resolveExisting(relativePath, true);
        try {
            long size = Files.size(resolved);
            enforceSizeLimit(relativePath, size, "read");
            return size;
        } catch (IOException e) {
            throw wrapIo("Unable to read size of '" + relativePath + "'", e);
        }
    }

    public String readText(String relativePath) {
        return new String(readBytes(relativePath), StandardCharsets.UTF_8);
    }

    public byte[] readBytes(String relativePath) {
        Path resolved = resolveExisting(relativePath, true);
        if (Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
            throw new StorageException("Cannot read directory as a file: " + relativePath);
        }
        try {
            long size = Files.size(resolved);
            enforceSizeLimit(relativePath, size, "read");
            return Files.readAllBytes(resolved);
        } catch (IOException e) {
            throw wrapIo("Unable to read '" + relativePath + "'", e);
        }
    }

    public void writeText(String relativePath, String content) {
        Objects.requireNonNull(content, "content");
        writeBytes(relativePath, content.getBytes(StandardCharsets.UTF_8));
    }

    public void writeBytes(String relativePath, byte[] content) {
        Objects.requireNonNull(content, "content");
        rejectProtectedWrite(relativePath);
        enforceSizeLimit(relativePath, content.length, "write");
        Path resolved = resolveForWrite(relativePath);
        try {
            Path parent = resolved.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(resolved, content);
        } catch (IOException e) {
            throw wrapIo("Unable to write '" + relativePath + "'", e);
        }
    }

    /**
     * Deletes a file. Missing paths are a no-op so regeneration can clear a
     * report directory that does not yet exist.
     */
    public void delete(String relativePath) {
        rejectProtectedWrite(relativePath);
        Path resolved = resolveExisting(relativePath, false);
        if (!Files.exists(resolved, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
            throw new StorageException("Refusing to delete a directory via delete(): " + relativePath);
        }
        try {
            Files.delete(resolved);
        } catch (IOException e) {
            throw wrapIo("Unable to delete '" + relativePath + "'", e);
        }
    }

    /**
     * Deletes every regular file under {@code relativeDirectory}, leaving the
     * directory itself in place. Used to regenerate reports from scratch.
     */
    public void deleteContents(String relativeDirectory) {
        Path resolved = resolveExisting(relativeDirectory, false);
        if (!Files.exists(resolved, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (!Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
            throw new StorageException("Not a directory: " + relativeDirectory);
        }
        assertInsideSandbox(resolved, relativeDirectory);
        try (Stream<Path> walk = Files.walk(resolved)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(resolved))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw wrapIo("Unable to delete '" + toPublicPath(path) + "'", e);
                        }
                    });
        } catch (StorageException e) {
            throw e;
        } catch (IOException e) {
            throw wrapIo("Unable to clear '" + relativeDirectory + "'", e);
        }
    }

    public List<String> list(String relativeDirectory) {
        return list(relativeDirectory, false);
    }

    public List<String> listRecursive(String relativeDirectory) {
        return list(relativeDirectory, true);
    }

    public List<String> list(String relativeDirectory, boolean recursive) {
        Path resolved = resolveExisting(relativeDirectory == null || relativeDirectory.isBlank() ? "." : relativeDirectory, false);
        if (!Files.exists(resolved, LinkOption.NOFOLLOW_LINKS)) {
            return List.of();
        }
        if (!Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
            throw new StorageException("Not a directory: " + relativeDirectory);
        }
        assertInsideSandbox(resolved, relativeDirectory);
        List<String> names = new ArrayList<>();
        try {
            if (recursive) {
                try (Stream<Path> walk = Files.walk(resolved)) {
                    walk.filter(path -> !path.equals(resolved))
                            .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                            .map(this::toPublicPath)
                            .sorted()
                            .forEach(names::add);
                }
            } else {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(resolved)) {
                    for (Path child : stream) {
                        names.add(toPublicPath(child));
                    }
                    names.sort(String::compareTo);
                }
            }
        } catch (IOException e) {
            throw wrapIo("Unable to list '" + relativeDirectory + "'", e);
        }
        return List.copyOf(names);
    }

    private void rejectProtectedWrite(String relativePath) {
        String publicPath = toSlashPath(relativePath);
        if (DNA_FILE_NAME.equalsIgnoreCase(publicPath)) {
            throw new StorageException("The DNA file is read-only: " + DNA_FILE_NAME);
        }
    }

    private void enforceSizeLimit(String relativePath, long sizeBytes, String operation) {
        if (sizeBytes > maxFileSizeBytes) {
            throw new FileSizeLimitException(
                    "File '" + relativePath + "' exceeds the " + maxFileSizeBytes
                            + " byte limit on " + operation + " (size=" + sizeBytes + ")");
        }
    }

    /**
     * Resolves a path that is expected to already exist (or may be missing when
     * {@code mustExist} is false). Follows the real path of existing files to
     * reject symlink escapes.
     */
    private Path resolveExisting(String relativePath, boolean mustExist) {
        Path candidate = resolveAgainstRoot(relativePath);
        if (!Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            if (mustExist) {
                throw new StorageNotFoundException("File not found: " + relativePath);
            }
            return candidate;
        }
        assertInsideSandbox(candidate, relativePath);
        return candidate;
    }

    private Path resolveForWrite(String relativePath) {
        Path candidate = resolveAgainstRoot(relativePath);
        Path probe = candidate;
        while (probe != null && !Files.exists(probe, LinkOption.NOFOLLOW_LINKS)) {
            probe = probe.getParent();
        }
        if (probe == null) {
            throw new PathEscapeException("Path escapes the project root: " + relativePath);
        }
        assertInsideSandbox(probe, relativePath);
        return candidate;
    }

    private Path resolveAgainstRoot(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return projectRoot;
        }
        String trimmed = relativePath.trim();
        if (trimmed.indexOf('\0') >= 0) {
            throw new PathEscapeException("Path contains a NUL character");
        }
        Path incoming = Path.of(trimmed);
        if (incoming.isAbsolute()) {
            throw new PathEscapeException("Absolute paths are not allowed: " + relativePath);
        }
        if (incoming.getRoot() != null) {
            throw new PathEscapeException("Paths with a root component are not allowed: " + relativePath);
        }
        Path resolved = projectRoot.resolve(incoming).normalize();
        if (!isInsideRoot(resolved, projectRoot)) {
            throw new PathEscapeException("Path escapes the project root: " + relativePath);
        }
        return resolved;
    }

    private void assertInsideSandbox(Path path, String original) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!isInsideRoot(normalized, projectRoot)) {
            throw new PathEscapeException("Path escapes the project root: " + original);
        }
        try {
            if (Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
                Path real = normalized.toRealPath();
                Path realRoot = projectRoot.toRealPath();
                if (!isInsideRoot(real, realRoot)) {
                    throw new PathEscapeException("Symlink escapes the project root: " + original);
                }
            }
        } catch (PathEscapeException e) {
            throw e;
        } catch (IOException e) {
            throw wrapIo("Unable to resolve real path for '" + original + "'", e);
        }
    }

    private static boolean isInsideRoot(Path candidate, Path root) {
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        Path normalizedRoot = root.toAbsolutePath().normalize();
        return normalizedCandidate.startsWith(normalizedRoot);
    }

    private String toPublicPath(Path path) {
        Path relative = projectRoot.relativize(path.toAbsolutePath().normalize());
        return toSlashPath(relative.toString());
    }

    private static String toSlashPath(String path) {
        return path.replace('\\', '/');
    }

    private static StorageException wrapIo(String message, IOException e) {
        return new StorageException(message, e);
    }
}
