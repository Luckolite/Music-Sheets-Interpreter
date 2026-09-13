// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original row layouts with competing readings and one absent number. */
public class SkippedAnchorAmbiguityTest {
    private static List<MeasureRegion> layout(int... counts) {
        var out=new ArrayList<MeasureRegion>();
        for(int r=0;r<counts.length;r++)for(int m=0;m<counts[r];m++)
            out.add(new MeasureRegion(.15f+m*.75f/counts[r],.15f+(m+1)*.75f/counts[r],.1f+r*.1f,.16f+r*.1f));
        return out;
    }
    private static MeasureNumberReconciler.NumberToken number(int value,int row) {
        float y=.1f+row*.1f;
        return new MeasureNumberReconciler.NumberToken(value,.08f,y-.025f,.1f,y-.01f);
    }
    private static void counts(List<MeasureRegion> result,int... expected) {
        assertEquals(Arrays.stream(expected).sum(),result.size());
        for(int r=0;r<expected.length;r++){float y=.1f+r*.1f;assertEquals(expected[r],result.stream().filter(m->Math.abs(m.top()-y)<.01f).count());}
    }
    @Test public void ambiguousNumberUsesBothRowsBeforeTheNextAnchor() {
        var raw=layout(5,5,5,5,5);
        counts(MeasureNumberReconciler.reconcile(raw,List.of(number(9,0),number(6,0),number(16,2),number(21,3),number(26,4))),5,5,5,5,5);
    }
    @Test public void orderOfCompetingReadingsDoesNotChooseTheAnswer() {
        var raw=layout(5,5,5,5,5);
        counts(MeasureNumberReconciler.reconcile(raw,List.of(number(6,0),number(9,0),number(16,2),number(21,3),number(26,4))),5,5,5,5,5);
    }
    @Test public void unequalRowsContributeTheirOwnCounts() {
        var raw=layout(4,6,5,5,5);
        counts(MeasureNumberReconciler.reconcile(raw,List.of(number(7,0),number(3,0),number(13,2),number(18,3),number(23,4))),4,6,5,5,5);
    }
    @Test public void adjacentAnchorStillUsesOneRow() {
        var raw=layout(4,4,4,4);
        counts(MeasureNumberReconciler.reconcile(raw,List.of(number(6,0),number(9,0),number(13,1),number(17,2),number(21,3))),4,4,4,4);
    }
    @Test public void consistentPrintedNumbersCanOverrideDamagedBarDetection() {
        var raw=layout(4,6,5,5,5);
        counts(MeasureNumberReconciler.reconcile(raw,List.of(number(1,0),number(5,1),number(9,2),number(13,3),number(17,4))),4,4,4,4,5);
    }
    @Test public void inputRegionsRemainUnchanged() {
        var raw=layout(5,5,5,5);var before=List.copyOf(raw);
        MeasureNumberReconciler.reconcile(raw,List.of(number(6,0),number(9,0),number(16,2),number(21,3)));
        assertEquals(before,raw);
    }
}
