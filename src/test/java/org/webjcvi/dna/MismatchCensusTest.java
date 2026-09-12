package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MismatchCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptySequenceHasNoWindows() {
        MismatchCensus census = MismatchCensus.from(parser.parse(""));
        assertThat(census.windows()).isZero();
        assertThat(census.modalDistance()).isZero();
    }

    @Test
    void perfectEightMerReverseComplementIsDistanceZero() {
        MismatchCensus census = MismatchCensus.from(parser.parse("AATTAATT"));
        assertThat(census.windows()).isEqualTo(1);
        assertThat(census.rows().get(0).count()).isEqualTo(1);
        assertThat(census.modalDistance()).isZero();
    }

    @Test
    void nonComplementaryHalvesAreNotDistanceZero() {
        MismatchCensus census = MismatchCensus.from(parser.parse("AAAAAAAA"));
        assertThat(census.windows()).isEqualTo(1);
        assertThat(census.rows().get(0).count()).isZero();
        assertThat(census.modalDistance()).isGreaterThan(0);
    }
}
