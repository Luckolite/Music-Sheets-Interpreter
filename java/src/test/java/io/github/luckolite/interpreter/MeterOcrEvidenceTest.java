// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class MeterOcrEvidenceTest {
    private static MeterOcrEvidence.Token token(String text,int x,int y,int width,int height) {
        return new MeterOcrEvidence.Token(text,x,y,x+width,y+height);
    }
    @Test public void joinsHorizontalFractionFragments() {
        assertEquals("3/8",MeterOcrEvidence.horizontalFraction(List.of(token("3",10,12,30,80),token("/8",70,14,70,78))));
        assertEquals("4/4",MeterOcrEvidence.horizontalFraction(List.of(token("4",10,12,30,80),token("|4",60,12,70,80))));
        assertEquals("12/8",MeterOcrEvidence.horizontalFraction(List.of(token("12",10,12,40,80),token("/",70,12,10,80),token("8",100,12,30,80))));
    }
    @Test public void doesNotSplitATwoDigitNumeratorIntoAStackedMeter() {
        assertEquals("12/8",MeterOcrEvidence.horizontalFraction(List.of(token("12/",10,10,120,80),token("8",170,12,25,78))));
        assertEquals("",MeterOcrEvidence.horizontalFraction(List.of(token("12/",10,10,120,80))));
    }
    @Test public void rejectsStackedDistantAndOverlappingFragments() {
        assertEquals("",MeterOcrEvidence.horizontalFraction(List.of(token("3",10,0,30,80),token("/8",10,90,70,80))));
        assertEquals("",MeterOcrEvidence.horizontalFraction(List.of(token("3",10,0,30,80),token("/8",200,0,70,80))));
        assertEquals("",MeterOcrEvidence.horizontalFraction(List.of(token("3",10,0,30,80),token("/8",30,0,70,80))));
    }
    @Test public void doesNotInventDigitsOrSeparators() {
        for(String text:List.of("414","12/","12","B/8","3/3","33/4","0/4","4/0"))
            assertEquals(text,"",MeterOcrEvidence.horizontalFraction(List.of(token(text,0,0,100,80))));
    }
    @Test public void rejectsContradictoryFractions() {
        assertEquals("",MeterOcrEvidence.horizontalFraction(List.of(token("3/8",0,0,100,80),token("8/8",120,0,100,80))));
        assertEquals("",MeterOcrEvidence.consensus(List.of("3/8","3/8","8/8")));
    }
    @Test public void requiresRepeatedValidEvidence() {
        assertEquals("",MeterOcrEvidence.consensus(List.of("","3/8","")));
        assertEquals("3/8",MeterOcrEvidence.consensus(List.of("","3/8","3/8")));
        assertEquals("4/4",MeterOcrEvidence.consensus(List.of("4|4","4/4")));
    }
    @Test public void inkMaskMatchesCleanerThreshold() {
        assertTrue(MeterOcrEvidence.ink(0));assertTrue(MeterOcrEvidence.ink(134));
        assertFalse(MeterOcrEvidence.ink(135));assertFalse(MeterOcrEvidence.ink(255));
    }
}
