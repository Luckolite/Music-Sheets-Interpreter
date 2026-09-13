// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/** Original geometry: elevated tempo equations and similar written-note controls. */
public class ElevatedTempoUnitTest {
    private TempoUnitNoteTest.Page elevated() {
        var p=new TempoUnitNoteTest.Page();
        p.erase(20,100,460,65);
        for(int y=180;y<=244;y+=16)p.rect(20,y,460,1,(byte)4);
        return p;
    }
    private void lowerText(TempoUnitNoteTest.Page p) {
        p.erase(162,60,16,31);
        p.rect(164,68,3,27,(byte)5);p.rect(164,68,12,3,(byte)5);
        p.rect(164,80,12,3,(byte)5);p.rect(164,92,12,3,(byte)5);
        p.rect(173,80,3,15,(byte)5);
    }
    @Test public void tempoEquationCanSitAboveTheUsualLedgerRange() {
        var p=elevated();assertTrue(p.heads(p.labels)>0);assertEquals(0,p.heads(p.normalized()));
    }
    @Test public void bpmDigitsCanExtendBelowTheBeatHeadCenter() {
        var p=new TempoUnitNoteTest.Page();lowerText(p);assertEquals(0,p.heads(p.normalized()));
    }
    @Test public void elevatedEquationWithLowerTextIsRemovedWithoutMutatingInputs() {
        var p=elevated();lowerText(p);byte[] before=p.labels.clone(),gray=p.gray.clone();
        assertEquals(0,p.heads(p.normalized()));assertArrayEquals(before,p.labels);assertArrayEquals(gray,p.gray);
    }
    @Test public void highWrittenNoteWithoutEqualsIsRetained() {
        var p=elevated();p.erase(130,72,21,12);assertArrayEquals(p.labels,p.normalized());
    }
    @Test public void highWrittenNoteWithOneStrokeIsRetained() {
        var p=elevated();p.erase(131,80,19,3);assertArrayEquals(p.labels,p.normalized());
    }
    @Test public void elevatedEqualsWithoutBpmTextIsInsufficient() {
        var p=elevated();p.erase(162,60,16,31);assertArrayEquals(p.labels,p.normalized());
    }
    @Test public void elevatedLedgerSpacedStrokesAreNotEquals() {
        var p=elevated();p.erase(130,72,21,12);
        p.rect(131,64,19,3,(byte)5);p.rect(131,80,19,3,(byte)5);
        assertArrayEquals(p.labels,p.normalized());
    }
    @Test public void tempoInkDoesNotBlockAVerifiedRestCount() {
        var p=elevated();lowerText(p);
        p.rect(60,207,110,8,(byte)5);p.rect(60,200,3,24,(byte)5);p.rect(167,200,3,24,(byte)5);
        var measures=List.of(new MeasureRegion(.04f,.94f,164f/p.h,256f/p.h));
        var count=new MeasureNumberReconciler.NumberToken(4,105f/p.w,148f/p.h,117f/p.w,163f/p.h);
        assertTrue(MultiMeasureRestDetector.detect(p.labels,p.gray,p.w,p.h,measures,List.of(count)).isEmpty());
        var clean=OmrScoreInterpreter.normalizeHeaderSymbols(p.labels,p.gray,p.w,p.h,measures);
        assertEquals(List.of(count),MultiMeasureRestDetector.detect(clean,p.gray,p.w,p.h,measures,List.of(count)));
    }
}
