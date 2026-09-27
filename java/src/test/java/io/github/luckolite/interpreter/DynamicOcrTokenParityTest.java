// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class DynamicOcrTokenParityTest {
    private final byte[] gray = new byte[400 * 200];

    public DynamicOcrTokenParityTest() {
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
    public void joinedLevelAndDirectionSurvive() {
        assertEquals(
                List.of("p", "dim"),
                read("pdim.", "pdim.").stream().map(PlayingTechniqueDetector.Word::text).toList());
    }

    @Test
    public void packedDynamicsWithPunctuationSurvive() {
        assertEquals(
                List.of("mf", "mp"),
                read("mfmp.", "mfmp.").stream().map(PlayingTechniqueDetector.Word::text).toList());
    }

    @Test
    public void splitBoundsStayInsideOriginalWord() {
        var words = read("mfcresc.", "mfcresc.");
        assertEquals(.25f, words.get(0).left(), 0);
        assertEquals(words.get(0).right(), words.get(1).left(), 0);
        assertEquals(.4f, words.get(1).right(), 0);
    }

    @Test
    public void noInkCannotCreateDynamic() {
        Arrays.fill(gray, (byte) 255);
        assertTrue(read("pdim.", "pdim.").isEmpty());
    }

    @Test
    public void ordinaryLyricDoesNotAuthorizeLetter() {
        assertTrue(read("a poem for me", "p").isEmpty());
    }

    @Test
    public void directionInMixedLineStillSurvives() {
        assertEquals("cresc.", read("cresc. rall. poco a poco", "cresc.").get(0).text());
    }

    @Test
    public void softLevelSurvivesDolceInstruction() {
        assertEquals("p", read("p dolce", "p").get(0).text());
    }

    @Test
    public void quietLevelSurvivesTranquilloInstruction() {
        assertEquals("pp", read("pp tranquillo", "pp").get(0).text());
    }

    @Test
    public void levelSurvivesExpressiveInstructionAndPunctuation() {
        assertEquals("mf", read("sempre mf espressivo.", "mf").get(0).text());
    }

    @Test
    public void styleWordsDoNotBecomeLevels() {
        for (String style : List.of("dolce", "tranquillo", "espressivo"))
            assertTrue(read("p " + style, style).isEmpty());
    }

    @Test
    public void styleWordInLyricDoesNotAuthorizeLevel() {
        assertTrue(read("p dolce my love", "p").isEmpty());
        assertTrue(read("p dolcemente", "p").isEmpty());
    }

    @Test
    public void styleDoesNotBypassInkRequirement() {
        Arrays.fill(gray, (byte) 255);
        assertTrue(read("pp tranquillo", "pp").isEmpty());
    }

    @Test
    public void sustainedTripleForteMustNotSplit() {
        assertEquals(1, read("fff", "fff").size());
        assertEquals("fff", read("fff", "fff").get(0).text());
    }
}
