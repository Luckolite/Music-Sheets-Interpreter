// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original ellipses and a shared shaft, never a score crop. */
public class FilledSecondChordTest {
    List<?> split(HollowSecondChordTest p) throws Exception {
        Object head = p.component(115, 163, 81, 144), staff = p.staff();
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "splitFilledSecondChord",
                        byte[].class,
                        byte[].class,
                        int.class,
                        int.class,
                        head.getClass(),
                        staff.getClass(),
                        int.class);
        m.setAccessible(true);
        return (List<?>) m.invoke(null, p.labels, p.gray, p.W, p.H, head, staff, 0);
    }

    @Test
    public void fourPrintedFilledTonesSurviveFusedMask() throws Exception {
        assertEquals(4, split(new HollowSecondChordTest(true)).size());
    }

    @Test
    public void filledRectangleIsNotFourTones() throws Exception {
        var p = new HollowSecondChordTest(true);
        for (int y = 81; y <= 144; y++) for (int x = 115; x <= 163; x++) p.gray[y * p.W + x] = 0;
        assertTrue(split(p).isEmpty());
    }

    @Test
    public void noPrintedInkCannotBecomeNotes() throws Exception {
        var p = new HollowSecondChordTest(true);
        Arrays.fill(p.gray, (byte) 255);
        assertTrue(split(p).isEmpty());
    }

    @Test
    public void hollowSecondMustNotBeTrimmedToItsFilledRim() throws Exception {
        var p = new HollowSecondChordTest(true);
        for (int y = 126; y <= 144; y++)
            for (int x = 115; x <= 141; x++)
                if (Math.pow((x - 128) / 13d, 2) + Math.pow((y - 135) / 9d, 2) < .45)
                    p.gray[y * p.W + x] = (byte) 255;
        assertTrue(split(p).isEmpty());
    }

    @Test
    public void commonStemIsRequired() throws Exception {
        var p = new HollowSecondChordTest(true);
        Arrays.fill(p.gray, (byte) 255);
        for (int y : new int[] {90, 108, 126}) p.oval(150, y, true);
        p.oval(128, 135, true);
        assertTrue(split(p).isEmpty());
    }

    @Test
    public void originalPixelsRemainUnchanged() throws Exception {
        var p = new HollowSecondChordTest(true);
        var g = p.gray.clone();
        var l = p.labels.clone();
        split(p);
        assertArrayEquals(g, p.gray);
        assertArrayEquals(l, p.labels);
    }
}
