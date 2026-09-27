// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original held voice continuing past a later attack in another voice. */
public class HeldVoiceSystemTieTest {
    SystemBreakTieTest fixture(boolean outgoing, boolean incoming, boolean held) {
        var p = new SystemBreakTieTest();
        Arrays.fill(p.gray, (byte) 255);
        for (int top : new int[] {90, 300})
            for (int line = 0; line < 5; line++)
                for (int x = 40; x < 780; x++) p.ink(x, top + line * 16, 4);
        p.head(450, 186);
        p.head(650, 170);
        p.head(180, 396);
        if (held)
            for (int[] c : new int[][] {{450, 186}, {180, 396}}) {
                for (int y = c[1] - 45; y <= c[1] + 8; y++)
                    for (int x = c[0] - 14; x <= c[0] + 14; x++) {
                        p.gray[y * 800 + x] = (byte) 255;
                        p.labels[y * 800 + x] = 0;
                    }
                for (int y = c[1] - 7; y <= c[1] + 7; y++)
                    for (int x = c[0] - 11; x <= c[0] + 11; x++)
                        if (Math.pow((x - c[0]) / 11., 2) + Math.pow((y - c[1]) / 7., 2) <= 1) {
                            p.labels[y * 800 + x] = 2;
                            p.gray[y * 800 + x] =
                                    (byte)
                                            (Math.pow((x - c[0]) / 5., 2)
                                                                    + Math.pow((y - c[1]) / 4., 2)
                                                            < 1
                                                    ? 255
                                                    : 0);
                        }
                for (int y : new int[] {c[1] - 16, c[1]})
                    for (int x = c[0] - 16; x <= c[0] + 16; x++) p.gray[y * 800 + x] = 0;
                for (int y = c[1] - 45; y < c[1]; y++) p.ink(c[0] + 11, y, 1);
            }
        if (outgoing) p.arc(462, 755, 186, 1, false);
        if (incoming) p.arc(136, 168, 396, 1, false);
        return p;
    }

    boolean tied(boolean out, boolean in, boolean held) {
        var n = fixture(out, in, held).notes();
        assertEquals(3, n.size());
        return n.get(2).tiedFromPrevious();
    }

    @Test
    public void sustainedVoiceCanPassInterveningAttack() {
        assertTrue(tied(true, true, true));
    }

    @Test
    public void oneEndedCurveDoesNotJoin() {
        assertFalse(tied(true, false, true));
    }

    @Test
    public void ordinaryQuarterStillRequiresSystemEndOwnership() {
        assertFalse(tied(true, true, false));
    }
}
