package org.webjcvi.contrast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BlockContrastTest {

    private final BlockContrast contrast = new BlockContrast();

    @Test
    void repeatedBlocksAreStutters() {
        BlockContrast.Scan scan = contrast.scan("abcdabcd", 4, 1);
        assertThat(scan.scanned()).isEqualTo(1);
        assertThat(scan.stutterCount()).isEqualTo(1);
        assertThat(scan.modalDistance()).isZero();
        assertThat(scan.hits().get(0).left()).isEqualTo("ABCD");
        assertThat(scan.hits().get(0).right()).isEqualTo("ABCD");
    }

    @Test
    void contrastingNeighborsAreNotFlagged() {
        BlockContrast.Scan scan = contrast.scan("abcdwxyz", 4, 1);
        assertThat(scan.scanned()).isEqualTo(1);
        assertThat(scan.stutterCount()).isZero();
        assertThat(scan.modalDistance()).isEqualTo(4);
        assertThat(scan.meanDistance()).isEqualTo(4.0);
    }

    @Test
    void oneSubstitutionIsAStutterAtDefaultFlag() {
        BlockContrast.Scan scan = contrast.scan("abcdabce", 4, 1);
        assertThat(scan.stutterCount()).isEqualTo(1);
        assertThat(scan.hits().get(0).distance()).isEqualTo(1);
    }

    @Test
    void emptyTextHasNoWindows() {
        BlockContrast.Scan scan = contrast.scan("");
        assertThat(scan.scanned()).isZero();
        assertThat(scan.stutterCount()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> contrast.scan(null))
                .isInstanceOf(ContrastException.class);
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(BlockContrast.MAX_CHARS + 1);
        assertThatThrownBy(() -> contrast.scan(huge))
                .isInstanceOf(ContrastException.class)
                .hasMessageContaining("exceeds");
    }
}
