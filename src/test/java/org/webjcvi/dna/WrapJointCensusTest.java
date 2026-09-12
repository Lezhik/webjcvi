package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class WrapJointCensusTest {

    @Test
    void emptyInputHasNoJoints() {
        WrapJointCensus census = WrapJointCensus.fromRaw("");
        assertThat(census.jointCount()).isZero();
        assertThat(census.topJoint()).isEmpty();
    }

    @Test
    void countsComplementaryWrapJoints() {
        WrapJointCensus census = WrapJointCensus.fromRaw("AAAA\nTTTT\n");
        assertThat(census.jointCount()).isEqualTo(1);
        assertThat(census.complementaryJoints()).isEqualTo(1);
        assertThat(census.topJoint()).isEqualTo("AT");
        assertThat(census.topJoints().keySet()).containsExactly("AT");
    }

    @Test
    void nonPairedJointIsNotComplementary() {
        WrapJointCensus census = WrapJointCensus.fromRaw("AAAA\nAAAA\n");
        assertThat(census.jointCount()).isEqualTo(1);
        assertThat(census.complementaryJoints()).isZero();
        assertThat(census.topJoint()).isEqualTo("AA");
    }
}
