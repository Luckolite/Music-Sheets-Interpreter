// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original note/flag/flat geometry, without score samples. */
public class GraceFlagAccidentalTest {
    private static final int W=520,H=260;
    private final byte[] labels=new byte[W*H],gray=new byte[W*H];
    public GraceFlagAccidentalTest() {
        Arrays.fill(gray,(byte)255);
        for(int y=100;y<=164;y+=16)for(int x=20;x<500;x++)ink(x,y,4);
        head(334,148,10,6);for(int y=110;y<=148;y++)ink(344,y,1);
    }
    private void ink(int x,int y,int label){labels[y*W+x]=(byte)label;gray[y*W+x]=0;}
    private void head(int cx,int cy,int rx,int ry) {
        for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
            if((x-cx)*(x-cx)/(float)(rx*rx)+(y-cy)*(y-cy)/(float)(ry*ry)<=1)ink(x,y,2);
    }
    private void flag(boolean grace) {
        for(int y=116;y<=156;y++)ink(307,y,1);
        for(int y=119;y<=147;y++)for(int x=306;x<=308;x++)ink(x,y,3);
        for(int y=138;y<=147;y++)for(int x=315;x<=318;x++)ink(x,y,3);
        for(int y:new int[]{138,139,140,145,146,147})for(int x=307;x<=317;x++)ink(x,y,3);
        if(grace)head(300,156,7,5);
    }
    private ScoreNoteEvent mainNote() {
        return OmrScoreInterpreter.analyze(labels,gray,W,H,
                List.of(new MeasureRegion(.02f,.98f,.2f,.9f))).notes().stream()
                .filter(n->n.positionInMeasure()>.62f).findFirst().orElseThrow();
    }
    @Test public void anAttachedGraceFlagDoesNotFlattenTheFollowingNote() {
        flag(true);assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,mainNote().writtenAccidental());
    }
    @Test public void flagWithNoNoteheadStillReceivesNormalAccidentalClassification() {
        flag(false);assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,mainNote().writtenAccidental());
    }
    @Test public void nearbyHeadWithoutAConnectingStemDoesNotHideAFlat() {
        flag(true);for(int y=148;y<=152;y++){labels[y*W+307]=0;gray[y*W+307]=(byte)255;}
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,mainNote().writtenAccidental());
    }
    @Test public void aSeparateSmallNoteCannotClaimTheAccidentalSpine() {
        flag(false);head(284,156,7,5);for(int y=116;y<=156;y++)ink(291,y,1);
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,mainNote().writtenAccidental());
    }
    @Test public void theAcceptedGraceHeadIsPreserved() {
        flag(true);var score=OmrScoreInterpreter.analyze(labels,gray,W,H,
                List.of(new MeasureRegion(.02f,.98f,.2f,.9f)));
        assertEquals(2,score.notes().size());assertEquals(1,score.notes().get(0).staffStep());
    }
    @Test public void classificationDoesNotEditTheSourceMask() {
        flag(true);var l=labels.clone();var g=gray.clone();mainNote();
        assertArrayEquals(l,labels);assertArrayEquals(g,gray);
    }
}
