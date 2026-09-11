package org.webjcvi.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BannerSplitterTest {

    private final BannerSplitter splitter = new BannerSplitter();

    @Test
    void splitsOnRunsOfDefaultLengthTen() {
        String text = "alpha\n==========\nbeta\n----------\ngamma";
        var sections = splitter.split(text);
        assertThat(sections).hasSize(3);
        assertThat(sections.get(0).preview()).contains("alpha");
        assertThat(sections.get(0).banner()).isEmpty();
        assertThat(sections.get(1).preview()).contains("beta");
        assertThat(sections.get(1).banner()).isEqualTo("==========");
        assertThat(sections.get(2).preview()).contains("gamma");
        assertThat(sections.get(2).banner()).isEqualTo("----------");
    }

    @Test
    void shortRunsStayInsideThePayload() {
        String text = "keep=====this";
        var sections = splitter.split(text, 10);
        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).length()).isEqualTo(text.length());
    }

    @Test
    void foldsCaseWhenMeasuringRuns() {
        var sections = splitter.split("head\naaaaaaaaaa\ntail", 10);
        assertThat(sections).hasSize(2);
        assertThat(sections.get(1).preview()).contains("tail");
    }

    @Test
    void noBannerYieldsOneSection() {
        var sections = splitter.split("just a log line");
        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).offset()).isZero();
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(BannerSplitter.MAX_CHARS + 1);
        assertThatThrownBy(() -> splitter.split(huge))
                .isInstanceOf(SegmentException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void rejectsTinyMinRun() {
        assertThatThrownBy(() -> splitter.split("ab", 1))
                .isInstanceOf(SegmentException.class);
    }
}
