// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original uneven caret strokes crossed by an independently long thin rule. */
public class RuleCrossedCaretTest {
    @Test
    public void hugeFiniteFrameCannotEnterRuleRecovery() {
        setup(true, false, true);
        var invalid =
                new NoteArticulationDetector.Anchor(
                        Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, 0);
        assertEquals(0, NoteArticulationDetector.detect(labels, gray, W, H, List.of(invalid))[0]);
    }

    @Test
    public void offPageAnchorCannotEnterRuleRecovery() {
        setup(true, false, true);
        var invalid = new NoteArticulationDetector.Anchor(note.x(), H + 1, note.gap(), 0);
        assertEquals(0, NoteArticulationDetector.detect(labels, gray, W, H, List.of(invalid))[0]);
    }

    static final int W = 1200, H = 1600;
    final byte[] gray = new byte[W * H], labels = new byte[W * H];
    final NoteArticulationDetector.Anchor note =
            new NoteArticulationDetector.Anchor(307, 250, 14, 0);

    void rect(int left, int right, int top, int bottom) {
        for (int y = top; y <= bottom; y++)
            for (int x = left; x <= right; x++) gray[y * W + x] = 35;
    }

    void setup(boolean caret, boolean upBow, boolean shaft) {
        Arrays.fill(gray, (byte) 250);
        if (caret)
            for (int d = 0; d <= 18; d++) {
                int offset = Math.round(d * 7 / 18f), y = upBow ? 161 - d : 143 + d;
                rect(307 - offset, 308 - offset, y, y);
                rect(306 + offset, 310 + offset, y, y);
            }
        rect(220, 400, 153, 154);
        if (shaft) {
            rect(313, 313, 181, 247);
            rect(313, 337, 181, 184);
        }
    }

    int detect() {
        return NoteArticulationDetector.detect(labels, gray, W, H, List.of(note))[0];
    }

    @Test
    public void provedUnevenCaretCrossedByRuleIsRecovered() {
        setup(true, false, true);
        assertEquals(NoteArticulation.MARCATO, detect());
    }

    @Test
    public void upBowCrossedByRuleIsNotMarcato() {
        setup(true, true, true);
        assertEquals(0, detect());
    }

    @Test
    public void unownedCrossedCaretDoesNotBorrowBeam() {
        setup(true, false, false);
        assertEquals(0, detect());
    }

    @Test
    public void ruleAndStemDoNotManufactureAMark() {
        setup(false, false, true);
        assertEquals(0, detect());
    }

    @Test
    public void interiorLetterCrossbarIsRejected() {
        setup(true, false, true);
        rect(302, 314, 156, 157);
        assertEquals(0, detect());
    }

    @Test
    public void locallyShortStripeIsNotAProvedRule() {
        setup(true, false, true);
        for (int y = 153; y <= 154; y++)
            for (int x = 220; x <= 400; x++) if (x < 290 || x > 324) gray[y * W + x] = (byte) 250;
        assertEquals(0, detect());
    }

    @Test
    public void thickBeamCannotServeAsAThinRule() {
        setup(true, false, true);
        rect(220, 400, 150, 157);
        assertEquals(0, detect());
    }

    @Test
    public void acceptedNotationCannotBeReclassified() {
        setup(true, false, true);
        for (int y = 143; y <= 161; y++)
            for (int x = 299; x <= 318; x++)
                labels[y * W + x] = OmrMeasurePostProcessor.STEM_OR_REST;
        assertEquals(0, detect());
    }

    @Test
    public void detachedShaftCannotOwnCaret() {
        setup(true, false, true);
        for (int y = 200; y <= 218; y++) gray[y * W + 313] = (byte) 250;
        assertEquals(0, detect());
    }

    @Test
    public void incompleteArmCannotBorrowOtherInk() {
        setup(true, false, true);
        for (int y = 143; y <= 161; y++)
            for (int x = 310; x <= 318; x++) if (y < 153 || y > 154) gray[y * W + x] = (byte) 250;
        assertEquals(0, detect());
    }

    @Test
    public void displacedMarkCannotBorrowNearbyShaft() {
        setup(true, false, true);
        var other =
                new NoteArticulationDetector.Anchor(
                        note.x() + note.gap() * 2, note.y(), note.gap(), 0);
        assertEquals(0, NoteArticulationDetector.detect(labels, gray, W, H, List.of(other))[0]);
    }

    @Test
    public void inputsRemainUnmodified() {
        setup(true, false, true);
        byte[] g = gray.clone(), l = labels.clone();
        detect();
        assertArrayEquals(g, gray);
        assertArrayEquals(l, labels);
    }

    @Test
    public void staffCrossedWordIsNotMarcato() {
        setup(true, false, true);
        for (int x : new int[] {322, 334, 346}) {
            rect(x, x + 1, 143, 161);
            rect(x, x + 7, 143, 144);
            rect(x, x + 6, 151, 152);
            rect(x, x + 7, 160, 161);
        }
        assertEquals(0, detect());
    }

    @Test
    public void staffCrossedWordEndingIsNotMarcato() {
        setup(true, false, true);
        for (int x : new int[] {261, 273, 285}) {
            rect(x, x + 1, 143, 161);
            rect(x, x + 7, 143, 144);
            rect(x, x + 6, 151, 152);
            rect(x, x + 7, 160, 161);
        }
        assertEquals(0, detect());
    }

    @Test
    public void nonFiniteGapCannotEnterRuleRecovery() {
        setup(true, false, true);
        var invalid =
                new NoteArticulationDetector.Anchor(note.x(), note.y(), Float.POSITIVE_INFINITY, 0);
        assertEquals(0, NoteArticulationDetector.detect(labels, gray, W, H, List.of(invalid))[0]);
    }
}
