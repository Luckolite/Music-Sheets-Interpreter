// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original repeated wave and deliberately separated strokes. */
public final class RestVerticalWaveTest {
    private final byte[] gray = new byte[200 * 200];

    private void draw(int end, boolean separated, boolean straight) {
        Arrays.fill(gray, (byte) 255);
        for (int y = 50; y <= end; y++)
            if (!separated || y < 90 || y > 101) {
                int x =
                        straight
                                ? 90
                                : 90 + Math.round(5 * (float) Math.sin((y - 50) * Math.PI / 10));
                for (int dx = -2; dx <= 2; dx++) gray[y * 200 + x + dx] = 0;
            }
    }

    private boolean match() {
        return RestVerticalWave.crosses(gray, 200, 200, 80, 100, 70, 110, 16);
    }

    @Test
    public void tallWaveCannotBecomeACroppedRest() {
        draw(140, false, false);
        assertTrue(match());
    }

    @Test
    public void straightStemIsNotAWave() {
        draw(140, false, true);
        assertFalse(match());
    }

    @Test
    public void shortContourDoesNotProveArpeggio() {
        draw(98, false, false);
        assertFalse(match());
    }

    @Test
    public void separateMarksCannotBeJoinedIntoAWave() {
        draw(140, true, false);
        assertFalse(match());
    }

    @Test
    public void pixelsRemainUnchanged() {
        draw(140, false, false);
        var copy = gray.clone();
        match();
        assertArrayEquals(copy, gray);
    }
}
