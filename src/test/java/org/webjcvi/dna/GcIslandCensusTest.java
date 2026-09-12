package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GcIslandCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptySequenceHasNoIslands() {
        GcIslandCensus census = GcIslandCensus.from(parser.parse(""));
        assertThat(census.gcIslands()).isZero();
        assertThat(census.atIslands()).isZero();
    }

    @Test
    void mixedGcRunIsOneIslandNotTwoHomopolymers() {
        GcIslandCensus census = GcIslandCensus.from(parser.parse("AA GCGC TT"));
        assertThat(census.gcIslands()).isEqualTo(1);
        assertThat(census.longestGc()).isEqualTo(4);
        assertThat(census.atIslands()).isEqualTo(2);
    }

    @Test
    void countsLongGcIslands() {
        GcIslandCensus census = GcIslandCensus.from(parser.parse("G".repeat(12) + "AAA"));
        assertThat(census.gcIslandsAtLeast10()).isEqualTo(1);
        assertThat(census.longestGc()).isEqualTo(12);
    }
}
