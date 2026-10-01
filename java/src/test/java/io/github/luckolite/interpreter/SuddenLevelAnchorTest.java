// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original logical staff geometry and synthetic OCR tokens. */
public class SuddenLevelAnchorTest {
    private static final int W = 400, H = 240;

    private PlayingTechniqueDetector.Word word(String text, float x) {
        return new PlayingTechniqueDetector.Word(text, x, .56f, x + .03f, .62f);
    }

    private float anchor(String text, float x) {
        var note = new ScoreNoteEvent(0, .2f, 0, 0, 1, .4f, false, 1, 0, 2, 2);
        return anchor(text, x, List.of(note));
    }

    private float anchor(String text, float x, List<ScoreNoteEvent> notes) {
        var result =
                ScoreDynamicsDetector.detect(
                        List.of(word(text, x)),
                        List.of(new PlayingTechniqueDetector.Staff(80, 120, 10, 0, 1)),
                        List.of(new MeasureRegion(.1f, .9f, .3f, .52f)),
                        notes,
                        null,
                        W,
                        H);
        return result.get(0).positionInMeasure();
    }

    @Test
    public void qualifiedLevelAfterModifierWidthStartsAtAttachedAttack() {
        assertEquals(.2f, anchor("subp", .3225f), .00001f);
    }

    @Test
    public void spelledSuddenLevelStartsAtAttachedAttack() {
        assertEquals(.2f, anchor("subito p", .3225f), .00001f);
    }

    @Test
    public void ordinaryDuringNoteLevelRetainsPrintedPosition() {
        assertEquals((.3225f - .1f) / .8f, anchor("p", .3225f), .00001f);
    }

    @Test
    public void distantSuddenLevelRetainsPrintedPosition() {
        assertEquals((.55f - .1f) / .8f, anchor("subp", .55f), .00001f);
    }

    @Test
    public void spacedOcrRetainsTheSuddenInstruction() {
        var gray = new byte[W * H];
        Arrays.fill(gray, (byte) 255);
        for (int y = 135; y < 146; y++) for (int x = 129; x < 140; x++) gray[y * W + x] = 0;
        var result = ScoreDynamicsDetector.ocrWords("sub p", word("p", .3225f), gray, W, H);
        assertEquals(1, result.size());
        assertEquals("sub p", result.get(0).text());
        assertEquals(-8, ScoreDynamicsDetector.level(result.get(0).text()), 0);
    }

    @Test
    public void ambiguousNearbyAttacksKeepPrintedPosition() {
        var a = new ScoreNoteEvent(0, .2f, 0, 0, 1, .4f, false, 1, 0, 2, 1);
        var b = new ScoreNoteEvent(0, .35625f, 1, 0, 1, .4f, false, 1, 0, 2, 1);
        assertEquals((.3225f - .1f) / .8f, anchor("subp", .3225f, List.of(a, b)), .00001f);
    }

    @Test
    public void differentPartTopologyCannotSupplyAttachedAttack() {
        var a = new ScoreNoteEvent(0, .2f, 0, 0, 2, .4f, false, 1, 0, 2, 2);
        assertEquals((.3225f - .1f) / .8f, anchor("subp", .3225f, List.of(a)), .00001f);
    }
}
