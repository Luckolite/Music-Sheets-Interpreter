// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometry only; no score scans or song-specific production rules. */
public class StablePitchGeometryTest {
    private static class Page {
        final int w=400,h=400;
        final byte[] labels=new byte[w*h],gray=new byte[w*h];
        Page(boolean grand) {
            Arrays.fill(gray,(byte)255);
            for(int top:grand?new int[]{80,240}:new int[]{80})
                for(int y=top;y<=top+64;y+=16)for(int x=20;x<380;x++) {
                    labels[y*w+x]=4;gray[y*w+x]=0;
                }
        }
        void ink(int x1,int x2,int y1,int y2) {
            for(int y=y1;y<=y2;y++)for(int x=x1;x<=x2;x++)gray[y*w+x]=0;
        }
        void head(int cy,boolean up) {
            for(int y=cy-7;y<=cy+7;y++)for(int x=170;x<=190;x++)
                if(Math.pow((x-180)/10.,2)+Math.pow((y-cy)/7.,2)<=1) {
                    labels[y*w+x]=2;gray[y*w+x]=0;
                }
            int x=up?190:170;
            for(int y=up?cy-48:cy;y<=(up?cy:cy+48);y++) {
                labels[y*w+x]=1;gray[y*w+x]=0;
            }
        }
        ScoreNoteEvent note() {
            var notes=OmrScoreInterpreter.extract(labels,gray,w,h,List.of(new MeasureRegion(.05f,.95f,.1f,.85f)));
            assertEquals(1,notes.size());return notes.get(0);
        }
    }
    @Test public void upperBeamEdgeCannotPullPitchOffThePrintedStaff() {
        Page p=new Page(false);p.head(88,false);
        p.ink(120,240,140,148);p.ink(120,240,135,137);
        assertEquals(7,p.note().staffStep());
    }
    @Test public void lowerBeamEdgeCannotPullPitchOffThePrintedStaff() {
        Page p=new Page(false);p.head(104,false);
        p.ink(120,240,140,148);p.ink(120,240,151,153);
        assertEquals(5,p.note().staffStep());
    }
    @Test public void agreementStillFollowsALocallyShiftedStaff() {
        Page p=new Page(false);
        for(int y=70;y<160;y++)for(int x=120;x<=240;x++)p.gray[y*p.w+x]=(byte)255;
        for(int y=85;y<=149;y+=16)p.ink(120,240,y,y);
        p.head(93,false);
        assertEquals(7,p.note().staffStep());
    }
    @Test public void highBassLedgerChainOverridesAnUpwardBeamStem() {
        Page p=new Page(true);p.head(200,true);p.ink(165,195,208,209);p.ink(165,195,224,225);
        var n=p.note();assertEquals(1,n.staffIndex());assertEquals(13,n.staffStep());
    }
    @Test public void lowUpperLedgerChainOverridesADownwardBeamStem() {
        Page p=new Page(true);p.head(184,false);p.ink(165,195,159,160);p.ink(165,195,175,176);
        var n=p.note();assertEquals(0,n.staffIndex());assertEquals(-5,n.staffStep());
    }
    @Test public void ambiguousHeadWithoutLedgerEvidenceKeepsStemFallback() {
        Page p=new Page(true);p.head(200,true);
        var notes=OmrScoreInterpreter.extract(p.labels,p.w,p.h,List.of(new MeasureRegion(.05f,.95f,.1f,.85f)));
        assertEquals(1,notes.size());assertEquals(0,notes.get(0).staffIndex());
    }
    private boolean glyph(boolean sharp)throws Exception {
        int w=100,h=100,gap=14;byte[] labels=new byte[w*h];
        for(int y=0;y<44;y++)for(int x=0;x<24;x++) {
            boolean ink=x>=8&&x<=9||sharp&&x>=16&&x<=17
                    ||sharp&&x>=4&&x<=20&&(y>=12&&y<=16||y>=27&&y<=31)
                    ||!sharp&&y>=24&&y<=36&&x>=8&&x<=17
                    ||y==30;
            if(ink)labels[(20+y)*w+20+x]=3;
        }
        int area=0;for(byte value:labels)if(value==3)area++;
        Class<?> component=Class.forName(OmrScoreInterpreter.class.getName()+"$Component");
        var cc=component.getDeclaredConstructors()[0];cc.setAccessible(true);
        Object c=cc.newInstance(area,20,43,20,63,32f,42f);
        Class<?> candidate=Class.forName(OmrScoreInterpreter.class.getName()+"$AccidentalCandidate");
        var ac=candidate.getDeclaredConstructors()[0];ac.setAccessible(true);
        var method=OmrScoreInterpreter.class.getDeclaredMethod("isSharpGlyph",byte[].class,int.class,int.class,candidate,float.class);method.setAccessible(true);
        return (boolean)method.invoke(null,labels,w,h,ac.newInstance(c,(byte)3),(float)gap);
    }
    @Test public void sharpWithStaffFringeKeepsBothSpinesAndCrossbars()throws Exception { assertTrue(glyph(true)); }
    @Test public void widenedFlatDoesNotBecomeSharp()throws Exception { assertFalse(glyph(false)); }

    private List<ScoreNoteEvent> seconds(boolean shared) {
        Page p=new Page(false);
        for(int[] center:new int[][]{{180,104},{202,112}}) {
            int cx=center[0],cy=center[1];
            for(int y=cy-7;y<=cy+7;y++)for(int x=cx-10;x<=cx+10;x++)
                if(Math.pow((x-cx)/10.,2)+Math.pow((y-cy)/7.,2)<=1) {
                    p.labels[y*p.w+x]=2;p.gray[y*p.w+x]=0;
                }
        }
        for(int x:shared?new int[]{191}:new int[]{170,212})for(int y=64;y<109;y++) {
            p.labels[y*p.w+x]=1;p.gray[y*p.w+x]=0;
        }
        return OmrScoreInterpreter.extract(p.labels,p.gray,p.w,p.h,List.of(new MeasureRegion(.05f,.95f,.1f,.5f)));
    }
    @Test public void separateSecondHeadsSharingOneStemHaveOneAttack() {
        var notes=seconds(true);assertEquals(2,notes.size());
        assertEquals(notes.get(0).positionInMeasure(),notes.get(1).positionInMeasure(),0);
    }
    @Test public void closeSecondsWithoutASharedStemKeepSeparateAttacks() {
        var notes=seconds(false);assertEquals(2,notes.size());
        assertTrue(Math.abs(notes.get(0).positionInMeasure()-notes.get(1).positionInMeasure())>.018);
    }
    @Test public void mixedChordValuesPreserveTheQuarterVoiceDuration() {
        var quarter=new ScoreNoteEvent(0,.1f,10,0,1,.3f,false,0,0,2,1);
        var eighth=new ScoreNoteEvent(0,.1f,0,0,1,.4f,false,0,1,2,0);
        var next=new ScoreNoteEvent(0,.3f,2,0,1,.4f,false,0,1,2,0);
        var notes=List.of(quarter,eighth,next);
        assertEquals(1,ScoreNoteTiming.resolvedWrittenDurationBeats(quarter,notes,3),0);
        assertEquals(.5,ScoreNoteTiming.resolvedWrittenDurationBeats(eighth,notes,3),0);
    }
}
