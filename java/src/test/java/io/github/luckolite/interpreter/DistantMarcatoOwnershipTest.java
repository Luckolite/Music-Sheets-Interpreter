// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** Original distant-caret ownership, finite-shaft and independent-rule controls. */
public class DistantMarcatoOwnershipTest {
    private static final int W = 1200, H = 1600;
    private final byte[] gray = new byte[W * H], labels = new byte[W * H];
    private final NoteArticulationDetector.Anchor note =
            new NoteArticulationDetector.Anchor(307, 250, 14, 0);

    private void rectangle(int l, int r, int t, int b, int shade) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) gray[y * W + x] = (byte) shade;
    }

    private void setup() {
        Arrays.fill(gray, (byte) 250);
        for (int dy = 0; dy <= 18; dy++) {
            int shift = Math.round(dy * 7 / 18f);
            rectangle(307 - shift, 308 - shift, 143 + dy, 143 + dy, 35);
            rectangle(306 + shift, 307 + shift, 143 + dy, 143 + dy, 35);
        }
        rectangle(313, 313, 181, 247, 35);
        rectangle(313, 337, 181, 184, 35);
    }

    private boolean owned() throws Exception {
        var type = Class.forName(NoteArticulationDetector.class.getName() + "$Glyph");
        var constructor =
                type.getDeclaredConstructor(
                        int.class, int.class, int.class, int.class, int.class, int[].class);
        constructor.setAccessible(true);
        var mark = constructor.newInstance(300, 143, 314, 161, 1, new int[] {143 * W + 307});
        var method =
                NoteArticulationDetector.class.getDeclaredMethod(
                        "beamOwnedMarcato",
                        type,
                        NoteArticulationDetector.Anchor.class,
                        byte[].class,
                        int.class,
                        int.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, mark, note, gray, W, H);
    }

    @Test
    public void longHeadEdgeShaftAndThickBeamOwnTheCaret() throws Exception {
        setup();
        assertTrue(owned());
    }

    @Test
    public void missingShaftCannotBorrowTheBeam() throws Exception {
        setup();
        rectangle(313, 313, 188, 244, 250);
        assertFalse(owned());
    }

    @Test
    public void brokenShaftCannotBorrowTheBeam() throws Exception {
        setup();
        rectangle(313, 313, 207, 222, 250);
        assertFalse(owned());
    }

    @Test
    public void missingBeamCannotOwnTheCaret() throws Exception {
        setup();
        rectangle(314, 337, 181, 184, 250);
        assertFalse(owned());
    }

    @Test
    public void distantNeighborStemDoesNotOwnTheCaret() throws Exception {
        setup();
        rectangle(313, 337, 181, 247, 250);
        rectangle(335, 335, 181, 247, 35);
        rectangle(335, 359, 181, 184, 35);
        assertFalse(owned());
    }

    @Test
    public void aThinStaffRuleIsNotAThickBeam() throws Exception {
        setup();
        rectangle(300, 360, 181, 184, 250);
        rectangle(313, 313, 181, 247, 35);
        rectangle(280, 360, 181, 181, 35);
        assertFalse(owned());
    }

    @Test
    public void disconnectedBeamDoesNotOwnTheCaret() throws Exception {
        setup();
        rectangle(314, 320, 181, 184, 250);
        assertFalse(owned());
    }

    @Test
    public void completeDetectorRecoversOnlyTheProvedDistantMarcato() {
        setup();
        assertEquals(
                NoteArticulation.MARCATO,
                NoteArticulationDetector.detect(labels, gray, W, H, List.of(note))[0]);
    }

    @Test
    public void completeDetectorRejectsUnownedDistantCaret() {
        setup();
        rectangle(313, 337, 181, 247, 250);
        assertEquals(0, NoteArticulationDetector.detect(labels, gray, W, H, List.of(note))[0]);
    }

    @Test
    public void provedCaretAtSixPointThreeGapsRemainsOwned() {
        setup();
        var closer = new NoteArticulationDetector.Anchor(307, 240.2f, 14, 0);
        assertEquals(
                NoteArticulation.MARCATO,
                NoteArticulationDetector.detect(labels, gray, W, H, List.of(closer))[0]);
    }

    @Test
    public void distantUpBowIsNotMarcato() {
        setup();
        rectangle(299, 315, 142, 162, 250);
        for (int dy = 0; dy <= 18; dy++) {
            int shift = Math.round(dy * 7 / 18f);
            rectangle(307 - shift, 308 - shift, 161 - dy, 161 - dy, 35);
            rectangle(306 + shift, 307 + shift, 161 - dy, 161 - dy, 35);
        }
        assertEquals(0, NoteArticulationDetector.detect(labels, gray, W, H, List.of(note))[0]);
    }

    @Test
    public void distantLetterCrossbarIsNotMarcato() {
        setup();
        rectangle(302, 312, 154, 155, 35);
        assertEquals(0, NoteArticulationDetector.detect(labels, gray, W, H, List.of(note))[0]);
    }

    @Test
    public void shaftContinuingPastBeamIsNotAnEndpoint() throws Exception {
        setup();
        rectangle(313, 313, 165, 180, 35);
        assertFalse(owned());
    }

    @Test
    public void independentThinRuleAboveBeamDoesNotExtendShaft() throws Exception {
        setup();
        rectangle(220, 400, 175, 177, 35);
        assertTrue(owned());
    }

    @Test
    public void isolatedTextStrokeDoesNotInvalidateIndependentRule() throws Exception {
        setup();
        rectangle(220, 400, 175, 177, 35);
        rectangle(348, 349, 165, 177, 35);
        assertTrue(owned());
    }

    @Test
    public void slopedBeamMeetingRuleFarFromShaftDoesNotExtendShaft() throws Exception {
        setup();
        rectangle(220, 400, 175, 177, 35);
        rectangle(220, 273, 173, 181, 35);
        rectangle(348, 349, 165, 177, 35);
        assertTrue(owned());
    }

    @Test
    public void continuingShaftWithThinRuleStillFails() throws Exception {
        setup();
        rectangle(313, 313, 165, 180, 35);
        rectangle(220, 400, 175, 177, 35);
        assertFalse(owned());
    }

    @Test
    public void broadInkBandCannotBeIgnoredAsRule() throws Exception {
        setup();
        rectangle(220, 400, 173, 177, 35);
        assertFalse(owned());
    }

    @Test
    public void aSingleThinColumnCannotEstablishARule() throws Exception {
        setup();
        rectangle(220, 400, 175, 177, 35);
        for (int side : new int[] {-1, 1})
            for (int sample = 1; sample < 5; sample++) {
                int x = 313 + side * Math.round(14 * (2f + sample * .5f));
                rectangle(x, x, 173, 179, 35);
            }
        var method =
                NoteArticulationDetector.class.getDeclaredMethod(
                        "thinIndependentRule",
                        byte[].class,
                        int.class,
                        int.class,
                        int.class,
                        int.class,
                        float.class);
        method.setAccessible(true);
        assertFalse((boolean) method.invoke(null, gray, W, H, 313, 176, 14f));
    }

    @Test
    public void belowHeadCaretUsesTheFacingBeamEdge() {
        Arrays.fill(gray, (byte) 250);
        for (int dy = 0; dy <= 18; dy++) {
            int shift = Math.round(dy * 7 / 18f);
            rectangle(307 - shift, 308 - shift, 348 - dy, 348 - dy, 35);
            rectangle(306 + shift, 307 + shift, 348 - dy, 348 - dy, 35);
        }
        rectangle(313, 313, 253, 310, 35);
        rectangle(313, 337, 307, 310, 35);
        assertEquals(
                NoteArticulation.MARCATO,
                NoteArticulationDetector.detect(labels, gray, W, H, List.of(note))[0]);
    }
}
