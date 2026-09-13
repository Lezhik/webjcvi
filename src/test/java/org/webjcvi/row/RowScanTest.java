package org.webjcvi.row;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.webjcvi.tape.ScratchTape;

class RowScanTest {

    private final RowScan scan = new RowScan();

    @Test
    void sharedTimestampPrefixCollidesAtSixteen() {
        String text = "2026-09-13 21:56 worker-a\n2026-09-13 21:56 worker-b\n";
        RowScan.Scan result = scan.profile(text);
        assertThat(result.lineCount()).isEqualTo(2);
        assertThat(result.scanned()).isEqualTo(2);
        assertThat(result.tileLength()).isEqualTo(16);
        assertThat(result.uniqueShareAt16()).isEqualTo(0.5);
        assertThat(result.twinCount()).isEqualTo(1);
        assertThat(result.twinLines()).isEqualTo(2);
        assertThat(result.topPrefix()).isEqualTo("2026-09-13 21:56");
        assertThat(result.uniqueAt()).isEqualTo(25);
    }

    @Test
    void distinctLinePrefixesSaturate() {
        String text = "AAAAAAAA11111111 one\nCCCCCCCC22222222 two\n";
        RowScan.Scan result = scan.profile(text);
        assertThat(result.uniqueShareAt16()).isEqualTo(1.0);
        assertThat(result.twinCount()).isZero();
        assertThat(result.uniqueAt()).isEqualTo(8);
    }

    @Test
    void blankLinesAreCountedButNotKeyed() {
        String text = "AAAAAAAA11111111\n\nCCCCCCCC22222222\n";
        RowScan.Scan result = scan.profile(text);
        assertThat(result.lineCount()).isEqualTo(3);
        assertThat(result.scanned()).isEqualTo(2);
        assertThat(result.uniqueShareAt16()).isEqualTo(1.0);
    }

    @Test
    void emptyTextHasNoRows() {
        RowScan.Scan result = scan.profile("");
        assertThat(result.lineCount()).isEqualTo(1);
        assertThat(result.scanned()).isZero();
        assertThat(result.uniqueShareAt16()).isZero();
        assertThat(result.uniqueAt()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> scan.profile(null))
                .isInstanceOf(RowException.class);
    }

    @Test
    void oversizeTextIsRejected() {
        String huge = "A".repeat(ScratchTape.MAX_CHARS + 1);
        assertThatThrownBy(() -> scan.profile(huge))
                .isInstanceOf(RowException.class)
                .hasMessageContaining(Integer.toString(ScratchTape.MAX_CHARS));
    }
}
