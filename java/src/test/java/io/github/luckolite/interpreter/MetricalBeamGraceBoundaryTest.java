// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original mixed-size heads in a regular beam and a disconnected ornamental prefix. */
public class MetricalBeamGraceBoundaryTest {
    private List<ScoreNoteEvent> run(boolean connected, boolean secondStaff) throws Exception {
        int w = 320, h = 250;
        float gap = 14.3f;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        var hc =
                Class.forName(OmrScoreInterpreter.class.getName() + "$Component")
                        .getDeclaredConstructors()[0];
        hc.setAccessible(true);
        var dc =
                Class.forName(OmrScoreInterpreter.class.getName() + "$DetectedNote")
                        .getDeclaredConstructors()[0];
        dc.setAccessible(true);
        var detected = new ArrayList<Object>();
        var events = new ArrayList<ScoreNoteEvent>();
        int[] xs = {70, 108, 136, 164, 202}, ys = {100, 107, 121, 135, 100};
        for (int i = 0; i < xs.length; i++) {
            int x = xs[i],
                    y = ys[i],
                    rx = i == 0 || i == 4 ? 10 : 6,
                    ry = i == 0 || i == 4 ? 8 : 5,
                    area = 0;
            for (int yy = y - ry; yy <= y + ry; yy++)
                for (int xx = x - rx; xx <= x + rx; xx++)
                    if ((xx - x) * (xx - x) / (double) (rx * rx)
                                    + (yy - y) * (yy - y) / (double) (ry * ry)
                            <= 1) {
                        gray[yy * w + xx] = 40;
                        labels[yy * w + xx] = 2;
                        area++;
                    }
            int stem = x - rx + 1;
            for (int yy = y; yy <= 188; yy++) {
                gray[yy * w + stem] = 40;
                labels[yy * w + stem] = 1;
            }
            var head = hc.newInstance(area, x - rx, x + rx, y - ry, y + ry, (float) x, (float) y);
            var e =
                    new ScoreNoteEvent(
                            0,
                            x / (float) w,
                            2,
                            0,
                            secondStaff ? 2 : 1,
                            y / (float) h,
                            false,
                            0,
                            2,
                            2,
                            0);
            detected.add(dc.newInstance(e, head, gap));
            events.add(e);
        }
        for (int x = connected ? 61 : 103; x <= 159; x++)
            for (int beam = 0; beam < 2; beam++)
                for (int dy = 0; dy < 5; dy++) {
                    int y = 175 + beam * 9 + dy;
                    gray[y * w + x] = 40;
                    labels[y * w + x] = 1;
                }
        if (secondStaff) {
            var head = hc.newInstance(250, 113, 133, 205, 221, 123f, 213f);
            var e = new ScoreNoteEvent(0, 123f / w, 2, 1, 2, 213f / h, false, 0, 2, 2, 0);
            detected.add(2, dc.newInstance(e, head, gap));
            events.add(2, e);
        }
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "markGraceHeads",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        List.class,
                        List.class);
        m.setAccessible(true);
        m.invoke(null, labels, gray, w, h, detected, events);
        return events;
    }

    @Test
    public void smallerHeadsOnContinuousOrdinaryBeamKeepTheirBeats() throws Exception {
        assertEquals(
                0,
                run(true, false).stream()
                        .filter(n -> (n.articulations() & NoteOrnament.GRACE) != 0)
                        .count());
    }

    @Test
    public void OtherStaffNotesDoNotHideMetricalBeamOwnership() throws Exception {
        assertEquals(
                0,
                run(true, true).stream()
                        .filter(n -> (n.articulations() & NoteOrnament.GRACE) != 0)
                        .count());
    }

    @Test
    public void disconnectedReducedPrefixStillActsAsGraceNotes() throws Exception {
        assertTrue(
                run(false, false).stream()
                        .anyMatch(n -> (n.articulations() & NoteOrnament.GRACE) != 0));
    }
}
