// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class PublicApiTest {
    @Test public void blankPageIsAnEmptyScore() {
        byte[] labels=new byte[640*480],gray=new byte[labels.length];Arrays.fill(gray,(byte)255);
        var score=SheetInterpreter.analyze(labels,gray,640,480);
        assertTrue(score.notes().isEmpty());assertTrue(score.measures().isEmpty());
    }
    @Test(expected=IllegalArgumentException.class) public void invalidMaskShapeIsRejected() {
        SheetInterpreter.analyze(new byte[5],new byte[6],2,3);
    }
    @Test(expected=IllegalArgumentException.class) public void invalidLabelIsRejected() {
        SheetInterpreter.analyze(new byte[]{6},new byte[]{0},1,1);
    }
    @Test(expected=IllegalArgumentException.class) public void overflowingDimensionsAreRejected() {
        SheetInterpreter.analyze(new byte[0],new byte[0],Integer.MAX_VALUE,Integer.MAX_VALUE);
    }
    @Test(expected=IllegalArgumentException.class) public void invalidOcrDirectionAnchorIsRejected() {
        new SheetInterpreter.NumberToken(120,.1f,.1f,.2f,.2f,Float.NaN);
    }
    @Test public void allJsonControlCharactersAreEscaped() throws Exception {
        assertEquals("\"a\\u0000\\u0008\\u000a\\\"\\\\\"",Main.json("a\u0000\b\n\"\\"));
    }
}
