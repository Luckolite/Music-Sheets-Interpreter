// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;

/** Recovers a reduced beamed pair split by an independently visible white scan crease. */
final class CreaseGracePairRecovery {
    record Box(int left,int top,int right,int bottom,float x,float y) {}
    record Pair(Box recovered,int firstStem,int firstEnd,int secondStem,int secondEnd,int beams) {}
    private CreaseGracePairRecovery() {}
    static Pair find(byte[] gray,int w,int h,float gap,float staffTop,float staffBottom,Box first,Box principal) {
        if(gray==null||w<=0||h<=0||gray.length!=(long)w*h||!Float.isFinite(gap)||gap<8
                ||!Float.isFinite(staffTop)||!Float.isFinite(staffBottom)
                ||staffTop<0||staffBottom>=h||staffTop>=staffBottom
                ||!valid(first,w,h)||!valid(principal,w,h))return null;
        float fw=first.right-first.left+1,fh=first.bottom-first.top+1,pw=principal.right-principal.left+1;
        if(fw<gap*.6f||fw>gap*1.05f||fh<gap*.45f||fh>gap*.9f||pw<gap*1.2f
                ||principal.x-first.x<gap*3||principal.x-first.x>gap*5
                ||Math.abs(principal.y-first.y)>gap||first.y<staffTop+gap*.4f||first.y>staffBottom-gap*.4f
                ||!filledHead(gray,w,first)||!filledHead(gray,w,principal))return null;
        int[] a=paleStem(gray,w,h,first,gap);if(a==null)return null;
        int creaseLeft=-1,creaseRight=-1;
        int top=Math.max(0,Math.round(staffTop-gap*.5f)),bottom=Math.min(h-1,Math.round(staffBottom+gap*.5f));
        for(int x=Math.round(a[0]+gap*.25f);x<=Math.min(w-2,Math.round(principal.left-gap*.6f));x++) {
            boolean white=true;for(int y=top;y<=bottom;y++)if((gray[y*w+x]&255)<240){white=false;break;}
            if(white){if(creaseLeft<0)creaseLeft=x;creaseRight=x;}
            else if(creaseLeft>=0)break;
        }
        int creaseWidth=creaseRight-creaseLeft+1;
        if(creaseLeft<0||creaseWidth<Math.max(2,Math.round(gap*.12f))||creaseWidth>gap*.7f)return null;
        // Require ink on the surrounding ruled page, not ordinary blank paper.
        for(int side:new int[]{-1,1}) {
            int margin=Math.max(3,Math.round(gap*.5f));
            int x=side<0?creaseLeft-margin:creaseRight+margin,hits=0;
            if(x<0||x>=w)return null;
            for(int y=top;y<=bottom;y++)if((gray[y*w+x]&255)<210)hits++;
            if(hits<gap)return null;
        }
        int l=creaseRight+1,r=Math.min(principal.left-3,Math.round(creaseRight+gap*.9f));
        int t=Math.max(1,Math.round(first.y-gap*1.1f)),b=Math.min(h-2,Math.round(first.y+gap*.25f));
        if(r-l<3||b-t<3)return null;
        int darkest=255;for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)darkest=Math.min(darkest,gray[y*w+x]&255);
        if(darkest>160)return null;int threshold=Math.min(155,darkest+25);
        List<Box> cores=components(gray,w,l,t,r,b,threshold);
        List<Pair> matches=new ArrayList<>();
        for(Box core:cores) {
            int cw=core.right-core.left+1,ch=core.bottom-core.top+1;
            if(cw<3||ch<3||cw>gap*.9f||ch>gap*.8f||core.x-first.x<gap||core.x-first.x>gap*2.3f
                    ||Math.abs(core.y-first.y)>gap||core.bottom>=b)continue;
            Box recovered=new Box(Math.round(core.x-fw*.5f),Math.round(core.y-fh*.5f),
                    Math.round(core.x+fw*.5f),Math.round(core.y+fh*.5f),core.x,core.y);
            if(!valid(recovered,w,h))continue;
            int[] stem=paleStem(gray,w,h,recovered,gap);
            if(stem==null||stem[0]<=creaseRight||stem[0]>creaseRight+gap
                    ||Math.abs(stem[1]-a[1])>gap*.6f||Math.abs(stem[0]-a[0])<gap*.8f
                    ||Math.abs(stem[0]-a[0])>gap*2.2f)continue;
            List<float[]> beamRows=doubleCore(gray,w,h,stem[0],stem[1],gap);
            if(beamRows.size()<2)continue;
            boolean cap=false;
            for(float[] rows:beamRows) {
                int proven=0;
                for(float beam:rows) {
                    int hits=0,total=0;float shifted=beam+a[1]-stem[1];
                    // The near-white stripe has a short antialiased halo. Verify
                    // the complete surviving cap before that erased fringe.
                    for(int x=a[0];x<creaseLeft-Math.round(gap*.25f);x++) {
                        total++;int min=255;for(int dy=-1;dy<=1;dy++) {
                            int y=Math.round(shifted)+dy;if(y>=0&&y<h)min=Math.min(min,gray[y*w+x]&255);
                        }
                        if(min<170)hits++;
                    }
                    if(total>=3&&hits>=total*.8f)proven++;
                }
                if(proven==2){cap=true;break;}
            }
            if(cap)matches.add(new Pair(recovered,a[0],a[1],stem[0],stem[1],2));
        }
        return matches.size()==1?matches.get(0):null;
    }
    private static boolean valid(Box box,int w,int h) {
        return box!=null&&box.left>=0&&box.top>=0&&box.right<w&&box.bottom<h
                &&box.left<=box.right&&box.top<=box.bottom&&Float.isFinite(box.x+box.y)
                &&box.x>=box.left&&box.x<=box.right&&box.y>=box.top&&box.y<=box.bottom;
    }
    private static boolean filledHead(byte[] g,int w,Box box) {
        int ink=0,wideRows=0,width=box.right-box.left+1,height=box.bottom-box.top+1;
        for(int y=box.top;y<=box.bottom;y++){int count=0;for(int x=box.left;x<=box.right;x++)if((g[y*w+x]&255)<170)count++;ink+=count;if(count>=width*.45f)wideRows++;}
        return ink>=width*height*.2f&&wideRows>=3;
    }
    private static int[] paleStem(byte[] g,int w,int h,Box head,float gap) {
        int[] best=null;int longest=0;
        for(int x=Math.max(1,head.right-Math.round(gap*.45f));x<=Math.min(w-2,head.right+Math.round(gap*.2f));x++) {
            int end=Math.round(head.y),blank=0;
            for(int y=end;y>=Math.max(0,Math.round(head.y-gap*3.7f));y--) {
                if((g[y*w+x]&255)<205){end=y;blank=0;}else if(++blank>1)break;
            }
            int length=Math.round(head.y)-end;
            if(length>longest&&length<=gap*3.5f){longest=length;best=new int[]{x,end};}
        }
        return longest>=gap*1.6f?best:null;
    }
    private static List<float[]> doubleCore(byte[] g,int w,int h,int stem,int end,float gap) {
        List<float[]> found=new ArrayList<>();
        for(int offset=2;offset<=Math.round(gap*.35f);offset++) {
            int x=stem-offset;if(x<1||x>=w-1)continue;
            int t=Math.max(1,end-1),b=Math.min(h-2,Math.round(end+gap*1.2f)),dark=255;
            for(int y=t;y<=b;y++)dark=Math.min(dark,g[y*w+x]&255);
            int threshold=Math.min(165,dark+38),start=-1;List<Float> rows=new ArrayList<>();
            for(int y=t;y<=b+1;y++) {
                boolean ink=y<=b&&(g[y*w+x]&255)<threshold;
                if(ink&&start<0)start=y;
                if(!ink&&start>=0){int size=y-start;if(size>=2&&size<=gap*.45f)rows.add((start+y-1)*.5f);start=-1;}
            }
            if(rows.size()==2&&rows.get(1)-rows.get(0)>=gap*.3f&&rows.get(1)-rows.get(0)<=gap*.7f)
                found.add(new float[]{rows.get(0),rows.get(1)});
        }
        return found;
    }
    private static List<Box> components(byte[] g,int w,int l,int t,int r,int b,int threshold) {
        int rw=r-l+1,rh=b-t+1;boolean[] seen=new boolean[rw*rh];int[] queue=new int[rw*rh];List<Box> out=new ArrayList<>();
        for(int sy=0;sy<rh;sy++)for(int sx=0;sx<rw;sx++) {
            int seed=sy*rw+sx;if(seen[seed]||(g[(sy+t)*w+sx+l]&255)>threshold)continue;
            int size=1,take=0,minX=sx,maxX=sx,minY=sy,maxY=sy;long sumX=0,sumY=0;queue[0]=seed;seen[seed]=true;
            while(take<size){int at=queue[take++],x=at%rw,y=at/rw;sumX+=x;sumY+=y;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int xx=x+dx,yy=y+dy;if(xx<0||xx>=rw||yy<0||yy>=rh)continue;int n=yy*rw+xx;if(!seen[n]&&(g[(yy+t)*w+xx+l]&255)<=threshold){seen[n]=true;queue[size++]=n;}}}
            if(size>=9)out.add(new Box(l+minX,t+minY,l+maxX,t+maxY,l+(float)sumX/size,t+(float)sumY/size));
        }
        return out;
    }
}
