package org.webjcvi.rise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.webjcvi.tape.ScratchTape;

class RiseScanTest {

    private final RiseScan scan = new RiseScan();

    @Test
    void distinctLinesRiseAtFloor() {
        String text = "AAAAAAAA11111111 one\nCCCCCCCC22222222 two\n";
        RiseScan.Scan result = scan.profile(text);
        assertThat(result.riseAt()).isEqualTo(8);
        assertThat(result.gain()).isEqualTo(1.0);
        assertThat(result.pastClock()).isFalse();
        assertThat(result.cliffAt()).isEqualTo(8);
    }

    @Test
    void lateUniquenessRiseIsPastTheClock() {
        StringBuilder text = new StringBuilder();
        String clock = "2026-09-13 21:56";
        for (int i = 0; i < 20; i++) {
            char split = (i % 2 == 0) ? 'A' : 'B';
            text.append(clock).append('X').append(split)
                    .append("Y".repeat(30))
                    .append(String.format("%02d", i))
                    .append('\n');
        }
        RiseScan.Scan result = scan.profile(text.toString());
        assertThat(result.riseAt()).isEqualTo(50);
        assertThat(result.gain()).isGreaterThan(0.5);
        assertThat(result.pastClock()).isTrue();
        assertThat(result.riseAt()).isGreaterThan(RiseScan.TILE);
    }

    @Test
    void emptyTextHasNoRise() {
        RiseScan.Scan result = scan.profile("");
        assertThat(result.scanned()).isZero();
        assertThat(result.riseAt()).isZero();
        assertThat(result.pastClock()).isFalse();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> scan.profile(null))
                .isInstanceOf(RiseException.class);
    }

    @Test
    void oversizeTextIsRejected() {
        String huge = "A".repeat(ScratchTape.MAX_CHARS + 1);
        assertThatThrownBy(() -> scan.profile(huge))
                .isInstanceOf(RiseException.class)
                .hasMessageContaining(Integer.toString(ScratchTape.MAX_CHARS));
    }
}
