package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WrapCensusTest {

    @Test
    void emptyRawHasZeroLines() {
        WrapCensus census = WrapCensus.fromRaw("");
        assertThat(census.lineCount()).isZero();
        assertThat(census.modalWidth()).isZero();
    }

    @Test
    void measuresWrappedLineWidths() {
        WrapCensus census = WrapCensus.fromRaw("AAA\nBB\nAAA\n");
        assertThat(census.lineCount()).isEqualTo(3);
        assertThat(census.minWidth()).isEqualTo(2);
        assertThat(census.maxWidth()).isEqualTo(3);
        assertThat(census.modalWidth()).isEqualTo(3);
        assertThat(census.medianWidth()).isEqualTo(3);
    }
}
