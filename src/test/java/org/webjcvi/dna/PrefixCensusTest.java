package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PrefixCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptyInputHasNoFrames() {
        PrefixCensus census = PrefixCensus.from(parser.parse(""), 70);
        assertThat(census.frames()).isZero();
        assertThat(census.familyCount()).isZero();
        assertThat(census.uniqueShare()).isZero();
    }

    @Test
    void sharedLeadingEightIsAFamily() {
        String a = "AAAAAAAA" + "T".repeat(62);
        String b = "AAAAAAAA" + "G".repeat(62);
        PrefixCensus census = PrefixCensus.from(parser.parse(a + b), 70);
        assertThat(census.frames()).isEqualTo(2);
        assertThat(census.distinct()).isEqualTo(1);
        assertThat(census.familyCount()).isEqualTo(1);
        assertThat(census.familyFrames()).isEqualTo(2);
        assertThat(census.topPrefix()).isEqualTo("AAAAAAAA");
        assertThat(census.uniqueShare()).isEqualTo(0.5);
    }

    @Test
    void distinctPrefixesHaveNoFamilies() {
        PrefixCensus census = PrefixCensus.from(
                parser.parse("AAAAAAAA" + "T".repeat(62) + "CCCCCCCC" + "G".repeat(62)), 70);
        assertThat(census.frames()).isEqualTo(2);
        assertThat(census.distinct()).isEqualTo(2);
        assertThat(census.familyCount()).isZero();
        assertThat(census.topCount()).isEqualTo(1);
        assertThat(census.uniqueShare()).isEqualTo(1.0);
    }
}
