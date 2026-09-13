package org.webjcvi.lane;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LaneScanTest {

    private final LaneScan lanes = new LaneScan();

    @Test
    void interiorPeakWhenOnlyTheCenterDiffers() {
        String a = "T".repeat(35) + "A".repeat(8) + "T".repeat(27);
        String b = "T".repeat(35) + "C".repeat(8) + "T".repeat(27);
        LaneScan.Scan scan = lanes.profile(a + b);
        assertThat(scan.scanned()).isEqualTo(2);
        assertThat(scan.tileLength()).isEqualTo(16);
        assertThat(scan.peakAt()).isEqualTo(27);
        assertThat(scan.peakShare()).isEqualTo(1.0);
        assertThat(scan.troughAt()).isEqualTo(0);
        assertThat(scan.troughShare()).isEqualTo(0.5);
        assertThat(scan.spread()).isEqualTo(0.5);
    }

    @Test
    void leadPeakWhenOnlyThePrefixDiffers() {
        String a = "A".repeat(16) + "T".repeat(54);
        String b = "C".repeat(16) + "T".repeat(54);
        LaneScan.Scan scan = lanes.profile(a + b);
        assertThat(scan.peakAt()).isEqualTo(0);
        assertThat(scan.peakShare()).isEqualTo(1.0);
        assertThat(scan.troughShare()).isEqualTo(0.5);
        assertThat(scan.spread()).isEqualTo(0.5);
    }

    @Test
    void identicalFramesHaveZeroSpread() {
        String frame = "G".repeat(70);
        LaneScan.Scan scan = lanes.profile(frame + frame);
        assertThat(scan.spread()).isZero();
        assertThat(scan.peakShare()).isEqualTo(0.5);
        assertThat(scan.troughShare()).isEqualTo(0.5);
    }

    @Test
    void newlinesAreDroppedBeforeFraming() {
        String a = "T".repeat(35) + "A".repeat(8) + "T".repeat(27);
        String b = "T".repeat(35) + "C".repeat(8) + "T".repeat(27);
        String text = a.substring(0, 30) + "\n" + a.substring(30) + b;
        LaneScan.Scan scan = lanes.profile(text);
        assertThat(scan.peakAt()).isEqualTo(27);
        assertThat(scan.spread()).isEqualTo(0.5);
    }

    @Test
    void shortTextHasNoFrames() {
        LaneScan.Scan scan = lanes.profile("short");
        assertThat(scan.scanned()).isZero();
        assertThat(scan.spread()).isZero();
    }

    @Test
    void emptyTextHasNoFrames() {
        LaneScan.Scan scan = lanes.profile("");
        assertThat(scan.scanned()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> lanes.profile(null))
                .isInstanceOf(LaneException.class);
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(LaneScan.MAX_CHARS + 1);
        assertThatThrownBy(() -> lanes.profile(huge))
                .isInstanceOf(LaneException.class)
                .hasMessageContaining("exceeds");
    }
}
