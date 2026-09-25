// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
/** Original crease-clipped head/stem, surviving beam caps and ruled-page controls. */
public class CreaseBeamAttachmentTest {
 static final int W=240,H=220;final byte[] g=new byte[W*H];
 public CreaseBeamAttachmentTest(){Arrays.fill(g,(byte)255);}
 void rect(int a,int b,int c,int d,int v){for(int y=c;y<=d;y++)for(int x=a;x<=b;x++)g[y*W+x]=(byte)v;}
 void setup(){for(int y=96;y<=160;y+=16)rect(40,200,y,y+1,45);rect(100,113,154,168,45);rect(120,120,80,158,45);rect(60,120,80,85,45);rect(60,120,92,97,45);rect(111,116,70,170,255);}
 int count(){return CreaseBeamAttachment.count(g,W,H,16,96,160,100,113,154,168,161);}
 @Test public void erasedRulesAndTwoSurvivingCapsReconnectTheClippedHead(){setup();assertEquals(2,count());}
 @Test public void intactRulesDoNotAuthorizeDetachedStemBorrowing(){setup();for(int y=96;y<=160;y+=16)rect(111,116,y,y+1,45);assertEquals(0,count());}
 @Test public void OnlyTwoErasedRulesAreNotASufficientCrease(){setup();for(int y=96;y<=128;y+=16)rect(111,116,y,y+1,45);assertEquals(0,count());}
 @Test public void singleCapDoesNotInventSecondBeam(){setup();rect(114,119,92,97,255);assertEquals(0,count());}
 @Test public void disconnectedShaftDoesNotBorrowCaps(){setup();rect(120,120,112,126,255);assertEquals(0,count());}
 @Test public void shortGraceHeightIsRejected(){setup();assertEquals(0,CreaseBeamAttachment.count(g,W,H,16,96,160,100,113,157,164,161));}
 @Test public void unalignedLeftBeamDoesNotProveThePair(){setup();rect(94,103,92,97,255);assertEquals(0,count());}
 @Test public void veryWideOvalIsNotAClippedHead(){setup();assertEquals(0,CreaseBeamAttachment.count(g,W,H,16,96,160,90,113,154,168,161));}
 @Test public void blankImageHasNoBeam(){assertEquals(0,count());}
 @Test public void invalidBoundsAreRejected(){assertEquals(0,CreaseBeamAttachment.count(g,W,H,16,Float.NaN,160,100,113,154,168,161));assertEquals(0,CreaseBeamAttachment.count(g,W,H,16,96,160,-1,113,154,168,161));assertEquals(0,CreaseBeamAttachment.count(new byte[1],W,H,16,96,160,100,113,154,168,161));}
 @Test public void pixelsAreUnchanged(){setup();byte[] old=g.clone();count();assertArrayEquals(old,g);}
}
