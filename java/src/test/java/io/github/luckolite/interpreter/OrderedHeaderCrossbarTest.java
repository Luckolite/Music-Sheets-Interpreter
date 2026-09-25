// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original fragmented F-sharp/C-sharp header and independent-note controls. */
public class OrderedHeaderCrossbarTest {
    HeaderCrossbarOwnershipTest page(boolean first,boolean connected,int offset) {
        var f=new HeaderCrossbarOwnershipTest().page(true,true,true,0);
        if(!first)for(int y=77;y<=123;y++)for(int x=83;x<=105;x++){f.gray[y*f.W+x]=(byte)255;f.labels[y*f.W+x]=0;}
        if(connected)for(int y=104+offset;y<=144+offset;y++)for(int x=117;x<=118;x++)f.ink(x,y,5,220);
        for(int cy:new int[]{116+offset,132+offset})for(int y=cy-2;y<=cy+2;y++)for(int x=110;x<=126;x++)f.ink(x,y,x>=114&&x<=122?2:5,0);
        return f;
    }
    boolean removed(HeaderCrossbarOwnershipTest f){byte[] n=f.normalized();return n[116*f.W+118]!=2&&n[132*f.W+118]!=2;}
    @Test public void orderedSecondSharpCrossbarsAreNotNotes(){assertTrue(removed(page(true,true,0)));}
    @Test public void noPrecedingSharpDoesNotAuthorizeSecondSlot(){assertFalse(removed(page(false,true,0)));}
    @Test public void disconnectedSecondBarsArePreserved(){assertFalse(removed(page(true,false,0)));}
    @Test public void arbitrarySlotRemainsProtected(){var f=page(true,true,8);byte[] n=f.normalized();assertEquals(2,n[124*f.W+118]);assertEquals(2,n[140*f.W+118]);}
    @Test public void attachedStemProtectsSmallChord(){var f=page(true,true,0);for(int y=65;y<=132;y++)f.ink(122,y,1,170);assertFalse(removed(f));}
    @Test public void followingRealHeadRemainsUntouched(){var f=page(true,true,0);byte[] n=f.normalized();for(int y=125;y<=139;y++)for(int x=139;x<=161;x++)assertEquals(f.labels[y*f.W+x],n[y*f.W+x]);}
}
