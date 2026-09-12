package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SameBaseLagCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptySequenceHasZeroEnrichment() {
        SameBaseLagCensus census = SameBaseLagCensus.from(parser.parse(""));
        assertThat(census.peakEnrichment()).isZero();
        assertThat(census.lags()).hasSize(SameBaseLagCensus.LAGS.length);
    }

    @Test
    void homopolymerPeaksAtLagOne() {
        SameBaseLagCensus census = SameBaseLagCensus.from(parser.parse("AAAAAAAAAA"));
        assertThat(census.peakLag()).isEqualTo(1);
        assertThat(census.lags().get(0).matchPercent()).isEqualTo(100.0);
    }

    @Test
    void periodThreeEnrichesLagThreeOverLagOne() {
        SameBaseLagCensus census = SameBaseLagCensus.from(parser.parse("ATGATGATGATGATGATGATGATG"));
        double lag1 = census.lags().stream().filter(l -> l.lag() == 1).findFirst().orElseThrow().enrichment();
        double lag3 = census.lags().stream().filter(l -> l.lag() == 3).findFirst().orElseThrow().enrichment();
        assertThat(lag3).isGreaterThan(lag1);
        assertThat(census.peakLag()).isEqualTo(3);
    }
}
