package org.webjcvi.reflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WrapReflowTest {

    private final WrapReflow reflow = new WrapReflow();

    @Test
    void stitchesFullWidthLinesAndBreaksOnAShortLine() {
        String wrapped = "a".repeat(70) + "\ncontinues\nshort";
        WrapReflow.Result result = reflow.unwrap(wrapped, 70);
        assertThat(result.stitches()).isEqualTo(1);
        assertThat(result.paragraphCount()).isEqualTo(2);
        assertThat(result.paragraphs().get(0).preview()).startsWith("a");
        assertThat(result.paragraphs().get(0).preview()).endsWith("continues");
        assertThat(result.paragraphs().get(1).preview()).isEqualTo("short");
    }

    @Test
    void blankLineSeparatesParagraphs() {
        WrapReflow.Result result = reflow.unwrap("hello\n\nworld", 70);
        assertThat(result.stitches()).isZero();
        assertThat(result.paragraphCount()).isEqualTo(2);
        assertThat(result.paragraphs()).extracting(WrapReflow.Paragraph::preview)
                .containsExactly("hello", "world");
    }

    @Test
    void twoShortLinesStaySeparate() {
        WrapReflow.Result result = reflow.unwrap("hello\nworld", 70);
        assertThat(result.paragraphCount()).isEqualTo(2);
        assertThat(result.stitches()).isZero();
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(WrapReflow.MAX_CHARS + 1);
        assertThatThrownBy(() -> reflow.unwrap(huge))
                .isInstanceOf(ReflowException.class)
                .hasMessageContaining("exceeds");
    }
}
