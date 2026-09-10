// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Follows complete printed five-line groups across a curved page. */
final class StaffPitchTrack {
    private final float[][] points;
    private StaffPitchTrack(List<float[]> points) { this.points=points.toArray(new float[0][]); }

    static StaffPitchTrack detect(byte[] gray,int width,int height,float top,float bottom,float gap) {
        if(gray==null||gap<3)return null;
        int stripWidth=Math.min(width,Math.max(80,Math.round(gap*10)));
        int first=Math.max(0,Math.round(top-gap*6)),last=Math.min(height,Math.round(bottom+gap*6));
        if(last<=first)return null;
        List<float[]> samples=new ArrayList<>();
        for(int strip=0;strip<7;strip++) {
            int left=Math.max(0,Math.min(width-stripWidth,Math.round(width*(.15f+strip*.12f)-stripWidth*.5f)));
            byte[] local=new byte[stripWidth*(last-first)];
            for(int y=first;y<last;y++)System.arraycopy(gray,y*width+left,local,(y-first)*stripWidth,stripWidth);
            RawStaffLineDetector.StaffLines best=null;float distance=Float.MAX_VALUE;
            for(var lines:RawStaffLineDetector.detect(local,stripWidth,last-first)) {
                if(lines.gap()<gap*.88f||lines.gap()>gap*1.12f)continue;
                if(!completeRules(local,stripWidth,last-first,lines))continue;
                float d=Math.abs(lines.bottom()+first-bottom);
                if(d<distance){distance=d;best=lines;}
            }
            if(best!=null)samples.add(new float[]{left+stripWidth*.5f,best.bottom()+first,best.gap()});
        }
        if(samples.size()<4)return null;
        float typicalGap=median(samples.stream().map(a->a[2]).toList());
        samples.removeIf(a->Math.abs(a[2]-typicalGap)>typicalGap*.08f);
        if(samples.size()<4)return null;
        List<Float> slopes=new ArrayList<>();
        for(int i=0;i<samples.size();i++)for(int j=i+1;j<samples.size();j++) {
            float[] a=samples.get(i),b=samples.get(j);
            slopes.add((b[1]-a[1])/(b[0]-a[0]));
        }
        float slope=median(slopes);
        if(Math.abs(slope)*width>typicalGap*8)return null;
        float intercept=median(samples.stream().map(a->a[1]-slope*a[0]).toList());
        samples.removeIf(a->Math.abs(a[1]-intercept-slope*a[0])>typicalGap*.8f);
        if(samples.size()<4)return null;
        samples.sort(Comparator.comparingDouble(a->a[0]));
        if(samples.get(samples.size()-1)[0]-samples.get(0)[0]<width*.36f)return null;
        float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;
        for(float[] p:samples){min=Math.min(min,p[1]);max=Math.max(max,p[1]);}
        if(max-min<typicalGap*.8f)return null;
        return new StaffPitchTrack(samples);
    }

    private static boolean completeRules(byte[] gray,int width,int height,RawStaffLineDetector.StaffLines lines) {
        int radius=Math.max(1,Math.round(lines.gap()*.2f)),flank=Math.max(2,Math.round(lines.gap()*.32f));
        for(int row:lines.rows()) {
            int columns=0;
            for(int x=0;x<width;x++)for(int y=Math.max(flank,row-radius);y<=Math.min(height-1-flank,row+radius);y++) {
                int ink=gray[y*width+x]&255;
                if(ink<=170&&(gray[(y-flank)*width+x]&255)>=ink+12&&(gray[(y+flank)*width+x]&255)>=ink+12) {
                    columns++;break;
                }
            }
            if(columns<width*.6f)return false;
        }
        return true;
    }

    float[] at(float x) {
        int right=1;
        while(right<points.length-1&&x>points[right][0])right++;
        float[] a=points[right-1],b=points[right];
        float fraction=(x-a[0])/(b[0]-a[0]);
        return new float[]{a[1]+fraction*(b[1]-a[1]),a[2]+Math.max(0,Math.min(1,fraction))*(b[2]-a[2])};
    }

    /** A complete local five-line group resolves the one-line ambiguity of a partial match. */
    static boolean needsContrast(byte[] gray,int width,int height,float x,float bottom,float gap) {
        if(gray==null)return false;
        int left=Math.max(0,Math.round(x-gap*4)),right=Math.min(width-1,Math.round(x+gap*4));
        int top=Math.max(0,Math.round(bottom-gap*5)),last=Math.min(height-1,Math.round(bottom+gap));
        int samples=0,shaded=0;
        for(int y=top;y<=last;y+=2)for(int xx=left;xx<=right;xx+=2) {
            samples++;if((gray[y*width+xx]&255)<=205)shaded++;
        }
        return samples>=24&&shaded>=samples*.65f;
    }

    static float[] localRules(byte[] labels,byte[] gray,int width,int height,float x,int headLeft,int headRight,
                              float referenceBottom,float gap) {
        return localRules(labels,gray,width,height,x,headLeft,headRight,referenceBottom,gap,false);
    }

    static float[] localRules(byte[] labels,byte[] gray,int width,int height,float x,int headLeft,int headRight,
                              float referenceBottom,float gap,boolean curved) {
        // Curled paper can turn sharply after the final reliable wide sample.
        // Compare complete rules in a sloped window instead of extrapolating
        // the last wide-strip slope past the printed note.
        // Require page-level curvature evidence before trying inclined rules:
        // beams on a flat staff can otherwise form a competing tilted pattern.
        for(float slope:curved?new float[]{0,.04f,-.04f,.08f,-.08f,.12f,-.12f,.16f,-.16f}:new float[]{0}) {
            float[] found=localRulesWithSlope(labels,gray,width,height,x,headLeft,headRight,referenceBottom,gap,slope);
            if(found!=null)return found;
        }
        return null;
    }

    private static float[] localRulesWithSlope(byte[] labels,byte[] gray,int width,int height,float x,int headLeft,int headRight,
                              float referenceBottom,float gap,float slope) {
        if(gray==null)return null;
        int radius=Math.max(4,Math.round(gap*3.5f)),exclusion=Math.max(1,Math.round(gap*.45f));
        int left=Math.max(0,Math.round(x)-radius),right=Math.min(width-1,Math.round(x)+radius);
        int top=Math.max(0,Math.round(referenceBottom-gap*5.5f)),bottom=Math.min(height-1,Math.round(referenceBottom+gap*1.5f));
        if(bottom<top)return null;
        int[] strength=new int[bottom-top+1];int samples=0,flank=Math.max(2,Math.round(gap*.32f));
        for(int xx=left;xx<=right;xx++) {
            if(xx>=headLeft-exclusion&&xx<=headRight+exclusion)continue;
            samples++;
            for(int y=Math.max(flank,top);y<=Math.min(height-1-flank,bottom);y++) {
                int row=y+Math.round((xx-x)*slope);
                if(row<flank||row+flank>=height)continue;
                int ink=gray[row*width+xx]&255;
                if(ink<=205&&(gray[(row-flank)*width+xx]&255)>=ink+12&&(gray[(row+flank)*width+xx]&255)>=ink+12)
                    strength[y-top]++;
            }
        }
        if(samples<8)return null;
        int minimum=Math.max(8,Math.round(samples*.45f)),band=Math.max(2,Math.round(gap*.25f));
        // Reject beam-only peaks before selecting a five-line group, so a stronger beam
        // cannot hide a valid staff group through overlap suppression.
        for(int row=0;row<strength.length;row++)if(strength[row]>=minimum) {
            int supported=0,center=top+row;
            for(int xx=left;xx<=right;xx++) {
                if(xx>=headLeft-exclusion&&xx<=headRight+exclusion)continue;
                int shifted=center+Math.round((xx-x)*slope);
                for(int yy=Math.max(0,shifted-band);yy<=Math.min(height-1,shifted+band);yy++)
                    if(labels[yy*width+xx]==4){supported++;break;}
            }
            if(supported<Math.max(4,samples*.2f))strength[row]=0;
        }
        for(var lines:RawStaffLineDetector.detectFromStrength(strength,minimum,strength.length)) {
            if(lines.gap()<gap*.88f||lines.gap()>gap*1.12f)continue;
            float[] centers=new float[5];
            for(int i=0;i<5;i++) {
                int peak=lines.rows()[i],a=peak,b=peak;
                while(a>0&&peak-a<gap*.2f&&strength[a-1]>=strength[peak]*.85f)a--;
                while(b+1<strength.length&&b-peak<gap*.2f&&strength[b+1]>=strength[peak]*.85f)b++;
                centers[i]=top+(a+b)*.5f;
            }
            List<Float> spacings=new ArrayList<>();
            for(int i=0;i<5;i++)for(int j=i+1;j<5;j++)spacings.add((centers[j]-centers[i])/(j-i));
            float spacing=median(spacings);List<Float> bottoms=new ArrayList<>();
            for(int i=0;i<5;i++)bottoms.add(centers[i]+(4-i)*spacing);
            float base=median(bottoms);int consistent=0;
            for(float value:bottoms)if(Math.abs(value-base)<=gap*.16f)consistent++;
            if(consistent==5&&Math.abs(base-referenceBottom)<=gap*1.5f)return new float[]{base,spacing};
        }
        return null;
    }

    private static float median(List<Float> values) {
        float[] sorted=new float[values.size()];for(int i=0;i<sorted.length;i++)sorted[i]=values.get(i);
        Arrays.sort(sorted);return sorted[sorted.length/2];
    }
}
