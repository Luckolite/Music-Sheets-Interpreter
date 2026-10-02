// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class PaperDynamicWordTest {
    private static final PlayingTechniqueDetector.Word BOX =
            new PlayingTechniqueDetector.Word("", .4f, .5f, .46f, .6f);
    private static final PaperDynamicWord.Crop CROP = PaperDynamicWord.crop(BOX, 1000, 1000);

    private static OcrText text(String s, int scale) {
        return text(s, scale, 395, 495, 465, 605);
    }

    private static OcrText text(String s, int scale, int l, int t, int r, int b) {
        var box =
                new OcrText.Box(
                        (l - CROP.left()) * scale,
                        (t - CROP.top()) * scale,
                        (r - CROP.left()) * scale,
                        (b - CROP.top()) * scale);
        var element = new OcrText.Element(s, box, List.of());
        var line = new OcrText.Line(s, box, List.of(element));
        return new OcrText(s, List.of(new OcrText.Block(s, box, List.of(line))));
    }

    private static PlayingTechniqueDetector.Word agree(OcrText a, OcrText b) {
        return PaperDynamicWord.agree(BOX, a, b, CROP, 1000, 1000, List.of());
    }

    @Test
    public void completeMezzoForteAgrees() {
        assertEquals("mf", agree(text("mf", 2), text("mf", 3)).text());
    }

    @Test
    public void completeForteAgrees() {
        assertEquals("f", agree(text("f", 2), text("f", 3)).text());
    }

    @Test
    public void allEightLevelsAgree() {
        for (String s : List.of("ppp", "pp", "p", "mp", "mf", "fff", "ff", "f"))
            assertEquals(s, agree(text(s, 2), text(s, 3)).text());
    }

    @Test
    public void differingLevelsReject() {
        assertNull(agree(text("f", 2), text("ff", 3)));
    }

    @Test
    public void ordinaryWordRejects() {
        assertNull(agree(text("pizz", 2), text("pizz", 3)));
    }

    @Test
    public void incompleteMRejects() {
        assertNull(agree(text("m", 2), text("m", 3)));
    }

    @Test
    public void multipleTokensReject() {
        assertNull(agree(text("mf f", 2), text("mf f", 3)));
    }

    @Test
    public void suffixFragmentRejects() {
        assertNull(agree(text("f", 2, 430, 495, 465, 605), text("f", 3, 430, 495, 465, 605)));
    }

    @Test
    public void oversizeCompoundRejects() {
        assertNull(agree(text("mf", 2, 375, 495, 490, 605), text("mf", 3, 375, 495, 490, 605)));
    }

    @Test
    public void verticalFragmentRejects() {
        assertNull(agree(text("f", 2, 395, 540, 465, 605), text("f", 3, 395, 540, 465, 605)));
    }

    @Test
    public void existingCompoundPreserved() {
        var old = new PlayingTechniqueDetector.Word("mf", .39f, .49f, .47f, .61f);
        assertNull(
                PaperDynamicWord.agree(
                        BOX, text("f", 2), text("f", 3), CROP, 1000, 1000, List.of(old)));
        assertEquals("mf", old.text());
    }

    @Test
    public void unrelatedClaimDoesNotBlock() {
        assertTrue(
                PaperDynamicWord.unclaimed(
                        BOX, List.of(new PlayingTechniqueDetector.Word("p", .1f, .2f, .2f, .3f))));
    }

    @Test
    public void boundingBoxOutsideCropRejects() {
        assertNull(agree(text("f", 2, 365, 495, 465, 605), text("f", 3)));
    }

    @Test
    public void missingElementBoxRejects() {
        var e = new OcrText.Element("f", null, List.of());
        var l = new OcrText.Line("f", null, List.of(e));
        assertNull(
                agree(
                        new OcrText("f", List.of(new OcrText.Block("f", null, List.of(l)))),
                        text("f", 3)));
    }

    @Test
    public void duplicateElementsReject() {
        var a = text("f", 2);
        var e = a.blocks().get(0).lines().get(0).elements().get(0);
        var l = new OcrText.Line("f", e.box(), List.of(e, e));
        assertNull(
                agree(
                        new OcrText("f", List.of(new OcrText.Block("f", e.box(), List.of(l)))),
                        text("f", 3)));
    }

    @Test
    public void nonFiniteProposalRejects() {
        assertNull(
                PaperDynamicWord.crop(
                        new PlayingTechniqueDetector.Word("", Float.NaN, .5f, .46f, .6f),
                        1000,
                        1000));
    }

    @Test
    public void boundsAndInvalidDimensionsReject() {
        assertNull(PaperDynamicWord.crop(BOX, 0, 1000));
        assertNull(
                PaperDynamicWord.crop(
                        new PlayingTechniqueDetector.Word("", -.1f, .5f, .46f, .6f), 1000, 1000));
    }

    @Test
    public void capitalizationDoesNotChangeLevel() {
        assertEquals("mf", agree(text("MF", 2), text("mf", 3)).text());
    }
}
