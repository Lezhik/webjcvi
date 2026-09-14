package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LineLagCensusTest {

    @Test
    void emptyInputHasNoLag() {
        LineLagCensus census = LineLagCensus.fromRaw("");
        assertThat(census.lines()).isZero();
        assertThat(census.nearAt()).isZero();
        assertThat(census.twinGroups()).isZero();
        assertThat(census.modalLag()).isZero();
    }

    @Test
    void distinctShortLinesHaveNoLeftoverLag() {
        LineLagCensus census = LineLagCensus.fromRaw("AAAAAAAA\nCCCCCCCC\n");
        assertThat(census.nearAt()).isEqualTo(8);
        assertThat(census.twinGroups()).isZero();
        assertThat(census.lag0()).isZero();
        assertThat(census.lag1()).isZero();
    }

    @Test
    void identicalLeftoverLinesBinAtLag0() {
        LineLagCensus census = LineLagCensus.fromRaw(
                "AAAAAAAA\nCCCCCCCC\nTTTTTTTT\nAAAACCCC\nAAAATTTT\n"
                        + "CCCCAAAA\nCCCCTTTT\nTTTTAAAA\nGGGGGGGGTTTT\nGGGGGGGGTTTT\n");
        assertThat(census.nearAt()).isEqualTo(8);
        assertThat(census.twinGroups()).isEqualTo(1);
        assertThat(census.lag0()).isEqualTo(1);
        assertThat(census.lag1()).isZero();
        assertThat(census.lagLong()).isZero();
        assertThat(census.modalLag()).isZero();
    }

    @Test
    void leftoverTwinsThatDifferOneBaseBinAtLag1() {
        LineLagCensus census = LineLagCensus.fromRaw(
                "AAAAAAAA\nCCCCCCCC\nTTTTTTTT\nAAAACCCC\nAAAATTTT\n"
                        + "CCCCAAAA\nCCCCTTTT\nTTTTAAAA\nGGGGGGGGAAAA\nGGGGGGGGTTTT\n");
        assertThat(census.nearAt()).isEqualTo(8);
        assertThat(census.twinGroups()).isEqualTo(1);
        assertThat(census.lag0()).isZero();
        assertThat(census.lag1()).isEqualTo(1);
        assertThat(census.modalLag()).isEqualTo(1);
    }
}
