// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original sloping beam fixtures; no score-derived pixels. */
public class StemOwnedBeamTipTest {
    private static final int W=300,H=240;private final byte[] gray=new byte[W*H];
    private final int[] left={100,150,1},right={160,134,1};
    public StemOwnedBeamTipTest(){Arrays.fill(gray,(byte)220);beam(70);}
    private void beam(int value){for(int x=100;x<=160;x++){int bottom=150-Math.round((x-100)*16f/60);for(int y=bottom-7;y<=bottom;y++)gray[y*W+x]=(byte)value;}}
    private boolean match(){return StemOwnedBeamTip.matches(gray,W,H,left,right,157,132,10,7,45,16);}
    @Test public void completeOwnedBeamRejectsTinyTip(){assertTrue(match());}
    @Test public void grayPaperDoesNotBecomeWideInk(){beam(130);assertTrue(match());}
    @Test public void twoStemOrderDoesNotMatter(){assertTrue(StemOwnedBeamTip.matches(gray,W,H,right,left,157,132,10,7,45,16));}
    @Test public void brokenBeamCannotEstablishOwnership(){for(int x=119;x<=145;x++)for(int y=125;y<=155;y++)gray[y*W+x]=(byte)220;assertFalse(match());}
    @Test public void roundedHeadBulgeSurvives(){for(int y=117;y<=143;y++)for(int x=145;x<=169;x++)if(Math.pow((x-156)/12d,2)+Math.pow((y-130)/13d,2)<=1)gray[y*W+x]=0;assertFalse(match());}
    @Test public void fullSizedHeadSurvives(){assertFalse(StemOwnedBeamTip.matches(gray,W,H,left,right,157,132,19,13,190,16));}
    @Test public void oppositeStemDirectionsAreNotTheSameBeam(){assertFalse(StemOwnedBeamTip.matches(gray,W,H,left,new int[]{160,134,-1},157,132,10,7,45,16));}
    @Test public void inputIsNotModified(){byte[] before=gray.clone();match();assertArrayEquals(before,gray);}
}
