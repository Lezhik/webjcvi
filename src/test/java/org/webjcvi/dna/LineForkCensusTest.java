package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LineForkCensusTest {

    @Test
    void emptyInputHasNoTwins() {
        LineForkCensus census = LineForkCensus.fromRaw("");
        assertThat(census.lines()).isZero();
        assertThat(census.twinGroups()).isZero();
        assertThat(census.modalFork()).isZero();
    }

    @Test
    void sharedSixteenPrefixForksAtSeventeen() {
        String a = "A".repeat(16) + "T".repeat(54);
        String b = "A".repeat(16) + "C".repeat(54);
        LineForkCensus census = LineForkCensus.fromRaw(a + "\n" + b + "\n");
        assertThat(census.lines()).isEqualTo(2);
        assertThat(census.twinGroups()).isEqualTo(1);
        assertThat(census.minFork()).isEqualTo(17);
        assertThat(census.modalFork()).isEqualTo(17);
        assertThat(census.maxFork()).isEqualTo(17);
    }

    @Test
    void concatenationWithoutNewlinesIsOneRecord() {
        String a = "A".repeat(16) + "T".repeat(54);
        String b = "A".repeat(16) + "C".repeat(54);
        LineForkCensus census = LineForkCensus.fromRaw(a + b);
        assertThat(census.lines()).isEqualTo(1);
        assertThat(census.twinGroups()).isZero();
    }
}
