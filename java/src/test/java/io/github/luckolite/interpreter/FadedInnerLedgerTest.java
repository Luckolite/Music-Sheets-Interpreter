// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original synthetic ledger strokes with a faded middle or unsupported pale line. */
public final class FadedInnerLedgerTest {
    private RemoteLedgerEvidenceTest.Page page(boolean above,int shade,boolean allFaint,boolean complete) {
        var p=new RemoteLedgerEvidenceTest.Page(above,false,complete,true,false);
        int y=p.y+p.direction*2*RemoteLedgerEvidenceTest.GAP;
        for(int x=86;x<=124;x++) {
            boolean faint=allFaint||(x>=99&&x<=110);
            if(faint)p.gray[y*RemoteLedgerEvidenceTest.W+x]=(byte)shade;
        }
        return p;
    }
    @Test public void fadedMiddleOfUpperLedgerPreservesNote() {
        var n=page(true,175,false,true).remote();assertNotNull(n);assertEquals(16,n.staffStep());
    }
    @Test public void fadedMiddleOfLowerLedgerPreservesNote() {
        var n=page(false,175,false,true).remote();assertNotNull(n);assertEquals(-8,n.staffStep());
    }
    @Test public void missingMiddleStillRejectsDisconnectedFragments() {
        assertNull(page(true,255,false,true).remote());
    }
    @Test public void entirelyPaleThirdStrokeCannotValidateAnUnderline() {
        assertNull(page(true,175,true,false).remote());
    }
    @Test public void mostlyPaleStrokeLacksStrongLedgerSupport() {
        var p=page(false,175,true,false);
        for(int x=101;x<=110;x++)p.gray[(p.y+p.direction*32)*RemoteLedgerEvidenceTest.W+x]=0;
        assertNull(p.remote());
    }
    @Test public void strongOrdinaryLedgersRemainUnchanged() {
        assertEquals(16,page(true,0,false,true).remote().staffStep());
    }
}
