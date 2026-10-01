// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original reduced-note pairs beside one full-size principal. */
public class PairedGraceAttachmentTest {
    private static final int W = 300, H = 240;

    private static List<ScoreNoteEvent> run(
            boolean trailing, boolean paired, int paper, float principalX, boolean fullPair)
            throws Exception {
        return run(trailing, paired, paper, principalX, fullPair, paired ? 2 : 1, 2);
    }

    private static List<ScoreNoteEvent> run(
            boolean trailing,
            boolean paired,
            int paper,
            float principalX,
            boolean fullPair,
            int printedBeams,
            int initialBeams)
            throws Exception {
        return run(
                trailing, paired, paper, principalX, fullPair, printedBeams, initialBeams, false);
    }

    private static List<ScoreNoteEvent> run(
            boolean trailing,
            boolean paired,
            int paper,
            float principalX,
            boolean fullPair,
            int printedBeams,
            int initialBeams,
            boolean interleaved)
            throws Exception {
        byte[] gray = new byte[W * H];
        Arrays.fill(gray, (byte) paper);
        for (int y = 110; y <= 150; y++) gray[y * W + 105] = 40;
        for (int y = 112; y <= 158; y++) gray[y * W + 130] = 40;
        for (int x = 105; x <= 130; x++) {
            int y = 110 + Math.round((x - 105) * 2f / 25);
            for (int b = 0; b < printedBeams; b++)
                for (int dy = 0; dy < 5; dy++)
                    gray[(y + b * (printedBeams == 3 ? 8 : 11) + dy) * W + x] = 40;
        }
        var hc =
                Class.forName(OmrScoreInterpreter.class.getName() + "$Component")
                        .getDeclaredConstructors()[0];
        hc.setAccessible(true);
        var dc =
                Class.forName(OmrScoreInterpreter.class.getName() + "$DetectedNote")
                        .getDeclaredConstructors()[0];
        dc.setAccessible(true);
        var events = new ArrayList<ScoreNoteEvent>();
        var detected = new ArrayList<Object>();
        float[] xs =
                trailing ? new float[] {principalX, 100, 125} : new float[] {100, 125, principalX};
        for (float x : xs) {
            boolean principal = x == principalX;
            float y = x == 125 ? 158 : 150;
            boolean full = principal || fullPair;
            int rx = full ? 11 : 5, ry = full ? 7 : 4;
            var head =
                    hc.newInstance(
                            full ? 260 : 65,
                            (int) x - rx,
                            (int) x + rx,
                            (int) y - ry,
                            (int) y + ry,
                            x,
                            y);
            var event =
                    new ScoreNoteEvent(
                            0,
                            x / W,
                            2,
                            0,
                            interleaved ? 2 : 1,
                            y / H,
                            false,
                            0,
                            principal ? 0 : initialBeams,
                            2,
                            principal ? 1 : 0);
            events.add(event);
            detected.add(dc.newInstance(event, head, 16f));
        }
        if (interleaved)
            for (int index : new int[] {2, 1}) {
                float x = trailing ? (index == 1 ? 80 : 112) : (index == 1 ? 112 : 140);
                var head = hc.newInstance(260, (int) x - 11, (int) x + 11, 208, 222, x, 215f);
                var event = new ScoreNoteEvent(0, x / W, 2, 1, 2, 215f / H, false, 0, 1, 2, 0);
                detected.add(index, dc.newInstance(event, head, 16f));
                events.add(index, event);
            }
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "markPairedGraces",
                        byte[].class,
                        int.class,
                        int.class,
                        List.class,
                        List.class);
        m.setAccessible(true);
        m.invoke(null, gray, W, H, detected, events);
        return events;
    }

    @Test
    public void otherStaffAttacksDoNotSplitPrefixPair() throws Exception {
        var r = run(false, true, 220, 180, false, 2, 3, true);
        assertTrue((r.get(0).articulations() & NoteOrnament.GRACE) != 0);
        assertTrue((r.get(2).articulations() & NoteOrnament.GRACE) != 0);
        assertEquals(2, r.get(0).beamCount());
        assertEquals(2, r.get(2).beamCount());
        assertEquals(0, r.get(1).articulations());
        assertEquals(0, r.get(3).articulations());
    }

    @Test
    public void otherStaffAttacksDoNotSplitTrailingPair() throws Exception {
        var r = run(true, true, 220, 55, false, 2, 3, true);
        assertTrue((r.get(2).articulations() & NoteOrnament.GRACE) != 0);
        assertTrue((r.get(4).articulations() & NoteOrnament.GRACE) != 0);
        assertEquals(2, r.get(2).beamCount());
        assertEquals(2, r.get(4).beamCount());
    }

    @Test
    public void provedDoubleBeamReplacesSpuriousThirdBeam() throws Exception {
        var r = run(false, true, 220, 180, false, 2, 3);
        assertTrue((r.get(0).articulations() & NoteOrnament.GRACE) != 0);
        assertEquals(2, r.get(0).beamCount());
        assertEquals(2, r.get(1).beamCount());
    }

    @Test
    public void genuineThreeBeamGracePairKeepsAllThreeBeams() throws Exception {
        var r = run(false, true, 220, 180, false, 3, 3);
        assertTrue((r.get(0).articulations() & NoteOrnament.GRACE) != 0);
        assertEquals(3, r.get(0).beamCount());
        assertEquals(3, r.get(1).beamCount());
    }

    @Test
    public void confirmedPairCanReachPrincipalThreeSpacesAway() throws Exception {
        var r = run(false, true, 220, 180, false);
        assertTrue((r.get(0).articulations() & 32768) != 0);
        assertTrue((r.get(1).articulations() & 32768) != 0);
        assertEquals(0, r.get(2).articulations());
    }

    @Test
    public void shadedPaperCannotExtendTheStems() throws Exception {
        var r = run(false, true, 155, 180, false);
        assertTrue((r.get(0).articulations() & 32768) != 0);
    }

    @Test
    public void terminalPairMayFollowThePrincipal() throws Exception {
        var r = run(true, true, 220, 50, false);
        assertEquals(0, r.get(0).articulations());
        assertTrue((r.get(1).articulations() & 32768) != 0);
        assertTrue((r.get(2).articulations() & 32768) != 0);
    }

    @Test
    public void singleBeamDoesNotProveThisPair() throws Exception {
        for (var n : run(false, false, 220, 180, false)) assertEquals(0, n.articulations());
    }

    @Test
    public void fullSizePrintedNotesStayMetrical() throws Exception {
        for (var n : run(false, true, 220, 180, true)) assertEquals(0, n.articulations());
    }

    @Test
    public void distantPrincipalCannotOwnThePair() throws Exception {
        for (var n : run(false, true, 220, 240, false)) assertEquals(0, n.articulations());
    }
}
