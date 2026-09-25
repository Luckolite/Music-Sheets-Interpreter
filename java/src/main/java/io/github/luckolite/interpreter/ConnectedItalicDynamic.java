// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Owns a tiny false oval inside a connected italic dynamic word around a long f. */
final class ConnectedItalicDynamic {
    private ConnectedItalicDynamic() { }
    static boolean matches(byte[] gray,int width,int height,int headLeft,int headTop,int headRight,int headBottom,float gap) {
        if(gray==null||gap<4||headRight-headLeft+1>gap*1.05f||headBottom-headTop+1>gap)return false;
        int left=Math.max(0,Math.round(headLeft-gap)),right=Math.min(width-1,Math.round(headRight+gap*3.4f));
        int top=Math.max(0,Math.round(headTop-gap*1.6f)),bottom=Math.min(height-1,Math.round(headBottom+gap*1.5f));
        int w=right-left+1,h=bottom-top+1,threshold=BeamInkThreshold.at(gray,width,height,(headLeft+headRight)/2,top,bottom,gap)+5;
        boolean[] ink=new boolean[w*h];int[] queue=new int[w*h];int read=0,size=0;
        for(int y=headTop;y<=headBottom;y++)for(int x=headLeft;x<=headRight;x++) {
            int at=(y-top)*w+x-left;
            if((gray[y*width+x]&255)<threshold&&!ink[at]){ink[at]=true;queue[size++]=at;}
        }
        int minX=w,maxX=-1,minY=h,maxY=-1;
        while(read<size) {
            int at=queue[read++],x=at%w,y=at/w;
            if(x==0||x==w-1||y==0||y==h-1)return false;
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                int xx=x+dx,yy=y+dy,next=yy*w+xx;
                if(!ink[next]&&(gray[(top+yy)*width+left+xx]&255)<threshold){ink[next]=true;queue[size++]=next;}
            }
        }
        int gh=maxY-minY+1,gw=maxX-minX+1;
        if(gh<gap*2||gh>gap*3.5f||gw<gap*2.2f||gw>gap*4.2f||size<gw*gh*.18f||size>gw*gh*.7f)return false;
        float hx=(headLeft+headRight)*.5f-left,hy=(headTop+headBottom)*.5f-top;
        if(hy<minY+gh*.25f||hy>minY+gh*.78f)return false;
        float upper=capCenter(ink,w,minX,maxX,minY,minY+Math.round(gh*.22f));
        float lower=capCenter(ink,w,minX,maxX,minY+Math.round(gh*.80f),maxY);
        if(!Float.isFinite(upper)||!Float.isFinite(lower)||upper-lower<gap*.7f
                ||upper-hx<gap*.7f||maxX-upper<gap*.55f||Math.abs(lower-hx)>gap*1.1f)return false;
        int support=0,total=0;
        for(int y=minY+Math.round(gh*.22f);y<=minY+Math.round(gh*.80f);y++) {
            float f=(y-minY)/(float)Math.max(1,gh-1),cx=upper+(lower-upper)*f;
            boolean found=false;
            for(int x=Math.max(minX,Math.round(cx-gap*.28f));x<=Math.min(maxX,Math.round(cx+gap*.28f));x++)found|=ink[y*w+x];
            total++;if(found)support++;
        }
        return support>=total*.85f;
    }
    private static float capCenter(boolean[] p,int width,int left,int right,int top,int bottom) {
        int sum=0,count=0;for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++)if(p[y*width+x]){sum+=x;count++;}
        return count==0?Float.NaN:sum/(float)count;
    }
}
