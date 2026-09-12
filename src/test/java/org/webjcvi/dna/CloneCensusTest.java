package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CloneCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptyInputHasNoFrames() {
        CloneCensus census = CloneCensus.from(parser.parse(""), 70);
        assertThat(census.frames()).isZero();
        assertThat(census.cloneGroups()).isZero();
        assertThat(census.uniqueShare()).isZero();
    }

    @Test
    void twoIdenticalFramesCountAsClones() {
        String frame = "A".repeat(70);
        CloneCensus census = CloneCensus.from(parser.parse(frame + frame), 70);
        assertThat(census.frames()).isEqualTo(2);
        assertThat(census.distinct()).isEqualTo(1);
        assertThat(census.cloneGroups()).isEqualTo(1);
        assertThat(census.cloneFrames()).isEqualTo(2);
        assertThat(census.topCount()).isEqualTo(2);
        assertThat(census.uniqueShare()).isEqualTo(0.5);
    }

    @Test
    void distinctFramesHaveNoClones() {
        CloneCensus census = CloneCensus.from(parser.parse("A".repeat(70) + "T".repeat(70)), 70);
        assertThat(census.frames()).isEqualTo(2);
        assertThat(census.distinct()).isEqualTo(2);
        assertThat(census.cloneGroups()).isZero();
        assertThat(census.topCount()).isEqualTo(1);
        assertThat(census.uniqueShare()).isEqualTo(1.0);
    }
}
