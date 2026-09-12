package org.webjcvi.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KeyWidthTest {

    private final KeyWidth keys = new KeyWidth();

    @Test
    void sharedEightCharHeaderIsUniqueAtNine() {
        String a = "HEADHEAD" + "A".repeat(62);
        String b = "HEADHEAD" + "B".repeat(62);
        KeyWidth.Scan scan = keys.measure(a + b);
        assertThat(scan.scanned()).isEqualTo(2);
        assertThat(scan.uniqueAt()).isEqualTo(9);
        assertThat(scan.uniqueShareAtFloor()).isEqualTo(0.5);
        assertThat(scan.uniqueShareAtWrap()).isEqualTo(1.0);
        assertThat(scan.floorLength()).isEqualTo(8);
        assertThat(scan.wrapWidth()).isEqualTo(70);
    }

    @Test
    void alreadyUniqueAtFloorReportsEight() {
        String a = "AAAAAAAA" + "X".repeat(62);
        String b = "BBBBBBBB" + "Y".repeat(62);
        KeyWidth.Scan scan = keys.measure(a + b);
        assertThat(scan.uniqueAt()).isEqualTo(8);
        assertThat(scan.uniqueShareAtFloor()).isEqualTo(1.0);
        assertThat(scan.uniqueShareAtWrap()).isEqualTo(1.0);
    }

    @Test
    void identicalFramesNeverBecomeUnique() {
        String frame = "C".repeat(70);
        KeyWidth.Scan scan = keys.measure(frame + frame);
        assertThat(scan.uniqueAt()).isZero();
        assertThat(scan.uniqueShareAtFloor()).isEqualTo(0.5);
        assertThat(scan.uniqueShareAtWrap()).isEqualTo(0.5);
    }

    @Test
    void lastBaseDifferenceIsUniqueAtWrap() {
        String a = "T".repeat(69) + "A";
        String b = "T".repeat(69) + "G";
        KeyWidth.Scan scan = keys.measure(a + b);
        assertThat(scan.uniqueAt()).isEqualTo(70);
        assertThat(scan.uniqueShareAtFloor()).isEqualTo(0.5);
        assertThat(scan.uniqueShareAtWrap()).isEqualTo(1.0);
    }

    @Test
    void newlinesAreDroppedBeforeFraming() {
        String a = "HEADHEAD" + "A".repeat(62);
        String b = "HEADHEAD" + "B".repeat(62);
        String text = a.substring(0, 30) + "\n" + a.substring(30) + b;
        KeyWidth.Scan scan = keys.measure(text);
        assertThat(scan.uniqueAt()).isEqualTo(9);
    }

    @Test
    void shortTextHasNoFrames() {
        KeyWidth.Scan scan = keys.measure("short");
        assertThat(scan.scanned()).isZero();
        assertThat(scan.uniqueAt()).isZero();
        assertThat(scan.samples()).isEmpty();
    }

    @Test
    void emptyTextHasNoFrames() {
        KeyWidth.Scan scan = keys.measure("");
        assertThat(scan.scanned()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> keys.measure(null))
                .isInstanceOf(KeyException.class);
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(KeyWidth.MAX_CHARS + 1);
        assertThatThrownBy(() -> keys.measure(huge))
                .isInstanceOf(KeyException.class)
                .hasMessageContaining("exceeds");
    }
}
