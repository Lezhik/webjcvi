package org.webjcvi.frame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FrameFieldsTest {

    private final FrameFields fields = new FrameFields();

    @Test
    void extractsThreeSlotsFromAFullFrame() {
        char[] buf = new char[70];
        java.util.Arrays.fill(buf, 'x');
        "gctagcta".getChars(0, 8, buf, 4);
        "abcddcba".getChars(0, 8, buf, 19);
        "atgcatgc".getChars(0, 8, buf, 58);
        FrameFields.Scan scan = fields.extract(new String(buf));
        assertThat(scan.recordCount()).isEqualTo(1);
        assertThat(scan.records().get(0).rc()).isEqualTo("GCTAGCTA");
        assertThat(scan.records().get(0).reverse()).isEqualTo("ABCDDCBA");
        assertThat(scan.records().get(0).identity()).isEqualTo("ATGCATGC");
        assertThat(scan.topRc()).isEqualTo("GCTAGCTA");
    }

    @Test
    void newlinesAreDroppedBeforeFraming() {
        char[] buf = new char[70];
        java.util.Arrays.fill(buf, 'y');
        "abcddcba".getChars(0, 8, buf, 19);
        String text = new String(buf, 0, 30) + "\n" + new String(buf, 30, 40);
        FrameFields.Scan scan = fields.extract(text);
        assertThat(scan.recordCount()).isEqualTo(1);
        assertThat(scan.records().get(0).reverse()).isEqualTo("ABCDDCBA");
    }

    @Test
    void shortTextHasNoRecords() {
        FrameFields.Scan scan = fields.extract("short");
        assertThat(scan.recordCount()).isZero();
        assertThat(scan.topRc()).isEmpty();
    }

    @Test
    void emptyTextHasNoRecords() {
        FrameFields.Scan scan = fields.extract("");
        assertThat(scan.recordCount()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> fields.extract(null))
                .isInstanceOf(FrameException.class);
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(FrameFields.MAX_CHARS + 1);
        assertThatThrownBy(() -> fields.extract(huge))
                .isInstanceOf(FrameException.class)
                .hasMessageContaining("exceeds");
    }
}
