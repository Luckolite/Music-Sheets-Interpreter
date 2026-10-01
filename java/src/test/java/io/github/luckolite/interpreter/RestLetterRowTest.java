// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original outline letters beside a displaced rest-shaped bulb, not score pixels. */
public class RestLetterRowTest {
    private final DeepPolyphonicRestTest fixture = new DeepPolyphonicRestTest();

    private void letter(int cx, int cy) {
        fixture.ellipse(cx, cy, 5, 8);
        for (int y = cy - 6; y <= cy + 6; y++)
            for (int x = cx - 3; x <= cx + 3; x++)
                if (Math.pow((x - cx) / 3.0, 2) + Math.pow((y - cy) / 6.0, 2) <= 1)
                    fixture.gray[y * DeepPolyphonicRestTest.W + x] = (byte) 255;
    }

    @Test
    public void alignedLetterRowCannotSupplyDisplacedRest() {
        fixture.page(true, false);
        for (int x : new int[] {112, 132, 152, 210, 230, 250}) letter(x, 160);
        assertTrue(fixture.rests(List.of(fixture.held(0, 0, 4))).isEmpty());
    }

    @Test
    public void isolatedDisplacedRestIsPreserved() {
        fixture.page(true, false);
        assertEquals(1, fixture.rests(List.of(fixture.held(0, 0, 4))).size());
    }

    @Test
    public void TwoDetachedMarksDoNotProveText() {
        fixture.page(true, false);
        letter(132, 160);
        letter(230, 160);
        assertEquals(1, fixture.rests(List.of(fixture.held(0, 0, 4))).size());
    }

    @Test
    public void distantTextRowDoesNotOwnRest() {
        fixture.page(true, false);
        for (int x : new int[] {112, 132, 152, 210, 230, 250}) letter(x, 210);
        assertEquals(1, fixture.rests(List.of(fixture.held(0, 0, 4))).size());
    }
}
