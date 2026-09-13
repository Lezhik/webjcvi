package org.webjcvi.fork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ForkScanTest {

    private final ForkScan forks = new ForkScan();

    @Test
    void sharedSixteenCharPrefixForksAtSeventeen() {
        String a = "ABCDEFGHIJKLMNOP" + "A".repeat(54);
        String b = "ABCDEFGHIJKLMNOP" + "B".repeat(54);
        ForkScan.Scan scan = forks.scan(a + b);
        assertThat(scan.scanned()).isEqualTo(2);
        assertThat(scan.twinCount()).isEqualTo(1);
        assertThat(scan.twinFrames()).isEqualTo(2);
        assertThat(scan.topFork()).isEqualTo(17);
        assertThat(scan.hits()).hasSize(1);
        assertThat(scan.hits().get(0).forkAt()).isEqualTo(17);
        assertThat(scan.hits().get(0).prefix()).isEqualTo("ABCDEFGHIJKLMNOP");
        assertThat(scan.prefixLength()).isEqualTo(16);
    }

    @Test
    void differentSixteenCharPrefixesAreNotTwins() {
        String a = "AAAAAAAAAAAAAAA" + "X" + "Y".repeat(54);
        String b = "BBBBBBBBBBBBBBB" + "Z" + "W".repeat(54);
        ForkScan.Scan scan = forks.scan(a + b);
        assertThat(scan.twinCount()).isZero();
        assertThat(scan.hits()).isEmpty();
        assertThat(scan.uniqueShare()).isEqualTo(1.0);
    }

    @Test
    void identicalFramesNeverFork() {
        String frame = "C".repeat(70);
        ForkScan.Scan scan = forks.scan(frame + frame);
        assertThat(scan.twinCount()).isEqualTo(1);
        assertThat(scan.topFork()).isZero();
        assertThat(scan.hits().get(0).forkAt()).isZero();
    }

    @Test
    void newlinesAreDroppedBeforeFraming() {
        String a = "ABCDEFGHIJKLMNOP" + "A".repeat(54);
        String b = "ABCDEFGHIJKLMNOP" + "B".repeat(54);
        String text = a.substring(0, 30) + "\n" + a.substring(30) + b;
        ForkScan.Scan scan = forks.scan(text);
        assertThat(scan.twinCount()).isEqualTo(1);
        assertThat(scan.topFork()).isEqualTo(17);
    }

    @Test
    void shortTextHasNoFrames() {
        ForkScan.Scan scan = forks.scan("short");
        assertThat(scan.scanned()).isZero();
        assertThat(scan.twinCount()).isZero();
    }

    @Test
    void emptyTextHasNoFrames() {
        ForkScan.Scan scan = forks.scan("");
        assertThat(scan.scanned()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> forks.scan(null))
                .isInstanceOf(ForkException.class);
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(ForkScan.MAX_CHARS + 1);
        assertThatThrownBy(() -> forks.scan(huge))
                .isInstanceOf(ForkException.class)
                .hasMessageContaining("exceeds");
    }
}
