// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic OCR tokens and ink; no source-score material. */
public class SubitoDynamicOcrTest {
    private final byte[] gray = new byte[400 * 200];

    public SubitoDynamicOcrTest() {
        Arrays.fill(gray, (byte) 255);
        for (int y = 100; y < 110; y++) for (int x = 100; x < 160; x++) gray[y * 400 + x] = 0;
    }

    private List<PlayingTechniqueDetector.Word> read(String line, String token) {
        return ScoreDynamicsDetector.ocrWords(
                line,
                new PlayingTechniqueDetector.Word(token, .25f, .5f, .4f, .55f),
                gray,
                400,
                200);
    }

    @Test
    public void abbreviatedSpacedInstructionKeepsLevel() {
        assertTrue(ScoreDynamicsDetector.dynamicLine("sub p"));
        assertEquals("p", read("sub p", "p").get(0).text());
    }

    @Test
    public void abbreviatedMergedInstructionKeepsLevel() {
        var words = read("subp", "subp");
        assertEquals(1, words.size());
        assertEquals(-8, ScoreDynamicsDetector.level(words.get(0).text()), 0);
        assertEquals(.25f, words.get(0).left(), 0);
        assertEquals(.4f, words.get(0).right(), 0);
    }

    @Test
    public void QualifiedWholeTokenAllowsPunctuationAndCase() {
        assertEquals(-12, ScoreDynamicsDetector.level("SUB. pp."), 0);
        assertEquals(3, ScoreDynamicsDetector.level("subito f"), 0);
        assertEquals(6, ScoreDynamicsDetector.level("sub.ff"), 0);
        assertEquals(-4, ScoreDynamicsDetector.level("sub mp"), 0);
    }

    @Test
    public void PrefixAloneIsNotALevel() {
        assertTrue(read("sub p", "sub").isEmpty());
        assertTrue(read("sub. pp", "sub.").isEmpty());
        assertTrue(Float.isNaN(ScoreDynamicsDetector.level("subito")));
    }

    @Test
    public void UnrelatedWordsAndLyricsDoNotAuthorizeLevels() {
        for (String text : List.of("submarine p", "subscription p", "sub p my love", "subtle p")) {
            assertFalse(ScoreDynamicsDetector.dynamicLine(text));
            assertTrue(read(text, "p").isEmpty());
            assertTrue(Float.isNaN(ScoreDynamicsDetector.level(text)));
        }
    }

    @Test
    public void BlankInkCannotCreateSuddenDynamic() {
        Arrays.fill(gray, (byte) 255);
        assertTrue(read("sub p", "p").isEmpty());
        assertTrue(read("subp", "subp").isEmpty());
    }

    @Test
    public void ExistingLevelsKeepTheirMeaning() {
        assertEquals(-8, ScoreDynamicsDetector.level(read("p", "p").get(0).text()), 0);
        assertEquals(9, ScoreDynamicsDetector.level(read("fff", "fff").get(0).text()), 0);
        assertEquals(0, ScoreDynamicsDetector.level(read("sempre mf", "mf").get(0).text()), 0);
    }
}
