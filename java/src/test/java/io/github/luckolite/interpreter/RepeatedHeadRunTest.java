// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original close-set ovals joined only by a thin semantic bridge. */
public final class RepeatedHeadRunTest {
 static class Page {
  int w=400,h=240;byte[] labels=new byte[w*h],gray=new byte[w*h];
  Page(){Arrays.fill(gray,(byte)255);for(int y=80;y<=144;y+=16)rect(20,y,360,1,4);}
  void rect(int x,int y,int a,int b,int label){for(int j=y;j<y+b;j++)for(int i=x;i<x+a;i++){labels[j*w+i]=(byte)label;gray[j*w+i]=0;}}
  void oval(int x){for(int y=128;y<=144;y++)for(int xx=x-11;xx<=x+11;xx++)if((xx-x)*(xx-x)/121f+(y-136)*(y-136)/64f<=1){labels[y*w+xx]=2;gray[y*w+xx]=0;}}
  List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.extract(labels,gray,w,h,List.of(new MeasureRegion(.05f,.95f,.15f,.85f)));}
 }
 static Page run(int n,boolean stems){Page p=new Page();for(int i=0;i<n;i++){int x=100+i*24;if(stems)p.rect(x+10,50,2,87,1);p.oval(x);if(i>0)for(int y=135;y<=137;y++)for(int xx=x-13;xx<=x-10;xx++)p.labels[y*p.w+xx]=2;}if(stems)for(int y=50;y<=70;y+=10)p.rect(110,y,(n-1)*24+2,5,5);return p;}
 @Test public void sevenBridgedOvalsBecomeSevenAttacks(){assertEquals(7,run(7,true).notes().size());}
 @Test public void recoveredHeadsKeepTheirPitch(){var n=run(7,true).notes();assertEquals(7,n.size());assertTrue(n.stream().allMatch(x->x.staffStep()==1));}
 @Test public void recoveredAttacksRemainSeparateAndBeamed(){var n=run(7,true).notes().stream().sorted(Comparator.comparingDouble(ScoreNoteEvent::positionInMeasure)).toList();assertEquals(7,n.size());for(int i=1;i<7;i++)assertTrue(n.get(i).positionInMeasure()>n.get(i-1).positionInMeasure()+.02);assertTrue(n.toString(),n.stream().allMatch(x->x.beamCount()==3));}
 @Test public void aSolidWideBlobIsNotSplit(){Page p=run(7,true);p.rect(89,128,167,17,2);assertTrue(p.notes().size()<7);}
 @Test public void aStemlessRowIsNotInvented(){assertTrue(run(7,false).notes().size()<7);}
 @Test public void ordinarySingleHeadIsUnchanged(){assertEquals(1,run(1,true).notes().size());}
 @Test public void decodingPreservesInputArrays(){Page p=run(7,true);byte[] a=p.labels.clone(),b=p.gray.clone();p.notes();assertArrayEquals(a,p.labels);assertArrayEquals(b,p.gray);}
}
