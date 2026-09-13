package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LineKeyCensusTest {

    @Test
    void emptyInputHasNoLines() {
        LineKeyCensus census = LineKeyCensus.fromRaw("");
        assertThat(census.lines()).isZero();
        assertThat(census.uniqueAt()).isZero();
        assertThat(census.shareAt(16)).isZero();
    }

    @Test
    void sharedPrefixCollidesAtSixteenThenSaturates() {
        String a = "A".repeat(16) + "T".repeat(54);
        String b = "A".repeat(16) + "C".repeat(54);
        LineKeyCensus census = LineKeyCensus.fromRaw(a + "\n" + b + "\n");
        assertThat(census.lines()).isEqualTo(2);
        assertThat(census.shareAt(16)).isEqualTo(0.5);
        assertThat(census.uniqueAt()).isEqualTo(17);
        assertThat(census.shareAt(20)).isEqualTo(1.0);
    }

    @Test
    void concatenationWithoutNewlinesIsOneRecord() {
        String a = "A".repeat(16) + "T".repeat(54);
        String b = "A".repeat(16) + "C".repeat(54);
        LineKeyCensus census = LineKeyCensus.fromRaw(a + b);
        assertThat(census.lines()).isEqualTo(1);
        assertThat(census.shareAt(16)).isEqualTo(1.0);
        assertThat(census.uniqueAt()).isEqualTo(8);
    }
}
