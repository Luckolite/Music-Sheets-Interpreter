// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original tilted pages with incomplete staff labels and known pitches. */
public class SparseDeskewedStaffTest {
 static OmrScoreInterpreter.Analysis page(float slope,int stride,int phase) {
  int w=2048,h=1000,gap=14;byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);List<MeasureRegion> measures=new ArrayList<>();
  for(int row=0;row<4;row++) {
   int top=120+row*210,bottom=top+4*gap;
   measures.add(new MeasureRegion(.08f,.95f,(top-25f)/h,(bottom+25f)/h));
   for(int x=180;x<1940;x++)for(int line=0;line<5;line++)for(int dy=0;dy<2;dy++) {
    int y=Math.round(top+line*gap+slope*(x-w*.5f))+dy;
    gray[y*w+x]=0;labels[y*w+x]=(byte)((x/stride+line*phase+row)%4==0?4:5);
   }
   for(int i=0;i<3;i++) {
    int cx=500+i*500,cy=bottom-(i+1)*gap;
    for(int y=cy-7;y<=cy+7;y++)for(int x=cx-10;x<=cx+10;x++)if(Math.pow((x-cx)/10.,2)+Math.pow((y-cy)/7.,2)<=1) {
     int at=Math.round(y+slope*(x-w*.5f))*w+x;gray[at]=0;labels[at]=2;
    }
    for(int y=cy-3*gap;y<=cy;y++){int at=Math.round(y+slope*(cx+10-w*.5f))*w+cx+10;gray[at]=0;labels[at]=1;}
   }
  }
  return OmrScoreInterpreter.analyze(labels,gray,w,h,measures);
 }
 private void check(float slope,int stride,int phase) {
  var notes=page(slope,stride,phase).notes();assertEquals(12,notes.size());
  for(int row=0;row<4;row++) {
   final int index=row;var group=notes.stream().filter(n->n.measureIndex()==index).toList();assertEquals(3,group.size());
   for(int i=0;i<3;i++)assertEquals((i+1)*2,group.get(i).staffStep());
  }
 }
 @Test public void sparsePositiveTiltKeepsEverySystem(){for(float slope:new float[]{.0084f,.012f})for(int phase:new int[]{0,1})check(slope,80,phase);}
 @Test public void sparseNegativeTiltKeepsEverySystem(){for(float slope:new float[]{-.0084f,-.012f})for(int phase:new int[]{0,1})check(slope,80,phase);}
 @Test public void positiveTiltCannotHalvePitchSpacing(){for(float slope:new float[]{.0084f,.012f})for(int stride:new int[]{160,320})for(int phase:new int[]{0,1})check(slope,stride,phase);}
 @Test public void negativeTiltCannotHalvePitchSpacing(){for(float slope:new float[]{-.0084f,-.012f})for(int stride:new int[]{160,320})for(int phase:new int[]{0,1})check(slope,stride,phase);}
 private boolean rawEvidence(int count,boolean extra,boolean missing)throws Exception {
  int w=800,h=240;byte[] gray=new byte[w*h];Arrays.fill(gray,(byte)255);float slope=.01f;int[] rows={80,94,108,122,136};
  for(int line=0;line<count;line++)for(int x=30;x<w-30;x++) {
   if(missing&&line==4)continue;
   int y=Math.round(rows[line]+slope*(x-w*.5f));gray[y*w+x]=0;
   if(extra&&line<4)gray[(y+7)*w+x]=0;
  }
  var method=OmrScoreInterpreter.class.getDeclaredMethod("completePrintedDeskewedStaff",byte[].class,int.class,int.class,RawStaffLineDetector.StaffLines.class,float.class);
  method.setAccessible(true);return (boolean)method.invoke(null,gray,w,h,new RawStaffLineDetector.StaffLines(rows,14f),slope);
 }
 @Test public void completeRawRulesSupportSparseSemanticPaint()throws Exception{assertTrue(rawEvidence(5,false,false));}
 @Test public void fourRulesCannotEstablishAStaff()throws Exception{assertFalse(rawEvidence(5,false,true));}
 @Test public void interveningRulesCannotDoubleARealSmallStaff()throws Exception{assertFalse(rawEvidence(5,true,false));}
 @Test public void blankPrintedRegionCannotEstablishAStaff()throws Exception{assertFalse(rawEvidence(0,false,false));}
}
