// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original procedural octave chord accidentals, not a score crop. */
public final class StackedChordKeyGuardTest {
    private JoinedSignatureSharpTest page(boolean stacked) {
        var p = new JoinedSignatureSharpTest();
        int top = 160;
        for (int y = top; y <= top + 64; y += 16) for (int x = 20; x < 620; x++) p.ink(x, y, 4);
        for (int y = top; y <= top + 64; y++) p.ink(200, y, 1);
        p.sharp(220, stacked ? 216 : 160);
        p.sharp(238, 184);
        if (stacked) p.sharp(238, 240);
        int hx = stacked ? 264 : 350;
        for (int cy : stacked ? new int[] {184, 216, 240} : new int[] {192}) {
            for (int y = cy - 5; y <= cy + 5; y++)
                for (int x = hx - 8; x <= hx + 8; x++)
                    if ((x - hx) * (x - hx) / 64d + (y - cy) * (y - cy) / 25d <= 1) p.ink(x, y, 2);
        }
        for (int y = 140; y <= 240; y++) p.ink(hx + 8, y, 1);
        p.measures.add(new MeasureRegion(200f / 640, 620f / 640, 130f / 720, 272f / 720));
        return p;
    }

    @Test
    public void octaveChordSharpsCannotInventTwoSharpKey() {
        var p = page(true);
        assertEquals(List.of(), p.keys());
    }

    @Test
    public void realOrderedTwoSharpSignatureSurvives() {
        var p = page(false);
        assertEquals(List.of(2), p.keys());
    }

    @Test
    public void detectionPreservesInputPixels() {
        var p = page(true);
        var labels = p.labels.clone();
        var gray = p.gray.clone();
        p.keys();
        assertArrayEquals(labels, p.labels);
        assertArrayEquals(gray, p.gray);
    }
}
