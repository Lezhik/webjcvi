package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReversePhaseCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptyInputHasNoWindows() {
        ReversePhaseCensus census = ReversePhaseCensus.from(parser.parse(""), 70);
        assertThat(census.windows()).isZero();
        assertThat(census.reverseZero()).isZero();
    }

    @Test
    void reverseJointAtPhaseZeroIsThePeak() {
        ReversePhaseCensus census = ReversePhaseCensus.from(parser.parse("ATGCCGTA"), 70);
        assertThat(census.windows()).isEqualTo(1);
        assertThat(census.reverseZero()).isEqualTo(1);
        assertThat(census.peakPhase()).isZero();
        assertThat(census.peakShare()).isEqualTo(1.0);
        assertThat(census.wrapPhase()).isEqualTo(66);
        assertThat(census.wrapPhaseShare()).isZero();
    }

    @Test
    void nonMirrorHasZeroReverseShare() {
        ReversePhaseCensus census = ReversePhaseCensus.from(parser.parse("AAAATTTT"), 70);
        assertThat(census.windows()).isEqualTo(1);
        assertThat(census.reverseZero()).isZero();
        assertThat(census.meanShare()).isZero();
    }
}
