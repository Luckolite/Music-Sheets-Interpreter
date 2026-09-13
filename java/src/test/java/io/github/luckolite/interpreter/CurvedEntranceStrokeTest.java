// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original scoop curves, staff crossings and independent semantic islands. */
public class CurvedEntranceStrokeTest {
    static final int W=520,H=300;
    static final List<MeasureRegion> M=List.of(new MeasureRegion(.02f,.98f,.2f,.95f));
    static class Page {
        final byte[] gray=new byte[W*H],labels=new byte[W*H];
        Page(boolean owner,boolean curve) {
            Arrays.fill(gray,(byte)255);
            for(int y=108;y<=172;y+=16)for(int x=20;x<500;x++)ink(x,y,4);
            if(owner){head(330,156,11,7,true);stem(341,100,156);}
            if(curve)curve(1);
            head(306,174,4,4,false);
        }
        void ink(int x,int y,int label){gray[y*W+x]=0;labels[y*W+x]=(byte)label;}
        void curve(int thickness) {
            for(int x=292;x<=315;x++) {
                double t=(x-292)/23.;int cy=(int)Math.round(180-24*t*t*t);
                for(int dy=-thickness;dy<=thickness;dy++)gray[(cy+dy)*W+x]=0;
            }
        }
        void head(int cx,int cy,int rx,int ry,boolean raw){
            for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
                if(Math.pow((x-cx)/(double)rx,2)+Math.pow((y-cy)/(double)ry,2)<=1){labels[y*W+x]=2;if(raw)gray[y*W+x]=0;}
        }
        void stem(int x,int top,int bottom){for(int y=top;y<=bottom;y++)ink(x,y,1);}
        OmrScoreInterpreter.Analysis read(){return OmrScoreInterpreter.analyze(labels,gray,W,H,M);}
        boolean hasSmall(){return read().notes().stream().anyMatch(n->Math.abs((.02f+n.positionInMeasure()*.96f)*W-306)<6);}
    }
    @Test public void curvedEntranceIsNotASeparateNote(){var p=new Page(true,true);assertFalse(p.hasSmall());assertEquals(1,p.read().notes().size());}
    @Test public void destinationPitchIsPreserved(){var p=new Page(true,true);assertEquals(2,p.read().notes().get(0).staffStep());}
    @Test public void realStemmedGraceSurvives(){var p=new Page(true,true);p.head(306,174,4,4,true);p.stem(310,132,174);assertTrue(p.hasSmall());}
    @Test public void nearbyDestinationIsRequired(){assertTrue(new Page(false,true).hasSmall());}
    @Test public void printedSmallOvalIsRetained(){var p=new Page(true,false);p.head(306,174,4,4,true);assertTrue(p.hasSmall());}
    @Test public void aRawOvalCannotHideBehindASmallSemanticIsland(){
        var p=new Page(true,true);var original=p.labels.clone();p.head(306,174,7,5,true);System.arraycopy(original,0,p.labels,0,original.length);assertTrue(p.hasSmall());
    }
    @Test public void broadCurvedBandIsNotAThinEntrance(){var p=new Page(true,false);p.curve(5);assertTrue(p.hasSmall());}
    @Test public void labelShapeAloneIsInsufficient(){var p=new Page(true,true);assertEquals(2,OmrScoreInterpreter.analyze(p.labels,null,W,H,M).notes().size());}
    @Test public void sourceArraysRemainUnchanged(){var p=new Page(true,true);var g=p.gray.clone();var l=p.labels.clone();p.read();assertArrayEquals(l,p.labels);assertArrayEquals(g,p.gray);}
}
