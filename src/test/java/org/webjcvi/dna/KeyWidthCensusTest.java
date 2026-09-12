package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KeyWidthCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptyInputHasNoFrames() {
        KeyWidthCensus census = KeyWidthCensus.from(parser.parse(""), 70);
        assertThat(census.frames()).isZero();
        assertThat(census.uniqueAt()).isZero();
        assertThat(census.shareAt(8)).isZero();
    }

    @Test
    void sharedEightIsUniqueAtNine() {
        String a = "AAAAAAAA" + "T".repeat(62);
        String b = "AAAAAAAA" + "G".repeat(62);
        KeyWidthCensus census = KeyWidthCensus.from(parser.parse(a + b), 70);
        assertThat(census.frames()).isEqualTo(2);
        assertThat(census.uniqueAt()).isEqualTo(9);
        assertThat(census.shareAt(8)).isEqualTo(0.5);
        assertThat(census.shareAt(70)).isEqualTo(1.0);
    }

    @Test
    void identicalFramesNeverSaturate() {
        String frame = "A".repeat(70);
        KeyWidthCensus census = KeyWidthCensus.from(parser.parse(frame + frame), 70);
        assertThat(census.uniqueAt()).isZero();
        assertThat(census.shareAt(8)).isEqualTo(0.5);
        assertThat(census.shareAt(70)).isEqualTo(0.5);
    }
}
