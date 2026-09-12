// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original remote ledger examples with an additional, closer staff. */
public class DistantLedgerOwnerTest {
    static RemoteLedgerEvidenceTest.Page page(boolean above,boolean complete,boolean hollow){
        var p=new RemoteLedgerEvidenceTest.Page(above,false,complete,!hollow,hollow);
        int top=above?16:288;
        for(int y=top;y<=top+32;y+=8)p.rule(14,465,y,4);
        p.head(350,top+16,6,4,false);p.stem(356,top+16,top+2);
        return p;
    }
    @Test public void highChordToneKeepsDistantPrintedStaff(){var n=page(true,true,false).remote();assertNotNull(n);assertEquals(16,n.staffStep());}
    @Test public void highWholeHeadDoesNotNeedAStemToIdentifyItsStaff(){var n=page(true,true,true).remote();assertNotNull(n);assertEquals(16,n.staffStep());}
    @Test public void lowWholeHeadDoesNotMoveToFollowingSystem(){var n=page(false,true,true).remote();assertNotNull(n);assertEquals(-8,n.staffStep());}
    @Test public void singleCoincidentRuleDoesNotOutvoteCompleteLedgerChain(){var p=page(true,true,false);p.rule(86,124,56,5);var n=p.remote();assertNotNull(n);assertEquals(16,n.staffStep());}
    @Test public void incompleteDistantLedgersDoNotInventAnExtremePitch(){var n=page(true,false,false).remote();assertTrue(n==null||n.staffStep()!=16);}
    @Test public void wideRulesDoNotIdentifyDistantStaff(){var p=page(true,true,false);for(int y:new int[]{80,96,112})p.rule(14,465,y,5);var n=p.remote();assertTrue(n==null||n.staffStep()!=16);}
    @Test public void inputMasksRemainUnchanged(){var p=page(false,true,true);var l=p.labels.clone();var g=p.gray.clone();p.remote();assertArrayEquals(l,p.labels);assertArrayEquals(g,p.gray);}
}
