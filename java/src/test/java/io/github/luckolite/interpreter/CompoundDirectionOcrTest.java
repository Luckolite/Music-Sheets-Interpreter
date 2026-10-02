// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class CompoundDirectionOcrTest {
    private static OcrText phrase(String content) {
        var line =
                new OcrText.Line(
                        content,
                        new OcrText.Box(20, 10, 140, 30),
                        List.of(
                                new OcrText.Element(
                                        "mf", new OcrText.Box(20, 10, 50, 30), List.of()),
                                new OcrText.Element(
                                        "cantabile", new OcrText.Box(70, 10, 140, 30), List.of())));
        return new OcrText(content, List.of(new OcrText.Block(content, line.box(), List.of(line))));
    }

    @Test
    public void compoundExpressionKeepsTheFirstPrintedAnchorWithoutADuplicate() {
        var words = new ArrayList<PlayingTechniqueDetector.Word>();
        MusicalOcrEvidence.addDirectionWords(phrase("mf cantabile"), words, 2, 100, 1000, 1000);
        assertEquals(
                List.of(
                        new PlayingTechniqueDetector.Word(
                                "mf cantabile", .01f, .105f, .07f, .115f)),
                words);
    }

    @Test
    public void negatedCompoundDirectionIsStillExcluded() {
        var words = new ArrayList<PlayingTechniqueDetector.Word>();
        MusicalOcrEvidence.addDirectionWords(phrase("non mf cantabile"), words, 2, 100, 1000, 1000);
        assertTrue(words.isEmpty());
    }
}
