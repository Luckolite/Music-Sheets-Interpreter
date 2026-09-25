// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometric 4/4 after a blank slot, plus protected accidental ink. */
public final class ExplicitEmptySignatureTest {
    private static final int W=240,H=150;
    private byte[] page(boolean meter,boolean accidental) {
        byte[] p=new byte[W*H];Arrays.fill(p,(byte)255);
        for(int y=40;y<=96;y+=14)rect(p,20,210,y,y,0);
        if(meter) {
            rect(p,100,102,40,96,0);
            for(int y:new int[]{55,83}) {
                rect(p,91,102,y-2,y+6,0);
                for(int row=0;row<5;row++)rect(p,98-Math.min(6,row+2)+1,98,y+row,y+row,255);
            }
        }
        if(accidental){rect(p,75,77,49,79,0);rect(p,77,84,65,69,0);}
        return p;
    }
    private void rect(byte[] p,int l,int r,int t,int b,int value){for(int y=t;y<=b;y++)for(int x=l;x<=r;x++)p[y*W+x]=(byte)value;}
    private boolean blank(byte[] p){return ExplicitEmptySignature.matches(p,W,H,60,140,96,14);}
    private byte[] openFours(boolean crossbars,boolean accidental) {
        byte[] p=page(false,accidental);
        for(int digit=0;digit<2;digit++) {
            int t=40+digit*28;
            for(int dy=8;dy<=19;dy++){int x=94-Math.round((dy-8)*.7f);rect(p,x,x+3,t+dy,t+dy,0);rect(p,99,103,t+dy,t+dy,0);}
            if(crossbars)rect(p,86,105,t+20,t+22,0);
            rect(p,99,103,t+23,t+26,0);
        }
        return p;
    }
    @Test public void verifiedFourFourAfterBlankSlotProvesEmptySignature(){assertTrue(blank(page(true,false)));}
    @Test public void accidentalInkPreventsEmptySignature(){assertFalse(blank(page(true,true)));}
    @Test public void missingMeterLeavesContinuationUnknown(){assertFalse(blank(page(false,false)));}
    @Test public void headerMustHaveRoomBeforeFirstNote(){assertFalse(ExplicitEmptySignature.matches(page(true,false),W,H,60,78,96,14));}
    @Test public void distantMeterDoesNotProveClefKeyAdjacency(){assertFalse(ExplicitEmptySignature.matches(page(true,false),W,H,20,140,96,14));}
    @Test public void unknownFirstNoteIsNotEnoughEvidence(){assertFalse(ExplicitEmptySignature.matches(page(true,false),W,H,60,Float.POSITIVE_INFINITY,96,14));}
    @Test public void sourcePixelsRemainUnchanged(){byte[] p=page(true,false),before=p.clone();blank(p);assertArrayEquals(before,p);}
    @Test public void openFourPairAlsoProvesExplicitMeter(){assertTrue(blank(openFours(true,false)));}
    @Test public void openFourPairDoesNotIgnoreAccidental(){assertFalse(blank(openFours(true,true)));}
    @Test public void separatedArmsWithoutCrossbarsAreNotFours(){assertFalse(blank(openFours(false,false)));}
}
