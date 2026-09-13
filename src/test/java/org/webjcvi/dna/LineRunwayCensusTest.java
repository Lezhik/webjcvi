package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LineRunwayCensusTest {

    @Test
    void emptyInputHasNoCliff() {
        LineRunwayCensus census = LineRunwayCensus.fromRaw("");
        assertThat(census.lines()).isZero();
        assertThat(census.cliffAt()).isZero();
        assertThat(census.runway()).isZero();
        assertThat(census.overhang()).isZero();
    }

    @Test
    void distinctLinesCliffAtFloorWithNoFork() {
        LineRunwayCensus census = LineRunwayCensus.fromRaw("AAAAAAAA\nCCCCCCCC\n");
        assertThat(census.lines()).isEqualTo(2);
        assertThat(census.cliffAt()).isEqualTo(8);
        assertThat(census.modalFork()).isZero();
        assertThat(census.runway()).isZero();
        assertThat(census.overhang()).isZero();
    }

    @Test
    void sharedSixteenPrefixForksAfterCliff() {
        String a = "A".repeat(16) + "T".repeat(54);
        String b = "A".repeat(16) + "C".repeat(54);
        String unique = "G".repeat(70);
        LineRunwayCensus census = LineRunwayCensus.fromRaw(a + "\n" + b + "\n" + unique + "\n");
        assertThat(census.modalFork()).isEqualTo(17);
        assertThat(census.cliffAt()).isPositive();
        assertThat(census.cliffAt()).isLessThanOrEqualTo(17);
        assertThat(census.overhang()).isGreaterThanOrEqualTo(0);
    }
}
