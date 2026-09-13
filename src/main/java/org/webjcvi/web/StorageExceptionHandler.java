package org.webjcvi.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.webjcvi.drift.DriftException;
import org.webjcvi.rare.RareException;
import org.webjcvi.reflow.ReflowException;
import org.webjcvi.segment.SegmentException;
import org.webjcvi.storage.FileSizeLimitException;
import org.webjcvi.storage.PathEscapeException;
import org.webjcvi.storage.StorageException;
import org.webjcvi.storage.StorageNotFoundException;
import org.webjcvi.tape.TapeException;
import org.webjcvi.token.TokenException;
import org.webjcvi.stamp.StampException;
import org.webjcvi.fold.FoldException;
import org.webjcvi.loop.LoopException;
import org.webjcvi.fuzzy.FuzzyException;
import org.webjcvi.contrast.ContrastException;
import org.webjcvi.mirror.MirrorException;
import org.webjcvi.seam.SeamException;
import org.webjcvi.phase.PhaseException;
import org.webjcvi.frame.FrameException;
import org.webjcvi.clone.CloneException;
import org.webjcvi.prefix.PrefixException;
import org.webjcvi.key.KeyException;
import org.webjcvi.fork.ForkException;
import org.webjcvi.affix.AffixException;
import org.webjcvi.lane.LaneException;
import org.webjcvi.row.RowException;
import org.webjcvi.cliff.CliffException;
import org.webjcvi.runway.RunwayException;
import org.webjcvi.rise.RiseException;

@RestControllerAdvice
public class StorageExceptionHandler {

    @ExceptionHandler(TapeException.class)
    ResponseEntity<Map<String, String>> tape(TapeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "tape", "message", ex.getMessage()));
    }

    @ExceptionHandler(SegmentException.class)
    ResponseEntity<Map<String, String>> segment(SegmentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "segment", "message", ex.getMessage()));
    }

    @ExceptionHandler(DriftException.class)
    ResponseEntity<Map<String, String>> drift(DriftException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "drift", "message", ex.getMessage()));
    }

    @ExceptionHandler(ReflowException.class)
    ResponseEntity<Map<String, String>> reflow(ReflowException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "reflow", "message", ex.getMessage()));
    }

    @ExceptionHandler(RareException.class)
    ResponseEntity<Map<String, String>> rare(RareException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "rare", "message", ex.getMessage()));
    }

    @ExceptionHandler(TokenException.class)
    ResponseEntity<Map<String, String>> token(TokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "token", "message", ex.getMessage()));
    }

    @ExceptionHandler(StampException.class)
    ResponseEntity<Map<String, String>> stamp(StampException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "stamp", "message", ex.getMessage()));
    }

    @ExceptionHandler(FoldException.class)
    ResponseEntity<Map<String, String>> fold(FoldException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "fold", "message", ex.getMessage()));
    }

    @ExceptionHandler(LoopException.class)
    ResponseEntity<Map<String, String>> loop(LoopException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "loop", "message", ex.getMessage()));
    }

    @ExceptionHandler(FuzzyException.class)
    ResponseEntity<Map<String, String>> fuzzy(FuzzyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "fuzzy", "message", ex.getMessage()));
    }

    @ExceptionHandler(ContrastException.class)
    ResponseEntity<Map<String, String>> contrast(ContrastException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "contrast", "message", ex.getMessage()));
    }

    @ExceptionHandler(MirrorException.class)
    ResponseEntity<Map<String, String>> mirror(MirrorException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "mirror", "message", ex.getMessage()));
    }

    @ExceptionHandler(SeamException.class)
    ResponseEntity<Map<String, String>> seam(SeamException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "seam", "message", ex.getMessage()));
    }

    @ExceptionHandler(PhaseException.class)
    ResponseEntity<Map<String, String>> phase(PhaseException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "phase", "message", ex.getMessage()));
    }

    @ExceptionHandler(FrameException.class)
    ResponseEntity<Map<String, String>> frame(FrameException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "frame", "message", ex.getMessage()));
    }

    @ExceptionHandler(CloneException.class)
    ResponseEntity<Map<String, String>> clone(CloneException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "clone", "message", ex.getMessage()));
    }

    @ExceptionHandler(PrefixException.class)
    ResponseEntity<Map<String, String>> prefix(PrefixException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "prefix", "message", ex.getMessage()));
    }

    @ExceptionHandler(KeyException.class)
    ResponseEntity<Map<String, String>> key(KeyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "key", "message", ex.getMessage()));
    }

    @ExceptionHandler(ForkException.class)
    ResponseEntity<Map<String, String>> fork(ForkException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "fork", "message", ex.getMessage()));
    }

    @ExceptionHandler(AffixException.class)
    ResponseEntity<Map<String, String>> affix(AffixException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "affix", "message", ex.getMessage()));
    }

    @ExceptionHandler(LaneException.class)
    ResponseEntity<Map<String, String>> lane(LaneException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "lane", "message", ex.getMessage()));
    }

    @ExceptionHandler(RowException.class)
    ResponseEntity<Map<String, String>> row(RowException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "row", "message", ex.getMessage()));
    }

    @ExceptionHandler(CliffException.class)
    ResponseEntity<Map<String, String>> cliff(CliffException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "cliff", "message", ex.getMessage()));
    }

    @ExceptionHandler(RunwayException.class)
    ResponseEntity<Map<String, String>> runway(RunwayException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "runway", "message", ex.getMessage()));
    }

    @ExceptionHandler(RiseException.class)
    ResponseEntity<Map<String, String>> rise(RiseException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "rise", "message", ex.getMessage()));
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
