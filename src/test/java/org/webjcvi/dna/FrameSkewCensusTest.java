package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class FrameSkewCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptySequenceHasNoWindows() {
        FrameSkewCensus census = FrameSkewCensus.from(parser.parse(""), 70);
        assertThat(census.windowCount()).isZero();
        assertThat(census.windowsPastThreshold()).isZero();
    }

    @Test
    void balancedWindowHasZeroAtSkew() {
        FrameSkewCensus census = FrameSkewCensus.from(parser.parse("AT".repeat(35)), 70);
        assertThat(census.windowCount()).isEqualTo(1);
        assertThat(census.maxAbsAtSkew()).isCloseTo(0.0, within(1e-9));
        assertThat(census.windowsPastThreshold()).isZero();
    }

    @Test
    void oppositeFramesShowLocalChargaffFailure() {
        String raw = "A".repeat(70) + "T".repeat(70);
        FrameSkewCensus census = FrameSkewCensus.from(parser.parse(raw), 70);
        assertThat(census.windowCount()).isEqualTo(2);
        assertThat(census.maxAbsAtSkew()).isCloseTo(1.0, within(1e-9));
        assertThat(census.meanAbsAtSkew()).isCloseTo(1.0, within(1e-9));
        assertThat(census.windowsPastThreshold()).isEqualTo(2);
    }
}
