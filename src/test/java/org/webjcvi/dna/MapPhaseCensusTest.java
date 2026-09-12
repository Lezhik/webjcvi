package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MapPhaseCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptyInputHasNoWindows() {
        MapPhaseCensus census = MapPhaseCensus.from(parser.parse(""), 70);
        assertThat(census.windows()).isZero();
        assertThat(census.reverse().peakShare()).isZero();
    }

    @Test
    void reverseJointAtPhaseZeroPeaksReverseThere() {
        MapPhaseCensus census = MapPhaseCensus.from(parser.parse("ATGCCGTA"), 70);
        assertThat(census.windows()).isEqualTo(1);
        assertThat(census.reverse().peakPhase()).isZero();
        assertThat(census.reverse().peakShare()).isEqualTo(1.0);
        assertThat(census.identity().peakShare()).isZero();
    }

    @Test
    void identityCopyPeaksIdentityNotReverse() {
        MapPhaseCensus census = MapPhaseCensus.from(parser.parse("ATGCATGC"), 70);
        assertThat(census.windows()).isEqualTo(1);
        assertThat(census.identity().peakShare()).isEqualTo(1.0);
        assertThat(census.reverse().peakShare()).isZero();
    }
}
