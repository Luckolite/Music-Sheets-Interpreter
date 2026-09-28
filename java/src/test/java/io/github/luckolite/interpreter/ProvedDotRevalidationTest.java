// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original round dot whose line-to-space offset exceeds a compressed semantic gap. */
public class ProvedDotRevalidationTest {
    static final int W = 220, H = 160;

    byte[] pixels() {
        byte[] g = new byte[W * H];
        Arrays.fill(g, (byte) 255);
        for (int y = 90; y <= 94; y++)
            for (int x = 109; x <= 113; x++)
                if ((x - 111) * (x - 111) + (y - 92) * (y - 92) <= 5) g[y * W + x] = (byte) 100;
        return g;
    }

    Object make(String name, Object... values) throws Exception {
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$" + name);
        var c = type.getDeclaredConstructors()[0];
        c.setAccessible(true);
        return c.newInstance(values);
    }

    int count(float gap, boolean restOwns, boolean accidentalOwns, byte[] g) throws Exception {
        Object head = make("Component", 100, 84, 96, 96, 104, 90f, 100f);
        var event = new ScoreNoteEvent(0, .4f, -4, 0, 1, 100f / H, false, 1, 0, 2, 1, 1, 0, 0, 30);
        Object note = make("DetectedNote", event, head, 10f);
        var rests = List.of(new ScoreRestEvent(restOwns ? 0 : 1, 111f / W, 92f / H, 10f / H, 0, 1));
        Object exclusion =
                make(
                        "Component",
                        20,
                        accidentalOwns ? 108 : 190,
                        accidentalOwns ? 114 : 195,
                        89,
                        95,
                        accidentalOwns ? 111f : 192f,
                        92f);
        try {
            var method =
                    OmrScoreInterpreter.class.getDeclaredMethod(
                            "dotsOutsideRests",
                            List.class,
                            note.getClass(),
                            List.class,
                            List.class,
                            byte[].class,
                            int.class,
                            int.class,
                            List.class,
                            head.getClass(),
                            List.class,
                            float.class);
            method.setAccessible(true);
            return (int)
                    method.invoke(
                            null,
                            List.of(),
                            note,
                            rests,
                            List.of(new MeasureRegion(0, 1, 0, 1), new MeasureRegion(0, 1, 0, 1)),
                            g,
                            W,
                            H,
                            List.of(exclusion),
                            head,
                            List.of(head),
                            gap);
        } catch (NoSuchMethodException baseline) {
            var method =
                    OmrScoreInterpreter.class.getDeclaredMethod(
                            "dotsOutsideRests",
                            List.class,
                            note.getClass(),
                            List.class,
                            List.class,
                            byte[].class,
                            int.class,
                            int.class,
                            List.class,
                            head.getClass(),
                            List.class);
            method.setAccessible(true);
            return (int)
                    method.invoke(
                            null,
                            List.of(),
                            note,
                            rests,
                            List.of(new MeasureRegion(0, 1, 0, 1), new MeasureRegion(0, 1, 0, 1)),
                            g,
                            W,
                            H,
                            List.of(exclusion),
                            head,
                            List.of(head));
        }
    }

    @Test
    public void provedSpacingSurvivesRevalidation() throws Exception {
        assertEquals(1, count(15, false, false, pixels()));
    }

    @Test
    public void uncorrectedSpacingCannotSeeOffsetDot() throws Exception {
        assertEquals(0, count(10, false, false, pixels()));
    }

    @Test
    public void restOwnedDotRemainsExcluded() throws Exception {
        assertEquals(0, count(15, true, false, pixels()));
    }

    @Test
    public void accidentalOwnedDotRemainsExcluded() throws Exception {
        assertEquals(0, count(15, false, true, pixels()));
    }

    @Test
    public void pixelsAreNotModified() throws Exception {
        byte[] g = pixels(), copy = g.clone();
        count(15, false, false, g);
        assertArrayEquals(copy, g);
    }
}
