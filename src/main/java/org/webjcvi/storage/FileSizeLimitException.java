package org.webjcvi.storage;

/**
 * Thrown when a read or write would exceed the configured maximum file size.
 */
public class FileSizeLimitException extends StorageException {

    public FileSizeLimitException(String message) {
        super(message);
    }
}
