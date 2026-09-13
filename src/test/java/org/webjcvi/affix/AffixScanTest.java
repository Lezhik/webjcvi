package org.webjcvi.affix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AffixScanTest {

    private final AffixScan affixes = new AffixScan();

    @Test
    void tailDiscriminatesWhenOnlyTheSuffixDiffers() {
        String a = "T".repeat(62) + "AAAAAAAA";
        String b = "T".repeat(62) + "CCCCCCCC";
        AffixScan.Scan scan = affixes.measure(a + b);
        assertThat(scan.scanned()).isEqualTo(2);
        assertThat(scan.tailUniqueAt()).isEqualTo(8);
        assertThat(scan.leadUniqueAt()).isGreaterThan(scan.tailUniqueAt());
        assertThat(scan.cheaperEnd()).isEqualTo("tail");
        assertThat(scan.tailShareAtFloor()).isEqualTo(1.0);
        assertThat(scan.leadShareAtFloor()).isEqualTo(0.5);
    }

    @Test
    void leadDiscriminatesWhenOnlyThePrefixDiffers() {
        String a = "AAAAAAAA" + "T".repeat(62);
        String b = "CCCCCCCC" + "T".repeat(62);
        AffixScan.Scan scan = affixes.measure(a + b);
        assertThat(scan.leadUniqueAt()).isEqualTo(8);
        assertThat(scan.tailUniqueAt()).isGreaterThan(scan.leadUniqueAt());
        assertThat(scan.cheaperEnd()).isEqualTo("lead");
    }

    @Test
    void bothEndsUniqueAtFloorIsATie() {
        String a = "A".repeat(70);
        String b = "C".repeat(70);
        AffixScan.Scan scan = affixes.measure(a + b);
        assertThat(scan.leadUniqueAt()).isEqualTo(8);
        assertThat(scan.tailUniqueAt()).isEqualTo(8);
        assertThat(scan.cheaperEnd()).isEqualTo("tie");
    }

    @Test
    void identicalFramesNeverSaturate() {
        String frame = "G".repeat(70);
        AffixScan.Scan scan = affixes.measure(frame + frame);
        assertThat(scan.leadUniqueAt()).isZero();
        assertThat(scan.tailUniqueAt()).isZero();
        assertThat(scan.cheaperEnd()).isEqualTo("none");
    }

    @Test
    void newlinesAreDroppedBeforeFraming() {
        String a = "T".repeat(62) + "AAAAAAAA";
        String b = "T".repeat(62) + "CCCCCCCC";
        String text = a.substring(0, 30) + "\n" + a.substring(30) + b;
        AffixScan.Scan scan = affixes.measure(text);
        assertThat(scan.cheaperEnd()).isEqualTo("tail");
        assertThat(scan.tailUniqueAt()).isEqualTo(8);
    }

    @Test
    void shortTextHasNoFrames() {
        AffixScan.Scan scan = affixes.measure("short");
        assertThat(scan.scanned()).isZero();
        assertThat(scan.cheaperEnd()).isEqualTo("none");
    }

    @Test
    void emptyTextHasNoFrames() {
        AffixScan.Scan scan = affixes.measure("");
        assertThat(scan.scanned()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> affixes.measure(null))
                .isInstanceOf(AffixException.class);
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(AffixScan.MAX_CHARS + 1);
        assertThatThrownBy(() -> affixes.measure(huge))
                .isInstanceOf(AffixException.class)
                .hasMessageContaining("exceeds");
    }
}
