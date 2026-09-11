// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original short tie drawings and independent sustained-note counterexamples. */
public final class ShortTieBowlTest {
 static class Page {
  int w=320,h=240;byte[]a=new byte[w*h],g=new byte[w*h];
  Page(){Arrays.fill(g,(byte)255);for(int y=100;y<=164;y+=16)rect(20,y,280,1,4);oval(100,140,11,8,false,true);oval(150,140,11,8,false,true);}
  void rect(int x,int y,int ww,int hh,int label){for(int yy=y;yy<y+hh;yy++)for(int xx=x;xx<x+ww;xx++){a[yy*w+xx]=(byte)label;g[yy*w+xx]=0;}}
  void oval(int x,int y,int rx,int ry,boolean hollow,boolean stem){if(stem)rect(x+rx-1,y-50,2,50,1);for(int yy=y-ry;yy<=y+ry;yy++)for(int xx=x-rx;xx<=x+rx;xx++){float d=(xx-x)*(xx-x)/(float)(rx*rx)+(yy-y)*(yy-y)/(float)(ry*ry);if(d<=1&&(!hollow||d>=.45f))rect(xx,yy,1,1,2);}}
  void tie(boolean corrupt){for(int x=112;x<=138;x++){float t=(x-112)/26f;int cy=Math.round(146+10*4*t*(1-t));for(int dy=-1;dy<=1;dy++)rect(x,cy+dy,1,1,5);}if(corrupt)for(int y=150;y<=157;y++)for(int x=118;x<=133;x++)a[y*w+x]=2;}
  List<ScoreNoteEvent> notes(){return OmrScoreInterpreter.extract(a,g,w,h,List.of(new MeasureRegion(.05f,.95f,.3f,.85f))).stream().sorted(Comparator.comparingDouble(ScoreNoteEvent::positionInMeasure)).toList();}
 }
 @Test public void clippedTieBowlDoesNotSoundAsThirdNote(){Page p=new Page();p.tie(true);assertEquals(2,p.notes().size());}
 @Test public void corruptedTieStillJoinsItsPitchedEndpoints(){Page p=new Page();p.tie(true);assertTrue(p.notes().get(p.notes().size()-1).tiedFromPrevious());}
 @Test public void cleanTieDoesNotLoseEitherEndpoint(){Page p=new Page();p.tie(false);assertEquals(2,p.notes().size());}
 @Test public void upperTieBowlIsAlsoExcluded(){Page p=new Page();p.tie(true);byte[]a=p.a.clone(),g=p.g.clone();for(int y=0;y<p.h;y++)for(int x=0;x<p.w;x++){p.a[y*p.w+x]=a[(p.h-1-y)*p.w+p.w-1-x];p.g[y*p.w+x]=g[(p.h-1-y)*p.w+p.w-1-x];}assertEquals(2,p.notes().size());}
 @Test public void independentHollowNoteBetweenHeadsIsRetained(){Page p=new Page();p.oval(125,156,10,6,true,false);assertEquals(3,p.notes().size());}
 @Test public void independentFilledNoteBetweenHeadsIsRetained(){Page p=new Page();p.oval(125,156,10,6,false,false);assertEquals(3,p.notes().size());}
 @Test public void attachedSmallHeadBetweenHeadsIsRetained(){Page p=new Page();p.oval(125,156,7,5,false,true);assertEquals(3,p.notes().size());}
 @Test public void unrelatedArcCannotRemoveASustainedNote(){Page p=new Page();p.oval(125,156,10,6,true,false);for(int x=112;x<=138;x++){float t=(x-112)/26f;int y=Math.round(132-8*4*t*(1-t));p.rect(x,y,1,2,5);}assertEquals(3,p.notes().size());}
 @Test public void sourceArraysArePreserved(){Page p=new Page();p.tie(true);byte[]a=p.a.clone(),g=p.g.clone();p.notes();assertArrayEquals(a,p.a);assertArrayEquals(g,p.g);}
}
