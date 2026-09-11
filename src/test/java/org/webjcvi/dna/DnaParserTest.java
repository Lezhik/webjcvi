package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DnaParserTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void parsesLineWrappedMixedCaseSequence() {
        DnaSequence sequence = parser.parse("atga\nCGta\r\n  tt\n");
        assertThat(sequence.normalized()).isEqualTo("ATGACGTA TT".replace(" ", ""));
        assertThat(sequence.normalized()).isEqualTo("ATGACGTATT");
        assertThat(sequence.length()).isEqualTo(10);
        assertThat(sequence.count('A')).isEqualTo(3);
        assertThat(sequence.count('T')).isEqualTo(4);
        assertThat(sequence.count('G')).isEqualTo(2);
        assertThat(sequence.count('C')).isEqualTo(1);
        assertThat(sequence.sourceLineCount()).isEqualTo(3);
    }

    @Test
    void emptyAndWhitespaceOnlyAreEmptySequences() {
        assertThat(parser.parse("").isEmpty()).isTrue();
        assertThat(parser.parse(" \n\t\r\n ").isEmpty()).isTrue();
        assertThat(parser.parse(null).isEmpty()).isTrue();
    }

    @Test
    void ambiguousIupacCodesAreCountedAndKept() {
        DnaSequence sequence = parser.parse("ATGNnR");
        assertThat(sequence.normalized()).isEqualTo("ATGNNR");
        assertThat(sequence.ambiguousTotal()).isEqualTo(3);
        assertThat(sequence.ambiguousCounts()).containsEntry('N', 2L).containsEntry('R', 1L);
        assertThat(sequence.invalidTotal()).isZero();
    }

    @Test
    void invalidCharactersAreDroppedAndCounted() {
        DnaSequence sequence = parser.parse("ATGC-XXX?");
        assertThat(sequence.normalized()).isEqualTo("ATGC");
        assertThat(sequence.invalidTotal()).isEqualTo(5);
        assertThat(sequence.invalidCounts()).containsEntry('-', 1L).containsEntry('X', 3L).containsEntry('?', 1L);
    }

    @Test
    void malformedDoesNotCrashAndLeavesCanonicalCounts() {
        DnaSequence sequence = parser.parse("!!!!\nATAT");
        assertThat(sequence.normalized()).isEqualTo("ATAT");
        assertThat(sequence.invalidTotal()).isEqualTo(4);
        assertThat(sequence.count('A')).isEqualTo(2);
        assertThat(sequence.count('T')).isEqualTo(2);
    }

    @Test
    void gcPercentUsesCanonicalBasesOnly() {
        DnaSequence sequence = parser.parse("GGCCNNNN");
        assertThat(sequence.gcPercent()).isEqualTo(100.0);
        DnaSequence atOnly = parser.parse("ATAT");
        assertThat(atOnly.gcPercent()).isEqualTo(0.0);
        assertThat(parser.parse("").gcPercent()).isEqualTo(0.0);
    }
}
