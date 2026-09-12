package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HairpinCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptySequenceHasNoHairpins() {
        HairpinCensus census = HairpinCensus.from(parser.parse(""));
        assertThat(census.adjacent()).isZero();
        assertThat(census.gapped()).isZero();
    }

    @Test
    void adjacentEvenStemIsLoopZero() {
        HairpinCensus census = HairpinCensus.from(parser.parse("AATTAATT"));
        assertThat(census.adjacent()).isGreaterThanOrEqualTo(1);
        assertThat(census.longestAdjacent()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void oneBaseLoopCountsAsGapped() {
        HairpinCensus census = HairpinCensus.from(parser.parse("AATAATT"));
        int loop1 = census.rows().stream()
                .filter(row -> row.loop() == 1)
                .findFirst()
                .orElseThrow()
                .hairpins();
        assertThat(loop1).isGreaterThan(0);
        assertThat(census.gapped()).isGreaterThanOrEqualTo(loop1);
    }
}
