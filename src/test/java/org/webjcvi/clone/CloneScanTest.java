package org.webjcvi.clone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CloneScanTest {

    private final CloneScan clones = new CloneScan();

    @Test
    void twoIdenticalFramesAreACloneGroup() {
        String frame = "A".repeat(70);
        CloneScan.Scan scan = clones.scan(frame + frame);
        assertThat(scan.scanned()).isEqualTo(2);
        assertThat(scan.distinct()).isEqualTo(1);
        assertThat(scan.cloneGroups()).isEqualTo(1);
        assertThat(scan.cloneFrames()).isEqualTo(2);
        assertThat(scan.topCount()).isEqualTo(2);
        assertThat(scan.hits()).hasSize(1);
        assertThat(scan.hits().get(0).count()).isEqualTo(2);
        assertThat(scan.hits().get(0).frame()).isEqualTo(frame);
    }

    @Test
    void distinctFramesAreNotClones() {
        String a = "A".repeat(70);
        String b = "B".repeat(70);
        CloneScan.Scan scan = clones.scan(a + b);
        assertThat(scan.scanned()).isEqualTo(2);
        assertThat(scan.distinct()).isEqualTo(2);
        assertThat(scan.cloneGroups()).isZero();
        assertThat(scan.hits()).isEmpty();
    }

    @Test
    void newlinesAreDroppedBeforeFraming() {
        String frame = "C".repeat(70);
        String text = frame.substring(0, 30) + "\n" + frame.substring(30) + frame;
        CloneScan.Scan scan = clones.scan(text);
        assertThat(scan.cloneGroups()).isEqualTo(1);
        assertThat(scan.cloneFrames()).isEqualTo(2);
    }

    @Test
    void shortTextHasNoFrames() {
        CloneScan.Scan scan = clones.scan("short");
        assertThat(scan.scanned()).isZero();
        assertThat(scan.cloneGroups()).isZero();
    }

    @Test
    void emptyTextHasNoFrames() {
        CloneScan.Scan scan = clones.scan("");
        assertThat(scan.scanned()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> clones.scan(null))
                .isInstanceOf(CloneException.class);
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(CloneScan.MAX_CHARS + 1);
        assertThatThrownBy(() -> clones.scan(huge))
                .isInstanceOf(CloneException.class)
                .hasMessageContaining("exceeds");
    }
}
