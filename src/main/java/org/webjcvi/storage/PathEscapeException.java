package org.webjcvi.storage;

/**
 * Thrown when a requested path would escape the project-root sandbox
 * (absolute path, {@code ..} traversal, or symlink escape).
 */
public class PathEscapeException extends StorageException {

    public PathEscapeException(String message) {
        super(message);
    }
}
