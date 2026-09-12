package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodonFrameCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptySequenceHasNoCodons() {
        CodonFrameCensus census = CodonFrameCensus.from(parser.parse(""));
        assertThat(census.maxAtg()).isZero();
        assertThat(census.maxStops()).isZero();
        assertThat(census.frames()).hasSize(3);
        assertThat(census.frames().get(0).codonCount()).isZero();
    }

    @Test
    void phaseZeroReadsStackedStartsAndAStop() {
        CodonFrameCensus census = CodonFrameCensus.from(parser.parse("ATGATGTGA"));
        assertThat(census.bestAtgPhase()).isEqualTo(0);
        assertThat(census.maxAtg()).isEqualTo(2);
        assertThat(census.frames().get(0).stops()).isEqualTo(1);
        assertThat(census.frames().get(0).topCodon()).isEqualTo("ATG");
    }

    @Test
    void gcSpreadSeparatesCodonPositions() {
        CodonFrameCensus census = CodonFrameCensus.from(parser.parse("AAGAAGAAG"));
        assertThat(census.frames().get(0).gcPos1()).isEqualTo(0.0);
        assertThat(census.frames().get(0).gcPos3()).isEqualTo(100.0);
        assertThat(census.maxGcSpread()).isEqualTo(100.0);
    }
}
