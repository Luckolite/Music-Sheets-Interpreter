// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic dark-paper count, never a scan-derived glyph. */
public class ShadowedRestCountTest {
    private static final int W=240,H=160;
    private static final MeasureRegion REGION=new MeasureRegion(.15f,.85f,.50f,.90f);
    private byte[] sample(boolean bar,int paper) {
        byte[] g=new byte[W*H];Arrays.fill(g,(byte)paper);
        // A pale rectangular scan halo would fill both bowls at the old cutoff.
        rect(g,109,47,22,28,158);
        for(int y:new int[]{47,59,73})rect(g,110,y,20,3,30);
        rect(g,126,49,4,24,30);
        if(bar)rect(g,73,101,95,6,30);
        return g;
    }
    private void rect(byte[] g,int x,int y,int w,int h,int value) {
        for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++)g[yy*W+xx]=(byte)value;
    }
    private MeasureNumberReconciler.NumberToken read(byte[] g) {
        try {
            var method=MultiMeasureRestDetector.class.getDeclaredMethod("standaloneCount",byte[].class,int.class,int.class,
                    MultiMeasureRestDetector.RestBarCandidate.class,boolean.class);
            method.setAccessible(true);
            return (MeasureNumberReconciler.NumberToken)method.invoke(null,g,W,H,
                    new MultiMeasureRestDetector.RestBarCandidate(0,REGION),true);
        } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
    }
    @Test public void shadowHaloDoesNotFillThreeBowls() {
        var token=read(sample(true,175));assertNotNull(token);assertEquals(3,token.value());
        assertEquals(110f/W,token.left(),.001f);assertEquals(47f/H,token.top(),.001f);
    }
    @Test public void normalRestDetectionReceivesVerifiedCount() {
        var labels=new byte[W*H];
        for(int y=80;y<=136;y+=14)for(int x=36;x<=204;x++)labels[y*W+x]=OmrMeasurePostProcessor.STAFF;
        var counts=MultiMeasureRestDetector.detect(labels,sample(true,175),W,H,List.of(REGION),List.of());
        assertEquals(1,counts.size());assertEquals(3,counts.get(0).value());
    }
    @Test public void shadowRetryRequiresHeavyBar() {assertNull(read(sample(false,175)));}
    @Test public void brightPaperDoesNotEnableTheNewRetry() {assertNull(read(sample(true,240)));}
    @Test public void extremeDarkBandAbstains() {assertNull(read(sample(true,70)));}
    @Test public void adjacentTextRemainsExcluded() {
        var g=sample(true,175);rect(g,132,47,3,28,30);assertNull(read(g));
    }
    @Test public void thinStaffRuleCannotEnableRetry() {
        var g=sample(false,175);rect(g,73,101,95,1,30);assertNull(read(g));
    }
    @Test public void ordinaryDarkNumeralStillUsesUnchangedPath() {
        var g=sample(false,255);for(int i=0;i<g.length;i++)if((g[i]&255)==158)g[i]=(byte)255;
        assertEquals(3,read(g).value());
    }
    @Test public void inputIsNotModified() {
        var g=sample(true,175);var before=g.clone();read(g);assertArrayEquals(before,g);
    }
    @Test public void headSuppressionEntryPointDoesNotUseContrastRetry() {
        assertNull(MultiMeasureRestDetector.standaloneCount(sample(true,175),W,H,
                new MultiMeasureRestDetector.RestBarCandidate(0,REGION)));
    }
    @Test public void nearbyPlayableHeadStillBlocksExpansion() {
        var g=sample(true,175);var labels=new byte[W*H];
        for(int y=110;y<117;y++)for(int x=55;x<65;x++)labels[y*W+x]=OmrMeasurePostProcessor.NOTEHEAD;
        assertEquals(List.of(),MultiMeasureRestDetector.detect(labels,g,W,H,List.of(REGION),List.of()));
    }
}
