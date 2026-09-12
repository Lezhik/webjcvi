package org.webjcvi.drift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class PairDriftTest {

    private final PairDrift drift = new PairDrift();

    @Test
    void balancedWindowIsNotAHotspot() {
        String text = "()".repeat(35);
        PairDrift.Scan scan = drift.scan(text, 70, 0.05);
        assertThat(scan.windowCount()).isEqualTo(1);
        assertThat(scan.hotspotCount()).isZero();
        assertThat(scan.globalSkew()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void localImbalanceIsAHotspotEvenIfLaterWindowsCompensate() {
        String text = "((((((((((          " + "))))))))))          ";
        PairDrift.Scan scan = drift.scan(text, 20, 0.05);
        assertThat(scan.windowCount()).isEqualTo(2);
        assertThat(scan.hotspotCount()).isEqualTo(2);
        assertThat(scan.hotspots().get(0).opens()).isEqualTo(10);
        assertThat(scan.hotspots().get(0).closes()).isZero();
        assertThat(scan.hotspots().get(1).opens()).isZero();
        assertThat(scan.hotspots().get(1).closes()).isEqualTo(10);
        assertThat(scan.globalSkew()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void windowsWithoutBracketsAreNotHotspots() {
        PairDrift.Scan scan = drift.scan("just a log line", 70, 0.05);
        assertThat(scan.hotspotCount()).isZero();
        assertThat(scan.opens()).isZero();
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(PairDrift.MAX_CHARS + 1);
        assertThatThrownBy(() -> drift.scan(huge))
                .isInstanceOf(DriftException.class)
                .hasMessageContaining("exceeds");
    }
}
