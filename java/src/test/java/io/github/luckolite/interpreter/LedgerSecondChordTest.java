// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;import java.util.List;import org.junit.Test;import static org.junit.Assert.*;
/** Original overlapping seconds: each oval has ledger evidence around its own centre. */
public final class LedgerSecondChordTest {
 static final int W=480,H=320;
 static class Page {
  final byte[] labels=new byte[W*H],gray=new byte[W*H];
  Page(boolean complete) {
   Arrays.fill(gray,(byte)255);
   for(int y=128;y<=192;y+=16)rule(15,465,y,4);
   if(complete){rule(90,119,96,4);rule(90,140,112,4);}
   head(105,96);head(125,88);
   for(int y=96;y<=168;y++){pixel(115,y,1);pixel(116,y,1);}
   // A narrow semantic neck joins the adjacent heads, as a shared chord component.
   for(int y=91;y<=96;y++)for(int x=113;x<=117;x++)pixel(x,y,2);
   head(320,176);for(int y=128;y<=176;y++)pixel(330,y,1);
  }
  void pixel(int x,int y,int kind){labels[y*W+x]=(byte)kind;gray[y*W+x]=0;}
  void rule(int a,int b,int y,int kind){for(int x=a;x<=b;x++)pixel(x,y,kind);}
  void head(int cx,int cy){for(int y=cy-9;y<=cy+9;y++)for(int x=cx-11;x<=cx+11;x++)if(Math.pow((x-cx)/11d,2)+Math.pow((y-cy)/9d,2)<=1)pixel(x,y,2);}
  List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.analyze(labels,gray,W,H,List.of(new MeasureRegion(0,1,.1f,.9f))).notes();}
  List<ScoreNoteEvent> chord(){return notes().stream().filter(n->n.positionInMeasure()*W<150).toList();}
 }
 @Test public void adjacentLedgerChordRetainsBothTones(){var p=new Page(true);assertEquals(2,p.chord().size());}
 @Test public void eachToneKeepsItsPrintedPitch(){var p=new Page(true);assertEquals(List.of(12,13),p.chord().stream().map(ScoreNoteEvent::staffStep).sorted().toList());}
 @Test public void chordSharesOneAttack(){var p=new Page(true);var c=p.chord();assertEquals(2,c.size());assertEquals(c.get(0).positionInMeasure(),c.get(1).positionInMeasure(),.00001);}
 @Test public void missingLedgerEvidenceStillRejectsArtifact(){assertEquals(0,new Page(false).chord().size());}
 @Test public void ordinaryStaffNoteIsPreserved(){var p=new Page(true);assertEquals(1,p.notes().stream().filter(n->n.positionInMeasure()*W>250).count());}
 @Test public void inputPixelsArePreserved(){var p=new Page(true);var l=p.labels.clone();var g=p.gray.clone();p.notes();assertArrayEquals(l,p.labels);assertArrayEquals(g,p.gray);}
}
