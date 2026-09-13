package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InteriorCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptyInputHasNoFrames() {
        InteriorCensus census = InteriorCensus.from(parser.parse(""), 70);
        assertThat(census.frames()).isZero();
        assertThat(census.uniqueAt()).isZero();
        assertThat(census.shareAt(8)).isZero();
        assertThat(census.midStartAt16()).isEqualTo(27);
    }

    @Test
    void centeredEightSaturatesAtFloor() {
        String a = "T".repeat(31) + "AAAAAAAA" + "T".repeat(31);
        String b = "T".repeat(31) + "CCCCCCCC" + "T".repeat(31);
        InteriorCensus census = InteriorCensus.from(parser.parse(a + b), 70);
        assertThat(census.frames()).isEqualTo(2);
        assertThat(census.uniqueAt()).isEqualTo(8);
        assertThat(census.shareAt(8)).isEqualTo(1.0);
        assertThat(census.midStartAt16()).isEqualTo(27);
    }

    @Test
    void edgeOnlyDifferenceLeavesTheMiddleColliding() {
        String a = "AAAAAAAA" + "T".repeat(62);
        String b = "CCCCCCCC" + "T".repeat(62);
        InteriorCensus census = InteriorCensus.from(parser.parse(a + b), 70);
        assertThat(census.shareAt(8)).isEqualTo(0.5);
        assertThat(census.shareAt(16)).isEqualTo(0.5);
        assertThat(census.uniqueAt()).isGreaterThan(16);
        assertThat(census.shareAt(70)).isEqualTo(1.0);
    }
}
