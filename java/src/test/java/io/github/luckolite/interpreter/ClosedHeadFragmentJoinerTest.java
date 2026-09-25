// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;
public class ClosedHeadFragmentJoinerTest {
    private static final int W=160,H=100;
    private byte[] gray(int interior) {
        byte[] result=new byte[W*H];Arrays.fill(result,(byte)240);
        for(int y=35;y<=65;y++)for(int x=60;x<=100;x++) {
            double e=(x-80)*(x-80)/196.0+(y-50)*(y-50)/100.0;
            if(e<=1)result[y*W+x]=50;
            if((x-80)*(x-80)/121.0+(y-50)*(y-50)/49.0<=1)result[y*W+x]=(byte)interior;
        }
        return result;
    }
    private ClosedHeadFragmentJoiner.Box a(){return new ClosedHeadFragmentJoiner.Box(67,44,79,58,73,51);}
    private ClosedHeadFragmentJoiner.Box b(){return new ClosedHeadFragmentJoiner.Box(80,42,91,56,86,49);}
    @Test public void paleFilledRingIsOneHead(){var joined=ClosedHeadFragmentJoiner.join(gray(178),W,H,20,a(),b());assertNotNull(joined);assertEquals(80,joined.x(),.1);assertEquals(50,joined.y(),.1);assertTrue(joined.filled());}
    @Test public void hollowRingAlsoJoins(){var joined=ClosedHeadFragmentJoiner.join(gray(240),W,H,20,a(),b());assertNotNull(joined);assertFalse(joined.filled());}
    @Test public void rawClosedBoundaryIsRequired(){byte[] g=gray(178);for(int x=60;x<=100;x++)g[50*W+x]=(byte)240;assertNull(ClosedHeadFragmentJoiner.join(g,W,H,20,a(),b()));}
    @Test public void twoVerticalChordHeadsRemainDistinct(){var lower=new ClosedHeadFragmentJoiner.Box(80,58,91,72,86,66);assertNull(ClosedHeadFragmentJoiner.join(gray(178),W,H,20,a(),lower));}
    @Test public void ordinaryFullSizeHeadsAreNotFragments(){var whole=new ClosedHeadFragmentJoiner.Box(67,40,94,60,80,50);assertNull(ClosedHeadFragmentJoiner.join(gray(178),W,H,20,whole,b()));}
    @Test public void separateUnisonHeadsRemainDistinct(){var right=new ClosedHeadFragmentJoiner.Box(95,42,106,56,101,49);assertNull(ClosedHeadFragmentJoiner.join(gray(178),W,H,20,a(),right));}
    @Test public void blankPaperHasNoClosedPocket(){byte[] g=new byte[W*H];Arrays.fill(g,(byte)240);assertNull(ClosedHeadFragmentJoiner.join(g,W,H,20,a(),b()));}
    @Test public void invalidInputDoesNotJoin(){assertNull(ClosedHeadFragmentJoiner.join(null,W,H,20,a(),b()));}
}
