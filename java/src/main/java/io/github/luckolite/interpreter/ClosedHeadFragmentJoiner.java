// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

/** Verifies that two small semantic islands belong to one closed printed notehead. */
final class ClosedHeadFragmentJoiner {
    private ClosedHeadFragmentJoiner() { }
    record Box(int left,int top,int right,int bottom,float x,float y,boolean filled) {
        Box(int left,int top,int right,int bottom,float x,float y){this(left,top,right,bottom,x,y,false);}
    }
    static Box join(byte[] gray,int width,int height,float gap,Box a,Box b) {
        if(gray==null||gray.length!=(long)width*height||gap<6||!Float.isFinite(gap))return null;
        if(Math.abs(a.x-b.x)>gap*.95f||Math.abs(a.y-b.y)>gap*.35f
                ||a.right-a.left+1>gap*.85f||b.right-b.left+1>gap*.85f
                ||a.bottom-a.top+1>gap*.95f||b.bottom-b.top+1>gap*.95f)return null;
        int unionLeft=Math.min(a.left,b.left),unionRight=Math.max(a.right,b.right);
        int unionTop=Math.min(a.top,b.top),unionBottom=Math.max(a.bottom,b.bottom);
        if(unionRight-unionLeft+1<gap*.95f||unionRight-unionLeft+1>gap*2.1f
                ||unionBottom-unionTop+1<gap*.45f||unionBottom-unionTop+1>gap*1.15f)return null;
        int left=Math.max(0,unionLeft-Math.round(gap*.5f)),right=Math.min(width-1,unionRight+Math.round(gap*.5f));
        int top=Math.max(0,unionTop-Math.round(gap*.5f)),bottom=Math.min(height-1,unionBottom+Math.round(gap*.5f));
        int w=right-left+1,h=bottom-top+1,size=w*h;boolean[] seen=new boolean[size];int[] queue=new int[size];
        for(int seed=0;seed<size;seed++) {
            if(seen[seed]||(gray[(top+seed/w)*width+left+seed%w]&255)<=150)continue;
            int read=0,end=1;queue[0]=seed;seen[seed]=true;int minX=w,maxX=0,minY=h,maxY=0;
            while(read<end) {
                int p=queue[read++],x=p%w,y=p/w;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                for(int d=0;d<4;d++) {
                    int nx=x+(d==0?-1:d==1?1:0),ny=y+(d==2?-1:d==3?1:0);
                    if(nx<0||nx>=w||ny<0||ny>=h)continue;
                    int next=ny*w+nx;if(!seen[next]&&(gray[(top+ny)*width+left+nx]&255)>150){seen[next]=true;queue[end++]=next;}
                }
            }
            int pw=maxX-minX+1,ph=maxY-minY+1;
            if(minX==0||maxX==w-1||minY==0||maxY==h-1||pw<gap*.45f||pw>gap*1.65f
                    ||ph<gap*.4f||ph>gap*1.05f||end<pw*ph*.45f)continue;
            float cx=left+(minX+maxX)*.5f,cy=top+(minY+maxY)*.5f;
            if(Math.abs(cx-a.x)>gap*.75f||Math.abs(cx-b.x)>gap*.75f
                    ||Math.abs(cy-a.y)>gap*.35f||Math.abs(cy-b.y)>gap*.35f)continue;
            // Both islands must meet the same pocket, not two adjacent independent heads.
            if(a.right<left+minX-gap*.2f||a.left>left+maxX+gap*.2f
                    ||b.right<left+minX-gap*.2f||b.left>left+maxX+gap*.2f)continue;
            int[] shades=new int[end];
            for(int i=0;i<end;i++)shades[i]=gray[(top+queue[i]/w)*width+left+queue[i]%w]&255;
            java.util.Arrays.sort(shades);
            return new Box(Math.min(unionLeft,left+minX-2),Math.min(unionTop,top+minY-2),
                    Math.max(unionRight,left+maxX+2),Math.max(unionBottom,top+maxY+2),cx,cy,shades[end/2]<=205);
        }
        return null;
    }
}
