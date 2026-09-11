// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original touching eighth and dotted-half ovals on a staff with fractional spacing. */
public class FractionalUnisonWidthTest {
 static final int W=440,H=260;
 static class Page {
  final byte[] labels=new byte[W*H],gray=new byte[W*H];
  Page(boolean held,boolean stems){
   Arrays.fill(gray,(byte)255);
   for(int y:new int[]{80,94,107,120,134})rect(20,y,400,1,4);
   if(stems){rect(189,73,2,48,1);rect(190,120,2,56,1);rect(189,73,73,4,5);rect(259,73,2,34,1);}
   oval(180,120,false);oval(201,120,held);if(stems)oval(250,106,false);rect(221,112,4,4,5);
  }
  void rect(int x,int y,int w,int h,int label){for(int yy=y;yy<y+h;yy++)for(int xx=x;xx<x+w;xx++){labels[yy*W+xx]=(byte)label;gray[yy*W+xx]=0;}}
  void oval(int x,int y,boolean hollow){for(int yy=y-8;yy<=y+8;yy++)for(int xx=x-10;xx<=x+10;xx++){double d=Math.pow((xx-x)/10.,2)+Math.pow((yy-y)/8.,2);if(d<=1){labels[yy*W+xx]=2;gray[yy*W+xx]=(byte)(hollow&&d<.42?255:0);}}}
  List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.extract(labels,gray,W,H,List.of(new MeasureRegion(.03f,.97f,.15f,.8f)));}
 }
 @Test public void fractionalStaffKeepsShortAndHeldVoices(){
  var notes=new Page(true,true).notes().stream().filter(n->n.staffStep()==2).toList();assertEquals(2,notes.size());assertTrue(notes.stream().allMatch(n->n.staffStep()==2));
  var held=notes.stream().filter(n->n.unbeamedDurationBeats()==2).findFirst().orElseThrow();assertEquals(1,held.augmentationDots());
  var moving=notes.stream().filter(n->n.beamCount()>0).findFirst().orElseThrow();assertEquals(1,moving.beamCount());assertEquals(0,moving.augmentationDots());
 }
 @Test public void filledNeighborDoesNotInventHeldDuration(){assertTrue(new Page(false,true).notes().stream().noneMatch(n->n.unbeamedDurationBeats()==2));}
 @Test public void preservingVoicesDoesNotModifyCallerPixels(){var p=new Page(true,true);byte[] a=p.labels.clone(),b=p.gray.clone();p.notes();assertArrayEquals(a,p.labels);assertArrayEquals(b,p.gray);}
}
