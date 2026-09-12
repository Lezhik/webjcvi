package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DimerContrastTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptySequenceHasNoDimers() {
        DimerContrast contrast = DimerContrast.from(parser.parse(""), WrapJointCensus.fromRaw(""));
        assertThat(contrast.globalDimers()).isZero();
        assertThat(contrast.mostEnriched()).isEqualTo("AA");
        assertThat(contrast.mostEnrichedValue()).isZero();
    }

    @Test
    void jointSameBaseIsEnrichedAgainstMixedBackground() {
        String raw = "ATGCAT\nTGCATA\nATGCAT\n";
        DnaSequence sequence = parser.parse(raw);
        DimerContrast contrast = DimerContrast.from(sequence, WrapJointCensus.fromRaw(raw));
        assertThat(contrast.jointCount()).isEqualTo(2);
        assertThat(contrast.sameBaseJointShare()).isGreaterThan(contrast.sameBaseGlobalShare());
    }
}
