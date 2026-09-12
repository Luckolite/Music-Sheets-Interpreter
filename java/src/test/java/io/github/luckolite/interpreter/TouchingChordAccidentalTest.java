// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric glyphs; no score pixels or model outputs. */
public class TouchingChordAccidentalTest {
    static final int W=360,H=210,G=16;
    final byte[] labels=new byte[W*H],gray=new byte[W*H];
    public TouchingChordAccidentalTest(){Arrays.fill(gray,(byte)255);for(int y=80;y<=144;y+=G)rect(20,y,340,y,4);}
    void rect(int left,int top,int right,int bottom,int label){for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++){labels[y*W+x]=(byte)label;gray[y*W+x]=0;}}
    void sharp(int x,int y){rect(x-5,y-22,x-3,y+22,3);rect(x+3,y-22,x+5,y+22,3);for(int cy:new int[]{y-6,y+6})rect(x-8,cy-1,x+9,cy+2,3);}
    void natural(int x,int y){rect(x-5,y-24,x-3,y+12,3);rect(x+4,y-12,x+6,y+24,3);rect(x-5,y-11,x+6,y-8,3);rect(x-5,y+7,x+6,y+10,3);}
    void head(int x,int y){for(int yy=y-4;yy<=y+4;yy++)for(int xx=x-7;xx<=x+7;xx++)if((xx-x)*(xx-x)/49d+(yy-y)*(yy-y)/16d<=1)rect(xx,yy,xx,yy,2);rect(x+7,y-34,x+7,y,1);}
    void pair(boolean joined,boolean reversed,boolean bothHeads){int sy=reversed?104:120,ny=reversed?120:104;sharp(180,sy);natural(200,ny);if(joined)rect(189,reversed?110:114,195,reversed?111:115,3);head(216,sy);if(bothHeads)head(216,ny);}
    List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.extract(labels,gray,W,H,List.of(new MeasureRegion(100f/W,335f/W,50f/H,177f/H)));}
    int accidental(List<ScoreNoteEvent> notes,int y){var found=notes.stream().filter(n->Math.abs(n.pageY()*H-y)<3).toList();assertEquals("head at "+y,1,found.size());return found.get(0).writtenAccidental();}
    @Test public void touchingSharpAndNaturalApplyToTheirOwnChordTones(){pair(true,false,true);var notes=notes();assertEquals(2,notes.size());assertEquals(1,accidental(notes,120));assertEquals(0,accidental(notes,104));}
    @Test public void reversedPitchOrderAlsoSeparates(){pair(true,true,true);var notes=notes();assertEquals(2,notes.size());assertEquals(1,accidental(notes,104));assertEquals(0,accidental(notes,120));}
    @Test public void separatedSymbolsRemainReadable(){pair(false,false,true);var notes=notes();assertEquals(1,accidental(notes,120));assertEquals(0,accidental(notes,104));}
    @Test public void loneHeadDoesNotJustifySplittingTwoSymbols(){pair(true,false,false);var notes=notes();assertEquals(1,notes.size());assertEquals(ScoreNoteEvent.ACCIDENTAL_FROM_KEY,accidental(notes,120));}
    @Test public void inputMasksRemainUnchanged(){pair(true,false,true);var l=labels.clone();var g=gray.clone();notes();assertArrayEquals(l,labels);assertArrayEquals(g,gray);}
}
