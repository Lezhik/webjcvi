package org.webjcvi.tape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScratchTapeTest {

    private ScratchTape tape;

    @BeforeEach
    void setUp() {
        tape = new ScratchTape();
    }

    @Test
    void findFoldsCaseAndReportsLineOffsets() {
        tape.load("Hello\nhello");
        var hits = tape.find(" HELLO ");
        assertThat(hits).hasSize(2);
        assertThat(hits.getFirst().line()).isEqualTo(1);
        assertThat(hits.getFirst().offset()).isZero();
        assertThat(hits.get(1).line()).isEqualTo(2);
    }

    @Test
    void runsDetectRepeatedSymbols() {
        tape.load("xxAAAyy");
        var runs = tape.runs(2, 10);
        assertThat(runs).extracting(ScratchTape.Run::symbol).containsExactly('X', 'A', 'Y');
        assertThat(runs.get(1).length()).isEqualTo(3);
        assertThat(runs.get(1).offset()).isEqualTo(2);
    }

    @Test
    void emptyMotifIsRejected() {
        tape.load("abc");
        assertThatThrownBy(() -> tape.find("  "))
                .isInstanceOf(TapeException.class);
    }

    @Test
    void oversizeLoadIsRejected() {
        String huge = "x".repeat(ScratchTape.MAX_CHARS + 1);
        assertThatThrownBy(() -> tape.load(huge))
                .isInstanceOf(TapeException.class);
    }

    @Test
    void emptyLoadIsAllowedAndClearResets() {
        tape.load("");
        assertThat(tape.isEmpty()).isTrue();
        tape.load("abc");
        tape.clear();
        assertThat(tape.length()).isZero();
    }

    @Test
    void nullLoadIsRejected() {
        assertThatThrownBy(() -> tape.load(null))
                .isInstanceOf(TapeException.class);
    }
}
