package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WrapReverseCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptyInputHasNoJoints() {
        NeighborCensus interior = NeighborCensus.from(parser.parse(""));
        WrapReverseCensus census = WrapReverseCensus.from("", interior);
        assertThat(census.wrapJoints()).isZero();
        assertThat(census.interiorWindows()).isZero();
    }

    @Test
    void wrapSeamReverseIsCountedSeparatelyFromInterior() {
        String raw = "ATGC\nCGTA";
        NeighborCensus interior = NeighborCensus.from(parser.parse(raw));
        WrapReverseCensus census = WrapReverseCensus.from(raw, interior);
        assertThat(census.wrapJoints()).isEqualTo(1);
        assertThat(census.wrapReverseZero()).isEqualTo(1);
        assertThat(census.interiorWindows()).isEqualTo(1);
        assertThat(census.interiorReverseZero()).isEqualTo(1);
        assertThat(census.wrapVsInterior()).isEqualTo(1.0);
    }

    @Test
    void nonMirrorWrapIsNotReverseZero() {
        String raw = "AAAA\nTTTT";
        NeighborCensus interior = NeighborCensus.from(parser.parse(raw));
        WrapReverseCensus census = WrapReverseCensus.from(raw, interior);
        assertThat(census.wrapJoints()).isEqualTo(1);
        assertThat(census.wrapReverseZero()).isZero();
        assertThat(census.wrapVsInterior()).isZero();
    }
}
