package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NeighborCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptySequenceHasNoWindows() {
        NeighborCensus census = NeighborCensus.from(parser.parse(""));
        assertThat(census.windows()).isZero();
        assertThat(census.identity().zeroCount()).isZero();
    }

    @Test
    void repeatedHalvesAreIdentityDistanceZero() {
        NeighborCensus census = NeighborCensus.from(parser.parse("AAAAAAAA"));
        assertThat(census.windows()).isEqualTo(1);
        assertThat(census.identity().modalDistance()).isZero();
        assertThat(census.identity().zeroCount()).isEqualTo(1);
        assertThat(census.rc().zeroCount()).isZero();
    }

    @Test
    void reverseComplementHalvesAreRcDistanceZero() {
        NeighborCensus census = NeighborCensus.from(parser.parse("ATGCGCAT"));
        assertThat(census.windows()).isEqualTo(1);
        assertThat(census.rc().zeroCount()).isEqualTo(1);
        assertThat(census.identity().zeroCount()).isZero();
    }

    @Test
    void reversedHalvesAreReverseDistanceZero() {
        NeighborCensus census = NeighborCensus.from(parser.parse("ATGCCGTA"));
        assertThat(census.windows()).isEqualTo(1);
        assertThat(census.reverse().zeroCount()).isEqualTo(1);
        assertThat(census.identity().zeroCount()).isZero();
        assertThat(census.rc().zeroCount()).isZero();
    }
}
