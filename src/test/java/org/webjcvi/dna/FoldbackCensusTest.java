package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FoldbackCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptySequenceHasNoStems() {
        FoldbackCensus census = FoldbackCensus.from(parser.parse(""));
        assertThat(census.rcStems()).isZero();
        assertThat(census.longestRc()).isZero();
    }

    @Test
    void evenReverseComplementStemCounts() {
        FoldbackCensus census = FoldbackCensus.from(parser.parse("AATTAATT"));
        assertThat(census.rcEven()).isGreaterThanOrEqualTo(1);
        assertThat(census.longestRc()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void sameBaseAbbaIsNotReverseComplementOnly() {
        FoldbackCensus census = FoldbackCensus.from(parser.parse("ACCA"));
        assertThat(census.sameEven()).isGreaterThanOrEqualTo(1);
    }
}
