package org.webjcvi.runway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.webjcvi.tape.ScratchTape;

class RunwayScanTest {

    private final RunwayScan scan = new RunwayScan();

    @Test
    void timestampTwinsHaveZeroRunway() {
        String text = "2026-09-13 21:56 worker-a\n2026-09-13 21:56 worker-b\n";
        RunwayScan.Scan result = scan.profile(text);
        assertThat(result.forkAt()).isEqualTo(25);
        assertThat(result.cliffAt()).isEqualTo(25);
        assertThat(result.runway()).isZero();
        assertThat(result.stretched()).isFalse();
    }

    @Test
    void earlyForkLateCliffIsStretched() {
        StringBuilder text = new StringBuilder();
        String clock = "2026-09-13 21:56";
        for (int i = 0; i < 20; i++) {
            char split = (i % 2 == 0) ? 'A' : 'B';
            text.append(clock).append('X').append(split)
                    .append("Y".repeat(30))
                    .append(String.format("%02d", i))
                    .append('\n');
        }
        RunwayScan.Scan result = scan.profile(text.toString());
        assertThat(result.forkAt()).isEqualTo(18);
        assertThat(result.cliffAt()).isEqualTo(50);
        assertThat(result.runway()).isEqualTo(32);
        assertThat(result.stretched()).isTrue();
        assertThat(result.runway()).isGreaterThan(RunwayScan.DNA_UNIQUE_AT);
    }

    @Test
    void identicalLinesHaveNoRunway() {
        String text = "2026-09-13 21:56 worker-a\n2026-09-13 21:56 worker-a\n";
        RunwayScan.Scan result = scan.profile(text);
        assertThat(result.forkAt()).isZero();
        assertThat(result.cliffAt()).isZero();
        assertThat(result.runway()).isZero();
        assertThat(result.stretched()).isFalse();
    }

    @Test
    void emptyTextHasNoRows() {
        RunwayScan.Scan result = scan.profile("");
        assertThat(result.scanned()).isZero();
        assertThat(result.runway()).isZero();
        assertThat(result.stretched()).isFalse();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> scan.profile(null))
                .isInstanceOf(RunwayException.class);
    }

    @Test
    void oversizeTextIsRejected() {
        String huge = "A".repeat(ScratchTape.MAX_CHARS + 1);
        assertThatThrownBy(() -> scan.profile(huge))
                .isInstanceOf(RunwayException.class)
                .hasMessageContaining(Integer.toString(ScratchTape.MAX_CHARS));
    }
}
