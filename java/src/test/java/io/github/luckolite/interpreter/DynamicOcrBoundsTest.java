// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original small ink components and OCR padding; no fonts or private score images. */
public class DynamicOcrBoundsTest {
    private final int w=100,h=120;
    private byte[] raster(int paper) {byte[] g=new byte[w*h];Arrays.fill(g,(byte)paper);return g;}
    private void rect(byte[] g,int l,int t,int r,int b,int ink) {
        for(int y=t;y<b;y++)for(int x=l;x<r;x++)g[y*w+x]=(byte)ink;
    }
    private PlayingTechniqueDetector.Word word(String text){return new PlayingTechniqueDetector.Word(text,.2f,30f/h,.6f,60f/h);}
    private List<PlayingTechniqueDetector.Word> clean(byte[] g,String text) {
        return DynamicOcrBounds.clean(List.of(word(text)),g,w,h,List.of(new PlayingTechniqueDetector.Staff(70,110,10,0,1)));
    }
    @Test public void literalPaddingIsTrimmedToPrintedInk() {
        var g=raster(255);rect(g,28,37,47,54,0);var result=clean(g,"mp");
        assertEquals(List.of(new PlayingTechniqueDetector.Word("mp",.28f,37f/h,.47f,54f/h)),result);
    }
    @Test public void grayPaperIsNotPartOfTheGlyphBox() {
        var g=raster(138);rect(g,28,37,47,54,20);var result=clean(g,"ff");
        assertEquals(.28f,result.get(0).left(),0);assertEquals(37f/h,result.get(0).top(),0);
    }
    @Test public void blankOcrGuessIsRemoved(){assertTrue(clean(raster(255),"p").isEmpty());}
    @Test public void isolatedSpeckCannotSupportDynamic(){var g=raster(255);g[40*w+35]=0;assertTrue(clean(g,"f").isEmpty());}
    @Test public void downwardStemContinuingOutsideOcrCropIsRejected() {
        var g=raster(255);rect(g,28,37,47,54,0);rect(g,30,40,32,90,0);
        assertTrue(clean(g,"p").isEmpty());
    }
    @Test public void upwardStemContinuingOutsideOcrCropIsRejected() {
        var g=raster(255);rect(g,28,37,47,54,0);rect(g,44,5,46,50,0);
        assertTrue(clean(g,"p").isEmpty());
    }
    @Test public void shortDescenderDoesNotImplyNoteStem() {
        var g=raster(255);rect(g,28,37,47,54,0);rect(g,30,50,32,65,0);
        assertFalse(clean(g,"p").isEmpty());
    }
    @Test public void directionWordsKeepTheirOriginalGeometry() {
        assertEquals(List.of(word("cresc")),clean(raster(255),"cresc"));
    }
    @Test public void callerRasterIsUnchanged() {
        var g=raster(160);rect(g,28,37,47,54,0);var before=g.clone();clean(g,"mf");assertArrayEquals(before,g);
    }
}
