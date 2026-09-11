// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original printed staffs with a missing outer semantic rule and short ledger fragments. */
public class PrintedStaffPhaseTest {
 static OmrScoreInterpreter.Analysis page(float slope,int phase) {
  int w=2048,h=1000,gap=14;byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);List<MeasureRegion> measures=new ArrayList<>();
  for(int row=0;row<4;row++) {
   int top=120+row*210,bottom=top+4*gap;
   measures.add(new MeasureRegion(.08f,.95f,(top-45f)/h,(bottom+35f)/h));
   for(int x=180;x<1940;x++)for(int line=0;line<5;line++)for(int dy=0;dy<2;dy++) {
    int at=Math.round(top+line*gap+slope*(x-w*.5f)+dy)*w+x;
    gray[at]=0;labels[at]=(byte)(row==2&&line==(phase<0?4:0)?5:4);
   }
   if(row==2)for(int x=210;x<1900;x++)if(x%100<70)for(int dy=0;dy<2;dy++) {
    int at=Math.round((phase<0?top-gap:bottom+gap)+slope*(x-w*.5f)+dy)*w+x;
    gray[at]=0;labels[at]=4;
   }
   for(int i=0;i<3;i++) {
    int cx=500+i*500,cy=bottom-(i+1)*gap;
    for(int y=cy-6;y<=cy+6;y++)for(int x=cx-10;x<=cx+10;x++)if(Math.pow((x-cx)/10.,2)+Math.pow((y-cy)/6.,2)<=1) {
     int at=Math.round(y+slope*(x-w*.5f))*w+x;gray[at]=0;labels[at]=2;
    }
    for(int y=cy-3*gap;y<=cy;y++){int at=Math.round(y+slope*(cx+10-w*.5f))*w+cx+10;gray[at]=0;labels[at]=1;}
   }
  }
  return OmrScoreInterpreter.analyze(labels,gray,w,h,measures);
 }
 private void check(float slope,int phase) {
  var notes=page(slope,phase).notes();assertEquals(12,notes.size());
  for(int row=0;row<4;row++) {
   final int index=row;var group=notes.stream().filter(n->n.measureIndex()==index).toList();assertEquals(3,group.size());
   for(int i=0;i<3;i++)assertEquals("row="+row+" note="+i,(i+1)*2,group.get(i).staffStep());
  }
 }
 @Test public void positiveTiltMissingBottomRule(){check(.0084f,-1);}
 @Test public void negativeTiltMissingBottomRule(){check(-.0084f,-1);}
 @Test public void positiveTiltMissingTopRule(){check(.0084f,1);}
 @Test public void negativeTiltMissingTopRule(){check(-.0084f,1);}
}
