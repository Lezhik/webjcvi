package org.webjcvi.storage;

/**
 * Thrown when a storage operation fails for a reason other than a more specific
 * sandbox or size-limit violation.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
