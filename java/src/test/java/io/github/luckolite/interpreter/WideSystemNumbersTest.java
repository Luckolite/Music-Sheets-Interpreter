// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class WideSystemNumbersTest {
    private List<MeasureRegion> score(float[] tops,int[] counts) {
        List<MeasureRegion> result=new ArrayList<>();
        for(int r=0;r<tops.length;r++)for(int n=0;n<counts[r];n++)
            result.add(new MeasureRegion(.17f+.71f*n/counts[r],.17f+.71f*(n+1)/counts[r],tops[r],tops[r]+.15f));
        return result;
    }
    private MeasureNumberReconciler.NumberToken number(int value,float top) {
        return new MeasureNumberReconciler.NumberToken(value,.13f,top-.012f,.15f,top-.002f);
    }
    @Test public void largeGrandStaffGapsKeepAllFourteenMeasures() {
        var regions=score(new float[]{.09f,.32f,.53f,.78f},new int[]{4,2,4,4});
        var tokens=List.of(number(45,.09f),number(49,.32f),number(51,.53f),number(55,.78f));
        assertEquals(regions,MeasureNumberReconciler.reconcile(regions,tokens));
    }
    @Test public void twoWidelySpacedSystemsCanEstablishTheirStartingMeasure() {
        var regions=score(new float[]{.10f,.51f},new int[]{3,3});
        var tokens=List.of(number(24,.10f),number(27,.51f));
        assertEquals(24,MeasureNumberReconciler.firstMeasureNumber(regions,tokens));
    }
    @Test public void distantFooterNumberDoesNotBecomeAStaffAnchor() {
        var regions=score(new float[]{.10f,.36f},new int[]{3,3});
        var tokens=List.of(number(24,.10f),number(27,.36f),number(30,.90f));
        assertEquals(regions,MeasureNumberReconciler.reconcile(regions,tokens));
    }
    @Test public void wideGapNeedsAgreementWithVisibleBarCount() {
        var regions=score(new float[]{.10f,.51f},new int[]{3,3});
        var tokens=List.of(number(24,.10f),number(29,.51f));
        assertEquals(regions,MeasureNumberReconciler.reconcile(regions,tokens));
        assertEquals(0,MeasureNumberReconciler.firstMeasureNumber(regions,tokens));
    }
}
