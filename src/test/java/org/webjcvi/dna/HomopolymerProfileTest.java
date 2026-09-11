package org.webjcvi.dna;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HomopolymerProfileTest {

    private final DnaParser parser = new DnaParser();

    @Test
    void emptySequenceHasNoRuns() {
        HomopolymerProfile profile = HomopolymerProfile.from(parser.parse(""));
        assertThat(profile.longestLength()).isZero();
        assertThat(profile.runsAtLeast5()).isZero();
    }

    @Test
    void measuresLongestCanonicalRun() {
        HomopolymerProfile profile = HomopolymerProfile.from(parser.parse("AAAATAAAAA"));
        assertThat(profile.maxRun().get('A')).isEqualTo(5);
        assertThat(profile.longestBase()).isEqualTo('A');
        assertThat(profile.longestLength()).isEqualTo(5);
        assertThat(profile.runsAtLeast5()).isEqualTo(1);
        assertThat(profile.runs5to9()).isEqualTo(1);
    }
}
