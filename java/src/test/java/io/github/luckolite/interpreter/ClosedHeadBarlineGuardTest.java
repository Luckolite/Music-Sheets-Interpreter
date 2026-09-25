// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
public class ClosedHeadBarlineGuardTest {
    private static final int W=220,H=180;
    private byte[] page(int fill,boolean head,boolean attached) {
        byte[] g=new byte[W*H];Arrays.fill(g,(byte)238);
        if(head)for(int y=120;y<=140;y++)for(int x=65;x<=95;x++) {
            if((x-80)*(x-80)/169.0+(y-130)*(y-130)/100.0<=1)g[y*W+x]=45;
            if((x-80)*(x-80)/121.0+(y-130)*(y-130)/49.0<=1)g[y*W+x]=(byte)fill;
        }
        int sx=attached?93:102;
        for(int y=60;y<=140;y++)for(int x=sx;x<=sx+2;x++)g[y*W+x]=45;
        return g;
    }
    @Test public void missingPaleHeadOwnsItsStem(){assertTrue(ClosedHeadBarlineGuard.attached(page(178,true,true),W,H,94,60,140,20));}
    @Test public void DarkerGreyInteriorStillOwnsStem(){assertTrue(ClosedHeadBarlineGuard.attached(page(145,true,true),W,H,94,60,140,20));}
    @Test public void isolatedPrintedBarlineSurvives(){assertFalse(ClosedHeadBarlineGuard.attached(page(178,false,true),W,H,94,60,140,20));}
    @Test public void nearbyButDetachedHeadDoesNotOwnBarline(){assertFalse(ClosedHeadBarlineGuard.attached(page(178,true,false),W,H,103,60,140,20));}
    @Test public void whiteTextCounterDoesNotBecomeGreyHead(){assertFalse(ClosedHeadBarlineGuard.attached(page(238,true,true),W,H,94,60,140,20));}
    @Test public void shadedPaperEnclosureDoesNotOwnBarline(){
        byte[] g=page(186,true,true);
        for(int i=0;i<g.length;i++)if((g[i]&255)==238)g[i]=(byte)196;
        assertFalse(ClosedHeadBarlineGuard.attached(g,W,H,94,60,140,20));
    }
    @Test public void tieCrossingBarlineAndStaffRuleDoesNotOwnBarline(){
        byte[] g=page(178,false,true);
        for(int y=60;y<=140;y+=20)for(int dy=-1;dy<=1;dy++)for(int x=10;x<210;x++)g[(y+dy)*W+x]=45;
        for(int x=50;x<=140;x++){
            int y=Math.round(120-9*(1-(x-95)*(x-95)/2025f));
            for(int dy=-1;dy<=1;dy++)g[(y+dy)*W+x]=45;
        }
        assertFalse(ClosedHeadBarlineGuard.attached(g,W,H,94,60,140,20));
    }
    @Test public void brokenOutlineStillHasBroadFilledOval(){
        byte[] g=page(178,true,true);
        for(int y=120;y<=124;y++)for(int x=78;x<=80;x++)g[y*W+x]=(byte)238;
        assertTrue(ClosedHeadBarlineGuard.attached(g,W,H,94,60,140,20));
    }
    @Test public void corroboratedBoundaryOnBothStavesSurvives(){
        assertEquals(List.of(10,94,200),ClosedHeadBarlineGuard.withoutOwnedByOtherStaff(List.of(10,94,200),List.of(10,95,200),page(178,true,true),W,H,60,140,20));
    }
    @Test public void oppositeStaffHeadVetoesOnlyUncorroboratedInterior(){
        assertEquals(List.of(10,200),ClosedHeadBarlineGuard.withoutOwnedByOtherStaff(List.of(10,94,200),List.of(10,200),page(178,true,true),W,H,60,140,20));
    }
    @Test public void outerStaffExtentIsNeverVetoed(){
        assertEquals(List.of(94,200),ClosedHeadBarlineGuard.withoutOwnedByOtherStaff(List.of(94,200),List.of(10,200),page(178,true,true),W,H,60,140,20));
    }
}
