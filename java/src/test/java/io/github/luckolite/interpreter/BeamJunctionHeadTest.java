// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class BeamJunctionHeadTest {
    private static final int W=320,H=280;
    private final byte[] labels=new byte[W*H],gray=new byte[W*H];
    private void ink(int x,int y,int label) {gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
    private void ellipse(int cx,int cy,int rx,int ry,boolean semantic) {
        for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
            if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1) {
                gray[y*W+x]=0;if(semantic)labels[y*W+x]=2;
            }
    }
    private void page(boolean beam,boolean fullHead,boolean owner,boolean gap) {
        Arrays.fill(gray,(byte)255);
        for(int y=80;y<=144;y+=16)for(int x=15;x<305;x++)ink(x,y,4);
        if(owner)ellipse(92,96,11,8,true);
        for(int y=96;y<=150;y++)for(int x=81;x<=83;x++)ink(x,y,1);
        if(beam)for(int x=81;x<=136;x++) {
            int center=Math.round(147-(x-81)*.4f);
            for(int y=center-2;y<=center+2;y++)ink(x,y,1);
        }
        if(fullHead)ellipse(87,146,7,6,false);
        if(!beam&&!fullHead)ellipse(87,146,6,4,false);
        for(int y=143;y<=150;y++)for(int x=81;x<=91;x++)
            if((gray[y*W+x]&255)<165)labels[y*W+x]=2;
        if(gap)for(int y=114;y<=124;y++)for(int x=79;x<=85;x++) {
            gray[y*W+x]=(byte)255;labels[y*W+x]=0;
        }
    }
    private List<ScoreNoteEvent> notes(boolean raw) {
        return OmrScoreInterpreter.analyze(labels,raw?gray:null,W,H,
                List.of(new MeasureRegion(.02f,.98f,.02f,.98f))).notes();
    }
    @Test public void downStemBeamCornerDoesNotAddANote() {
        page(true,false,true,false);assertEquals(1,notes(true).size());
    }
    @Test public void upStemBeamCornerDoesNotAddANote() {
        page(true,false,true,false);
        for(int i=0;i<labels.length/2;i++) {
            int other=labels.length-1-i;byte v=labels[i];labels[i]=labels[other];labels[other]=v;
            v=gray[i];gray[i]=gray[other];gray[other]=v;
        }
        assertEquals(1,notes(true).size());
    }
    @Test public void partialMaskOnSmallPrintedChordHeadIsPreserved() {
        page(true,true,true,false);assertEquals(2,notes(true).size());
    }
    @Test public void smallHeadWithoutAnExtendedBeamIsPreserved() {
        page(false,false,true,false);assertEquals(2,notes(true).size());
    }
    @Test public void disconnectedStemCannotEstablishOwnership() {
        page(true,false,true,true);assertEquals(2,notes(true).size());
    }
    @Test public void noLargerOwnerMeansNoRejection() {
        page(true,false,false,false);assertEquals(1,notes(true).size());
    }
    @Test public void semanticOnlyInputIsPreserved() {
        page(true,false,true,false);assertEquals(2,notes(false).size());
    }
}
