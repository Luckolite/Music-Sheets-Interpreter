// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original grace-head, slur and accidental geometry. */
public class MergedGraceFlagTest {
    private static final int W=520,H=260;
    private final byte[] labels=new byte[W*H],gray=new byte[W*H];
    public MergedGraceFlagTest() {
        Arrays.fill(gray,(byte)255);
        for(int y=100;y<=164;y+=16)for(int x=20;x<500;x++)ink(x,y,4);
        head(334,148,10,6);for(int y=110;y<=148;y++)ink(344,y,1);
        for(int y=116;y<=156;y++)ink(307,y,1);
        for(int y=119;y<=147;y++)for(int x=306;x<=308;x++)ink(x,y,3);
        for(int y=138;y<=147;y++)for(int x=315;x<=318;x++)ink(x,y,3);
        for(int y:new int[]{138,139,140,145,146,147})for(int x=307;x<=317;x++)ink(x,y,3);
        head(300,156,7,5);
    }
    private void ink(int x,int y,int label){labels[y*W+x]=(byte)label;gray[y*W+x]=0;}
    private void head(int cx,int cy,int rx,int ry) {
        for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
            if((x-cx)*(x-cx)/(float)(rx*rx)+(y-cy)*(y-cy)/(float)(ry*ry)<=1)ink(x,y,2);
    }
    private void mergeSlur() {
        for(int x=299;x<=318;x++) {
            int y=166-Math.abs(x-308)/3;
            ink(x,y,2);
        }
        for(int y=151;y<=170;y++)for(int x=293;x<=317;x++)labels[y*W+x]=2;
    }
    private OmrScoreInterpreter.Analysis score() {
        return OmrScoreInterpreter.analyze(labels,gray,W,H,List.of(new MeasureRegion(.02f,.98f,.2f,.9f)));
    }
    private ScoreNoteEvent mainNote() {
        return score().notes().stream().filter(n->n.positionInMeasure()>.62f).findFirst().orElseThrow();
    }
    @Test public void aMergedGraceSlurDoesNotFlattenTheFollowingNote() {
        mergeSlur();assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,mainNote().writtenAccidental());
    }
    @Test public void aRecoveredGraceHeadKeepsItsPrintedPitch() {
        mergeSlur();var notes=score().notes();assertEquals(2,notes.size());assertEquals(1,notes.get(0).staffStep());
    }
    @Test public void aBrokenStemStillAllowsARealFlat() {
        mergeSlur();for(int y=148;y<=152;y++){labels[y*W+307]=0;gray[y*W+307]=(byte)255;}
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,mainNote().writtenAccidental());
    }
    @Test public void semanticHeadInkWithoutAPrintedHeadCannotHideAFlat() {
        mergeSlur();for(int y=151;y<=161;y++)for(int x=293;x<=306;x++)gray[y*W+x]=(byte)255;
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,mainNote().writtenAccidental());
    }
    @Test public void correctionDoesNotRewriteTheSourcePixels() {
        mergeSlur();var l=labels.clone();var g=gray.clone();score();assertArrayEquals(l,labels);assertArrayEquals(g,gray);
    }
}
