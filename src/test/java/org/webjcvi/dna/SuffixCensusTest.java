package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SuffixCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptyInputHasNoFrames() {
        SuffixCensus census = SuffixCensus.from(parser.parse(""), 70);
        assertThat(census.frames()).isZero();
        assertThat(census.uniqueAt()).isZero();
        assertThat(census.shareAt(8)).isZero();
    }

    @Test
    void sharedTrailingEightIsNotUniqueAtEight() {
        String a = "T".repeat(62) + "AAAAAAAA";
        String b = "G".repeat(62) + "AAAAAAAA";
        SuffixCensus census = SuffixCensus.from(parser.parse(a + b), 70);
        assertThat(census.frames()).isEqualTo(2);
        assertThat(census.shareAt(8)).isEqualTo(0.5);
        assertThat(census.uniqueAt()).isEqualTo(9);
    }

    @Test
    void distinctTailsSaturateAtFloor() {
        String a = "T".repeat(62) + "AAAAAAAA";
        String b = "T".repeat(62) + "CCCCCCCC";
        SuffixCensus census = SuffixCensus.from(parser.parse(a + b), 70);
        assertThat(census.uniqueAt()).isEqualTo(8);
        assertThat(census.shareAt(8)).isEqualTo(1.0);
    }
}
