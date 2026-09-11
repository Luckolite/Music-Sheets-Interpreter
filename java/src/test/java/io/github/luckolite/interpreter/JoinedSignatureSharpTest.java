// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original signature shapes and semantic bridges, without score samples. */
public class JoinedSignatureSharpTest {
    static final int W=640,H=720,G=16;
    final byte[] labels=new byte[W*H],gray=new byte[W*H];
    final List<MeasureRegion> measures=new ArrayList<>();
    public JoinedSignatureSharpTest(){Arrays.fill(gray,(byte)255);}
    void ink(int x,int y,int label){labels[y*W+x]=(byte)label;gray[y*W+x]=0;}
    void sharp(int x,int y) {
        for(int xx:new int[]{x-5,x-4,x+3,x+4})for(int yy=y-22;yy<=y+22;yy++)ink(xx,yy,3);
        for(int cy:new int[]{y-5,y+5})for(int yy=cy-1;yy<=cy+1;yy++)
            for(int xx=x-8;xx<=x+7;xx++)ink(xx,yy,3);
    }
    void row(int top,int count,int joins,boolean clippedTail) {
        for(int y=top;y<=top+4*G;y+=G)for(int x=20;x<620;x++)ink(x,y,4);
        for(int y=top-30;y<=top+80;y++)for(int x=30;x<=55;x++)ink(x,y,3);
        int[] dy={0,24,-8,16,-16,8,-24};
        for(int i=0;i<count;i++)sharp(80+i*21,top+dy[i]);
        for(int i=0;i+1<count;i++)if((joins&(1<<i))!=0) {
            int y=top+(dy[i]+dy[i+1])/2;
            for(int x=80+i*21+3;x<=80+(i+1)*21-4;x++)ink(x,y,3);
        }
        if(clippedTail) {
            sharp(143,top+16);
            // The source still has a sharp, but only its two upper spines survive in the mask.
            for(int y=top-6;y<=top+38;y++)for(int x=135;x<=150;x++)labels[y*W+x]=0;
            for(int y=top-6;y<=top+15;y++)for(int x:new int[]{138,139,146,147})labels[y*W+x]=3;
            for(int x=138;x<=147;x++)labels[(top-6)*W+x]=3;
        }
        for(int y=top+26;y<=top+38;y++)for(int x=342;x<=358;x++)
            if((x-350)*(x-350)/64d+(y-top-32)*(y-top-32)/36d<=1)ink(x,y,2);
        for(int y=top-16;y<=top+32;y++)ink(358,y,1);
        measures.add(new MeasureRegion(180f/W,620f/W,(top-35f)/H,(top+99f)/H));
    }
    List<Integer> keys() {
        var result=new ArrayList<Integer>();
        for(var key:OmrScoreInterpreter.analyze(labels,gray,W,H,measures).keyChanges())result.add(key.fifths());
        return result;
    }
    @Test public void connectedFirstPairStillHasFourSharps(){row(100,4,1,false);assertEquals(List.of(4),keys());}
    @Test public void connectedSecondPairStillHasFourSharps(){row(100,4,4,false);assertEquals(List.of(4),keys());}
    @Test public void joinedRunCanContainMoreThanTwoSharps(){row(100,4,7,false);assertEquals(List.of(4),keys());}
    @Test public void intactSignatureIsUnchanged(){row(100,4,0,false);assertEquals(List.of(4),keys());}
    @Test public void realReductionInSharpsStillChangesTheKey(){row(100,4,0,false);row(310,3,0,false);assertEquals(List.of(4,3),keys());}
    @Test public void clippedExtraSymbolDoesNotInventAKeyChange(){row(100,4,0,false);row(310,3,0,true);assertEquals(List.of(4),keys());}
    @Test public void anotherRowsSymbolsCannotBlockARealChange(){row(100,4,0,false);row(310,3,0,false);row(520,4,0,false);assertEquals(List.of(4,3,4),keys());}
    @Test public void thickSeparateSpinesDoNotBecomeACrossbar() {
        row(100,0,0,false);
        for(int y=82;y<=120;y++)for(int x=75;x<=77;x++)ink(x,y,3);
        for(int y=78;y<=120;y++)for(int x=81;x<=85;x++)ink(x,y,3);
        for(int cy:new int[]{92,104})for(int y=cy;y<cy+4;y++)for(int x=72;x<=87;x++)ink(x,y,3);
        assertEquals(List.of(1),keys());
    }
    @Test public void aSmallMaskGapDoesNotEraseTheLowerCrossbar() {
        row(100,0,0,false);
        for(int y=82;y<=120;y++)for(int x=75;x<=77;x++)ink(x,y,3);
        for(int y=78;y<=120;y++)for(int x=81;x<=85;x++)ink(x,y,3);
        for(int cy:new int[]{92,104})for(int y=cy;y<cy+4;y++)for(int x=72;x<=87;x++)ink(x,y,3);
        for(int y=104;y<108;y++)for(int x=78;x<=79;x++)labels[y*W+x]=0;
        assertEquals(List.of(1),keys());
    }
    @Test public void twoStemsAloneAreNotASharp() {
        row(100,0,0,false);
        for(int y=82;y<=120;y++)for(int x=75;x<=77;x++)ink(x,y,3);
        for(int y=78;y<=120;y++)for(int x=81;x<=85;x++)ink(x,y,3);
        assertEquals(List.of(),keys());
    }
    @Test public void signatureAnalysisDoesNotChangeInputMasks(){row(100,4,7,false);var l=labels.clone();var g=gray.clone();keys();assertArrayEquals(l,labels);assertArrayEquals(g,gray);}
}
