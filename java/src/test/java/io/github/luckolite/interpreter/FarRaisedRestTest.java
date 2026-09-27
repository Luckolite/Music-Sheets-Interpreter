// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original rest translated above a separate sustained voice. */
public class FarRaisedRestTest {
    private List<ScoreRestEvent> read(boolean sustained, int staff, int measure) {
        byte[] gray = new byte[600 * 280];
        java.util.Arrays.fill(gray, (byte) 245);
        for (int y = 120; y <= 184; y += 16) for (int x = 20; x < 580; x++) gray[y * 600 + x] = 0;
        for (int y = 56; y <= 66; y++)
            for (int x = 150; x <= 162; x++)
                if (Math.pow((x - 156) / 6.0, 2) + Math.pow((y - 61) / 5.0, 2) <= 1)
                    gray[y * 600 + x] = 0;
        for (int y = 58; y <= 88; y++) {
            int x = 165 - (y - 58) / 3;
            gray[y * 600 + x] = 0;
            gray[y * 600 + x + 1] = 0;
        }
        for (int y = 101; y <= 109; y++)
            for (int x = 153; x <= 167; x++)
                if (Math.pow((x - 160) / 7.0, 2) + Math.pow((y - 105) / 4.0, 2) <= 1)
                    gray[y * 600 + x] = 0;
        var held =
                new ScoreNoteEvent(
                        measure,
                        160 / 600f,
                        0,
                        staff,
                        1,
                        105 / 280f,
                        false,
                        0,
                        0,
                        2,
                        sustained ? 2 : 1);
        return SixteenthRestDetector.detect(
                gray,
                600,
                280,
                List.of(new MeasureRegion(0, 1, 0, 1)),
                List.of(new SixteenthRestDetector.Staff(120, 184, 16, 0, 1)),
                List.of(held));
    }

    @Test
    public void sustainedVoiceAllowsCompleteRaisedRest() {
        assertTrue(
                read(true, 0, 0).stream()
                        .anyMatch(r -> r.durationBeats() == .5 && r.pageY() < .35f));
    }

    @Test
    public void quarterAttackDoesNotProveIndependentSustain() {
        assertTrue(read(false, 0, 0).isEmpty());
    }

    @Test
    public void otherStaffCannotAuthorizeRaisedRest() {
        assertTrue(read(true, 1, 0).isEmpty());
    }

    @Test
    public void otherMeasureCannotAuthorizeRaisedRest() {
        assertTrue(read(true, 0, 1).isEmpty());
    }
}
