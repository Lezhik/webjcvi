package org.webjcvi.mirror;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MirrorJointTest {

    private final MirrorJoint mirrors = new MirrorJoint();

    @Test
    void reverseNeighborsAreJointsWhenNotCopies() {
        MirrorJoint.Scan scan = mirrors.scan("abcddcba", 4);
        assertThat(scan.scanned()).isEqualTo(1);
        assertThat(scan.jointCount()).isEqualTo(1);
        assertThat(scan.hits().get(0).left()).isEqualTo("ABCD");
        assertThat(scan.hits().get(0).right()).isEqualTo("DCBA");
        assertThat(scan.hits().get(0).identityDistance()).isEqualTo(4);
    }

    @Test
    void repeatedBlocksAreNotMirrorJoints() {
        MirrorJoint.Scan scan = mirrors.scan("abcdabcd", 4);
        assertThat(scan.jointCount()).isZero();
    }

    @Test
    void emptyTextHasNoWindows() {
        MirrorJoint.Scan scan = mirrors.scan("");
        assertThat(scan.scanned()).isZero();
        assertThat(scan.jointCount()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> mirrors.scan(null))
                .isInstanceOf(MirrorException.class);
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(MirrorJoint.MAX_CHARS + 1);
        assertThatThrownBy(() -> mirrors.scan(huge))
                .isInstanceOf(MirrorException.class)
                .hasMessageContaining("exceeds");
    }
}
