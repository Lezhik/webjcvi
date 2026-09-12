package org.webjcvi.stamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KmerStampTest {

    private final KmerStamp stamp = new KmerStamp();

    @Test
    void overlappingTripletsCountRepeatedAaa() {
        KmerStamp.Census census = stamp.rank("aaa bbb aaa", 3);
        assertThat(census.k()).isEqualTo(3);
        assertThat(census.topKmer()).isEqualTo("AAA");
        assertThat(census.topCount()).isGreaterThanOrEqualTo(2);
        assertThat(census.stamps().get(0).kmer()).isEqualTo("AAA");
    }

    @Test
    void shortTextHasNoStamps() {
        KmerStamp.Census census = stamp.rank("ab", 3);
        assertThat(census.stamps()).isEmpty();
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(KmerStamp.MAX_CHARS + 1);
        assertThatThrownBy(() -> stamp.rank(huge))
                .isInstanceOf(StampException.class)
                .hasMessageContaining("exceeds");
    }
}
