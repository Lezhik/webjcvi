package org.webjcvi.residue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.webjcvi.tape.ScratchTape;

class ResidueScanTest {

    private final ResidueScan scan = new ResidueScan();

    @Test
    void distinctLinesHaveNoResidue() {
        String text = "AAAAAAAA11111111 one\nCCCCCCCC22222222 two\n";
        ResidueScan.Scan result = scan.profile(text);
        assertThat(result.nearAt()).isEqualTo(8);
        assertThat(result.shareAtNear()).isEqualTo(1.0);
        assertThat(result.twinGroups()).isZero();
        assertThat(result.twinLines()).isZero();
        assertThat(result.residueShare()).isZero();
        assertThat(result.stretched()).isFalse();
        assertThat(result.splitLag()).isZero();
    }

    @Test
    void residualTwinsStretchPastNearUnique() {
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
        ResidueScan.Scan result = scan.profile(text.toString());
        assertThat(result.nearAt()).isEqualTo(28);
        assertThat(result.shareAtNear()).isGreaterThanOrEqualTo(0.90);
        assertThat(result.twinGroups()).isEqualTo(1);
        assertThat(result.twinLines()).isEqualTo(2);
        assertThat(result.residueShare()).isEqualTo(0.2);
        assertThat(result.modalSplit()).isEqualTo(35);
        assertThat(result.splitLag()).isEqualTo(7);
        assertThat(result.splitLag()).isGreaterThan(ResidueScan.DNA_TIGHT_LAG);
        assertThat(result.stretched()).isTrue();
        assertThat(result.nearAt()).isGreaterThan(ResidueScan.TILE);
    }

    @Test
    void emptyTextHasNoResidue() {
        ResidueScan.Scan result = scan.profile("");
        assertThat(result.scanned()).isZero();
        assertThat(result.nearAt()).isZero();
        assertThat(result.twinGroups()).isZero();
        assertThat(result.stretched()).isFalse();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> scan.profile(null))
                .isInstanceOf(ResidueException.class);
    }

    @Test
    void oversizeTextIsRejected() {
        String huge = "A".repeat(ScratchTape.MAX_CHARS + 1);
        assertThatThrownBy(() -> scan.profile(huge))
                .isInstanceOf(ResidueException.class)
                .hasMessageContaining(Integer.toString(ScratchTape.MAX_CHARS));
    }
}
