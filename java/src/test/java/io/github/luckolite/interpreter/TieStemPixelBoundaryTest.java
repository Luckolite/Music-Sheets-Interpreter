// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original integer raster shafts at the rounded staff-relative length boundary. */
public class TieStemPixelBoundaryTest {
    private int read(int length, float gap, int direction) throws Exception {
        int w = 100, h = 160, cx = 50, cy = 80;
        byte[] gray = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int y = 74; y <= 86; y++) for (int x = 44; x <= 56; x++) gray[y * w + x] = 0;
        int edge = direction < 0 ? 56 : 44;
        for (int d = 0; d <= length; d++) gray[(cy + direction * d) * w + edge] = 0;
        var type = Class.forName(OmrScoreInterpreter.class.getName() + "$Component");
        var ctor = type.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object head = ctor.newInstance(169, 44, 56, 74, 86, (float) cx, (float) cy);
        var method =
                OmrScoreInterpreter.class.getDeclaredMethod(
                        "tieVoiceStemDirections",
                        byte[].class,
                        int.class,
                        int.class,
                        type,
                        float.class);
        method.setAccessible(true);
        byte[] before = gray.clone();
        int result = (int) method.invoke(null, gray, w, h, head, gap);
        assertArrayEquals(before, gray);
        return result;
    }

    @Test
    public void roundedUpwardShaftProvesVoice() throws Exception {
        assertEquals(1, read(32, 14, -1));
    }

    @Test
    public void roundedDownwardShaftProvesVoice() throws Exception {
        assertEquals(2, read(32, 14, 1));
    }

    @Test
    public void shorterShaftCannotProveVoice() throws Exception {
        assertEquals(0, read(31, 14, -1));
    }

    @Test
    public void fractionalSpacingKeepsItsRoundedMinimum() throws Exception {
        assertEquals(1, read(33, 14.25f, -1));
    }

    @Test
    public void pixelBelowFractionalMinimumIsRejected() throws Exception {
        assertEquals(0, read(32, 14.25f, -1));
    }
}
