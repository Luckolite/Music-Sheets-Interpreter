// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original three diagonal stem strokes and confusable straight/round shapes. */
public final class AttachedTremoloInkTest {
    private static final int W=240,H=200;
    private static byte[] page(int strokes,int span,float slope,int tone) {
        byte[] p=new byte[W*H];Arrays.fill(p,(byte)255);
        for(int y=60;y<=148;y++)p[y*W+100]=(byte)tone;
        for(int i=0;i<strokes;i++)for(int dx=-span;dx<=span;dx++)for(int dy=-2;dy<=2;dy++) {
            int y=91+i*11+Math.round(dx*slope)+dy;p[y*W+100+dx]=(byte)tone;
        }
        return p;
    }
    private static AttachedTremoloInk.Mark find(byte[] p){return AttachedTremoloInk.find(p,W,H,100,68,148,1,14);}
    @Test public void threeShortDiagonalStrokesAreConfirmed(){assertNotNull(find(page(3,10,-.3f,0)));}
    @Test public void paleStrokesRemainRecognizable(){assertNotNull(find(page(3,10,-.3f,170)));}
    @Test public void noStrokesMeansNoTremolo(){assertNull(find(page(0,10,-.3f,0)));}
    @Test public void isolatedStrokeIsNotTripleTremolo(){assertNull(find(page(1,10,-.3f,0)));}
    @Test public void twoStrokesAreNotAssumedToBeThree(){assertNull(find(page(2,10,-.3f,0)));}
    @Test public void horizontalLedgerLinesAreNotDiagonalStrokes(){assertNull(find(page(3,10,0,0)));}
    @Test public void longRhythmicBeamsAreNotTremolo(){assertNull(find(page(3,60,-.3f,0)));}
    @Test public void disconnectedRoundHeadsAreNotDiagonalStrokes(){byte[] p=page(0,10,0,0);for(int cy:new int[]{91,102,113})for(int y=cy-4;y<=cy+4;y++)for(int x=92;x<=108;x++)if(Math.pow((x-100)/8.,2)+Math.pow((y-cy)/4.,2)<=1)p[y*W+x]=0;assertNull(find(p));}
    @Test public void fadedOwningStemCanBeVerified(){assertNotNull(AttachedTremoloInk.fadedStem(page(3,10,-.3f,170),W,H,100,55,120,68,14));}
    @Test public void isolatedStrokesDoNotInventAnOwningStem(){byte[] p=page(3,10,-.3f,0);for(int y=60;y<=148;y++)p[y*W+100]=(byte)255;assertNull(AttachedTremoloInk.fadedStem(p,W,H,100,55,120,68,14));}
    @Test public void sourcePixelsArePreserved(){byte[] p=page(3,10,-.3f,170),copy=p.clone();find(p);assertArrayEquals(copy,p);}
    @Test public void malformedImageIsRejected(){assertNull(AttachedTremoloInk.find(new byte[1],W,H,100,68,148,1,14));}
}
