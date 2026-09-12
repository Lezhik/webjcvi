package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlotCensusTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptyInputHasNoFrames() {
        SlotCensus census = SlotCensus.from(parser.parse(""), 70);
        assertThat(census.frames()).isZero();
        assertThat(census.reverse().topCount()).isZero();
    }

    @Test
    void fullFrameFillsTheThreeSlots() {
        char[] buf = new char[70];
        java.util.Arrays.fill(buf, 'A');
        "GCGCGCGC".getChars(0, 8, buf, 4);
        "ATATATAT".getChars(0, 8, buf, 19);
        "AAAATTTT".getChars(0, 8, buf, 58);
        SlotCensus census = SlotCensus.from(parser.parse(new String(buf)), 70);
        assertThat(census.frames()).isEqualTo(1);
        assertThat(census.rc().topKmer()).isEqualTo("GCGCGCGC");
        assertThat(census.reverse().topKmer()).isEqualTo("ATATATAT");
        assertThat(census.identity().topKmer()).isEqualTo("AAAATTTT");
        assertThat(census.rc().gcPercent()).isEqualTo(100.0);
        assertThat(census.reverse().gcPercent()).isZero();
    }

    @Test
    void wrapTooNarrowUsesDefaultSeventy() {
        char[] buf = new char[70];
        java.util.Arrays.fill(buf, 'T');
        SlotCensus census = SlotCensus.from(parser.parse(new String(buf)), 10);
        assertThat(census.wrapWidth()).isEqualTo(70);
        assertThat(census.frames()).isEqualTo(1);
    }
}
