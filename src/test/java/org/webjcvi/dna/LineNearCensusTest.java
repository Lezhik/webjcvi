package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LineNearCensusTest {

    @Test
    void emptyInputHasNoNear() {
        LineNearCensus census = LineNearCensus.fromRaw("");
        assertThat(census.lines()).isZero();
        assertThat(census.nearAt()).isZero();
        assertThat(census.shareAtNear()).isZero();
    }

    @Test
    void distinctShortLinesReachNearAtFloor() {
        LineNearCensus census = LineNearCensus.fromRaw("AAAAAAAA\nCCCCCCCC\n");
        assertThat(census.lines()).isEqualTo(2);
        assertThat(census.nearAt()).isEqualTo(8);
        assertThat(census.shareAtNear()).isEqualTo(1.0);
        assertThat(census.majorityAt()).isEqualTo(8);
    }
}
