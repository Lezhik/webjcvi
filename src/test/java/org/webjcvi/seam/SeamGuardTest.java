package org.webjcvi.seam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SeamGuardTest {

    private final SeamGuard seams = new SeamGuard();

    @Test
    void fullWidthReverseJointIsAHit() {
        String left = "x".repeat(66) + "ABCD";
        String right = "DCBA" + "y".repeat(66);
        SeamGuard.Scan scan = seams.scan(left + "\n" + right);
        assertThat(scan.scanned()).isEqualTo(1);
        assertThat(scan.hitCount()).isEqualTo(1);
        assertThat(scan.hits().get(0).left()).isEqualTo("ABCD");
        assertThat(scan.hits().get(0).right()).isEqualTo("DCBA");
        assertThat(scan.hits().get(0).identityDistance()).isEqualTo(4);
    }

    @Test
    void repeatedBlocksAtTheSeamAreNotHits() {
        String left = "x".repeat(66) + "AAAA";
        String right = "AAAA" + "y".repeat(66);
        SeamGuard.Scan scan = seams.scan(left + "\n" + right);
        assertThat(scan.scanned()).isEqualTo(1);
        assertThat(scan.hitCount()).isZero();
    }

    @Test
    void shortLinesAreNotWrapSeams() {
        SeamGuard.Scan scan = seams.scan("ABCD\nDCBA");
        assertThat(scan.scanned()).isZero();
        assertThat(scan.hitCount()).isZero();
    }

    @Test
    void emptyTextHasNoSeams() {
        SeamGuard.Scan scan = seams.scan("");
        assertThat(scan.scanned()).isZero();
        assertThat(scan.hitCount()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> seams.scan(null))
                .isInstanceOf(SeamException.class);
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(SeamGuard.MAX_CHARS + 1);
        assertThatThrownBy(() -> seams.scan(huge))
                .isInstanceOf(SeamException.class)
                .hasMessageContaining("exceeds");
    }
}
