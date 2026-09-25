// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original two-island semantic masks inside a single printed oval. */
public class ShadedSplitFragmentOwnershipTest {
    ShadedNoteheadRecoveryTest fixture() {
        var p=new ShadedNoteheadRecoveryTest();
        p.fixture(true,240,185,false);
        for(int y=87;y<=94;y++)for(int x=80;x<=87;x++)
            if((x-83.5f)*(x-83.5f)+(y-90.5f)*(y-90.5f)<16)p.labels[y*240+x]=2;
        for(int y=83;y<=92;y++)for(int x=98;x<=100;x++)
            if(x==99||y>=88&&x==100)p.labels[y*240+x]=2;
        return p;
    }
    List<ScoreNoteEvent> notes(ShadedNoteheadRecoveryTest p) {
        return OmrScoreInterpreter.extract(p.labels,p.gray,240,220,
                List.of(new MeasureRegion(.06f,.95f,.2f,.75f)));
    }
    @Test public void onePrintedOvalCannotKeepTwoSemanticFragmentEvents() {
        var notes=notes(fixture());
        assertEquals(1,notes.size());
        assertEquals(5,notes.get(0).staffStep());
    }
    @Test public void singlePixelSemanticOvershootStillBelongsToTheOval() {
        var p=fixture();
        for(int y=87;y<=94;y++)for(int x=77;x<=87;x++)p.labels[y*240+x]=0;
        for(int y=87;y<=94;y++)for(int x=77;x<=85;x++)
            if((x-81)*(x-81)+(y-90.5f)*(y-90.5f)<18)p.labels[y*240+x]=2;
        assertEquals(1,notes(p).size());
    }
    @Test public void neighboringCompleteHeadWithItsOwnStemIsRetained() {
        var p=fixture();
        for(int y=85;y<=95;y++)for(int x=121;x<=139;x++)
            if((x-130)*(x-130)/81f+(y-90)*(y-90)/25f<=1) {
                p.gray[y*240+x]=30;p.labels[y*240+x]=2;
            }
        for(int y=48;y<=90;y++){p.gray[y*240+139]=30;p.labels[y*240+139]=1;}
        var notes=notes(p);assertEquals(2,notes.size());
        assertTrue(notes.stream().allMatch(n->n.staffStep()==5));
    }
    @Test public void twoVerticallyAdjacentOriginalHeadsStaySeparate() {
        var p=fixture();
        for(int y=105;y<=115;y++)for(int x=81;x<=99;x++)
            if((x-90)*(x-90)/81f+(y-110)*(y-110)/25f<=1) {
                p.gray[y*240+x]=30;p.labels[y*240+x]=2;
            }
        for(int y=90;y<=150;y++){p.gray[y*240+81]=30;p.labels[y*240+81]=1;}
        var notes=notes(p);assertEquals(2,notes.size());
        assertEquals(java.util.Set.of(3,5),new java.util.HashSet<>(notes.stream().map(ScoreNoteEvent::staffStep).toList()));
    }
}
