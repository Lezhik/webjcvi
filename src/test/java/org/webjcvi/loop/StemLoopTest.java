package org.webjcvi.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StemLoopTest {

    private final StemLoop loops = new StemLoop();

    @Test
    void extractsComplementarySpanAndSkipsEmptyAndQuotes() {
        StemLoop.Scan scan = loops.extract("(hello) () \"nope\"", 1);
        assertThat(scan.spanCount()).isEqualTo(1);
        assertThat(scan.spans().get(0).preview()).isEqualTo("hello");
        assertThat(scan.spans().get(0).opener()).isEqualTo("(");
        assertThat(scan.leftoverOpens()).isZero();
    }

    @Test
    void countsNestedSpans() {
        StemLoop.Scan scan = loops.extract("((x))", 1);
        assertThat(scan.spanCount()).isEqualTo(2);
        assertThat(scan.nested()).isEqualTo(1);
        assertThat(scan.spans().stream().map(StemLoop.Span::preview)).contains("x", "(x)");
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(StemLoop.MAX_CHARS + 1);
        assertThatThrownBy(() -> loops.extract(huge))
                .isInstanceOf(LoopException.class)
                .hasMessageContaining("exceeds");
    }
}
