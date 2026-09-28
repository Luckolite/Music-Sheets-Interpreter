// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original rendered strokes exercising the shared photometric fallback. */
public class CompactMordentRecoveryTest {
    private static final int W = 44, H = 24;

    private byte[] stroke(double pen, int dark, int paper, double cycles) {
        byte[] g = new byte[W * H];
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                double center = 11.5 - 6 * Math.sin(cycles * 2 * Math.PI * x / (W - 1));
                double ink = Math.max(0, Math.min(1, pen + .5 - Math.abs(y - center)));
                g[y * W + x] = (byte) Math.round(paper - (paper - dark) * ink);
            }
        return g;
    }

    private PortableOrnamentGlyphs matcher() {
        var m = new PortableOrnamentGlyphs();
        m.add(stroke(2, 0, 255, 2), W, H, NoteOrnament.MORDENT, false);
        return m;
    }

    private PortableOrnamentGlyphs.Match match(PortableOrnamentGlyphs m, byte[] g) {
        return m.match(g, W, new PortableNoteOrnaments.Bounds(0, 0, W, H));
    }

    @Test
    public void compactFadedWaveRecoversAtExistingConfidence() {
        var result = match(matcher(), stroke(7, 65, 240, 2));
        assertTrue(result.toString(), result.accepted());
        assertEquals(NoteOrnament.MORDENT, result.kind());
    }

    @Test
    public void acceptedOriginalDecisionHasPriority() {
        byte[] g = stroke(7, 65, 240, 2);
        var m = matcher();
        m.add(g, W, H, NoteOrnament.TURN, false);
        var result = match(m, g);
        assertTrue(result.accepted());
        assertEquals(NoteOrnament.TURN, result.kind());
    }

    @Test
    public void lowContrastDoesNotAuthorizeFallback() {
        assertFalse(match(matcher(), stroke(7, 170, 215, 2)).accepted());
    }

    @Test
    public void emptyTemplateSetStillAbstains() {
        assertFalse(match(new PortableOrnamentGlyphs(), stroke(7, 65, 240, 2)).accepted());
    }

    @Test
    public void blankInputAbstains() {
        byte[] g = new byte[W * H];
        Arrays.fill(g, (byte) 230);
        assertFalse(match(matcher(), g).accepted());
    }

    @Test
    public void oneCycleDoesNotBecomeMordent() {
        assertFalse(match(matcher(), stroke(7, 65, 240, 1)).accepted());
    }

    @Test
    public void sourceRemainsUnchanged() {
        byte[] g = stroke(7, 65, 240, 2), before = g.clone();
        match(matcher(), g);
        assertArrayEquals(before, g);
    }
}
