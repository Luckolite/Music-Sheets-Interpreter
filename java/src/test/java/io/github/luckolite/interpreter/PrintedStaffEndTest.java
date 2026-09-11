// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original staff endings with a note inside the percentile-trimmed margin. */
public class PrintedStaffEndTest {
 private void check(float slope,boolean faded,boolean stray) {
  int w=2048,h=320,top=110,gap=14,end=1900;byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);
  for(int x=100;x<=end;x++)for(int line=0;line<5;line++)for(int dy=0;dy<2;dy++) {
   int at=Math.round(top+line*gap+slope*(x-w*.5f)+dy)*w+x;gray[at]=0;
   if(!faded||x<end-35)labels[at]=4;
  }
  // A faint closing stroke is visible but absent from the model's barline class.
  for(int y=top;y<=top+4*gap;y++)gray[Math.round(y+slope*(end-w*.5f))*w+end]=(byte)190;
  if(stray)for(int x=1940;x<2040;x++)for(int line=0;line<3;line++)gray[(top+line*gap)*w+x]=0;
  for(int cx:new int[]{400,1883}) {
   int cy=top;
   for(int y=cy-6;y<=cy+6;y++)for(int x=cx-10;x<=cx+10;x++)if(Math.pow((x-cx)/10.,2)+Math.pow((y-cy)/6.,2)<=1) {
    int at=Math.round(y+slope*(x-w*.5f))*w+x;gray[at]=0;labels[at]=2;
   }
   for(int y=cy;y<=cy+3*gap;y++){int at=Math.round(y+slope*(cx-9-w*.5f))*w+cx-9;gray[at]=0;labels[at]=1;}
  }
  var measures=OmrMeasurePostProcessor.process(labels,gray,w,h);
  var notes=OmrScoreInterpreter.extract(labels,gray,w,h,measures);
  assertEquals("measures="+measures+" notes="+notes,2,notes.size());for(var note:notes)assertEquals(8,note.staffStep());
  assertTrue(measures.get(measures.size()-1).right()*w<end+5);
 }
 @Test public void closingNoteSurvivesPercentileTrimming(){check(0,false,false);}
 @Test public void fadedSemanticEndingUsesPrintedRules(){check(0,true,false);}
 @Test public void tiltedEndingRetainsTheLastNote(){check(.006f,true,false);}
 @Test public void detachedShortRulesDoNotExtendTheStaff(){check(0,true,true);}
 @Test public void courtesyOnlyTailDoesNotCreateAnotherMeasure(){
  int w=2200,h=300;byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);
  for(int x=100;x<=2000;x++)for(int line=0;line<5;line++)for(int dy=0;dy<2;dy++){int at=(100+line*10+dy)*w+x;gray[at]=0;labels[at]=4;}
  for(int y=100;y<=141;y++)for(int x=1967;x<=1969;x++){gray[y*w+x]=0;labels[y*w+x]=1;}
  for(int y=95;y<=121;y++)for(int x=1980;x<=1982;x++){gray[y*w+x]=0;labels[y*w+x]=3;}
  for(int y=112;y<=121;y++)for(int x=1983;x<=1987;x++){gray[y*w+x]=0;labels[y*w+x]=3;}
  assertEquals(1,OmrMeasurePostProcessor.process(labels,gray,w,h).size());
 }
 @Test public void isolatedHeadNoiseDoesNotExtendTheStaff(){
  int w=2048,h=300;byte[] gray=new byte[w*h],labels=new byte[w*h];Arrays.fill(gray,(byte)255);
  for(int x=100;x<=1900;x++)for(int line=0;line<5;line++){gray[(100+line*14)*w+x]=0;labels[(100+line*14)*w+x]=4;}
  labels[100*w+1888]=2;labels[101*w+1888]=2;
  var measures=OmrMeasurePostProcessor.process(labels,gray,w,h);
  assertEquals(1,measures.size());assertTrue(measures.get(0).right()*w<1885);
 }
}
