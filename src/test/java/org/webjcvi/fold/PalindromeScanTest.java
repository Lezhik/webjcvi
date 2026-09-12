package org.webjcvi.fold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PalindromeScanTest {

    private final PalindromeScan scan = new PalindromeScan();

    @Test
    void findsAbbaAndSkipsHomopolymer() {
        PalindromeScan.Scan result = scan.find("xx ABBA TTTT yy", 4);
        assertThat(result.hitCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.hits().stream().map(PalindromeScan.Hit::preview)).contains("ABBA");
        assertThat(result.hits().stream().map(PalindromeScan.Hit::preview)).doesNotContain("TTTT");
    }

    @Test
    void skipsLengthThreeXyx() {
        PalindromeScan.Scan result = scan.find("ABA", 4);
        assertThat(result.hitCount()).isZero();
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(PalindromeScan.MAX_CHARS + 1);
        assertThatThrownBy(() -> scan.find(huge))
                .isInstanceOf(FoldException.class)
                .hasMessageContaining("exceeds");
    }
}
