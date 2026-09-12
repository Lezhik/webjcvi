package org.webjcvi.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RareBreakTokenizerTest {

    private final RareBreakTokenizer tokenizer = new RareBreakTokenizer();

    @Test
    void gcLikeBreaksSplitAtTokens() {
        RareBreakTokenizer.Cut cut = tokenizer.cut("AAAAAAAAAA GCGCGC AAAAAAAAAA", 2);
        assertThat(cut.rareClass()).containsAnyOf("G", "C");
        assertThat(cut.tokenCount()).isEqualTo(2);
        assertThat(cut.tokens().get(0).preview()).isEqualTo("AAAAAAAAAA");
        assertThat(cut.tokens().get(1).preview()).isEqualTo("AAAAAAAAAA");
    }

    @Test
    void majorityOnlyIsOneToken() {
        RareBreakTokenizer.Cut cut = tokenizer.cut("AAAAAAAAAA", 2);
        assertThat(cut.rareClass()).isEmpty();
        assertThat(cut.tokenCount()).isEqualTo(1);
        assertThat(cut.tokens().get(0).preview()).isEqualTo("AAAAAAAAAA");
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(RareBreakTokenizer.MAX_CHARS + 1);
        assertThatThrownBy(() -> tokenizer.cut(huge))
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void emptyTextHasNoTokens() {
        RareBreakTokenizer.Cut cut = tokenizer.cut("");
        assertThat(cut.tokenCount()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> tokenizer.cut(null))
                .isInstanceOf(TokenException.class);
    }
}
