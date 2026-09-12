package org.webjcvi.phase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PhaseJointTest {

    private final PhaseJoint joints = new PhaseJoint();

    @Test
    void reverseAtThePeakColumnIsAHit() {
        String text = "x".repeat(19) + "abcddcba";
        PhaseJoint.Scan scan = joints.scan(text);
        assertThat(scan.scanned()).isEqualTo(1);
        assertThat(scan.hitCount()).isEqualTo(1);
        assertThat(scan.hits().get(0).left()).isEqualTo("ABCD");
        assertThat(scan.hits().get(0).right()).isEqualTo("DCBA");
        assertThat(scan.hits().get(0).offset()).isEqualTo(19);
    }

    @Test
    void newlinesAreDroppedBeforePhasing() {
        String text = "x".repeat(10) + "\n" + "x".repeat(9) + "abcddcba";
        PhaseJoint.Scan scan = joints.scan(text);
        assertThat(scan.hitCount()).isEqualTo(1);
        assertThat(scan.hits().get(0).offset()).isEqualTo(19);
    }

    @Test
    void repeatedBlocksAtTheColumnAreNotHits() {
        String text = "x".repeat(19) + "abcdabcd";
        PhaseJoint.Scan scan = joints.scan(text);
        assertThat(scan.scanned()).isEqualTo(1);
        assertThat(scan.hitCount()).isZero();
    }

    @Test
    void shortTextHasNoWindows() {
        PhaseJoint.Scan scan = joints.scan("abcddcba");
        assertThat(scan.scanned()).isZero();
        assertThat(scan.hitCount()).isZero();
    }

    @Test
    void emptyTextHasNoWindows() {
        PhaseJoint.Scan scan = joints.scan("");
        assertThat(scan.scanned()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> joints.scan(null))
                .isInstanceOf(PhaseException.class);
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(PhaseJoint.MAX_CHARS + 1);
        assertThatThrownBy(() -> joints.scan(huge))
                .isInstanceOf(PhaseException.class)
                .hasMessageContaining("exceeds");
    }
}
