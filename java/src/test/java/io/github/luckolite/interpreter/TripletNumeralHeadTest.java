// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original curled numeral outlines and independently constructed note events. */
public class TripletNumeralHeadTest {
    private static String[] three() {
        return new String[]{"..#######...", ".##########.", "###......###", "####.....###",
                "####.....###", "####.....###", ".##.....####", ".......####.",
                "......####..", "....#####...", "....#####...", "....#####...",
                "......####..", ".......####.", "##.....####.", "###....####.",
                "###....####.", "###....####.", ".###....###.", "..########..",
                "..########..", "....####...."};
    }
    private static byte[] image(String[] shape,int top) {
        byte[] gray=new byte[400*240];Arrays.fill(gray,(byte)255);
        for(int y=0;y<shape.length;y++)for(int x=0;x<shape[y].length();x++)
            if(shape[y].charAt(x)=='#')gray[(top+y)*400+119+x]=0;
        return gray;
    }
    private static ScoreNoteEvent moving(float x,int beams) {
        return new ScoreNoteEvent(0,x,2,0,1,.4f,false,0,beams,2,0,1);
    }
    private static ScoreNoteEvent numeral(int top) {
        return new ScoreNoteEvent(0,.3125f,-3,0,1,(top+16)/240f,false,0,0,2,2,1);
    }
    private static List<ScoreNoteEvent> notes(int top) {
        return List.of(moving(.25f,2),numeral(top),moving(.3125f,2),moving(.375f,2));
    }
    private static List<ScoreNoteEvent> clean(List<ScoreNoteEvent> notes,byte[] gray) {
        return TripletRhythmDetector.withoutNumeralHeads(notes,List.of(new MeasureRegion(0,1,.1f,.8f)),gray,400,240);
    }
    private static List<ScoreNoteEvent> pairedBowls() {
        var n=new ArrayList<>(notes(145));
        n.set(1,new ScoreNoteEvent(0,.29f,-3,0,1,161/240f,false,0,0,2,2,1));
        n.add(new ScoreNoteEvent(0,.29f,-1,0,1,150/240f,false,0,0,2,2,1));
        return n;
    }
    private static byte[] shiftedNumeral(String[] shape) {
        var original=image(shape,145);byte[] shifted=new byte[original.length];Arrays.fill(shifted,(byte)255);
        for(int y=0;y<240;y++)for(int x=8;x<400;x++)shifted[y*400+x-8]=original[y*400+x];
        return shifted;
    }
    @Test public void twoPredictedBowlsDoNotInterruptTheSameNumeralCleanup() {
        assertEquals(List.of(moving(.25f,2),moving(.3125f,2),moving(.375f,2)),clean(pairedBowls(),shiftedNumeral(three())));
    }
    @Test public void pairedBowlsStillRequireACompletePrintedThree() {
        var n=pairedBowls();String[] glyph=three();
        for(int y=2;y<glyph.length-2;y++)glyph[y]="##"+glyph[y].substring(2);
        assertEquals(n,clean(n,shiftedNumeral(glyph)));
    }
    @Test public void aRealInterveningNoteStillBlocksPairedNumeralRemoval() {
        var n=new ArrayList<>(pairedBowls());
        n.add(new ScoreNoteEvent(0,.28f,3,0,1,.4f,false,0,0,2,1,1));
        assertEquals(n,clean(n,shiftedNumeral(three())));
    }
    private static String[] extendedCurl() {
        var rows=new ArrayList<>(Arrays.asList(three()));
        rows.add(7,rows.get(6));rows.add(7,rows.get(6));
        return rows.toArray(new String[0]);
    }
    @Test public void upperCurlOpeningOneRasterRowPastTheBandStillCounts() {
        assertEquals(3,clean(notes(145),image(extendedCurl(),145)).size());
    }
    @Test public void rasterBoundaryToleranceWorksAboveTheStaffToo() {
        assertEquals(3,clean(notes(30),image(extendedCurl(),30)).size());
    }
    @Test public void boundaryToleranceDoesNotTurnAClosedCounterIntoAThree() {
        String[] glyph=extendedCurl();for(int y=2;y<glyph.length-2;y++)glyph[y]="##"+glyph[y].substring(2);
        var n=notes(145);assertEquals(n,clean(n,image(glyph,145)));
    }
    @Test public void aOnePixelWaistRemainsAnIndentationAtSmallGlyphWidths() {
        String[] original=three(),wide=new String[original.length];
        for(int y=0;y<wide.length;y++) {
            var line=new StringBuilder();
            for(int x=0;x<19;x++)line.append(original[y].charAt(x*12/19));
            wide[y]=line.toString();
        }
        int lower=-1;
        for(int y=0;y<wide.length;y++)if(y/(float)wide.length>=.6f&&y/(float)wide.length<=.75f)
            lower=Math.max(lower,wide[y].lastIndexOf('#'));
        for(int y=0;y<wide.length;y++)if(y/(float)wide.length>=.37f&&y/(float)wide.length<=.55f) {
            char[] line=wide[y].toCharArray();
            for(int x=wide[y].lastIndexOf('#');x<lower;x++)line[x]='#';
            wide[y]=new String(line);
        }
        assertEquals(3,clean(notes(145),image(wide,145)).size());
    }
    @Test public void aThreeHeadNoLongerInterruptsItsOwnTriplet() {
        var gray=image(three(),145);var cleaned=clean(notes(145),gray);assertEquals(3,cleaned.size());
        var timed=TripletRhythmDetector.apply(cleaned,List.of(new MeasureRegion(0,1,.1f,.8f)),gray,400,240);
        assertTrue(timed.stream().allMatch(n->n.tupletDivisor()==3));
    }
    @Test public void anAboveStaffNumeralCanAlsoBeRemoved() {assertEquals(3,clean(notes(30),image(three(),30)).size());}
    @Test public void aClosedEightDoesNotSupplyTheThreeEvidence() {
        String[] glyph=three();for(int y=2;y<glyph.length-2;y++)glyph[y]="##"+glyph[y].substring(2);
        assertEquals(4,clean(notes(145),image(glyph,145)).size());
    }
    @Test public void aSolidFiveStemDoesNotSupplyTheThreeEvidence() {
        String[] glyph=three();for(int y=2;y<10;y++)glyph[y]="###.........";
        assertEquals(4,clean(notes(145),image(glyph,145)).size());
    }
    @Test public void theAllegedHeadMustOverlapTheNumeralItself() {
        var n=new ArrayList<>(notes(145));n.set(1,new ScoreNoteEvent(0,.3125f,-3,0,1,.8f,false,0,0,2,2,1));
        assertEquals(4,clean(n,image(three(),145)).size());
    }
    @Test public void aGenuineInterveningQuarterCannotBeSkippedToMakeAGroup() {
        var n=new ArrayList<>(notes(145));n.add(new ScoreNoteEvent(0,.28f,3,0,1,.4f,false,0,0,2,1,1));
        assertEquals(5,clean(n,image(three(),145)).size());
    }
    @Test public void unevenBeamValuesDoNotAuthorizeRemoval() {
        var n=new ArrayList<>(notes(145));n.set(2,moving(.3125f,1));assertEquals(4,clean(n,image(three(),145)).size());
    }
    @Test public void graceNotesDoNotCreateAMetricalTriplet() {
        var n=new ArrayList<>(notes(145));n.set(0,n.get(0).withArticulations(NoteOrnament.GRACE));
        assertEquals(4,clean(n,image(three(),145)).size());
    }
    @Test public void twoNotesAreInsufficientEvidence() {
        var n=new ArrayList<>(notes(145));n.remove(3);assertEquals(3,clean(n,image(three(),145)).size());
    }
    @Test public void sourceNotesAndPixelsRemainUnchanged() {
        var n=notes(145);byte[] g=image(three(),145),saved=g.clone();clean(n,g);
        assertEquals(4,n.size());assertArrayEquals(saved,g);
    }
    @Test public void rawScoreExtractionRemovesTheNumeralBeforeRestAndTupletTiming() {
        byte[] gray=image(three(),155),labels=new byte[gray.length];
        for(int y=80;y<=144;y+=16)for(int x=20;x<380;x++){gray[y*400+x]=0;labels[y*400+x]=4;}
        for(int cx:new int[]{100,125,150}) {
            for(int y=99;y<=109;y++)for(int x=cx-8;x<=cx+8;x++)
                if(Math.pow((x-cx)/8.0,2)+Math.pow((y-104)/5.0,2)<=1){gray[y*400+x]=0;labels[y*400+x]=2;}
            for(int y=54;y<=104;y++){gray[y*400+cx+8]=0;labels[y*400+cx+8]=1;}
        }
        for(int y:new int[]{54,55,56,57,58,64,65,66,67,68})for(int x=108;x<=158;x++){gray[y*400+x]=0;labels[y*400+x]=1;}
        for(int y=167;y<=177;y++)for(int x=118;x<=131;x++)
            if(Math.pow((x-125)/7.0,2)+Math.pow((y-172)/5.0,2)<=1)labels[y*400+x]=2;
        var regions=List.of(new MeasureRegion(0,1,.1f,.8f));
        var score=OmrScoreInterpreter.analyze(labels,gray,400,240,regions);
        assertEquals(score.notes().toString(),3,score.notes().size());
        var timed=TripletRhythmDetector.apply(score.notes(),regions,gray,400,240);
        assertTrue(timed.stream().allMatch(n->n.tupletDivisor()==3));
    }
}
