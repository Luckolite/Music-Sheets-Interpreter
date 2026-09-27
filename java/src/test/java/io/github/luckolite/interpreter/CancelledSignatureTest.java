// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original cancellation glyphs at a double bar, without source-score fixtures. */
public class CancelledSignatureTest {
    private void natural(JoinedSignatureSharpTest f, int x, int y) {
        for (int yy = y - 22; yy <= y + 9; yy++)
            for (int xx = x - 5; xx <= x - 4; xx++) f.ink(xx, yy, 3);
        for (int yy = y - 9; yy <= y + 22; yy++)
            for (int xx = x + 4; xx <= x + 5; xx++) f.ink(xx, yy, 3);
        for (int cy : new int[] {y - 8, y + 8})
            for (int yy = cy - 1; yy <= cy + 1; yy++)
                for (int xx = x - 4; xx <= x + 4; xx++) f.ink(xx, yy, 3);
    }

    private void flat(JoinedSignatureSharpTest f, int x, int y) {
        for (int yy = y - 28; yy <= y + 7; yy++)
            for (int xx = x; xx <= x + 2; xx++) f.ink(xx, yy, 3);
        for (int yy = y - 7; yy <= y + 7; yy++)
            for (int xx = x + 2; xx <= x + 12; xx++) {
                double r = Math.pow((xx - x - 3) / 9d, 2) + Math.pow((yy - y) / 7d, 2);
                if (r <= 1 && r >= .35) f.ink(xx, yy, 3);
            }
    }

    private JoinedSignatureSharpTest page(boolean bar) {
        var f = new JoinedSignatureSharpTest();
        f.row(100, 0, 0, false);
        for (int y = 70; y <= 180; y++)
            for (int x = 30; x <= 55; x++) {
                f.labels[y * JoinedSignatureSharpTest.W + x] = 0;
                f.gray[y * JoinedSignatureSharpTest.W + x] = (byte) 255;
            }
        if (bar)
            for (int x : new int[] {177, 178, 184, 185})
                for (int y = 100; y <= 164; y++) f.ink(x, y, 1);
        return f;
    }

    @Test
    public void cancelledSharpsFollowedByOneFlatUseTheFlatKey() {
        var f = page(true);
        natural(f, 204, 100);
        natural(f, 225, 124);
        flat(f, 243, 132);
        assertEquals(List.of(-1), f.keys());
    }

    @Test
    public void cancelledFlatFollowedBySharpsUsesTheSharpKey() {
        var f = page(true);
        natural(f, 204, 132);
        f.sharp(225, 100);
        f.sharp(246, 124);
        assertEquals(List.of(2), f.keys());
    }

    @Test
    public void mixedLocalChordAccidentalsDoNotChangeKeyWithoutBar() {
        var f = page(false);
        natural(f, 204, 132);
        f.sharp(225, 100);
        f.sharp(246, 124);
        assertEquals(List.of(), f.keys());
    }

    @Test
    public void invalidNewSignatureOrderIsRejected() {
        var f = page(true);
        natural(f, 204, 132);
        f.sharp(225, 100);
        f.sharp(246, 108);
        assertEquals(List.of(), f.keys());
    }

    @Test
    public void cancellationDoesNotModifySourceMasks() {
        var f = page(true);
        natural(f, 204, 132);
        f.sharp(225, 100);
        f.sharp(246, 124);
        var labels = f.labels.clone();
        var gray = f.gray.clone();
        f.keys();
        assertArrayEquals(labels, f.labels);
        assertArrayEquals(gray, f.gray);
    }
}
