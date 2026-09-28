// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** The proved printed scale may supersede a compressed segmentation seed. */
public class ProvedBeamScaleTest extends LocalBeamStaffFrameTest {
    private Object proved() throws Exception {
        Object s = staff();
        var phase = ST.getDeclaredField("printedPhase");
        phase.setAccessible(true);
        phase.setBoolean(s, true);
        var gap = ST.getDeclaredField("pitchGap");
        gap.setAccessible(true);
        gap.setFloat(s, 22);
        var bottom = ST.getDeclaredField("pitchBottom");
        bottom.setAccessible(true);
        bottom.setFloat(s, 160);
        var slope = ST.getDeclaredField("pitchSlope");
        slope.setAccessible(true);
        slope.setFloat(s, .02f);
        return s;
    }

    @Test
    public void corroboratedPrintedScaleSupersedesCompressedSeed() throws Exception {
        Object original = proved(), actual = frame(original, 162, 22);
        assertNotSame(original, actual);
        var gap = ST.getDeclaredField("gap");
        gap.setAccessible(true);
        assertEquals(22, gap.getFloat(actual), 0);
        assertEquals(16, gap.getFloat(original), 0);
    }

    @Test
    public void provedSlopeIsPreservedInTrack() throws Exception {
        Object actual = frame(proved(), 162, 22);
        var field = ST.getDeclaredField("pitchTrack");
        field.setAccessible(true);
        var track = (StaffPitchTrack) field.get(actual);
        assertNotNull(track);
        assertEquals(160, track.at(W * .5f)[0], .001f);
        assertEquals(.02f * 100, track.at(W * .5f + 100)[0] - track.at(W * .5f)[0], .01f);
    }

    @Test
    public void inconsistentLocalGapDoesNotReplaceSeed() throws Exception {
        Object s = proved();
        assertSame(s, frame(s, 160, 25));
    }

    @Test
    public void invalidLocalBottomDoesNotReplaceSeed() throws Exception {
        Object s = proved();
        assertSame(s, frame(s, Float.NaN, 22));
    }
}
