package org.webjcvi.storage;

/**
 * Thrown when a requested file or directory does not exist inside the sandbox.
 */
public class StorageNotFoundException extends StorageException {

    public StorageNotFoundException(String message) {
        super(message);
    }
}
