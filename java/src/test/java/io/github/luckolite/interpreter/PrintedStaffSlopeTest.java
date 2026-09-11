// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original tilted rules with a partial semantic mask and a faded ending. */
public class PrintedStaffSlopeTest {
 static OmrScoreInterpreter.Analysis page(float slope,int seedOffset,boolean faded) {
  int w=2048,h=1000,gap=14;byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);List<MeasureRegion> measures=new ArrayList<>();
  for(int row=0;row<4;row++) {
   int top=120+row*210,bottom=top+4*gap;
   measures.add(new MeasureRegion(.08f,.95f,(top-45f)/h,(bottom+35f)/h));
   for(int x=180;x<1940;x++)for(int line=0;line<5;line++)for(int dy=0;dy<2;dy++) {
    int at=Math.round(top+line*gap+slope*(x-w*.5f)+dy)*w+x;
    if(!faded||row!=2||x<1400)gray[at]=0;if(row!=2)labels[at]=4;
   }
   if(row==2)for(int x=180;x<1940;x++)for(int line=0;line<5;line++)for(int dy=0;dy<2;dy++) {
    int semanticTop=top+seedOffset;
    int at=Math.round(semanticTop+line*gap+slope*(x-w*.5f)+dy)*w+x;if(x<1100)labels[at]=4;
   }
   int[] steps={-2,-1,0,1,2,3,4,5,6};
   for(int i=0;i<steps.length;i++) {
    int cx=200+i*210,cy=bottom-steps[i]*gap;
    for(int step=-2;step<=6;step++)if((step<0&&step>=steps[i])||(step>4&&step<=steps[i]))
     for(int x=cx-15;x<=cx+15;x++) {
      int at=Math.round(bottom-step*gap+slope*(x-w*.5f))*w+x;gray[at]=0;labels[at]=4;
     }
    for(int y=cy-6;y<=cy+6;y++)for(int x=cx-10;x<=cx+10;x++)if(Math.pow((x-cx)/10.,2)+Math.pow((y-cy)/6.,2)<=1) {
     int at=Math.round(y+slope*(x-w*.5f))*w+x;gray[at]=0;labels[at]=2;
    }
    for(int y=cy-3*gap;y<=cy;y++){int at=Math.round(y+slope*(cx+10-w*.5f))*w+cx+10;gray[at]=0;labels[at]=1;}
   }
  }
  return OmrScoreInterpreter.analyze(labels,gray,w,h,measures);
 }
 private void check(float slope,int seedOffset,boolean faded) {
  var notes=page(slope,seedOffset,faded).notes();assertEquals(36,notes.size());
  for(int row=0;row<4;row++) {
   final int index=row;var group=notes.stream().filter(n->n.measureIndex()==index).toList();assertEquals(9,group.size());
   for(int i=0;i<9;i++)assertEquals("row="+row+" note="+i,(i-2)*2,group.get(i).staffStep());
  }
 }
 @Test public void upwardTiltWithLeftBiasedSeed(){check(.0084f,-5,true);}
 @Test public void completeStaffWithRightBiasedSeed(){check(-.0084f,5,false);}
 @Test public void upwardTiltWithCenteredSeed(){check(.0084f,0,true);}
 @Test public void downwardTiltWithCenteredSeed(){check(-.0084f,0,true);}
}
