// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original numeral strokes distinguish chord fingerings from rhythmic tuplets. */
public final class StackedFingeringTest {
 static final int W=400,H=260;
 static final List<MeasureRegion> M=List.of(new MeasureRegion(0,1,.15f,.65f));
 static final String[] THREE={"..#######...", ".##########.", "###......###", "####.....###",
  "####.....###", "####.....###", ".##.....####", ".......####.", "......####..", "....#####...",
  "....#####...", "....#####...", "......####..", ".......####.", "##.....####.", "###....####.",
  "###....####.", "###....####.", ".###....###.", "..########..", "..########..", "....####...."};
 static byte[] image(){byte[] a=new byte[W*H];Arrays.fill(a,(byte)255);for(int y=0;y<THREE.length;y++)for(int x=0;x<12;x++)if(THREE[y].charAt(x)=='#')a[(145+y)*W+119+x]=0;return a;}
 static void one(byte[] a,int x,int y){for(int j=0;j<20;j++)for(int i=0;i<8;i++)if(i==3||i==4||j>=18||j<3&&i>=1&&i<=4)a[(y+j)*W+x+i]=0;}
 static List<ScoreNoteEvent> notes(int voices){var out=new ArrayList<ScoreNoteEvent>();for(float x:new float[]{.25f,.3125f,.375f})for(int v=0;v<voices;v++)out.add(new ScoreNoteEvent(0,x,2+v,0,1,.35f+v*.03f,false,0,3,2,0,1));return out;}
 static List<ScoreNoteEvent> apply(byte[] a,int voices){return TripletRhythmDetector.apply(notes(voices),M,a,W,H);}
 @Test public void standaloneThreeStillMarksChordTuplet(){assertTrue(apply(image(),2).stream().allMatch(n->n.tupletDivisor()==3));}
 @Test public void upperStackedFingerDoesNotShortenChords(){var a=image();one(a,121,117);assertEquals(notes(2),apply(a,2));}
 @Test public void lowerStackedFingerDoesNotShortenChords(){var a=image();one(a,121,174);assertEquals(notes(2),apply(a,2));}
 @Test public void horizontalNeighborDoesNotSuppressTuplet(){var a=image();one(a,143,145);assertTrue(apply(a,2).stream().allMatch(n->n.tupletDivisor()==3));}
 @Test public void distantGlyphDoesNotSuppressTuplet(){var a=image();one(a,121,90);assertTrue(apply(a,2).stream().allMatch(n->n.tupletDivisor()==3));}
 @Test public void singleVoiceKeepsItsTupletReading(){var a=image();one(a,121,117);assertTrue(apply(a,1).stream().allMatch(n->n.tupletDivisor()==3));}
 @Test public void ordinaryChordsWithoutThreeAreUnchanged(){byte[] a=new byte[W*H];Arrays.fill(a,(byte)255);one(a,121,117);assertEquals(notes(2),apply(a,2));}
 @Test public void sourceArraysAndEventsArePreserved(){var a=image();one(a,121,117);var saved=a.clone();var n=notes(2);var copy=List.copyOf(n);TripletRhythmDetector.apply(n,M,a,W,H);assertArrayEquals(saved,a);assertEquals(copy,n);}
}
