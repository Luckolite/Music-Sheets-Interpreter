// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original notation geometry; model islands are independent of printed strokes. */
public class StraightEntranceStrokeTest {
    private static final int W=520,H=300;
    private static final List<MeasureRegion> M=List.of(new MeasureRegion(.02f,.98f,.2f,.95f));
    private static class Page {
        final byte[] gray=new byte[W*H],labels=new byte[W*H];
        Page(boolean owner,boolean stroke) {
            Arrays.fill(gray,(byte)255);
            for(int y=108;y<=172;y+=16)for(int x=20;x<500;x++)ink(x,y,4);
            if(owner){head(330,156,11,7);stem(341,100,156);}
            if(stroke)for(int x=292;x<=316;x++)for(int dy=-2;dy<=2;dy++)gray[(483-x+dy)*W+x]=0;
            for(int y=176;y<=182;y++)for(int x=299;x<=309;x++)
                if(Math.pow((x-304)/5.,2)+Math.pow((y-179)/3.,2)<=1)labels[y*W+x]=2;
        }
        void ink(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
        void head(int cx,int cy,int rx,int ry){for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
            if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1)ink(x,y,2);}
        void stem(int x,int a,int b){for(int y=a;y<=b;y++)ink(x,y,1);}
        OmrScoreInterpreter.Analysis analyze(){return OmrScoreInterpreter.analyze(labels,gray,W,H,M);}
        boolean hasSmall(){return analyze().notes().stream().anyMatch(n->Math.abs((.02f+n.positionInMeasure()*.96f)*W-304)<6);}
    }
    @Test public void entranceStrokeIslandDoesNotCreateAnAttack(){var p=new Page(true,true);assertFalse(p.hasSmall());assertEquals(1,p.analyze().notes().size());}
    @Test public void destinationPitchRemainsUnchanged(){var p=new Page(true,true);assertEquals(2,p.analyze().notes().get(0).staffStep());}
    @Test public void realStemmedGraceNoteSurvives(){var p=new Page(true,true);p.head(304,179,5,3);p.stem(309,141,179);assertTrue(p.hasSmall());}
    @Test public void anIsolatedSmallOvalIsNotAnEntrance(){var p=new Page(true,false);p.head(304,179,5,3);assertTrue(p.hasSmall());}
    @Test public void aNearbyNoteIsRequired(){var p=new Page(false,true);assertTrue(p.hasSmall());}
    @Test public void aDestinationStemIsRequired(){var p=new Page(true,true);for(int y=100;y<149;y++){p.gray[y*W+341]=(byte)255;p.labels[y*W+341]=0;}assertTrue(p.hasSmall());}
    @Test public void shortDiagonalInkIsInsufficient(){var p=new Page(true,false);for(int x=300;x<=308;x++)for(int dy=-2;dy<=2;dy++)p.gray[(483-x+dy)*W+x]=0;assertTrue(p.hasSmall());}
    @Test public void printedOvalCannotBeDiscardedByASmallMask(){var p=new Page(true,true);for(int y=174;y<=184;y++)for(int x=298;x<=310;x++)if(Math.pow((x-304)/6.,2)+Math.pow((y-179)/5.,2)<=1)p.gray[y*W+x]=0;assertTrue(p.hasSmall());}
    @Test public void semanticLabelsAloneCannotProveAStroke(){var p=new Page(true,true);assertEquals(2,OmrScoreInterpreter.analyze(p.labels,null,W,H,M).notes().size());}
    @Test public void sourcePixelsArePreserved(){var p=new Page(true,true);var l=p.labels.clone();var g=p.gray.clone();p.analyze();assertArrayEquals(l,p.labels);assertArrayEquals(g,p.gray);}
}
