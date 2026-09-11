// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original curves and noteheads with independently varied semantic predictions. */
public class NoteParenthesisTest {
    private static final int W=520,H=280;
    private static final List<MeasureRegion> M=List.of(new MeasureRegion(.02f,.98f,.2f,.95f));
    private static class Page {
        final byte[] gray=new byte[W*H],labels=new byte[W*H];
        Page(boolean owner) {
            Arrays.fill(gray,(byte)255);
            for(int y=100;y<=164;y+=16)for(int x=20;x<500;x++)ink(x,y,4);
            head(356,172,11,7);for(int y=124;y<=172;y++)ink(367,y,1);
            if(owner){head(305,156,11,7);for(int y=156;y<=202;y++)ink(294,y,1);}
        }
        void ink(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
        void head(int cx,int cy,int rx,int ry){for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
            if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1)ink(x,y,2);}
        void curve(int left,int top,boolean opening,int label) {
            for(int y=0;y<29;y++) {
                double t=(y-14)/14.;int dx=(int)Math.round(6*t*t);
                int x=left+(opening?dx:6-dx);
                ink(x,top+y,label);ink(x+1,top+y,label);
            }
        }
        void flatMask(int left,int top,int label,boolean printed) {
            for(int y=top;y<=top+28;y++)for(int x=left;x<=left+1;x++)mark(x,y,label,printed);
            for(int y=top+16;y<=top+27;y++)for(int x=left+6;x<=left+7;x++)mark(x,y,label,printed);
            for(int y:new int[]{top+16,top+17,top+26,top+27})for(int x=left;x<=left+7;x++)mark(x,y,label,printed);
        }
        void mark(int x,int y,int label,boolean printed){labels[y*W+x]=(byte)label;if(printed)gray[y*W+x]=0;}
        void pair(boolean opening,boolean closing,int label) {
            if(opening)curve(280,142,true,5);
            curve(325,142,!closing,label);
            for(int y=142;y<=170;y++)for(int x=325;x<=332;x++)labels[y*W+x]=0;
            flatMask(325,142,label,false);
        }
        OmrScoreInterpreter.Analysis analyze(){return OmrScoreInterpreter.analyze(labels,gray,W,H,M);}
        ScoreNoteEvent target(){return analyze().notes().stream().filter(n->Math.abs((.02f+n.positionInMeasure()*.96f)*W-356)<3).findFirst().orElseThrow();}
    }
    @Test public void closingParenthesisDoesNotFlattenTheFollowingNote(){var p=new Page(true);p.pair(true,true,3);assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,p.target().writtenAccidental());}
    @Test public void enclosedOptionalNoteRemainsPresent(){var p=new Page(true);p.pair(true,true,3);var a=p.analyze();assertEquals(2,a.notes().size());assertEquals(1,a.notes().get(0).staffStep());}
    @Test public void symbolLabelAlsoUsesThePrintedPair(){var p=new Page(true);p.pair(true,true,5);assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,p.target().writtenAccidental());}
    @Test public void oneCurveIsInsufficient(){var p=new Page(true);p.pair(false,true,3);assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,p.target().writtenAccidental());}
    @Test public void curvesMustEncloseAnAcceptedNote(){var p=new Page(false);p.pair(true,true,3);assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,p.target().writtenAccidental());}
    @Test public void twoCurvesFacingTheSameWayAreNotAPair(){var p=new Page(true);p.pair(true,false,3);assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,p.target().writtenAccidental());}
    @Test public void mismatchedVerticalPositionsAreNotAPair(){var p=new Page(true);p.pair(false,true,3);p.curve(280,118,true,5);assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,p.target().writtenAccidental());}
    @Test public void anActualPrintedFlatRetainsItsMeaning(){var p=new Page(true);p.curve(280,142,true,5);p.flatMask(325,142,3,true);assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,p.target().writtenAccidental());}
    @Test public void semanticMasksAloneDoNotProveParentheses(){var p=new Page(true);p.pair(true,true,3);var a=OmrScoreInterpreter.analyze(p.labels,null,W,H,M);assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,a.notes().get(a.notes().size()-1).writtenAccidental());}
    @Test public void parenthesesAroundACautionaryFlatDoNotCancelIt(){var p=new Page(false);p.curve(290,148,true,5);p.curve(330,148,false,5);p.flatMask(317,142,3,true);assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,p.target().writtenAccidental());}
    @Test public void classificationPreservesSourcePixels(){var p=new Page(true);p.pair(true,true,3);var l=p.labels.clone();var g=p.gray.clone();p.analyze();assertArrayEquals(l,p.labels);assertArrayEquals(g,p.gray);}
}
