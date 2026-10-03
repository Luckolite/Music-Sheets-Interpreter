// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class MusicalOcrExpressionEvidenceTest {
    private static List<PlayingTechniqueDetector.Word> words(String phrase) {
        var box = new OcrText.Box(100, 20, 220, 40);
        var element = new OcrText.Element(phrase, box, List.of());
        var line = new OcrText.Line(phrase, box, List.of(element));
        var text = new OcrText(phrase, List.of(new OcrText.Block(phrase, box, List.of(line))));
        var words = new ArrayList<PlayingTechniqueDetector.Word>();
        MusicalOcrEvidence.addDirectionWords(text, words, 1, 0, 500, 300);
        return words;
    }

    @Test
    public void ocrAdapterKeepsCompoundSlowingAndQualifiedStrength() {
        var result = words("poco rit.");
        assertEquals(1, result.size());
        assertEquals("poco rit.", result.get(0).text());
        assertEquals(
                ScoreExpressiveEvent.Strength.POCO,
                ExpressiveDirectionText.parse(result.get(0).text()).get(0).strength());
        assertEquals(1, words("crescendo e rall.").size());
    }

    @Test
    public void ocrAdapterKeepsMetricRelationshipAndAttackButRejectsOrdinaryText() {
        assertEquals(1, words("♩. = ♩").size());
        assertEquals(1, words("sfz").size());
        assertTrue(words("write a story").isEmpty());
    }
}
