package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LineLongCensusTest {

    @Test
    void emptyInputHasNoLongTail() {
        LineLongCensus census = LineLongCensus.fromRaw("");
        assertThat(census.lines()).isZero();
        assertThat(census.nearAt()).isZero();
        assertThat(census.twinGroups()).isZero();
        assertThat(census.lagLong()).isZero();
        assertThat(census.longTail()).isFalse();
        assertThat(census.maxLag()).isZero();
    }

    @Test
    void distinctShortLinesHaveNoLongTail() {
        LineLongCensus census = LineLongCensus.fromRaw("AAAAAAAA\nCCCCCCCC\n");
        assertThat(census.nearAt()).isEqualTo(8);
        assertThat(census.twinGroups()).isZero();
        assertThat(census.lagLong()).isZero();
        assertThat(census.longTail()).isFalse();
    }

    @Test
    void identicalLeftoverLinesHaveNoLongTail() {
        LineLongCensus census = LineLongCensus.fromRaw(
                "AAAAAAAA\nCCCCCCCC\nTTTTTTTT\nAAAACCCC\nAAAATTTT\n"
                        + "CCCCAAAA\nCCCCTTTT\nTTTTAAAA\nGGGGGGGGTTTT\nGGGGGGGGTTTT\n");
        assertThat(census.nearAt()).isEqualTo(8);
        assertThat(census.twinGroups()).isEqualTo(1);
        assertThat(census.lagLong()).isZero();
        assertThat(census.maxLag()).isZero();
        assertThat(census.longTail()).isFalse();
        assertThat(census.longShare()).isZero();
    }

    @Test
    void leftoverTwinsThatLingerPastDnaTightCountAsLongTail() {
        LineLongCensus census = LineLongCensus.fromRaw(
                "AAAAAAAA\nCCCCCCCC\nTTTTTTTT\nAAAACCCC\nAAAATTTT\n"
                        + "CCCCAAAA\nCCCCTTTT\nTTTTAAAA\nGGGGGGGGAAAATTTT\nGGGGGGGGAAAACCCC\n");
        assertThat(census.nearAt()).isEqualTo(8);
        assertThat(census.twinGroups()).isEqualTo(1);
        assertThat(census.lagLong()).isEqualTo(1);
        assertThat(census.maxLag()).isGreaterThan(LineLongCensus.DNA_TIGHT_LAG);
        assertThat(census.longTail()).isTrue();
        assertThat(census.longShare()).isEqualTo(1.0);
    }
}
