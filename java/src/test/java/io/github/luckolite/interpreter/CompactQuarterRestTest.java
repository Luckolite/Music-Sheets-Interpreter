// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original compact zigzag-and-hook engraving on a five-line staff. */
public class CompactQuarterRestTest {
    static final int W=600,H=280;
    static class Page {
        final byte[] gray=new byte[W*H],labels=new byte[W*H];final int gap;
        Page(int gap,boolean hook,boolean zigzag,boolean head,boolean stem) {
            this.gap=gap;Arrays.fill(gray,(byte)240);
            for(int y=100;y<=100+4*gap;y+=gap)for(int x=20;x<W-20;x++){gray[y*W+x]=0;labels[y*W+x]=4;}
            int[][] rows={{1,2},{2,3},{3,4},{4,5},{5,6},{6,7},{7,8},
                    {7,10},{7,11},{6,11},{6,11},{5,11},{5,11},
                    {4,10},{4,9},{5,9},{6,9},{7,10},{8,11},
                    {6,12},{4,13},{3,13},{2,13},{2,5},{2,5},
                    {2,5},{2,5},{3,5},{3,5},{3,5},{3,5}};
            for(int row=0;row<rows.length;row++) {
                int l=rows[row][0],r=rows[row][1];
                if(!hook&&row>=19){l=7;r=10;}
                if(!zigzag&&row<19){l=5;r=8;}
                int y1=100+Math.round(gap*.8f+row*gap*2.2f/31);
                int y2=100+Math.round(gap*.8f+(row+1)*gap*2.2f/31);
                for(int y=y1;y<=y2;y++)for(int x=150+Math.round(l*gap/16f);x<=150+Math.round(r*gap/16f);x++){gray[y*W+x]=0;labels[y*W+x]=5;}
            }
            int cx=150+Math.round(gap*.22f),cy=100+Math.round(gap*2.65f);
            int rx=stem?4:3,ry=stem?5:4;
            if(head)for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
                if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1){labels[y*W+x]=2;if(stem)gray[y*W+x]=0;}
            if(stem)for(int y=cy-3*gap;y<=cy;y++){gray[y*W+cx+rx]=0;labels[y*W+cx+rx]=1;}
        }
        List<ScoreRestEvent> detect(){return SixteenthRestDetector.detect(gray,W,H,List.of(new MeasureRegion(.04f,.95f,.2f,.9f)),List.of(new SixteenthRestDetector.Staff(100,100+4*gap,gap,0,1)),List.of());}
        OmrScoreInterpreter.Analysis analyze(){return OmrScoreInterpreter.analyze(labels,gray,W,H,List.of(new MeasureRegion(.04f,.95f,.2f,.9f)));}
    }
    @Test public void compactQuarterRestHasOneBeat(){var rests=new Page(16,true,true,false,false).detect();assertEquals(1,rests.size());assertEquals(1,rests.get(0).durationBeats(),0);}
    @Test public void largerCompactEngravingHasOneBeat(){var rests=new Page(22,true,true,false,false).detect();assertEquals(1,rests.size());assertEquals(1,rests.get(0).durationBeats(),0);}
    @Test public void predictedHookDoesNotBecomeANote(){var a=new Page(16,true,true,true,false).analyze();assertEquals(a.notes().toString(),0,a.notes().size());assertEquals(1,a.rests().size());}
    @Test public void realContinuousStemKeepsItsHead(){assertEquals(1,new Page(16,true,true,true,true).analyze().notes().size());}
    @Test public void missingHookCannotProveQuarterRest(){assertTrue(new Page(16,false,true,false,false).detect().isEmpty());}
    @Test public void missingZigzagCannotProveQuarterRest(){assertTrue(new Page(16,true,false,false,false).detect().isEmpty());}
    @Test public void inputImagesRemainUnchanged(){var p=new Page(16,true,true,true,false);var g=p.gray.clone();var l=p.labels.clone();p.analyze();assertArrayEquals(g,p.gray);assertArrayEquals(l,p.labels);}
}
