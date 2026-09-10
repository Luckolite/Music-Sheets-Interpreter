// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic notation; no score images or device data are used here. */
public class EngravedSymbolRecoveryTest {
    private static final int W = 400, H = 280;
    private static class Page {
        byte[] labels = new byte[W * H], gray = new byte[W * H];
        Page() {
            Arrays.fill(gray, (byte)255);
            for (int y = 80; y <= 144; y += 16)
                for (int x = 20; x < 380; x++) { labels[y * W + x] = 4; gray[y * W + x] = 0; }
        }
        void head(int x, int y, int radiusY, boolean hollow) {
            for (int row = y - radiusY; row <= y + radiusY; row++)
                for (int col = x - 10; col <= x + 10; col++) {
                    double d = Math.pow((col - x) / 10.0, 2) + Math.pow((row - y) / (double)radiusY, 2);
                    if (d <= 1) { labels[row * W + col] = 2; gray[row * W + col] = hollow && d < .45 ? (byte)255 : 0; }
                }
            for (int row = y - 48; row < y; row++) { labels[row * W + x + 10] = 1; gray[row * W + x + 10] = 0; }
            if (y >= 160) for (int col = x - 14; col <= x + 14; col++) gray[y * W + col] = 0;
        }
        void symbol(int x, int y) { labels[y * W + x] = 3; gray[y * W + x] = 0; }
        List<ScoreNoteEvent> notes() {
            return OmrScoreInterpreter.extract(labels, gray, W, H,
                    List.of(new MeasureRegion(.05f, .95f, .20f, .73f)));
        }
    }

    @Test public void naturalWithShortLowerExtensionStillCancelsTheKey() {
        Page p = new Page(); p.head(180, 128, 8, false);
        for (int y = 108; y <= 140; y++) for (int x = 147; x <= 148; x++) p.symbol(x, y);
        for (int y = 118; y <= 142; y++) for (int x = 157; x <= 158; x++) p.symbol(x, y);
        for (int y : new int[]{119,120,135,136}) for (int x = 147; x <= 158; x++) p.symbol(x, y);
        assertEquals(1, p.notes().size());
        assertEquals(ScoreNoteEvent.ACCIDENTAL_NATURAL, p.notes().get(0).writtenAccidental());
    }

    @Test public void twoSpineSharpTakesPriorityOverFlatLikeLowerInk() {
        Page p = new Page(); p.head(180, 128, 8, false);
        for (int y = 108; y <= 147; y++) for (int x = 150; x <= 151; x++) p.symbol(x, y);
        for (int y = 106; y <= 144; y++) for (int x = 156; x <= 157; x++) p.symbol(x, y);
        for (int x = 146; x <= 161; x++) for (int dy = 0; dy < 6; dy++) {
            p.symbol(x, 121 - (x - 146) / 2 + dy);
            p.symbol(x, 138 - (x - 146) / 2 + dy);
        }
        assertEquals(ScoreNoteEvent.ACCIDENTAL_SHARP, p.notes().get(0).writtenAccidental());
    }

    @Test public void tallFlatBowlDoesNotBecomeASecondSharpSpine() {
        Page p = new Page(); p.head(180,128,8,false);
        for (int y=102;y<=139;y++) for(int x=146;x<=147;x++) p.symbol(x,y);
        for (int y=113;y<=138;y++) for(int x=147;x<=160;x++) {
            double d=Math.pow((x-147)/13.0,2)+Math.pow((y-123)/15.0,2);
            if(d<=1 && d>=.35) p.symbol(x,y);
        }
        assertEquals(ScoreNoteEvent.ACCIDENTAL_FLAT,p.notes().get(0).writtenAccidental());
    }

    @Test public void connectedTriadRetainsAllThreePrintedPitches() {
        Page p = new Page();
        for (int y : new int[]{144,160,176}) p.head(180, y, 10, false);
        assertEquals(List.of(-4,-2,0), p.notes().stream().map(ScoreNoteEvent::staffStep).sorted().toList());
    }

    @Test public void fullBarBassDoesNotFollowTheInsetOfAnotherStaff() {
        ScoreNoteEvent bass = new ScoreNoteEvent(0,.27f,-3,2,3,.68f,false,1,0,
                ScoreNoteEvent.ACCIDENTAL_FROM_KEY,2);
        ScoreNoteEvent melody = new ScoreNoteEvent(0,.10f,5,0,3,.54f,false,0,2);
        ScoreNoteEvent main = new ScoreNoteEvent(0,.27f,3,0,3,.54f,false,0,0,
                ScoreNoteEvent.ACCIDENTAL_FROM_KEY,1);
        assertEquals(0, ScoreNoteTiming.beatInMeasure(bass,List.of(melody,main,bass),3),.0001);
        ScoreNoteEvent afterRest = new ScoreNoteEvent(0,.35f,-3,2,3,.68f,false,0,0,
                ScoreNoteEvent.ACCIDENTAL_FROM_KEY,2).withLeadingRest(1f);
        assertTrue(ScoreNoteTiming.beatInMeasure(afterRest,List.of(melody,afterRest),3) > 0);
    }

    @Test public void isolatedDotSurvivesAThinRuleTouchingItsEdge() {
        Page p = new Page(); p.head(180,136,8,false);
        for (int y = 130; y <= 138; y++) for (int x = 201; x <= 209; x++)
            if ((x-205)*(x-205)+(y-134)*(y-134)<=16) p.gray[y*W+x]=0;
        p.gray[129*W+205]=0;
        assertEquals(1,p.notes().get(0).augmentationDots());
    }

    @Test public void darkDotCoreSurvivesAnAntialiasedTieBridge() {
        Page p = new Page(); p.head(180,136,8,false);
        for (int y = 132; y <= 140; y++) for (int x = 201; x <= 209; x++)
            if ((x-205)*(x-205)+(y-136)*(y-136)<=16) p.gray[y*W+x]=0;
        for (int y=128;y<=132;y++) p.gray[y*W+205]=100;
        assertEquals(1,p.notes().get(0).augmentationDots());
    }

    @Test public void tripletNumeralCanBeOffsetTowardTheStems() {
        byte[] gray = new byte[400 * 200]; Arrays.fill(gray,(byte)255);
        var notes = List.of(new ScoreNoteEvent(0,.25f,2,0,1,.5f,false,0,2),
                new ScoreNoteEvent(0,.3125f,3,0,1,.5f,false,0,2),
                new ScoreNoteEvent(0,.375f,2,0,1,.5f,false,0,2));
        var bars = List.of(new MeasureRegion(0,1,.18f,.58f));
        assertEquals(notes,TripletRhythmDetector.apply(notes,bars,gray,400,200));
        String[] glyph = {"...####..","..######.","......###","......###",
                ".....###.","...####..","...###...","....###..",".....###.",
                "......###","......###","......###",".....###.","..#####..",
                ".#####...","..###...."};
        for(int y=0;y<glyph.length;y++)for(int x=0;x<glyph[y].length();x++)
            if(glyph[y].charAt(x)=='#')gray[(142+y)*400+110+x]=0;
        var result = TripletRhythmDetector.apply(notes,bars,gray,400,200);
        assertTrue(result.stream().allMatch(n->n.tupletDivisor()==3));
        assertEquals(1.0/6,ScoreNoteTiming.writtenDurationBeats(result.get(0)),.0001);
    }
    @Test public void smallStemmedPrefixIsGraceButUniformSmallNotesAreNot() {
        Page p=new Page();smallHead(p,80,120);smallHead(p,110,128);p.head(145,136,8,false);
        p.head(220,128,8,false);p.head(300,120,8,false);
        var notes=p.notes();
        assertEquals(2,notes.stream().filter(n->(n.articulations()&NoteOrnament.GRACE)!=0).count());
        Page small=new Page();smallHead(small,80,120);smallHead(small,110,128);smallHead(small,140,136);
        assertTrue(small.notes().stream().noneMatch(n->(n.articulations()&NoteOrnament.GRACE)!=0));
    }

    private static void smallHead(Page p,int x,int y) {
        for(int row=y-5;row<=y+5;row++)for(int col=x-7;col<=x+7;col++)
            if(Math.pow((col-x)/7.0,2)+Math.pow((row-y)/5.0,2)<=1) {
                p.labels[row*W+col]=2;p.gray[row*W+col]=0;
            }
        for(int row=y-32;row<y;row++){p.labels[row*W+x+7]=1;p.gray[row*W+x+7]=0;}
    }

    @Test public void gracePrefixBorrowsPrincipalTimeWithoutDelayingAccompaniment() {
        var a=new ScoreNoteEvent(0,.10f,5,0,2,.4f,false,0,2).withArticulations(NoteOrnament.GRACE);
        var b=new ScoreNoteEvent(0,.17f,4,0,2,.4f,false,0,2).withArticulations(NoteOrnament.GRACE);
        var main=quarter(.25f,0);var second=quarter(.5f,0);var third=quarter(.75f,0);
        var piano=quarter(.25f,1);var piano2=quarter(.5f,1);var piano3=quarter(.75f,1);
        var notes=List.of(a,b,main,second,third,piano,piano2,piano3);
        assertEquals(0,ScoreNoteTiming.beatInMeasure(a,notes,3),.0001);
        assertEquals(.125,ScoreNoteTiming.beatInMeasure(b,notes,3),.0001);
        assertEquals(.25,ScoreNoteTiming.beatInMeasure(main,notes,3),.0001);
        assertEquals(.75,ScoreNoteTiming.resolvedWrittenDurationBeats(main,notes,3),.0001);
        assertEquals(.125,ScoreNoteTiming.resolvedWrittenDurationBeats(a,notes,3),.0001);
        assertEquals(0,ScoreNoteTiming.beatInMeasure(piano,notes,3),.0001);
        assertEquals(1,ScoreNoteTiming.beatInMeasure(second,notes,3),.0001);
        assertEquals(2,ScoreNoteTiming.beatInMeasure(third,notes,3),.0001);
    }

    private static ScoreNoteEvent quarter(float position,int staff) {
        return new ScoreNoteEvent(0,position,2,staff,2,staff==0?.4f:.7f,false,0,0,
                ScoreNoteEvent.ACCIDENTAL_FROM_KEY,1);
    }

    @Test public void zigzagQuarterRestsRetainTheirFullSilentBeat() {
        Page p=new Page();
        // Original polygonal rest: alternating bends followed by a lower hook.
        int[][] rows={{0,1,2},{1,2,3},{2,3,4},{3,4,5},{4,5,6},{5,6,7},{6,7,8},
                {7,7,10},{8,7,11},{9,6,11},{10,6,11},{11,5,11},{12,5,11},
                {13,4,10},{14,4,9},{15,5,9},{16,6,9},{17,7,10},{18,8,11},
                {19,6,12},{20,4,13},{21,3,13},{22,2,13},{23,2,6},{24,3,6},
                {25,3,6},{26,4,7},{27,5,7},{28,6,8},{29,7,9},{30,8,9}};
        for(int[] row:rows)for(int dy=0;dy<2;dy++)for(int x=row[1];x<=row[2];x++)
            p.gray[(90+(int)Math.round(row[0]*1.5)+dy)*W+170+x]=0;
        var rests=SixteenthRestDetector.detect(p.gray,W,H,List.of(new MeasureRegion(.05f,.95f,.20f,.73f)),
                List.of(new SixteenthRestDetector.Staff(80,144,16,0,1)),List.of());
        assertEquals(1,rests.size());assertEquals(1,rests.get(0).durationBeats(),.0001);
    }

    @Test public void tinyDarkSpotOnAnAntialiasedStemIsNotADot() {
        Page p=new Page();p.head(180,136,8,false);
        for(int y=100;y<=165;y++)for(int x=204;x<=206;x++)p.gray[y*W+x]=110;
        for(int y=134;y<=135;y++)for(int x=204;x<=206;x++)p.gray[y*W+x]=0;
        assertEquals(0,p.notes().get(0).augmentationDots());
    }

    @Test public void midBarGraceKeepsThePrincipalChordTogether() {
        var first=quarter(.10f,0);
        var grace=new ScoreNoteEvent(0,.33f,5,0,2,.4f,false,0,1).withArticulations(NoteOrnament.GRACE);
        var main=quarter(.45f,0);
        var chord=new ScoreNoteEvent(0,.452f,4,0,2,.4f,false,0,0,ScoreNoteEvent.ACCIDENTAL_FROM_KEY,1);
        var last=quarter(.75f,0);var notes=List.of(first,grace,main,chord,last);
        assertEquals(1,ScoreNoteTiming.beatInMeasure(grace,notes,3),.0001);
        assertEquals(1.25,ScoreNoteTiming.beatInMeasure(main,notes,3),.0001);
        assertEquals(1.25,ScoreNoteTiming.beatInMeasure(chord,notes,3),.0001);
        assertEquals(.75,ScoreNoteTiming.resolvedWrittenDurationBeats(chord,notes,3),.0001);
        assertEquals(2,ScoreNoteTiming.beatInMeasure(last,notes,3),.0001);
    }

    @Test public void flagTipSeparatedByAStaffRuleIsNotADot() {
        Page p=new Page();p.head(180,136,8,false);
        for(int y=127;y<=131;y++)for(int x=204;x<=206;x++)p.gray[y*W+x]=0;
        assertEquals(0,p.notes().get(0).augmentationDots());
    }

    @Test public void darkRestBulbsAreNotRhythmDotsForThePreviousNote() {
        Page p=new Page();p.head(180,112,8,false);
        for(int y=96;y<=143;y++) {
            int x=224-Math.round((y-96)*.25f);
            for(int dx=-1;dx<=1;dx++)p.gray[y*W+x+dx]=100;
        }
        for(int cy:new int[]{100,116}) {
            int cx=218-(cy-100)/4;
            for(int y=cy-5;y<=cy+5;y++)for(int x=cx-5;x<=cx+5;x++) {
                int d=(x-cx)*(x-cx)+(y-cy)*(y-cy);
                if(d<=25)p.gray[y*W+x]=(byte)(d<=16?0:100);
            }
            for(int x=cx;x<=cx+6;x++)p.gray[cy*W+x]=100;
        }
        var score=OmrScoreInterpreter.analyze(p.labels,p.gray,W,H,
                List.of(new MeasureRegion(.05f,.95f,.20f,.73f)));
        assertEquals(1,score.rests().size());
        assertEquals(.25,score.rests().get(0).durationBeats(),.001);
        assertEquals(0,score.notes().get(0).augmentationDots());
    }

}
