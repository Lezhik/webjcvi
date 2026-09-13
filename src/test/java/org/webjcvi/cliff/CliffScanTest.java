package org.webjcvi.cliff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.webjcvi.tape.ScratchTape;

class CliffScanTest {

    private final CliffScan scan = new CliffScan();

    @Test
    void timestampTwinsForkPastSixteen() {
        String text = "2026-09-13 21:56 worker-a\n2026-09-13 21:56 worker-b\n";
        CliffScan.Scan result = scan.profile(text);
        assertThat(result.lineCount()).isEqualTo(2);
        assertThat(result.scanned()).isEqualTo(2);
        assertThat(result.forkAt()).isEqualTo(25);
        assertThat(result.cliffAt()).isEqualTo(25);
        assertThat(result.shareAt16()).isEqualTo(0.5);
        assertThat(result.topPrefix()).isEqualTo("2026-09-13 21:56");
    }

    @Test
    void identicalLinesHaveNoFork() {
        String text = "2026-09-13 21:56 worker-a\n2026-09-13 21:56 worker-a\n";
        CliffScan.Scan result = scan.profile(text);
        assertThat(result.forkAt()).isZero();
        assertThat(result.cliffAt()).isZero();
        assertThat(result.shareAt16()).isEqualTo(0.5);
    }

    @Test
    void distinctLinesCliffAtFloor() {
        String text = "AAAAAAAA11111111 one\nCCCCCCCC22222222 two\n";
        CliffScan.Scan result = scan.profile(text);
        assertThat(result.forkAt()).isZero();
        assertThat(result.cliffAt()).isEqualTo(8);
        assertThat(result.shareAt16()).isEqualTo(1.0);
    }

    @Test
    void emptyTextHasNoRows() {
        CliffScan.Scan result = scan.profile("");
        assertThat(result.scanned()).isZero();
        assertThat(result.forkAt()).isZero();
        assertThat(result.cliffAt()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> scan.profile(null))
                .isInstanceOf(CliffException.class);
    }

    @Test
    void oversizeTextIsRejected() {
        String huge = "A".repeat(ScratchTape.MAX_CHARS + 1);
        assertThatThrownBy(() -> scan.profile(huge))
                .isInstanceOf(CliffException.class)
                .hasMessageContaining(Integer.toString(ScratchTape.MAX_CHARS));
    }
}
