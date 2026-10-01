// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import java.util.zip.GZIPInputStream;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original Bravura OFL U+E4AC render at 68 pixels, cropped and resized to 23x17 with ink level 8.
 * No private score ink. The glyph meets an independently generated staff rule. */
public class FontStrokeCaretArticulationTest {
    private int mark(boolean flipped, boolean crossbar) throws Exception {
        byte[] pixels;
        try (var in =
                new GZIPInputStream(
                        new ByteArrayInputStream(
                                Base64.getDecoder()
                                        .decode(
                                                "H4sIAAAAAAAC//v/HwY+XXr46T8G+LhBxmb2dwzhXTEcnBEHf6MK/ntWJcHBoZT2GlX851Q7Dg4OTt1FT5FFv1x1FwYKcwiZ7fqBJHy1WAEkysHJXXIKIfp+qTg3BwQotf38BxX9uyZQbGKOPES967KvENHfd1M1s++ereDmBIkLO90Au/7f+1rzgCtffx4yEgGrl624DDZ4v4njZCD9fLExWDmP/Py3QMXHI5Ub7oPM+pipCLE2dMP//w96ZcJ3Qyw/EANWziGede//XGflTW8hdn+bYwxxp0n5rxLL8scwl96ZrMALttXu3/MHH/7AvPXn84O7d4Dg3mMAGHGm94cBAAA=")))) {
            pixels = in.readAllBytes();
        }
        int w = 2048, h = 1600;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int row : new int[] {100, 117, 134, 151, 168})
            for (int x = 230; x <= 370; x++)
                for (int dy = -1; dy <= 1; dy++) gray[(row + dy) * w + x] = 0;
        for (int y = 0; y < 17; y++)
            for (int x = 0; x < 23; x++) {
                int yy = 83 + (flipped ? 16 - y : y), at = yy * w + 289 + x;
                gray[at] = (byte) Math.min(gray[at] & 255, pixels[y * 23 + x] & 255);
            }
        if (crossbar)
            for (int x = 294; x <= 307; x++)
                for (int dy = -1; dy <= 1; dy++) gray[(94 + dy) * w + x] = 0;
        return NoteArticulationDetector.detect(
                        labels,
                        gray,
                        w,
                        h,
                        List.of(new NoteArticulationDetector.Anchor(300, 127, 17, 0)))[0]
                & NoteArticulation.MARCATO;
    }

    @Test
    public void fontStrokeJoinedToRuleIsMarcato() throws Exception {
        assertEquals(NoteArticulation.MARCATO, mark(false, false));
    }

    @Test
    public void fontStrokeUpBowIsNotMarcato() throws Exception {
        assertEquals(0, mark(true, false));
    }

    @Test
    public void fontStrokeCrossbarIsNotMarcato() throws Exception {
        assertEquals(0, mark(false, true));
    }
}
