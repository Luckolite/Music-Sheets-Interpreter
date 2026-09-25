// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.ArrayList;
/** Three independent aligned cores across a slightly wider full-size beam pair. */
final class WideTripleBeamInk {
 private WideTripleBeamInk() { }
 static int count(byte[] gray,int w,int h,int[] a,int[] b,float gap) {
  if(gray==null||w<3||h<3||gray.length!=(long)w*h||!Float.isFinite(gap)||gap<8||a==null||b==null||a.length<3||b.length<3
    ||Math.abs(a[2])!=1||a[2]!=b[2]||a[0]<1||a[0]>=w-1||b[0]<1||b[0]>=w-1||a[1]<1||a[1]>=h-1||b[1]<1||b[1]>=h-1)return 0;
  int span=Math.abs(a[0]-b[0]);if(span<gap*3||span>gap*4||Math.abs(a[1]-b[1])>gap*.75f)return 0;
  for(float fraction:new float[]{.5f,.75f}) {float[] previous=null;boolean okay=true;
   for(float f:new float[]{.25f,.5f,.75f}) {
    int x=Math.round(a[0]+(b[0]-a[0])*f);float end=a[1]+(b[1]-a[1])*f;
    int top=Math.max(1,Math.round(end-(a[2]<0?.35f:2.4f)*gap)),bottom=Math.min(h-2,Math.round(end+(a[2]<0?2.4f:.35f)*gap));
    int threshold=BeamInkThreshold.at(gray,w,h,x,top,bottom,gap),darkest=threshold;
    for(int y=top;y<=bottom;y++)darkest=Math.min(darkest,gray[y*w+x]&255);
    threshold=darkest+Math.round((threshold-darkest)*fraction);
    var cores=new ArrayList<Float>();int start=-1;
    for(int y=top;y<=bottom+1;y++) {
     boolean ink=y<=bottom&&(gray[y*w+x-1]&255)<threshold&&(gray[y*w+x]&255)<threshold&&(gray[y*w+x+1]&255)<threshold;
     if(ink&&start<0)start=y;
     if(!ink&&start>=0){int size=y-start;if(size>=Math.max(3,(int)Math.ceil(gap*.3f))&&size<=gap*.8f)cores.add((start+y-1)*.5f-end);start=-1;}
    }
    if(cores.size()!=3){okay=false;break;}float[] current=new float[3];
    for(int i=0;i<3;i++){current[i]=cores.get(i);if(i>0&&(current[i]-current[i-1]<gap*.3f||current[i]-current[i-1]>gap*.9f))okay=false;if(previous!=null&&Math.abs(current[i]-previous[i])>gap*.22f)okay=false;}
    previous=current;
   }
   if(okay)return 3;
  }
  return 0;
 }
}
