// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;
import java.util.*;import org.junit.Test;import static org.junit.Assert.*;
public class OctaveMeasurePrefixTest {
    private int shift(String text) {
        var g=OctaveMarkDetectorTest.page();OctaveMarkDetectorTest.dash(g,120,600,55);
        return OctaveMarkDetectorTest.apply(g,List.of(OctaveMarkDetectorTest.word(text,80,40)),
                List.of(OctaveMarkDetectorTest.note(570,0))).get(0).octaveShift();
    }
    @Test public void joinedSystemNumberRetainsParenthesizedOctave(){assertEquals(1,shift("31 (8va)."));}
    @Test public void joinedSystemNumberRetainsTwoOctaves(){assertEquals(2,shift("127 (15ma)"));}
    @Test public void ordinaryNumericTextDoesNotTranspose(){assertEquals(0,shift("31 eighth notes"));}
    @Test public void unparenthesizedNumericPhraseIsNotGuessed(){assertEquals(0,shift("31 8va"));}
}
