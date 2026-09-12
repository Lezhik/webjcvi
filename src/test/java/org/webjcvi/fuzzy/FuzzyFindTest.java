package org.webjcvi.fuzzy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FuzzyFindTest {

    private final FuzzyFind fuzzy = new FuzzyFind();

    @Test
    void ranksExactBeforeOneMismatch() {
        FuzzyFind.Scan scan = fuzzy.search("hello hallo hello", "hello", 1);
        assertThat(scan.hitCount()).isGreaterThanOrEqualTo(3);
        assertThat(scan.hits().get(0).distance()).isZero();
        assertThat(scan.hits().get(0).preview()).isEqualToIgnoringCase("hello");
        assertThat(scan.hits().stream().map(FuzzyFind.Hit::preview)).contains("hallo");
    }

    @Test
    void rejectsShortMotif() {
        assertThatThrownBy(() -> fuzzy.search("abcdef", "ab", 1))
                .isInstanceOf(FuzzyException.class)
                .hasMessageContaining("at least");
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(FuzzyFind.MAX_CHARS + 1);
        assertThatThrownBy(() -> fuzzy.search(huge, "abcd"))
                .isInstanceOf(FuzzyException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void emptyTextWithValidMotifHasNoHits() {
        FuzzyFind.Scan scan = fuzzy.search("", "abcd");
        assertThat(scan.hitCount()).isZero();
        assertThat(scan.scanned()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> fuzzy.search(null, "abcd"))
                .isInstanceOf(FuzzyException.class);
    }
}
