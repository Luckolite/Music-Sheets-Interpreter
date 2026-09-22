// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original stacked-meter raster and independent-note controls. */
public class StackedOpeningMeterFragmentTest {
    static final int W=600,H=240;
    static final List<MeasureRegion> MEASURES=List.of(new MeasureRegion(80f/W,560f/W,60f/H,170f/H));
    final byte[] labels=new byte[W*H],gray=new byte[W*H];
    public StackedOpeningMeterFragmentTest(){
        Arrays.fill(gray,(byte)255);
        for(int y=80;y<=144;y+=16)for(int x=10;x<590;x++)ink(x,y,4);
    }
    void ink(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
    void digit(int top){
        for(int y=top;y<=top+18;y++)for(int dx=-2;dx<=2;dx++)
            ink(95-(y-top)*7/18+dx,y,5);
        for(int y=top+17;y<=top+20;y++)for(int x=88;x<=120;x++)ink(x,y,5);
        for(int y=top;y<=top+28;y++)for(int x=113;x<=117;x++)ink(x,y,5);
    }
    void firstNote(){
        for(int y=122;y<=134;y++)for(int x=174;x<=194;x++)
            if(Math.pow((x-184)/10d,2)+Math.pow((y-128)/6d,2)<=1)ink(x,y,2);
        for(int y=80;y<=128;y++)ink(194,y,1);
    }
    byte[] normalized(){return OmrScoreInterpreter.normalizeHeaderSymbols(labels,gray,W,H,MEASURES);}
    @Test public void stackedMeterCornerIsNotAPlayedNote(){
        digit(82);digit(117);for(int y=101;y<=109;y++)for(int x=112;x<=118;x++)ink(x,y,2);
        firstNote();byte[] before=labels.clone(),ink=gray.clone(),clean=normalized();
        assertEquals(0,clean[105*W+115]);assertEquals(2,clean[128*W+184]);
        assertArrayEquals(before,labels);assertArrayEquals(ink,gray);
        assertArrayEquals(clean,OmrScoreInterpreter.normalizeHeaderSymbols(clean,gray,W,H,MEASURES));
    }
    @Test public void singleGlyphCannotHideARealHead(){
        digit(82);for(int y=101;y<=109;y++)for(int x=112;x<=118;x++)ink(x,y,2);
        assertEquals(2,normalized()[105*W+115]);
    }
    @Test public void independentStemOutsideStaffProtectsTheHead(){
        digit(82);digit(117);for(int y=101;y<=109;y++)for(int x=112;x<=118;x++)ink(x,y,2);
        for(int y=45;y<=105;y++)ink(118,y,1);
        assertEquals(2,normalized()[105*W+115]);
    }
}
