package org.webjcvi.rare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RareClassScannerTest {

    private final RareClassScanner scanner = new RareClassScanner();

    @Test
    void gcLikeMinorityFormsIslandsAmongAs() {
        String text = "AAAAAAAAAA GCGCGC AAAAAAAAAA";
        RareClassScanner.Scan scan = scanner.scan(text, 3);
        assertThat(scan.rareClass()).containsAnyOf("G", "C");
        assertThat(scan.islandCount()).isGreaterThanOrEqualTo(1);
        assertThat(scan.islands().get(0).preview()).contains("GC");
    }

    @Test
    void majorityOnlyHasNoRareIslandsOfLengthThree() {
        RareClassScanner.Scan scan = scanner.scan("AAAAAAAAAA", 3);
        assertThat(scan.rareClass()).isEmpty();
        assertThat(scan.islandCount()).isZero();
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(RareClassScanner.MAX_CHARS + 1);
        assertThatThrownBy(() -> scanner.scan(huge))
                .isInstanceOf(RareException.class)
                .hasMessageContaining("exceeds");
    }
}
