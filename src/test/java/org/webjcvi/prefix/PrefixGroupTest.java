package org.webjcvi.prefix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PrefixGroupTest {

    private final PrefixGroup groups = new PrefixGroup();

    @Test
    void sharedPrefixWithDifferentBodiesIsAFamily() {
        String a = "HEADHEAD" + "A".repeat(62);
        String b = "HEADHEAD" + "B".repeat(62);
        PrefixGroup.Scan scan = groups.group(a + b);
        assertThat(scan.scanned()).isEqualTo(2);
        assertThat(scan.distinct()).isEqualTo(1);
        assertThat(scan.familyCount()).isEqualTo(1);
        assertThat(scan.familyFrames()).isEqualTo(2);
        assertThat(scan.topPrefix()).isEqualTo("HEADHEAD");
        assertThat(scan.hits()).hasSize(1);
        assertThat(scan.hits().get(0).count()).isEqualTo(2);
    }

    @Test
    void differentPrefixesAreNotFamilies() {
        String a = "AAAAAAAA" + "X".repeat(62);
        String b = "BBBBBBBB" + "Y".repeat(62);
        PrefixGroup.Scan scan = groups.group(a + b);
        assertThat(scan.scanned()).isEqualTo(2);
        assertThat(scan.distinct()).isEqualTo(2);
        assertThat(scan.familyCount()).isZero();
        assertThat(scan.hits()).isEmpty();
    }

    @Test
    void newlinesAreDroppedBeforeFraming() {
        String frame = "HEADHEAD" + "C".repeat(62);
        String other = "HEADHEAD" + "D".repeat(62);
        String text = frame.substring(0, 30) + "\n" + frame.substring(30) + other;
        PrefixGroup.Scan scan = groups.group(text);
        assertThat(scan.familyCount()).isEqualTo(1);
        assertThat(scan.familyFrames()).isEqualTo(2);
    }

    @Test
    void shortTextHasNoFrames() {
        PrefixGroup.Scan scan = groups.group("short");
        assertThat(scan.scanned()).isZero();
        assertThat(scan.familyCount()).isZero();
    }

    @Test
    void emptyTextHasNoFrames() {
        PrefixGroup.Scan scan = groups.group("");
        assertThat(scan.scanned()).isZero();
    }

    @Test
    void nullTextIsRejected() {
        assertThatThrownBy(() -> groups.group(null))
                .isInstanceOf(PrefixException.class);
    }

    @Test
    void rejectsOversizeText() {
        String huge = "x".repeat(PrefixGroup.MAX_CHARS + 1);
        assertThatThrownBy(() -> groups.group(huge))
                .isInstanceOf(PrefixException.class)
                .hasMessageContaining("exceeds");
    }
}
