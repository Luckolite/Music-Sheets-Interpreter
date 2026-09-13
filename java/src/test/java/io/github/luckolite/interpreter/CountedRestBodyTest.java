// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original short H-rest, count glyph and deliberately mislabeled band pixels. */
public class CountedRestBodyTest {
    static final int W=400,H=260;
    static final List<MeasureRegion> REGIONS=List.of(
            new MeasureRegion(.15f,.55f,.23f,.72f),new MeasureRegion(.56f,.95f,.23f,.72f));
    static class Page {
        byte[] gray=new byte[W*H],labels=new byte[W*H];
        Page(boolean count,boolean leftCap,boolean rightCap) {
            Arrays.fill(gray,(byte)255);
            for(int y=96;y<=160;y+=16)rect(20,y,379,y,4);
            rect(104,122,184,133,1);
            if(leftCap)rect(104,112,106,144,1);
            if(rightCap)rect(182,112,184,144,1);
            if(count)two(137,60);
            for(int y=125;y<=131;y++)for(int x=140;x<=159;x++)labels[y*W+x]=2;
            head(300,152);rect(310,104,310,152,1);
        }
        void rect(int l,int t,int r,int b,int label){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}}
        void two(int x,int y){rect(x,y,x+14,y,1);rect(x+14,y+1,x+14,y+7,1);for(int d=0;d<=13;d++)rect(x+14-d,y+7+d,x+14-d,y+7+d,1);rect(x,y+20,x+14,y+20,1);}
        void head(int x,int y){for(int yy=y-7;yy<=y+7;yy++)for(int xx=x-10;xx<=x+10;xx++)if(Math.pow((xx-x)/10.,2)+Math.pow((yy-y)/7.,2)<=1){gray[yy*W+xx]=0;labels[yy*W+xx]=2;}}
        byte[] normalize(){return OmrScoreInterpreter.normalizeHeaderSymbols(labels,gray,W,H,REGIONS);}
    }
    @Test public void aLargeIslandInsideAShortCountedRestIsRemoved(){var p=new Page(true,true,true);assertEquals(0,p.normalize()[128*W+150]);}
    @Test public void theRestBodyDoesNotBecomeASoundingNote(){var p=new Page(true,true,true);assertEquals(1,OmrScoreInterpreter.analyze(p.labels,p.gray,W,H,REGIONS).notes().size());}
    @Test public void removingTheFalseHeadAllowsRestExpansion(){var p=new Page(true,true,true);assertEquals(1,MultiMeasureRestDetector.detect(p.normalize(),p.gray,W,H,REGIONS,List.of()).size());}
    @Test public void aShortUncountedBarIsInsufficient(){var p=new Page(false,true,true);assertArrayEquals(p.labels,p.normalize());}
    @Test public void aCountDoesNotReplaceTheLeftCap(){var p=new Page(true,false,true);assertArrayEquals(p.labels,p.normalize());}
    @Test public void aCountDoesNotReplaceTheRightCap(){var p=new Page(true,true,false);assertArrayEquals(p.labels,p.normalize());}
    @Test public void aRealHeadOutsideTheBandIsPreserved(){var p=new Page(true,true,true);p.head(155,160);p.rect(165,112,165,160,1);assertEquals(2,p.normalize()[160*W+155]);}
    @Test public void aDisplacedCountDoesNotCorroborateTheBar(){var p=new Page(false,true,true);p.two(190,60);assertArrayEquals(p.labels,p.normalize());}
    @Test public void normalizationPreservesInputAndIsIdempotent(){var p=new Page(true,true,true);var before=p.labels.clone();var gray=p.gray.clone();var normalized=p.normalize();assertArrayEquals(before,p.labels);assertArrayEquals(gray,p.gray);assertArrayEquals(normalized,OmrScoreInterpreter.normalizeHeaderSymbols(normalized,p.gray,W,H,REGIONS));}
}
