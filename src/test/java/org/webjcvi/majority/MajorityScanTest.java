package org.webjcvi.majority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.webjcvi.tape.ScratchTape;

class MajorityScanTest {

    private final MajorityScan scan = new MajorityScan();

    @Test
    void distinctLinesReachMajorityAtFloor() {
        String text = "AAAAAAAA11111111 one\nCCCCCCCC22222222 two\n";
        MajorityScan.Scan result = scan.profile(text);
        assertThat(result.majorityAt()).isEqualTo(8);
        assertThat(result.shareAtMajority()).isEqualTo(1.0);
        assertThat(result.riseAt()).isEqualTo(8);
        assertThat(result.pastRise()).isFalse();
        assertThat(result.lag()).isZero();
    }

    @Test
    void majoritySitsPastASteepIncompleteRise() {
        String clock = "2026-09-13 21:56";
        String fill = "XXXXXX";
        char[] groups = {'A', 'A', 'A', 'A', 'A', 'B', 'B', 'B', 'B', 'B',
                'C', 'C', 'C', 'C', 'D', 'D', 'E', 'E', 'F', 'F'};
        String[] tails = {
                "0YYY", "X1YY", "XX2Y", "XXX3", "XXXX",
                "XXXX", "XXXX", "XXXX", "XXXX", "XXXX",
                "XXXX", "XXXX", "XXXX", "XXXX",
                "XXXX", "XXXX",
                "XXXX", "XXXX",
                "XXXX", "XXXX"
        };
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < groups.length; i++) {
            text.append(clock).append(fill).append(groups[i]).append(tails[i]).append('\n');
        }
        MajorityScan.Scan result = scan.profile(text.toString());
        assertThat(result.riseAt()).isEqualTo(23);
        assertThat(result.shareAtRise()).isLessThan(0.5);
        assertThat(result.majorityAt()).isEqualTo(27);
        assertThat(result.shareAtMajority()).isGreaterThanOrEqualTo(0.5);
        assertThat(result.pastRise()).isTrue();
        assertThat(result.lag()).isEqualTo(4);
        assertThat(result.majorityAt()).isGreaterThan(MajorityScan.TILE);
    }

    @Test
    void emptyTextHasNoMajority() {
        MajorityScan.Scan result = scan.profile("");
        assertThat(result.scanned()).isZero();
        assertThat(result.majorityAt()).isZero();
        assertThat(result.pastRise()).isFalse();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> scan.profile(null))
                .isInstanceOf(MajorityException.class);
    }

    @Test
    void oversizeTextIsRejected() {
        String huge = "A".repeat(ScratchTape.MAX_CHARS + 1);
        assertThatThrownBy(() -> scan.profile(huge))
                .isInstanceOf(MajorityException.class)
                .hasMessageContaining(Integer.toString(ScratchTape.MAX_CHARS));
    }
}
