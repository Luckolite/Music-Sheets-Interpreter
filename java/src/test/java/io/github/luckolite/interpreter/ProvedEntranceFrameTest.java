// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original scoop fixtures under a compressed seed with an independent printed frame. */
public class ProvedEntranceFrameTest {
    boolean removed(CurvedEntranceStrokeTest.Page page, boolean proved) throws Exception {
        var find =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "findComponents", byte[].class, int.class, int.class, byte.class);
        find.setAccessible(true);
        var heads =
                (List<?>)
                        find.invoke(
                                null,
                                page.labels,
                                CurvedEntranceStrokeTest.W,
                                CurvedEntranceStrokeTest.H,
                                (byte) 2);
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Staff");
        var ctor = type.getDeclaredConstructor(float.class, float.class, float.class);
        ctor.setAccessible(true);
        var staff = ctor.newInstance(124f, 172f, 12f);
        var phase = type.getDeclaredField("printedPhase");
        phase.setAccessible(true);
        phase.setBoolean(staff, proved);
        var gap = type.getDeclaredField("pitchGap");
        gap.setAccessible(true);
        gap.setFloat(staff, 16f);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "entranceStrokeFragments",
                        byte[].class,
                        int.class,
                        int.class,
                        List.class,
                        List.class);
        method.setAccessible(true);
        return !((List<?>)
                        method.invoke(
                                null,
                                page.gray,
                                CurvedEntranceStrokeTest.W,
                                CurvedEntranceStrokeTest.H,
                                heads,
                                List.of(staff)))
                .isEmpty();
    }

    @Test
    public void provedFrameRemovesCurvedStroke() throws Exception {
        assertTrue(removed(new CurvedEntranceStrokeTest.Page(true, true), true));
    }

    @Test
    public void unprovedSpacingDoesNotExpandShapeGate() throws Exception {
        assertFalse(removed(new CurvedEntranceStrokeTest.Page(true, true), false));
    }

    @Test
    public void realStemmedGraceSurvives() throws Exception {
        var p = new CurvedEntranceStrokeTest.Page(true, true);
        p.head(306, 174, 4, 4, true);
        p.stem(310, 132, 174);
        assertFalse(removed(p, true));
    }

    @Test
    public void missingDestinationPreservesCandidate() throws Exception {
        assertFalse(removed(new CurvedEntranceStrokeTest.Page(false, true), true));
    }

    @Test
    public void rawOvalSurvives() throws Exception {
        var p = new CurvedEntranceStrokeTest.Page(true, false);
        p.head(306, 174, 4, 4, true);
        assertFalse(removed(p, true));
    }

    @Test
    public void broadBandSurvives() throws Exception {
        var p = new CurvedEntranceStrokeTest.Page(true, false);
        p.curve(5);
        assertFalse(removed(p, true));
    }

    @Test
    public void pixelsAndLabelsArePreserved() throws Exception {
        var p = new CurvedEntranceStrokeTest.Page(true, true);
        var g = p.gray.clone();
        var l = p.labels.clone();
        removed(p, true);
        assertArrayEquals(g, p.gray);
        assertArrayEquals(l, p.labels);
    }
}
