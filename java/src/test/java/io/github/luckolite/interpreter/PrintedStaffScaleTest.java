// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Original printed staffs with a compressed semantic spacing, including a severe alias. */
public class PrintedStaffScaleTest {
 static OmrScoreInterpreter.Analysis page(float slope,int predictedGap,boolean missingRule) {return page(slope,predictedGap,missingRule,false);}
 static OmrScoreInterpreter.Analysis page(float slope,int predictedGap,boolean missingRule,boolean twoAliases) {
  int w=2048,h=1000,gap=14;byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);List<MeasureRegion> measures=new ArrayList<>();
  for(int row=0;row<4;row++) {
   int top=120+row*210,bottom=top+4*gap;
   measures.add(new MeasureRegion(.08f,.95f,(top-45f)/h,(bottom+35f)/h));
   for(int x=180;x<1940;x++)for(int line=0;line<5;line++)for(int dy=0;dy<2;dy++) {
    int at=Math.round(top+line*gap+slope*(x-w*.5f)+dy)*w+x;
    if(!missingRule||row!=2||line!=4)gray[at]=0;if(row!=2)labels[at]=4;
   }
   if(row==2)for(int alias=0;alias<(twoAliases?2:1);alias++)for(int x=180;x<1940;x++)for(int line=0;line<5;line++)for(int dy=0;dy<2;dy++) {
    int semanticTop=top+(4*gap-4*predictedGap)/2+(twoAliases?alias*25-15:0);
    int at=Math.round(semanticTop+line*predictedGap+slope*(x-w*.5f)+dy)*w+x;labels[at]=4;
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
 private void check(float slope,int predictedGap) {
  var notes=page(slope,predictedGap,false).notes();assertEquals(36,notes.size());
  for(int row=0;row<4;row++) {
   final int index=row;var group=notes.stream().filter(n->n.measureIndex()==index).toList();assertEquals(9,group.size());
   for(int i=0;i<9;i++)assertEquals("row="+row+" note="+i,(i-2)*2,group.get(i).staffStep());
  }
 }
 @Test public void positiveTiltCompressedSpacing(){check(.0084f,12);}
 @Test public void negativeTiltCompressedSpacing(){check(-.0084f,12);}
 @Test public void positiveTiltSevereAlias(){check(.0084f,5);}
 @Test public void negativeTiltSevereAlias(){check(-.0084f,5);}
 @Test public void incompletePrintedGroupDoesNotAuthorizeRecovery(){assertEquals(27,page(.0084f,5,true).notes().size());}
 @Test public void twoAliasesDoNotInventASecondInstrument(){var notes=page(.0084f,5,false,true).notes();assertEquals(36,notes.size());for(var note:notes)assertEquals(1,note.staffCount());}
}
