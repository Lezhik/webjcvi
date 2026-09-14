package org.webjcvi.dup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.webjcvi.tape.ScratchTape;

class DupScanTest {

    private final DupScan scan = new DupScan();

    @Test
    void distinctLinesHaveNoCopies() {
        String text = "AAAAAAAA11111111 one\nCCCCCCCC22222222 two\n";
        DupScan.Scan result = scan.profile(text);
        assertThat(result.nearAt()).isEqualTo(8);
        assertThat(result.copyGroups()).isZero();
        assertThat(result.forkGroups()).isZero();
        assertThat(result.copyShare()).isZero();
        assertThat(result.mostlyCopies()).isFalse();
    }

    @Test
    void leftoverIdenticalLinesCountAsCopies() {
        String text = "AAAAAAAA\nCCCCCCCC\nTTTTTTTT\nAAAACCCC\nAAAATTTT\n"
                + "CCCCAAAA\nCCCCTTTT\nTTTTAAAA\nGGGGGGGGTTTT leftover\nGGGGGGGGTTTT leftover\n";
        DupScan.Scan result = scan.profile(text);
        assertThat(result.nearAt()).isEqualTo(8);
        assertThat(result.copyGroups()).isEqualTo(1);
        assertThat(result.copyLines()).isEqualTo(2);
        assertThat(result.forkGroups()).isZero();
        assertThat(result.copyShare()).isEqualTo(1.0);
        assertThat(result.mostlyCopies()).isTrue();
        assertThat(result.minFork()).isZero();
        assertThat(result.copyShare()).isGreaterThan(DupScan.COPY_MAJORITY);
    }

    @Test
    void leftoverTwinsThatDifferCountAsForks() {
        String clock = "2026-09-13 21:56";
        String fill = "XXXXXX";
        String[] tails = {
                "A0YYYYYY",
                "AX1YYYYY",
                "AXXYYYYY",
                "BYY0YYYY",
                "BYYX1YYY",
                "BYYXXYYY",
                "CYYYY0YY",
                "CYYYYXYY",
                "DYYYYYYYAAAA00",
                "DYYYYYYYAAAA11"
        };
        StringBuilder text = new StringBuilder();
        for (String tail : tails) {
            text.append(clock).append(fill).append(tail).append('\n');
        }
        DupScan.Scan result = scan.profile(text.toString());
        assertThat(result.nearAt()).isEqualTo(28);
        assertThat(result.copyGroups()).isZero();
        assertThat(result.forkGroups()).isEqualTo(1);
        assertThat(result.forkLines()).isEqualTo(2);
        assertThat(result.copyShare()).isZero();
        assertThat(result.mostlyCopies()).isFalse();
        assertThat(result.minFork()).isEqualTo(35);
    }

    @Test
    void emptyTextHasNoCopies() {
        DupScan.Scan result = scan.profile("");
        assertThat(result.scanned()).isZero();
        assertThat(result.copyGroups()).isZero();
        assertThat(result.mostlyCopies()).isFalse();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> scan.profile(null))
                .isInstanceOf(DupException.class);
    }

    @Test
    void oversizeTextIsRejected() {
        String huge = "A".repeat(ScratchTape.MAX_CHARS + 1);
        assertThatThrownBy(() -> scan.profile(huge))
                .isInstanceOf(DupException.class)
                .hasMessageContaining(Integer.toString(ScratchTape.MAX_CHARS));
    }
}
