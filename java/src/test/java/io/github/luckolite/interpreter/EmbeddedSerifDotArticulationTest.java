// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original word silhouettes with a detached round upper serif. */
public class EmbeddedSerifDotArticulationTest {
    private int mark(boolean word, boolean aboveText, boolean notation, boolean accepted) {
        int w = 2048, h = 1600;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int y = 99; y <= 102; y++)
            for (int x = 300; x <= 303; x++) {
                if ((y == 99 || y == 102) && (x == 300 || x == 303)) continue;
                gray[y * w + x] = 0;
                labels[y * w + x] =
                        accepted
                                ? OmrMeasurePostProcessor.NOTEHEAD
                                : OmrMeasurePostProcessor.SYMBOL;
            }
        if (word)
            for (int left : new int[] {280, 291, 305, 316})
                for (int y = aboveText ? 113 : 98; y <= (aboveText ? 126 : 111); y++)
                    for (int x = left; x < left + 7; x++) {
                        if (x > left + 1
                                && x < left + 5
                                && y > (aboveText ? 114 : 99)
                                && y < (aboveText ? 125 : 110)) continue;
                        gray[y * w + x] = 0;
                        labels[y * w + x] =
                                notation
                                        ? OmrMeasurePostProcessor.NOTEHEAD
                                        : OmrMeasurePostProcessor.SYMBOL;
                    }
        List<NoteArticulationDetector.Anchor> notes = new ArrayList<>();
        notes.add(new NoteArticulationDetector.Anchor(301.5f, 65, 13, 0));
        if (accepted) notes.add(new NoteArticulationDetector.Anchor(301.5f, 100.5f, 13, 0));
        return NoteArticulationDetector.detect(labels, gray, w, h, notes)[0]
                & NoteArticulation.STACCATO;
    }

    @Test
    public void detachedUpperWordSerifIsNotStaccato() {
        assertEquals(0, mark(true, false, false, false));
    }

    @Test
    public void isolatedRoundDotStillIsStaccato() {
        assertEquals(NoteArticulation.STACCATO, mark(false, false, false, false));
    }

    @Test
    public void dotAboveLetterBodiesStillIsStaccato() {
        assertEquals(NoteArticulation.STACCATO, mark(true, true, false, false));
    }

    @Test
    public void notationMasksDoNotEstablishWord() {
        assertEquals(NoteArticulation.STACCATO, mark(true, false, true, false));
    }

    @Test
    public void acceptedHeadDoesNotBecomeStaccato() {
        assertEquals(0, mark(false, false, false, true));
    }

    @Test
    public void shortDisconnectedLetterPieceDoesNotBreakWordBaseline() {
        int w = 2048, h = 1600;
        byte[] gray = new byte[w * h], labels = new byte[w * h];
        Arrays.fill(gray, (byte) 255);
        for (int y = 99; y <= 102; y++)
            for (int x = 306; x <= 309; x++) {
                if ((y == 99 || y == 102) && (x == 306 || x == 309)) continue;
                gray[y * w + x] = 0;
            }
        for (int left : new int[] {280, 296, 312, 328})
            for (int y = 98; y <= 111; y++)
                for (int x = left; x < left + 7; x++) {
                    if (x > left + 1 && x < left + 5 && y > 99 && y < 110) continue;
                    gray[y * w + x] = 0;
                }
        for (int y = 94; y <= 102; y++) for (int x = 288; x <= 292; x++) gray[y * w + x] = 0;
        assertEquals(
                0,
                NoteArticulationDetector.detect(
                                labels,
                                gray,
                                w,
                                h,
                                List.of(new NoteArticulationDetector.Anchor(307.5f, 65, 17, 0)))[0]
                        & NoteArticulation.STACCATO);
    }
}
