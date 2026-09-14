package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LineMajorityCensusTest {

    @Test
    void emptyInputHasNoMajority() {
        LineMajorityCensus census = LineMajorityCensus.fromRaw("");
        assertThat(census.lines()).isZero();
        assertThat(census.majorityAt()).isZero();
        assertThat(census.shareAtMajority()).isZero();
    }

    @Test
    void distinctShortLinesReachMajorityAtFloor() {
        LineMajorityCensus census = LineMajorityCensus.fromRaw("AAAAAAAA\nCCCCCCCC\n");
        assertThat(census.lines()).isEqualTo(2);
        assertThat(census.majorityAt()).isEqualTo(8);
        assertThat(census.shareAtMajority()).isEqualTo(1.0);
        assertThat(census.riseAt()).isEqualTo(8);
    }
}
