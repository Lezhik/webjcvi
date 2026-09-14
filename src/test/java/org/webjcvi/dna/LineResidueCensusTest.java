package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LineResidueCensusTest {

    @Test
    void emptyInputHasNoResidue() {
        LineResidueCensus census = LineResidueCensus.fromRaw("");
        assertThat(census.lines()).isZero();
        assertThat(census.nearAt()).isZero();
        assertThat(census.twinGroups()).isZero();
        assertThat(census.copyGroups()).isZero();
        assertThat(census.forkGroups()).isZero();
    }

    @Test
    void distinctShortLinesHaveNoLeftover() {
        LineResidueCensus census = LineResidueCensus.fromRaw("AAAAAAAA\nCCCCCCCC\n");
        assertThat(census.lines()).isEqualTo(2);
        assertThat(census.nearAt()).isEqualTo(8);
        assertThat(census.shareAtNear()).isEqualTo(1.0);
        assertThat(census.twinGroups()).isZero();
        assertThat(census.copyShare()).isZero();
    }

    @Test
    void identicalLeftoverLinesCountAsCopies() {
        LineResidueCensus census = LineResidueCensus.fromRaw(
                "AAAAAAAA\nCCCCCCCC\nTTTTTTTT\nAAAACCCC\nAAAATTTT\n"
                        + "CCCCAAAA\nCCCCTTTT\nTTTTAAAA\nGGGGGGGGTTTT\nGGGGGGGGTTTT\n");
        assertThat(census.nearAt()).isEqualTo(8);
        assertThat(census.twinGroups()).isEqualTo(1);
        assertThat(census.copyGroups()).isEqualTo(1);
        assertThat(census.copyLines()).isEqualTo(2);
        assertThat(census.forkGroups()).isZero();
        assertThat(census.copyShare()).isEqualTo(1.0);
        assertThat(census.minFork()).isZero();
    }

    @Test
    void leftoverTwinsThatDifferCountAsForks() {
        LineResidueCensus census = LineResidueCensus.fromRaw(
                "AAAAAAAA\nCCCCCCCC\nTTTTTTTT\nAAAACCCC\nAAAATTTT\n"
                        + "CCCCAAAA\nCCCCTTTT\nTTTTAAAA\nGGGGGGGGAAAA\nGGGGGGGGTTTT\n");
        assertThat(census.nearAt()).isEqualTo(8);
        assertThat(census.twinGroups()).isEqualTo(1);
        assertThat(census.copyGroups()).isZero();
        assertThat(census.forkGroups()).isEqualTo(1);
        assertThat(census.forkLines()).isEqualTo(2);
        assertThat(census.copyShare()).isZero();
        assertThat(census.minFork()).isEqualTo(9);
    }
}
