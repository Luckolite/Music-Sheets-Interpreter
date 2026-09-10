// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic shapes; no score images or device data. */
public class StaffAndTupletRecoveryTest {
    private static final String[] CURLY_THREE = {
        "..#######...", ".##########.", "###......###", "####.....###",
        "####.....###", "####.....###", ".##.....####", ".......####.",
        "......####..", "....#####...", "....#####...", "......####..",
        ".......####.", "##.....####.", "###....####.", "###....####.",
        "###....####.", ".###....###.", "..########..", "....####...."
    };

    private List<ScoreNoteEvent> triplet(String[] glyph) {
        byte[] gray = new byte[400 * 200]; Arrays.fill(gray, (byte)255);
        var notes = List.of(new ScoreNoteEvent(0,.25f,2,0,1,.5f,false,0,2),
                new ScoreNoteEvent(0,.3125f,3,0,1,.5f,false,0,2),
                new ScoreNoteEvent(0,.375f,2,0,1,.5f,false,0,2));
        for (int y=0;y<glyph.length;y++) for (int x=0;x<glyph[y].length();x++)
            if (glyph[y].charAt(x)=='#') gray[(140+y)*400+116+x]=0;
        return TripletRhythmDetector.apply(notes,List.of(new MeasureRegion(0,1,.18f,.58f)),gray,400,200);
    }

    @Test public void curledThreeRetainsItsTripletValue() {
        var result=triplet(CURLY_THREE);
        assertTrue(result.stream().allMatch(n->n.tupletDivisor()==3));
        assertEquals(1.0/6,ScoreNoteTiming.writtenDurationBeats(result.get(0)),.0001);
    }

    @Test public void closedEightDoesNotBecomeATriplet() {
        String[] glyph=CURLY_THREE.clone();
        for(int y=2;y<18;y++)glyph[y]="##"+glyph[y].substring(2);
        assertTrue(triplet(glyph).stream().allMatch(n->n.tupletDivisor()==1));
    }

    @Test public void upperLeftStemOfFiveDoesNotBecomeATriplet() {
        String[] glyph=CURLY_THREE.clone();
        for(int y=2;y<10;y++)glyph[y]="###.........";
        assertTrue(triplet(glyph).stream().allMatch(n->n.tupletDivisor()==1));
    }

    private static class Page {
        final int w=400,h=280;
        byte[] labels=new byte[w*h],gray=new byte[w*h];
        Page(boolean shifted) {
            Arrays.fill(gray,(byte)255);
            for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++)gray[y*w+x]=0;
            for(int y=shifted?96:80;y<=(shifted?160:144);y+=16)
                for(int x=20;x<380;x++)labels[y*w+x]=4;
        }
        void head(int cy,boolean hollow) {
            for(int y=cy-9;y<=cy+9;y++)for(int x=170;x<=190;x++) {
                double d=Math.pow((x-180)/10.0,2)+Math.pow((y-cy)/9.0,2);
                if(d<=1){labels[y*w+x]=2;gray[y*w+x]=(byte)(hollow&&d<.45?255:0);}
            }
            for(int y=cy-48;y<cy;y++){labels[y*w+190]=1;gray[y*w+190]=0;}
            if(cy>=160)for(int x=166;x<=194;x++)gray[cy*w+x]=0;
        }
        List<ScoreNoteEvent> notes() {
            return OmrScoreInterpreter.extract(labels,gray,w,h,List.of(new MeasureRegion(.05f,.95f,.20f,.75f)));
        }
    }

    private Page weakenedMiddle(boolean hollow) {
        Page p=new Page(false);
        for(int y:new int[]{144,160,176})p.head(y,hollow);
        // Semantic neck paint joins the ovals while the middle oval loses its edges.
        for(int y=151;y<=169;y++)for(int x=170;x<=190;x++)
            if(p.labels[y*p.w+x]==2 && (x<174||x>186))p.labels[y*p.w+x]=0;
        for(int y=144;y<=176;y++)for(int x=177;x<=183;x++)p.labels[y*p.w+x]=2;
        return p;
    }

    @Test public void hollowTriadSurvivesAWeakMiddleMaskLobe() {
        var notes=weakenedMiddle(true).notes();
        assertEquals(List.of(-4,-2,0),notes.stream().map(ScoreNoteEvent::staffStep).sorted().toList());
        assertTrue(notes.stream().allMatch(n->n.unbeamedDurationBeats()==2));
    }

    @Test public void tallSolidMaskDoesNotInventThreeHollowNotes() {
        assertTrue(weakenedMiddle(false).notes().size()<3);
    }

    @Test public void printedStaffCorrectsAOneRuleSemanticShift() {
        Page p=new Page(true);p.head(168,false);
        assertEquals(1,p.notes().size());
        assertEquals(-3,p.notes().get(0).staffStep());
    }

    @Test public void completeAccompanimentAnchorsASparseMelodyAfterGraceNotes() {
        var grace=new ScoreNoteEvent(0,.05f,3,0,2,.4f,false,0,2).withArticulations(NoteOrnament.GRACE);
        var main=new ScoreNoteEvent(0,.26f,2,0,2,.4f,false,0,0,ScoreNoteEvent.ACCIDENTAL_FROM_KEY,1);
        var piano=new ScoreNoteEvent(0,.26f,2,1,2,.7f,false,0,0,ScoreNoteEvent.ACCIDENTAL_FROM_KEY,1);
        var second=new ScoreNoteEvent(0,.50f,3,1,2,.7f,false,0,0,ScoreNoteEvent.ACCIDENTAL_FROM_KEY,1);
        var third=new ScoreNoteEvent(0,.80f,4,1,2,.7f,false,0,0,ScoreNoteEvent.ACCIDENTAL_FROM_KEY,1);
        var notes=List.of(grace,main,piano,second,third);
        assertEquals(0,ScoreNoteTiming.beatInMeasure(piano,notes,3),.0001);
        assertEquals(0,ScoreNoteTiming.beatInMeasure(grace,notes,3),.0001);
        assertEquals(.25,ScoreNoteTiming.beatInMeasure(main,notes,3),.0001);
        assertEquals(1,ScoreNoteTiming.beatInMeasure(second,notes,3),.0001);
    }

    @Test public void flatOutlineCanCrossSemanticLabels() {
        Page p=new Page(false);p.head(128,false);
        // Original flat: a gray spine and a darker, hollow lower-right bowl.
        for(int y=98;y<=134;y++)for(int x=151;x<=152;x++) {
            p.labels[y*p.w+x]=3;p.gray[y*p.w+x]=110;
        }
        for(int y=119;y<=134;y++)for(int x=152;x<=164;x++) {
            double d=Math.pow((x-152)/12.0,2)+Math.pow((y-126)/8.0,2);
            if(d<=1&&d>=.30){p.labels[y*p.w+x]=5;p.gray[y*p.w+x]=0;}
        }
        var notes=p.notes();assertEquals(1,notes.size());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,notes.get(0).writtenAccidental());
    }

    @Test public void restUsesPrintedRulesWhenSemanticLinesAreOffset() {
        Page p=new Page(false);Arrays.fill(p.labels,(byte)0);
        for(int y=84;y<=148;y+=16)for(int x=20;x<380;x++)p.labels[y*p.w+x]=4;
        for(int y=79;y<=145;y++)if((y-79)%16<=2)
            for(int x=20;x<380;x++)p.gray[y*p.w+x]=0;
        for(int y=97;y<=129;y++)for(int dx=-1;dx<=1;dx++)
            p.gray[y*p.w+220-Math.round((y-97)*.25f)+dx]=0;
        for(int y=97;y<=107;y++)for(int x=205;x<=217;x++)
            if(Math.pow((x-211)/6.0,2)+Math.pow((y-102)/5.0,2)<=1)p.gray[y*p.w+x]=0;
        for(int x=211;x<=220;x++)p.gray[102*p.w+x]=0;
        var score=OmrScoreInterpreter.analyze(p.labels,p.gray,p.w,p.h,List.of(new MeasureRegion(.05f,.95f,.20f,.75f)));
        assertEquals(1,score.rests().size());
        assertEquals(.5,score.rests().get(0).durationBeats(),.0001);
    }

    @Test public void thinSlurTerminalNearAStemIsNotAnotherBeam() {
        Page p=new Page(false);Arrays.fill(p.labels,(byte)0);Arrays.fill(p.gray,(byte)255);
        for(int y=100;y<=156;y+=14)for(int x=20;x<380;x++) {
            p.labels[y*p.w+x]=4;p.gray[y*p.w+x]=0;
        }
        p.head(128,false);
        for(int y=70;y<=128;y++){p.labels[y*p.w+190]=1;p.gray[y*p.w+190]=0;}
        for(int y=90;y<=97;y++)for(int x=130;x<=190;x++)p.gray[y*p.w+x]=0;
        for(int y=79;y<=82;y++)for(int x=172;x<=190;x++)p.gray[y*p.w+x]=0;
        var notes=p.notes();assertEquals(1,notes.size());
        assertEquals(1,notes.get(0).beamCount());
    }
}
