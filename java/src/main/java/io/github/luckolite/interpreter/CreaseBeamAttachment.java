// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
/** Two surviving beam caps and erased staff rules can reconnect a crease-clipped oval to its own stem. */
final class CreaseBeamAttachment {
 private CreaseBeamAttachment(){}
 static int count(byte[] g,int w,int h,float gap,float staffTop,float staffBottom,
         int left,int right,int top,int bottom,float centerY){
  if(g==null||w<=0||h<=0||g.length!=(long)w*h||!Float.isFinite(gap+staffTop+staffBottom+centerY)
    ||gap<8||staffTop<0||staffBottom>=h||staffTop>=staffBottom||left<0||right>=w||top<0||bottom>=h
    ||left>right||top>bottom||centerY<top||centerY>bottom)return 0;
  int hw=right-left+1,hh=bottom-top+1;
  if(hw<gap*.7f||hw>gap*1.1f||hh<gap*.75f||hh>gap*1.3f)return 0;
  for(int stem=right+2;stem<=Math.min(w-2,Math.round(right+gap*.85f));stem++){
   int start=Math.round(centerY-gap*.3f),end=start,blank=0;
   for(int y=start;y>=Math.max(1,Math.round(centerY-gap*6));y--){
    if((g[y*w+stem]&255)<230){end=y;blank=0;}else if(++blank>Math.max(1,Math.round(gap*.14f)))break;
   }
   if(centerY-end<gap*2.5f||centerY-end>gap*5.8f)continue;
   int samples=0,contrast=0,flank=Math.max(3,Math.round(gap*.3f));
   if(stem+flank>=w)continue;
   for(int y=end+Math.round(gap*1.7f);y<start;y++){
    float rule=staffBottom+Math.round((y-staffBottom)/gap)*gap;if(Math.abs(y-rule)<gap*.22f)continue;
    samples++;int ink=g[y*w+stem]&255;
    if(ink<205&&(g[y*w+stem-flank]&255)>=ink+12&&(g[y*w+stem+flank]&255)>=ink+12)contrast++;
   }
   if(samples<gap*.6f||contrast<samples*.7f)continue;
   List<float[]> caps=new ArrayList<>();
   for(int x=stem-4;x<=stem-2;x++){
    if(x<=right)continue;List<float[]> bands=bands(g,w,h,x,end-1,Math.round(end+gap*1.6f),gap);
    if(bands.size()==2){float[] a=bands.get(0),b=bands.get(1);float distance=(b[0]+b[1]-a[0]-a[1])*.5f;
     if(distance>=gap*.35f&&distance<=gap*1.05f)caps.add(new float[]{(a[0]+a[1])*.5f,(b[0]+b[1])*.5f});}
   }
   if(caps.isEmpty())continue;
   boolean matched=false;
   for(float[] cap:caps){boolean pair=true;for(float cy:cap){int good=0,total=0;for(int x=left-Math.round(gap*.3f);x<=left+Math.round(gap*.2f);x++){
     if(x<0){pair=false;break;}total++;int ink=0;for(int y=Math.round(cy)-1;y<=Math.round(cy)+1;y++)if(y>=0&&y<h&&(g[y*w+x]&255)<170)ink++;
     if(ink==3)good++;}if(total<3||good<total*.8f)pair=false;}if(pair){matched=true;break;}}
   if(!matched)continue;
   int erased=0;
   for(int row=0;row<5;row++){
    int y=Math.round(staffTop+gap*row),lx=Math.max(0,left-Math.round(gap*.4f)),rx=stem+flank;
    if(!inkNear(g,w,h,lx,y,205)||!inkNear(g,w,h,rx,y,205))continue;
    int longest=0,run=0;
    for(int x=Math.max(left,right-Math.round(gap*.5f));x<stem;x++){
     boolean white=true;for(int dy=-1;dy<=1;dy++)if(y+dy<0||y+dy>=h||(g[(y+dy)*w+x]&255)<240)white=false;
     run=white?run+1:0;longest=Math.max(longest,run);
    }
    if(longest>=Math.max(2,Math.round(gap*.12f))&&longest<=gap*.85f)erased++;
   }
   if(erased>=3)return 2;
  }
  return 0;
 }
 private static boolean inkNear(byte[]g,int w,int h,int x,int y,int threshold){for(int dy=-1;dy<=1;dy++)if(y+dy>=0&&y+dy<h&&(g[(y+dy)*w+x]&255)<threshold)return true;return false;}
 private static List<float[]> bands(byte[]g,int w,int h,int x,int top,int bottom,float gap){List<float[]> out=new ArrayList<>();int start=-1;top=Math.max(0,top);bottom=Math.min(h-1,bottom);for(int y=top;y<=bottom+1;y++){boolean ink=y<=bottom&&(g[y*w+x]&255)<170;if(ink&&start<0)start=y;if(!ink&&start>=0){int size=y-start;if(size>=Math.max(3,Math.round(gap*.22f))&&size<=gap*.7f)out.add(new float[]{start,y-1});start=-1;}}return out;}
}
