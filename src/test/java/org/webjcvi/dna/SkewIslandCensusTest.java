package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SkewIslandCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptySequenceHasNoIslands() {
        SkewIslandCensus census = SkewIslandCensus.from(parser.parse(""), 70);
        assertThat(census.windowCount()).isZero();
        assertThat(census.failIslands()).isZero();
        assertThat(census.passIslands()).isZero();
    }

    @Test
    void consecutiveFailedWindowsCollapseToOneIsland() {
        String raw = "A".repeat(140);
        SkewIslandCensus census = SkewIslandCensus.from(parser.parse(raw), 70);
        assertThat(census.windowCount()).isEqualTo(2);
        assertThat(census.failWindows()).isEqualTo(2);
        assertThat(census.failIslands()).isEqualTo(1);
        assertThat(census.longestFail()).isEqualTo(2);
        assertThat(census.passIslands()).isZero();
    }

    @Test
    void balancedThenFailedAreTwoIslands() {
        String raw = "AT".repeat(35) + "A".repeat(70);
        SkewIslandCensus census = SkewIslandCensus.from(parser.parse(raw), 70);
        assertThat(census.passIslands()).isEqualTo(1);
        assertThat(census.failIslands()).isEqualTo(1);
        assertThat(census.longestPass()).isEqualTo(1);
        assertThat(census.longestFail()).isEqualTo(1);
    }
}
