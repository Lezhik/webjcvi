package org.webjcvi.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.webjcvi.storage.FileSizeLimitException;
import org.webjcvi.storage.PathEscapeException;
import org.webjcvi.storage.StorageException;
import org.webjcvi.storage.StorageNotFoundException;
import org.webjcvi.tape.TapeException;

@RestControllerAdvice
public class StorageExceptionHandler {

    @ExceptionHandler(TapeException.class)
    ResponseEntity<Map<String, String>> tape(TapeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "tape", "message", ex.getMessage()));
    }

    @ExceptionHandler(PathEscapeException.class)
    ResponseEntity<Map<String, String>> pathEscape(PathEscapeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "path_escape", "message", ex.getMessage()));
    }

    @ExceptionHandler(FileSizeLimitException.class)
    ResponseEntity<Map<String, String>> sizeLimit(FileSizeLimitException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "file_size_limit", "message", ex.getMessage()));
    }

    @ExceptionHandler(StorageNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(StorageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "not_found", "message", ex.getMessage()));
    }

    @ExceptionHandler(StorageException.class)
    ResponseEntity<Map<String, String>> storage(StorageException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "storage", "message", ex.getMessage()));
    }
}
