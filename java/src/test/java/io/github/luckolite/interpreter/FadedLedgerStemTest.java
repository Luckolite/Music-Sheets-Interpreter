// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import static org.junit.Assert.*;

/** Original geometry: a short outer ledger and a lighter, continuous attached stem. */
public class FadedLedgerStemTest {
    private RemoteLedgerEvidenceTest.Page page(boolean above,boolean complete,boolean stem) {
        var p=new RemoteLedgerEvidenceTest.Page(above,false,complete,stem,false);
        int w=RemoteLedgerEvidenceTest.W;
        // The outer rule reaches one oval edge, while the inner ledgers remain complete.
        for(int x=86;x<=124;x++)if(above?x>116||x<92:x<94||x>118) {
            p.gray[p.y*w+x]=(byte)255;p.labels[p.y*w+x]=0;
        }
        for(int i=0;i<p.gray.length;i++) {
            if((p.gray[i]&255)==255)p.gray[i]=(byte)250;
            else p.gray[i]=(byte)(p.labels[i]==2?110:p.labels[i]==1?190:180);
        }
        return p;
    }
    @Test public void fadedUpperStemKeepsLedgerPitch() {
        var n=page(true,true,true).remote();assertNotNull(n);assertEquals(16,n.staffStep());
    }
    @Test public void fadedLowerStemKeepsLedgerPitch() {
        var n=page(false,true,true).remote();assertNotNull(n);assertEquals(-8,n.staffStep());
    }
    @Test public void shortRuleWithoutStemStillRejectsInstructionInk() {
        assertNull(page(true,true,false).remote());assertNull(page(false,true,false).remote());
    }
    @Test public void fadedStemCannotReplaceMissingInnerLedgers() {
        assertNull(page(true,false,true).remote());assertNull(page(false,false,true).remote());
    }
    @Test public void stemLabelsWithoutPrintedInkDoNotValidateHead() {
        var p=page(true,true,true);
        for(int i=0;i<p.labels.length;i++)if(p.labels[i]==1)p.gray[i]=(byte)250;
        assertNull(p.remote());
    }
    @Test public void inputPixelsAndLabelsRemainUnchanged() {
        var p=page(true,true,true);byte[] gray=p.gray.clone(),labels=p.labels.clone();
        p.remote();assertArrayEquals(gray,p.gray);assertArrayEquals(labels,p.labels);
    }
}
