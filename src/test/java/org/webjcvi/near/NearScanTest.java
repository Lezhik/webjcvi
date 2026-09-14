package org.webjcvi.near;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.webjcvi.tape.ScratchTape;

class NearScanTest {

    private final NearScan scan = new NearScan();

    @Test
    void distinctLinesReachNearAtFloor() {
        String text = "AAAAAAAA11111111 one\nCCCCCCCC22222222 two\n";
        NearScan.Scan result = scan.profile(text);
        assertThat(result.nearAt()).isEqualTo(8);
        assertThat(result.shareAtNear()).isEqualTo(1.0);
        assertThat(result.majorityAt()).isEqualTo(8);
        assertThat(result.pastMajority()).isFalse();
        assertThat(result.lag()).isZero();
    }

    @Test
    void nearUniqueSitsPastABareMajority() {
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
                "DYYYYYYY",
                "DYYYYYYY"
        };
        StringBuilder text = new StringBuilder();
        for (String tail : tails) {
            text.append(clock).append(fill).append(tail).append('\n');
        }
        NearScan.Scan result = scan.profile(text.toString());
        assertThat(result.majorityAt()).isEqualTo(24);
        assertThat(result.shareAtMajority()).isGreaterThanOrEqualTo(0.5);
        assertThat(result.shareAtMajority()).isLessThan(0.90);
        assertThat(result.nearAt()).isEqualTo(28);
        assertThat(result.shareAtNear()).isGreaterThanOrEqualTo(0.90);
        assertThat(result.pastMajority()).isTrue();
        assertThat(result.lag()).isEqualTo(4);
        assertThat(result.nearAt()).isGreaterThan(NearScan.TILE);
    }

    @Test
    void emptyTextHasNoNear() {
        NearScan.Scan result = scan.profile("");
        assertThat(result.scanned()).isZero();
        assertThat(result.nearAt()).isZero();
        assertThat(result.pastMajority()).isFalse();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> scan.profile(null))
                .isInstanceOf(NearException.class);
    }

    @Test
    void oversizeTextIsRejected() {
        String huge = "A".repeat(ScratchTape.MAX_CHARS + 1);
        assertThatThrownBy(() -> scan.profile(huge))
                .isInstanceOf(NearException.class)
                .hasMessageContaining(Integer.toString(ScratchTape.MAX_CHARS));
    }
}
