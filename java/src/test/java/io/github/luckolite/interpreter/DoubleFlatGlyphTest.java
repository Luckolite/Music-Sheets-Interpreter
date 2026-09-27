// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original paired flat bowls drawn from rectangles, never score pixels. */
public class DoubleFlatGlyphTest {
    final byte[] labels = new byte[180 * 140];

    void rect(int l, int r, int t, int b) {
        for (int y = t; y <= b; y++) for (int x = l; x <= r; x++) labels[y * 180 + x] = 3;
    }

    void flat(int x, int y) {
        rect(x, x + 2, y, y + 43);
        rect(x + 2, x + 10, y + 24, y + 27);
        rect(x + 9, x + 12, y + 26, y + 35);
        rect(x + 5, x + 10, y + 35, y + 39);
        rect(x + 2, x + 6, y + 39, y + 42);
    }

    Object make(String name, Object... args) throws Exception {
        var c =
                Class.forName(OmrScoreInterpreter.class.getName() + "$" + name)
                        .getDeclaredConstructors()[0];
        c.setAccessible(true);
        return c.newInstance(args);
    }

    Object candidate() throws Exception {
        int l = 180, r = -1, t = 140, b = -1, n = 0;
        long sx = 0, sy = 0;
        for (int y = 0; y < 140; y++)
            for (int x = 0; x < 180; x++)
                if (labels[y * 180 + x] == 3) {
                    l = Math.min(l, x);
                    r = Math.max(r, x);
                    t = Math.min(t, y);
                    b = Math.max(b, y);
                    n++;
                    sx += x;
                    sy += y;
                }
        return make(
                "AccidentalCandidate",
                make("Component", n, l, r, t, b, sx / (float) n, sy / (float) n),
                (byte) 3);
    }

    float center() throws Exception {
        Object c = candidate();
        var m =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "doubleFlatPitchCenter",
                        byte[].class,
                        int.class,
                        int.class,
                        c.getClass(),
                        float.class);
        m.setAccessible(true);
        return (float) m.invoke(null, labels, 180, 140, c, 20f);
    }

    @Test
    public void pairedBowlsProveDoubleFlat() throws Exception {
        flat(30, 30);
        flat(46, 30);
        assertTrue(Float.isFinite(center()));
    }

    @Test
    public void singleFlatCannotBecomeDoubleFlat() throws Exception {
        flat(30, 30);
        assertFalse(Float.isFinite(center()));
    }

    @Test
    public void staggeredFlatsBelongToDifferentPitches() throws Exception {
        flat(30, 30);
        flat(46, 40);
        assertFalse(Float.isFinite(center()));
    }

    @Test
    public void twoTallSpinesWithoutBowlsAreNotDoubleFlat() throws Exception {
        rect(30, 32, 30, 73);
        rect(46, 48, 30, 73);
        assertFalse(Float.isFinite(center()));
    }

    @Test
    public void sharpCrossbarsAreNotTwoFlatBowls() throws Exception {
        rect(30, 32, 30, 73);
        rect(46, 48, 30, 73);
        rect(27, 52, 41, 44);
        rect(27, 52, 58, 61);
        assertFalse(Float.isFinite(center()));
    }

    @Test
    public void offsetNaturalSpinesAreNotTwoFlatBowls() throws Exception {
        rect(30, 32, 30, 64);
        rect(44, 46, 42, 76);
        rect(30, 46, 42, 45);
        rect(30, 46, 61, 64);
        assertFalse(Float.isFinite(center()));
    }

    @Test
    public void eventRetainsTwoSemitoneLowering() {
        var e = new ScoreNoteEvent(0, .4f, 3, 0, 1, .5f, false, 0, 0, -2, 1);
        assertEquals(-2, e.writtenAccidental());
        assertEquals(-2, ScoreNoteEvent.accidentalSemitones(e.writtenAccidental()));
    }

    @Test
    public void invalidLowerAccidentalStillFallsBack() {
        var e = new ScoreNoteEvent(0, .4f, 3, 0, 1, .5f, false, 0, 0, -3, 1);
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY, e.writtenAccidental());
    }
}
