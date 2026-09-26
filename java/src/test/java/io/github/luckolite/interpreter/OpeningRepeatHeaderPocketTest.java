// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original header/repeat layout; no score-derived pixels. */
public class OpeningRepeatHeaderPocketTest {
    static final int W=600,H=240;
    byte[] page(int opening,boolean header) {
        byte[] labels=new byte[W*H];
        for(int k=0;k<5;k++)for(int x=60;x<=540;x++)labels[(100+k*8)*W+x]=4;
        for(int x:new int[]{60,opening,300,540})for(int y=97;y<=135;y++)labels[y*W+x]=1;
        if(header)for(int x=75;x<=90;x++)for(int y=105;y<=126;y++)labels[y*W+x]=3;
        for(int x=170;x<=182;x++)for(int y=115;y<=123;y++)labels[y*W+x]=2;
        return labels;
    }
    @Test public void fourGapSymbolOnlyPocketBeforeRepeatHasNoTime(){assertEquals(2,OmrMeasurePostProcessor.process(page(130,true),W,H).size());}
    @Test public void restInNarrowOpeningIsPreserved(){byte[] l=page(130,true);for(int x=104;x<=108;x++)for(int y=112;y<=118;y++)l[y*W+x]=1;assertEquals(3,OmrMeasurePostProcessor.process(l,W,H).size());}
    @Test public void noteInNarrowOpeningIsPreserved(){byte[] l=page(130,true);for(int x=104;x<=114;x++)for(int y=112;y<=120;y++)l[y*W+x]=2;assertEquals(3,OmrMeasurePostProcessor.process(l,W,H).size());}
    @Test public void widerEmptyRegionCannotBeDiscarded(){assertEquals(3,OmrMeasurePostProcessor.process(page(150,true),W,H).size());}
    @Test public void noHeaderCannotAuthorizeDeletion(){assertEquals(3,OmrMeasurePostProcessor.process(page(130,false),W,H).size());}
    @Test public void labelsAreUnchanged(){byte[] l=page(130,true),copy=l.clone();OmrMeasurePostProcessor.process(l,W,H);assertArrayEquals(copy,l);}
    @Test public void singlePixelHeadNoiseDoesNotCreateTime(){byte[] l=page(130,true);l[112*W+110]=2;assertEquals(2,OmrMeasurePostProcessor.process(l,W,H).size());}
    @Test public void smallHeadBeyondNoiseBudgetIsPreserved(){byte[] l=page(130,true);for(int x=107;x<=109;x++)l[112*W+x]=2;assertEquals(3,OmrMeasurePostProcessor.process(l,W,H).size());}
}
