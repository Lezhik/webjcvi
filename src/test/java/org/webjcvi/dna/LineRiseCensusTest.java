package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LineRiseCensusTest {

    @Test
    void emptyInputHasNoRise() {
        LineRiseCensus census = LineRiseCensus.fromRaw("");
        assertThat(census.lines()).isZero();
        assertThat(census.riseAt()).isZero();
        assertThat(census.gain()).isZero();
    }

    @Test
    void distinctShortLinesRiseAtFloor() {
        LineRiseCensus census = LineRiseCensus.fromRaw("AAAAAAAA\nCCCCCCCC\n");
        assertThat(census.lines()).isEqualTo(2);
        assertThat(census.riseAt()).isEqualTo(8);
        assertThat(census.gain()).isEqualTo(1.0);
        assertThat(census.cliffAt()).isEqualTo(8);
    }
}
