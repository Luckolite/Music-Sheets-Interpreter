// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

public class PaleChordHeadRecoveryTest {
    private static final int W=220,H=180,GAP=20;
    private static final class Fixture {
        final byte[] gray=new byte[W*H],labels=new byte[W*H];
        Fixture(int count,int interior,boolean stem) {
            Arrays.fill(gray,(byte)238);
            for(int i=0;i<count;i++)ellipse(100,50+i*GAP,13,11,55);
            rectangle(91,50,113,50+(count-1)*GAP,55);
            for(int i=0;i<count;i++)ellipse(100,50+i*GAP,11,8,interior);
            rectangle(93,50,110,50+(count-1)*GAP,interior);
            if(stem)for(int y=5;y<=50;y++)for(int x=112;x<=114;x++) {
                gray[y*W+x]=55;labels[y*W+x]=OmrMeasurePostProcessor.STEM_OR_REST;
            }
        }
        void ellipse(int cx,int cy,int rx,int ry,int value) {
            for(int y=cy-ry;y<=cy+ry;y++)for(int x=cx-rx;x<=cx+rx;x++)
                if((x-cx)*(x-cx)/(float)(rx*rx)+(y-cy)*(y-cy)/(float)(ry*ry)<=1)gray[y*W+x]=(byte)value;
        }
        void rectangle(int left,int top,int right,int bottom,int value) {
            for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++)gray[y*W+x]=(byte)value;
        }
        java.util.List<PaleChordHeadRecovery.Head> find(){return PaleChordHeadRecovery.find(labels,gray,W,H,GAP,0,H-1);}
    }
    @Test public void threeTouchingPaleHeadsHaveIndependentCenters() {
        var heads=new Fixture(3,178,true).find();assertEquals(3,heads.size());
        assertEquals(50,heads.get(0).centerY(),1);assertEquals(70,heads.get(1).centerY(),1);assertEquals(90,heads.get(2).centerY(),1);
    }
    @Test public void twoTouchingPaleHeadsAreRecovered(){assertEquals(2,new Fixture(2,178,true).find().size());}
    @Test public void fourTouchingPaleHeadsAreRecovered(){assertEquals(4,new Fixture(4,178,true).find().size());}
    @Test public void whiteHollowPocketIsNotPaleFilledChord(){assertTrue(new Fixture(3,238,true).find().isEmpty());}
    @Test public void anIndependentPrintedStemIsRequired(){assertTrue(new Fixture(3,178,false).find().isEmpty());}
    @Test public void semanticStemAloneDoesNotInventInk(){var f=new Fixture(3,178,true);f.rectangle(112,5,114,30,238);assertTrue(f.find().isEmpty());}
    @Test public void rawStemAloneDoesNotInventChord(){var f=new Fixture(3,178,true);Arrays.fill(f.labels,(byte)0);assertTrue(f.find().isEmpty());}
    @Test public void flatStaffCellCannotReplaceLobes(){var f=new Fixture(3,178,true);f.rectangle(85,39,115,101,55);f.rectangle(89,42,111,98,178);assertTrue(f.find().isEmpty());}
    @Test public void openPaperRegionIsNotAHead(){var f=new Fixture(3,178,true);f.rectangle(85,45,100,46,238);assertTrue(f.find().isEmpty());}
    @Test public void singleHeadIsOutsideThisRecovery(){assertTrue(new Fixture(1,178,true).find().isEmpty());}
}
