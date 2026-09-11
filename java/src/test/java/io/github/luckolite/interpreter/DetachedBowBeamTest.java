// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original beam and down-bow drawings separated by a real paper gap. */
public final class DetachedBowBeamTest {
 static class Page {
  int w=400,h=280;byte[]a=new byte[w*h],g=new byte[w*h];
  Page(int beams,int bowGap){Arrays.fill(g,(byte)255);for(int y=140;y<=204;y+=16)rect(20,y,360,1,4);for(int x:new int[]{120,264}){rect(x+10,104,2,85,1);for(int y=180;y<=196;y++)for(int xx=x-11;xx<=x+11;xx++)if((xx-x)*(xx-x)/121f+(y-188)*(y-188)/64f<=1)rect(xx,y,1,1,2);}for(int i=0;i<beams;i++)rect(130,104+i*12,146,6,5);if(bowGap>=0){int top=104-bowGap-19;rect(110,top,22,6,5);rect(110,top,3,19,5);rect(129,top,3,19,5);}}
  void rect(int x,int y,int w0,int h0,int v){for(int yy=y;yy<y+h0;yy++)for(int xx=x;xx<x+w0;xx++){a[yy*w+xx]=(byte)v;g[yy*w+xx]=0;}}
  List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.extract(a,g,w,h,List.of(new MeasureRegion(.05f,.95f,.3f,.85f))).stream().sorted(Comparator.comparingDouble(ScoreNoteEvent::positionInMeasure)).toList();}
 }
 @Test public void nearbyBowDoesNotHideThirdBeam(){var n=new Page(3,3).notes();assertEquals(2,n.size());assertEquals(3,n.get(0).beamCount());}
 @Test public void bowCapDoesNotBecomeAnExtraBeam(){assertEquals(1,new Page(1,3).notes().get(0).beamCount());}
 @Test public void twoBeamsKeepTheirCount(){assertEquals(2,new Page(2,3).notes().get(0).beamCount());}
 @Test public void ordinaryThreeBeamRunStaysUnchanged(){var n=new Page(3,-1).notes();assertTrue(n.stream().allMatch(x->x.beamCount()==3));}
 @Test public void wellSeparatedBowDoesNotAlterBeams(){assertEquals(3,new Page(3,8).notes().get(0).beamCount());}
 @Test public void notePitchesAndPositionsArePreserved(){var a=new Page(3,-1).notes();var b=new Page(3,3).notes();assertEquals(a.size(),b.size());for(int i=0;i<a.size();i++){assertEquals(a.get(i).staffStep(),b.get(i).staffStep());assertEquals(a.get(i).positionInMeasure(),b.get(i).positionInMeasure(),0);}}
 @Test public void sourceInkIsNotModified(){Page p=new Page(3,3);byte[]a=p.a.clone(),g=p.g.clone();p.notes();assertArrayEquals(a,p.a);assertArrayEquals(g,p.g);}
}
