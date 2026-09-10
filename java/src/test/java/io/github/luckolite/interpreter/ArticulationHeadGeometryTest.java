// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArticulationHeadGeometryTest {
    private static final int W=320,H=480;
    private final byte[] labels=new byte[W*H],gray=new byte[W*H];
    private void ink(int x,int y,int label) { gray[y*W+x]=0;labels[y*W+x]=(byte)label; }
    private void page(boolean smallStem) {
        Arrays.fill(gray,(byte)255);
        for(int y=80;y<=128;y+=12)for(int x=16;x<304;x++)ink(x,y,4);
        for(int y=135;y<=145;y++)for(int x=82;x<=98;x++)
            if(Math.pow((x-90)/8d,2)+Math.pow((y-140)/5d,2)<=1)ink(x,y,2);
        for(int y=100;y<=140;y++)ink(98,y,1);
        for(int y=151;y<=157;y++)for(int x=87;x<=93;x++)
            if((x-90)*(x-90)+(y-154)*(y-154)<=10)ink(x,y,2);
        if(smallStem) {
            for(int y=154;y<=193;y++)ink(87,y,1);
            for(int y=140;y<=152;y+=12)for(int x=77;x<=103;x++)if(labels[y*W+x]!=2)ink(x,y,4);
        }
    }
    private List<ScoreNoteEvent> notes() {
        return OmrScoreInterpreter.analyze(labels,gray,W,H,List.of(new MeasureRegion(.05f,.95f,.05f,.6f))).notes();
    }
    @Test public void staccatoMislabeledAsHeadDoesNotAddAnAttack() {
        page(false);var result=notes();assertEquals(1,result.size());
        assertTrue((result.get(0).articulations()&NoteArticulation.STACCATO)!=0);
    }
    @Test public void smallHeadWithItsOwnStemRemainsANote() {
        page(true);assertEquals(2,notes().size());
    }
    private void highHead(boolean ledger) {
        for(int y=39;y<=49;y++)for(int x=212;x<=228;x++)
            if(Math.pow((x-220)/8d,2)+Math.pow((y-44)/5d,2)<=1)ink(x,y,2);
        for(int y=44;y<=77;y++)ink(212,y,1);
        if(ledger)for(int y=44;y<=68;y+=12)for(int x=207;x<=234;x++)ink(x,y,4);
    }
    @Test public void textLikeOvalAndStrokeAboveStaffDoNotBecomeAPitch() {
        page(false);highHead(false);assertEquals(1,notes().size());
    }
    @Test public void ledgerLinesValidateARealHighNote() {
        page(false);highHead(true);assertEquals(2,notes().size());
    }
    @Test public void oneUnderlineCannotStandInForSeveralLedgerLines() {
        page(false);highHead(false);
        for(int x=207;x<=234;x++)ink(x,44,4);
        assertEquals(1,notes().size());
    }
    @Test public void misclassifiedStaffLineDoesNotTurnStaccatoIntoFermata() {
        Arrays.fill(gray,(byte)255);
        for(int y=80;y<=128;y+=12)for(int x=16;x<304;x++)ink(x,y,5);
        for(int y=83;y<=89;y++)for(int x=157;x<=163;x++)
            if((x-160)*(x-160)+(y-86)*(y-86)<=10)ink(x,y,5);
        var anchors=List.of(new NoteArticulationDetector.Anchor(160,104,12,0));
        assertEquals(NoteArticulation.STACCATO,NoteArticulationDetector.detect(labels,gray,W,H,anchors)[0]);
    }
    @Test public void darkFragmentOfFadedStaffIsNotTenuto() {
        Arrays.fill(gray,(byte)255);
        for(int x=50;x<=270;x++)gray[92*W+x]=(byte)220;
        for(int x=155;x<=165;x++)for(int y=92;y<=93;y++)ink(x,y,5);
        var anchors=List.of(new NoteArticulationDetector.Anchor(160,112,12,0));
        assertEquals(0,NoteArticulationDetector.detect(labels,gray,W,H,anchors)[0]);
    }
    private void flatOval(boolean stem) {
        for(int y=100;y<=104;y++)for(int x=180;x<=194;x++)
            if(Math.pow((x-187)/7d,2)+Math.pow((y-102)/2d,2)<=1)ink(x,y,2);
        if(stem)for(int y=69;y<=102;y++)ink(194,y,1);
    }
    @Test public void flatStemlessSemanticOvalCannotAddAnAttack() {
        page(false);flatOval(false);assertEquals(1,notes().size());
    }
    @Test public void smallFlatHeadWithAnAttachedStemIsPreserved() {
        page(false);flatOval(true);assertEquals(2,notes().size());
    }
}
