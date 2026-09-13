package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LandscapeCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptyInputHasNoFrames() {
        LandscapeCensus census = LandscapeCensus.from(parser.parse(""), 70);
        assertThat(census.frames()).isZero();
        assertThat(census.columns()).isEmpty();
        assertThat(census.spread()).isZero();
        assertThat(census.tileLength()).isEqualTo(16);
    }

    @Test
    void interiorDifferencePeaksAtColumnTwentySeven() {
        String a = "T".repeat(35) + "A".repeat(8) + "T".repeat(27);
        String b = "T".repeat(35) + "C".repeat(8) + "T".repeat(27);
        LandscapeCensus census = LandscapeCensus.from(parser.parse(a + b), 70);
        assertThat(census.frames()).isEqualTo(2);
        assertThat(census.peakAt()).isEqualTo(20);
        assertThat(census.troughAt()).isEqualTo(0);
        assertThat(census.peakShare()).isEqualTo(1.0);
        assertThat(census.troughShare()).isEqualTo(0.5);
        assertThat(census.spread()).isEqualTo(0.5);
        assertThat(census.columns()).hasSize(55);
    }

    @Test
    void identicalFramesAreFlat() {
        String frame = "A".repeat(70);
        LandscapeCensus census = LandscapeCensus.from(parser.parse(frame + frame), 70);
        assertThat(census.spread()).isZero();
        assertThat(census.peakShare()).isEqualTo(0.5);
        assertThat(census.troughShare()).isEqualTo(0.5);
    }
}
