// Copyright 2026 Luckolite
// SPDX-License-Identifier: Apache-2.0
package io.github.luckolite.interpreter;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Original logical meter sequences; no audio engine or source-score fixture required. */
public class ScoreMeterMapTest {
    @Test public void variableMetersHaveCumulativeBarBoundaries() {
        var map=new ScoreMeterMap(5,List.of(new ScoreMeterChange(1,4,4),new ScoreMeterChange(3,12,8)));
        assertEquals(5,map.startBeat(1),.001);
        assertEquals(13,map.startBeat(3),.001);
        assertEquals(25,map.startBeat(5),.001);
        assertEquals(3.5,map.measurePosition(16),.001);
    }
    @Test public void changingMeterUsesQuarterBeatsNotNumeratorAlone() {
        var map=new ScoreMeterMap(4,List.of(new ScoreMeterChange(1,6,4),new ScoreMeterChange(2,4,4)));
        assertEquals(4,map.startBeat(1),0);
        assertEquals(10,map.startBeat(2),0);
        assertEquals(2.5,map.measurePosition(12),0);
        var cut=new ScoreMeterMap(4,List.of(new ScoreMeterChange(0,2,2)));
        assertEquals(4,cut.beatsInMeasure(0),0);
    }
    @Test public void inverseIncludesEveryMeterBoundaryAndFraction() {
        var map=new ScoreMeterMap(3,List.of(new ScoreMeterChange(2,5,8),new ScoreMeterChange(5,7,4)));
        for(int bar=0;bar<9;bar++)for(double fraction:new double[]{0,.125,.5,.875,1})
            assertEquals(bar+fraction,map.measurePosition(map.beatAt(bar,fraction)),1e-9);
    }
    @Test public void changesAreSortedAndNullEntriesIgnored() {
        var map=new ScoreMeterMap(4,Arrays.asList(new ScoreMeterChange(3,6,8),null,new ScoreMeterChange(1,3,4)));
        assertEquals(4,map.beatsInMeasure(0),0);
        assertEquals(3,map.beatsInMeasure(2),0);
        assertEquals(10,map.startBeat(3),0);
        assertEquals(13,map.startBeat(4),0);
    }
    @Test public void nonFiniteOpeningFallsBackAndBoundaryFractionsClamp() {
        var map=new ScoreMeterMap(Float.NaN,null);
        assertEquals(4,map.beatsInMeasure(0),0);
        assertEquals(0,map.startBeat(-2),0);
        assertEquals(0,map.measurePosition(Double.NaN),0);
        assertEquals(0,map.measurePosition(-10),0);
        assertEquals(8,map.beatAt(2,-.5),0);
        assertEquals(12,map.beatAt(2,1.5),0);
    }
    @Test public void constructorSnapshotsChanges() {
        var changes=new java.util.ArrayList<ScoreMeterChange>();
        changes.add(new ScoreMeterChange(1,3,4));
        var map=new ScoreMeterMap(4,changes);
        changes.clear();
        assertEquals(3,map.beatsInMeasure(1),0);
        assertEquals(7,map.startBeat(2),0);
    }
}
