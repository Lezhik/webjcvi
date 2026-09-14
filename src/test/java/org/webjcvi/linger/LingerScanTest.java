package org.webjcvi.linger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.webjcvi.tape.ScratchTape;

class LingerScanTest {

    private final LingerScan scan = new LingerScan();

    @Test
    void distinctLinesHaveNoLongTail() {
        String text = "AAAAAAAA11111111 one\nCCCCCCCC22222222 two\n";
        LingerScan.Scan result = scan.profile(text);
        assertThat(result.nearAt()).isEqualTo(8);
        assertThat(result.twinGroups()).isZero();
        assertThat(result.lagLong()).isZero();
        assertThat(result.longTail()).isFalse();
    }

    @Test
    void leftoverCopiesBinAtLag0WithoutALongTail() {
        String text = "AAAAAAAA\nCCCCCCCC\nTTTTTTTT\nAAAACCCC\nAAAATTTT\n"
                + "CCCCAAAA\nCCCCTTTT\nTTTTAAAA\nGGGGGGGGTTTT leftover\nGGGGGGGGTTTT leftover\n";
        LingerScan.Scan result = scan.profile(text);
        assertThat(result.nearAt()).isEqualTo(8);
        assertThat(result.lag0()).isEqualTo(1);
        assertThat(result.lagLong()).isZero();
        assertThat(result.longTail()).isFalse();
        assertThat(result.modalLag()).isZero();
        assertThat(result.mostlyCopies()).isTrue();
    }

    @Test
    void leftoverTwinsThatLingerPastDnaTightCountAsLongTail() {
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
        LingerScan.Scan result = scan.profile(text.toString());
        assertThat(result.nearAt()).isEqualTo(28);
        assertThat(result.lag0()).isZero();
        assertThat(result.lagLong()).isEqualTo(1);
        assertThat(result.maxLag()).isEqualTo(7);
        assertThat(result.maxLag()).isGreaterThan(LingerScan.DNA_TIGHT_LAG);
        assertThat(result.longTail()).isTrue();
        assertThat(result.modalLag()).isEqualTo(LingerScan.DNA_TIGHT_LAG + 1);
    }

    @Test
    void emptyTextHasNoLongTail() {
        LingerScan.Scan result = scan.profile("");
        assertThat(result.scanned()).isZero();
        assertThat(result.lagLong()).isZero();
        assertThat(result.longTail()).isFalse();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> scan.profile(null))
                .isInstanceOf(LingerException.class);
    }

    @Test
    void oversizeTextIsRejected() {
        String huge = "A".repeat(ScratchTape.MAX_CHARS + 1);
        assertThatThrownBy(() -> scan.profile(huge))
                .isInstanceOf(LingerException.class)
                .hasMessageContaining(Integer.toString(ScratchTape.MAX_CHARS));
    }
}
